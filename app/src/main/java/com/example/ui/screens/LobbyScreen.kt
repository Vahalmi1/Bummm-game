package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPrefs
import com.example.model.Difficulty
import com.example.model.PlayerSlot
import com.example.model.SlotType
import com.example.ui.components.AvatarRing
import com.example.ui.components.ComicButton
import com.example.ui.components.ComicButtonStyle
import com.example.ui.components.RoundButtonStyle
import com.example.ui.components.RoundIconButton
import com.example.ui.theme.Blue
import com.example.ui.theme.LilitaOneFont
import com.example.ui.theme.Navy
import com.example.ui.theme.NunitoFont
import com.example.ui.theme.Sky
import com.example.ui.theme.Yellow

@Composable
fun LobbyScreen(
    prefs: AppPrefs,
    magicForLevel: (Int) -> Int,
    onBack: () -> Unit,
    onCycleAvatar: (Int) -> Unit,
    onUpdateName: (Int, String) -> Unit,
    onSetSlotType: (Int, SlotType) -> Unit,
    onSetDifficulty: (Difficulty) -> Unit,
    onStepLevel: (Int) -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
        ) {
            // Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RoundIconButton(
                    onClick = onBack,
                    style = RoundButtonStyle.BLUE,
                    testTag = "lobby_back_button"
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to title",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "WHO'S PLAYING?",
                    style = TextStyle(
                        fontFamily = LilitaOneFont,
                        fontSize = 32.sp,
                        color = Color.White,
                        shadow = Shadow(color = Blue, offset = Offset(0f, 6f), blurRadius = 10f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Scrollable slot cards + settings
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                prefs.slots.forEachIndexed { index, slot ->
                    SlotCard(
                        index = index,
                        slot = slot,
                        onCycleAvatar = { onCycleAvatar(index) },
                        onUpdateName = { onUpdateName(index, it) },
                        onSetType = { onSetSlotType(index, it) }
                    )
                }

                // Settings Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Navy.copy(alpha = 0.78f))
                        .border(3.dp, Sky, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Difficulty",
                            style = TextStyle(
                                fontFamily = LilitaOneFont,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Difficulty.entries.forEach { diff ->
                                val selected = prefs.diff == diff
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) {
                                                Brush.verticalGradient(listOf(Color(0xFF8CF06A), Color(0xFF169A28)))
                                            } else {
                                                SolidColor(Navy)
                                            }
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = if (selected) Color.White else Sky,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onSetDifficulty(diff) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("diff_chip_${diff.name.lowercase()}")
                                ) {
                                    Text(
                                        text = diff.label,
                                        style = TextStyle(
                                            fontFamily = LilitaOneFont,
                                            fontSize = 14.sp,
                                            color = if (selected) Color.White else Color(0xFF9FB4FF)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Start level",
                            style = TextStyle(
                                fontFamily = LilitaOneFont,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StepperCircleButton(
                                text = "−",
                                onClick = { onStepLevel(-1) },
                                testTag = "level_minus_button"
                            )
                            Text(
                                text = prefs.startLevel.toString(),
                                style = TextStyle(
                                    fontFamily = LilitaOneFont,
                                    fontSize = 22.sp,
                                    color = Yellow
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = 38.dp)
                            )
                            StepperCircleButton(
                                text = "+",
                                onClick = { onStepLevel(1) },
                                testTag = "level_plus_button"
                            )
                        }
                    }

                    Text(
                        text = "Level ${prefs.startLevel} magic number: ${magicForLevel(prefs.startLevel)}",
                        style = TextStyle(
                            fontFamily = NunitoFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color(0xFFC9D6FF)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            ComicButton(
                text = "START GAME",
                onClick = onStartGame,
                style = ComicButtonStyle.GREEN,
                fontSize = 32.sp,
                testTag = "start_game_button"
            )
        }
    }
}

@Composable
private fun StepperCircleButton(
    text: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Navy)
            .border(3.dp, Sky, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = LilitaOneFont,
                fontSize = 20.sp,
                color = Color.White
            )
        )
    }
}

@Composable
private fun SlotCard(
    index: Int,
    slot: PlayerSlot,
    onCycleAvatar: () -> Unit,
    onUpdateName: (String) -> Unit,
    onSetType: (SlotType) -> Unit
) {
    val isOff = slot.type == SlotType.OFF
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .alpha(if (isOff) 0.55f else 1f)
    ) {
        // 5dp Navy bottom lip
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Navy)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A4FD6), Blue)))
                .border(4.dp, Sky, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onCycleAvatar)
                    .testTag("slot_avatar_$index")
            ) {
                AvatarRing(
                    avatarIndex = slot.av,
                    size = 54.dp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = slot.name,
                    onValueChange = onUpdateName,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = NunitoFont,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Navy
                    ),
                    cursorBrush = SolidColor(Navy),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(3.dp, Navy, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slot_name_input_$index")
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val types = listOf(
                        SlotType.HUMAN to "👤 HUMAN",
                        SlotType.BOT to "🤖 BOT",
                        SlotType.OFF to "OFF"
                    )
                    types.forEach { (type, label) ->
                        val active = slot.type == type
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (active) {
                                        Brush.verticalGradient(listOf(Color(0xFFFFC93D), Color(0xFFFF8A1F)))
                                    } else {
                                        SolidColor(Navy)
                                    }
                                )
                                .border(2.dp, Navy, RoundedCornerShape(10.dp))
                                .clickable { onSetType(type) }
                                .padding(vertical = 5.dp)
                                .testTag("slot_${index}_${type.name.lowercase()}")
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = LilitaOneFont,
                                    fontSize = 13.sp,
                                    color = if (active) Color.White else Color(0xFF9FB4FF),
                                    shadow = if (active) {
                                        Shadow(color = Color(0x80781E00), offset = Offset(0f, 3f), blurRadius = 0f)
                                    } else null
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
