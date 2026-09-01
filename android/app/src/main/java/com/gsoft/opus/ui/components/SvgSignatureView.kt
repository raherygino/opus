package com.gsoft.opus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A parsed SVG path: a list of move-to / line-to points.
 * Only M and L commands are supported — that's all the signature pad
 * produces. Each pair is (x, y) in the SVG's coordinate space.
 */
private data class SvgPath(
    val points: List<Pair<Float, Float>>
)

/**
 * Parse an SVG string produced by the signature pad into a list of
 * [SvgPath] objects plus the viewBox dimensions. The expected format is:
 *
 *   <svg ... viewBox="0 0 W H">
 *     <path d="M x y L x y L x y ..." .../>
 *     <path d="M x y L x y ..." .../>
 *     ...
 *   </svg>
 *
 * Only M (moveTo) and L (lineTo) commands are emitted by the signature
 * pad, so we only need to handle those.
 */
private fun parseSvg(svg: String): Pair<List<SvgPath>, Pair<Float, Float>> {
    val paths = mutableListOf<SvgPath>()

    // Extract viewBox dimensions
    var width = 400f
    var height = 200f
    val viewBoxRegex = Regex("""viewBox\s*=\s*"\s*0\s+0\s+([\d.]+)\s+([\d.]+)\s*"""")
    viewBoxRegex.find(svg)?.let { m ->
        width = m.groupValues[1].toFloatOrNull() ?: width
        height = m.groupValues[2].toFloatOrNull() ?: height
    }
    // Also try width/height attributes
    val wRegex = Regex("""\bwidth\s*=\s*"?(\d+(?:\.\d+)?)"?""")
    val hRegex = Regex("""\bheight\s*=\s*"?(\d+(?:\.\d+)?)"?""")
    wRegex.find(svg)?.let { m ->
        m.groupValues[1].toFloatOrNull()?.let { if (it > 0) width = it }
    }
    hRegex.find(svg)?.let { m ->
        m.groupValues[1].toFloatOrNull()?.let { if (it > 0) height = it }
    }

    // Extract all <path d="..."> contents
    val pathRegex = Regex("""<path[^>]*\bd\s*=\s*"([^"]*)"[^>]*/?>""")
    for (match in pathRegex.findAll(svg)) {
        val d = match.groupValues[1].trim()
        val points = parsePathData(d)
        if (points.isNotEmpty()) {
            paths.add(SvgPath(points))
        }
    }

    return paths to (width to height)
}

/**
 * Parse the `d` attribute of an SVG <path> element. Supports M (moveTo)
 * and L (lineTo) commands with absolute coordinates. Each command may
 * be followed by multiple coordinate pairs (implicit repetition).
 */
private fun parsePathData(d: String): List<Pair<Float, Float>> {
    val points = mutableListOf<Pair<Float, Float>>()
    // Split into command groups: M x y x y... L x y x y...
    val tokens = d.split(Regex("\\s+|(?<=[\\d.])\\s*(?=[ML])")).filter { it.isNotBlank() }

    var i = 0
    var currentCmd = ' '
    while (i < tokens.size) {
        val tok = tokens[i]
        if (tok.equals("M", ignoreCase = true) || tok.equals("L", ignoreCase = true)) {
            currentCmd = tok[0]
            i++
            continue
        }
        // Parse x y pair
        if (i + 1 < tokens.size) {
            val x = tok.toFloatOrNull()
            val y = tokens[i + 1].toFloatOrNull()
            if (x != null && y != null) {
                points.add(x to y)
                i += 2
                // After the first pair of M, subsequent pairs are implicit L
                if (currentCmd == 'M') currentCmd = 'L'
                continue
            }
        }
        i++
    }
    return points
}

/**
 * Render an SVG signature string natively using Compose Canvas — no
 * WebView required. This works on devices without a WebView provider
 * (e.g. custom ROMs, non-GMS devices).
 *
 * The SVG must use the format produced by the signature pad:
 * `<svg viewBox="0 0 W H"><path d="M x y L x y..."/></svg>`
 *
 * @param svg   The SVG string to render.
 * @param modifier Layout modifier (defaults to fillMaxWidth, 120dp tall).
 * @param strokeColor Color of the signature strokes.
 * @param strokeWidth Width of the strokes in the SVG's coordinate space.
 */
@Composable
fun SvgSignatureView(
    svg: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .clip(RoundedCornerShape(8.dp)),
    strokeColor: Color = Color(0xFF1A1A2E),
    strokeWidth: Float = 2f
) {
    val (paths, viewBox) = remember(svg) { parseSvg(svg) }
    val (vbWidth, vbHeight) = viewBox

    Box(
        modifier = modifier.background(Color.White)
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            drawSignature(paths, vbWidth, vbHeight, strokeColor, strokeWidth)
        }
    }
}

/**
 * Draw the parsed SVG paths into the [DrawScope], scaling from the SVG
 * viewBox coordinate space to the canvas size.
 */
private fun DrawScope.drawSignature(
    paths: List<SvgPath>,
    vbWidth: Float,
    vbHeight: Float,
    color: Color,
    strokeWidth: Float
) {
    if (paths.isEmpty()) return
    val canvasW = size.width
    val canvasH = size.height
    if (vbWidth <= 0f || vbHeight <= 0f) return

    val scaleX = canvasW / vbWidth
    val scaleY = canvasH / vbHeight
    val scale = minOf(scaleX, scaleY)

    // Center the signature in the canvas
    val offsetX = (canvasW - vbWidth * scale) / 2f
    val offsetY = (canvasH - vbHeight * scale) / 2f

    val strokeW = strokeWidth * scale

    for (svgPath in paths) {
        if (svgPath.points.size < 2) continue
        val path = Path()
        val first = svgPath.points[0]
        path.moveTo(first.first * scale + offsetX, first.second * scale + offsetY)
        for (j in 1 until svgPath.points.size) {
            val p = svgPath.points[j]
            path.lineTo(p.first * scale + offsetX, p.second * scale + offsetY)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeW,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}
