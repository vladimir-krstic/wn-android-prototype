package dev.ipf.whitenoise.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.ipf.whitenoise.model.LanguagePreference
import java.util.Locale

/** Applies the active in-memory profile locale to app-owned resources immediately. */
@Composable
fun AppLocale(
    language: LanguagePreference,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localeTag = language.localeTag
    if (localeTag == null) {
        content()
        return
    }
    val configuration = remember(baseConfiguration, localeTag) {
        Configuration(baseConfiguration).apply {
            setLocale(Locale.forLanguageTag(localeTag))
            setLayoutDirection(Locale.forLanguageTag(localeTag))
        }
    }
    val localizedContext = remember(baseContext, configuration) {
        baseContext.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides localizedContext,
        content = content,
    )
}
