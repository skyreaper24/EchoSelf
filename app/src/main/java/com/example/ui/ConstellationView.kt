package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CognitiveProfile
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ConstellationView(
    profiles: List<CognitiveProfile>,
    selectedDimension: String,
    onDimensionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Phase animators for breathing stars and gentle orbital rotation
    val infiniteTransition = rememberInfiniteTransition("ConstellationAtmosphere")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val breathingFactor by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarBreathing"
    )

    // Interactive ripple wave pulse from tap
    var tapRippleWave by remember { mutableStateOf(0f) }
    var tapRippleSource by remember { mutableStateOf(Offset.Zero) }
    val rippleAnim = animateFloatAsState(
        targetValue = tapRippleWave,
        animationSpec = tween(1250, easing = EaseOutQuad),
        finishedListener = { tapRippleWave = 0f },
        label = "SynapsePulse"
    )

    val alignmentScore = remember(profiles) {
        if (profiles.isNotEmpty()) {
            profiles.map { it.score }.average().toFloat()
        } else {
            0.88f
        }
    }

    Box(modifier = modifier) {
        // Map dimensional profiles
        val dimList = listOf(
            "Curiosity" to Color(0xFF60A5FA),      // Slate sky blue
            "Decision" to Color(0xFFFBBF24),       // Solid Amber Gold
            "Learning" to Color(0xFF34D399),       // Mint green
            "Creativity" to Color(0xFFC084FC),     // Amethyst violet
            "Focus" to Color(0xFF2DD4BF),          // Emerald teal
            "Communication" to Color(0xFFF59E0B),   // Dark amber
            "Risk" to Color(0xFFF87171),           // Precision coral red
            "Productivity" to Color(0xFFA7F3D0),   // Light mint
            "Memory" to Color(0xFF818CF8),         // Structural indigo
            "Preference" to Color(0xFFF472B6)      // Rose elegance
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(profiles, dimList) {
                    detectTapGestures { offset ->
                        val sizeWidth = size.width.toFloat()
                        val sizeHeight = size.height.toFloat()
                        val centerX = sizeWidth / 2f
                        val centerY = sizeHeight / 2f
                        val radiusBase = minOf(sizeWidth, sizeHeight) * 0.35f

                        var closestDim: String? = null
                        var minDistance = Float.MAX_VALUE

                        // Calculate physical star coordinates to check closeness
                        dimList.forEachIndexed { index, (dimName, _) ->
                            val score = profiles.find { it.dimension == dimName }?.score ?: 0.5f
                            val angleRad = Math.toRadians((index * 36f + rotationAngle).toDouble())
                            val currentRadius = radiusBase * (0.8f + (score * 0.4f)) // Score extends orbit size!

                            val starX = centerX + (currentRadius * cos(angleRad)).toFloat()
                            val starY = centerY + (currentRadius * sin(angleRad)).toFloat()

                            val dx = offset.x - starX
                            val dy = offset.y - starY
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (dist < 80f && dist < minDistance) { // Tap threshold is generous for touch targets
                                minDistance = dist
                                closestDim = dimName
                            }
                        }

                        closestDim?.let {
                            onDimensionSelected(it)
                            // Trigger synapse pulse animation
                            tapRippleSource = offset
                            tapRippleWave = radiusBase * 1.5f
                        }
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxBound = minOf(size.width, size.height)
            val radiusBase = maxBound * 0.35f

            // 1. Draw elegant ambient glows (amber-500/10 and blue-500/5) directly on background
            // Background ambient amber glow
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.08f * breathingFactor),
                radius = radiusBase * 1.4f,
                center = Offset(centerX, centerY)
            )
            // Background translated blue glow
            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = 0.04f),
                radius = radiusBase * 1.1f,
                center = Offset(centerX + 60f, centerY - 40f)
            )

            // 2. Draw Geometric Circular Concentric Orbits (Structural Balance)
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.4f), // slate-800/40
                radius = radiusBase * 1.45f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.3f), // slate-800/30
                radius = radiusBase * 1.15f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF334155).copy(alpha = 0.2f), // slate-700/20
                radius = radiusBase * 0.85f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.15f),
                radius = radiusBase * 0.55f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Calculate exact nodes coordinates for drawing synapses
            val nodes = dimList.mapIndexed { index, (dimName, color) ->
                val score = profiles.find { it.dimension == dimName }?.score ?: 0.5f
                val angleRad = Math.toRadians((index * 36f + rotationAngle).toDouble())
                // Orbits are shaped organically by the user's focus! Higher score = more expanded galaxy
                val currentRadius = radiusBase * (0.8f + (score * 0.4f))
                
                val starX = centerX + (currentRadius * cos(angleRad)).toFloat()
                val starY = centerY + (currentRadius * sin(angleRad)).toFloat()
                
                StarNode(
                    name = dimName,
                    color = color,
                    pos = Offset(starX, starY),
                    score = score
                )
            }

            // 4. Draw Synapses (Orbital linkages / Star connections) with low-contrast slate-800 tones
            for (i in nodes.indices) {
                val nodeA = nodes[i]
                
                // Connect to adjacent node
                val nodeB = nodes[(i + 1) % nodes.size]
                drawLine(
                    color = Color(0xFF475569).copy(alpha = 0.15f), // slate-600/15
                    start = nodeA.pos,
                    end = nodeB.pos,
                    strokeWidth = 1.dp.toPx()
                )

                // Connect to diagonal nodes (complex synapses)
                val nodeC = nodes[(i + 4) % nodes.size]
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.08f), // slate-700/8
                    start = nodeA.pos,
                    end = nodeC.pos,
                    strokeWidth = 0.8f.dp.toPx()
                )
            }

            // 5. Draw interactive synapse pulse waves
            if (rippleAnim.value > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - (rippleAnim.value / (radiusBase * 1.5f))).coerceIn(0f, 1f) * 0.25f),
                    radius = rippleAnim.value,
                    center = tapRippleSource,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // 6. Draw Stars and their neural halos
            nodes.forEach { node ->
                val isSelected = node.name == selectedDimension
                val starRadius = (4.dp.toPx() + (node.score * 6.dp.toPx())) * (if (isSelected) 1.25f else 1.0f)
                val haloRadius = starRadius * 3.2f * breathingFactor

                // Glowing Halo
                drawCircle(
                    color = node.color.copy(alpha = if (isSelected) 0.32f else 0.10f),
                    radius = haloRadius,
                    center = node.pos
                )

                // Pure star core
                drawCircle(
                    color = if (isSelected) Color.White else node.color,
                    radius = starRadius,
                    center = node.pos
                )

                // Dynamic halo ring if selected
                if (isSelected) {
                    drawCircle(
                        color = node.color.copy(alpha = 0.5f),
                        radius = starRadius * 1.9f,
                        center = node.pos,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
        }

        // 7. Render Central Mirror Telemetry HUD
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ALIGNMENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                letterSpacing = 2.8.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "${(alignmentScore * 100).toInt()}%",
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraLight,
                color = Color.White,
                letterSpacing = (-2).sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Mental Synchrony",
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
        }
    }
}

private data class StarNode(
    val name: String,
    val color: Color,
    val pos: Offset,
    val score: Float
)
