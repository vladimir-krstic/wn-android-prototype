package dev.ipf.whitenoise.ui.settings

import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test

class IncognitoKeyboardTest {
    @Test fun requestPreservesImeActionAndExistingFlags() {
        val before=EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        val after=incognitoImeOptions(before,true,26)
        assertEquals(before or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING,after)
        assertEquals(EditorInfo.IME_ACTION_SEND,after and EditorInfo.IME_MASK_ACTION)
    }
    @Test fun olderAndroidAndDisabledPreferenceDoNotChangeFieldOptions() {
        val before=EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        assertEquals(before,incognitoImeOptions(before,false,37)); assertEquals(before,incognitoImeOptions(before,true,25))
    }
}
