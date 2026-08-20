package dev.ipf.whitenoise

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ipf.whitenoise.model.AvatarAsset
import dev.ipf.whitenoise.ui.components.drawableResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppResourceIntegrityTest {
    @Test
    fun everyAvatarFixtureHasOneDistinctCompiledDrawable() {
        val ids = AvatarAsset.entries.map(AvatarAsset::drawableResource)
        assertEquals(45, ids.size)
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all { it != 0 })
    }

    @Test
    fun launcherAndBundledHandoffFilesArePackaged() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launcher = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        assertTrue(launcher != 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertNotNull(context.getDrawable(R.drawable.ic_white_noise_splash))
        }
        val density = context.resources.displayMetrics.density
        assertEquals(
            149.5f,
            context.resources.getDimension(R.dimen.wn_launch_mark_width) / density,
            0.01f,
        )
        assertEquals(
            115f,
            context.resources.getDimension(R.dimen.wn_launch_mark_height) / density,
            0.01f,
        )

        listOf(
            R.raw.chat_trail_clip,
            R.raw.project_brief,
            R.raw.project_notes,
            R.raw.trail_plan,
            R.raw.weekend_notes,
        ).forEach { resource ->
            context.resources.openRawResource(resource).use { assertTrue(it.read() != -1) }
        }
    }
}
