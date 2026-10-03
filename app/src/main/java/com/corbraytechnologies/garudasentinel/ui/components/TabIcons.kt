package com.corbraytechnologies.garudasentinel.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Outline icons for the bottom bar, drawn with a 1.6dp stroke on a 24dp grid. Material's
 * outlined icons use a heavier stroke, so these are drawn here. The colour comes from the
 * Icon tint.
 */
object TabIcons {

    val Shield: ImageVector = outline("Shield") {
        moveTo(12f, 3f)
        lineTo(19f, 6f)
        verticalLineTo(11f)
        curveTo(19f, 15.5f, 16f, 19.2f, 12f, 21f)
        curveTo(8f, 19.2f, 5f, 15.5f, 5f, 11f)
        verticalLineTo(6f)
        close()
    }

    val Grid: ImageVector = outline("Grid") {
        square(4f, 4f)
        square(13.5f, 4f)
        square(4f, 13.5f)
        square(13.5f, 13.5f)
    }

    val Sliders: ImageVector = outline("Sliders") {
        slider(y = 7f, knobX = 9f)
        slider(y = 12f, knobX = 15f)
        slider(y = 17f, knobX = 7f)
    }

    val Help: ImageVector = outline("Help") {
        circle(12f, 12f, 9f)
        moveTo(9.6f, 9.6f)
        curveTo(9.6f, 8.2f, 10.7f, 7.3f, 12f, 7.3f)
        curveTo(13.3f, 7.3f, 14.4f, 8.2f, 14.4f, 9.5f)
        curveTo(14.4f, 10.6f, 13.6f, 11.1f, 12.9f, 11.6f)
        curveTo(12.3f, 12f, 12f, 12.4f, 12f, 13.2f)
        verticalLineTo(13.8f)
        // The dot: a very short line with a round cap.
        moveTo(12f, 16.7f)
        lineTo(12f, 16.72f)
    }

    private const val STROKE = 1.6f
    private const val KNOB_RADIUS = 2f

    private fun PathBuilder.square(left: Float, top: Float, size: Float = 6.5f) {
        moveTo(left, top)
        horizontalLineTo(left + size)
        verticalLineTo(top + size)
        horizontalLineTo(left)
        close()
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    /** A horizontal track from x 4 to 20 with a round knob; the track stops at the knob's edge. */
    private fun PathBuilder.slider(y: Float, knobX: Float) {
        moveTo(4f, y)
        horizontalLineTo(knobX - KNOB_RADIUS)
        moveTo(knobX + KNOB_RADIUS, y)
        horizontalLineTo(20f)
        circle(knobX, y, KNOB_RADIUS)
    }

    private fun outline(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()
}
