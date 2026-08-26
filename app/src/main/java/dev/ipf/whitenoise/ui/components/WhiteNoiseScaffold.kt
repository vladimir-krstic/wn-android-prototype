package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
internal val LocalWhiteNoiseHeaderScroll = compositionLocalOf<TopAppBarScrollBehavior?> { null }

/** Each destination owns a pinned Material header. Nested sheets keep their own presentation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteNoiseScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    CompositionLocalProvider(LocalWhiteNoiseHeaderScroll provides scrollBehavior) {
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
            content = content,
        )
    }
}

/** Sync real position too: nested-scroll events alone miss restoration and scrollToItem(). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackHeaderScroll(state: LazyListState) {
    val behavior = LocalWhiteNoiseHeaderScroll.current
    DisposableEffect(behavior) { onDispose { behavior?.state?.contentOffset = 0f } }
    LaunchedEffect(state, behavior) {
        if (behavior == null) return@LaunchedEffect
        snapshotFlow {
            Triple(state.layoutInfo.totalItemsCount, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }.collect { (count, index, offset) ->
            behavior.state.contentOffset = when {
                count == 0 -> 0f
                index > 0 -> behavior.state.heightOffsetLimit
                else -> -offset.toFloat()
            }
        }
    }
}

@Composable
fun WhiteNoiseLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    TrackHeaderScroll(state)
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.whiteNoiseVerticalScroll(state: ScrollState? = null): Modifier = composed {
    val actualState = state ?: rememberScrollState()
    val behavior = LocalWhiteNoiseHeaderScroll.current
    DisposableEffect(behavior) { onDispose { behavior?.state?.contentOffset = 0f } }
    LaunchedEffect(actualState, behavior) {
        snapshotFlow { actualState.value }.collect { behavior?.state?.contentOffset = -it.toFloat() }
    }
    verticalScroll(actualState)
}

@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.trackWhiteNoiseHeader(state: LazyGridState): Modifier = composed {
    val behavior = LocalWhiteNoiseHeaderScroll.current
    DisposableEffect(behavior) { onDispose { behavior?.state?.contentOffset = 0f } }
    LaunchedEffect(state, behavior) {
        if (behavior == null) return@LaunchedEffect
        snapshotFlow {
            Triple(state.layoutInfo.totalItemsCount, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }.collect { (count, index, offset) ->
            behavior.state.contentOffset = when {
                count == 0 -> 0f
                index > 0 -> behavior.state.heightOffsetLimit
                else -> -offset.toFloat()
            }
        }
    }
    this
}
