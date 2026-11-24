package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.model.ParticleRequest
import com.example.model.ParticleTarget
import com.example.ui.theme.LilitaOneFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val OrbColors = listOf(
    Color(0xFFFF4FA3),
    Color(0xFF3CD64A),
    Color(0xFF2E9BFF),
    Color(0xFFFFD21F),
    Color(0xFF9B5CFF),
    Color(0xFF14B9C4),
    Color(0xFFFF3B4E)
)
private val OrbNumbers = listOf("3", "6", "9", "5", "7", "1", "2")

private data class OrbSpec(
    val xFrac: Float,
    val sizeDp: Float,
    val color: Color,
    val label: String,
    val durationSec: Float,
    val delayOffset: Float
)

@Composable
fun BummmBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val spinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(80_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "conicSpin"
    )
    val orbClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbClock"
    )

    val orbs = remember {
        List(14) { i ->
            OrbSpec(
                xFrac = Random.nextFloat() * 0.94f,
                sizeDp = (22 + Random.nextInt(23)).toFloat(),
                color = OrbColors[i % 7],
                label = OrbNumbers[i % 7],
                durationSec = (10 + Random.nextInt(13)).toFloat(),
                delayOffset = Random.nextFloat() * 20f
            )
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val orbTextStyle = remember {
        TextStyle(
            fontFamily = LilitaOneFont,
            fontSize = 18.sp,
            color = Color.White
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.32f
        val baseRadius = hypot(w.toDouble(), h.toDouble()).toFloat() * 0.72f

        // Base radial sunburst gradient
        drawRect(
            brush = Brush.radialGradient(
                0.00f to Color(0xFFFFE45C),
                0.26f to Color(0xFFFFB02B),
                0.52f to Color(0xFFFF7A2E),
                0.78f to Color(0xFFE0407A),
                1.00f to Color(0xFF6A2FE0),
                center = Offset(cx, cy),
                radius = baseRadius
            )
        )

        // Top-left cyan glow
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF25D0FF),
                1.0f to Color(0x0025D0FF),
                center = Offset(0f, 0f),
                radius = w * 0.42f
            )
        )

        // Top-right cyan glow
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF25D0FF),
                1.0f to Color(0x0025D0FF),
                center = Offset(w, 0f),
                radius = w * 0.38f
            )
        )

        // Rotating conic rays (24 rays: 5 deg every 15 deg)
        val rayRadius = maxOf(w, h) * 1.6f
        val rayColor = Color.White.copy(alpha = 0.22f)
        rotate(degrees = spinDeg, pivot = Offset(cx, cy)) {
            for (k in 0 until 24) {
                val startRad = Math.toRadians(k * 15.0)
                val endRad = Math.toRadians(k * 15.0 + 5.0)
                val path = Path().apply {
                    moveTo(cx, cy)
                    lineTo(
                        cx + (cos(startRad) * rayRadius).toFloat(),
                        cy + (sin(startRad) * rayRadius).toFloat()
                    )
                    lineTo(
                        cx + (cos(endRad) * rayRadius).toFloat(),
                        cy + (sin(endRad) * rayRadius).toFloat()
                    )
                    close()
                }
                drawPath(path, color = rayColor)
            }
        }

        // 14 floating numbered orbs
        val totalSpan = h * 1.15f + with(density) { 80f * this.density }
        for (orb in orbs) {
            val rPx = with(density) { (orb.sizeDp * 0.5f) * this.density }
            val t = ((orbClock + orb.delayOffset) % orb.durationSec) / orb.durationSec
            val x = orb.xFrac * w + rPx
            val y = (h + rPx * 2f) - t * totalSpan
            val rot = t * 180f

            rotate(degrees = rot, pivot = Offset(x, y)) {
                drawCircle(
                    color = orb.color.copy(alpha = 0.80f),
                    radius = rPx,
                    center = Offset(x, y)
                )
                // Gloss highlight & inner bottom shadow
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.45f),
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.22f),
                        center = Offset(x - rPx * 0.25f, y - rPx * 0.25f),
                        radius = rPx * 1.2f
                    ),
                    radius = rPx,
                    center = Offset(x, y)
                )
                val layout = textMeasurer.measure(orb.label, orbTextStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x - layout.size.width * 0.5f,
                        y - layout.size.height * 0.5f
                    )
                )
            }
        }
    }
}

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val g: Float,
    val size: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    var rot: Float,
    val vRot: Float,
    val isSquare: Boolean
)

private val ParticlePalette = listOf(
    Color(0xFFFFD21F),
    Color(0xFFFF8A1F),
    Color(0xFFFF3B4E),
    Color(0xFF2E9BFF),
    Color(0xFF3CD64A),
    Color(0xFFFFFFFF),
    Color(0xFFFF4FA3)
)

