package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val LilitaOneFont = FontFamily(
    Font(R.font.lilita_one, FontWeight.Normal)
)

val NunitoFont = FontFamily(
    Font(R.font.nunito, FontWeight.Bold),
    Font(R.font.nunito, FontWeight.ExtraBold),
    Font(R.font.nunito, FontWeight.Black)
)

val OrbitronFont = FontFamily(
    Font(R.font.orbitron, FontWeight.Bold),
    Font(R.font.orbitron, FontWeight.Black)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = LilitaOneFont,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp
    ),
    titleLarge = TextStyle(
        fontFamily = LilitaOneFont,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OrbitronFont,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp
    )
)
