package com.example.core.protection

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemAlarmSound(
    val id: String,
    val title: String,
    val uriString: String,
    val isDefault: Boolean = false
)

/**
 * مدير أصوات المنبه الحقيقية من نظام أندرويد مباشرة:
 * 1) استخراج نغمات المنبه الرسمية الموجودة في جهاز المستخدم عبر RingtoneManager
 * 2) تجربة واستماع النغمات (Preview)
 * 3) تشغيل الرنين المتواصل بحجم صوت كامل وأقصى إلحاح حتى التحقق
 */
object AlarmSoundManager {

    private var mediaPlayer: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var previewStopRunnable: Runnable? = null

    private val _isPlayingPreview = MutableStateFlow<String?>(null)
    val isPlayingPreview: StateFlow<String?> = _isPlayingPreview.asStateFlow()

    /**
     * استخراج جميع نغمات المنبه الحقيقية المتوفرة على هاتف المستخدم
     */
    fun getAvailableSystemAlarmSounds(context: Context): List<SystemAlarmSound> {
        val soundList = mutableListOf<SystemAlarmSound>()

        val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        soundList.add(
            SystemAlarmSound(
                id = "system_default",
                title = "نغمة المنبه الافتراضية للجهاز",
                uriString = defaultAlarmUri?.toString() ?: "",
                isDefault = true
            )
        )

        soundList.add(
            SystemAlarmSound(
                id = "high_emergency_siren",
                title = "🚨 صفارة إنذار طوارئ قصوى (نغمة قوية)",
                uriString = "internal://emergency_siren",
                isDefault = false
            )
        )

        try {
            val ringtoneManager = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_ALARM)
            }
            val cursor = ringtoneManager.cursor
            if (cursor != null && cursor.moveToFirst()) {
                val titleIndex = RingtoneManager.TITLE_COLUMN_INDEX
                do {
                    val title = cursor.getString(titleIndex)
                    val uri = ringtoneManager.getRingtoneUri(cursor.position)
                    if (uri != null && uri.toString().isNotBlank()) {
                        val uriStr = uri.toString()
                        if (soundList.none { it.uriString == uriStr }) {
                            soundList.add(
                                SystemAlarmSound(
                                    id = "sys_alarm_${cursor.position}",
                                    title = title ?: "نغمة منبه نظام",
                                    uriString = uriStr
                                )
                            )
                        }
                    }
                } while (cursor.moveToNext())
            }
        } catch (_: Exception) {
        }

        // إذا كانت قائمة أصوات المنبه فارغة في بعض أجهزة المحاكاة، نجلب نغمات الرنين العادية كنغمات بديلة
        if (soundList.size <= 2) {
            try {
                val ringtoneManager = RingtoneManager(context).apply {
                    setType(RingtoneManager.TYPE_RINGTONE)
                }
                val cursor = ringtoneManager.cursor
                if (cursor != null && cursor.moveToFirst()) {
                    var count = 0
                    do {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = ringtoneManager.getRingtoneUri(cursor.position)
                        if (uri != null) {
                            soundList.add(
                                SystemAlarmSound(
                                    id = "sys_ringtone_${cursor.position}",
                                    title = title ?: "نغمة رنين جهاز",
                                    uriString = uri.toString()
                                )
                            )
                            count++
                        }
                    } while (cursor.moveToNext() && count < 8)
                }
            } catch (_: Exception) {
            }
        }

        return soundList
    }

    /**
     * تشغيل صوت المنبه بحجم صوت كامل وتكرار مستمر Loop حتى يتم إيقافه بالتحقق
     */
    fun playAlarmLooping(context: Context, soundUriString: String?) {
        stopAlarm()
        stopPreview()

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            val maxAlarmVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 15
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVol, 0)
        } catch (_: Exception) {
        }

        val uriToPlay: Uri? = when {
            soundUriString.isNullOrBlank() || soundUriString == "internal://emergency_siren" -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            else -> {
                try {
                    Uri.parse(soundUriString)
                } catch (_: Exception) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            }
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                if (uriToPlay != null) {
                    setDataSource(context, uriToPlay)
                }
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            // حل احتياطي في حال تعذر فتح الرابط
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (fallbackUri != null) {
                    val fallbackPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(context, fallbackUri)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = fallbackPlayer
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * إيقاف تشغيل رنين المنبه
     */
    fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    /**
     * تشغيل عينة تجريبية للنغمة لبضع ثوانٍ
     */
    fun previewSound(context: Context, uriString: String, durationMs: Long = 4000L) {
        stopPreview()

        val rawUri: Uri? = when {
            uriString.isBlank() || uriString == "internal://emergency_siren" -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            else -> {
                try {
                    Uri.parse(uriString)
                } catch (_: Exception) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            }
        }
        val uriToPlay = rawUri ?: return

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uriToPlay)
                isLooping = false
                prepare()
                start()
            }
            previewPlayer = player
            _isPlayingPreview.value = uriString

            previewStopRunnable?.let { mainHandler.removeCallbacks(it) }
            val runnable = Runnable {
                stopPreview()
            }
            previewStopRunnable = runnable
            mainHandler.postDelayed(runnable, durationMs)

            player.setOnCompletionListener {
                stopPreview()
            }
        } catch (_: Exception) {
            stopPreview()
        }
    }

    /**
     * إيقاف العينة التجريبية
     */
    fun stopPreview() {
        previewStopRunnable?.let { mainHandler.removeCallbacks(it) }
        previewStopRunnable = null
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {
        } finally {
            previewPlayer = null
            _isPlayingPreview.value = null
        }
    }
}
