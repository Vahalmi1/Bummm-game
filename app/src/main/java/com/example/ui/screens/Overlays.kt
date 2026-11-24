package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ComicButton
import com.example.ui.components.ComicButtonStyle
import com.example.ui.theme.Blue
import com.example.ui.theme.LedBg
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LilitaOneFont
import com.example.ui.theme.Navy
import com.example.ui.theme.NunitoFont
import com.example.ui.theme.Sky
import com.example.ui.theme.Yellow

@Composable
fun PauseOverlay(
    visible: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    OverlaySheet(
        visible = visible,
        onDismiss = onResume,
        modifier = modifier
    ) {
        Text(
            text = "PAUSED",
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 32.sp,
                color = Yellow,
                shadow = Shadow(color = Navy, offset = Offset(0f, 5f), blurRadius = 0f)
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The clock is frozen. Take a breath!",
            style = TextStyle(
                fontFamily = NunitoFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        ComicButton(
            text = "RESUME",
            onClick = onResume,
            style = ComicButtonStyle.GREEN,
            fontSize = 26.sp,
            testTag = "pause_resume_button"
        )
        Spacer(Modifier.height(10.dp))
        ComicButton(
            text = "RESTART",
            onClick = onRestart,
            style = ComicButtonStyle.BLUE,
            fontSize = 20.sp,
            small = true,
            testTag = "pause_restart_button"
        )
        Spacer(Modifier.height(10.dp))
        ComicButton(
            text = "QUIT",
            onClick = onQuit,
            style = ComicButtonStyle.BLUE,
            fontSize = 20.sp,
            small = true,
            testTag = "pause_quit_button"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HowToPlayOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    OverlaySheet(
        visible = visible,
        onDismiss = onClose,
        modifier = modifier
    ) {
        Text(
            text = "HOW TO PLAY",
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 32.sp,
                color = Yellow,
                shadow = Shadow(color = Navy, offset = Offset(0f, 5f), blurRadius = 0f)
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        val rules = remember {
            listOf(
                listOf("🎲 " to false, "Take turns" to true, " counting up, just like passing the dice in Ludo. Each turn you get ONE number." to false),
                listOf("🔢 If the number is " to false, "safe" to true, ", find it on the big board and tap it." to false),
                listOf("💣 If it is a " to false, "BUMMM number" to true, ", tap the red " to false, "BUMMM!" to true, " button instead." to false),
                listOf("✨ A number is BUMMM if it is a " to false, "multiple of the magic number" to true, " or " to false, "contains it" to true, ". Level 1 magic number is 3, so 3, 6, 9, 12, 13, 23, 30… are all BUMMM!" to false),
                listOf("❤️ Wrong tap or too slow? You lose a heart. Lose all 3 and you're out. Last one standing wins!" to false),
                listOf("🪜 Every level the magic number grows: " to false, "3, 5, 7, 9, 11…" to true, " forever, and the clock gets faster." to false)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            rules.forEach { parts ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Navy.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            parts.forEach { (seg, isBold) ->
                                if (isBold) {
                                    withStyle(SpanStyle(color = Yellow, fontWeight = FontWeight.Black)) {
                                        append(seg)
                                    }
                                } else {
                                    append(seg)
                                }
                            }
                        },
                        style = TextStyle(
                            fontFamily = NunitoFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val ladder = remember { listOf("Lv1 → 3", "Lv2 → 5", "Lv3 → 7", "Lv4 → 9", "Lv5 → 11", "…") }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ladder.forEach { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LedBg)
                        .border(2.dp, Sky, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = chip,
                        style = TextStyle(
                            fontFamily = LilitaOneFont,
                            fontSize = 13.sp,
                            color = LedCyan
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ComicButton(
            text = "GOT IT!",
            onClick = onClose,
            style = ComicButtonStyle.ORANGE,
            fontSize = 26.sp,
            testTag = "how_close_button"
        )
    }
}

@Composable
private fun OverlaySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Navy.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.9f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
                        ),
                        exit = scaleOut(targetScale = 0.9f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // Hard lip shadow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 10.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Navy)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF2A4FD6), Blue)))
                        .border(5.dp, Sky, RoundedCornerShape(28.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ToastBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(bottom = 30.dp)
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Navy)
                    .border(2.dp, Sky, RoundedCornerShape(99.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.orEmpty(),
                    style = TextStyle(
                        fontFamily = NunitoFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
