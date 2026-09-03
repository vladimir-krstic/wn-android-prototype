package dev.ipf.whitenoise.ui.conversation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryDismissPolicyTest {
    @Test
    fun deliberateDownwardPullClosesButShortPullReturns() {
        assertFalse(GalleryDismissPolicy.shouldDismiss(119f, 0f, 800f))
        assertTrue(GalleryDismissPolicy.shouldDismiss(120f, 0f, 800f))
    }

    @Test
    fun shortWindowsUseProportionalTravel() {
        assertFalse(GalleryDismissPolicy.shouldDismiss(59f, 0f, 300f))
        assertTrue(GalleryDismissPolicy.shouldDismiss(60f, 0f, 300f))
    }

    @Test
    fun downwardFlickRequiresMinimumDeliberateTravel() {
        assertFalse(GalleryDismissPolicy.shouldDismiss(31f, 2_000f, 800f))
        assertFalse(GalleryDismissPolicy.shouldDismiss(32f, 1_249f, 800f))
        assertTrue(GalleryDismissPolicy.shouldDismiss(32f, 1_250f, 800f))
    }

    @Test
    fun strongUpwardReversalCancelsEvenAfterCrossingDistanceThreshold() {
        assertFalse(GalleryDismissPolicy.shouldDismiss(160f, -1_250f, 800f))
    }

    @Test
    fun stationaryUpwardAndUnmeasuredGesturesCannotClose() {
        assertFalse(GalleryDismissPolicy.shouldDismiss(0f, 2_000f, 800f))
        assertFalse(GalleryDismissPolicy.shouldDismiss(-120f, 2_000f, 800f))
        assertFalse(GalleryDismissPolicy.shouldDismiss(120f, 0f, 0f))
    }
}
