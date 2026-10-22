package com.studiopaint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class DrawLine(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StudioPaintApp()
        }
    }
}

@Composable
fun StudioPaintApp() {
    var lines by remember { mutableStateOf(listOf<DrawLine>()) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var brushSize by remember { mutableFloatStateOf(8f) }

    val colors = listOf(
        Color.Black,
        Color.Red,
        Color.Blue,
        Color.Green,
        Color(0xFF9C27B0),
        Color(0xFFFF9800)
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F3F8))
                .padding(12.dp)
        ) {
            Text(
                text = "StudioPaint",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF303F9F)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { color ->
                    Surface(
                        modifier = Modifier
                            .size(34.dp),
                        shape = RoundedCornerShape(50),
                        color = color,
                        onClick = { selectedColor = color },
                        border = if (selectedColor == color) {
                            androidx.compose.foundation.BorderStroke(
                                3.dp,
                                Color.LightGray
                            )
                        } else null
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Tamanho do pincel: ${brushSize.toInt()}")

            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 2f..30f
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (lines.isNotEmpty()) {
                            lines = lines.dropLast(1)
                        }
                    }
                ) {
                    Text("Desfazer")
                }

                OutlinedButton(
                    onClick = {
                        lines = emptyList()
                        currentPoints = emptyList()
                    }
                ) {
                    Text("Limpar")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
                    .pointerInput(selectedColor, brushSize, lines) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                currentPoints =
                                    currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    lines = lines + DrawLine(
                                        points = currentPoints,
                                        color = selectedColor,
                                        width = brushSize
                                    )
                                }
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                val allLines = lines + listOfNotNull(
                    if (currentPoints.isNotEmpty()) {
                        DrawLine(
                            points = currentPoints,
                            color = selectedColor,
                            width = brushSize
                        )
                    } else null
                )

                allLines.forEach { line ->
                    line.points.zipWithNext().forEach { (start, end) ->
                        drawLine(
                            color = line.color,
                            start = start,
                            end = end,
                            strokeWidth = line.width,
                            cap = StrokeCap.Round
                        )
                    }

                    if (line.points.size == 1) {
                        drawCircle(
                            color = line.color,
                            radius = line.width / 2,
                            center = line.points.first()
                        )
                    }
                }
            }
        }
    }
}
