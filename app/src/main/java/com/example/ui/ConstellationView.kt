package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CognitiveProfile
import kotlin.math.cos
import kotlin.math.sin

class PhysicsNode(
    val name: String,
    val color: Color,
    var score: Float,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isDragging: Boolean = false
)

@Composable
fun ConstellationView(
    profiles: List<CognitiveProfile>,
    selectedDimension: String,
    onDimensionSelected: (String) -> Unit,
    selectedSpecialist: String = "Mirror",
    modifier: Modifier = Modifier
) {
    // Phase animators for breathing elements
    val infiniteTransition = rememberInfiniteTransition("ConstellationAtmosphere")
    
    val breathingFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
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
        animationSpec = tween(1100, easing = EaseOutQuad),
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

    // Cognitive dimension metadata
    val dimList = remember {
        listOf(
            "Curiosity" to Color(0xFF60A5FA),      // Slate sky blue
            "Decision" to Color(0xFFFBBF24),       // Solid Amber Gold
            "Learning" to Color(0xFF34D399),       // Mint green
            "Creativity" to Color(0xFFC084FC),     // Amethyst violet
            "Focus" to Color(0xFF2DD4BF),          // Emerald teal
            "Communication" to Color(0xFFF59E0B),  // Dark amber
            "Risk" to Color(0xFFF87171),           // Precision coral red
            "Productivity" to Color(0xFFA7F3D0),   // Light mint
            "Memory" to Color(0xFF818CF8),         // Structural indigo
            "Preference" to Color(0xFFF472B6)      // Rose elegance
        )
    }

    val plannerList = remember { listOf("Productivity", "Focus", "Decision", "Memory") }
    val criticList = remember { listOf("Risk", "Decision", "Preference", "Creativity") }
    val scientistList = remember { listOf("Curiosity", "Learning", "Memory", "Communication") }

    // Animated states for the specialist persona boundaries (GSAP style soft transitions and elastic spring scales)
    val plannerAlpha by animateFloatAsState(
        targetValue = if (selectedSpecialist == "Planner") 1f else 0f,
        animationSpec = tween(750, easing = EaseInOutCubic),
        label = "PlannerAlpha"
    )
    val plannerScale by animateFloatAsState(
        targetValue = if (selectedSpecialist == "Planner") 1.0f else 0.88f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow),
        label = "PlannerScale"
    )

    val criticAlpha by animateFloatAsState(
        targetValue = if (selectedSpecialist == "Critic") 1f else 0f,
        animationSpec = tween(750, easing = EaseInOutCubic),
        label = "CriticAlpha"
    )
    val criticScale by animateFloatAsState(
        targetValue = if (selectedSpecialist == "Critic") 1.0f else 0.88f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow),
        label = "CriticScale"
    )

    val scientistAlpha by animateFloatAsState(
        targetValue = if (selectedSpecialist == "Scientist") 1f else 0f,
        animationSpec = tween(750, easing = EaseInOutCubic),
        label = "ScientistAlpha"
    )
    val scientistScale by animateFloatAsState(
        targetValue = if (selectedSpecialist == "Scientist") 1.0f else 0.88f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow),
        label = "ScientistScale"
    )

    // Morph layout transition (interpolates between the old specialist layout coordinates and the new ones)
    var previousSpecialist by remember { mutableStateOf(selectedSpecialist) }
    var activeSpecialistState by remember { mutableStateOf(selectedSpecialist) }
    val layoutProgressSpec = remember { Animatable(1f) }

    LaunchedEffect(selectedSpecialist) {
        if (selectedSpecialist != activeSpecialistState) {
            previousSpecialist = activeSpecialistState
            activeSpecialistState = selectedSpecialist
            layoutProgressSpec.snapTo(0f)
            layoutProgressSpec.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy, // Elegant organic bounce/settle
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Canvas coordinate storage
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    
    // Physics simulation state list
    val nodes = remember { mutableStateListOf<PhysicsNode>() }

    // Recomp trigger state for frame updates
    var physicsTick by remember { mutableIntStateOf(0) }

    // Trigger elegant constellation synapse wave when a dimension is mapped/selected via voice on sentiment/topic analysis
    LaunchedEffect(selectedDimension) {
        if (selectedDimension.isNotEmpty() && canvasSize.x > 0f && nodes.isNotEmpty()) {
            val node = nodes.find { it.name == selectedDimension }
            if (node != null) {
                tapRippleSource = Offset(node.x, node.y)
                tapRippleWave = minOf(canvasSize.x, canvasSize.y) * 0.34f * 1.5f
            }
        }
    }

    // Sync profiles into stateful physics nodes
    LaunchedEffect(profiles) {
        if (nodes.isEmpty()) {
            dimList.forEach { (dimName, color) ->
                val score = profiles.find { it.dimension == dimName }?.score ?: 0.5f
                nodes.add(
                    PhysicsNode(
                        name = dimName,
                        color = color,
                        score = score,
                        x = 0f,
                        y = 0f
                    )
                )
            }
        } else {
            nodes.forEach { node ->
                val matching = profiles.find { it.dimension == node.name }
                if (matching != null) {
                    node.score = matching.score
                }
            }
        }
    }

    // High performance frame-ticker physics loop (d3-force physics engine equivalent in native Kotlin)
    var lastTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(nodes) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastTimeNanos == 0L) {
                    lastTimeNanos = frameTimeNanos
                }
                val dt = ((frameTimeNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.002f, 0.033f)
                lastTimeNanos = frameTimeNanos

                if (canvasSize.x > 0f && canvasSize.y > 0f && nodes.isNotEmpty()) {
                    val cx = canvasSize.x / 2f
                    val cy = canvasSize.y / 2f
                    val rBase = minOf(canvasSize.x, canvasSize.y) * 0.34f

                    // 1. Position initialization relative to center screen coordinates
                    val isFirstInit = nodes.all { it.x == 0f && it.y == 0f }
                    if (isFirstInit) {
                        nodes.forEachIndexed { index, node ->
                            val angleRad = Math.toRadians((index * 36f).toDouble())
                            val currentRadius = rBase * (0.8f + (node.score * 0.4f))
                            node.x = cx + (currentRadius * cos(angleRad)).toFloat()
                            node.y = cy + (currentRadius * sin(angleRad)).toFloat()
                        }
                    }

                    // 2. Physical force accumulators
                    val numNodes = nodes.size
                    val fxs = FloatArray(numNodes)
                    val fys = FloatArray(numNodes)

                    // Physics tuning constants matching modern elastic web simulations
                    val kAnchor = 9.0f      // Hooke's spring pulling star nodes back matching Symmetric structures
                    val kLink = 3.8f        // Spring tension stringing adjacent node links like network fibers
                    val kCharge = 9200f     // Coulomb electric repulsion to balance/spread floating entities
                    val damping = 0.88f     // Drag damping to decelerate stable geometric constellations

                    // A. Restore Anchor Forces (Centers each dimension onto elegant concentric orbits, using fluid transition progress)
                    val progress = layoutProgressSpec.value
                    nodes.forEachIndexed { index, node ->
                        if (node.isDragging) return@forEachIndexed
                        
                        val prevTarget = getTargetForSpecialist(
                            spec = previousSpecialist,
                            nodeName = node.name,
                            nodeIndex = index,
                            cx = cx,
                            cy = cy,
                            rBase = rBase,
                            totalNodes = nodes.size,
                            nodeScore = node.score,
                            physicsTick = physicsTick,
                            plannerList = plannerList,
                            criticList = criticList,
                            scientistList = scientistList
                        )
                        
                        val currTarget = getTargetForSpecialist(
                            spec = activeSpecialistState,
                            nodeName = node.name,
                            nodeIndex = index,
                            cx = cx,
                            cy = cy,
                            rBase = rBase,
                            totalNodes = nodes.size,
                            nodeScore = node.score,
                            physicsTick = physicsTick,
                            plannerList = plannerList,
                            criticList = criticList,
                            scientistList = scientistList
                        )

                        // Smoothly GSAP-interpolate the source/destination target anchor coordinates
                        val tx = prevTarget.x + (currTarget.x - prevTarget.x) * progress
                        val ty = prevTarget.y + (currTarget.y - prevTarget.y) * progress

                        fxs[index] += kAnchor * (tx - node.x)
                        fys[index] += kAnchor * (ty - node.y)
                    }

                    // B. Repulsive Mutual Coulomb Charge Forces
                    for (i in 0 until numNodes) {
                        val nodeA = nodes[i]
                        for (j in i + 1 until numNodes) {
                            val nodeB = nodes[j]
                            val dx = nodeA.x - nodeB.x
                            val dy = nodeA.y - nodeB.y
                            val distSq = dx * dx + dy * dy
                            val dist = kotlin.math.sqrt(distSq) + 0.1f
                            
                            val force = kCharge / (distSq + 16f)
                            val fx = (dx / dist) * force
                            val fy = (dy / dist) * force

                            if (!nodeA.isDragging) {
                                fxs[i] += fx
                                fys[i] += fy
                            }
                            if (!nodeB.isDragging) {
                                fxs[j] -= fx
                                fys[j] -= fy
                            }
                        }
                    }

                    // C. Adjacent String Elastic Restoring spring forces
                    for (i in 0 until numNodes) {
                        val nodeA = nodes[i]
                        val j = (i + 1) % numNodes
                        val nodeB = nodes[j]

                        val dx = nodeB.x - nodeA.x
                        val dy = nodeB.y - nodeA.y
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy) + 0.1f

                        val targetAangle = Math.toRadians((i * 36f).toDouble())
                        val targetBangle = Math.toRadians((j * 36f).toDouble())
                        val rA = rBase * (0.8f + (nodeA.score * 0.4f))
                        val rB = rBase * (0.8f + (nodeB.score * 0.4f))
                        val ax = cx + (rA * cos(targetAangle)).toFloat()
                        val ay = cy + (rA * sin(targetAangle)).toFloat()
                        val bx = cx + (rB * cos(targetBangle)).toFloat()
                        val by = cy + (rB * sin(targetBangle)).toFloat()

                        val adx = bx - ax
                        val ady = by - ay
                        val restDist = kotlin.math.sqrt(adx * adx + ady * ady)

                        val strain = dist - restDist
                        val force = kLink * strain
                        val fx = (dx / dist) * force
                        val fy = (dy / dist) * force

                        if (!nodeA.isDragging) {
                            fxs[i] += fx
                            fys[i] += fy
                        }
                        if (!nodeB.isDragging) {
                            fxs[j] -= fx
                            fys[j] -= fy
                        }
                    }

                    // 3. Coordinate integration (Verlet/Euler velocity propagation)
                    nodes.forEachIndexed { index, node ->
                        if (!node.isDragging) {
                            node.vx = (node.vx + fxs[index] * dt) * damping
                            node.vy = (node.vy + fys[index] * dt) * damping
                            node.x += node.vx * dt
                            node.y += node.vy * dt

                            // Safe boundary restrictions to keep nodes visible on screen
                            val borderOffset = 16f
                            node.x = node.x.coerceIn(borderOffset, canvasSize.x - borderOffset)
                            node.y = node.y.coerceIn(borderOffset, canvasSize.y - borderOffset)
                        }
                    }

                    physicsTick++
                }
            }
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes, canvasSize) {
                    if (canvasSize.x <= 0f || canvasSize.y <= 0f) return@pointerInput
                    val rBase = minOf(canvasSize.x, canvasSize.y) * 0.34f
                    
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            var draggedNode: PhysicsNode? = null
                            var minDistance = 86f // Touch margin target
                            
                            for (node in nodes) {
                                val dx = down.position.x - node.x
                                val dy = down.position.y - node.y
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                if (dist < minDistance) {
                                    minDistance = dist
                                    draggedNode = node
                                }
                            }
                            
                            if (draggedNode != null) {
                                draggedNode.isDragging = true
                                onDimensionSelected(draggedNode.name)
                                tapRippleSource = down.position
                                tapRippleWave = rBase * 1.5f
                                
                                val pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    draggedNode.x = change.position.x
                                    draggedNode.y = change.position.y
                                    draggedNode.vx = 0f
                                    draggedNode.vy = 0f
                                    change.consume()
                                }
                                draggedNode.isDragging = false
                            } else {
                                // Tapped open area - spawn concentric shockwaves & push simulation elements outward
                                tapRippleSource = down.position
                                tapRippleWave = rBase * 1.5f
                                
                                nodes.forEach { node ->
                                    val dx = node.x - down.position.x
                                    val dy = node.y - down.position.y
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy) + 0.1f
                                    if (dist < rBase * 2.2f) {
                                        val repulsionImpulse = 480f / (dist * 0.06f + 1f)
                                        node.vx += (dx / dist) * repulsionImpulse
                                        node.vy += (dy / dist) * repulsionImpulse
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Read state ticker of physics step to force recomposition
            val frameSequenceTick = physicsTick
            
            // Save Canvas dynamic specs
            if (canvasSize.x != size.width || canvasSize.y != size.height) {
                canvasSize = Offset(size.width, size.height)
            }

            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxBound = minOf(size.width, size.height)
            val radiusBase = maxBound * 0.34f

            // 1. Render ambient glow centers
            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.07f * breathingFactor),
                radius = radiusBase * 1.35f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = 0.035f),
                radius = radiusBase * 1.05f,
                center = Offset(centerX + 40f, centerY - 30f)
            )

            // 2. Render physical anchor alignment rings (Geometric Harmony)
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.35f),
                radius = radiusBase * 1.45f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.28f),
                radius = radiusBase * 1.15f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF334155).copy(alpha = 0.18f),
                radius = radiusBase * 0.85f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.12f),
                radius = radiusBase * 0.55f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Render connection links (Dynamic elasticity deforming on stretches)
            if (nodes.isNotEmpty()) {
                val numNodes = nodes.size
                for (i in 0 until numNodes) {
                    val nodeA = nodes[i]
                    
                    // Connected Adjacent link
                    val nodeB = nodes[(i + 1) % numNodes]
                    drawLine(
                        color = Color(0xFF475569).copy(alpha = 0.16f),
                        start = Offset(nodeA.x, nodeA.y),
                        end = Offset(nodeB.x, nodeB.y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Connected Diagonal Synapses
                    val nodeC = nodes[(i + 4) % numNodes]
                    drawLine(
                        color = Color(0xFF334155).copy(alpha = 0.08f),
                        start = Offset(nodeA.x, nodeA.y),
                        end = Offset(nodeC.x, nodeC.y),
                        strokeWidth = 0.8f.dp.toPx()
                    )
                }
            }

            val cx = centerX
            val cy = centerY
            val rBase = radiusBase

            // Specialist Persona Enclosing Outlines (GSAP style soft fades and spring scale transitions)
            if (plannerAlpha > 0.01f) {
                val pNodes = nodes.filter { it.name in plannerList }
                if (pNodes.isNotEmpty()) {
                    val minX = pNodes.minOf { it.x }
                    val minY = pNodes.minOf { it.y }
                    val maxX = pNodes.maxOf { it.x }
                    val maxY = pNodes.maxOf { it.y }
                    val midX = (minX + maxX) / 2f
                    val midY = (minY + maxY) / 2f
                    val padding = 28f
                    val s = plannerScale
                    
                    val w = (maxX - minX + padding * 2) * s
                    val h = (maxY - minY + padding * 2) * s
                    val tlX = midX - w / 2f
                    val tlY = midY - h / 2f
                    
                    // Enclosure background fill
                    drawRoundRect(
                        color = Color(0xFFFBBF24).copy(alpha = 0.015f * plannerAlpha),
                        topLeft = Offset(tlX, tlY),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * s, 14f * s)
                    )
                    
                    // Dashed golden boundary
                    drawRoundRect(
                        color = Color(0xFFFBBF24).copy(alpha = 0.28f * plannerAlpha),
                        topLeft = Offset(tlX, tlY),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * s, 14f * s),
                        style = Stroke(
                            width = 1.2.dp.toPx() * s,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * s, 8f * s), 0f)
                        )
                    )
                }
            }

            if (criticAlpha > 0.01f) {
                val cNodes = nodes.filter { it.name in criticList }
                if (cNodes.isNotEmpty()) {
                    val minX = cNodes.minOf { it.x }
                    val minY = cNodes.minOf { it.y }
                    val maxX = cNodes.maxOf { it.x }
                    val maxY = cNodes.maxOf { it.y }
                    val padding = 25f
                    
                    val centerDiamondX = cx
                    val centerDiamondY = cy - rBase * 0.35f
                    val s = criticScale
                    
                    val p1X = centerDiamondX
                    val p1Y = centerDiamondY + (minY - padding - centerDiamondY) * s
                    
                    val p2X = centerDiamondX + (maxX + padding - centerDiamondX) * s
                    val p2Y = centerDiamondY
                    
                    val p3X = centerDiamondX
                    val p3Y = centerDiamondY + (maxY + padding - centerDiamondY) * s
                    
                    val p4X = centerDiamondX + (minX - padding - centerDiamondX) * s
                    val p4Y = centerDiamondY

                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(p1X, p1Y)
                        lineTo(p2X, p2Y)
                        lineTo(p3X, p3Y)
                        lineTo(p4X, p4Y)
                        close()
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFFF87171).copy(alpha = 0.015f * criticAlpha)
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFFF87171).copy(alpha = 0.32f * criticAlpha),
                        style = Stroke(
                            width = 1.2.dp.toPx() * s,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * s, 6f * s), 0f)
                        )
                    )
                }
            }

            if (scientistAlpha > 0.01f) {
                val sNodes = nodes.filter { it.name in scientistList }
                if (sNodes.isNotEmpty()) {
                    val s = scientistScale
                    drawCircle(
                        color = Color(0xFF34D399).copy(alpha = 0.22f * scientistAlpha),
                        radius = rBase * 0.48f * s,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 1.dp.toPx() * s,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f * s, 12f * s), 0f)
                        )
                    )
                    drawCircle(
                        color = Color(0xFF34D399).copy(alpha = 0.06f * scientistAlpha),
                        radius = rBase * 0.28f * s,
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.8f.dp.toPx() * s)
                    )
                }
            }

            // 4. Render interactive kinetic shockwaves
            if (rippleAnim.value > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - (rippleAnim.value / (radiusBase * 1.5f))).coerceIn(0f, 1f) * 0.22f),
                    radius = rippleAnim.value,
                    center = tapRippleSource,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }

            // 5. Render physical star cores and high contrast selection halos
            nodes.forEach { node ->
                val isSelected = node.name == selectedDimension
                val starRadius = (4.dp.toPx() + (node.score * 5.5.dp.toPx())) * (if (isSelected) 1.25f else 1.0f)
                val haloRadius = starRadius * 3.3f * breathingFactor

                // Neon halo
                drawCircle(
                    color = node.color.copy(alpha = if (isSelected) 0.35f else 0.11f),
                    radius = haloRadius,
                    center = Offset(node.x, node.y)
                )

                // High intensity solid core
                drawCircle(
                    color = if (isSelected) Color.White else node.color,
                    radius = starRadius,
                    center = Offset(node.x, node.y)
                )

                // Extra orbital contour if target selected
                if (isSelected) {
                    drawCircle(
                        color = node.color.copy(alpha = 0.45f),
                        radius = starRadius * 1.85f,
                        center = Offset(node.x, node.y),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
        }

        // 6. Center geometric HUD showing Synchrony
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

private fun getTargetForSpecialist(
    spec: String,
    nodeName: String,
    nodeIndex: Int,
    cx: Float,
    cy: Float,
    rBase: Float,
    totalNodes: Int,
    nodeScore: Float,
    physicsTick: Int,
    plannerList: List<String>,
    criticList: List<String>,
    scientistList: List<String>
): Offset {
    return when (spec) {
        "Planner" -> {
            if (nodeName in plannerList) {
                val offsetIndex = plannerList.indexOf(nodeName)
                val row = offsetIndex / 2
                val col = offsetIndex % 2
                val gridX = (cx - rBase * 0.55f) + (col * 130f)
                val gridY = (cy - rBase * 0.45f) + (row * 130f)
                Offset(gridX, gridY)
            } else {
                val otherIndex = (totalNodes - 1 - nodeIndex)
                val angleRad = Math.toRadians((otherIndex * 50f).toDouble()) + (physicsTick * 0.002f)
                val tx = (cx + rBase * 0.5f) + (rBase * 0.45f * cos(angleRad)).toFloat()
                val ty = (cy + rBase * 0.4f) + (rBase * 0.45f * sin(angleRad)).toFloat()
                Offset(tx, ty)
            }
        }
        "Critic" -> {
            if (nodeName in criticList) {
                val offsetIndex = criticList.indexOf(nodeName)
                val angleRad = Math.toRadians((offsetIndex * 90f + 45f).toDouble())
                val tx = cx + (rBase * 0.45f * cos(angleRad)).toFloat()
                val ty = (cy - rBase * 0.35f) + (rBase * 0.45f * sin(angleRad)).toFloat()
                Offset(tx, ty)
            } else {
                val otherIndex = nodeIndex
                val angleRad = Math.toRadians((otherIndex * 40f + 140f).toDouble()) + (physicsTick * 0.001f)
                val tx = cx + (rBase * 0.85f * cos(angleRad)).toFloat()
                val ty = (cy + rBase * 0.45f) + (rBase * 0.35f * sin(angleRad)).toFloat()
                Offset(tx, ty)
            }
        }
        "Scientist" -> {
            if (nodeName in scientistList) {
                val offsetIndex = scientistList.indexOf(nodeName)
                val angleRad = Math.toRadians((offsetIndex * 90f).toDouble()) + (physicsTick * 0.004f)
                val tx = cx + (rBase * 0.38f * cos(angleRad)).toFloat()
                val ty = cy + (rBase * 0.38f * sin(angleRad)).toFloat()
                Offset(tx, ty)
            } else {
                val otherIndex = nodeIndex
                val angleRad = Math.toRadians((otherIndex * 60f).toDouble()) + (physicsTick * -0.002f)
                val tx = cx + (rBase * 1.1f * cos(angleRad)).toFloat()
                val ty = cy + (rBase * 1.1f * sin(angleRad)).toFloat()
                Offset(tx, ty)
            }
        }
        else -> { // "Mirror" or default
            val rotationOffset = (physicsTick * 0.0035f)
            val angleRad = Math.toRadians((nodeIndex * 36f).toDouble()) + rotationOffset
            val targetRadius = rBase * (0.8f + (nodeScore * 0.4f))
            val tx = cx + (targetRadius * cos(angleRad)).toFloat()
            val ty = cy + (targetRadius * sin(angleRad)).toFloat()
            Offset(tx, ty)
        }
    }
}
