
package com.tolmol.app.feature.listing.presentation
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.tolmol.app.core.ui.components.*
import com.tolmol.app.core.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerSection(vm: PublishViewModel) {
    val context=LocalContext.current
    var sheet by remember { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if(ok) pending?.let { vm.add(listOf(it)) }
    }
    fun take() {
        val folder=File(context.cacheDir,"listing-photos").apply { mkdirs() }
        val file=File.createTempFile("capture-",".jpg",folder)
        val uri=FileProvider.getUriForFile(context,context.packageName+".photos",file)
        pending=uri.toString();camera.launch(uri)
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) take() else vm.cameraDenied()
    }
    val gallery=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        vm.add(uris.map { it.toString() })
    }
    PhotoSection(vm.selected.map { it.source },vm.preparing || vm.state.value.loading,
        {sheet=true},{vm.remove(vm.selected[it].key)},{vm.cover(vm.selected[it].key)})
    if(vm.preparing) Text("Preparing photos…",style=MaterialTheme.typography.bodyMedium)
    vm.photoError?.let { Text(it,color=GameColors.Navy) }
    if(sheet) ModalBottomSheet(onDismissRequest={sheet=false},containerColor=GameColors.Cream) {
        TextButton({ sheet=false
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) take()
            else permission.launch(Manifest.permission.CAMERA)
        },Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Take photo")}
        TextButton({sheet=false;gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
            Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Choose from gallery")}
    }
}

private fun Modifier.dashed() = drawBehind {
    drawRoundRect(GameColors.Navy,style=Stroke(GameDimens.Border.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(),8.dp.toPx()))),cornerRadius=androidx.compose.ui.geometry.CornerRadius(GameDimens.Radius.toPx()))
}

@Composable
private fun PhotoSection(sources: List<String>,busy: Boolean,add: ()->Unit,remove: (Int)->Unit,cover: (Int)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        Text("UPLOAD PHOTOS",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f))
        Text("STEP 1 OF 2",style=MaterialTheme.typography.labelSmall)
    }
    Column(Modifier.fillMaxWidth().heightIn(min=160.dp).dashed().clickable(enabled=!busy && sources.size<5,onClick=add)
        .padding(GameDimens.Margin),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        com.tolmol.app.core.ui.NativeIcon(com.tolmol.app.R.drawable.ic_photo_camera,"Add scrap photos")
        Text("TAP TO ADD PHOTOS",style=MaterialTheme.typography.labelMedium)
    }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(GameDimens.SmallGap)) {
        repeat(5) { index ->
            Box(Modifier.size(88.dp).dashed()) {
                if(index<sources.size) {
                    AsyncImage(sources[index],"Scrap photo "+(index+1)+". Tap to make cover",Modifier.fillMaxSize().clickable(enabled=!busy) {cover(index)},contentScale=androidx.compose.ui.layout.ContentScale.Crop)
                    if(index==0) Text("Cover",Modifier.align(Alignment.BottomStart).background(GameColors.Cream),style=MaterialTheme.typography.labelSmall)
                    TextButton({remove(index)},Modifier.align(Alignment.TopEnd).size(48.dp).semantics { contentDescription="Remove photo "+(index+1) }.background(GameColors.Cream,CircleShape)
                        .border(GameDimens.Border,GameColors.Navy,CircleShape),enabled=!busy,contentPadding=PaddingValues(0.dp)) {Text("X")}
                }
            }
        }
    }
    Text("Photos are optional. Clear photos help buyers review your scrap.",style=MaterialTheme.typography.bodySmall,color=GameColors.Muted)
}
@Preview @Composable private fun EmptyPhotosPreview() = TolMolTheme {Column {PhotoSection(emptyList(),false,{},{},{})}}
@Preview @Composable private fun FilledPhotosPreview() = TolMolTheme {Column {PhotoSection(listOf("android.resource://com.tolmol.app/"+com.tolmol.app.R.drawable.scrap_paper_01),false,{},{},{})}}
