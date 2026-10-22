package com.studiopaint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PaintStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val opacity: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioPaintApp() }
    }
}

@Composable
fun StudioPaintApp() {
    var strokes by remember { mutableStateOf(listOf<PaintStroke>()) }
    var redoStrokes by remember { mutableStateOf(listOf<PaintStroke>()) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var brushSize by remember { mutableFloatStateOf(8f) }
    var opacity by remember { mutableFloatStateOf(1f) }
    var eraser by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }

    val colors = listOf(
        Color.Black, Color.White, Color.Red, Color(0xFFFF9800),
        Color.Yellow, Color.Green, Color.Cyan, Color.Blue,
        Color(0xFF673AB7), Color(0xFFE91E63), Color(0xFF795548)
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF171923))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "StudioPaint",
                        color = Color.White,
                        fontSize = 25.sp
                    )
                    Text(
                        "ESTÚDIO DE DESENHO",
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp
                    )
                }
                Text(
                    "ZOOM ${ (zoom * 100).toInt() }%",
                    color = Color(0xFFB9A5FF),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton("Pincel", !eraser) {
                    eraser = false
                }
                ToolButton("Borracha", eraser) {
                    eraser = true
                }
                ToolButton("Desfazer", false) {
                    if (strokes.isNotEmpty()) {
                        redoStrokes = redoStrokes + strokes.last()
                        strokes = strokes.dropLast(1)
                    }
                }
                ToolButton("Refazer", false) {
                    if (redoStrokes.isNotEmpty()) {
                        strokes = strokes + redoStrokes.last()
                        redoStrokes = redoStrokes.dropLast(1)
                    }
                }
                ToolButton("Limpar", false) {
                    strokes = emptyList()
                    redoStrokes = emptyList()
                    currentPoints = emptyList()
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("CORES", color = Color(0xFFB9A5FF), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color, CircleShape)
                            .then(
                                if (selectedColor == color) {
                                    Modifier.border(
                                        3.dp,
                                        Color(0xFFB9A5FF),
                                        CircleShape
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize(),
                            color = Color.Transparent,
                            onClick = {
                                selectedColor = color
                                eraser = false
                            },
                            shape = CircleShape
                        ) {}
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Tamanho do pincel: ${brushSize.toInt()}",
                color = Color.White,
                fontSize = 13.sp
            )
            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 2f..40f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFB9A5FF),
                    activeTrackColor = Color(0xFF8B5CF6)
                )
            )

            Text(
                "Opacidade: ${(opacity * 100).toInt()}%",
                color = Color.White,
                fontSize = 13.sp
            )
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFB9A5FF),
                    activeTrackColor = Color(0xFF8B5CF6)
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "ÁREA DE DESENHO",
                color = Color(0xFFB9A5FF),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF303442), RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .pointerInput(zoom, eraser, selectedColor, brushSize, opacity) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(
                                        Offset(offset.x / zoom, offset.y / zoom)
                                    )
                                },
                                onDrag = { change, _ ->
                                    currentPoints = currentPoints + Offset(
                                        change.position.x / zoom,
                                        change.position.y / zoom
                                    )
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        strokes = strokes + PaintStroke(
                                            points = currentPoints,
                                            color = if (eraser) Color.White else selectedColor,
                                            width = if (eraser) brushSize * 3f else brushSize,
                                            opacity = if (eraser) 1f else opacity
                                        )
                                        redoStrokes = emptyList()
                                    }
                                    currentPoints = emptyList()
                                }
                            )
                        }
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                        }
                ) {
                    val allStrokes = strokes + listOfNotNull(
                        if (currentPoints.isNotEmpty()) {
                            PaintStroke(
                                currentPoints,
                                if (eraser) Color.White else selectedColor,
                                if (eraser) brushSize * 3f else brushSize,
                                if (eraser) 1f else opacity
                            )
                        } else null
                    )

                    allStrokes.forEach { stroke ->
                        stroke.points.zipWithNext().forEach { (start, end) ->
                            drawLine(
                                color = stroke.color.copy(alpha = stroke.opacity),
                                start = start,
                                end = end,
                                strokeWidth = stroke.width,
                                cap = StrokeCap.Round
                            )
                        }

                        if (stroke.points.size == 1) {
                            drawCircle(
                                color = stroke.color.copy(alpha = stroke.opacity),
                                radius = stroke.width / 2,
                                center = stroke.points.first()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Traços: ${strokes.size}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { zoom = (zoom - 0.2f).coerceAtLeast(0.6f) }
                    ) {
                        Text("−")
                    }
                    OutlinedButton(
                        onClick = { zoom = (zoom + 0.2f).coerceAtMost(2.5f) }
                    ) {
                        Text("+")
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) {
                Color(0xFF7653D6)
            } else {
                Color(0xFF303442)
            },
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}
