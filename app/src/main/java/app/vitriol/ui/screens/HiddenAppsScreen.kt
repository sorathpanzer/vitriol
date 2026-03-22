package app.vitriol.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vitriol.MainViewModel
import app.vitriol.data.AppModel
import app.vitriol.ui.components.AppItem

private const val EFFECT_TWEEN_DELAY = 300

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
}

@Composable
private fun EmptyHiddenAppsMessage(modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(EFFECT_TWEEN_DELAY)) + expandVertically(tween(EFFECT_TWEEN_DELAY)),
        modifier = modifier, // ✅ Accept modifier from parent
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No hidden apps", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Long-press on any app in the app drawer to hide it",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HiddenAppsList(
    hiddenApps: List<AppModel>,
    onLaunch: (AppModel) -> Unit,
    onToggleHidden: (AppModel) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = hiddenApps,
            key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" },
        ) { app: AppModel ->
            AnimatedVisibility(
                visible = true, // always true, since we're not dynamically removing items here
                enter = fadeIn(animationSpec = tween(EFFECT_TWEEN_DELAY)),
                exit = fadeOut(animationSpec = tween(EFFECT_TWEEN_DELAY)),
            ) {
                AppItem(
                    app = app,
                    onClick = { onLaunch(app) },
                    onLongClick = { onToggleHidden(app) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HiddenAppsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val loading by remember { mutableStateOf(false) }
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getHiddenApps()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                hiddenApps.isEmpty() -> EmptyHiddenAppsMessage(Modifier.align(Alignment.Center))
                else ->
                    HiddenAppsList(
                        hiddenApps = hiddenApps,
                        onLaunch = viewModel::launchApp,
                        onToggleHidden = viewModel::toggleAppHidden,
                    )
            }
        }
    }
}
