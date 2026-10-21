package com.example.features.ai

import android.content.Context
import com.example.BuildConfig
import com.example.core.study.StudyTimerManager
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * محرك الذكاء الاصطناعي للتطبيق:
 * يدير الاتصال بنموذج Gemini (gemini-3.5-flash) ويفسر الأوامر التنفيذية،
 * كما يمتلك محرك تحليل محلي ذكي فوري باللغة العربية للعمل بدون تأخير أو إنترنت.
 */
object AiAssistantEngine {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val GEMINI_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    suspend fun processUserPrompt(
        context: Context,
        prompt: String,
        uiState: DailyManagerUiState,
        viewModel: DailyManagerViewModel
    ): Pair<String, String?> = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return@withContext Pair("تفضل، كيف يمكنني مساعدتك اليوم؟", null)
        }

        // بناء سياق اليوم
        val contextSummary = buildContextSummary(uiState)

        // محاولة الاتصال بـ Gemini إذا كان المفتاح متوفراً
        val geminiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        var responseText: String? = null
        val isValidKey = geminiKey.isNotBlank() &&
            geminiKey != "MY_GEMINI_API_KEY" &&
            !geminiKey.contains("placeholder", ignoreCase = true)

        if (isValidKey) {
            try {
                responseText = callGeminiRestApi(geminiKey, trimmed, contextSummary)
            } catch (e: Exception) {
                // فشل الاتصال الخارجي، ننتقل فوراً للمحرك المحلي
                responseText = null
            }
        }

        // إذا لم يكن هناك رد من Gemini أو المفتاح افتراضي، نستخدم المعالج المحلي الذكي
        if (responseText.isNullOrBlank()) {
            val localResult = processLocallyWithContext(context, trimmed, uiState, viewModel)
            return@withContext localResult
        }

        // تحليل الرد من Gemini لاستخراج الأوامر التنفيذية وتطبيقها
        val actionBadge = executeActionsFromAiResponse(context, responseText, uiState, viewModel)
        val cleanReply = cleanResponseFromTags(responseText)

        return@withContext Pair(cleanReply, actionBadge)
    }

    private fun buildContextSummary(uiState: DailyManagerUiState): String {
        val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy - HH:mm", Locale("ar"))
        val nowStr = dateFormat.format(Date())

        val nextPrayer = uiState.prayerSchedule.nextPrayer
        val remainingMinutes = (uiState.prayerSchedule.remainingSecondsToNext / 60).coerceAtLeast(0)
        val nextPrayerStr = "الصلاة القادمة: ${nextPrayer.type.arabicName} الساعة ${nextPrayer.formatted12h} (المتبقي: $remainingMinutes دقيقة)"

        val pendingTasks = uiState.mainTasks.filter { !it.isCompleted }
        val completedTasks = uiState.mainTasks.filter { it.isCompleted }

        val pendingTasksDesc = pendingTasks.take(15).joinToString("\n") { task ->
            "- [ID:${task.id}] ${task.title} (أولوية: ${task.priority}, الوقت: ${task.reminderTimeText.ifEmpty { "غير محدد" }})"
        }.ifEmpty { "لا توجد مهام متبقية" }

        val completedTasksDesc = completedTasks.take(10).joinToString("\n") { task ->
            "- [ID:${task.id}] ${task.title}"
        }.ifEmpty { "لم تكتمل مهام بعد" }

        val habitsDesc = uiState.habits.take(15).joinToString("\n") { habit ->
            val log = uiState.habitLogsByHabitAndDate[habit.id]?.values?.lastOrNull()
            val doneToday = log?.isCompleted == true
            "- [ID:${habit.id}] ${habit.title} (الهدف: ${habit.targetCount} ${habit.unitLabel}, منجزة اليوم: ${if (doneToday) "نعم" else "لا"})"
        }.ifEmpty { "لا توجد عادات مسجلة" }

        val quranWird = "الورد القرآني: الصفحة الحالية ${uiState.wirdProgress.currentPage} من أصل ${uiState.wirdProgress.totalPages} (الهدف اليومي: ${uiState.wirdProgress.dailyTargetPages} صفحة)"
        val studySessionsCount = "جلسات التركيز المسجلة: ${uiState.studySessions.size} جلسة"

        val calendarSummary = if (uiState.isGoogleConnected) {
            if (uiState.googleCalendarEvents.isNotEmpty()) {
                "أحداث تقويم Google لليوم (${uiState.googleCalendarEvents.size}):\n" +
                    uiState.googleCalendarEvents.joinToString("\n") { "• [${it.startTimeFormatted}] ${it.title}${if (it.description.isNotBlank()) " - ${it.description.take(40)}" else ""}" }
            } else {
                "تقويم Google: متصل ولكن لا توجد أحداث مجدولة لليوم."
            }
        } else {
            "تقويم Google: غير متصل (يمكن للمستخدم ربطه من الإعدادات)."
        }

        val gmailSummary = if (uiState.isGoogleConnected) {
            if (uiState.gmailMessages.isNotEmpty()) {
                "رسائل Gmail غير المقروءة والمهام المستخرجة (${uiState.gmailMessages.size}):\n" +
                    uiState.gmailMessages.joinToString("\n") { 
                        "• من ${it.sender}: ${it.subject}" + (it.extractedTaskTitle?.let { t -> " (مهمة مقترحة: $t)" } ?: "")
                    }
            } else {
                "بريد Gmail: لا توجد رسائل غير مقروءة تتطلب متابعة."
            }
        } else {
            "بريد Gmail: غير متصل."
        }

        return """
            الوقت الحالي: $nowStr
            $nextPrayerStr
            $quranWird
            $studySessionsCount
            
            $calendarSummary
            
            $gmailSummary
            
            المهام المتبقية (${pendingTasks.size}):
            $pendingTasksDesc
            
            المهام المكتملة (${completedTasks.size}):
            $completedTasksDesc
            
            العادات اليومية (${uiState.habits.size}):
            $habitsDesc
        """.trimIndent()
    }

    private fun callGeminiRestApi(
        apiKey: String,
        userPrompt: String,
        contextSummary: String
    ): String {
        val url = URL("$GEMINI_ENDPOINT?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        val systemPrompt = """
            أنت المساعد الذكي الشخصي والتنفيذي للتطبيق اليومي للمستخدم.
            أنت تملك حق الوصول الكامل والتحكم في التطبيق وبيانات المستخدم (المهام، العادات، الصلوات، الورد، والعداد).
            
            بيانات المستخدم الحالية في التطبيق هي:
            $contextSummary
            
            إرشادات العمل:
            1. أجب دائماً باللغة العربية الفصحى بطريقة ودية، ذكية، مشجعة ومختصرة.
            2. إذا طلب المستخدم إضافة مهمة، نفذها عبر تضمين هذا الوسم في ردك بالضبط:
               [ACTION_ADD_TASK: {"title":"عنوان المهمة", "priority":"HIGH|MEDIUM|LOW", "dueTime":"14:00"}]
            3. إذا طلب المستخدم إنجاز أو شطب مهمة:
               [ACTION_TOGGLE_TASK: {"id":"معرف_المهمة"}]
            4. إذا طلب حذف مهمة:
               [ACTION_DELETE_TASK: {"id":"معرف_المهمة"}]
            5. إذا طلب إضافة عادة:
               [ACTION_ADD_HABIT: {"title":"عنوان العادة", "target":1, "unit":"مرة"}]
            6. إذا طلب إنجاز عادة:
               [ACTION_TOGGLE_HABIT: {"id":"معرف_العادة"}]
            7. إذا طلب حذف عادة:
               [ACTION_DELETE_HABIT: {"id":"معرف_العادة"}]
            8. إذا طلب تشغيل وضع الستاند باي:
               [ACTION_STANDBY]
            9. إذا طلب تشغيل مؤقت/عداد التركيز:
               [ACTION_START_COUNTER: {"title":"اسم النشاط"}]
            10. إذا سأل عما يجب فعله الآن، حلل وقته والصلاة القادمة وأولويات المهام وأرشده بأفضل خطوة تالية مباشرة!
            11. إذا طلب تلخيص اليوم أو تجهيز بداية اليوم، قدم له خطة محكمة وملهمة.
        """.trimIndent()

        val rootJson = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put(
                            "parts",
                            JSONArray().apply {
                                put(JSONObject().put("text", "$systemPrompt\n\nطلب المستخدم: $userPrompt"))
                            }
                        )
                    }
                )
            }
            put("contents", contentsArr)
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(rootJson.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorStream = conn.errorStream?.let { BufferedReader(InputStreamReader(it)).readText() }
            throw RuntimeException("Gemini API Error $responseCode: $errorStream")
        }

        val responseBody = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
        val resJson = JSONObject(responseBody)
        val candidates = resJson.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val first = candidates.getJSONObject(0)
            val content = first.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "")
            }
        }
        return ""
    }

    private fun executeActionsFromAiResponse(
        context: Context,
        rawText: String,
        uiState: DailyManagerUiState,
        viewModel: DailyManagerViewModel
    ): String? {
        val actions = mutableListOf<String>()

        // 1. ADD_TASK
        val addTaskRegex = Regex("\\[ACTION_ADD_TASK:\\s*(\\{.*?\\})\\]")
        addTaskRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                val title = json.optString("title").trim()
                val priority = json.optString("priority", "MEDIUM").uppercase()
                val dueTime = json.optString("dueTime", "")
                if (title.isNotEmpty()) {
                    viewModel.addTask(
                        title = title,
                        notes = "أضيفت بواسطة المساعد الذكي",
                        listCategory = "عام",
                        priority = priority,
                        dueDateText = "",
                        reminderTimeText = dueTime,
                        recurrence = "NONE"
                    )
                    actions.add("تمت إضافة مهمة: $title ✅")
                }
            } catch (_: Exception) {}
        }

        // 2. TOGGLE_TASK
        val toggleTaskRegex = Regex("\\[ACTION_TOGGLE_TASK:\\s*(\\{.*?\\})\\]")
        toggleTaskRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                val taskId = json.optString("id").trim()
                if (taskId.isNotEmpty()) {
                    viewModel.toggleTask(taskId)
                    actions.add("تم تحديث حالة المهمة 🎯")
                }
            } catch (_: Exception) {}
        }

        // 3. DELETE_TASK
        val deleteTaskRegex = Regex("\\[ACTION_DELETE_TASK:\\s*(\\{.*?\\})\\]")
        deleteTaskRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                val taskId = json.optString("id").trim()
                if (taskId.isNotEmpty()) {
                    viewModel.deleteTask(taskId)
                    actions.add("تم حذف المهمة 🗑️")
                }
            } catch (_: Exception) {}
        }

        // 4. ADD_HABIT
        val addHabitRegex = Regex("\\[ACTION_ADD_HABIT:\\s*(\\{.*?\\})\\]")
        addHabitRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                val title = json.optString("title").trim()
                val target = json.optInt("target", 1)
                val unit = json.optString("unit", "مرة")
                if (title.isNotEmpty()) {
                    viewModel.addHabit(
                        title = title,
                        description = "أضيفت بواسطة المساعد الذكي",
                        frequencyType = "DAILY",
                        targetCount = target,
                        unitLabel = unit,
                        autoSource = "MANUAL"
                    )
                    actions.add("تمت إضافة عادة: $title 🌱")
                }
            } catch (_: Exception) {}
        }

        // 5. TOGGLE_HABIT
        val toggleHabitRegex = Regex("\\[ACTION_TOGGLE_HABIT:\\s*(\\{.*?\\})\\]")
        toggleHabitRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                val habitId = json.optString("id").trim()
                val habit = uiState.habits.find { it.id == habitId }
                if (habit != null) {
                    val log = uiState.habitLogsByHabitAndDate[habit.id]?.values?.lastOrNull()
                    val nextCompleted = !(log?.isCompleted == true)
                    viewModel.setCheckboxHabitState(habit, nextCompleted)
                    actions.add("تم تحديث إنجاز عادة: ${habit.title} 🌟")
                }
            } catch (_: Exception) {}
        }

        // 6. DELETE_HABIT
        val deleteHabitRegex = Regex("\\[ACTION_DELETE_HABIT:\\s*(\\{.*?\\})\\]")
        deleteHabitRegex.findAll(rawText).forEach { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                val habitId = json.optString("id").trim()
                if (habitId.isNotEmpty()) {
                    viewModel.deleteHabit(habitId)
                    actions.add("تم حذف العادة 🗑️")
                }
            } catch (_: Exception) {}
        }

        // 7. STANDBY
        if (rawText.contains("[ACTION_STANDBY]")) {
            viewModel.openStandbyMode()
            actions.add("تم فتح وضع الستاند باي 📱")
        }

        // 8. START_COUNTER
        val counterRegex = Regex("\\[ACTION_START_COUNTER:\\s*(\\{.*?\\})\\]")
        counterRegex.find(rawText)?.let { match ->
            try {
                val json = JSONObject(match.groupValues[1])
                val title = json.optString("title", "جلسة تركيز")
                StudyTimerManager.startTimerForTarget(
                    context = context,
                    targetType = "NONE",
                    targetId = null,
                    targetTitle = title,
                    targetColorHex = "#10B981",
                    mode = "COUNT_UP",
                    targetMinutes = 25
                )
                actions.add("تم بدء عداد التركيز: $title ⏱️")
            } catch (_: Exception) {}
        }

        return if (actions.isNotEmpty()) actions.joinToString(" • ") else null
    }

    private fun cleanResponseFromTags(text: String): String {
        return text
            .replace(Regex("\\[ACTION_[A-Z_]+(:\\s*\\{.*?\\})?\\]"), "")
            .trim()
    }

    /**
     * معالج محلي فوري فائق الذكاء باللغة العربية
     * قادر على فهم الأوامر وإدارتها حتى بدون إنترنت أو عند انعدام مفتاح API
     */
    private fun processLocallyWithContext(
        context: Context,
        prompt: String,
        uiState: DailyManagerUiState,
        viewModel: DailyManagerViewModel
    ): Pair<String, String?> {
        val lower = prompt.lowercase().trim()
        val pendingTasks = uiState.mainTasks.filter { !it.isCompleted }
        val completedTasks = uiState.mainTasks.filter { it.isCompleted }
        val nextPrayer = uiState.prayerSchedule.nextPrayer
        val remainingMinutes = (uiState.prayerSchedule.remainingSecondsToNext / 60).coerceAtLeast(0)

        // 1. طلب ملخص اليوم (Summary)
        if (lower.contains("لخص") || lower.contains("ملخص") || lower.contains("تقرير") || lower.contains("وش عندي اليوم")) {
            val prayerInfo = "🕌 الصلاة القادمة: ${nextPrayer.type.arabicName} الساعة ${nextPrayer.formatted12h} (متبقي $remainingMinutes دقيقة)."

            val tasksInfo = if (pendingTasks.isNotEmpty()) {
                "📋 متبقي لديك ${pendingTasks.size} مهام، أهمها:\n" +
                    pendingTasks.take(3).joinToString("\n") { "• ${it.title} (${it.priority})" }
            } else {
                "📋 رائع! لقد أنهيت جميع مهام اليوم المسجلة 👏"
            }

            val habitsInfo = if (uiState.habits.isNotEmpty()) {
                val completedHabits = uiState.habits.count { habit ->
                    uiState.habitLogsByHabitAndDate[habit.id]?.values?.lastOrNull()?.isCompleted == true
                }
                "🌱 العادات: أنجزت $completedHabits من أصل ${uiState.habits.size} عادة اليوم."
            } else {
                "🌱 لم تقم بإضافة عادات بعد."
            }

            val wirdInfo = "📖 الورد القرآني: أنت في الصفحة ${uiState.wirdProgress.currentPage} (الهدف اليومي: ${uiState.wirdProgress.dailyTargetPages} صفحة)."

            val reply = """
                📊 **ملخص يومك الشامل:**
                
                $prayerInfo
                
                $tasksInfo
                
                $habitsInfo
                
                $wirdInfo
                
                💪 يومك مليء بالفرص! هل تود أن أساعدك في إنجاز شيء محدد الآن؟
            """.trimIndent()

            return Pair(reply, "تم توليد ملخص اليوم 📊")
        }

        // 2. وش المفروض أسوي الحين؟ (What to do next)
        if (lower.contains("وش اسوي") || lower.contains("وش مفروض") || lower.contains("ماذا افعل") || lower.contains("ايش اسوي") || lower.contains("أولوياتي")) {
            val highPriorityTask = pendingTasks.firstOrNull { it.priority == "HIGH" || it.priority == "URGENT" }
                ?: pendingTasks.firstOrNull()

            val suggestion = StringBuilder()
            suggestion.append("🎯 **توصيتي لك في هذه اللحظة:**\n\n")

            if (remainingMinutes <= 20) {
                suggestion.append("🕌 **الصلاة أولاً:** باقي فقط $remainingMinutes دقيقة على أذان ${nextPrayer.type.arabicName}. استعد للوضوء والمسجد.\n\n")
            }

            if (highPriorityTask != null) {
                suggestion.append("⚡ **المهمة الأهم لديك الآن:**\n")
                suggestion.append("«${highPriorityTask.title}»\n")
                if (highPriorityTask.reminderTimeText.isNotBlank()) {
                    suggestion.append("⏰ وقتها المحدد: ${highPriorityTask.reminderTimeText}\n")
                }
                suggestion.append("ابدأ فيها الآن لمدة 25 دقيقة وسأفعل لك مؤقت التركيز إذا رغبت!\n\n")
            } else {
                suggestion.append("✨ لا توجد مهام ملحة حالياً! يمكنك مراجعة وردك القرآني أو إنجاز عاداتك اليومية.\n\n")
            }

            val unfinishedHabit = uiState.habits.firstOrNull { habit ->
                uiState.habitLogsByHabitAndDate[habit.id]?.values?.lastOrNull()?.isCompleted != true
            }
            if (unfinishedHabit != null) {
                suggestion.append("🌱 **عادة مقترحة للإنجاز:** «${unfinishedHabit.title}»")
            }

            return Pair(suggestion.toString(), "تم تحليل أولوياتك الحالية 🎯")
        }

        // 3. تجهيز لبداية اليوم (Morning Setup / Day Preparation)
        if (lower.contains("تجهيز") || lower.contains("بداية اليوم") || lower.contains("ابدأ يومي") || lower.contains("صباح") || lower.contains("خطط ليومي")) {
            val reply = """
                🌅 **خطة انطلاقة يومك المميز:**
                
                1. 🕌 **البركة أولاً:** جدول صلواتك ووردك القرآني (${uiState.wirdProgress.dailyTargetPages} صفحة).
                2. 🎯 **قاعدة الـ 3 مهام الأساسية:**
                ${if (pendingTasks.isNotEmpty()) pendingTasks.take(3).mapIndexed { i, t -> "   ${i + 1}. ${t.title}" }.joinToString("\n") else "   • أضف الآن أهم 3 مهام تريد إنجازها اليوم."}
                3. 🌱 **العادات اليومية:** حافظ على الاستمرارية وعدم كسر السلسلة!
                
                هل تود أن نضيف مهام جديدة لبداية يومك؟ فقط قل لي اسمها وسأضيفها فوراً! ✍️
            """.trimIndent()
            return Pair(reply, "تم تجهيز خطة بداية اليوم 🌅")
        }

        // 4. أمر إضافة مهمة (Add Task)
        val addTaskMatch = Regex("(?:اضف|أضف|سجل|سجّل|حط|إنشاء)\\s+(?:لي\\s+)?(?:مهمة|مهمه)\\s*(.*)").find(prompt)
        if (addTaskMatch != null) {
            val taskText = addTaskMatch.groupValues[1].trim()
            if (taskText.isNotEmpty()) {
                val cleanTitle = taskText.removePrefix(":").removePrefix("-").trim()
                viewModel.addTask(
                    title = cleanTitle,
                    notes = "أضيفت عبر المساعد الذكي",
                    listCategory = "عام",
                    priority = if (cleanTitle.contains("مهم") || cleanTitle.contains("عاجل")) "HIGH" else "MEDIUM",
                    dueDateText = "",
                    reminderTimeText = "",
                    recurrence = "NONE"
                )
                return Pair(
                    "أبشر! تمت إضافة المهمة الجديدة:\n📌 «$cleanTitle»\n\nتظهر الآن في قائمة مهامك ويمكنك تتبعها وإنجازها في أي وقت.",
                    "تمت إضافة المهمة بنجاح ✅"
                )
            }
        }

        // 5. أمر إنجاز مهمة (Toggle Task)
        val completeTaskMatch = Regex("(?:اكمل|أكمل|انجز|أنجز|خلصت|تمت)\\s+(?:مهمة|مهمه)?\\s*(.*)").find(prompt)
        if (completeTaskMatch != null) {
            val query = completeTaskMatch.groupValues[1].trim()
            val targetTask = pendingTasks.find { it.title.contains(query, ignoreCase = true) }
                ?: uiState.mainTasks.find { it.title.contains(query, ignoreCase = true) }

            if (targetTask != null) {
                viewModel.toggleTask(targetTask.id)
                return Pair(
                    "كفو عليك! 🎉 تم شطب المهمة وإنجازها:\n✅ «${targetTask.title}»\nأحسنت الاستمرار!",
                    "تم إنجاز المهمة 🎯"
                )
            }
        }

        // 6. أمر حذف مهمة (Delete Task)
        val deleteTaskMatch = Regex("(?:احذف|إحذف|امسح|إمسح|شيل)\\s+(?:مهمة|مهمه)\\s*(.*)").find(prompt)
        if (deleteTaskMatch != null) {
            val query = deleteTaskMatch.groupValues[1].trim()
            val targetTask = uiState.mainTasks.find { it.title.contains(query, ignoreCase = true) }
            if (targetTask != null) {
                viewModel.deleteTask(targetTask.id)
                return Pair(
                    "تم حذف المهمة بنجاح:\n🗑️ «${targetTask.title}»",
                    "تم حذف المهمة 🗑️"
                )
            }
        }

        // 7. أمر إضافة عادة (Add Habit)
        val addHabitMatch = Regex("(?:اضف|أضف|سجل|سجّل)\\s+(?:لي\\s+)?(?:عادة|عاده)\\s*(.*)").find(prompt)
        if (addHabitMatch != null) {
            val habitText = addHabitMatch.groupValues[1].trim()
            if (habitText.isNotEmpty()) {
                val cleanTitle = habitText.removePrefix(":").removePrefix("-").trim()
                viewModel.addHabit(
                    title = cleanTitle,
                    description = "أضيفت عبر المساعد الذكي",
                    frequencyType = "DAILY",
                    targetCount = 1,
                    unitLabel = "مرة",
                    autoSource = "MANUAL"
                )
                return Pair(
                    "تم تسجيل عادتك الجديدة بنجاح 🌱\n✨ «$cleanTitle»\n\nتذكر: الاستمرار الصغير كل يوم يبني نتائج عظيمة!",
                    "تمت إضافة العادة بنجاح 🌱"
                )
            }
        }

        // 8. أمر إنجاز عادة (Toggle Habit)
        val completeHabitMatch = Regex("(?:اكملت|أكملت|سويت|أنجزت|انجزت)\\s+(?:عادة|عاده)\\s*(.*)").find(prompt)
        if (completeHabitMatch != null) {
            val query = completeHabitMatch.groupValues[1].trim()
            val targetHabit = uiState.habits.find { it.title.contains(query, ignoreCase = true) }
            if (targetHabit != null) {
                viewModel.setCheckboxHabitState(targetHabit, true)
                return Pair(
                    "بارك الله فيك! تم تسجيل إنجاز العادة اليوم:\n🌟 «${targetHabit.title}»\nسلسلة العادات في تصاعد مستمر!",
                    "تم تسجيل العادة بنجاح 🌟"
                )
            }
        }

        // 9. أمر حذف عادة (Delete Habit)
        val deleteHabitMatch = Regex("(?:احذف|إحذف|امسح|شيل)\\s+(?:عادة|عاده)\\s*(.*)").find(prompt)
        if (deleteHabitMatch != null) {
            val query = deleteHabitMatch.groupValues[1].trim()
            val targetHabit = uiState.habits.find { it.title.contains(query, ignoreCase = true) }
            if (targetHabit != null) {
                viewModel.deleteHabit(targetHabit.id)
                return Pair(
                    "تم حذف العادة بنجاح:\n🗑️ «${targetHabit.title}»",
                    "تم حذف العادة 🗑️"
                )
            }
        }

        // 10. تشغيل العداد أو مؤقت التركيز (Timer)
        if (lower.contains("شغل العداد") || lower.contains("مؤقت") || lower.contains("تركيز") || lower.contains("بومودورو")) {
            StudyTimerManager.startTimerForTarget(
                context = context,
                targetType = "NONE",
                targetId = null,
                targetTitle = "جلسة تركيز ذكية",
                targetColorHex = "#10B981",
                mode = "COUNT_UP",
                targetMinutes = 25
            )
            return Pair(
                "تم تشغيل عداد التركيز لك فوراً ⏱️\nأغلق المشتتات وابدأ الآن!",
                "تم تشغيل العداد بنجاح ⏱️"
            )
        }

        // 11. تشغيل وضع الستاند باي (Standby)
        if (lower.contains("ستاند باي") || lower.contains("ستاندباي") || lower.contains("شاشه الانتظار")) {
            viewModel.openStandbyMode()
            return Pair(
                "تم تفعيل وضع الستاند باي لك الآن 📱",
                "تم فتح وضع الستاند باي 📱"
            )
        }

        // 12. استفسار عن الصلوات
        if (lower.contains("صلاة") || lower.contains("صلوات") || lower.contains("اذان") || lower.contains("أذان")) {
            val prayersFormatted = uiState.prayerSchedule.prayers.joinToString("\n") {
                "• ${it.type.arabicName}: ${it.formatted12h}"
            }
            val reply = """
                🕌 **مواقيت الصلوات لليوم (${uiState.selectedCity.arabicName}):**
                
                $prayersFormatted
                
                ⏳ القادمة هي: ${nextPrayer.type.arabicName} (متبقي $remainingMinutes دقيقة)
            """.trimIndent()
            return Pair(reply, "مواقيت الصلوات 🕌")
        }

        // 13. استفسار عن تقويم Google (Calendar)
        if (lower.contains("تقويم") || lower.contains("calendar") || lower.contains("مواعيد اليوم") || lower.contains("جدولي اليوم") || lower.contains("وش عندي في التقويم")) {
            if (!uiState.isGoogleConnected) {
                return Pair(
                    "لم يتم ربط حساب Google الخاص بك بعد 📅.\nيمكنك التوجه إلى شاشة الإعدادات وربط حسابك لمزامنة أحداث التقويم ورسائل البريد فوراً!",
                    "تقويم Google غير متصل ⚠️"
                )
            }
            val events = uiState.googleCalendarEvents
            if (events.isEmpty()) {
                return Pair(
                    "تفقدت تقويم Google الخاص بك 📅:\nلا توجد أي مواعيد أو أحداث مجدولة لليوم. وقتك متاح للمهام والتركيز!",
                    "التقويم فارغ لليوم ✨"
                )
            }
            val eventsList = events.joinToString("\n") { ev ->
                "• [${ev.startTimeFormatted}] ${ev.title}" + if (ev.description.isNotBlank()) " (${ev.description.take(30)})" else ""
            }
            val reply = """
                📅 **مواعيدك في تقويم Google لليوم (${events.size}):**
                
                $eventsList
                
                💡 هل تريدني أن أساعدك في ترتيب مهامك لتناسب هذه المواعيد؟
            """.trimIndent()
            return Pair(reply, "تم جلب أحداث التقويم 📅")
        }

        // 14. استفسار عن بريد Gmail والمهام المستخرجة
        if (lower.contains("إيميل") || lower.contains("ايميل") || lower.contains("بريد") || lower.contains("جيميل") || lower.contains("gmail") || lower.contains("رسائل")) {
            if (!uiState.isGoogleConnected) {
                return Pair(
                    "لم يتم ربط حساب Google بعد ✉️.\nيمكنك ربطه من الإعدادات للسماح بقراءة الرسائل غير المقروءة واستخراج المهام تلقائياً.",
                    "Gmail غير متصل ⚠️"
                )
            }
            val messages = uiState.gmailMessages
            if (messages.isEmpty()) {
                return Pair(
                    "تفقدت بريد Gmail الخاص بك ✉️:\nلا توجد رسائل غير مقروءة تتطلب إجراء أو مهام جديدة حالياً.",
                    "لا توجد رسائل جديدة ✉️"
                )
            }

            // إذا كان المستخدم يطلب إضافة المهام المستخرجة
            if (lower.contains("استخرج") || lower.contains("اضف") || lower.contains("أضف") || lower.contains("حولها") || lower.contains("حوّلها")) {
                var addedCount = 0
                messages.forEach { msg ->
                    if (!msg.extractedTaskTitle.isNullOrBlank()) {
                        viewModel.addEmailTaskDirectly(msg)
                        addedCount++
                    }
                }
                return if (addedCount > 0) {
                    Pair(
                        "تمت إضافة $addedCount مهام بنجاح من رسائل بريدك الإلكتروني إلى قائمة المهام! 🎯",
                        "تم استخراج $addedCount مهام ✉️"
                    )
                } else {
                    Pair(
                        "تفقدت الرسائل ولكن لم أجد مهاماً واضحة لاستخراجها تلقائياً. هل تود إضافة مهمة لمتابعة إيميل محدد؟",
                        null
                    )
                }
            }

            val list = messages.take(5).joinToString("\n\n") { msg ->
                val taskHint = msg.extractedTaskTitle?.let { "\n   ⚡ *مهمة مقترحة:* $it" } ?: ""
                "• **من:** ${msg.sender}\n  **الموضوع:** ${msg.subject}$taskHint"
            }
            val reply = """
                ✉️ **أحدث رسائل Gmail غير المقروءة (${messages.size}):**
                
                $list
                
                💡 يمكنك أن تقول لي: «استخرج المهام من الإيميل» لإضافتها فوراً لقائمة مهامك!
            """.trimIndent()
            return Pair(reply, "تم فحص بريد Gmail ✉️")
        }

        // الرد العام
        val generalReply = """
            أهلاً بك.
            وصلني طلبك: «$prompt»
            يمكنك كتابة طلبك لتنفيذه مباشرة في التطبيق.
        """.trimIndent()

        return Pair(generalReply, null)
    }
}
