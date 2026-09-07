package dev.ipf.whitenoise.ui.components

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityPickerIndexTest {
    @Test fun duplicateNamesKeepStableIdentityAndJumpToFirstMatch() {
        val items = sortedPickerItems(listOf(
            WhiteNoisePickerItem("z", "Zoe"), WhiteNoisePickerItem("b", "Alex"),
            WhiteNoisePickerItem("a", "Alex"), WhiteNoisePickerItem("m", "Maya"),
        ), Locale.ENGLISH)
        assertEquals(listOf("a", "b", "m", "z"), items.map { it.id })
        assertEquals(listOf(PickerSection("A", 0), PickerSection("M", 2), PickerSection("Z", 3)), pickerSections(items, Locale.ENGLISH))
        assertEquals(listOf(PickerSection("M", 0)), pickerSections(items.filter { it.title == "Maya" }, Locale.ENGLISH))
    }

    @Test fun initialUsesLocaleAndKeepsCombiningCharacterIntact() {
        val items = listOf(WhiteNoisePickerItem("i", "ipek"), WhiteNoisePickerItem("e", "e\u0301mile"))
        assertEquals(listOf("İ", "E\u0301"), pickerSections(items, Locale.forLanguageTag("tr")).map { it.label })
    }

    @Test fun nonLatinAndEmptyNamesHaveUsableSections() {
        val items = listOf(WhiteNoisePickerItem("a", " Ана"), WhiteNoisePickerItem("b", "王明"), WhiteNoisePickerItem("c", " "))
        assertEquals(listOf("А", "王", "#"), pickerSections(items, Locale.ROOT).map { it.label })
        assertEquals(emptyList<PickerSection>(), pickerSections(emptyList(), Locale.ROOT))
    }
}
