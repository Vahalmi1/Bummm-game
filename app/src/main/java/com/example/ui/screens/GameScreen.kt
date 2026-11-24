package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.BurstBanner
import com.example.model.CellKind
import com.example.model.CountdownBanner
import com.example.model.FloatingCallout
import com.example.model.GameMessage
import com.example.model.GamePhase
import com.example.model.GameUiState
import com.example.model.MessageTone
import com.example.model.PadButtonState
import com.example.model.PadFeedback
import com.example.model.PlayerState
import com.example.model.WallCell
import com.example.ui.components.AvatarRing
import com.example.ui.components.BombBurstGlyph
import com.example.ui.components.BummmTitleBanner
import com.example.ui.components.HandPointerGlyph
import com.example.ui.components.HeartIcon
import com.example.ui.components.RoundButtonStyle
import com.example.ui.components.RoundIconButton
import com.example.ui.components.StarburstCanvas
import com.example.ui.theme.BallPalettes
import com.example.ui.theme.Blue
import com.example.ui.theme.LedBg
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LedGreen
import com.example.ui.theme.LedRed
import com.example.ui.theme.LedYellow
import com.example.ui.theme.LilitaOneFont
import com.example.ui.theme.Navy
import com.example.ui.theme.NunitoFont
import com.example.ui.theme.Orange
import com.example.ui.theme.OrbitronFont
import com.example.ui.theme.Sky
import com.example.ui.theme.Yellow
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

private val SeatPositions = listOf(
    6f to 214f,   // 0: L0
    320f to 270f, // 1: R0
    6f to 284f,   // 2: L1
    320f to 338f, // 3: R1
    6f to 354f,   // 4: L2
    320f to 406f  // 5: R2
)

