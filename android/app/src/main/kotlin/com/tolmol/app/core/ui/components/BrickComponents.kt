package com.tolmol.app.core.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.tolmol.app.core.ui.theme.*
import com.tolmol.app.core.ui.NativeIcon

enum class BrickTone { Red, Blue, Yellow }
private fun BrickTone.fill()=when(this){BrickTone.Red->GameColors.Red;BrickTone.Blue->GameColors.Blue;BrickTone.Yellow->GameColors.Yellow}
private fun BrickTone.shade()=when(this){BrickTone.Red->GameColors.RedShadow;BrickTone.Blue->GameColors.BlueShadow;BrickTone.Yellow->GameColors.YellowShadow}
private fun BrickTone.ink()=if(this==BrickTone.Yellow) GameColors.Navy else GameColors.Paper

private fun DrawScope.brick(fill:Color,stud:Color=fill,pressed:Boolean=false,enabled:Boolean=true) {
    val edge=GameDimens.Border.toPx(); val shadow=GameDimens.Shadow.toPx()
    val top=GameDimens.Stud.toPx()*0.6f; val shift=if(pressed) shadow*0.5f else 0f
    val width=size.width-shadow; val height=size.height-top-shadow
    drawRoundRect(GameColors.Navy.copy(alpha=if(enabled) 1f else 0.4f),Offset(shadow,top+shadow),Size(width,height),CornerRadius(GameDimens.Radius.toPx()))
    drawRoundRect(GameColors.Navy,Offset(0f,top+shift),Size(width,height),CornerRadius(GameDimens.Radius.toPx()))
    drawRoundRect(if(enabled) fill else GameColors.SteelLight,Offset(edge,top+edge+shift),Size(width-edge*2,height-edge*2),CornerRadius(GameDimens.Radius.toPx()*0.65f))
    drawRect(GameColors.Paper.copy(alpha=0.35f),Offset(edge*2,top+edge*1.3f+shift),Size((width-edge*4).coerceAtLeast(1f),edge))
    val studWidth=GameDimens.Stud.toPx(); val gap=GameDimens.StudGap.toPx()
    var x=GameDimens.Margin.toPx()
    while(x+studWidth<width-edge) {
        drawRect(GameColors.Navy,Offset(x,shift),Size(studWidth,top+edge))
        drawRect(if(enabled) stud else GameColors.SteelLight,Offset(x+edge*0.65f,edge*0.65f+shift),Size(studWidth-edge*1.3f,top))
        drawLine(GameColors.Paper.copy(alpha=0.45f),Offset(x+edge,edge+shift),Offset(x+studWidth-edge,edge+shift),edge*0.6f)
        x+=gap+studWidth
    }
}

@Composable
fun BrickButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=GameShapes.small,contentPadding:PaddingValues=PaddingValues(horizontal=20.dp,vertical=12.dp),
    tone:BrickTone=BrickTone.Red,content:@Composable RowScope.()->Unit) {
    val source=remember {MutableInteractionSource()}; val pressed by source.collectIsPressedAsState()
    Box(modifier.heightIn(min=GameDimens.Button).drawBehind {brick(tone.fill(),pressed=pressed,enabled=enabled)}) {
        androidx.compose.material3.Button(onClick,Modifier.fillMaxWidth().heightIn(min=GameDimens.Button).padding(top=GameDimens.Stud/2,bottom=GameDimens.Shadow,end=GameDimens.Shadow),
            enabled=enabled,shape=shape,interactionSource=source,contentPadding=contentPadding,
            colors=ButtonDefaults.buttonColors(containerColor=GameColors.Transparent,contentColor=tone.ink(),
                disabledContainerColor=GameColors.Transparent,disabledContentColor=GameColors.Muted),content=content)
    }
}
@Composable
fun BrickSecondaryButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=GameShapes.small,contentPadding:PaddingValues=PaddingValues(horizontal=16.dp,vertical=10.dp),
    content:@Composable RowScope.()->Unit)=BrickButton(onClick,modifier,enabled,shape,contentPadding,BrickTone.Blue,content)

