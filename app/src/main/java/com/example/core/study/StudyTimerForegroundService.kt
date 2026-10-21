package com.example.core.study

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.notifications.AdhanNotificationHelper
import com.example.data.AppDatabase
import com.example.data.DailyManagerRepository
import com.example.data.SubjectEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * خدمة الخلفية لعداد المذاكرة والتركيز:
 * تدعم:
 * 1) المؤقت التصاعدي (COUNT_UP): يبدأ من 00:00، وعند الوصول للهدف المطلوب يرسل إشعار تحقيق الهدف ويستمر في العد لاحتساب أي وقت إضافي
 * 2) البومودورو المخصص (POMODORO): يتنقل تلقائياً بين جولات التركيز والراحة المخصصة، ويسمح بجولات إضافية فوق المطلوب
 */
class StudyTimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureStudyNotificationChannel()

        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildOngoingNotification())
                startTicker()
            }
            ACTION_TOGGLE_PAUSE -> {
                val now = System.currentTimeMillis()
                StudyTimerManager.internalUpdate { current ->
                    val nextPaused = !current.isPaused
                    current.copy(
                        isPaused = nextPaused,
                        pauseTimeMillis = if (nextPaused) now else current.pauseTimeMillis,
                        resumeTimeMillis = if (!nextPaused) now else current.resumeTimeMillis
                    )
                }
                updateOngoingNotification()
            }
            ACTION_FINISH_SAVE -> {
                saveSessionToDatabaseAndStop()
            }
            ACTION_CANCEL -> {
                tickerJob?.cancel()
                StudyTimerManager.internalUpdate { current ->
                    current.copy(
                        isActive = false,
                        isPaused = false,
                        elapsedSeconds = 0,
                        phaseElapsedSeconds = 0,
                        currentRound = 1,
                        isBreakPhase = false,
                        goalReachedNotified = false
                    )
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        var screenOffCounter = 0
        tickerJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val state = StudyTimerManager.timerState.value
                if (!state.isActive) break
                if (state.isPaused) continue

                // إذا كانت الشاشة مغلقة / الجوال مقفلاً، يشتغل ثانية ويطفى ثانية (30 دقيقة قفل = 15 دقيقة عداد)
                val isScreenInteractive = powerManager?.isInteractive ?: true
                if (!isScreenInteractive) {
                    screenOffCounter++
                    if (screenOffCounter % 2 != 0) {
                        continue
                    }
                } else {
                    screenOffCounter = 0
                }

                if (state.mode == "POMODORO") {
                    val nextPhaseSec = state.phaseElapsedSeconds + 1
                    val phaseLimitSec = (if (state.isBreakPhase) state.pomodoroBreakMinutes else state.pomodoroFocusMinutes) * 60
                    val nextTotalFocusSec = if (state.isBreakPhase) state.elapsedSeconds else state.elapsedSeconds + 1
                    val justReachedGoal = !state.goalReachedNotified && nextTotalFocusSec >= state.targetDurationSeconds

                    if (justReachedGoal) {
                        AdhanNotificationHelper.showStudyPomodoroCompletedNotification(
                            context = applicationContext,
                            subjectName = state.subjectName,
                            durationMinutes = (state.targetDurationSeconds / 60).coerceAtLeast(1)
                        )
                    }

                    if (nextPhaseSec >= phaseLimitSec) {
                        // انتهت الجولة الحالية (تركيز أو راحة) -> انتقل للمرحلة التالية واستمر!
                        if (!state.isBreakPhase) {
                            // انتهت جولة تركيز -> ابدأ جولة راحة
                            StudyTimerManager.internalUpdate { cur ->
                                cur.copy(
                                    elapsedSeconds = nextTotalFocusSec,
                                    isBreakPhase = true,
                                    phaseElapsedSeconds = 0,
                                    goalReachedNotified = cur.goalReachedNotified || justReachedGoal,
                                    lastCompletedMessage = if (justReachedGoal) {
                                        "🎉 حققت هدف «${cur.subjectName}» بالكامل! يمكنك الاستمرار لجولات إضافية أو الحفظ الآن"
                                    } else {
                                        "☕ اكتملت الجولة ${cur.currentRound} — وقت راحة (${cur.pomodoroBreakMinutes} د)"
                                    }
                                )
                            }
                        } else {
                            // انتهت الراحة -> ابدأ جولة تركيز جديدة
                            StudyTimerManager.internalUpdate { cur ->
                                cur.copy(
                                    isBreakPhase = false,
                                    currentRound = cur.currentRound + 1,
                                    phaseElapsedSeconds = 0,
                                    lastCompletedMessage = "🔥 بدأت جولة التركيز رقم ${cur.currentRound + 1}"
                                )
                            }
                        }
                    } else {
                        StudyTimerManager.internalUpdate { cur ->
                            cur.copy(
                                elapsedSeconds = nextTotalFocusSec,
                                phaseElapsedSeconds = nextPhaseSec,
                                goalReachedNotified = cur.goalReachedNotified || justReachedGoal,
                                lastCompletedMessage = if (justReachedGoal) {
                                    "🎉 حققت الهدف المطلوب لـ «${cur.subjectName}»! استمر كما تشاء أو احفظ الجلسة"
                                } else cur.lastCompletedMessage
                            )
                        }
                    }
                } else if (state.mode == "COUNTDOWN") {
                    // العداد التنازلي: ينقص حتى يصل إلى 00:00 ويتوقف مع تنبيه
                    val nextElapsed = state.elapsedSeconds + 1
                    val isFinished = nextElapsed >= state.targetDurationSeconds
                    val justReachedZero = !state.goalReachedNotified && isFinished

                    if (justReachedZero) {
                        AdhanNotificationHelper.showStudyPomodoroCompletedNotification(
                            context = applicationContext,
                            subjectName = if (state.subjectName.isNotBlank()) state.subjectName else "العداد التنازلي",
                            durationMinutes = (state.targetDurationSeconds / 60).coerceAtLeast(1)
                        )
                    }

                    StudyTimerManager.internalUpdate { cur ->
                        cur.copy(
                            elapsedSeconds = if (isFinished) cur.targetDurationSeconds else nextElapsed,
                            isPaused = if (isFinished) true else cur.isPaused,
                            goalReachedNotified = cur.goalReachedNotified || justReachedZero,
                            lastCompletedMessage = if (justReachedZero) {
                                "⏳🎉 انتهى العداد التنازلي بنجاح! يمكنك الآن حفظ الجلسة أو بدء عداد جديد"
                            } else cur.lastCompletedMessage
                        )
                    }
                } else {
                    // المؤقت التصاعدي (COUNT_UP): يبدأ من 00:00 ويستمر في الصعود حتى بعد تحقيق الهدف
                    val nextElapsed = state.elapsedSeconds + 1
                    val justReachedGoal = !state.goalReachedNotified && nextElapsed >= state.targetDurationSeconds

                    if (justReachedGoal) {
                        AdhanNotificationHelper.showStudyPomodoroCompletedNotification(
                            context = applicationContext,
                            subjectName = state.subjectName,
                            durationMinutes = (state.targetDurationSeconds / 60).coerceAtLeast(1)
                        )
                    }

                    StudyTimerManager.internalUpdate { cur ->
                        cur.copy(
                            elapsedSeconds = nextElapsed,
                            goalReachedNotified = cur.goalReachedNotified || justReachedGoal,
                            lastCompletedMessage = if (justReachedGoal) {
                                "🎉 حققت الهدف المطلوب لـ «${cur.subjectName}»! العداد مستمر لاحتساب أي وقت إضافي"
                            } else cur.lastCompletedMessage
                        )
                    }
                }

                updateOngoingNotification()
            }
        }
    }

    private fun saveSessionToDatabaseAndStop() {
        tickerJob?.cancel()
        val snapshot = StudyTimerManager.timerState.value
        val recordedSeconds = snapshot.elapsedSeconds

        serviceScope.launch {
            if (recordedSeconds >= 10) {
                val db = AppDatabase.getInstance(applicationContext)
                val repo = DailyManagerRepository(db)
                val subject = SubjectEntity(
                    id = snapshot.subjectId,
                    name = snapshot.subjectName,
                    colorHex = snapshot.subjectColorHex
                )
                val targetMins = (snapshot.targetDurationSeconds / 60).coerceAtLeast(1)
                val actualMins = (recordedSeconds / 60).coerceAtLeast(1)
                val extraMins = (actualMins - targetMins).coerceAtLeast(0)

                val note = when {
                    actualMins >= targetMins && extraMins > 0 ->
                        "حقق الهدف ($targetMins د) + $extraMins د إضافية 🔥"
                    actualMins >= targetMins ->
                        "حقق الهدف المطلوب بالكامل ($actualMins د) ✅"
                    else ->
                        "جلسة جزئية ($actualMins من $targetMins د)"
                }

                val now = System.currentTimeMillis()
                val startedAt = snapshot.startTimeMillis ?: (now - (recordedSeconds * 1000L))
                val pausedAt = snapshot.pauseTimeMillis
                val completedAt = now

                repo.saveStudySessionAndSync(
                    subjectName = snapshot.subjectName,
                    subjectColorHex = snapshot.subjectColorHex,
                    mode = snapshot.mode,
                    durationSeconds = recordedSeconds,
                    startedAt = startedAt,
                    endedAt = completedAt,
                    pausedAt = pausedAt,
                    completedAt = completedAt,
                    linkedType = snapshot.linkedType,
                    linkedId = snapshot.linkedId,
                    notes = note
                )
                StudyTimerManager.internalUpdate {
                    StudyTimerState(
                        subjectId = snapshot.subjectId,
                        subjectName = snapshot.subjectName,
                        subjectColorHex = snapshot.subjectColorHex,
                        mode = snapshot.mode,
                        targetDurationSeconds = snapshot.targetDurationSeconds,
                        pomodoroFocusMinutes = snapshot.pomodoroFocusMinutes,
                        pomodoroBreakMinutes = snapshot.pomodoroBreakMinutes,
                        pomodoroTargetRounds = snapshot.pomodoroTargetRounds,
                        lastCompletedMessage = "✅ تم حفظ $actualMins دقيقة لـ «${snapshot.subjectName}»"
                    )
                }
            } else {
                StudyTimerManager.internalUpdate {
                    it.copy(
                        isActive = false,
                        isPaused = false,
                        elapsedSeconds = 0,
                        phaseElapsedSeconds = 0,
                        lastCompletedMessage = "الجلسة أقصر من ١٠ ثوانٍ ولم تُحفظ"
                    )
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildOngoingNotification(): Notification {
        val state = StudyTimerManager.timerState.value
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeTitle = when {
            state.mode == "POMODORO" && state.isBreakPhase -> "استراحة بومودورو (${state.currentRound}/${state.pomodoroTargetRounds})"
            state.mode == "POMODORO" -> "تركيز بومودورو (${state.currentRound}/${state.pomodoroTargetRounds})"
            state.mode == "COUNTDOWN" && state.isGoalReached -> "انتهى العداد التنازلي ⏳🎉"
            state.mode == "COUNTDOWN" -> "عداد تنازلي متبقي: ${state.formattedTime()} ⏳"
            state.isGoalReached -> "عداد تصاعدي (تحقق الهدف + وقت إضافي 🔥)"
            else -> "عداد تصاعدي نحو الهدف (${state.targetDurationSeconds / 60} د)"
        }
        val statusText = if (state.isPaused) "متوقف مؤقتًا" else "جاري الآن"

        return NotificationCompat.Builder(this, CHANNEL_STUDY_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${state.subjectName} • ${state.formattedTime()}")
            .setContentText("$modeTitle ($statusText)")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingOpen)
            .build()
    }

    private fun updateOngoingNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildOngoingNotification())
    }

    private fun ensureStudyNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_STUDY_TIMER,
                "عداد المذاكرة والتركيز",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار مستمر لعداد المذاكرة والبومودورو في الخلفية"
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_STUDY_TIMER = "channel_study_timer_ongoing"
        const val NOTIFICATION_ID = 2201

        const val ACTION_START = "com.example.core.study.ACTION_START"
        const val ACTION_TOGGLE_PAUSE = "com.example.core.study.ACTION_TOGGLE_PAUSE"
        const val ACTION_FINISH_SAVE = "com.example.core.study.ACTION_FINISH_SAVE"
        const val ACTION_CANCEL = "com.example.core.study.ACTION_CANCEL"
    }
}