@Composable
fun GameScreen(
    state: GameUiState,
    onTapNumber: (Int) -> Unit,
    onTapBum: () -> Unit,
    onPauseClick: () -> Unit,
    onWallBoundsChanged: (Offset) -> Unit,
    onPadButtonBoundsChanged: (Int, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    // Artboard shake on fail/end
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.shakeCounter) {
        if (state.shakeCounter > 0) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 450
                    -8f at 67
                    8f at 135
                    -6f at 202
                    5f at 270
                    -1f at 360
                    0f at 450
                }
            )
        }
    }

    // Red flash overlay on mistake
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(state.flashRedCounter) {
        if (state.flashRedCounter > 0) {
            flashAlpha.snapTo(1f)
            flashAlpha.animateTo(0f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        }
    }

    // Burst animation state
    var activeBurst by remember { mutableStateOf<BurstBanner?>(null) }
    val burstScale = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }
    val burstRot = remember { Animatable(-30f) }
    LaunchedEffect(state.burstBanner?.id) {
        val b = state.burstBanner ?: return@LaunchedEffect
        activeBurst = b
        burstScale.snapTo(0f)
        burstAlpha.snapTo(1f)
        burstRot.snapTo(-30f)
        // Burst keyframes: 0 -> 1.15 (220ms) -> 1.0 (650ms) -> 1.25 (1000ms)
        burstScale.animateTo(
            targetValue = 1.25f,
            animationSpec = keyframes {
                durationMillis = 1000
                0f at 0
                1.15f at 220
                1f at 650
                1.25f at 1000
            }
        )
        burstAlpha.snapTo(0f)
        activeBurst = null
    }
    LaunchedEffect(state.burstBanner?.id) {
        val b = state.burstBanner ?: return@LaunchedEffect
        if (activeBurst?.id == b.id || true) {
            burstRot.animateTo(
                targetValue = -2f,
                animationSpec = keyframes {
                    durationMillis = 650
                    -30f at 0
                    4f at 220
                    -2f at 650
                }
            )
        }
    }
    LaunchedEffect(state.burstBanner?.id) {
        if (state.burstBanner == null) return@LaunchedEffect
        delay(650L)
        burstAlpha.animateTo(0f, animationSpec = tween(350))
    }

    // Countdown banner animation
    val cdScale = remember { Animatable(2f) }
    val cdAlpha = remember { Animatable(0f) }
    LaunchedEffect(state.countdownBanner?.id) {
        val cd = state.countdownBanner ?: return@LaunchedEffect
        val dur = cd.durationMs.coerceAtLeast(300)
        cdScale.snapTo(2f)
        cdAlpha.snapTo(0f)
        cdScale.animateTo(
            targetValue = 0.9f,
            animationSpec = keyframes {
                durationMillis = dur
                2f at 0
                1f at (dur * 0.2f).toInt()
                1f at (dur * 0.85f).toInt()
                0.9f at dur
            }
        )
    }
    LaunchedEffect(state.countdownBanner?.id) {
        val cd = state.countdownBanner ?: return@LaunchedEffect
        val dur = cd.durationMs.coerceAtLeast(300)
        cdAlpha.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = dur
                0f at 0
                1f at (dur * 0.2f).toInt()
                1f at (dur * 0.85f).toInt()
                0f at dur
            }
        )
    }

    // Infinite animations for hints & active ring
    val inf = rememberInfiniteTransition(label = "gameInf")
    val ringGlow by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ringGlow"
    )
    val arrowBounce by inf.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "arrowBounce"
    )
    val hintPulse by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintPulse"
    )
    val handTap by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "handTap"
    )

    // Track pad button local centers inside 390x780 artboard for hand pointer placement
    val padButtonLocalCenters = remember { mutableStateMapOf<Int, Offset>() }
    var artboardRootTopLeft by remember { mutableStateOf(Offset.Zero) }
    var currentScale by remember { mutableFloatStateOf(1f) }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val scale = minOf(maxWidth.value / 390f, maxHeight.value / 780f, 1.7f)
        currentScale = scale

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size((390f * scale).dp, (780f * scale).dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(390.dp, 780.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = shakeOffset.value * density
                    }
                    .onGloballyPositioned { coords ->
                        artboardRootTopLeft = coords.boundsInRoot().topLeft
                    }
            ) {
                // 1. Title Banner (0, 0, 390x108)
                BummmTitleBanner(
                    modifier = Modifier
                        .offset(x = 0.dp, y = 0.dp)
                        .size(390.dp, 108.dp)
                )

                // 2. Level Ribbon (centered at top 104)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 104.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF2A4FD6), Blue)))
                        .border(2.5.dp, Sky, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "LEVEL ${state.level}  ·  MAGIC ${state.magic}  ·  START AT ${state.from}",
                        style = TextStyle(
                            fontFamily = LilitaOneFont,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.4.sp,
                            color = Color.White,
                            shadow = Shadow(color = Navy, offset = Offset(0f, 2f), blurRadius = 0f)
                        ),
                        maxLines = 1,
                        modifier = Modifier.testTag("level_ribbon")
                    )
                }

                // 3. Timer Progress Bar (left 30, top 128, 330x9)
                Box(
                    modifier = Modifier
                        .offset(x = 30.dp, y = 128.dp)
                        .size(330.dp, 9.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Navy)
                        .border(2.dp, Sky, RoundedCornerShape(9.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                scaleX = state.timerFraction.coerceIn(0f, 1f)
                            }
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color(0xFF3CD64A),
                                    0.6f to Color(0xFFFFD21F),
                                    1.0f to Color(0xFFFF3B4E)
                                )
                            )
                    )
                }

                // 4. LED Panels
                val activePlayer = state.players.getOrNull(state.turn.coerceAtLeast(0))
                    ?: state.players.firstOrNull()

                LedPanel(
                    label = "NUMBERS",
                    value = state.totalCount.toString(),
                    borderColor = LedGreen,
                    textColor = LedGreen,
                    pulseTrigger = 0,
                    modifier = Modifier.offset(x = 6.dp, y = 146.dp)
                )
                LedPanel(
                    label = "POINTS",
                    value = (activePlayer?.score ?: 0).toString(),
                    borderColor = Color(0xFFFF5A5A),
                    textColor = LedRed,
                    pulseTrigger = state.ptsPulseCounter,
                    modifier = Modifier.offset(x = 320.dp, y = 146.dp)
                )
                LedPanel(
                    label = "COMBO",
                    value = (activePlayer?.combo ?: 0).toString(),
                    borderColor = LedGreen,
                    textColor = LedGreen,
                    pulseTrigger = state.comboPulseCounter,
                    modifier = Modifier.offset(x = 320.dp, y = 208.dp)
                )

                // 5. 6 Player Seats
                for (i in 0 until 6) {
                    val (sx, sy) = SeatPositions[i]
                    val isLeft = i % 2 == 0
                    val player = state.players.getOrNull(i)
                    val isActive = player != null && i == state.turn && player.alive
                    PlayerSeatView(
                        player = player,
                        isLeft = isLeft,
                        isActive = isActive,
                        ringGlow = ringGlow,
                        arrowBounce = arrowBounce,
                        modifier = Modifier
                            .offset(x = sx.dp, y = sy.dp)
                            .zIndex(if (isActive) 5f else 1f)
                    )
                }

                // 6. Center Wall (left 74, top 150, 242x234)
                CenterWall(
                    state = state,
                    modifier = Modifier
                        .offset(x = 74.dp, y = 150.dp)
                        .size(242.dp, 234.dp)
                        .onGloballyPositioned { coords ->
                            onWallBoundsChanged(coords.boundsInRoot().center)
                        }
                )

                // 7. NOW & MAGIC Badges
                OrbBadge(
                    number = state.currentNumber,
                    label = "NOW",
                    isMagic = false,
                    pulseTrigger = state.badgeNowPulseCounter,
                    modifier = Modifier
                        .offset(x = 62.dp, y = 136.dp)
                        .zIndex(6f)
                        .testTag("badge_now")
                )
                OrbBadge(
                    number = state.magic,
                    label = "MAGIC",
                    isMagic = true,
                    pulseTrigger = state.badgeMagicPulseCounter,
                    modifier = Modifier
                        .offset(x = 288.dp, y = 136.dp)
                        .zIndex(6f)
                        .testTag("badge_magic")
                )

                // 8. Starburst Explosion Overlay (left 95, top 167, 200x200)
                if (activeBurst != null && burstAlpha.value > 0.01f) {
                    val b = activeBurst!!
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = 95.dp, y = 167.dp)
                            .size(200.dp)
                            .zIndex(20f)
                            .graphicsLayer {
                                alpha = burstAlpha.value
                                scaleX = burstScale.value
                                scaleY = burstScale.value
                                rotationZ = burstRot.value
                            }
                    ) {
                        StarburstCanvas(
                            c1 = if (b.isBad) Color(0xFFB3123B) else Color(0xFFFF3B1F),
                            c2 = if (b.isBad) Color(0xFFFF4565) else Color(0xFFFF9A0A),
                            c3 = if (b.isBad) Color(0xFFFFC2CC) else Color(0xFFFFE95C),
                            strokeColor = if (b.isBad) Color(0xFF3C0018) else Color(0xFF7A1500),
                            withWhiteCore = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = b.text,
                            style = TextStyle(
                                fontFamily = LilitaOneFont,
                                fontSize = 32.sp,
                                color = Color.White,
                                shadow = Shadow(color = Color(0xFF7A1500), offset = Offset(0f, 6f), blurRadius = 0f)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { rotationZ = -6f }
                        )
                    }
                }

                // 9. Message Box (left 74, top 392, 242x62)
                MessageBoxView(
                    message = state.message,
                    modifier = Modifier
                        .offset(x = 74.dp, y = 392.dp)
                        .size(242.dp, 62.dp)
                        .testTag("game_message_box")
                )

                // 10. 3D Perspective Number Pad (left 6, top 466, 378x234)
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .offset(x = 6.dp, y = 466.dp)
                        .size(378.dp, 234.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(378.dp, 266.dp)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                rotationX = 34f
                                cameraDistance = 16f * density
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF1A2A8A), Color(0xFF0E1A5C))))
                            .border(7.dp, Color(0xFF3C7BFF), RoundedCornerShape(20.dp))
                            .drawBehind {
                                // Subtle grid lines
                                val step = 30.dp.toPx()
                                val lineCol = Sky.copy(alpha = 0.10f)
                                var gx = step
                                while (gx < size.width) {
                                    drawLine(lineCol, Offset(gx, 0f), Offset(gx, size.height), 1f)
                                    gx += step
                                }
                                var gy = step
                                while (gy < size.height) {
                                    drawLine(lineCol, Offset(0f, gy), Offset(size.width, gy), 1f)
                                    gy += step
                                }
                            }
                            .padding(8.dp)
                    ) {
                        PadGridView(
                            cols = state.padCols,
                            buttons = state.padButtons,
                            locked = state.padLocked,
                            hintPulse = hintPulse,
                            onTapNumber = onTapNumber,
                            onButtonPositioned = { num, rootCenter ->
                                onPadButtonBoundsChanged(num, rootCenter)
                                val localCenter = Offset(
                                    (rootCenter.x - artboardRootTopLeft.x) / (currentScale * density),
                                    (rootCenter.y - artboardRootTopLeft.y) / (currentScale * density)
                                )
                                padButtonLocalCenters[num] = localCenter
                            }
                        )
                    }
                }

                // 11. Bottom Control Bar (left 6, top 704, 378x72)
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = 704.dp)
                        .size(378.dp, 72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF2A4FD6), Blue)))
                        .border(4.dp, Sky, RoundedCornerShape(18.dp))
                )

                RoundIconButton(
                    onClick = onPauseClick,
                    style = RoundButtonStyle.GREEN,
                    size = 50.dp,
                    testTag = "pause_button",
                    modifier = Modifier.offset(x = 20.dp, y = 714.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = "Pause game",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                BottomTile(
                    label = "LEVEL",
                    value = state.level.toString(),
                    textColor = LedCyan,
                    modifier = Modifier.offset(x = 80.dp, y = 715.dp)
                )
                BottomTile(
                    label = "TIME",
                    value = state.timeText,
                    textColor = LedYellow,
                    modifier = Modifier.offset(x = 148.dp, y = 715.dp)
                )
                BottomTile(
                    label = "ALIVE",
                    value = state.players.count { it.alive }.toString(),
                    textColor = LedGreen,
                    modifier = Modifier.offset(x = 216.dp, y = 715.dp)
                )

                // Big Red BUMMM! Button (left 296, top 690, 90x90)
                BummmActionButton(
                    feedback = state.bumFeedback,
                    isHint = state.bumHint,
                    hintPulse = hintPulse,
                    onClick = onTapBum,
                    modifier = Modifier
                        .offset(x = 296.dp, y = 690.dp)
                        .zIndex(8f)
                        .testTag("bummm_button")
                )

                // 12. Floating Callouts (+15, -heart, OUT!, COMBO x5!)
                state.floatingCallouts.forEach { callout ->
                    FloatingCalloutView(
                        callout = callout,
                        modifier = Modifier.zIndex(30f)
                    )
                }

                // 13. Tutorial Hand Pointer
                if (state.showHandHint) {
                    val targetPos = if (state.handTargetIsBum) {
                        Offset(341f, 735f)
                    } else {
                        state.handTargetNumber?.let { padButtonLocalCenters[it] } ?: Offset(195f, 580f)
                    }
                    val handX by animateDpAsState(
                        targetValue = (targetPos.x - 12f).dp,
                        animationSpec = tween(250),
                        label = "handX"
                    )
                    val handY by animateDpAsState(
                        targetValue = (targetPos.y - 2f).dp,
                        animationSpec = tween(250),
                        label = "handY"
                    )
                    HandPointerGlyph(
                        modifier = Modifier
                            .offset(x = handX, y = handY)
                            .offset(x = (-4f * handTap).dp, y = (-8f * handTap).dp)
                            .size(46.dp, 54.dp)
                            .scale(1f - 0.08f * handTap)
                            .zIndex(40f)
                    )
                }

                // 14. Countdown / Level-Up Banner
                state.countdownBanner?.let { cd ->
                    if (cdAlpha.value > 0.01f) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .offset(x = 0.dp, y = 230.dp)
                                .width(390.dp)
                                .zIndex(50f)
                                .graphicsLayer {
                                    alpha = cdAlpha.value
                                    scaleX = cdScale.value
                                    scaleY = cdScale.value
                                }
                        ) {
                            Text(
                                text = cd.bigText,
                                style = TextStyle(
                                    fontFamily = LilitaOneFont,
                                    fontSize = if (cd.isLevelBanner) 60.sp else 112.sp,
                                    color = Color.White,
                                    shadow = Shadow(color = Orange, offset = Offset(0f, 12f), blurRadius = 0f)
                                ),
                                textAlign = TextAlign.Center
                            )
                            if (cd.smallText.isNotEmpty()) {
                                Text(
                                    text = cd.smallText,
                                    style = TextStyle(
                                        fontFamily = LilitaOneFont,
                                        fontSize = 21.sp,
                                        lineHeight = 25.sp,
                                        color = Color.White,
                                        shadow = Shadow(color = Blue, offset = Offset(0f, 6f), blurRadius = 8f)
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 15. Red Flash Overlay
                if (flashAlpha.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-100).dp, y = (-100).dp)
                            .requiredSize(590.dp, 980.dp)
                            .zIndex(45f)
                            .alpha(flashAlpha.value)
                            .background(
                                Brush.radialGradient(
                                    0.25f to Color(0x26FF3B4E),
                                    1.0f to Color(0xA6FF3B4E)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun LedPanel(
    label: String,
    value: String,
    borderColor: Color,
    textColor: Color,
    pulseTrigger: Int,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            scale.snapTo(1.2f)
            scale.animateTo(1f, animationSpec = tween(400))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3E6BFF), Color(0xFF1E3BC4))))
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(start = 4.dp, end = 4.dp, top = 3.dp, bottom = 5.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 9.5.sp,
                letterSpacing = 0.4.sp,
                color = Color.White,
                shadow = Shadow(color = Navy, offset = Offset(0f, 2f), blurRadius = 2f)
            ),
            maxLines = 1
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LedBg)
                .border(2.dp, Navy, RoundedCornerShape(6.dp))
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = OrbitronFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp,
                    color = textColor,
                    shadow = Shadow(color = textColor, offset = Offset.Zero, blurRadius = 8f)
                ),
                maxLines = 1,
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}

@Composable
private fun PlayerSeatView(
    player: PlayerState?,
    isLeft: Boolean,
    isActive: Boolean,
    ringGlow: Float,
    arrowBounce: Float,
    modifier: Modifier = Modifier
) {
    val seatShake = remember { Animatable(0f) }
    LaunchedEffect(player?.shakeKey) {
        if ((player?.shakeKey ?: 0) > 0) {
            seatShake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    -5f at 80
                    5f at 160
                    -5f at 240
                    5f at 320
                    0f at 400
                }
            )
        }
    }

    val isEmpty = player == null
    val isOut = player != null && !player.alive

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .size(64.dp, 68.dp)
            .alpha(if (isEmpty) 0.35f else if (isOut) 0.6f else 1f)
            .scale(if (isActive) 1.12f else 1f)
            .offset(x = seatShake.value.dp)
    ) {
        if (isActive) {
            Text(
                text = if (isLeft) "▶" else "◀",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = Color.White,
                    shadow = Shadow(color = Navy, offset = Offset(0f, 3f), blurRadius = 6f)
                ),
                modifier = Modifier
                    .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                    .offset(
                        x = if (isLeft) (-10 + arrowBounce).dp else (10 - arrowBounce).dp,
                        y = 14.dp
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AvatarRing(
                    avatarIndex = player?.av ?: 0,
                    size = 46.dp,
                    ringPadding = 3.dp,
                    isActive = isActive,
                    isEmpty = isEmpty,
                    isOut = isOut,
                    glowScale = ringGlow
                )
                if (isOut) {
                    Text(text = "💥", fontSize = 22.sp)
                }
            }

            if (player != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.height(11.dp)
                ) {
                    for (j in 0 until 3) {
                        HeartIcon(
                            lost = j >= player.lives,
                            modifier = Modifier.size(11.dp, 10.dp)
                        )
                    }
                }
                Text(
                    text = player.name,
                    style = TextStyle(
                        fontFamily = LilitaOneFont,
                        fontSize = 10.sp,
                        color = Color.White,
                        shadow = Shadow(color = Navy, offset = Offset(0f, 2f), blurRadius = 4f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CenterWall(
    state: GameUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A4FD6), Blue)))
            .border(4.dp, Sky, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(9.dp))
                .background(Navy)
                .drawBehind {
                    val step = 26.dp.toPx()
                    val lineCol = Sky.copy(alpha = 0.12f)
                    var gx = step
                    while (gx < size.width) {
                        drawLine(lineCol, Offset(gx, 0f), Offset(gx, size.height), 1f)
                        gx += step
                    }
                    var gy = step
                    while (gy < size.height) {
                        drawLine(lineCol, Offset(0f, gy), Offset(size.width, gy), 1f)
                        gy += step
                    }
                }
                .padding(horizontal = 4.dp, vertical = 7.dp)
        ) {
            StackColumn(
                label = "LAST",
                items = state.lastStack,
                textColor = Color(0xFFFF5A5A)
            )
            WallGrid(
                cells = state.wallCells,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            StackColumn(
                label = "NEXT",
                items = state.nextStack,
                textColor = Color(0xFFFFB02B)
            )
        }
    }
}

