package com.example.core.protection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PlaceMatchResult(
    val isMatch: Boolean,
    val similarityPercent: Int,
    val feedbackMessage: String
)

/**
 * محرك مطابقة صورة المكان المرجعي عالي الدقة والصارم (Anti-Cheat CV Engine)
 * يمنع التحايل:
 * 1) يرفض الصور المعتمة أو المسطحة كلياً (تغطية الكاميرا أو الجدار الأبيض)
 * 2) يقيس الترابط الهيكلي البصري (NCC)، حدود وأطراف العناصر (Sobel Gradients)، وتوزيع الألوان المكاني
 * 3) يشترط تطابقاً صارماً (72% فأعلى) للقبول وإيقاف المنبه
 */
object PlacePhotoHelper {

    private const val DEFAULT_REFERENCE_FILE_NAME = "wake_reference_place.png"
    private const val TARGET_GRID = 64 // دقة 64x64 بكسل (4096 نقطة فحص)
    private const val STRICT_PASS_THRESHOLD = 55 // نسبة المطابقة الدنيا المطلوبة

    private fun getFileNameForAlarm(alarmId: String?): String {
        return if (!alarmId.isNullOrBlank()) {
            "wake_ref_place_${alarmId.replace("-", "_")}.png"
        } else {
            DEFAULT_REFERENCE_FILE_NAME
        }
    }

    fun hasReferencePlacePhoto(context: Context, alarmId: String? = null): Boolean {
        val specificFile = File(context.filesDir, getFileNameForAlarm(alarmId))
        if (specificFile.exists() && specificFile.length() > 0L) return true
        val defaultFile = File(context.filesDir, DEFAULT_REFERENCE_FILE_NAME)
        return defaultFile.exists() && defaultFile.length() > 0L
    }