@Composable
fun BrickCard(modifier:Modifier=Modifier,onClick:(()->Unit)?=null,tone:BrickTone?=null,
    content:@Composable ColumnScope.()->Unit) {
    val color=tone?.fill() ?: GameColors.Paper
    val ink=tone?.ink() ?: GameColors.Navy
    Column(modifier.then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier)
        .drawBehind {brick(color,tone?.fill() ?: GameColors.Blue)}
        .padding(start=16.dp,end=20.dp,top=22.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(GameDimens.SmallGap)) {
        CompositionLocalProvider(LocalContentColor provides ink) {content()}
    }
}
@Composable
fun OutlinedPixelText(text:String,modifier:Modifier=Modifier,style:TextStyle=MaterialTheme.typography.headlineMedium,
    fill:Color=GameColors.Paper,textAlign:TextAlign?=null) {
    val outline=with(LocalDensity.current) {GameDimens.DisplayOutline.toPx()*2}
    Box(modifier) {
        Text(text,style=style.copy(drawStyle=Stroke(outline,join=StrokeJoin.Miter)),color=GameColors.Navy,textAlign=textAlign)
        Text(text,style=style,color=fill,textAlign=textAlign)
    }
}
@Composable
fun TolMolTextField(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    label:(@Composable ()->Unit)?=null,placeholder:(@Composable ()->Unit)?=null,
    leadingIcon:(@Composable ()->Unit)?=null,trailingIcon:(@Composable ()->Unit)?=null,
    singleLine:Boolean=false,minLines:Int=1,maxLines:Int=if(singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions:KeyboardOptions=KeyboardOptions.Default,visualTransformation:VisualTransformation=VisualTransformation.None,
    shape:Shape=GameShapes.small,isError:Boolean=false,supportingText:(@Composable ()->Unit)?=null) {
    OutlinedTextField(value,onValueChange,modifier.heightIn(min=GameDimens.Field)
        .border(GameDimens.Border,if(isError) MaterialTheme.colorScheme.error else GameColors.Navy,shape),
        enabled=enabled,label=label,placeholder=placeholder,leadingIcon=leadingIcon,trailingIcon=trailingIcon,
        singleLine=singleLine,minLines=minLines,maxLines=maxLines,keyboardOptions=keyboardOptions,
        visualTransformation=visualTransformation,shape=shape,isError=isError,supportingText=supportingText,
        textStyle=MaterialTheme.typography.bodyLarge,
        colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=GameColors.Paper,unfocusedContainerColor=GameColors.Paper,
            disabledContainerColor=GameColors.Cream,errorContainerColor=GameColors.Paper,
            focusedBorderColor=GameColors.Transparent,unfocusedBorderColor=GameColors.Transparent,
            focusedLabelColor=GameColors.Navy,unfocusedLabelColor=GameColors.Navy,cursorColor=GameColors.Blue))
}
@Composable
fun BrickChip(selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier,label:@Composable ()->Unit) {
    FilterChip(selected,onClick,modifier=modifier.heightIn(min=GameDimens.Chip).drawBehind {
        brick(if(selected) GameColors.Yellow else GameColors.Paper,if(selected) GameColors.Yellow else GameColors.Blue)
    },label=label,shape=GameShapes.extraSmall,
        border=FilterChipDefaults.filterChipBorder(enabled=true,selected=selected,borderColor=GameColors.Transparent,
            selectedBorderColor=GameColors.Transparent),
        colors=FilterChipDefaults.filterChipColors(containerColor=GameColors.Transparent,labelColor=GameColors.Navy,
            selectedContainerColor=GameColors.Transparent,selectedLabelColor=GameColors.Navy))
}
@Composable
fun PixelDialog(onDismissRequest:()->Unit,confirmButton:@Composable ()->Unit,modifier:Modifier=Modifier,
    dismissButton:(@Composable ()->Unit)?=null,icon:(@Composable ()->Unit)?=null,title:(@Composable ()->Unit)?=null,
    text:(@Composable ()->Unit)?=null) {
    AlertDialog(onDismissRequest,confirmButton,modifier.border(GameDimens.Border,GameColors.Navy,GameShapes.large),
        dismissButton=dismissButton,icon=icon,title=title,text=text,shape=GameShapes.large,
        containerColor=GameColors.Paper,titleContentColor=GameColors.Navy,textContentColor=GameColors.Navy,tonalElevation=0.dp)
}
@Composable
fun PixelSnackbar(data:SnackbarData,modifier:Modifier=Modifier) {
    Snackbar(data,modifier.border(GameDimens.Border,GameColors.Navy,GameShapes.small),shape=GameShapes.small,
        containerColor=GameColors.Navy,contentColor=GameColors.Paper,actionColor=GameColors.Yellow)
}
@Composable
fun SkeletonBrick(modifier:Modifier=Modifier) {
    Box(modifier.heightIn(min=64.dp).drawBehind {brick(GameColors.BlueLight,GameColors.SteelLight)})
}
@Composable
fun EmptyState(title:String,message:String,onRetry:(()->Unit)?=null) {
    Column(Modifier.fillMaxWidth().padding(GameDimens.Margin),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
        CardboardStack(Modifier.size(88.dp))
        OutlinedPixelText(title,textAlign=TextAlign.Center)
        Text(message,style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center)
        if(onRetry!=null) BrickSecondaryButton(onRetry) {Text("Retry")}
    }
}
@Composable
fun YellowUnderlineLink(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,onSky:Boolean=false) {
    TextButton(onClick,modifier,enabled=enabled,colors=ButtonDefaults.textButtonColors(contentColor=if(onSky) GameColors.Paper else GameColors.Navy)) {
        Text(text,Modifier.drawBehind {drawRect(GameColors.Yellow,Offset(0f,size.height-3.dp.toPx()),Size(size.width,3.dp.toPx()))},
            style=MaterialTheme.typography.labelLarge)
    }
}