@Composable
private fun StackColumn(
    label: String,
    items: List<String>,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .width(20.dp)
            .padding(top = 6.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 7.sp,
                color = Color(0xFF9FB4FF)
            ),
            maxLines = 1
        )
        Spacer(Modifier.height(10.dp))
        items.forEach { txt ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(LedBg)
                    .border(2.dp, Color(0xFF3B4FB8), RoundedCornerShape(5.dp))
            ) {
                Text(
                    text = txt,
                    style = TextStyle(
                        fontFamily = OrbitronFont,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        color = textColor,
                        shadow = Shadow(color = textColor, offset = Offset.Zero, blurRadius = 5f)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WallGrid(
    cells: List<WallCell>,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        for (row in 0 until 7) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (col in 0 until 6) {
                    val cell = cells.getOrNull(row * 6 + col) ?: WallCell()
                    WallCellItem(
                        cell = cell,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun WallCellItem(
    cell: WallCell,
    modifier: Modifier = Modifier
) {
    val popScale = remember { Animatable(1f) }
    LaunchedEffect(cell.kind, cell.number) {
        if (cell.kind != CellKind.EMPTY) {
            popScale.snapTo(0.2f)
            popScale.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        when (cell.kind) {
            CellKind.EMPTY -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF1A2A80), Color(0xFF0A1240))))
                )
            }
            CellKind.FAIL -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(popScale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                0.0f to Color(0xAAFFFFFF),
                                0.3f to Color(0xFF8A1A2D),
                                1.0f to Color(0xFF3B0612)
                            )
                        )
                ) {
                    Text(
                        text = "✖",
                        style = TextStyle(fontFamily = LilitaOneFont, fontSize = 11.sp, color = Color.White)
                    )
                }
            }
            CellKind.NUMBER, CellKind.BOOM -> {
                val pal = BallPalettes[cell.colorIndex.coerceIn(0, 7)]
                val numStr = cell.number.toString()
                val fs = when {
                    numStr.length >= 4 -> 7.5.sp
                    numStr.length == 3 -> 9.5.sp
                    else -> 11.sp
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(popScale.value)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    0.0f to Color.White.copy(alpha = 0.85f),
                                    0.32f to pal.c1,
                                    1.0f to pal.c2
                                )
                            )
                    ) {
                        Text(
                            text = numStr,
                            style = TextStyle(
                                fontFamily = LilitaOneFont,
                                fontSize = fs,
                                color = Color.White,
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 2f)
                            ),
                            maxLines = 1
                        )
                    }
                    if (cell.kind == CellKind.BOOM) {
                        Text(
                            text = "💥",
                            fontSize = 9.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-3).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbBadge(
    number: Int,
    label: String,
    isMagic: Boolean,
    pulseTrigger: Int,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            scale.snapTo(1.35f)
            scale.animateTo(1f, animationSpec = tween(400))
        }
    }
    val txt = number.toString()
    val fs = when {
        txt.length >= 5 -> 10.sp
        txt.length == 4 -> 12.sp
        txt.length == 3 -> 15.sp
        else -> 19.sp
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .scale(scale.value)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isMagic) {
                        Brush.radialGradient(
                            0.0f to Color.White,
                            0.18f to Color(0xFF8CF06A),
                            1.0f to Color(0xFF169A28)
                        )
                    } else {
                        Brush.radialGradient(
                            0.0f to Color.White,
                            0.18f to Color(0xFFFFC93D),
                            1.0f to Color(0xFFFF8A1F)
                        )
                    }
                )
                .border(3.dp, Blue, CircleShape)
        ) {
            Text(
                text = txt,
                style = TextStyle(
                    fontFamily = LilitaOneFont,
                    fontSize = fs,
                    color = Color.White,
                    shadow = Shadow(color = Color(0x99781E00), offset = Offset(0f, 3f), blurRadius = 0f)
                ),
                maxLines = 1
            )
        }
        Text(
            text = label,
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 8.sp,
                color = Color.White,
                shadow = Shadow(color = Navy, offset = Offset(0f, 2f), blurRadius = 3f)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 12.dp)
        )
    }
}

