package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R

object WhiteNoiseTextFieldDefaults {
    /** Material's standard content inset for input, supporting text, and leading icon artwork. */
    val ContentInset = 16.dp

    /** Material already gives an above label 4 dp; this closes the gap to the 16 dp content line. */
    val AboveLabelAdditionalStartInset = 12.dp

    /** A full-shape state ring replaces a persistent resting outline. */
    val StateRingWidth = 2.dp
}

/**
 * White Noise's ordinary form field.
 *
 * Material still owns text editing, cursor/selection, icon slots, label/supporting typography,
 * focus collection, state colors, and state animation. The shared container adds the approved
 * higher-contrast tonal rest surface, transparent rest border, and 2 dp focus/error ring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteNoiseTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = whiteNoiseTextFieldColors()
    val focused = interactionSource.collectIsFocusedAsState().value
    val resolvedTextColor = textStyle.color.takeOrElse {
        colors.whiteNoiseTextColor(enabled = enabled, isError = isError, focused = focused)
    }
    val resolvedErrorMessage = errorMessage ?: stringResource(R.string.invalid_input)

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            state = state,
            modifier = modifier
                .whiteNoiseErrorSemantics(isError, resolvedErrorMessage)
                .defaultMinSize(
                    minWidth = OutlinedTextFieldDefaults.MinWidth,
                    minHeight = OutlinedTextFieldDefaults.MinHeight,
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.merge(TextStyle(color = resolvedTextColor)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = lineLimits,
            interactionSource = interactionSource,
            inputTransformation = inputTransformation,
            outputTransformation = outputTransformation,
            decorator = OutlinedTextFieldDefaults.decorator(
                state = state,
                enabled = enabled,
                lineLimits = lineLimits,
                outputTransformation = outputTransformation,
                interactionSource = interactionSource,
                labelPosition = TextFieldLabelPosition.Above(),
                label = label?.let(::contentAlignedLabel),
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                isError = isError,
                colors = colors,
                contentPadding = whiteNoiseTextFieldContentPadding(),
                container = {
                    WhiteNoiseTextFieldContainer(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                    )
                },
            ),
        )
    }
}

/** Secure counterpart to [WhiteNoiseTextField], retaining Material's secure-editing behavior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteNoiseSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    inputTransformation: InputTransformation? = null,
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = whiteNoiseTextFieldColors()
    val focused = interactionSource.collectIsFocusedAsState().value
    val resolvedTextColor = textStyle.color.takeOrElse {
        colors.whiteNoiseTextColor(enabled = enabled, isError = isError, focused = focused)
    }
    val resolvedErrorMessage = errorMessage ?: stringResource(R.string.invalid_input)

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicSecureTextField(
            state = state,
            modifier = modifier
                .whiteNoiseErrorSemantics(isError, resolvedErrorMessage)
                .defaultMinSize(
                    minWidth = OutlinedTextFieldDefaults.MinWidth,
                    minHeight = OutlinedTextFieldDefaults.MinHeight,
                ),
            enabled = enabled,
            textStyle = textStyle.merge(TextStyle(color = resolvedTextColor)),
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            interactionSource = interactionSource,
            inputTransformation = inputTransformation,
            textObfuscationMode = textObfuscationMode,
            decorator = OutlinedTextFieldDefaults.decorator(
                state = state,
                enabled = enabled,
                lineLimits = TextFieldLineLimits.SingleLine,
                outputTransformation = null,
                interactionSource = interactionSource,
                labelPosition = TextFieldLabelPosition.Above(),
                label = label?.let(::contentAlignedLabel),
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                isError = isError,
                colors = colors,
                contentPadding = whiteNoiseTextFieldContentPadding(),
                container = {
                    WhiteNoiseTextFieldContainer(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                    )
                },
            ),
        )
    }
}

@Composable
private fun whiteNoiseTextFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerHigh,
        unfocusedContainerColor = scheme.surfaceContainerHigh,
        disabledContainerColor = scheme.surfaceContainerLow,
        errorContainerColor = scheme.surfaceContainerHigh,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = scheme.error,
    )
}

@Composable
private fun WhiteNoiseTextFieldContainer(
    enabled: Boolean,
    isError: Boolean,
    interactionSource: MutableInteractionSource,
    colors: TextFieldColors,
) {
    OutlinedTextFieldDefaults.Container(
        enabled = enabled,
        isError = isError,
        interactionSource = interactionSource,
        colors = colors,
        shape = MaterialTheme.shapes.extraLarge,
        focusedBorderThickness = WhiteNoiseTextFieldDefaults.StateRingWidth,
        unfocusedBorderThickness = WhiteNoiseTextFieldDefaults.StateRingWidth,
    )
}

private fun whiteNoiseTextFieldContentPadding(): PaddingValues =
    OutlinedTextFieldDefaults.contentPadding(
        start = WhiteNoiseTextFieldDefaults.ContentInset,
        end = WhiteNoiseTextFieldDefaults.ContentInset,
    )

@OptIn(ExperimentalMaterial3Api::class)
private fun contentAlignedLabel(
    content: @Composable TextFieldLabelScope.() -> Unit,
): @Composable TextFieldLabelScope.() -> Unit = {
    val labelScope = this
    Box(Modifier.padding(start = WhiteNoiseTextFieldDefaults.AboveLabelAdditionalStartInset)) {
        content(labelScope)
    }
}

private fun Modifier.whiteNoiseErrorSemantics(
    isError: Boolean,
    errorMessage: String,
): Modifier = if (isError) semantics { error(errorMessage) } else this

private fun TextFieldColors.whiteNoiseTextColor(
    enabled: Boolean,
    isError: Boolean,
    focused: Boolean,
): Color = when {
    !enabled -> disabledTextColor
    isError -> errorTextColor
    focused -> focusedTextColor
    else -> unfocusedTextColor
}
