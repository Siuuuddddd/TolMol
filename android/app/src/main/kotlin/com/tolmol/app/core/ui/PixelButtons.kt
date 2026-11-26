package com.tolmol.app.core.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tolmol.app.core.ui.components.*
@Composable
fun PixelButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit)=
    BrickButton(onClick,modifier,enabled,content=content)
@Composable
fun PixelOutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable RowScope.()->Unit)=
    BrickSecondaryButton(onClick,modifier,enabled,content=content)