@Composable
private fun MessageBoxView(
    message: GameMessage,
    modifier: Modifier = Modifier
) {
    val borderColor = when (message.tone) {
        MessageTone.GOOD -> LedGreen
        MessageTone.BAD -> Color(0xFFFF5A6A)
        MessageTone.NEUTRAL -> Sky
    }
    val baseTextColor = when (message.tone) {
        MessageTone.GOOD -> Color(0xFFC9FFD2)
        MessageTone.BAD -> Color(0xFFFFD0D6)
        MessageTone.NEUTRAL -> Color.White
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Navy.copy(alpha = 0.86f))
            .border(3.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        val annotated = buildAnnotatedString {
            append(message.prefix)
            if (message.boldPart.isNotEmpty()) {
                withStyle(SpanStyle(color = Yellow, fontWeight = FontWeight.Black)) {
                    append(message.boldPart)
                }
            }
            append(message.suffix)
            if (message.boldSecond.isNotEmpty()) {
                withStyle(SpanStyle(color = Yellow, fontWeight = FontWeight.Black)) {
                    append(message.boldSecond)
                }
            }
            append(message.tail)
        }
        Text(
            text = annotated,
            style = TextStyle(
                fontFamily = NunitoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                color = baseTextColor
            ),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PadGridView(
    cols: Int,
    buttons: List<PadButtonState>,
    locked: Boolean,
    hintPulse: Float,
    onTapNumber: (Int) -> Unit,
    onButtonPositioned: (Int, Offset) -> Unit
) {
    val rows = if (cols <= 0) 0 else (buttons.size + cols - 1) / cols
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (locked) 0.55f else 1f)
    ) {
        for (r in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (c in 0 until cols) {
                    val btn = buttons.getOrNull(r * cols + c)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (btn != null) {
                            PadNumberBall(
                                button = btn,
                                locked = locked,
                                hintPulse = hintPulse,
                                onTap = { onTapNumber(btn.number) },
                                onPositioned = { center -> onButtonPositioned(btn.number, center) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PadNumberBall(
    button: PadButtonState,
    locked: Boolean,
    hintPulse: Float,
    onTap: () -> Unit,
    onPositioned: (Offset) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val pal = BallPalettes[button.colorIndex.coerceIn(0, 7)]
    val c1 = when (button.feedback) {
        PadFeedback.OK -> Color(0xFFB6FF9B)
        PadFeedback.NO -> Color(0xFFFF8A96)
        PadFeedback.NONE -> pal.c1
    }
    val c2 = when (button.feedback) {
        PadFeedback.OK -> Color(0xFF27C93E)
        PadFeedback.NO -> Color(0xFFD1182F)
        PadFeedback.NONE -> pal.c2
    }
    val c3 = pal.c3

    val txt = button.number.toString()
    val fs = when {
        txt.length >= 5 -> 11.sp
        txt.length == 4 -> 13.sp
        txt.length == 3 -> 16.sp
        else -> 19.sp
    }

    val scaleAnim = when {
        button.feedback == PadFeedback.OK -> 1.35f
        button.isHint -> hintPulse
        else -> 1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .scale(scaleAnim)
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInRoot().center)
            }
            .testTag("pad_num_${button.number}")
    ) {
        // Lip shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 4.dp)
                .clip(CircleShape)
                .background(c3)
        )
        // Glossy ball
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (pressed) 3.dp else 0.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.9f),
                        0.30f to c1,
                        1.0f to c2
                    )
                )
                .pointerInput(locked, button.number) {
                    if (!locked) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                onTap()
                                tryAwaitRelease()
                                pressed = false
                            }
                        )
                    }
                }
        ) {
            Text(
                text = txt,
                style = TextStyle(
                    fontFamily = LilitaOneFont,
                    fontSize = fs,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), offset = Offset(0f, 3f), blurRadius = 3f)
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BottomTile(
    label: String,
    value: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .size(62.dp, 50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LedBg)
            .border(3.dp, Sky, RoundedCornerShape(10.dp))
            .padding(top = 3.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 8.sp,
                color = Color(0xFF9FB4FF)
            ),
            maxLines = 1
        )
        Text(
            text = value,
            style = TextStyle(
                fontFamily = OrbitronFont,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = textColor,
                shadow = Shadow(color = textColor, offset = Offset.Zero, blurRadius = 6f)
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun BummmActionButton(
    feedback: PadFeedback,
    isHint: Boolean,
    hintPulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale = when {
        feedback == PadFeedback.OK -> 1.25f
        isHint -> hintPulse * 0.94f
        else -> 1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(90.dp)
            .scale(scale)
    ) {
        // Hard bottom shadow lip
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(y = 6.dp)
                .clip(CircleShape)
                .background(Color(0xFF7A0A1C))
        )
        // Glossy Red Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(90.dp)
                .offset(y = if (pressed) 5.dp else 0.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to Color.White,
                        0.16f to Color(0xFFFF8A7A),
                        0.50f to Color(0xFFFF3B4E),
                        1.0f to Color(0xFFC2102A),
                        center = Offset(30f, 22f),
                        radius = 120f
                    )
                )
                .border(5.dp, Blue, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            onClick()
                            tryAwaitRelease()
                            pressed = false
                        }
                    )
                }
        ) {
            BombBurstGlyph(modifier = Modifier.size(36.dp))
            Text(
                text = "BUMMM!",
                style = TextStyle(
                    fontFamily = LilitaOneFont,
                    fontSize = 16.sp,
                    color = Color.White,
                    shadow = Shadow(color = Color(0xFF7A0A1C), offset = Offset(0f, 3f), blurRadius = 0f)
                )
            )
        }
    }
}

@Composable
private fun FloatingCalloutView(
    callout: FloatingCallout,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(callout.id) {
        progress.animateTo(1f, animationSpec = tween(950, easing = FastOutSlowInEasing))
    }
    if (progress.value >= 0.99f) return

    val pos = SeatPositions.getOrElse(callout.seatIndex) { 195f to 300f }
    val baseX = pos.first + 32f
    val baseY = pos.second + 20f
    val p = progress.value
    val yOffset = -56f * p
    val scale = if (p < 0.25f) 0.7f + (p / 0.25f) * 0.5f else 1.2f - ((p - 0.25f) / 0.75f) * 0.2f
    val alpha = if (p < 0.25f) 1f else (1f - (p - 0.25f) / 0.75f).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset { IntOffset((baseX - 60f).dp.roundToPx(), (baseY + yOffset - 16f).dp.roundToPx()) }
            .width(120.dp)
            .alpha(alpha)
            .scale(scale)
    ) {
        Text(
            text = callout.text,
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 20.sp,
                color = if (callout.isBad) Color(0xFFFF6B7B) else Color.White,
                shadow = Shadow(color = Navy, offset = Offset(0f, 4f), blurRadius = 8f)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
