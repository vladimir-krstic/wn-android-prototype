package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class GroupEmojiSelectionTest {
    @Test fun twoSlotsKeepCompleteGraphemesAndRejectThirdWithoutReplacement() {
        val pair = GroupEmojiSelection().add("👨‍👩‍👧‍👦").add("🇷🇸")
        val rejected = pair.add("🌲")
        assertEquals(pair.emojis, rejected.emojis); assertTrue(rejected.limitReached)
    }
    @Test fun removalAllowsReplacementAndClearsTheLimit() {
        val value = GroupEmojiSelection().add("🌲").add("🌲").add("🦊").remove(0).add("🦊")
        assertEquals(listOf("🌲", "🦊"), value.emojis); assertFalse(value.limitReached)
    }
    @Test fun blankSelectionCannotConsumeASlot() {
        assertEquals(GroupEmojiSelection(), GroupEmojiSelection().add(" "))
    }
}
