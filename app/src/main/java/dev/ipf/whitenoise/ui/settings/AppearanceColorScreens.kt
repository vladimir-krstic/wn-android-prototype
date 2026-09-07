package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.ui.theme.outlineButtonColors
import dev.ipf.whitenoise.ui.theme.amoledOutlineBorder
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
import dev.ipf.whitenoise.ui.components.WhiteNoiseOutlinedButton
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import dev.ipf.whitenoise.ui.theme.whiteNoiseColorScheme
import dev.ipf.whitenoise.ui.theme.withActionColor
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
    if (!theme.supportsCustomColors) {
        OutlineColorNotice(stringResource(R.string.action_color), onBack)
        return
    }
    val selected = settings.colors.forTheme(theme).actionArgb
    key(profile.id, theme, selected) {
        ActionColorEditor(profile, theme, onBack, onChange)
    }
}

@Composable
private fun ActionColorEditor(
    profile: Profile,
    theme: AppearanceColorTheme,
    onBack: () -> Unit,
    onChange: (ProfileSettings) -> Unit,
) {
    val settings = profile.settings
    val initial = settings.colors.forTheme(theme).actionArgb
    var draft by rememberSaveable { mutableStateOf(initial) }
    var valid by rememberSaveable { mutableStateOf(true) }
    var reset by rememberSaveable { mutableStateOf(false) }
    var resetRevision by rememberSaveable { mutableStateOf(0) }
    val base = whiteNoiseColorScheme(settings.appearance, isSystemInDarkTheme())
    // Only this screen sees the trial accent; Save commits to the owning theme.
    MaterialTheme(colorScheme = withActionColor(base, draft)) {
        SettingsScaffold(
            title = stringResource(R.string.action_color), onBack = onBack,
            bottomBar = {
                SettingsBottomAction {
                    WhiteNoiseButton(
                        onClick = {
                            onChange(settings.copy(colors = settings.colors.updateTheme(theme) { it.copy(actionArgb = draft) }))
                            onBack()
                        },
                        enabled = valid && draft != initial,
                        modifier = Modifier.fillMaxWidth().testTag("action_color.save"),
                    ) { Text(stringResource(R.string.save)) }
                }
            },
        ) {
            SettingsList {
                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        SettingsGroup {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.FormField),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(stringResource(R.string.color_preview), style = MaterialTheme.typography.titleMedium)
                                    Button(onClick = {}, border = amoledOutlineBorder(), colors = outlineButtonColors(),
                                        modifier = Modifier.testTag("action_color.preview")) { Text(stringResource(R.string.color_preview_action)) }
                                }
                            }
                        }
                    }
                }
                item {
                    SettingsGroup {
                        item {
                            Column(
                                Modifier.padding(WhiteNoiseSpacing.FormField).testTag("action_color.controls"),
                                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
                            ) {
                                key(resetRevision) {
                                    FullSpectrumColorPicker(
                                        selectedArgb = if (reset) null else initial,
                                        fallbackArgb = colorLong(base.primary),
                                        onColorSelected = { draft = it },
                                        onValidityChanged = { valid = it },
                                    )
                                }
                                WhiteNoiseOutlinedButton(
                                    onClick = { draft = null; valid = true; reset = true; resetRevision++ },
                                    enabled = draft != null || !valid,
                                    modifier = Modifier.fillMaxWidth().testTag("action_color.reset"),
                                ) { Text(stringResource(R.string.reset_to_default)) }
                            }
                        }
                    }
                }
                item { SettingsExplainer(stringResource(R.string.action_color_detail, theme.label())) }
            }
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
    if (!theme.supportsCustomColors) {
        OutlineColorNotice(stringResource(R.string.chat_bubble_colors), onBack)
        return
    }
    val global = settings.colors.forTheme(theme)
    // Source changes start a fresh editor; local previews never mutate these inputs.
    key(profile.id, chat?.id, theme, global, chat?.bubbleColors) {
        ChatBubbleColorEditor(profile, chat, theme, onBack, onProfileChange, onChatChange)
    }
}

