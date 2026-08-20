package dev.ipf.whitenoise.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarImageProcessorTest {
    @Test
    fun targetSizePreservesSmallImages() {
        assertEquals(ImageSize(320, 480), AvatarImageProcessor.targetSize(320, 480))
    }

    @Test
    fun targetSizeBoundsLargestDimensionAndPreservesAspect() {
        assertEquals(ImageSize(512, 256), AvatarImageProcessor.targetSize(2048, 1024))
        assertEquals(ImageSize(256, 512), AvatarImageProcessor.targetSize(1024, 2048))
    }
}
