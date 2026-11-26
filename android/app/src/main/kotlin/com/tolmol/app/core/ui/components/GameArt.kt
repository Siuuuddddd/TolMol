package com.tolmol.app.core.ui.components

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.tolmol.app.core.ui.theme.*
import com.tolmol.app.core.ui.NativeIcon

private fun DrawScope.r(x:Float,y:Float,w:Float,h:Float,c:Color)=drawRect(c,Offset(x,y),Size(w,h))
private fun DrawScope.cloud(x:Float,y:Float,s:Float=1f) { withTransform({translate(x,y);scale(s,s,Offset.Zero)}) {
    r(0f,14f,58f,16f,GameColors.Navy);r(8f,7f,42f,23f,GameColors.Navy);r(18f,0f,20f,30f,GameColors.Navy)
    r(4f,17f,50f,9f,GameColors.Paper);r(12f,11f,34f,15f,GameColors.Paper);r(21f,4f,14f,22f,GameColors.Paper)
    r(8f,25f,42f,3f,GameColors.SteelLight)
} }
private fun DrawScope.wall(y:Float,h:Float,w:Float) {
    r(0f,y,w,h,GameColors.Stone);var row=0;var py=y
    while(py<y+h) {r(0f,py,w,1.5f,GameColors.StoneLine);var x=if(row%2==0) 0f else -22f
        while(x<w){r(x,py,1.5f,14f,GameColors.StoneLine);x+=44f};row++;py+=14f}
}
private fun DrawScope.recycle(x:Float,y:Float,s:Float,c:Color) { withTransform({translate(x,y);scale(s,s,Offset.Zero)}) {
    repeat(3){i->rotate(i*120f,Offset(14f,14f)) {
        drawLine(c,Offset(5f,7f),Offset(20f,7f),5f)
        drawPath(Path().apply {moveTo(18f,1f);lineTo(26f,8f);lineTo(18f,14f);close()},c)
    }}
} }
private fun DrawScope.girder(y:Float,w:Float,h:Float) {
    r(0f,y,w,h,GameColors.Navy);r(2f,y+2f,w-4f,h-5f,GameColors.Steel)
    r(3f,y+3f,w-6f,3f,GameColors.SteelLight);r(2f,y+h-6f,w-4f,3f,GameColors.Tile)
    var x=8f;while(x<w){drawLine(GameColors.Tile,Offset(x,y+6),Offset(x+30,y+h-7),3f);drawLine(GameColors.SteelLight,Offset(x+1,y+5),Offset(x+31,y+h-8),1f)
        r(x,y+6,4f,4f,GameColors.Navy);r(x+1,y+7,2f,2f,GameColors.Paper);x+=42f}
}
private fun DrawScope.traffic(x:Float,y:Float) {
    r(x+8,y,6f,200f,GameColors.Navy)
    repeat(15) {i->r(x+9,y+35+i*10,4f,10f,if(i%2==0) GameColors.Paper else GameColors.Red)}
    r(x,y,23f,66f,GameColors.Navy);r(x+3,y+3,17f,60f,GameColors.Tile)
    listOf(GameColors.Red,GameColors.Yellow,GameColors.Green).forEachIndexed {i,c->
        r(x+7,y+8+i*18,10f,10f,c);r(x+9,y+6+i*18,6f,14f,c)}
}

@Composable
fun SkyScene(modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit) {
    val view=LocalView.current
    DisposableEffect(view) {
        var ctx=view.context
        while(ctx is ContextWrapper && ctx !is Activity) ctx=ctx.baseContext
        val activity=ctx as? Activity
        if(!view.isInEditMode) activity?.window?.let {WindowCompat.getInsetsController(it,view).isAppearanceLightStatusBars=false}
        onDispose {if(!view.isInEditMode) activity?.window?.let {WindowCompat.getInsetsController(it,view).isAppearanceLightStatusBars=true}}
    }
    Box(modifier.background(Brush.verticalGradient(listOf(GameColors.SkyTop,GameColors.SkyBottom)))) {
        Canvas(Modifier.matchParentSize()) {scale(size.width/390f,size.width/390f,Offset.Zero) {
            cloud(12f,65f,0.8f);cloud(300f,130f,1.1f);cloud(20f,505f,0.6f)
            traffic(4f,250f);traffic(363f,250f)
        }}
        CompositionLocalProvider(LocalContentColor provides GameColors.Paper) {content()}
    }
}
@Composable
fun CreamScene(modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit) {
    Box(modifier.background(GameColors.Cream)) {
        Canvas(Modifier.matchParentSize()) {scale(size.width/390f,size.width/390f,Offset.Zero) {
            cloud(6f,100f,0.8f);cloud(320f,32f,0.9f);cloud(280f,440f,0.6f)
            r(0f,12f,16f,56f,GameColors.Blue);r(16f,36f,16f,32f,GameColors.Blue)
            r(0f,68f,16f,32f,GameColors.Yellow);r(0f,100f,8f,32f,GameColors.Red)
            r(374f,170f,16f,40f,GameColors.Red);r(358f,192f,16f,18f,GameColors.Red)
            r(382f,210f,8f,45f,GameColors.Yellow)
        }};content()
    }
}
@Composable
fun SignBoard(modifier:Modifier=Modifier) {
    Box(modifier.fillMaxWidth().height(GameDimens.SignHeight)) {
        Canvas(Modifier.matchParentSize()) {scale(size.width/390f,size.height/150f,Offset.Zero) {
            girder(0f,390f,25f)
            listOf(85f,305f).forEach {x->repeat(7){i->
                drawOval(GameColors.Navy,Offset(x-4,23f+i*8),Size(8f,12f));drawOval(GameColors.SteelLight,Offset(x-2,25f+i*8),Size(4f,8f))}}
            r(28f,72f,334f,69f,GameColors.Navy);r(31f,75f,328f,62f,GameColors.Steel)
        }}
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=36.dp,vertical=17.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            "TolMol".forEach {letter->Box(Modifier.weight(1f).height(54.dp).background(GameColors.Tile).border(2.dp,GameColors.Navy),contentAlignment=Alignment.Center) {
                Text(letter.toString(),style=MaterialTheme.typography.displayMedium,color=GameColors.Yellow)
            }}
        }
    }
}
@Composable
fun CardboardStack(modifier:Modifier=Modifier) {
    Canvas(modifier) {scale(size.width/120f,size.height/120f,Offset.Zero) {
        r(25f,28f,66f,70f,GameColors.Navy);r(28f,31f,60f,64f,GameColors.Cardboard)
        drawPath(Path().apply {moveTo(25f,28f);lineTo(10f,17f);lineTo(39f,10f);lineTo(58f,23f);close()},GameColors.Navy)
        drawPath(Path().apply {moveTo(29f,27f);lineTo(18f,18f);lineTo(39f,14f);lineTo(53f,24f);close()},GameColors.CardboardLight)
        drawPath(Path().apply {moveTo(59f,23f);lineTo(78f,8f);lineTo(105f,19f);lineTo(91f,29f);close()},GameColors.Navy)
        drawPath(Path().apply {moveTo(64f,24f);lineTo(79f,12f);lineTo(99f,20f);lineTo(88f,27f);close()},GameColors.CardboardLight)
        recycle(45f,50f,1f,GameColors.Green)
        r(1f,74f,42f,42f,GameColors.Navy);r(4f,77f,36f,36f,GameColors.CardboardLight);r(18f,77f,7f,36f,GameColors.Cardboard)
        r(94f,67f,24f,45f,GameColors.Navy);r(97f,70f,18f,39f,GameColors.Steel);r(98f,73f,16f,4f,GameColors.Paper)
        r(99f,91f,14f,8f,GameColors.Green)
    }}
}
@Composable
fun RailCartScene(modifier:Modifier=Modifier) {
    Canvas(modifier.fillMaxWidth().height(150.dp)) {scale(size.width/390f,size.height/150f,Offset.Zero) {
        wall(120f,30f,390f)
        r(0f,107f,390f,5f,GameColors.Navy);r(0f,109f,390f,2f,GameColors.SteelLight)
        repeat(20){i->r(i*21f,113f,10f,4f,GameColors.Rust)}
        r(0f,100f,65f,10f,GameColors.DarkBush);r(9f,87f,18f,18f,GameColors.Bush);r(34f,92f,23f,14f,GameColors.Bush)
        r(340f,96f,50f,16f,GameColors.DarkBush);r(349f,84f,22f,23f,GameColors.Bush);r(377f,89f,13f,19f,GameColors.Bush)
        r(275f,55f,38f,55f,GameColors.Navy);r(278f,58f,32f,49f,GameColors.Rust);r(278f,72f,32f,5f,GameColors.Steel)
        r(278f,94f,32f,5f,GameColors.Steel);r(281f,60f,4f,43f,GameColors.RustLight)
        r(98f,48f,142f,52f,GameColors.Navy);r(101f,51f,136f,46f,GameColors.Rust)
        r(102f,52f,134f,7f,GameColors.RustLight);r(111f,58f,5f,34f,GameColors.RustLight)
        recycle(155f,64f,0.9f,GameColors.Paper)
        r(111f,25f,24f,23f,GameColors.Steel);r(143f,31f,22f,17f,GameColors.SteelLight)
        drawLine(GameColors.Navy,Offset(173f,47f),Offset(197f,17f),9f);drawLine(GameColors.SteelLight,Offset(173f,47f),Offset(197f,17f),5f)
        drawCircle(GameColors.Navy,12f,Offset(215f,36f));drawCircle(GameColors.Steel,8f,Offset(215f,36f));drawCircle(GameColors.Navy,3f,Offset(215f,36f))
        listOf(120f,220f).forEach {x->drawCircle(GameColors.Navy,10f,Offset(x,101f));drawCircle(GameColors.Steel,5f,Offset(x,101f))}
    }}
}
@Composable
fun GirderDivider(modifier:Modifier=Modifier) {
    Canvas(modifier.fillMaxWidth().height(GameDimens.Hazard)) {clipRect {
        drawRect(GameColors.Navy);drawRect(GameColors.Yellow,Offset(0f,2.dp.toPx()),Size(size.width,size.height-4.dp.toPx()))
        var x=-size.height;while(x<size.width+size.height) {drawLine(GameColors.Navy,Offset(x,size.height),Offset(x+size.height,0f),size.height*0.7f);x+=size.height*1.7f}
    }}
}
@Composable
fun BrickWallFooter(modifier:Modifier=Modifier) {
    Column(modifier) {GirderDivider();Canvas(Modifier.fillMaxWidth().height(GameDimens.Footer)) {scale(size.width/390f,size.height/40f,Offset.Zero){wall(0f,40f,390f)}}}
}
@Composable
fun SteelBar(modifier:Modifier=Modifier,content:@Composable ()->Unit) {
    val scheme=MaterialTheme.colorScheme.copy(primary=GameColors.Yellow,onPrimary=GameColors.Navy,
        onSurface=GameColors.Paper,onBackground=GameColors.Paper)
    Surface(modifier,color=GameColors.Navy,contentColor=GameColors.Paper) {
        Column {Canvas(Modifier.fillMaxWidth().height(4.dp)){drawRect(GameColors.Steel);drawLine(GameColors.SteelLight,Offset.Zero,Offset(size.width,0f),2.dp.toPx())}
            MaterialTheme(colorScheme=scheme) {content()}}
    }
}
@Composable
fun SteelTopBar(title:String,navigationIcon:@Composable ()->Unit={},actions:@Composable RowScope.()->Unit={}) {
    SteelBar {Row(Modifier.fillMaxWidth().height(GameDimens.Toolbar),verticalAlignment=Alignment.CenterVertically) {
        navigationIcon();Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);actions()
    }}
}
@Composable
fun BrickBottomBar(selected:Int,labels:List<String>,icons:List<Int>,select:(Int)->Unit) {
    Column {GirderDivider(Modifier.height(4.dp));NavigationBar(Modifier.height(GameDimens.Bar),containerColor=GameColors.Navy,windowInsets=WindowInsets(0,0,0,0)) {
        labels.forEachIndexed {index,label->NavigationBarItem(selected=index==selected,onClick={select(index)},
            icon={NativeIcon(icons[index],null)},label={Text(label,style=MaterialTheme.typography.labelSmall,maxLines=1)},
            colors=NavigationBarItemDefaults.colors(selectedIconColor=GameColors.Yellow,selectedTextColor=GameColors.Yellow,
                unselectedIconColor=GameColors.Paper,unselectedTextColor=GameColors.Paper,indicatorColor=GameColors.Blue))}
    }}
}

@Composable
fun SteelSystemBars() {
    val view=LocalView.current
    DisposableEffect(view) {
        val window=(view.context as? Activity)?.window
        window?.let {WindowCompat.getInsetsController(it,view).apply {isAppearanceLightStatusBars=false;isAppearanceLightNavigationBars=false}}
        onDispose {window?.let {WindowCompat.getInsetsController(it,view).apply {isAppearanceLightStatusBars=true;isAppearanceLightNavigationBars=true}}}
    }
}