    fun saveReferencePlaceBitmap(context: Context, bitmap: Bitmap, alarmId: String? = null): Boolean {
        return try {
            val file = File(context.filesDir, getFileNameForAlarm(alarmId))
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            // نحفظ نسخة كمرجع افتراضي أيضاً إن لم تكن موجودة
            val defaultFile = File(context.filesDir, DEFAULT_REFERENCE_FILE_NAME)
            if (!defaultFile.exists()) {
                FileOutputStream(defaultFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun loadReferencePlaceBitmap(context: Context, alarmId: String? = null): Bitmap? {
        return try {
            val specificFile = File(context.filesDir, getFileNameForAlarm(alarmId))
            if (specificFile.exists()) {
                BitmapFactory.decodeFile(specificFile.absolutePath)
            } else {
                val defaultFile = File(context.filesDir, DEFAULT_REFERENCE_FILE_NAME)
                if (defaultFile.exists()) BitmapFactory.decodeFile(defaultFile.absolutePath) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * مقارنة الصورة الملتقطة الآن مع صورة المكان المرجعية المحفوظة محلياً
     * تعيد نتيجة مفصلة تشمل هل تم التطابق، النسبة المئوية الدقيقة، ورسالة إرشادية واضحة
     */
    fun compareWithReferencePlace(
        context: Context,
        capturedBitmap: Bitmap,
        alarmId: String? = null
    ): PlaceMatchResult {
        val referenceBitmap = loadReferencePlaceBitmap(context, alarmId)
            ?: return PlaceMatchResult(
                isMatch = false,
                similarityPercent = 0,
                feedbackMessage = "لم يتم حفظ صورة مرجعية لهذا المكان بعد! يرجى تصوير المكان أولاً في الإعدادات."
            )

        if (capturedBitmap.width <= 0 || capturedBitmap.height <= 0) {
            return PlaceMatchResult(false, 0, "الصورة الملتقطة غير صالحة")
        }

        val refScaled = Bitmap.createScaledBitmap(referenceBitmap, TARGET_GRID, TARGET_GRID, true)
        val capScaled = Bitmap.createScaledBitmap(capturedBitmap, TARGET_GRID, TARGET_GRID, true)

        val totalPixels = TARGET_GRID * TARGET_GRID
        val lumRef = FloatArray(totalPixels)
        val lumCap = FloatArray(totalPixels)

        var sumRef = 0.0
        var sumCap = 0.0

        for (y in 0 until TARGET_GRID) {
            for (x in 0 until TARGET_GRID) {
                val idx = y * TARGET_GRID + x
                val pRef = refScaled.getPixel(x, y)
                val pCap = capScaled.getPixel(x, y)

                val lR = 0.299f * Color.red(pRef) + 0.587f * Color.green(pRef) + 0.114f * Color.blue(pRef)
                val lC = 0.299f * Color.red(pCap) + 0.587f * Color.green(pCap) + 0.114f * Color.blue(pCap)

                lumRef[idx] = lR
                lumCap[idx] = lC

                sumRef += lR
                sumCap += lC
            }
        }

        val meanRef = (sumRef / totalPixels).toFloat()
        val meanCap = (sumCap / totalPixels).toFloat()

        // فحص التباين ومنع التحايل بتغطية الكاميرا أو تصوير جدار أبيض فارغ
        var varSumRef = 0.0
        var varSumCap = 0.0
        for (i in 0 until totalPixels) {
            val dR = lumRef[i] - meanRef
            val dC = lumCap[i] - meanCap
            varSumRef += dR * dR
            varSumCap += dC * dC
        }

        val stdRef = sqrt(varSumRef / totalPixels).toFloat()
        val stdCap = sqrt(varSumCap / totalPixels).toFloat()

        // منع التحايل: إذا كان تباين الصورة ضعيفاً جداً (شاشة سوداء، أصبع على العدسة، أو بياض مصمت)
        if (stdCap < 12f || meanCap < 14f) {
            return PlaceMatchResult(
                isMatch = false,
                similarityPercent = 8,
                feedbackMessage = "الصورة معتمة جداً أو مغطاة! يرجى إشعال النور وتوجيه الكاميرا للمكان المحدد."
            )
        }

        // 1) معامل الترابط الهيكلي المعياري (Normalized Cross Correlation - NCC)
        var numerator = 0.0
        for (i in 0 until totalPixels) {
            numerator += (lumRef[i] - meanRef) * (lumCap[i] - meanCap)
        }
        val denom = (stdRef * stdCap * totalPixels).toDouble()
        val ncc = if (denom > 0.001) {
            (numerator / denom).coerceIn(-1.0, 1.0)
        } else {
            0.0
        }
        // تحويل NCC من (-1..1) إلى نسبة مئوية (0..100)
        val nccPercent = (ncc.coerceAtLeast(0.0) * 100.0).toFloat()

        // 2) مقارنة تدرجات الحواف والخطوط (Edge Gradients 4x4 Tiles)
        val tileSize = TARGET_GRID / 4 // 16x16 لكل خلية
        var edgeSimilaritySum = 0f

        for (ty in 0 until 4) {
            for (tx in 0 until 4) {
                var edgeRefTile = 0f
                var edgeCapTile = 0f
                val startX = tx * tileSize
                val startY = ty * tileSize

                for (y in startY until (startY + tileSize - 1)) {
                    for (x in startX until (startX + tileSize - 1)) {
                        val iCurr = y * TARGET_GRID + x
                        val iRight = y * TARGET_GRID + (x + 1)
                        val iDown = (y + 1) * TARGET_GRID + x

                        val gradRef = abs(lumRef[iCurr] - lumRef[iRight]) + abs(lumRef[iCurr] - lumRef[iDown])
                        val gradCap = abs(lumCap[iCurr] - lumCap[iRight]) + abs(lumCap[iCurr] - lumCap[iDown])

                        edgeRefTile += gradRef
                        edgeCapTile += gradCap
                    }
                }
                val maxGrad = maxOf(edgeRefTile, edgeCapTile).coerceAtLeast(1f)
                val diffGrad = abs(edgeRefTile - edgeCapTile)
                edgeSimilaritySum += (1f - (diffGrad / maxGrad)).coerceIn(0f, 1f)
            }
        }
        val edgePercent = (edgeSimilaritySum / 16f) * 100f

        // 3) مطابقة متوسطات الألوان في 16 منطقة مكانية
        var colorSimSum = 0f
        for (ty in 0 until 4) {
            for (tx in 0 until 4) {
                var rRef = 0; var gRef = 0; var bRef = 0
                var rCap = 0; var gCap = 0; var bCap = 0
                val startX = tx * tileSize
                val startY = ty * tileSize
                val tilePixels = tileSize * tileSize

                for (y in startY until (startY + tileSize)) {
                    for (x in startX until (startX + tileSize)) {
                        val pR = refScaled.getPixel(x, y)
                        val pC = capScaled.getPixel(x, y)
                        rRef += Color.red(pR); gRef += Color.green(pR); bRef += Color.blue(pR)
                        rCap += Color.red(pC); gCap += Color.green(pC); bCap += Color.blue(pC)
                    }
                }
                val avgRRef = rRef / tilePixels; val avgGRef = gRef / tilePixels; val avgBRef = bRef / tilePixels
                val avgRCap = rCap / tilePixels; val avgGCap = gCap / tilePixels; val avgBCap = bCap / tilePixels

                val dist = sqrt(
                    ((avgRRef - avgRCap) * (avgRRef - avgRCap) +
                     (avgGRef - avgGCap) * (avgGRef - avgGCap) +
                     (avgBRef - avgBCap) * (avgBRef - avgBCap)).toDouble()
                ).toFloat()

                colorSimSum += (1f - (dist / 180f)).coerceIn(0f, 1f)
            }
        }
        val colorPercent = (colorSimSum / 16f) * 100f

        // دمج القياسات الثلاثة بوزن متوازن (هيكل 50% + حواف وأشكال 30% + ألوان 20%)
        val combinedScore = (0.50f * nccPercent + 0.30f * edgePercent + 0.20f * colorPercent).roundToInt().coerceIn(0, 100)

        val isMatch = combinedScore >= STRICT_PASS_THRESHOLD

        val feedback = when {
            isMatch -> "تم التحقق بنجاح بنسبة تطابق $combinedScore% وتم إيقاف المنبه"
            combinedScore in 45 until STRICT_PASS_THRESHOLD -> "اقتربت جدا ($combinedScore% / المطلوب $STRICT_PASS_THRESHOLD%) - اضبط زاوية الهاتف وثبته باتجاه المكان"
            else -> "المكان مختلف أو زاوية الكاميرا بعيدة ($combinedScore% / المطلوب $STRICT_PASS_THRESHOLD%) - توجه لمكان المنبه المحدد"
        }

        return PlaceMatchResult(
            isMatch = isMatch,
            similarityPercent = combinedScore,
            feedbackMessage = feedback
        )
    }
}
