package com.example.core.study

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.SubjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class StudyTimerState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val subjectId: String = "general_timer",
    val subjectName: String = "عداد التركيز",
    val subjectColorHex: String = "#10B981",
    val linkedHabitId: String? = null,
    val linkedType: String = "NONE", // "HABIT", "TASK", "NONE"
    val linkedId: String? = null,
    val startTimeMillis: Long? = null,
    val pauseTimeMillis: Long? = null,
    val resumeTimeMillis: Long? = null,
    val completionTimeMillis: Long? = null,
    val mode: String = "COUNT_UP", // "COUNT_UP" (مؤقت تصاعدي) أو "COUNTDOWN" (عداد تنازلي) أو "POMODORO" (بومودورو مخصص)
    val targetDurationSeconds: Int = 30 * 60, // الهدف المطلوب بالثواني
    val elapsedSeconds: Int = 0, // إجمالي ثواني التركيز الفعلية المتراكمة
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val pomodoroTargetRounds: Int = 2,
    val currentRound: Int = 1,
    val isBreakPhase: Boolean = false,
    val phaseElapsedSeconds: Int = 0,
    val goalReachedNotified: Boolean = false,
    val lastCompletedMessage: String? = null
) {
    val isGoalReached: Boolean
        get() = targetDurationSeconds > 0 && elapsedSeconds >= targetDurationSeconds

    val extraSecondsBeyondGoal: Int
        get() = (elapsedSeconds - targetDurationSeconds).coerceAtLeast(0)

    val displaySeconds: Int
        get() = when (mode) {
            "POMODORO" -> {
                val phaseTotal = (if (isBreakPhase) pomodoroBreakMinutes else pomodoroFocusMinutes) * 60
                val effectiveElapsed = if (phaseElapsedSeconds > 0) phaseElapsedSeconds else elapsedSeconds
                (phaseTotal - effectiveElapsed).coerceAtLeast(0)
            }
            "COUNTDOWN" -> {
                (targetDurationSeconds - elapsedSeconds).coerceAtLeast(0)
            }
            else -> {
                elapsedSeconds
            }
        }

    val progressFraction: Float
        get() = when (mode) {
            "COUNTDOWN" -> {
                if (targetDurationSeconds > 0) {
                    (displaySeconds.toFloat() / targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
            else -> {
                if (targetDurationSeconds > 0) {
                    (elapsedSeconds.toFloat() / targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }

    fun formattedTime(): String {
        val total = displaySeconds
        val hrs = total / 3600
        val mins = (total % 3600) / 60
        val secs = total % 60
        val raw = if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
        return UmmAlQuraCalculator.toArabicDigits(raw)
    }

    fun formattedTotalFocusTime(): String {
        val hrs = elapsedSeconds / 3600
        val mins = (elapsedSeconds % 3600) / 60
        val secs = elapsedSeconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formattedStartTime(): String {
        val t = startTimeMillis ?: return ""
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", Locale("ar"))
        return sdf.format(java.util.Date(t))
    }

    fun formattedPauseTime(): String {
        val t = pauseTimeMillis ?: return ""
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", Locale("ar"))
        return sdf.format(java.util.Date(t))
    }

    fun formattedResumeTime(): String {
        val t = resumeTimeMillis ?: return ""
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", Locale("ar"))
        return sdf.format(java.util.Date(t))
    }

    fun formattedCompletionTime(): String {
        val t = completionTimeMillis ?: return ""
        val sdf = java.text.SimpleDateFormat("hh:mm:ss a", Locale("ar"))
        return sdf.format(java.util.Date(t))
    }
}

data class LastLinkInfo(
    val type: String, // "HABIT", "TASK"
    val id: String,
    val title: String,
    val colorHex: String = "#10B981"
)

object StudyTimerManager {

    private val _timerState = MutableStateFlow(StudyTimerState())
    val timerState: StateFlow<StudyTimerState> = _timerState.asStateFlow()

    fun updateSelection(
        subject: SubjectEntity,
        mode: String = subject.defaultTimerType.ifBlank { "COUNT_UP" },
        targetMinutes: Int = subject.dailyGoalMinutes.coerceAtLeast(5),
        linkedHabitId: String? = null
    ) {
        if (!_timerState.value.isActive) {
            val normalizedMode = if (mode == "POMODORO") "POMODORO" else "COUNT_UP"
            val focusMin = subject.pomodoroFocusMin.coerceAtLeast(5)
            val breakMin = subject.pomodoroBreakMin.coerceAtLeast(1)
            val rounds = subject.pomodoroRounds.coerceAtLeast(1)
            val effectiveTargetSec = if (normalizedMode == "POMODORO") {
                focusMin * rounds * 60
            } else {
                targetMinutes.coerceAtLeast(1) * 60
            }
            _timerState.value = _timerState.value.copy(
                subjectId = subject.id,
                subjectName = subject.name,
                subjectColorHex = subject.colorHex,
                linkedHabitId = linkedHabitId,
                mode = normalizedMode,
                targetDurationSeconds = effectiveTargetSec,
                pomodoroFocusMinutes = focusMin,
                pomodoroBreakMinutes = breakMin,
                pomodoroTargetRounds = rounds,
                currentRound = 1,
                isBreakPhase = false,
                phaseElapsedSeconds = 0,
                elapsedSeconds = 0,
                goalReachedNotified = false
            )
        }
    }

    fun setModeAndMinutes(mode: String, targetMinutes: Int) {
        if (!_timerState.value.isActive) {
            val normalizedMode = when (mode) {
                "POMODORO" -> "POMODORO"
                "COUNTDOWN" -> "COUNTDOWN"
                else -> "COUNT_UP"
            }
            val current = _timerState.value
            val effectiveTargetSec = if (normalizedMode == "POMODORO") {
                current.pomodoroFocusMinutes * current.pomodoroTargetRounds * 60
            } else {
                targetMinutes.coerceAtLeast(1) * 60
            }
            _timerState.value = current.copy(
                mode = normalizedMode,
                targetDurationSeconds = effectiveTargetSec,
                elapsedSeconds = 0,
                phaseElapsedSeconds = 0,
                currentRound = 1,
                isBreakPhase = false,
                goalReachedNotified = false
            )
        }
    }

    fun setCountdownDuration(seconds: Int) {
        if (!_timerState.value.isActive) {
            val sec = seconds.coerceAtLeast(10)
            _timerState.value = _timerState.value.copy(
                mode = "COUNTDOWN",
                targetDurationSeconds = sec,
                elapsedSeconds = 0,
                phaseElapsedSeconds = 0,
                goalReachedNotified = false
            )
        }
    }

    fun configureCustomPomodoro(
        focusMinutes: Int,
        breakMinutes: Int,
        targetRounds: Int
    ) {
        if (!_timerState.value.isActive) {
            val f = focusMinutes.coerceIn(5, 180)
            val b = breakMinutes.coerceIn(1, 60)
            val r = targetRounds.coerceIn(1, 12)
            _timerState.value = _timerState.value.copy(
                mode = "POMODORO",
                pomodoroFocusMinutes = f,
                pomodoroBreakMinutes = b,
                pomodoroTargetRounds = r,
                targetDurationSeconds = f * r * 60,
                currentRound = 1,
                isBreakPhase = false,
                phaseElapsedSeconds = 0,
                elapsedSeconds = 0,
                goalReachedNotified = false
            )
        }
    }

    fun startTimer(
        context: Context,
        subject: SubjectEntity,
        mode: String,
        targetMinutes: Int,
        linkedHabitId: String? = null,
        pomodoroFocusMin: Int = _timerState.value.pomodoroFocusMinutes,
        pomodoroBreakMin: Int = _timerState.value.pomodoroBreakMinutes,
        pomodoroRounds: Int = _timerState.value.pomodoroTargetRounds
    ) {
        val normalizedMode = when (mode) {
            "POMODORO" -> "POMODORO"
            "COUNTDOWN" -> "COUNTDOWN"
            else -> "COUNT_UP"
        }
        val effectiveTargetSec = if (normalizedMode == "POMODORO") {
            pomodoroFocusMin.coerceAtLeast(5) * pomodoroRounds.coerceAtLeast(1) * 60
        } else {
            targetMinutes.coerceAtLeast(1) * 60
        }

        val now = System.currentTimeMillis()
        _timerState.value = StudyTimerState(
            isActive = true,
            isPaused = false,
            subjectId = subject.id,
            subjectName = subject.name,
            subjectColorHex = subject.colorHex,
            linkedHabitId = linkedHabitId,
            linkedType = if (linkedHabitId != null) "HABIT" else "NONE",
            linkedId = linkedHabitId,
            startTimeMillis = now,
            pauseTimeMillis = null,
            completionTimeMillis = null,
            mode = normalizedMode,
            targetDurationSeconds = effectiveTargetSec,
            elapsedSeconds = 0,
            pomodoroFocusMinutes = pomodoroFocusMin.coerceAtLeast(5),
            pomodoroBreakMinutes = pomodoroBreakMin.coerceAtLeast(1),
            pomodoroTargetRounds = pomodoroRounds.coerceAtLeast(1),
            currentRound = 1,
            isBreakPhase = false,
            phaseElapsedSeconds = 0,
            goalReachedNotified = false,
            lastCompletedMessage = null
        )
        val intent = Intent(context, StudyTimerForegroundService::class.java).apply {
            action = StudyTimerForegroundService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            context.startService(intent)
        }
    }

    fun getLastLink(context: Context): LastLinkInfo? {
        val sp = context.getSharedPreferences("counter_prefs", Context.MODE_PRIVATE)
        val type = sp.getString("last_link_type", null) ?: return null
        val id = sp.getString("last_link_id", null) ?: return null
        val title = sp.getString("last_link_title", null) ?: return null
        val color = sp.getString("last_link_color", "#10B981") ?: "#10B981"
        return LastLinkInfo(type, id, title, color)
    }

    fun saveLastLink(context: Context, type: String, id: String, title: String, colorHex: String) {
        val sp = context.getSharedPreferences("counter_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("last_link_type", type)
            .putString("last_link_id", id)
            .putString("last_link_title", title)
            .putString("last_link_color", colorHex)
            .apply()
    }

    fun clearLastLink(context: Context) {
        val sp = context.getSharedPreferences("counter_prefs", Context.MODE_PRIVATE)
        sp.edit().clear().apply()
    }

    fun updateTargetSelection(
        context: Context,
        targetType: String, // "HABIT", "TASK", "NONE"
        targetId: String?,
        targetTitle: String,
        targetColorHex: String = "#10B981"
    ) {
        if (!_timerState.value.isActive) {
            _timerState.value = _timerState.value.copy(
                subjectId = targetId ?: "general_timer",
                subjectName = targetTitle,
                subjectColorHex = targetColorHex,
                linkedType = targetType,
                linkedId = targetId,
                linkedHabitId = if (targetType == "HABIT") targetId else null
            )
            if (targetType != "NONE" && !targetId.isNullOrBlank()) {
                saveLastLink(context, targetType, targetId, targetTitle, targetColorHex)
            }
        }
    }

    fun unlinkTarget() {
        if (!_timerState.value.isActive) {
            _timerState.value = _timerState.value.copy(
                subjectId = "general_timer",
                subjectName = "عداد التركيز",
                subjectColorHex = "#10B981",
                linkedType = "NONE",
                linkedId = null,
                linkedHabitId = null
            )
        }
    }

    fun startTimerForTarget(
        context: Context,
        targetType: String, // "HABIT", "TASK", "NONE"
        targetId: String?,
        targetTitle: String,
        targetColorHex: String = "#10B981",
        mode: String = "COUNT_UP",
        targetMinutes: Int = 30,
        pomodoroFocusMin: Int = 25,
        pomodoroBreakMin: Int = 5,
        pomodoroRounds: Int = 2
    ) {
        val normalizedMode = when (mode) {
            "POMODORO" -> "POMODORO"
            "COUNTDOWN" -> "COUNTDOWN"
            else -> "COUNT_UP"
        }
        val effectiveTargetSec = if (normalizedMode == "POMODORO") {
            pomodoroFocusMin.coerceAtLeast(5) * pomodoroRounds.coerceAtLeast(1) * 60
        } else {
            targetMinutes.coerceAtLeast(1) * 60
        }
        val now = System.currentTimeMillis()

        if (targetType != "NONE" && !targetId.isNullOrBlank()) {
            saveLastLink(context, targetType, targetId, targetTitle, targetColorHex)
        }

        _timerState.value = StudyTimerState(
            isActive = true,
            isPaused = false,
            subjectId = targetId ?: "general_timer",
            subjectName = targetTitle,
            subjectColorHex = targetColorHex,
            linkedHabitId = if (targetType == "HABIT") targetId else null,
            linkedType = targetType,
            linkedId = targetId,
            startTimeMillis = now,
            pauseTimeMillis = null,
            resumeTimeMillis = null,
            completionTimeMillis = null,
            mode = normalizedMode,
            targetDurationSeconds = effectiveTargetSec,
            elapsedSeconds = 0,
            pomodoroFocusMinutes = pomodoroFocusMin.coerceAtLeast(5),
            pomodoroBreakMinutes = pomodoroBreakMin.coerceAtLeast(1),
            pomodoroTargetRounds = pomodoroRounds.coerceAtLeast(1),
            currentRound = 1,
            isBreakPhase = false,
            phaseElapsedSeconds = 0,
            goalReachedNotified = false,
            lastCompletedMessage = null
        )
        val intent = Intent(context, StudyTimerForegroundService::class.java).apply {
            action = StudyTimerForegroundService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            context.startService(intent)
        }
    }

    fun togglePauseResume(context: Context) {
        if (!_timerState.value.isActive) return
        val intent = Intent(context, StudyTimerForegroundService::class.java).apply {
            action = StudyTimerForegroundService.ACTION_TOGGLE_PAUSE
        }
        context.startService(intent)
    }

    fun finishAndSaveSession(context: Context) {
        if (!_timerState.value.isActive) return
        val intent = Intent(context, StudyTimerForegroundService::class.java).apply {
            action = StudyTimerForegroundService.ACTION_FINISH_SAVE
        }
        context.startService(intent)
    }

    fun cancelTimer(context: Context) {
        val intent = Intent(context, StudyTimerForegroundService::class.java).apply {
            action = StudyTimerForegroundService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    internal fun internalUpdate(transform: (StudyTimerState) -> StudyTimerState) {
        _timerState.value = transform(_timerState.value)
    }
}
