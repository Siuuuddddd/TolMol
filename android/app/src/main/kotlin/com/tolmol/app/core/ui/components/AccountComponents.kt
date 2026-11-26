package com.tolmol.app.core.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tolmol.app.R
import com.tolmol.app.core.ui.NativeIcon
import com.tolmol.app.core.ui.theme.*

@Composable
fun ProfileHeader(name:String,memberYear:String?=null,onClick:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier)
        .padding(vertical=GameDimens.Gap),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(GameColors.Yellow)
            .border(GameDimens.Border,GameColors.Navy,CircleShape),contentAlignment=Alignment.Center) {
            Text(name.firstOrNull()?.uppercase() ?: "T",style=MaterialTheme.typography.headlineMedium,color=GameColors.Navy)
        }
        Column(Modifier.weight(1f)) {
            Text(name,style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
            if(memberYear!=null) Text("Member since $memberYear",style=MaterialTheme.typography.bodySmall,color=GameColors.Muted)
        }
        NativeIcon(R.drawable.ic_ui_chevron,if(onClick!=null) "Open account" else null)
    }
}

@Composable
fun UpdateCard(title:String,subtitle:String,count:Int?,modifier:Modifier=Modifier,onClick:()->Unit) {
    BrickCard(modifier,onClick=onClick,
        contentPadding=PaddingValues(start=12.dp,end=16.dp,top=22.dp,bottom=16.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
        Text(subtitle,style=MaterialTheme.typography.bodySmall,color=GameColors.Muted,
            maxLines=3,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            if(count!=null) Surface(shape=GameShapes.small,color=GameColors.Yellow,contentColor=GameColors.Navy) {
                Text(count.toString(),Modifier.padding(horizontal=8.dp,vertical=3.dp),style=MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            NativeIcon(R.drawable.ic_ui_chevron,null)
        }
    }
}

@Composable
fun SectionTitle(title:String) {
    Text(title,Modifier.padding(top=GameDimens.Gap,bottom=GameDimens.SmallGap),style=MaterialTheme.typography.titleLarge)
}

@Composable
fun IconRow(icon:Int,title:String,subtitle:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=72.dp).clickable(onClick=onClick).padding(vertical=GameDimens.SmallGap),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(GameDimens.Gap)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(GameColors.Paper)
            .border(GameDimens.Border,GameColors.Navy,CircleShape),contentAlignment=Alignment.Center) {NativeIcon(icon,null)}
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=GameColors.Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AccountRow(title:String,onClick:()->Unit,destructive:Boolean=false) {
    TextButton(onClick,Modifier.fillMaxWidth().heightIn(min=48.dp),
        contentPadding=PaddingValues(vertical=GameDimens.SmallGap),
        colors=ButtonDefaults.textButtonColors(contentColor=if(destructive) GameColors.RedShadow else GameColors.Navy)) {
        Text(title,Modifier.fillMaxWidth(),style=MaterialTheme.typography.bodyLarge)
    }
}