@Composable
private fun ChatBubbleColorEditor(
    profile: Profile,
    chat: Chat?,
    theme: AppearanceColorTheme,
    onBack: () -> Unit,
    onProfileChange: (ProfileSettings) -> Unit,
    onChatChange: (ChatBubbleColorOverrides) -> Unit,
) {
    val settings = profile.settings
    val global = settings.colors.forTheme(theme)
    val initialMine = if (chat == null) global.mineBubbleArgb else chat.bubbleColors.mineArgb
    val initialOther = if (chat == null) global.otherBubbleArgb else chat.bubbleColors.otherArgb
    var mine by rememberSaveable { mutableStateOf(initialMine) }
    var other by rememberSaveable { mutableStateOf(initialOther) }
    var reset by rememberSaveable { mutableStateOf(false) }
    var resetRevision by rememberSaveable { mutableStateOf(0) }
    var mineValid by rememberSaveable { mutableStateOf(true) }
    var otherValid by rememberSaveable { mutableStateOf(true) }
    val defaults = LocalDefaultMessageBubbleColors.current
    val inheritedMine = if (chat == null) null else global.mineBubbleArgb
    val inheritedOther = if (chat == null) null else global.otherBubbleArgb
    val mineSelected = (if (reset) null else initialMine) ?: inheritedMine
    val otherSelected = (if (reset) null else initialOther) ?: inheritedOther
    var menuOpen by remember { mutableStateOf(false) }
    val canReset = mine != null || other != null || !mineValid || !otherValid
    val changed = mine != initialMine || other != initialOther
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
                            mine = null; other = null
                            mineValid = true; otherValid = true
                            reset = true; resetRevision++
                        },
                    )),
                )
            }
        },
        bottomBar = {
            SettingsBottomAction {
                WhiteNoiseButton(
                    onClick = {
                        if (chat == null) onProfileChange(settings.copy(colors = settings.colors.updateTheme(theme) {
                            it.copy(mineBubbleArgb = mine, otherBubbleArgb = other)
                        })) else onChatChange(ChatBubbleColorOverrides(mineArgb = mine, otherArgb = other))
                        onBack()
                    },
                    enabled = changed && mineValid && otherValid,
                    modifier = Modifier.fillMaxWidth().testTag("bubble_colors.save"),
                ) { Text(stringResource(R.string.save)) }
            }
        },
    ) {
        SettingsList {
            stickyHeader {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    SettingsGroup {
                        item {
                            BubblePreview(
                                mineArgb = mine ?: inheritedMine,
                                otherArgb = other ?: inheritedOther,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
            item { SettingsSection(stringResource(R.string.bubble_my_messages)) }
            item {
                SettingsGroup {
                    item {
                        key(resetRevision) {
                            FullSpectrumColorPicker(
                                selectedArgb = mineSelected,
                                fallbackArgb = colorLong(defaults.mineContainer),
                                onColorSelected = { mine = it },
                                onValidityChanged = { mineValid = it },
                                modifier = Modifier.padding(16.dp).testTag("bubble_colors.mine.picker"),
                            )
                        }
                    }
                }
            }
            item { SettingsSection(stringResource(R.string.bubble_other_messages)) }
            item {
                SettingsGroup {
                    item {
                        key(resetRevision) {
                            FullSpectrumColorPicker(
                                selectedArgb = otherSelected,
                                fallbackArgb = colorLong(defaults.otherContainer),
                                onColorSelected = { other = it },
                                onValidityChanged = { otherValid = it },
                                modifier = Modifier.padding(16.dp).testTag("bubble_colors.other.picker"),
                            )
                        }
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
            modifier = Modifier.align(Alignment.Start).testTag("bubble_colors.other.preview"),
        )
        PreviewBubble(
            text = stringResource(R.string.bubble_preview_mine),
            container = mineColors.first,
            content = mineColors.second,
            modifier = Modifier.align(Alignment.End).testTag("bubble_colors.mine.preview"),
        )
    }
}

@Composable
private fun PreviewBubble(text: String, container: Color, content: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = container, contentColor = content, border = amoledOutlineBorder(), shape = MaterialTheme.shapes.large) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@Composable
private fun OutlineColorNotice(title: String, onBack: () -> Unit) {
    SettingsScaffold(title = title, onBack = onBack) {
        SettingsList { item { SettingsExplainer(stringResource(R.string.appearance_outline_colors_fixed)) } }
    }
}

@Composable
fun FullSpectrumColorPicker(
    selectedArgb: Long?,
    fallbackArgb: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onValidityChanged: (Boolean) -> Unit = {},
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
        val color = AppearanceColorPolicy.fromHsv(HsvColor(h, s, v))
        hex = AppearanceColorPolicy.formatHex(color)
        onValidityChanged(true)
        onColorSelected(color)
    }
    val swatchSelection = parsedHex
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
                            if (swatchSelection == argb) 3.dp else 1.dp,
                            if (swatchSelection == argb) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        )
                        .clickable {
                            val hsv = AppearanceColorPolicy.toHsv(argb)
                            hue = hsv.hue; saturation = hsv.saturation; brightness = hsv.value
                            hex = AppearanceColorPolicy.formatHex(argb)
                            onValidityChanged(true)
                            onColorSelected(argb)
                        }
                        .semantics {
                            role = Role.RadioButton
                            selected = swatchSelection == argb
                            contentDescription = description
                        },
                )
            }
        }
        val hueLabel = stringResource(R.string.color_hue, hue.toInt())
        Text(hueLabel, style = MaterialTheme.typography.labelLarge)
        Slider(value = hue, onValueChange = { hue = it; updateFromSliders(h = it) }, valueRange = 0f..359f,
            modifier = Modifier.testTag("color.hue").semantics { contentDescription = hueLabel })
        val saturationLabel = stringResource(R.string.color_saturation, (saturation * 100).toInt())
        Text(saturationLabel, style = MaterialTheme.typography.labelLarge)
        Slider(value = saturation, onValueChange = { saturation = it; updateFromSliders(s = it) },
            modifier = Modifier.testTag("color.saturation").semantics { contentDescription = saturationLabel })
        val brightnessLabel = stringResource(R.string.color_brightness, (brightness * 100).toInt())
        Text(brightnessLabel, style = MaterialTheme.typography.labelLarge)
        Slider(value = brightness, onValueChange = { brightness = it; updateFromSliders(v = it) },
            modifier = Modifier.testTag("color.brightness").semantics { contentDescription = brightnessLabel })
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
                    val parsed = AppearanceColorPolicy.parseHex(value)
                    onValidityChanged(parsed != null)
                    parsed?.let { color ->
                        onColorSelected(color)
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
    }
}

@Composable
private fun AppearanceColorTheme.label(): String = stringResource(
    when (this) {
        AppearanceColorTheme.Light -> R.string.theme_light
        AppearanceColorTheme.Dark -> R.string.theme_dark
        AppearanceColorTheme.Amoled -> R.string.appearance_amoled
        AppearanceColorTheme.AmoledOutline -> R.string.appearance_amoled_outline
    },
)

private fun Long?.readableColors(defaultContainer: Color, defaultContent: Color): Pair<Color, Color> {
    val readable = AppearanceColorPolicy.readable(this) ?: return defaultContainer to defaultContent
    return colorFromOpaqueArgb(readable.containerArgb) to colorFromOpaqueArgb(readable.contentArgb)
}

private fun colorLong(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL
