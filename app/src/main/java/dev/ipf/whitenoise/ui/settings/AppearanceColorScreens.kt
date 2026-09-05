package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AppearanceColorPolicy
import dev.ipf.whitenoise.model.AppearanceColorTheme
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatBubbleColorOverrides
import dev.ipf.whitenoise.model.HsvColor
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileSettings
import dev.ipf.whitenoise.model.ThemeColorOverrides
import dev.ipf.whitenoise.ui.theme.LocalDefaultMessageBubbleColors
import dev.ipf.whitenoise.ui.theme.colorFromOpaqueArgb

@Composable
fun ActionColorScreen(
    profile: Profile,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
) {
    val settings = profile.settings
    val theme = AppearanceColorTheme.resolve(settings.appearance, isSystemInDarkTheme())
    val selected = settings.colors.forTheme(theme).actionArgb
    SettingsScaffold(title = stringResource(R.string.action_color), onBack = onBack) {
        SettingsList {
            item {
                SettingsGroup {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.color_preview), style = MaterialTheme.typography.titleMedium)
                            Button(onClick = {}) { Text(stringResource(R.string.color_preview_action)) }
                        }
                    }
                }
            }

            item {
                SettingsGroup {
                    item {
                        FullSpectrumColorPicker(
                            selectedArgb = selected,
                            fallbackArgb = colorLong(MaterialTheme.colorScheme.primary),
                            onColorSelected = { color ->
                                onChange(settings.copy(colors = settings.colors.updateTheme(theme) { it.copy(actionArgb = color) }))
                            },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            item {
                TextButton(
                    onClick = { onChange(settings.copy(colors = settings.colors.updateTheme(theme) { it.copy(actionArgb = null) })) },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.reset_to_default)) }
            }
            item { SettingsExplainer(stringResource(R.string.action_color_detail, theme.label())) }
        }
    }
}

@Composable
fun ChatBubbleColorsScreen(
    profile: Profile,
    chat: Chat? = null,
    onBack: () -> Unit,
    onProfileChange: (ProfileSettings) -> Unit,
    onChatChange: (ChatBubbleColorOverrides) -> Unit = {},
) {
    val settings = profile.settings
    val theme = AppearanceColorTheme.resolve(settings.appearance, isSystemInDarkTheme())
    val global = settings.colors.forTheme(theme)
    val mineSelected = chat?.bubbleColors?.mineArgb ?: global.mineBubbleArgb
    val otherSelected = chat?.bubbleColors?.otherArgb ?: global.otherBubbleArgb
    val defaults = LocalDefaultMessageBubbleColors.current
    fun updateGlobal(transform: (ThemeColorOverrides) -> ThemeColorOverrides) {
        onProfileChange(settings.copy(colors = settings.colors.updateTheme(theme, transform)))
    }
    var menuOpen by remember(profile.id, chat?.id) { mutableStateOf(false) }
    val canReset = if (chat == null) global.mineBubbleArgb != null || global.otherBubbleArgb != null
        else chat.bubbleColors.mineArgb != null || chat.bubbleColors.otherArgb != null
    SettingsScaffold(
        title = stringResource(R.string.chat_bubble_colors), onBack = onBack,
        topBarActions = {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("bubble_colors.menu")) {
                    Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.more_options))
                }
                WhiteNoiseDropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    items = listOf(WhiteNoiseMenuItem(
                        label = stringResource(if (chat == null) R.string.reset_to_default else R.string.reset_to_global_colors),
                        enabled = canReset,
                        onClick = {
                            menuOpen = false
                            if (chat == null) updateGlobal { it.copy(mineBubbleArgb = null, otherBubbleArgb = null) }
                            else onChatChange(ChatBubbleColorOverrides())
                        },
                    )),
                )
            }
        },
    ) {
        SettingsList {
            item {
                SettingsGroup {
                    item {
                        BubblePreview(
                            mineArgb = AppearanceColorPolicy.effectiveBubble(chat?.bubbleColors?.mineArgb, global.mineBubbleArgb),
                            otherArgb = AppearanceColorPolicy.effectiveBubble(chat?.bubbleColors?.otherArgb, global.otherBubbleArgb),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            item { SettingsSection(stringResource(R.string.bubble_my_messages)) }
            item {
                SettingsGroup {
                    item {
                        FullSpectrumColorPicker(
                            selectedArgb = mineSelected,
                            fallbackArgb = colorLong(defaults.mineContainer),
                            onColorSelected = { color ->
                                if (chat == null) updateGlobal { it.copy(mineBubbleArgb = color) }
                                else onChatChange(chat.bubbleColors.copy(mineArgb = color))
                            },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            item { SettingsSection(stringResource(R.string.bubble_other_messages)) }
            item {
                SettingsGroup {
                    item {
                        FullSpectrumColorPicker(
                            selectedArgb = otherSelected,
                            fallbackArgb = colorLong(defaults.otherContainer),
                            onColorSelected = { color ->
                                if (chat == null) updateGlobal { it.copy(otherBubbleArgb = color) }
                                else onChatChange(chat.bubbleColors.copy(otherArgb = color))
                            },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            item {
                SettingsExplainer(
                    stringResource(
                        if (chat == null) R.string.chat_bubble_colors_global_detail else R.string.chat_bubble_colors_chat_detail,
                        theme.label(),
                    ),
                )
            }
        }
    }
}

@Composable
private fun BubblePreview(
    mineArgb: Long?,
    otherArgb: Long?,
    modifier: Modifier = Modifier,
) {
    val defaults = LocalDefaultMessageBubbleColors.current
    val otherColors = otherArgb.readableColors(defaults.otherContainer, defaults.otherContent)
    val mineColors = mineArgb.readableColors(defaults.mineContainer, defaults.mineContent)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PreviewBubble(
            text = stringResource(R.string.bubble_preview_other),
            container = otherColors.first,
            content = otherColors.second,
            modifier = Modifier.align(Alignment.Start),
        )
        PreviewBubble(
            text = stringResource(R.string.bubble_preview_mine),
            container = mineColors.first,
            content = mineColors.second,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun PreviewBubble(text: String, container: Color, content: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = container, contentColor = content, shape = MaterialTheme.shapes.large) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@Composable
fun FullSpectrumColorPicker(
    selectedArgb: Long?,
    fallbackArgb: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initial = selectedArgb ?: fallbackArgb
    val initialHsv = remember(initial) { AppearanceColorPolicy.toHsv(initial) }
    var hue by rememberSaveable(initial) { mutableFloatStateOf(initialHsv.hue) }
    var saturation by rememberSaveable(initial) { mutableFloatStateOf(initialHsv.saturation) }
    var brightness by rememberSaveable(initial) { mutableFloatStateOf(initialHsv.value) }
    var hex by rememberSaveable(initial) { mutableStateOf(AppearanceColorPolicy.formatHex(initial)) }
    val parsedHex = AppearanceColorPolicy.parseHex(hex)
    val sliderArgb = AppearanceColorPolicy.fromHsv(HsvColor(hue, saturation, brightness))
    fun updateFromSliders(h: Float = hue, s: Float = saturation, v: Float = brightness) {
        hex = AppearanceColorPolicy.formatHex(AppearanceColorPolicy.fromHsv(HsvColor(h, s, v)))
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppearanceColorPolicy.presets.forEach { argb ->
                val description = stringResource(R.string.color_swatch_description, AppearanceColorPolicy.formatHex(argb))
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(colorFromOpaqueArgb(argb), CircleShape)
                        .border(
                            if (selectedArgb == argb) 3.dp else 1.dp,
                            if (selectedArgb == argb) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        )
                        .clickable { onColorSelected(argb) }
                        .semantics {
                            role = Role.RadioButton
                            selected = selectedArgb == argb
                            contentDescription = description
                        },
                )
            }
        }
        Text(stringResource(R.string.color_hue, hue.toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(value = hue, onValueChange = { hue = it; updateFromSliders(h = it) }, valueRange = 0f..359f)
        Text(stringResource(R.string.color_saturation, (saturation * 100).toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(value = saturation, onValueChange = { saturation = it; updateFromSliders(s = it) })
        Text(stringResource(R.string.color_brightness, (brightness * 100).toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(value = brightness, onValueChange = { brightness = it; updateFromSliders(v = it) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val preview = parsedHex ?: sliderArgb
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(colorFromOpaqueArgb(preview), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            OutlinedTextField(
                value = hex,
                onValueChange = { value ->
                    hex = value
                    AppearanceColorPolicy.parseHex(value)?.let { color ->
                        AppearanceColorPolicy.toHsv(color).let { hsv ->
                            hue = hsv.hue; saturation = hsv.saturation; brightness = hsv.value
                        }
                    }
                },
                label = { Text(stringResource(R.string.color_hex)) },
                supportingText = if (parsedHex == null) ({ Text(stringResource(R.string.color_hex_error)) }) else null,
                isError = parsedHex == null,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        WhiteNoiseButton(
            onClick = { parsedHex?.let(onColorSelected) },
            enabled = parsedHex != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.color_apply)) }
    }
}

@Composable
private fun AppearanceColorTheme.label(): String = stringResource(
    when (this) {
        AppearanceColorTheme.Light -> R.string.theme_light
        AppearanceColorTheme.Dark -> R.string.theme_dark
        AppearanceColorTheme.Amoled -> R.string.appearance_amoled
    },
)

private fun Long?.readableColors(defaultContainer: Color, defaultContent: Color): Pair<Color, Color> {
    val readable = AppearanceColorPolicy.readable(this) ?: return defaultContainer to defaultContent
    return colorFromOpaqueArgb(readable.containerArgb) to colorFromOpaqueArgb(readable.contentArgb)
}

private fun colorLong(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL
