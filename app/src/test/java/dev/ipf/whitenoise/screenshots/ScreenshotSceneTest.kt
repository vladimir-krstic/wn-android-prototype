package dev.ipf.whitenoise.screenshots

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenshotSceneTest {
    @Test
    fun resolvesEveryPublicSceneRoute() {
        ScreenshotScene.entries.forEach { scene ->
            assertEquals(scene, ScreenshotScene.fromRoute(scene.route))
        }
    }

    @Test
    fun rejectsUnknownOrMissingRoutes() {
        assertNull(ScreenshotScene.fromRoute(null))
        assertNull(ScreenshotScene.fromRoute(""))
        assertNull(ScreenshotScene.fromRoute("settings"))
    }
}

