package dev.ipf.whitenoise.model

import dev.ipf.whitenoise.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class AppFontFamily(val labelRes: Int) {
    System(R.string.appearance_font_system),
    Manrope(R.string.appearance_font_manrope),
    Outfit(R.string.appearance_font_outfit),
    Urbanist(R.string.appearance_font_urbanist),
    Figtree(R.string.appearance_font_figtree),
}

enum class AppFontSize(val factor: Float, val labelRes: Int) {
    Small(0.85f, R.string.appearance_font_small),
    Default(1f, R.string.appearance_font_default),
    Large(1.15f, R.string.appearance_font_large),
    ExtraLarge(1.3f, R.string.appearance_font_extra_large),
}

enum class EnterKeyBehavior(val labelRes: Int) {
    NewLine(R.string.appearance_enter_newline),
    SendMessage(R.string.appearance_enter_send),
}

enum class ComposerEnterDecision { Native, Consume, Send }

/** Recognized hardware command only; never transform pasted or dictated text. */
object ComposerEnterPolicy {
    fun decide(
        preference: EnterKeyBehavior,
        isEnter: Boolean,
        isKeyDown: Boolean,
        shift: Boolean,
        otherModifier: Boolean,
        repeated: Boolean,
        composing: Boolean,
        enabled: Boolean,
        sendable: Boolean,
    ): ComposerEnterDecision = when {
        !isEnter || preference != EnterKeyBehavior.SendMessage || shift || otherModifier || composing -> ComposerEnterDecision.Native
        !isKeyDown || repeated || !enabled || !sendable -> ComposerEnterDecision.Consume
        else -> ComposerEnterDecision.Send
    }
}

enum class AppearanceColorTheme {
    Light,
    Dark,
    Amoled,
    ;

    companion object {
        fun resolve(appearance: AppearancePreference, systemDark: Boolean): AppearanceColorTheme = when (appearance) {
            AppearancePreference.System -> if (systemDark) Dark else Light
            AppearancePreference.Light -> Light
            AppearancePreference.Dark -> Dark
            AppearancePreference.Amoled -> Amoled
        }
    }
}

data class ThemeColorOverrides(
    val actionArgb: Long? = null,
    val mineBubbleArgb: Long? = null,
    val otherBubbleArgb: Long? = null,
)

data class AppearanceColorPreferences(
    val light: ThemeColorOverrides = ThemeColorOverrides(),
    val dark: ThemeColorOverrides = ThemeColorOverrides(),
    val amoled: ThemeColorOverrides = ThemeColorOverrides(),
) {
    fun forTheme(theme: AppearanceColorTheme): ThemeColorOverrides = when (theme) {
        AppearanceColorTheme.Light -> light
        AppearanceColorTheme.Dark -> dark
        AppearanceColorTheme.Amoled -> amoled
    }

    fun updateTheme(
        theme: AppearanceColorTheme,
        transform: (ThemeColorOverrides) -> ThemeColorOverrides,
    ): AppearanceColorPreferences = when (theme) {
        AppearanceColorTheme.Light -> copy(light = transform(light))
        AppearanceColorTheme.Dark -> copy(dark = transform(dark))
        AppearanceColorTheme.Amoled -> copy(amoled = transform(amoled))
    }
}

data class ChatBubbleColorOverrides(
    val mineArgb: Long? = null,
    val otherArgb: Long? = null,
)

data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

data class ReadableColor(
    val containerArgb: Long,
    val contentArgb: Long,
)

object AppearanceColorPolicy {
    const val OPAQUE_BLACK_ARGB = 0xFF000000L
    const val OPAQUE_WHITE_ARGB = 0xFFFFFFFFL
    const val MINIMUM_TEXT_CONTRAST = 4.5

    val presets: List<Long> = listOf(
        OPAQUE_BLACK_ARGB,
        OPAQUE_WHITE_ARGB,
        0xFFB91C1CL,
        0xFFC2410CL,
        0xFFA16207L,
        0xFF15803DL,
        0xFF0E7490L,
        0xFF1D4ED8L,
        0xFF6D28D9L,
        0xFFBE185DL,
    )

    fun normalizeOpaqueArgb(argb: Long?): Long? = argb?.takeIf {
        it in 0L..0xFFFFFFFFL && (it and 0xFF000000L) == 0xFF000000L
    }

    fun parseHex(input: String): Long? {
        val rgb = input.trim().removePrefix("#")
        if (rgb.length != 6 || rgb.any { it.digitToIntOrNull(16) == null }) return null
        return OPAQUE_BLACK_ARGB or rgb.toLong(16)
    }

    fun formatHex(argb: Long): String = "#%06X".format(argb and 0xFFFFFFL)

    fun readable(argb: Long?): ReadableColor? {
        val container = normalizeOpaqueArgb(argb) ?: return null
        val black = contrastRatio(OPAQUE_BLACK_ARGB, container)
        val white = contrastRatio(OPAQUE_WHITE_ARGB, container)
        val content = when {
            black >= white && black >= MINIMUM_TEXT_CONTRAST -> OPAQUE_BLACK_ARGB
            white >= MINIMUM_TEXT_CONTRAST -> OPAQUE_WHITE_ARGB
            black >= MINIMUM_TEXT_CONTRAST -> OPAQUE_BLACK_ARGB
            else -> return null
        }
        return ReadableColor(container, content)
    }

    fun effectiveBubble(
        chatOverride: Long?,
        globalOverride: Long?,
    ): Long? = normalizeOpaqueArgb(chatOverride) ?: normalizeOpaqueArgb(globalOverride)

    fun contrastRatio(foregroundArgb: Long, backgroundArgb: Long): Double {
        val foreground = normalizeOpaqueArgb(foregroundArgb) ?: return 0.0
        val background = normalizeOpaqueArgb(backgroundArgb) ?: return 0.0
        val lighter = max(relativeLuminance(foreground), relativeLuminance(background))
        val darker = min(relativeLuminance(foreground), relativeLuminance(background))
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun toHsv(argb: Long): HsvColor {
        val color = normalizeOpaqueArgb(argb) ?: OPAQUE_BLACK_ARGB
        val r = ((color shr 16) and 0xFF).toFloat() / 255f
        val g = ((color shr 8) and 0xFF).toFloat() / 255f
        val b = (color and 0xFF).toFloat() / 255f
        val high = max(r, max(g, b))
        val low = min(r, min(g, b))
        val delta = high - low
        val hue = when {
            delta == 0f -> 0f
            high == r -> 60f * (((g - b) / delta) % 6f)
            high == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return HsvColor(hue, if (high == 0f) 0f else delta / high, high)
    }

    fun fromHsv(hsv: HsvColor): Long {
        val hue = ((hsv.hue % 360f) + 360f) % 360f
        val saturation = hsv.saturation.coerceIn(0f, 1f)
        val value = hsv.value.coerceIn(0f, 1f)
        val chroma = value * saturation
        val section = hue / 60f
        val x = chroma * (1f - abs(section % 2f - 1f))
        val (r1, g1, b1) = when (section.toInt().coerceIn(0, 5)) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        val m = value - chroma
        fun channel(value: Float): Long = ((value + m) * 255f).toInt().coerceIn(0, 255).toLong()
        return OPAQUE_BLACK_ARGB or (channel(r1) shl 16) or (channel(g1) shl 8) or channel(b1)
    }

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val srgb = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (srgb <= 0.04045) srgb / 12.92 else ((srgb + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
