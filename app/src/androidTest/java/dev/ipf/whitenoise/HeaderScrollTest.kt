package dev.ipf.whitenoise

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeaderScrollTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun headerFollowsInitialProgrammaticEmptyAndReturnToTopPositions() {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        var rest = Color.Unspecified
        var scrolled = Color.Unspecified
        val count = mutableIntStateOf(60)
        rule.setContent {
            WhiteNoiseTheme {
                val list = rememberLazyListState(initialFirstVisibleItemIndex = 12)
                val coroutineScope = rememberCoroutineScope()
                val colors = MaterialTheme.colorScheme
                SideEffect { state = list; scope = coroutineScope; rest = colors.surface; scrolled = colors.surfaceContainer }
                WhiteNoiseScaffold(topBar = { WhiteNoiseTopBar("Header", {}, modifier = Modifier.testTag("header")) }) { padding ->
                    WhiteNoiseLazyColumn(Modifier.fillMaxSize().padding(padding), state = list) {
                        items(count.intValue) { Text("Row $it", Modifier.padding(24.dp)) }
                    }
                }
            }
        }
        fun assertBackground(expected: Color) {
            rule.waitForIdle()
            val pixels = rule.onNodeWithTag("header").captureToImage().toPixelMap()
            val actual = pixels[pixels.width - 2, pixels.height - 2]
            assertEquals(expected.red, actual.red, .01f)
            assertEquals(expected.green, actual.green, .01f)
            assertEquals(expected.blue, actual.blue, .01f)
        }
        assertBackground(scrolled)
        rule.runOnIdle { scope.launch { state.scrollToItem(0) } }
        assertBackground(rest)
        rule.runOnIdle { scope.launch { state.scrollToItem(20) } }
        assertBackground(scrolled)
        rule.runOnIdle { count.intValue = 0 }
        assertBackground(rest)
    }
}
