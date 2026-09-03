package dev.ipf.whitenoise.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaImageSamplingTest {
    @Test
    fun fullScreenScreenshotRetainsItsPixels() {
        assertEquals(1, mediaImageSampleSize(1080, 2400, 1080, 2400))
    }

    @Test
    fun thumbnailDecodingDoesNotAllocateTheFullPhoto() {
        assertEquals(16, mediaImageSampleSize(4096, 3072, 192, 192))
        assertEquals(16, mediaImageSampleSize(3072, 4096, 192, 192))
    }

    @Test
    fun croppedPanoramaKeepsEnoughVerticalDetail() {
        assertEquals(2, mediaImageSampleSize(4096, 512, 192, 192))
    }

    @Test
    fun smallOrInvalidSourcesAreNeverFurtherDownsampled() {
        assertEquals(1, mediaImageSampleSize(120, 80, 192, 192))
        assertEquals(1, mediaImageSampleSize(-1, -1, 192, 192))
        assertEquals(1, mediaImageSampleSize(4096, 3072, 0, 0))
    }
}
