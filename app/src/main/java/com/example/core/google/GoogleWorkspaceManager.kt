package com.example.core.google

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.TaskEntity
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
import java.util.TimeZone

/**
 * نموذج حدث تقويم Google
 */
data class GoogleCalendarEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val startTimeFormatted: String = "",
    val endTimeFormatted: String = "",
    val isAllDay: Boolean = false,
    val htmlLink: String = ""
)

/**
 * نموذج بريد إلكتروني مقروء ومستخرج من Gmail
 */
data class GmailEmailItem(
    val id: String,
    val threadId: String,
    val subject: String,
    val sender: String,
    val snippet: String,
    val dateReceived: String,
    val extractedTaskTitle: String? = null
)

/**
 * مدير وخدمة تكامل Google Workspace (Calendar & Gmail)
 * يتولى حفظ رمز الدخول (Access Token)، مزامنة أحداث التقويم، وقراءة رسائل البريد واستخراج المهام
 */
object GoogleWorkspaceManager {

    private const val TAG = "GoogleWorkspaceManager"
    private const val PREFS_NAME = "google_workspace_prefs"
    private const val KEY_ACCESS_TOKEN = "google_access_token"
    private const val KEY_USER_EMAIL = "google_user_email"
    private const val KEY_LAST_SYNC_TIME = "google_last_sync_time"

    fun saveAccessToken(context: Context, token: String, email: String = "") {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_EMAIL, email)
            .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun isConnected(context: Context): Boolean {
        return !getAccessToken(context).isNullOrBlank()
    }

