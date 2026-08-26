package ir.k1adili.projectcam.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.max
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
     * Draws a semi-transparent bar across the bottom of [source] containing the project name,
     * photographer, GPS coordinates (or "بدون موقعیت مکانی" if unavailable), and the Jalali
     * date/time. Returns a NEW bitmap; [source] is not mutated (callers should recycle it
     * themselves once no longer needed - not automatically recycled here since callers may
     * still be showing a preview from it).
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

        val basePadding = w * 0.03f
        val baseTextSize = max(w, h) * 0.026f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSize * 1.15f
            typeface = titleTypeface ?: Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 1f, 1f, Color.argb(200, 0, 0, 0))
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSize
            typeface = bodyTypeface ?: Typeface.DEFAULT
            setShadowLayer(3f, 1f, 1f, Color.argb(200, 0, 0, 0))
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

        val lines = listOf(info.projectName)
        val bodyLines = listOf(photographerLine, locationLine, dateLine)

        val lineSpacing = baseTextSize * 0.45f
        val titleHeight = titlePaint.fontSpacing
        val bodyHeight = bodyPaint.fontSpacing
        val barHeight = basePadding * 2 +
            titleHeight +
            lineSpacing +
            bodyLines.size * bodyHeight +
            (bodyLines.size - 1) * (lineSpacing * 0.5f)

        val barTop = h - barHeight
        val barRect = RectF(0f, max(0f, barTop), w.toFloat(), h.toFloat())
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 0, 0, 0)
        }
        canvas.drawRect(barRect, barPaint)

        var y = barRect.top + basePadding + titlePaint.textSize
        canvas.drawText(lines[0], w - basePadding, y, titlePaint)

        y += lineSpacing + bodyPaint.textSize
        for ((index, line) in bodyLines.withIndex()) {
            canvas.drawText(line, w - basePadding, y, bodyPaint)
            if (index != bodyLines.lastIndex) {
                y += bodyHeight + lineSpacing * 0.5f
            }
        }

        return result
    }
}
