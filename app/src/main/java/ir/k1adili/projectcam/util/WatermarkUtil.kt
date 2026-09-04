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
import kotlin.math.min
import kotlin.math.roundToInt

data class WatermarkInfo(
    val projectName: String,
    val photographerName: String,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val headingDegrees: Float?,
    val capturedAt: LocalDateTime
)

object WatermarkUtil {

    /**
     * Draws a small, rounded, semi-transparent info chip near the bottom-right of [source]
     * containing the project/album name, photographer, GPS coordinates (or a "no location" note),
     * and the Jalali date/time.
     *
     * The chip hugs its own text content (does not span the full photo width/height like a bar) so
     * it covers as little of the photo as possible. Sizing is based on the SHORTER image dimension
     * so it stays proportionate on both portrait and landscape photos.
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

        val outerMargin = shortSide * 0.02f
        val innerPadding = shortSide * 0.016f
        val baseTextSize = shortSide * 0.017f
        val cornerRadius = shortSide * 0.02f
        val maxChipWidth = w * 0.82f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSize * 1.1f
            typeface = titleTypeface ?: Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 1f, 1f, Color.argb(180, 0, 0, 0))
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseTextSize
            typeface = bodyTypeface ?: Typeface.DEFAULT
            setShadowLayer(2f, 1f, 1f, Color.argb(180, 0, 0, 0))
            textAlign = Paint.Align.RIGHT
        }

        val locationLine = if (info.latitude != null && info.longitude != null) {
            val coords = String.format(Locale.US, "%.6f, %.6f", info.latitude, info.longitude)
            val accuracy = info.accuracyMeters?.takeIf { !it.isNaN() }
                ?.let { " (± ${it.roundToInt()} m)" }
                .orEmpty()
            val direction = info.headingDegrees?.takeIf { !it.isNaN() }
                ?.let { " - ${CompassHelper.directionLabel(it)}" }
                .orEmpty()
            "GPS: $coords$accuracy$direction"
        } else {
            "بدون موقعیت مکانی"
        }
        val dateLine = JalaliDateUtils.formatDateTimeNumeric(info.capturedAt)
        val photographerLine = "عکاس: ${info.photographerName.ifBlank { "-" }}"

        // (paint, line, isTitle) - shrink any single line that would blow out the chip width
        // instead of letting it overflow the photo edge.
        data class Line(val paint: Paint, val text: String)

        val rawLines = buildList {
            if (info.projectName.isNotBlank()) add(Line(titlePaint, info.projectName))
            add(Line(bodyPaint, photographerLine))
            add(Line(bodyPaint, locationLine))
            add(Line(bodyPaint, dateLine))
        }

        val maxTextWidth = maxChipWidth - innerPadding * 2
        val lines = rawLines.map { line ->
            val measured = line.paint.measureText(line.text)
            if (measured > maxTextWidth && measured > 0f) {
                val scaledPaint = Paint(line.paint).apply {
                    textSize = line.paint.textSize * (maxTextWidth / measured)
                }
                Line(scaledPaint, line.text)
            } else {
                line
            }
        }

        val lineSpacing = baseTextSize * 0.4f
        val chipContentWidth = lines.maxOf { it.paint.measureText(it.text) }
        val chipWidth = min(chipContentWidth + innerPadding * 2, maxChipWidth)
        val chipHeight = innerPadding * 2 +
            lines.sumOf { it.paint.fontSpacing.toDouble() }.toFloat() +
            (lines.size - 1) * lineSpacing

        val chipRight = w - outerMargin
        val chipBottom = h - outerMargin
        val chipRect = RectF(
            max(0f, chipRight - chipWidth),
            max(0f, chipBottom - chipHeight),
            chipRight,
            chipBottom
        )
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 0, 0, 0)
        }
        canvas.drawRoundRect(chipRect, cornerRadius, cornerRadius, chipPaint)

        var y = chipRect.top + innerPadding - lines.first().paint.fontMetrics.top
        for ((index, line) in lines.withIndex()) {
            canvas.drawText(line.text, chipRect.right - innerPadding, y, line.paint)
            if (index != lines.lastIndex) {
                y += line.paint.fontSpacing + lineSpacing
            }
        }

        return result
    }
}
