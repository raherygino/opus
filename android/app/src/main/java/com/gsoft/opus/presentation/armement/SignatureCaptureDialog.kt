package com.gsoft.opus.presentation.armement

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gsoft.opus.data.signature.Stroke
import com.gsoft.opus.data.signature.StrokePoint

/**
 * Convert a list of strokes into an SVG string. The SVG uses a 400x200
 * viewBox and a black stroke — matching the desktop signature pad's
 * strokesToSvg() output so the server stores a consistent format.
 */
fun strokesToSvg(strokes: List<Stroke>): String {
    val paths = strokes.mapIndexed { i, stroke ->
        if (stroke.points.isEmpty()) return@mapIndexed ""
        val d = stroke.points.mapIndexed { j, p ->
            if (j == 0) "M ${p.x.format(2)} ${p.y.format(2)}"
            else "L ${p.x.format(2)} ${p.y.format(2)}"
        }.joinToString(" ")
        "<path d=\"$d\" stroke=\"#1a1a2e\" stroke-width=\"2\" fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />"
    }.filter { it.isNotEmpty() }
    return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 400 200\">" +
        "<rect width=\"400\" height=\"200\" fill=\"white\"/>" +
        paths.joinToString("") +
        "</svg>"
}

private fun Float.format(digits: Int): String = "%.${digits}f".format(this)

/**
 * A self-contained signature capture dialog with a drawing canvas. The
 * user draws their signature with their finger, then confirms to
 * produce an SVG string. Used by the Armement form to capture the
 * agent's signature after identity verification.
 */
@Composable
fun SignatureCaptureDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strokes = remember { mutableStateListOf<Stroke>() }
    val currentStroke = remember { mutableStateListOf<StrokePoint>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Signature de l'agent") },
        text = {
            Column {
                Text(
                    text = "L'agent doit apposer sa signature ci-dessous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 12.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke.clear()
                                    currentStroke.add(StrokePoint(offset.x, offset.y, System.currentTimeMillis()))
                                },
                                onDrag = { change, _ ->
                                    currentStroke.add(StrokePoint(change.position.x, change.position.y, System.currentTimeMillis()))
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes.add(Stroke(points = currentStroke.toList()))
                                        currentStroke.clear()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        strokes.forEach { stroke ->
                            if (stroke.points.size >= 2) {
                                val path = Path()
                                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (i in 1 until stroke.points.size) {
                                    path.lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                                drawPath(path, color = Color(0xFF1A1A2E), style = DrawStroke(width = 3f))
                            }
                        }
                        if (currentStroke.size >= 2) {
                            val path = Path()
                            path.moveTo(currentStroke[0].x, currentStroke[0].y)
                            for (i in 1 until currentStroke.size) {
                                path.lineTo(currentStroke[i].x, currentStroke[i].y)
                            }
                            drawPath(path, color = Color(0xFF1A1A2E), style = DrawStroke(width = 3f))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                    }) {
                        Icon(Icons.Filled.Undo, contentDescription = "Annuler le dernier trait")
                    }
                    IconButton(onClick = {
                        strokes.clear()
                        currentStroke.clear()
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Tout effacer")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(strokesToSvg(strokes.toList())) },
                enabled = strokes.isNotEmpty()
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Annuler")
            }
        }
    )
}
