package dev.ipf.whitenoise.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplySwipeMotionTest {
    @Test
    fun `bubble follows the finger until readiness then resists toward its maximum`() {
        assertEquals(0f, resistedReplySwipeDistance(-12f, 64f, 96f), 0.001f)
        assertEquals(32f, resistedReplySwipeDistance(32f, 64f, 96f), 0.001f)
        assertEquals(64f, resistedReplySwipeDistance(64f, 64f, 96f), 0.001f)
        assertEquals(74.666f, resistedReplySwipeDistance(96f, 64f, 96f), 0.001f)
        assertEquals(80f, resistedReplySwipeDistance(128f, 64f, 96f), 0.001f)

        val distantOverdrag = resistedReplySwipeDistance(10_000f, 64f, 96f)
        assertTrue(distantOverdrag > 64f)
        assertTrue(distantOverdrag < 96f)
    }
}
