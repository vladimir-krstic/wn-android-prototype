package dev.ipf.whitenoise

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.EmojiCatalog
import dev.ipf.whitenoise.ui.components.signalEmojiAtlasContains
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignalEmojiAssetTest {
    @Test
    fun pinnedSignalAtlasCoversTheCompleteAcceptedReactionCatalog() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val missing = EmojiCatalog.all.filterNot { signalEmojiAtlasContains(context, it) }

        assertTrue("Missing Signal emoji artwork for: ${missing.joinToString()}", missing.isEmpty())
    }
}
