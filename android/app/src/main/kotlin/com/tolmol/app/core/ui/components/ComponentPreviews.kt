package com.tolmol.app.core.ui.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tolmol.app.core.ui.theme.TolMolTheme
import com.tolmol.app.R

@Preview(name="Brick buttons",showBackground=true,widthDp=390)
@Composable fun BrickButtonsPreview() {TolMolTheme {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    BrickButton({},Modifier.fillMaxWidth()) {Text("Sign in")}
    BrickSecondaryButton({},Modifier.fillMaxWidth()) {Text("Sell scrap")}
    BrickButton({},Modifier.fillMaxWidth(),tone=BrickTone.Yellow) {Text("Buy scrap nearby")}
}}}
@Preview(name="Fields and Google",showBackground=true,widthDp=390)
@Composable fun FieldsPreview() {TolMolTheme {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    TolMolTextField("",{},Modifier.fillMaxWidth(),label={Text("Email")},singleLine=true)
    TolMolTextField("",{},Modifier.fillMaxWidth(),label={Text("Password")},isError=true,supportingText={Text("Check your password")})
    SocialSignInButton({},Modifier.fillMaxWidth())
}}}
@Preview(name="Cards, chips and headings",showBackground=true,widthDp=390)
@Composable fun BrickCardPreview() {TolMolTheme {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    OutlinedPixelText("Nearby scrap")
    BrickCard(Modifier.fillMaxWidth()) {Text("Copper wire scrap",style=MaterialTheme.typography.titleMedium);Text("₹540/kg · 18 kg")}
    Row {BrickChip(true,{},label={Text("All")});Spacer(Modifier.width(8.dp));BrickChip(false,{},label={Text("Metal")})}
}}}
@Preview(name="Empty and loading",showBackground=true,widthDp=390)
@Composable fun EmptyLoadingPreview() {TolMolTheme {Column {SkeletonBrick(Modifier.fillMaxWidth().padding(20.dp));EmptyState("Nothing here yet","Your scrap lots will appear here.",{})}}}
@Preview(name="Hanging sign and scene",widthDp=390,heightDp=390)
@Composable fun SignBoardPreview() {TolMolTheme {SkyScene(Modifier.fillMaxSize()) {Column {SignBoard();Spacer(Modifier.weight(1f));RailCartScene()}}}}
@Preview(name="Native steel navigation",widthDp=390)
@Composable fun ChromePreview() {TolMolTheme {Column {
    SteelTopBar("TolMol")
    GirderDivider()
    BrickBottomBar(0,listOf("Home","My TolMol","Search","Inbox","Selling"),listOf(R.drawable.ic_ui_home,R.drawable.ic_ui_account,R.drawable.ic_ui_search,R.drawable.ic_ui_bell,R.drawable.ic_ui_tag),{})
    BrickWallFooter()
}}}
@Preview(name="Pixel dialog",widthDp=390)
@Composable fun PixelDialogPreview() {TolMolTheme {PixelDialog({},confirmButton={TextButton({}) {Text("Confirm")}},title={Text("Accept this bid?")},text={Text("Review the agreed rate before confirming.")})}}
