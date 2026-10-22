package com.studiopaint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PaintStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val opacity: Float
)

data class PaintLayer(
    val id: Int,
    val name: String,
    val strokes: List<PaintStroke> = emptyList(),
    val visible: Boolean = true
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioPaintApp() }
    }
}

@Composable
fun StudioPaintApp() {
    var layers by remember {
        mutableStateOf(listOf(PaintLayer(1, "Camada 1")))
    }
    var selectedLayerId by remember { mutableIntStateOf(1) }
    var nextLayerId by remember { mutableIntStateOf(2) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var redoStrokes by remember { mutableStateOf(listOf<PaintStroke>()) }
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

    val selectedLayer = layers.firstOrNull { it.id == selectedLayerId }
    val allVisibleStrokes = layers
        .filter { it.visible }
        .flatMap { it.strokes }

    fun updateSelectedLayer(transform: (PaintLayer) -> PaintLayer) {
        layers = layers.map {
            if (it.id == selectedLayerId) transform(it) else it
        }
    }

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
                    Text("StudioPaint", color = Color.White, fontSize = 25.sp)
                    Text(
                        "ESTÚDIO DE DESENHO",
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp
                    )
                }
                Text(
                    "ZOOM ${(zoom * 100).toInt()}%",
                    color = Color(0xFFB9A5FF),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton("Pincel", !eraser) { eraser = false }
                ToolButton("Borracha", eraser) { eraser = true }
                ToolButton("Desfazer", false) {
                    val last = selectedLayer?.strokes?.lastOrNull()
                    if (last != null) {
                        updateSelectedLayer {
                            it.copy(strokes = it.strokes.dropLast(1))
                        }
                        redoStrokes = redoStrokes + last
                    }
                }
                ToolButton("Refazer", false) {
                    val last = redoStrokes.lastOrNull()
                    if (last != null) {
                        updateSelectedLayer {
                            it.copy(strokes = it.strokes + last)
                        }
                        redoStrokes = redoStrokes.dropLast(1)
                    }
                }
                ToolButton("Limpar camada", false) {
                    updateSelectedLayer { it.copy(strokes = emptyList()) }
                    redoStrokes = emptyList()
                    currentPoints = emptyList()
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("CORES", color = Color(0xFFB9A5FF), fontSize = 11.sp)

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
                                if (selectedColor == color)
                                    Modifier.border(3.dp, Color(0xFFB9A5FF), CircleShape)
                                else Modifier
                            )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent,
                            shape = CircleShape,
                            onClick = {
                                selectedColor = color
                                eraser = false
                            }
                        ) {}
                    }
                }
            }

            Text(
                "Tamanho do pincel: ${brushSize.toInt()}",
                color = Color.White,
                fontSize = 13.sp
            )
            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 2f..40f
            )

            Text(
                "Opacidade: ${(opacity * 100).toInt()}%",
                color = Color.White,
                fontSize = 13.sp
            )
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0.1f..1f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CAMADAS", color = Color(0xFFB9A5FF), fontSize = 12.sp)
                Button(
                    onClick = {
                        val id = nextLayerId
                        nextLayerId++
                        layers = layers + PaintLayer(id, "Camada $id")
                        selectedLayerId = id
                    }
                ) {
                    Text("+ Nova")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 112.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(layers.asReversed(), key = { _, layer -> layer.id }) { _, layer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (layer.id == selectedLayerId)
                                    Color(0xFF493778) else Color(0xFF303442),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            layer.name,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        )
                        TextButton(
                            onClick = {
                                layers = layers.map {
                                    if (it.id == layer.id)
                                        it.copy(visible = !it.visible) else it
                                }
                            }
                        ) {
                            Text(if (layer.visible) "Visível" else "Oculta")
                        }
                        TextButton(
                            onClick = { selectedLayerId = layer.id }
                        ) {
                            Text(if (layer.id == selectedLayerId) "Ativa" else "Selecionar")
                        }
                        TextButton(
                            onClick = {
                                if (layers.size > 1) {
                                    layers = layers.filter { it.id != layer.id }
                                    if (selectedLayerId == layer.id) {
                                        selectedLayerId = layers.last().id
                                    }
                                }
                            }
                        ) {
                            Text("X", color = Color(0xFFFF8A8A))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("ÁREA DE DESENHO", color = Color(0xFFB9A5FF), fontSize = 11.sp)

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
                        .pointerInput(
                            selectedLayerId, eraser, selectedColor, brushSize, opacity
                        ) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        val stroke = PaintStroke(
                                            points = currentPoints,
                                            color = if (eraser) Color.White else selectedColor,
                                            width = if (eraser) brushSize * 3f else brushSize,
                                            opacity = if (eraser) 1f else opacity
                                        )
                                        updateSelectedLayer {
                                            it.copy(strokes = it.strokes + stroke)
                                        }
                                        redoStrokes = emptyList()
                                    }
                                    currentPoints = emptyList()
                                }
                            )
                        }
                ) {
                    allVisibleStrokes.forEach { stroke ->
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

                    currentPoints.zipWithNext().forEach { (start, end) ->
                        drawLine(
                            color = if (eraser) Color.White else selectedColor,
                            start = start,
                            end = end,
                            strokeWidth = if (eraser) brushSize * 3f else brushSize,
                            cap = StrokeCap.Round
                        )
                    }
                    if (currentPoints.size == 1) {
                        drawCircle(
                            color = if (eraser) Color.White else selectedColor,
                            radius = brushSize / 2,
                            center = currentPoints.first()
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Camada ativa: ${selectedLayer?.name ?: "-"}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
                Row {
                    OutlinedButton(
                        onClick = { zoom = (zoom - 0.2f).coerceAtLeast(0.6f) }
                    ) { Text("−") }
                    Spacer(Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = { zoom = (zoom + 0.2f).coerceAtMost(2.5f) }
                    ) { Text("+") }
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
            containerColor = if (active) Color(0xFF7653D6) else Color(0xFF303442),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}
