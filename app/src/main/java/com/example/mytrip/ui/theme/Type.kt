package com.example.mytrip.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mytrip.R

val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

val MyTripTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = BeVietnamPro),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = BeVietnamPro),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = BeVietnamPro),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = BeVietnamPro),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = BeVietnamPro),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = BeVietnamPro),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = BeVietnamPro),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = BeVietnamPro),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = BeVietnamPro),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = BeVietnamPro),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = BeVietnamPro),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = BeVietnamPro),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = BeVietnamPro),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = BeVietnamPro),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = BeVietnamPro)
)