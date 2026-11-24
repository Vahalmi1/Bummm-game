package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0B1450)
val Blue = Color(0xFF1B2F9E)
val Sky = Color(0xFF5FB3FF)
val Orange = Color(0xFFFF8A1F)
val Yellow = Color(0xFFFFD21F)
val Red = Color(0xFFFF3B4E)
val Green = Color(0xFF3CD64A)
val OuterBg = Color(0xFF1A1450)

val LedGreen = Color(0xFF3CFF6B)
val LedRed = Color(0xFFFF4B4B)
val LedYellow = Color(0xFFFFD21F)
val LedCyan = Color(0xFF9FEFFF)
val LedBg = Color(0xFF06102E)

data class BallPalette(val c1: Color, val c2: Color, val c3: Color)

val BallPalettes = listOf(
    BallPalette(Color(0xFFFF8AC8), Color(0xFFE0198A), Color(0xFF8F0B58)), // c0 Pink
    BallPalette(Color(0xFF8CF47A), Color(0xFF1DB83A), Color(0xFF0B6A22)), // c1 Green
    BallPalette(Color(0xFFFFCB5A), Color(0xFFFF7A0A), Color(0xFFA64A00)), // c2 Orange
    BallPalette(Color(0xFF7ACBFF), Color(0xFF1F7BE8), Color(0xFF0E3F96)), // c3 Blue
    BallPalette(Color(0xFFFF8A94), Color(0xFFE01E37), Color(0xFF7E0A1B)), // c4 Red
    BallPalette(Color(0xFFFFF48A), Color(0xFFF5B400), Color(0xFF9A6A00)), // c5 Yellow
    BallPalette(Color(0xFFCBA5FF), Color(0xFF7A3CE0), Color(0xFF3F1C86)), // c6 Purple
    BallPalette(Color(0xFF86F5EE), Color(0xFF14B9C4), Color(0xFF0A6670)), // c7 Teal
)

enum class HairStyle { LONG, BOB, PONY }
enum class HatStyle { NONE, CAP, FEDORA }

data class AvatarSpec(
    val skin: Color,
    val hair: Color,
    val style: HairStyle,
    val hat: HatStyle,
    val hatColor: Color,
    val top: Color,
    val bg: Color,
    val ring: Color
)

val AvatarSpecs = listOf(
    AvatarSpec(
        skin = Color(0xFFFFD7B8),
        hair = Color(0xFFD6322F),
        style = HairStyle.LONG,
        hat = HatStyle.NONE,
        hatColor = Color.Transparent,
        top = Color(0xFF2A2F55),
        bg = Color(0xFF5AD1FF),
        ring = Color(0xFF43F26B)
    ),
    AvatarSpec(
        skin = Color(0xFFF6C9A6),
        hair = Color(0xFFF08A24),
        style = HairStyle.LONG,
        hat = HatStyle.NONE,
        hatColor = Color.Transparent,
        top = Color(0xFFE8456B),
        bg = Color(0xFFFFC93D),
        ring = Color(0xFFFFC21A)
    ),
    AvatarSpec(
        skin = Color(0xFFEFC09A),
        hair = Color(0xFF7A4A2A),
        style = HairStyle.PONY,
        hat = HatStyle.CAP,
        hatColor = Color(0xFF1C1C28),
        top = Color(0xFF2D3A66),
        bg = Color(0xFF8E7BFF),
        ring = Color(0xFF3AA0FF)
    ),
    AvatarSpec(
        skin = Color(0xFFFFD7B8),
        hair = Color(0xFFB3261E),
        style = HairStyle.LONG,
        hat = HatStyle.FEDORA,
        hatColor = Color(0xFF17171F),
        top = Color(0xFFFF8A1F),
        bg = Color(0xFF2FD0C2),
        ring = Color(0xFFFF9F1C)
    ),
    AvatarSpec(
        skin = Color(0xFFF4C7A5),
        hair = Color(0xFFE5346F),
        style = HairStyle.BOB,
        hat = HatStyle.NONE,
        hatColor = Color.Transparent,
        top = Color(0xFF7A3CFF),
        bg = Color(0xFFFF7AB6),
        ring = Color(0xFFFF5FA2)
    ),
    AvatarSpec(
        skin = Color(0xFFE9B48F),
        hair = Color(0xFFC8281E),
        style = HairStyle.BOB,
        hat = HatStyle.FEDORA,
        hatColor = Color(0xFF2B1B3F),
        top = Color(0xFF19B58A),
        bg = Color(0xFFFFD25A),
        ring = Color(0xFFB66CFF)
    )
)
