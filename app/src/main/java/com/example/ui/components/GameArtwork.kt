package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.theme.AvatarSpec
import com.example.ui.theme.AvatarSpecs
import com.example.ui.theme.Blue
import com.example.ui.theme.HairStyle
import com.example.ui.theme.HatStyle
import com.example.ui.theme.LilitaOneFont
import com.example.ui.theme.Navy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ComicButtonStyle {
    ORANGE, BLUE, GREEN
}

@Composable
fun ComicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ComicButtonStyle = ComicButtonStyle.ORANGE,
    fontSize: TextUnit = 26.sp,
    small: Boolean = false,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val lipHeight = if (small) 4.dp else 6.dp
    val pressOffset by animateDpAsState(
        targetValue = if (pressed) lipHeight - 1.dp else 0.dp,
        animationSpec = tween(70),
        label = "btnPress"
    )
    val radius = if (small) 16.dp else 22.dp
    val borderWidth = if (small) 3.dp else 4.dp
    val padVTop = if (small) 9.dp else 12.dp
    val padVBot = if (small) 11.dp else 14.dp
    val padH = if (small) 14.dp else 22.dp

    val gradColors = when (style) {
        ComicButtonStyle.ORANGE -> listOf(Color(0xFFFFC93D), Color(0xFFFF8A1F), Color(0xFFFF5A1F))
        ComicButtonStyle.BLUE -> listOf(Color(0xFF6CC4FF), Color(0xFF2E7BFF), Color(0xFF1F4FD6))
        ComicButtonStyle.GREEN -> listOf(Color(0xFF8CF06A), Color(0xFF2DC53A), Color(0xFF169A28))
    }
    val textShadowColor = when (style) {
        ComicButtonStyle.ORANGE -> Color(0x8C781E00)
        ComicButtonStyle.BLUE -> Color(0x99001464)
        ComicButtonStyle.GREEN -> Color(0x9900460A)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(bottom = lipHeight)
            .testTag(testTag)
    ) {
        // Hard bottom shadow lip
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = lipHeight)
                .clip(RoundedCornerShape(radius))
                .background(Navy)
        )
        // Main button surface
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = pressOffset)
                .clip(RoundedCornerShape(radius))
                .background(Brush.verticalGradient(gradColors))
                .border(borderWidth, Blue, RoundedCornerShape(radius))
                .drawBehind {
                    // Inner top glossy highlight
                    drawLine(
                        color = Color.White.copy(alpha = 0.55f),
                        start = Offset(radius.toPx() * 0.45f, borderWidth.toPx() + 2f),
                        end = Offset(size.width - radius.toPx() * 0.45f, borderWidth.toPx() + 2f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(start = padH, end = padH, top = padVTop, bottom = padVBot)
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = LilitaOneFont,
                    fontSize = fontSize,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = textShadowColor,
                        offset = Offset(0f, 6f),
                        blurRadius = 0f
                    )
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

enum class RoundButtonStyle {
    GREEN, BLUE, OFF
}

@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RoundButtonStyle = RoundButtonStyle.GREEN,
    size: Dp = 52.dp,
    testTag: String = "",
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val lip = 5.dp
    val pressOffset by animateDpAsState(
        targetValue = if (pressed) 4.dp else 0.dp,
        animationSpec = tween(70),
        label = "roundPress"
    )
    val grad = when (style) {
        RoundButtonStyle.GREEN -> listOf(Color(0xFF8CF06A), Color(0xFF169A28))
        RoundButtonStyle.BLUE -> listOf(Color(0xFF6CC4FF), Color(0xFF1F4FD6))
        RoundButtonStyle.OFF -> listOf(Color(0xFFB7BDD9), Color(0xFF6A708F))
    }

    Box(
        modifier = modifier
            .size(width = size, height = size + lip)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = lip)
                .clip(CircleShape)
                .background(Navy)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .offset(y = pressOffset)
                .clip(CircleShape)
                .background(Brush.verticalGradient(grad))
                .border(4.dp, Blue, CircleShape)
                .drawBehind {
                    drawArc(
                        color = Color.White.copy(alpha = 0.5f),
                        startAngle = 215f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(6f, 6f),
                        size = Size(this.size.width - 12f, this.size.height - 12f),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            content()
        }
    }
}

@Composable
fun BummmTitleBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lilitaTypeface = remember {
        ResourcesCompat.getFont(context, R.font.lilita_one) ?: Typeface.DEFAULT_BOLD
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(390f / 112f)
    ) {
        val sx = size.width / 390f
        val sy = size.height / 112f

        // Drop shadow plate (+6y)
        drawRoundRect(
            color = Navy,
            topLeft = Offset(16f * sx, 14f * sy),
            size = Size(358f * sx, 94f * sy),
            cornerRadius = CornerRadius(28f * sx, 28f * sy)
        )
        // Main blue gradient plate
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF3E78FF), Color(0xFF1C35B8)),
                startY = 8f * sy,
                endY = 102f * sy
            ),
            topLeft = Offset(16f * sx, 8f * sy),
            size = Size(358f * sx, 94f * sy),
            cornerRadius = CornerRadius(28f * sx, 28f * sy)
        )
        // Plate light blue border
        drawRoundRect(
            color = Color(0xFF8FE3FF),
            topLeft = Offset(16f * sx, 8f * sy),
            size = Size(358f * sx, 94f * sy),
            cornerRadius = CornerRadius(28f * sx, 28f * sy),
            style = Stroke(width = 5f * sx)
        )
        // Inner glossy highlight line
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(34f * sx, 15f * sy),
            end = Offset(348f * sx, 15f * sy),
            strokeWidth = 4f * sx,
            cap = StrokeCap.Round
        )

        // Tilted letters "BUMMM!" matching rotate="-3 2 -2 3 -3 4"
        val letters = listOf("B", "U", "M", "M", "M", "!")
        val xCenters = listOf(64f, 118f, 178f, 238f, 294f, 336f)
        val rots = listOf(-3f, 2f, -2f, 3f, -3f, 4f)
        val baseTextSize = 86f * sy
        val yBaseline = 88f * sy

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            val pShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = lilitaTypeface
                textSize = baseTextSize
                textAlign = Paint.Align.CENTER
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 18f * sx
                strokeJoin = Paint.Join.ROUND
                color = Color(0xFF0A1042).toArgb()
            }
            val pBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = lilitaTypeface
                textSize = baseTextSize
                textAlign = Paint.Align.CENTER
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 14f * sx
                strokeJoin = Paint.Join.ROUND
                color = Color(0xFF1D3FD0).toArgb()
            }
            val pWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = lilitaTypeface
                textSize = baseTextSize
                textAlign = Paint.Align.CENTER
                style = Paint.Style.STROKE
                strokeWidth = 6.5f * sx
                strokeJoin = Paint.Join.ROUND
                color = android.graphics.Color.WHITE
            }
            val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = lilitaTypeface
                textSize = baseTextSize
                textAlign = Paint.Align.CENTER
                style = Paint.Style.FILL
                shader = android.graphics.LinearGradient(
                    0f, yBaseline - baseTextSize * 0.85f,
                    0f, yBaseline,
                    intArrayOf(
                        Color(0xFFFFF77A).toArgb(),
                        Color(0xFFFFC21A).toArgb(),
                        Color(0xFFFF9A0A).toArgb(),
                        Color(0xFFFF5A00).toArgb()
                    ),
                    floatArrayOf(0f, 0.42f, 0.56f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }

            letters.forEachIndexed { idx, ch ->
                val xc = xCenters[idx] * sx
                nc.save()
                nc.translate(xc, yBaseline)
                nc.rotate(rots[idx])
                nc.drawText(ch, 0f, 7f * sy, pShadow)
                nc.drawText(ch, 0f, 0f, pBlue)
                nc.drawText(ch, 0f, 0f, pWhite)
                nc.drawText(ch, 0f, 0f, pFill)
                nc.restore()
            }
        }
    }
}

