package com.tolmol.app.core.ui.components
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tolmol.app.R
import com.tolmol.app.core.ui.theme.*

@Composable
fun SocialSignInButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    OutlinedButton(onClick,modifier.heightIn(min=GameDimens.Field),enabled=enabled,shape=RoundedCornerShape(28.dp),
        border=BorderStroke(GameDimens.Border,GameColors.Navy),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=GameColors.Paper,contentColor=GameColors.Navy)) {
        Image(painterResource(R.drawable.google_sign_in),null,Modifier.size(GameDimens.Icon))
        Spacer(Modifier.width(GameDimens.Gap));Text("Continue with Google")
    }
}
