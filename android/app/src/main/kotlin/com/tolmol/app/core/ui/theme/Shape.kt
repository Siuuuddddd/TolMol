package com.tolmol.app.core.ui.theme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
object GameDimens {
    val Margin=20.dp; val Gap=12.dp; val SmallGap=8.dp; val SectionGap=24.dp
    val Border=2.5.dp; val Shadow=5.dp; val Radius=8.dp; val Stud=12.dp; val StudGap=24.dp
    val Field=56.dp; val Button=64.dp; val Chip=44.dp; val Hazard=12.dp; val Footer=40.dp
    val AuthMargin=32.dp
    val Bar=64.dp; val Toolbar=56.dp; val Icon=24.dp; val DisplayOutline=2.dp
    val SignHeight=150.dp; val AuthTitle=32.sp; val OnboardingTitle=30.sp; val Logo=38.sp
    val ProductTitle=22.sp; val Price=28.sp; val TabLabel=12.sp; val Body=16.sp; val Small=14.sp
}
val GameShapes=Shapes(extraSmall=RoundedCornerShape(4.dp),small=RoundedCornerShape(8.dp),
    medium=RoundedCornerShape(12.dp),large=RoundedCornerShape(16.dp),extraLarge=RoundedCornerShape(20.dp))