fun buildBurstPolygon(cx: Float, cy: Float, r1: Float, r2: Float, points: Int, rotRad: Float): Path {
    val path = Path()
    for (i in 0 until points * 2) {
        val a = rotRad + i * PI.toFloat() / points
        val r = if (i % 2 == 1) r2 else r1
        val px = cx + cos(a) * r
        val py = cy + sin(a) * r
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

@Composable
fun StarburstCanvas(
    c1: Color,
    c2: Color,
    c3: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier,
    withWhiteCore: Boolean = false
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val unit = minOf(size.width, size.height) / 200f
        val p1 = buildBurstPolygon(cx, cy, 96f * unit, 58f * unit, 14, 0f)
        val p2 = buildBurstPolygon(cx, cy, 78f * unit, 48f * unit, 14, 0.1f)
        val p3 = buildBurstPolygon(cx, cy, 56f * unit, 36f * unit, 12, 0.2f)
        drawPath(p1, color = c1)
        drawPath(p1, color = strokeColor, style = Stroke(width = 3f * unit, join = StrokeJoin.Round))
        drawPath(p2, color = c2)
        drawPath(p3, color = c3)
        if (withWhiteCore) {
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 38f * unit, center = Offset(cx, cy))
        }
    }
}

@Composable
fun AvatarRing(
    avatarIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    ringPadding: Dp = 3.dp,
    isActive: Boolean = false,
    isEmpty: Boolean = false,
    isOut: Boolean = false,
    glowScale: Float = 0f
) {
    val spec = AvatarSpecs[avatarIndex.coerceIn(0, 5)]
    val ringColor = if (isEmpty) Color.White.copy(alpha = 0.30f) else spec.ring

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(
                if (!isEmpty) {
                    Modifier.drawBehind {
                        val extra = if (isActive) (4f + glowScale * 6f) else 0f
                        drawCircle(
                            color = spec.ring.copy(alpha = if (isActive) 0.55f else 0.35f),
                            radius = this.size.minDimension * 0.5f + extra
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = if (isActive) 0.95f else 0.65f),
                            radius = this.size.minDimension * 0.5f + 2f
                        )
                    }
                } else Modifier
            )
            .clip(CircleShape)
            .background(ringColor)
            .padding(ringPadding)
            .clip(CircleShape)
            .background(if (isEmpty) Color.Transparent else Color.White)
            .then(
                if (isEmpty) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape) else Modifier
            )
    ) {
        if (!isEmpty) {
            AvatarCanvas(spec = spec, isOut = isOut, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun AvatarCanvas(
    spec: AvatarSpec,
    modifier: Modifier = Modifier,
    isOut: Boolean = false
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 100f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)

        val clipCircle = Path().apply {
            addOval(Rect(0f, 0f, size.width, size.height))
        }
        clipPath(clipCircle) {
            // Background
            drawRect(color = spec.bg)

            // Back Hair
            val backHairPath = Path().apply {
                when (spec.style) {
                    HairStyle.LONG -> {
                        moveTo(20f * s, 52f * s)
                        cubicTo(16f * s, 24f * s, 32f * s, 12f * s, 50f * s, 12f * s)
                        cubicTo(68f * s, 12f * s, 84f * s, 24f * s, 80f * s, 52f * s)
                        cubicTo(80f * s, 70f * s, 86f * s, 84f * s, 88f * s, 94f * s)
                        lineTo(12f * s, 94f * s)
                        cubicTo(14f * s, 84f * s, 20f * s, 70f * s, 20f * s, 52f * s)
                        close()
                    }
                    HairStyle.BOB -> {
                        moveTo(18f * s, 54f * s)
                        cubicTo(14f * s, 26f * s, 30f * s, 12f * s, 50f * s, 12f * s)
                        cubicTo(70f * s, 12f * s, 86f * s, 26f * s, 82f * s, 54f * s)
                        cubicTo(82f * s, 66f * s, 84f * s, 72f * s, 80f * s, 78f * s)
                        lineTo(20f * s, 78f * s)
                        cubicTo(16f * s, 72f * s, 18f * s, 66f * s, 18f * s, 54f * s)
                        close()
                    }
                    HairStyle.PONY -> {
                        moveTo(22f * s, 50f * s)
                        cubicTo(20f * s, 26f * s, 34f * s, 14f * s, 50f * s, 14f * s)
                        cubicTo(66f * s, 14f * s, 80f * s, 26f * s, 78f * s, 50f * s)
                        close()
                    }
                }
            }
            drawPath(backHairPath, color = spec.hair)
            if (spec.style == HairStyle.PONY) {
                drawCircle(color = spec.hair, radius = 10f * s, center = p(84f, 56f))
                val tail = Path().apply {
                    moveTo(80f * s, 62f * s)
                    quadraticTo(90f * s, 76f * s, 82f * s, 88f * s)
                }
                drawPath(tail, color = spec.hair, style = Stroke(width = 8f * s, cap = StrokeCap.Round))
            }

            // Shirt / Shoulders
            val shirt = Path().apply {
                moveTo(12f * s, 102f * s)
                cubicTo(12f * s, 80f * s, 30f * s, 72f * s, 50f * s, 72f * s)
                cubicTo(70f * s, 72f * s, 88f * s, 80f * s, 88f * s, 102f * s)
                close()
            }
            drawPath(shirt, color = spec.top)

            // Neck + Collar V
            drawRoundRect(
                color = spec.skin,
                topLeft = p(43f, 62f),
                size = Size(14f * s, 14f * s),
                cornerRadius = CornerRadius(5f * s, 5f * s)
            )
            val collar = Path().apply {
                moveTo(38f * s, 72f * s)
                lineTo(50f * s, 86f * s)
                lineTo(62f * s, 72f * s)
                close()
            }
            drawPath(collar, color = spec.skin)

            // Head & Ears
            drawOval(color = spec.skin, topLeft = p(28f, 21f), size = Size(44f * s, 50f * s))
            drawCircle(color = spec.skin, radius = 4f * s, center = p(28f, 49f))
            drawCircle(color = spec.skin, radius = 4f * s, center = p(72f, 49f))

            // Cheeks
            drawCircle(color = Color(0xFFFF8FA0).copy(alpha = 0.55f), radius = 4.5f * s, center = p(35f, 57f))
            drawCircle(color = Color(0xFFFF8FA0).copy(alpha = 0.55f), radius = 4.5f * s, center = p(65f, 57f))

            // Eyes
            drawOval(color = Color.White, topLeft = p(35f, 40.8f), size = Size(10f * s, 12.4f * s))
            drawCircle(color = Color(0xFF2B1A12), radius = 3.4f * s, center = p(41f, 48f))
            drawCircle(color = Color.White, radius = 1.2f * s, center = p(42.2f, 46.4f))

            drawOval(color = Color.White, topLeft = p(55f, 40.8f), size = Size(10f * s, 12.4f * s))
            drawCircle(color = Color(0xFF2B1A12), radius = 3.4f * s, center = p(61f, 48f))
            drawCircle(color = Color.White, radius = 1.2f * s, center = p(62.2f, 46.4f))

            // Brows
            val brows = Path().apply {
                moveTo(34f * s, 42f * s)
                quadraticTo(40f * s, 37f * s, 46f * s, 41f * s)
                moveTo(54f * s, 41f * s)
                quadraticTo(60f * s, 37f * s, 66f * s, 42f * s)
            }
            drawPath(brows, color = Color(0xFF2B1A12), style = Stroke(width = 1.9f * s, cap = StrokeCap.Round))

            // Nose
            val nose = Path().apply {
                moveTo(50f * s, 53f * s)
                quadraticTo(47.5f * s, 56.5f * s, 51f * s, 57.5f * s)
            }
            drawPath(nose, color = Color(0xFFC98A6A), style = Stroke(width = 1.5f * s, cap = StrokeCap.Round))

            // Smile
            val mouth = Path().apply {
                moveTo(42f * s, 62f * s)
                quadraticTo(50f * s, 70f * s, 58f * s, 62f * s)
                quadraticTo(50f * s, 65f * s, 42f * s, 62f * s)
                close()
            }
            drawPath(mouth, color = Color(0xFFD6334A))
            drawPath(mouth, color = Color(0xFFB02038), style = Stroke(width = 1f * s, join = StrokeJoin.Round))

            // Front bangs
            val bangs = Path().apply {
                moveTo(28f * s, 46f * s)
                cubicTo(28f * s, 27f * s, 40f * s, 20f * s, 50f * s, 20f * s)
                cubicTo(60f * s, 20f * s, 72f * s, 27f * s, 72f * s, 46f * s)
                cubicTo(66f * s, 37f * s, 57f * s, 31f * s, 46f * s, 29f * s)
                cubicTo(40f * s, 37f * s, 33f * s, 42f * s, 28f * s, 46f * s)
                close()
            }
            drawPath(bangs, color = spec.hair)

            // Hats
            when (spec.hat) {
                HatStyle.CAP -> {
                    val crown = Path().apply {
                        moveTo(26f * s, 36f * s)
                        cubicTo(26f * s, 16f * s, 38f * s, 10f * s, 50f * s, 10f * s)
                        cubicTo(62f * s, 10f * s, 74f * s, 16f * s, 74f * s, 36f * s)
                        close()
                    }
                    val brim = Path().apply {
                        moveTo(22f * s, 36f * s)
                        quadraticTo(50f * s, 30f * s, 84f * s, 40f * s)
                        quadraticTo(84f * s, 45f * s, 50f * s, 40f * s)
                        quadraticTo(30f * s, 38f * s, 22f * s, 40f * s)
                        close()
                    }
                    drawPath(crown, color = spec.hatColor)
                    drawPath(brim, color = spec.hatColor)
                    drawCircle(color = Color(0xFFFF8A1F), radius = 3f * s, center = p(50f, 12f))
                }
                HatStyle.FEDORA -> {
                    val crown = Path().apply {
                        moveTo(32f * s, 32f * s)
                        cubicTo(32f * s, 12f * s, 42f * s, 6f * s, 50f * s, 6f * s)
                        cubicTo(58f * s, 6f * s, 68f * s, 12f * s, 68f * s, 32f * s)
                        close()
                    }
                    drawPath(crown, color = spec.hatColor)
                    drawRect(color = Color(0xFFFF8A1F), topLeft = p(32f, 24f), size = Size(36f * s, 7f * s))
                    drawOval(color = spec.hatColor, topLeft = p(12f, 25f), size = Size(76f * s, 16f * s))
                    drawOval(
                        color = Color.White.copy(alpha = 0.18f),
                        topLeft = p(28f, 27.5f),
                        size = Size(36f * s, 5f * s)
                    )
                }
                HatStyle.NONE -> Unit
            }

            if (isOut) {
                drawRect(color = Color(0x99101022))
            }
        }
    }
}

@Composable
fun HeartIcon(
    lost: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 32f
        val sy = size.height / 30f
        val path = Path().apply {
            moveTo(16f * sx, 29f * sy)
            cubicTo(16f * sx, 29f * sy, 2f * sx, 19.5f * sy, 2f * sx, 9.8f * sy)
            cubicTo(2f * sx, 5f * sy, 5.5f * sx, 2f * sy, 9.4f * sx, 2f * sy)
            cubicTo(12.3f * sx, 2f * sy, 14.8f * sx, 3.6f * sy, 16f * sx, 6f * sy)
            cubicTo(17.2f * sx, 3.6f * sy, 19.7f * sx, 2f * sy, 22.6f * sx, 2f * sy)
            cubicTo(26.5f * sx, 2f * sy, 30f * sx, 5f * sy, 30f * sx, 9.8f * sy)
            cubicTo(30f * sx, 19.5f * sy, 16f * sx, 29f * sy, 16f * sx, 29f * sy)
            close()
        }
        if (lost) {
            drawPath(path, color = Color(0xFF4A4F7A))
            drawPath(path, color = Color(0xFF2B2F55), style = Stroke(width = 2.4f * sx, join = StrokeJoin.Round))
        } else {
            drawPath(
                path,
                brush = Brush.verticalGradient(listOf(Color(0xFFFF9AAA), Color(0xFFFF3B4E)))
            )
            drawPath(path, color = Color.White, style = Stroke(width = 2.4f * sx, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun BombBurstGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 64f
        val sy = size.height / 64f
        val path = Path().apply {
            moveTo(32f * sx, 2f * sy)
            lineTo(38f * sx, 20f * sy)
            lineTo(56f * sx, 12f * sy)
            lineTo(46f * sx, 28f * sy)
            lineTo(62f * sx, 32f * sy)
            lineTo(46f * sx, 36f * sy)
            lineTo(56f * sx, 52f * sy)
            lineTo(38f * sx, 44f * sy)
            lineTo(32f * sx, 62f * sy)
            lineTo(26f * sx, 44f * sy)
            lineTo(8f * sx, 52f * sy)
            lineTo(18f * sx, 36f * sy)
            lineTo(2f * sx, 32f * sy)
            lineTo(18f * sx, 28f * sy)
            lineTo(8f * sx, 12f * sy)
            lineTo(26f * sx, 20f * sy)
            close()
        }
        drawPath(path, color = Color(0xFFFFD93D))
        drawPath(path, color = Color.White, style = Stroke(width = 3f * sx, join = StrokeJoin.Round))
        drawCircle(color = Color(0xFFFF7A30), radius = 9f * sx, center = Offset(32f * sx, 32f * sy))
    }
}

@Composable
fun HandPointerGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 60f
        val sy = size.height / 70f
        val path = Path().apply {
            moveTo(22f * sx, 6f * sy)
            cubicTo(26f * sx, 3f * sy, 31f * sx, 6f * sy, 31f * sx, 11f * sy)
            lineTo(31f * sx, 29f * sy)
            lineTo(35f * sx, 27f * sy)
            cubicTo(39f * sx, 25f * sy, 43f * sx, 28f * sy, 41f * sx, 32f * sy)
            lineTo(49f * sx, 29f * sy)
            cubicTo(53f * sx, 28f * sy, 56f * sx, 32f * sy, 54f * sx, 35f * sy)
            lineTo(57f * sx, 34f * sy)
            cubicTo(61f * sx, 33f * sy, 63f * sx, 37f * sy, 61f * sx, 41f * sy)
            lineTo(52f * sx, 66f * sy)
            cubicTo(50f * sx, 71f * sy, 45f * sx, 74f * sy, 40f * sx, 74f * sy)
            lineTo(30f * sx, 74f * sy)
            cubicTo(24f * sx, 74f * sy, 20f * sx, 71f * sy, 17f * sx, 66f * sy)
            lineTo(6f * sx, 40f * sy)
            cubicTo(4f * sx, 36f * sy, 7f * sx, 32f * sy, 11f * sx, 33f * sy)
            lineTo(18f * sx, 36f * sy)
            lineTo(18f * sx, 11f * sy)
            cubicTo(18f * sx, 9f * sy, 19f * sx, 7f * sy, 22f * sx, 6f * sy)
            close()
        }
        drawPath(path, color = Color.White)
        drawPath(path, color = Blue, style = Stroke(width = 3f * sx, join = StrokeJoin.Round))
    }
}
