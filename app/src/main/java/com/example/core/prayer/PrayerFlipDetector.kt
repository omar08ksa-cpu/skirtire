package com.example.core.prayer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * مستشعر قلب الجهاز (Flip to Snooze / Silence Azan)
 * يقوم بكتم صوت الأذان أو التنبيه فور قلب شاشة الهاتف للأسفل
 */
class PrayerFlipDetector(
    context: Context,
    private val onFlippedFaceDown: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var wasFaceUp = false
    private var isListening = false

    fun startListening() {
        if (isListening || accelerometer == null || sensorManager == null) return
        wasFaceUp = false
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        isListening = true
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val z = event.values[2] // Z-axis: موجب عندما تكون الشاشة للأعلى، سالب عندما تكون للأسفل
        val x = abs(event.values[0])
        val y = abs(event.values[1])

        // إذا كان الجهاز مستوياً وشاشته للأعلى
        if (z > 6.0f && x < 6.0f && y < 6.0f) {
            wasFaceUp = true
        }

        // إذا انقلب الجهاز وأصبحت الشاشة متجهة تماماً للأسفل
        if (wasFaceUp && z < -6.0f) {
            wasFaceUp = false
            stopListening()
            onFlippedFaceDown.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
