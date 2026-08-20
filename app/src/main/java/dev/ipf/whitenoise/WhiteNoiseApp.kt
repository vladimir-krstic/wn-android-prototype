package dev.ipf.whitenoise

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ipf.whitenoise.navigation.WhiteNoiseNavHost
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme

@Composable
fun WhiteNoiseApp(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = viewModel(),
) {
    val view = LocalView.current
    val hideScreenInRecents = appViewModel.uiState.activeProfile?.settings?.hideScreenInRecents == true
    SideEffect {
        view.context.findActivity()?.window?.let { window ->
            if (hideScreenInRecents) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    WhiteNoiseTheme(appearance = appViewModel.uiState.activeProfile?.settings?.appearance
        ?: dev.ipf.whitenoise.model.AppearancePreference.System) {
        WhiteNoiseNavHost(
            navController = navController,
            appViewModel = appViewModel,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
