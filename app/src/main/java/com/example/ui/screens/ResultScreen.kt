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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameUiState
import com.example.model.PlayerState
import com.example.model.SlotType
import com.example.ui.components.AvatarRing
import com.example.ui.components.ComicButton
import com.example.ui.components.ComicButtonStyle
import com.example.ui.theme.Blue
import com.example.ui.theme.LilitaOneFont
import com.example.ui.theme.Navy
import com.example.ui.theme.NunitoFont
import com.example.ui.theme.OrbitronFont
import com.example.ui.theme.Sky
import com.example.ui.theme.Yellow

@Composable
fun ResultScreen(
    state: GameUiState,
    onPlayAgain: () -> Unit,
    onShare: (String) -> Unit,
    onChangePlayers: () -> Unit,
    onTitleScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val solo = state.players.size == 1
    val winner = state.rankedResults.firstOrNull() ?: state.players.firstOrNull()

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val titleScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "resTitleScale"
    )
    val inf = rememberInfiniteTransition(label = "crownHop")
    val crownHop by inf.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "crownY"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = if (solo) "GAME OVER" else "WINNER!",
                style = TextStyle(
                    fontFamily = LilitaOneFont,
                    fontSize = 54.sp,
                    color = Yellow,
                    shadow = Shadow(color = Navy, offset = Offset(0f, 8f), blurRadius = 4f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = titleScale
                        scaleY = titleScale
                    }
                    .testTag("result_title")
            )

            Spacer(Modifier.height(26.dp))

            if (winner != null) {
                Box(contentAlignment = Alignment.TopCenter) {
                    AvatarRing(
                        avatarIndex = winner.av,
                        size = 132.dp,
                        ringPadding = 6.dp
                    )
                    Text(
                        text = "👑",
                        fontSize = 44.sp,
                        modifier = Modifier
                            .offset(y = (-34 + crownHop).dp)
                            .graphicsLayer { rotationZ = -8f }
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (solo) winner.name else "${winner.name} wins!",
                    style = TextStyle(
                        fontFamily = LilitaOneFont,
                        fontSize = 28.sp,
                        color = Color.White,
                        shadow = Shadow(color = Navy, offset = Offset(0f, 5f), blurRadius = 0f)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(6.dp))

            val meta = buildString {
                append("Reached level ${state.level} · ${state.totalCount} numbers counted")
                if (state.newBest) append(" · 🏆 NEW BEST!")
            }
            Text(
                text = meta,
                style = TextStyle(
                    fontFamily = NunitoFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.White,
                    shadow = Shadow(color = Blue, offset = Offset(0f, 3f), blurRadius = 0f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            state.rankedResults.forEachIndexed { idx, player ->
                RankRow(rank = idx, player = player)
            }

            Spacer(Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ComicButton(
                    text = "PLAY AGAIN",
                    onClick = onPlayAgain,
                    style = ComicButtonStyle.ORANGE,
                    fontSize = 32.sp,
                    testTag = "play_again_button"
                )
                ComicButton(
                    text = "SHARE",
                    onClick = {
                        val wName = winner?.name ?: "Player"
                        val wScore = winner?.score ?: 0
                        onShare("$wName won BUMMM! with $wScore points and we reached level ${state.level}! 💣 Think you can beat us?")
                    },
                    style = ComicButtonStyle.BLUE,
                    fontSize = 20.sp,
                    small = true,
                    testTag = "share_button"
                )
                ComicButton(
                    text = "CHANGE PLAYERS",
                    onClick = onChangePlayers,
                    style = ComicButtonStyle.BLUE,
                    fontSize = 20.sp,
                    small = true,
                    testTag = "change_players_button"
                )
                ComicButton(
                    text = "TITLE SCREEN",
                    onClick = onTitleScreen,
                    style = ComicButtonStyle.BLUE,
                    fontSize = 20.sp,
                    small = true,
                    testTag = "title_screen_button"
                )
            }
        }
    }
}

@Composable
private fun RankRow(
    rank: Int,
    player: PlayerState
) {
    val badge = when (rank) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "#${rank + 1}"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Navy)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A4FD6), Blue)))
                .border(3.dp, Sky, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text = badge,
                style = TextStyle(
                    fontFamily = LilitaOneFont,
                    fontSize = 22.sp,
                    color = Yellow
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp)
            )

            AvatarRing(
                avatarIndex = player.av,
                size = 40.dp,
                ringPadding = 2.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${player.name}${if (player.type == SlotType.BOT) " 🤖" else ""}",
                    style = TextStyle(
                        fontFamily = LilitaOneFont,
                        fontSize = 17.sp,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${player.correct} right · ${player.wrong} wrong · best combo ${player.bestCombo}",
                    style = TextStyle(
                        fontFamily = NunitoFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = Color(0xFFC9D6FF)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = player.score.toString(),
                style = TextStyle(
                    fontFamily = OrbitronFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Yellow
                )
            )
        }
    }
}
