package com.tolmol.app.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tolmol.app.core.ui.components.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScreenState<T>(val value: T? = null, val loading: Boolean = false, val error: String? = null)

abstract class ScreenViewModel<T> : ViewModel() {
    private val mutable = MutableStateFlow(ScreenState<T>())
    val state = mutable.asStateFlow()

    protected fun load(action: suspend () -> T) {
        if (mutable.value.loading) return
        mutable.value = mutable.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try { mutable.value = ScreenState(value = action()) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutable.value = mutable.value.copy(loading = false, error = error.message ?: "Try again.")
            }
        }
    }
}

@Composable
fun Screen(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedPixelText(title, style = MaterialTheme.typography.headlineSmall)
        content()
        BrickWallFooter(Modifier.fillMaxWidth())
    }
}

@Composable
fun ScreenFeedback(state: ScreenState<*>, retry: () -> Unit) {
    if (state.loading) SkeletonBrick(Modifier.fillMaxWidth())
    if (state.error != null) {
        BrickCard(Modifier.fillMaxWidth()) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            BrickSecondaryButton(retry) { Text("Retry") }
        }
    }
}

@Composable
fun RowCard(title: String, subtitle: String, onClick: () -> Unit) {
    BrickCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun String.label() = replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
