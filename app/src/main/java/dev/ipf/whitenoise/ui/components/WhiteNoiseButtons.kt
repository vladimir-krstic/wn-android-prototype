package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R

object WhiteNoiseButtonDefaults {
    /** Exact Material medium-button metrics; matches a 56 dp single-line text field. */
    val TaskHeight = 56.dp
    val TaskContentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
    val LoadingIndicatorSize = 20.dp
    val LoadingContentSpacing = 8.dp
}

@Composable
fun WhiteNoiseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    require(!loading || !loadingLabel.isNullOrBlank()) {
        "A loading primary button needs a visible progress label."
    }
    val inProgressDescription = stringResource(R.string.wn_in_progress)
    val colors = if (loading) {
        ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    } else {
        ButtonDefaults.buttonColors()
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = WhiteNoiseButtonDefaults.TaskHeight)
            .semantics {
                if (loading) {
                    stateDescription = inProgressDescription
                    liveRegion = LiveRegionMode.Polite
                }
            },
        enabled = enabled && !loading,
        colors = colors,
        contentPadding = WhiteNoiseButtonDefaults.TaskContentPadding,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(WhiteNoiseButtonDefaults.LoadingIndicatorSize)
                    .clearAndSetSemantics { },
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(WhiteNoiseButtonDefaults.LoadingContentSpacing))
            Text(checkNotNull(loadingLabel))
        } else {
            content()
        }
    }
}

@Composable
fun WhiteNoiseOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = WhiteNoiseButtonDefaults.TaskHeight),
        enabled = enabled,
        contentPadding = WhiteNoiseButtonDefaults.TaskContentPadding,
        content = content,
    )
}

@Composable
fun WhiteNoiseFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = WhiteNoiseButtonDefaults.TaskHeight),
        enabled = enabled,
        contentPadding = WhiteNoiseButtonDefaults.TaskContentPadding,
        content = content,
    )
}