private val BadParticlePalette = listOf(
    Color(0xFFFF3B4E),
    Color(0xFF7A2BFF),
    Color(0xFFFFFFFF)
)

@Composable
fun ParticleOverlay(
    requests: SharedFlow<ParticleRequest>,
    wallCenterProvider: () -> Offset?,
    padButtonCenterProvider: (Int) -> Offset?,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableListOf<Particle>() }
    var active by remember { mutableStateOf(false) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current.density

    fun spawnBurst(
        x: Float,
        y: Float,
        count: Int,
        speed: Float,
        g: Float,
        sizeDp: Float,
        lifeSec: Float,
        colors: List<Color>
    ) {
        val n = min(count, 90)
        repeat(n) {
            if (particles.size >= 260) return@repeat
            val a = Random.nextFloat() * (2f * PI.toFloat())
            val sp = (speed * density) * (0.4f + Random.nextFloat() * 0.8f)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(a) * sp,
                    vy = sin(a) * sp,
                    g = g * density,
                    size = (sizeDp * density) * (0.5f + Random.nextFloat()),
                    life = 0f,
                    maxLife = lifeSec * (0.7f + Random.nextFloat() * 0.6f),
                    color = colors[Random.nextInt(colors.size)],
                    rot = Random.nextFloat() * 6.283f,
                    vRot = (Random.nextFloat() - 0.5f) * 12f,
                    isSquare = Random.nextFloat() < 0.6f
                )
            )
        }
        active = true
    }

    LaunchedEffect(requests) {
        requests.collect { req ->
            val fallbackCenter = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.36f)
            when (req.target) {
                ParticleTarget.WALL_GOOD -> {
                    val c = wallCenterProvider() ?: fallbackCenter
                    spawnBurst(c.x, c.y, 70, 520f, 500f, 11f, 1.0f, ParticlePalette)
                }
                ParticleTarget.WALL_BAD -> {
                    val c = wallCenterProvider() ?: fallbackCenter
                    spawnBurst(c.x, c.y, 40, 420f, 700f, 8f, 0.8f, BadParticlePalette)
                }
                ParticleTarget.WALL_END -> {
                    val c = wallCenterProvider() ?: fallbackCenter
                    spawnBurst(c.x, c.y, 80, 600f, 500f, 12f, 1.1f, ParticlePalette)
                }
                ParticleTarget.PAD_BUTTON -> {
                    val c = (req.buttonNumber?.let { padButtonCenterProvider(it) }) ?: fallbackCenter
                    spawnBurst(c.x, c.y, 12, 260f, 700f, 8f, 0.8f, ParticlePalette)
                }
                ParticleTarget.BUM_BUTTON -> {
                    val c = wallCenterProvider() ?: fallbackCenter
                    spawnBurst(c.x, c.y, 20, 320f, 700f, 8f, 0.8f, ParticlePalette)
                }
                ParticleTarget.CONFETTI -> {
                    repeat(6) { k ->
                        val w = canvasSize.width.coerceAtLeast(120f)
                        val x = Random.nextFloat() * (w - 60f) + 30f
                        spawnBurst(x, -10f, 14, 120f, 520f, 9f, 2.2f, ParticlePalette)
                        if (k < 5) delay(130L)
                    }
                }
            }
        }
    }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var lastNanos = 0L
        while (active) {
            withFrameNanos { nanos ->
                if (lastNanos == 0L) lastNanos = nanos
                val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                lastNanos = nanos
                val fh = canvasSize.height
                val it = particles.listIterator(particles.size)
                while (it.hasPrevious()) {
                    val p = it.previous()
                    p.life += dt
                    if (p.life >= p.maxLife || (fh > 0f && p.y > fh + 40f * density)) {
                        it.remove()
                    } else {
                        p.vy += p.g * dt
                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        p.rot += p.vRot * dt
                    }
                }
                frameTick = nanos
                if (particles.isEmpty()) active = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick
            for (p in particles) {
                val alpha = (1f - p.life / p.maxLife).coerceIn(0f, 1f)
                val col = p.color.copy(alpha = alpha)
                if (p.isSquare) {
                    rotate(degrees = Math.toDegrees(p.rot.toDouble()).toFloat(), pivot = Offset(p.x, p.y)) {
                        drawRect(
                            color = col,
                            topLeft = Offset(p.x - p.size * 0.5f, p.y - p.size * 0.33f),
                            size = Size(p.size, p.size * 0.66f)
                        )
                    }
                } else {
                    drawCircle(
                        color = col,
                        radius = p.size * 0.5f,
                        center = Offset(p.x, p.y)
                    )
                }
            }
        }
    }
}
