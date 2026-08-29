package ir.k1adili.projectcam.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

data class WatermarkInfo(
    val projectName: String,
    val photographerName: String,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val capturedAt: LocalDateTime
)

object WatermarkUtil {

    /**
     * Draws a small, semi-transparent info bar across the bottom of [source] containing the
     * photographer name, GPS coordinates (or a "no location" note), and the Jalali date/time.
     * The project name is intentionally NOT included here (it's already shown in-app; burning it
     * into every photo was redundant and made the bar unnecessarily tall).
     *
     * Sizing is based on the SHORTER image dimension so the bar stays compact and proportionate
     * on both portrait and landscape photos - basing it on the longer dimension made the bar
     * enormous on landscape shots.
     *
     * Returns a NEW bitmap; [source] is not mutated.
     */
    fun applyWatermark(
        source: Bitmap,
        info: WatermarkInfo,
        titleTypeface: Typeface? = null,
        bodyTypeface: Typeface? = null
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val w = result.width
        val h = result.height
        val shortSide = min(w, h)

        val basePadding = shortSide * 0.018f
        val baseTextSize = shortSide * 0.017f

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSize
            typeface = bodyTypeface ?: Typeface.DEFAULT
            setShadowLayer(2f, 1f, 1f, Color.argb(200, 0, 0, 0))
            textAlign = Paint.Align.RIGHT
        }

        val locationLine = if (info.latitude != null && info.longitude != null) {
            val coords = String.format(
                Locale.US,
                "%.6f, %.6f",
                info.latitude,
                info.longitude
            )
            val accuracy = info.accuracyMeters?.takeIf { !it.isNaN() }
                ?.let { " (± ${it.roundToInt()} m)" }
                .orEmpty()
            "GPS: $coords$accuracy"
        } else {
            "بدون موقعیت مکانی (GPS در دسترس نبود)"
        }

        val dateLine = JalaliDateUtils.formatDateTimeNumeric(info.capturedAt)
        val photographerLine = "عکاس: ${info.photographerName.ifBlank { "-" }}"

        val bodyLines = listOf(photographerLine, locationLine, dateLine)

        val lineSpacing = baseTextSize * 0.35f
        val bodyHeight = bodyPaint.fontSpacing
        val barHeight = basePadding * 2 +
            bodyLines.size * bodyHeight +
            (bodyLines.size - 1) * lineSpacing

        val barTop = h - barHeight
        val barRect = RectF(0f, kotlin.math.max(0f, barTop), w.toFloat(), h.toFloat())
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 0, 0, 0)
        }
        canvas.drawRect(barRect, barPaint)

        var y = barRect.top + basePadding + bodyPaint.textSize
        for ((index, line) in bodyLines.withIndex()) {
            canvas.drawText(line, w - basePadding, y, bodyPaint)
            if (index != bodyLines.lastIndex) {
                y += bodyHeight + lineSpacing
            }
        }

        return result
    }
}