    fun disconnect(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // ==========================================
    // 1. Google Calendar Integration
    // ==========================================

    /**
     * جلب الأحداث القادمة لليوم من تقويم Google
     */
    suspend fun fetchTodayCalendarEvents(context: Context): Result<List<GoogleCalendarEvent>> = withContext(Dispatchers.IO) {
        val token = getAccessToken(context)
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم ربط حساب Google بعد"))
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val timeMin = sdf.format(Date())

            val calUrl = "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                    "?timeMin=$timeMin&singleEvents=true&orderBy=startTime&maxResults=15"

            val url = URL(calUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 12000
            conn.readTimeout = 12000

            val code = conn.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseStr = reader.readText()
                reader.close()

                val json = JSONObject(responseStr)
                val items = json.optJSONArray("items") ?: JSONArray()
                val events = mutableListOf<GoogleCalendarEvent>()

                val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale("ar"))

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item.optString("id", "")
                    val summary = item.optString("summary", "حدث بدون عنوان")
                    val desc = item.optString("description", "")
                    val link = item.optString("htmlLink", "")

                    val startObj = item.optJSONObject("start")
                    val endObj = item.optJSONObject("end")

                    val startDateTime = startObj?.optString("dateTime")
                    val startDate = startObj?.optString("date")
                    val isAllDay = !startDate.isNullOrBlank()

                    var formattedTime = if (isAllDay) "طوال اليوم" else ""
                    if (!startDateTime.isNullOrBlank()) {
                        try {
                            val parseFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            val cleanDateStr = startDateTime.substring(0, 19)
                            val parsedDate = parseFormat.parse(cleanDateStr)
                            if (parsedDate != null) {
                                formattedTime = displayTimeFormat.format(parsedDate)
                            }
                        } catch (_: Exception) {
                            formattedTime = startDateTime
                        }
                    }

                    events.add(
                        GoogleCalendarEvent(
                            id = id,
                            title = summary,
                            description = desc,
                            startTimeFormatted = formattedTime,
                            isAllDay = isAllDay,
                            htmlLink = link
                        )
                    )
                }

                Result.success(events)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "خطأ $code"
                Log.e(TAG, "Calendar fetch error ($code): $err")
                Result.failure(Exception("خطأ في الاتصال بتقويم Google ($code)"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching calendar events", e)
            Result.failure(e)
        }
    }

    /**
     * إنشاء وإضافة مهمة إلى تقويم Google كحدث مجدول
     */
    suspend fun exportTaskToCalendar(context: Context, task: TaskEntity): Result<String> = withContext(Dispatchers.IO) {
        val token = getAccessToken(context)
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم ربط حساب Google"))
        }

        try {
            val url = URL("https://www.googleapis.com/calendar/v3/calendars/primary/events")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.connectTimeout = 12000
            conn.readTimeout = 12000

            val postJson = JSONObject()
            postJson.put("summary", "✅ ${task.title}")
            postJson.put("description", "${task.notes}\n\n[تم تصديرها من تطبيق مدير يومي]")

            val now = Date()
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val startMillis = if (task.alarmTime24.isNotBlank() && task.alarmTime24.contains(":")) {
                val parts = task.alarmTime24.split(":")
                val h = parts[0].toIntOrNull() ?: 12
                val m = parts[1].toIntOrNull() ?: 0
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, h)
                cal.set(java.util.Calendar.MINUTE, m)
                cal.timeInMillis
            } else {
                now.time + 30 * 60 * 1000L
            }

            val startIso = isoFormat.format(Date(startMillis))
            val endIso = isoFormat.format(Date(startMillis + 45 * 60 * 1000L))

            val startObj = JSONObject().apply { put("dateTime", startIso) }
            val endObj = JSONObject().apply { put("dateTime", endIso) }

            postJson.put("start", startObj)
            postJson.put("end", endObj)

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(postJson.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val res = conn.inputStream.bufferedReader().readText()
                val resObj = JSONObject(res)
                val eventId = resObj.optString("id", "")
                Result.success(eventId)
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "خطأ $code"
                Result.failure(Exception("فشل إضافة الحدث إلى التقويم ($code): $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 2. Gmail Integration
    // ==========================================

    /**
     * جلب أحدث رسائل Gmail غير المقروءة واستخراج المهام المحتملة منها
     */
    suspend fun fetchRecentEmailsAndExtractTasks(context: Context, maxResults: Int = 10): Result<List<GmailEmailItem>> = withContext(Dispatchers.IO) {
        val token = getAccessToken(context)
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم ربط حساب Google"))
        }

        try {
            // جلب قائمة معرفات الرسائل المهمة أو غير المقروءة
            val listUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=$maxResults&q=is:unread"
            val conn = URL(listUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 12000
            conn.readTimeout = 12000

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "خطأ $code"
                return@withContext Result.failure(Exception("خطأ في قراءة رسائل Gmail ($code): $err"))
            }

            val listJson = JSONObject(conn.inputStream.bufferedReader().readText())
            val messagesArray = listJson.optJSONArray("messages") ?: JSONArray()
            val emailItems = mutableListOf<GmailEmailItem>()

            for (i in 0 until messagesArray.length()) {
                val msgObj = messagesArray.getJSONObject(i)
                val msgId = msgObj.optString("id")

                // جلب تفاصيل كل رسالة
                val detailUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$msgId?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date"
                val detailConn = URL(detailUrl).openConnection() as HttpURLConnection
                detailConn.requestMethod = "GET"
                detailConn.setRequestProperty("Authorization", "Bearer $token")
                detailConn.setRequestProperty("Accept", "application/json")
                detailConn.connectTimeout = 10000
                detailConn.readTimeout = 10000

                if (detailConn.responseCode in 200..299) {
                    val detailJson = JSONObject(detailConn.inputStream.bufferedReader().readText())
                    val snippet = detailJson.optString("snippet", "")
                    val threadId = detailJson.optString("threadId", "")

                    val payload = detailJson.optJSONObject("payload")
                    val headers = payload?.optJSONArray("headers") ?: JSONArray()

                    var subject = "بدون موضوع"
                    var sender = "غير معروف"
                    var dateStr = ""

                    for (h in 0 until headers.length()) {
                        val header = headers.getJSONObject(h)
                        val name = header.optString("name")
                        val value = header.optString("value")
                        when (name.lowercase()) {
                            "subject" -> subject = value
                            "from" -> sender = value.substringBefore("<").trim().ifEmpty { value }
                            "date" -> dateStr = value
                        }
                    }

                    // التحليل الذكي السريع لاستخراج مهمة من الرسالة إذا كانت تتطلب عملاً
                    val extractedTask = analyzeEmailForActionableTask(subject, snippet)

                    emailItems.add(
                        GmailEmailItem(
                            id = msgId,
                            threadId = threadId,
                            subject = subject,
                            sender = sender,
                            snippet = snippet,
                            dateReceived = dateStr,
                            extractedTaskTitle = extractedTask
                        )
                    )
                }
            }

            Result.success(emailItems)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Gmail messages", e)
            Result.failure(e)
        }
    }

    /**
     * استخراج عنوان مهمة تلقائي من نص الرسالة وموضوعها
     */
    private fun analyzeEmailForActionableTask(subject: String, snippet: String): String? {
        val combined = "$subject $snippet".lowercase()
        val triggerKeywords = listOf(
            "مطلوب", "يرجى", "تسليم", "موعد", "مشروع", "تقرير", "واجب",
            "اجتماع", "فاتورة", "دفع", "urgent", "deadline", "meeting",
            "review", "submit", "task", "action required"
        )

        val hasTrigger = triggerKeywords.any { combined.contains(it) }
        return if (hasTrigger) {
            "متابعة: ${subject.take(40)}"
        } else {
            null
        }
    }
}
