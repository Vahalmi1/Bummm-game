package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPrefs
import com.example.ui.components.AvatarRing
import com.example.ui.components.BummmTitleBanner
import com.example.ui.components.ComicButton
import com.example.ui.components.ComicButtonStyle
import com.example.ui.components.RoundButtonStyle
import com.example.ui.components.RoundIconButton
import com.example.ui.components.StarburstCanvas
import com.example.ui.theme.Blue
import com.example.ui.theme.LilitaOneFont
import com.example.ui.theme.NunitoFont
import java.text.NumberFormat

@Composable
fun TitleScreen(
    prefs: AppPrefs,
    onPlayClick: () -> Unit,
    onHowToPlayClick: () -> Unit,
    onToggleSfx: () -> Unit,
    onToggleMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val titleScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "titleScale"
    )
    val titleRot by animateFloatAsState(
        targetValue = if (entered) 0f else -6f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "titleRot"
    )

    val inf = rememberInfiniteTransition(label = "titleAnim")
    val burstPulse by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "burstPulse"
    )
    val playPulse by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playPulse"
    )
    val hopOdd by inf.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hopOdd"
    )
    val hopEven by inf.animateFloat(
        initialValue = -10f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hopEven"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
        ) {
            StarburstCanvas(
                c1 = Color(0xFFFF7A1F),
                c2 = Color(0xFFFFB02B),
                c3 = Color(0xFFFFE95C),
                strokeColor = Color(0xFF7A1500),
                modifier = Modifier
                    .size(270.dp)
                    .scale(burstPulse)
            )
            BummmTitleBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = titleScale
                        scaleY = titleScale
                        rotationZ = titleRot
                    }
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Count together. Don't get BUMMMed!",
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 21.sp,
                color = Color.White,
                shadow = Shadow(color = Blue, offset = Offset(0f, 5f), blurRadius = 8f)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            for (i in 0 until 6) {
                val hopY = if (i % 2 == 0) hopOdd else hopEven
                AvatarRing(
                    avatarIndex = i,
                    size = 48.dp,
                    modifier = Modifier.offset(y = hopY.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            ComicButton(
                text = "PLAY",
                onClick = onPlayClick,
                style = ComicButtonStyle.ORANGE,
                fontSize = 42.sp,
                testTag = "play_button",
                modifier = Modifier.scale(playPulse)
            )

            ComicButton(
                text = "HOW TO PLAY",
                onClick = onHowToPlayClick,
                style = ComicButtonStyle.BLUE,
                fontSize = 26.sp,
                testTag = "how_to_play_button"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    onClick = onToggleSfx,
                    style = if (prefs.sfx) RoundButtonStyle.GREEN else RoundButtonStyle.OFF,
                    testTag = "toggle_sfx_button"
                ) {
                    Icon(
                        imageVector = if (prefs.sfx) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                        contentDescription = if (prefs.sfx) "Mute sound effects" else "Unmute sound effects",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                RoundIconButton(
                    onClick = onToggleMusic,
                    style = if (prefs.music) RoundButtonStyle.GREEN else RoundButtonStyle.OFF,
                    testTag = "toggle_music_button"
                ) {
                    Icon(
                        imageVector = if (prefs.music) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                        contentDescription = if (prefs.music) "Mute music" else "Unmute music",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        val bestText = if (prefs.games > 0) {
            val formattedScore = NumberFormat.getIntegerInstance().format(prefs.bestScore)
            "🏆 Best level ${prefs.bestLevel}  ·  Best score $formattedScore"
        } else {
            "Local multiplayer: pass the device around!"
        }
        Text(
            text = bestText,
            style = TextStyle(
                fontFamily = NunitoFont,
                fontSize = 14.sp,
                color = Color.White,
                shadow = Shadow(color = Blue, offset = Offset(0f, 3f), blurRadius = 0f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("best_score_text")
        )
    }
}
