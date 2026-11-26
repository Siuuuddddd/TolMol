package com.tolmol.app.core.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.tolmol.app.R

@OptIn(ExperimentalTextApi::class)
private fun bundled(id:Int,weight:Int)=Font(id,weight=FontWeight(weight),
    variationSettings=FontVariation.Settings(FontVariation.weight(weight)))
@OptIn(ExperimentalTextApi::class)
val DisplayFamily=FontFamily(bundled(R.font.pixelify_sans,700))
@OptIn(ExperimentalTextApi::class)
private val BodyFamily=FontFamily(bundled(R.font.nunito_sans,400),bundled(R.font.nunito_sans,600),
    bundled(R.font.nunito_sans,700),bundled(R.font.nunito_sans,800),bundled(R.font.nunito_sans,900))
private fun body(size:Int,line:Int,weight:Int=700)=TextStyle(fontFamily=BodyFamily,fontWeight=FontWeight(weight),
    fontSize=size.sp,lineHeight=line.sp,fontSynthesis=FontSynthesis.None)
private fun display(size:Int,line:Int)=TextStyle(fontFamily=DisplayFamily,fontWeight=FontWeight.Bold,
    fontSize=size.sp,lineHeight=line.sp,fontSynthesis=FontSynthesis.None)
val GameTypography=Typography(
    displayLarge=display(44,50),displayMedium=display(38,44),displaySmall=display(32,38),
    headlineLarge=display(30,36),headlineMedium=display(26,32),headlineSmall=display(24,30),
    titleLarge=body(22,28,900),titleMedium=body(18,24,800),titleSmall=body(18,24,900),
    bodyLarge=body(16,24),bodyMedium=body(15,22),bodySmall=body(13,19,600),
    labelLarge=body(16,22,900),labelMedium=body(13,18,800),labelSmall=body(12,16,800))
