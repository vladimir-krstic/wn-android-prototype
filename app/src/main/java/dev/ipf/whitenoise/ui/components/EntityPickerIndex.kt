package dev.ipf.whitenoise.ui.components

import java.text.BreakIterator
import java.text.Collator
import java.util.Locale

internal data class PickerSection(val label: String, val index: Int)

internal fun sortedPickerItems(items: List<WhiteNoisePickerItem>, locale: Locale): List<WhiteNoisePickerItem> {
    val collator = Collator.getInstance(locale)
    return items.sortedWith { first, second ->
        collator.compare(first.title.trim(), second.title.trim()).takeIf { it != 0 }
            ?: first.id.compareTo(second.id)
    }
}

internal fun pickerSections(items: List<WhiteNoisePickerItem>, locale: Locale): List<PickerSection> {
    val boundaries = BreakIterator.getCharacterInstance(locale)
    return items.mapIndexed { index, item ->
        val name = item.title.trim()
        val initial = if (name.isEmpty()) "#" else {
            boundaries.setText(name)
            name.substring(0, boundaries.next()).uppercase(locale)
        }
        PickerSection(initial, index)
    }.distinctBy { it.label }
}
