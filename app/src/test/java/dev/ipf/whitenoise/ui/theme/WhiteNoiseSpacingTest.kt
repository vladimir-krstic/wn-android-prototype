package dev.ipf.whitenoise.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhiteNoiseSpacingTest {
    @Test
    fun customLayoutTokensUseTheDocumentedAndroidGrid() {
        assertEquals(16.dp, WhiteNoiseSpacing.CompactScreenMargin)
        assertEquals(8.dp, WhiteNoiseSpacing.Related)
        assertEquals(16.dp, WhiteNoiseSpacing.FormField)
        assertEquals(24.dp, WhiteNoiseSpacing.Section)
        assertEquals(16.dp, WhiteNoiseSpacing.PinnedActionInset)

        listOf(
            WhiteNoiseSpacing.CompactScreenMargin,
            WhiteNoiseSpacing.Related,
            WhiteNoiseSpacing.FormField,
            WhiteNoiseSpacing.Section,
            WhiteNoiseSpacing.PinnedActionInset,
        ).forEach { spacing ->
            assertTrue(spacing.value % 8f == 0f)
        }
    }
}
