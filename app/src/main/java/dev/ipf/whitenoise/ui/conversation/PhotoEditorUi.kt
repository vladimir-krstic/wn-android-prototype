package dev.ipf.whitenoise.ui.conversation

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoEditorDialog(session: PhotoEditorSession, onEvent: (PhotoEditorEvent) -> Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestEvent = rememberUpdatedState(onEvent)
    var preview by remember(session.id) { mutableStateOf<PhotoEditorPreview?>(null) }
    var discard by rememberSaveable(session.id) { mutableStateOf(false) }
    var qualityOpen by rememberSaveable(session.id) { mutableStateOf(false) }
    var coordinatesOpen by rememberSaveable(session.id) { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<Int?>(null) }
    fun act(event: PhotoEditorEvent) {
        if (onEvent(event)) announcement = when (event) {
            PhotoEditorEvent.Rotate -> R.string.photo_editor_rotated
            PhotoEditorEvent.Undo -> R.string.photo_editor_undo_done
            PhotoEditorEvent.Redo -> R.string.photo_editor_redo_done
            PhotoEditorEvent.Reset -> R.string.photo_editor_reset_done
            is PhotoEditorEvent.Stroke -> if (session.tool == PhotoEditorTool.Erase) R.string.photo_editor_erasing_added else R.string.photo_editor_drawing_added
            is PhotoEditorEvent.Crop, is PhotoEditorEvent.SelectPreset -> R.string.photo_editor_crop_changed
            else -> null
        }
    }
    fun close() {
        if (session.phase == PhotoEditorPhase.Saving) return
        if (session.dirty) discard = true else onEvent(PhotoEditorEvent.Close)
    }
    LaunchedEffect(session.id, session.phase == PhotoEditorPhase.Loading, session.source) {
        if (session.phase == PhotoEditorPhase.Loading || (preview?.bitmap == null && session.phase in setOf(PhotoEditorPhase.Ready, PhotoEditorPhase.Saving))) {
            val source = session.source
            val loaded = if (session.scenario == PhotoEditorScenario.LoadFailure || source == null) PhotoEditorPreview(null, null, PhotoEditorFailure.Unavailable)
                else PhotoEditorRenderer.preview(context, source)
            preview = loaded
            if (session.phase == PhotoEditorPhase.Loading) latestEvent.value(PhotoEditorEvent.Loaded(session.revision,
                loaded.source?.orientedWidth ?: 0, loaded.source?.orientedHeight ?: 0, loaded.failure))
        }
    }
    LaunchedEffect(session.id, session.phase, session.revision) {
        if (session.phase == PhotoEditorPhase.Saving) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(150)
            val source = session.source
            val rendered = if (session.scenario == PhotoEditorScenario.SaveFailure || source == null) PhotoEditorRender(failure = PhotoEditorFailure.SaveFailed)
                else PhotoEditorRenderer.renderFrame(context, source, session.history.current, session.requestedQuality)
            latestEvent.value(PhotoEditorEvent.Saved(session.revision, rendered.attachment, rendered.failure))
        }
    }
    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(enabled = !discard && !qualityOpen && !coordinatesOpen, onBack = ::close)
        Scaffold(modifier = Modifier.fillMaxSize().testTag("photo.editor"), contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { TopAppBar(title = { Column {
                Text(stringResource(R.string.photo_editor_title))
                if (session.expectedAttachment.images.size > 1) Text(stringResource(R.string.photo_editor_frame, session.imageIndex + 1), style = MaterialTheme.typography.labelMedium)
            } },
                navigationIcon = { IconButton(::close, enabled = session.phase != PhotoEditorPhase.Saving) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close)) } },
                actions = { TextButton({ act(PhotoEditorEvent.Save) }, enabled = session.editable && preview?.bitmap != null, modifier = Modifier.testTag("photo.editor.save")) {
                    Text(stringResource(if (session.phase == PhotoEditorPhase.Saving) R.string.photo_editor_saving else R.string.save))
                } }) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton({ act(PhotoEditorEvent.Undo) }, enabled = session.editable && session.history.undo.isNotEmpty()) { Text(stringResource(R.string.undo)) }
                    TextButton({ act(PhotoEditorEvent.Redo) }, enabled = session.editable && session.history.redo.isNotEmpty()) { Text(stringResource(R.string.photo_editor_redo)) }
                    TextButton({ act(PhotoEditorEvent.Reset) }, enabled = session.editable && session.dirty) { Text(stringResource(R.string.reset)) }
                    TextButton({ act(PhotoEditorEvent.Rotate) }, enabled = session.editable) { Text(stringResource(R.string.photo_editor_rotate)) }
                }
                if (session.phase == PhotoEditorPhase.Saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val wide = maxWidth >= 600.dp || maxHeight < 360.dp
                    @Composable fun controls(modifier: Modifier) {
                        PhotoEditorControls(session, { act(it) }, { coordinatesOpen = true }, { qualityOpen = true }, modifier)
                    }
                    @Composable fun photo(modifier: Modifier) {
                        val bitmap = preview?.bitmap
                        if (bitmap == null) Box(modifier, contentAlignment = Alignment.Center) {
                            if (session.phase == PhotoEditorPhase.Loading || (preview == null && session.phase == PhotoEditorPhase.Ready)) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(); Text(stringResource(R.string.photo_editor_loading))
                            }
                            else Text(stringResource(R.string.photo_editor_unavailable), Modifier.padding(16.dp))
                        } else PhotoEditorCanvas(bitmap, session, { act(it) }, modifier)
                    }
                    if (wide) Row(Modifier.fillMaxSize()) {
                        photo(Modifier.weight(1f).fillMaxHeight().padding(8.dp))
                        controls(Modifier.width(280.dp).fillMaxHeight())
                    } else Column(Modifier.fillMaxSize()) {
                        photo(Modifier.weight(1f).fillMaxWidth().heightIn(min = 120.dp).padding(8.dp))
                        controls(Modifier.fillMaxWidth().heightIn(max = 300.dp))
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                    announcement?.let { Text(stringResource(it), style = MaterialTheme.typography.labelSmall) }
                    session.limit?.let { Text(stringResource(when (it) { PhotoEditLimit.Strokes -> R.string.photo_editor_stroke_limit; PhotoEditLimit.StrokePoints -> R.string.photo_editor_point_limit; PhotoEditLimit.TotalPoints -> R.string.photo_editor_total_limit }), style = MaterialTheme.typography.bodySmall) }
                    session.failure?.let { failure ->
                        Text(stringResource(photoEditorFailureLabel(failure)), color = MaterialTheme.colorScheme.error)
                        if (failure != PhotoEditorFailure.SourceChanged) TextButton({ act(PhotoEditorEvent.Retry) }) { Text(stringResource(R.string.attachment_retry)) }
                    }
                }
            }
        }
        if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.photo_editor_discard_title)) },
            text = { Text(stringResource(R.string.photo_editor_discard_detail)) },
            confirmButton = { TextButton({ discard = false; onEvent(PhotoEditorEvent.Close) }) { Text(stringResource(R.string.photo_editor_discard)) } },
            dismissButton = { TextButton({ discard = false }) { Text(stringResource(R.string.photo_editor_keep_editing)) } })
        if (qualityOpen) PhotoQualityDialog(session.quality, { qualityOpen = false }) { quality -> qualityOpen = false; act(PhotoEditorEvent.SelectQuality(quality)) }
        if (coordinatesOpen) PhotoEditorCoordinates(session, { coordinatesOpen = false }) { event -> coordinatesOpen = false; act(event) }
    }
}

@Composable
private fun PhotoEditorControls(session: PhotoEditorSession, onEvent: (PhotoEditorEvent) -> Unit, onCoordinates: () -> Unit, onQuality: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhotoEditorTool.entries.forEach { tool -> FilterChip(session.tool == tool, { onEvent(PhotoEditorEvent.SelectTool(tool)) },
                label = { Text(stringResource(photoToolLabel(tool))) }, enabled = session.editable, modifier = Modifier.testTag("photo.editor.tool.${tool.name}")) }
        }
        if (session.tool == PhotoEditorTool.Crop) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoCropPreset.entries.forEach { preset -> FilterChip(session.preset == preset, { onEvent(PhotoEditorEvent.SelectPreset(preset)) },
                    label = { Text(stringResource(photoPresetLabel(preset))) }, enabled = session.editable, modifier = Modifier.testTag("photo.editor.preset.${preset.name}")) }
            }
        } else {
            if (session.tool == PhotoEditorTool.Draw) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoPenColor.entries.forEach { color -> FilterChip(session.color == color, { onEvent(PhotoEditorEvent.SelectColor(color)) },
                    label = { Text(stringResource(photoColorLabel(color))) }, enabled = session.editable,
                    leadingIcon = { Canvas(Modifier.size(20.dp)) { drawCircle(Color(color.argb)); drawCircle(Color.Gray, style = Stroke(1.dp.toPx())) } },
                    modifier = Modifier.testTag("photo.editor.color.${color.name}")) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoPenWidth.entries.forEach { width -> FilterChip(session.width == width, { onEvent(PhotoEditorEvent.SelectWidth(width)) },
                    label = { Text(stringResource(photoWidthLabel(width))) }, enabled = session.editable, modifier = Modifier.testTag("photo.editor.width.${width.name}")) }
            }
        }
        Text(stringResource(if (session.tool == PhotoEditorTool.Crop) R.string.photo_editor_crop_hint else R.string.photo_editor_draw_hint), style = MaterialTheme.typography.bodySmall)
        TextButton(onCoordinates, enabled = session.editable) { Text(stringResource(if (session.tool == PhotoEditorTool.Crop) R.string.photo_editor_adjust_crop else if (session.tool == PhotoEditorTool.Draw) R.string.photo_editor_draw_line else R.string.photo_editor_erase_line)) }
        TextButton(onQuality, enabled = session.editable, modifier = Modifier.testTag("photo.editor.quality")) { Text(stringResource(R.string.photo_quality) + ": " + photoQualityLabel(session.quality)) }
        if (session.requestedQuality == PhotoQuality.Original && !session.history.current.isOriginal) Text(stringResource(R.string.photo_editor_original_edited), style = MaterialTheme.typography.bodySmall)
    }
}

private fun photoEditorFailureLabel(failure: PhotoEditorFailure) = when (failure) {
    PhotoEditorFailure.Unavailable -> R.string.photo_editor_unavailable; PhotoEditorFailure.InvalidSource -> R.string.photo_editor_invalid
    PhotoEditorFailure.SaveFailed -> R.string.photo_editor_save_failed; PhotoEditorFailure.SourceChanged -> R.string.photo_editor_source_changed; PhotoEditorFailure.MemoryLimit -> R.string.photo_editor_memory
}
private fun photoToolLabel(tool: PhotoEditorTool) = when (tool) { PhotoEditorTool.Crop -> R.string.photo_editor_crop; PhotoEditorTool.Draw -> R.string.photo_editor_draw; PhotoEditorTool.Erase -> R.string.photo_editor_erase }
private fun photoPresetLabel(preset: PhotoCropPreset) = when (preset) {
    PhotoCropPreset.Free -> R.string.photo_editor_free; PhotoCropPreset.Original -> R.string.photo_editor_original; PhotoCropPreset.Square -> R.string.photo_editor_square
    PhotoCropPreset.FourThree -> R.string.photo_editor_four_three; PhotoCropPreset.ThreeFour -> R.string.photo_editor_three_four; PhotoCropPreset.SixteenNine -> R.string.photo_editor_sixteen_nine; PhotoCropPreset.NineSixteen -> R.string.photo_editor_nine_sixteen
}
private fun photoColorLabel(color: PhotoPenColor) = when (color) { PhotoPenColor.Red -> R.string.photo_editor_red; PhotoPenColor.Yellow -> R.string.photo_editor_yellow; PhotoPenColor.Green -> R.string.photo_editor_green; PhotoPenColor.Blue -> R.string.photo_editor_blue; PhotoPenColor.White -> R.string.photo_editor_white }
private fun photoWidthLabel(width: PhotoPenWidth) = when (width) { PhotoPenWidth.Small -> R.string.photo_editor_small; PhotoPenWidth.Medium -> R.string.photo_editor_medium; PhotoPenWidth.Large -> R.string.photo_editor_large; PhotoPenWidth.ExtraLarge -> R.string.photo_editor_extra_large }

@Composable
private fun PhotoEditorCanvas(bitmap: Bitmap, session: PhotoEditorSession, onEvent: (PhotoEditorEvent) -> Unit, modifier: Modifier = Modifier) {
    val recipe = session.history.current
    val cropMode = session.tool == PhotoEditorTool.Crop
    val shownRecipe = if (cropMode) recipe.copy(crop = PhotoCrop()) else recipe
    val density = LocalDensity.current
    val hitSize = with(density) { 24.dp.toPx() }
    val description = stringResource(R.string.photo_editor_image)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val frame = remember(canvasSize, bitmap, shownRecipe.crop, recipe.quarterTurns) {
        val width = bitmap.width * shownRecipe.crop.width
        val height = bitmap.height * shownRecipe.crop.height
        val aspect = if (recipe.quarterTurns % 2 == 0) width / height else height / width
        val shownWidth = min(canvasSize.width.toFloat(), canvasSize.height * aspect)
        val shownHeight = shownWidth / aspect
        RectF((canvasSize.width - shownWidth) / 2, (canvasSize.height - shownHeight) / 2,
            (canvasSize.width + shownWidth) / 2, (canvasSize.height + shownHeight) / 2)
    }
    var livePoints by remember(session.id, session.tool, recipe) { mutableStateOf(emptyList<PhotoPoint>()) }
    var liveCrop by remember(session.id, recipe) { mutableStateOf<PhotoCrop?>(null) }
    val latestEvent = rememberUpdatedState(onEvent)
    val crop = PhotoEditing.displayCrop(recipe.crop, recipe.quarterTurns)
    val rotatedWidth = if (recipe.quarterTurns % 2 == 0) session.sourceWidth else session.sourceHeight
    val rotatedHeight = if (recipe.quarterTurns % 2 == 0) session.sourceHeight else session.sourceWidth
    val minX = (32f / rotatedWidth.coerceAtLeast(1)).coerceIn(0.01f, 1f)
    val minY = (32f / rotatedHeight.coerceAtLeast(1)).coerceIn(0.01f, 1f)
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(modifier.onSizeChanged { canvasSize = it }.background(background).testTag("photo.editor.canvas").semantics { contentDescription = description }
        .pointerInput(session.id, session.tool, recipe, session.editable, session.width, session.color, frame) {
            if (!session.editable || frame.width() <= 0 || frame.height() <= 0) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!frame.contains(down.position.x, down.position.y)) return@awaitEachGesture
                fun normalize(offset: Offset) = PhotoPoint((offset.x - frame.left) / frame.width(), (offset.y - frame.top) / frame.height()).clamped()
                val origin = down.position
                val start = normalize(origin)
                var end = start
                val handle = cropHandle(crop, start, hitSize / frame.width(), hitSize / frame.height())
                var cancelled = false
                var limited = false
                down.consume()
                if (!cropMode) livePoints = listOf(PhotoEditing.fromDisplay(start, recipe.crop, recipe.quarterTurns))
                try {
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || event.changes.count { it.pressed } > 1) { cancelled = true; break }
                        end = normalize(change.position)
                        if (cropMode) liveCrop = PhotoEditing.dragCrop(crop, handle, start, end, minX, minY)
                        else {
                            val point = PhotoEditing.fromDisplay(end, recipe.crop, recipe.quarterTurns)
                            if (livePoints.size < PhotoEditing.MaximumStrokePoints) {
                                if (livePoints.lastOrNull() != point) livePoints = livePoints + point
                            } else limited = true
                        }
                        change.consume()
                        pressed = change.pressed
                    }
                    if (!cancelled) {
                        if (cropMode) {
                            val traveled = Offset((end.x - start.x) * frame.width(), (end.y - start.y) * frame.height()).getDistance()
                            if (traveled >= viewConfiguration.touchSlop) liveCrop?.let { latestEvent.value(PhotoEditorEvent.Crop(PhotoEditing.sourceCrop(it, recipe.quarterTurns))) }
                        } else if (livePoints.isNotEmpty()) latestEvent.value(PhotoEditorEvent.Stroke(livePoints, limited))
                    }
                } finally { liveCrop = null; livePoints = emptyList() }
            }
        }) {
        val destination = frame
        val shownWidth = frame.width()
        val shownHeight = frame.height()
        if (shownWidth <= 0 || shownHeight <= 0) return@Canvas
        val current = if (livePoints.isEmpty()) shownRecipe else shownRecipe.copy(strokes = shownRecipe.strokes + PhotoStroke(Long.MAX_VALUE, livePoints, session.width, session.color, session.tool == PhotoEditorTool.Erase))
        drawIntoCanvas { PhotoEditorRenderer.drawPreview(it.nativeCanvas, bitmap, current, destination) }
        if (cropMode) {
            val selection = liveCrop ?: crop
            val left = destination.left + selection.left * shownWidth; val right = destination.left + selection.right * shownWidth
            val top = destination.top + selection.top * shownHeight; val bottom = destination.top + selection.bottom * shownHeight
            val scrim = Color.Black.copy(alpha = 0.55f)
            drawRect(scrim, Offset(destination.left, destination.top), Size(shownWidth, (top - destination.top).coerceAtLeast(0f)))
            drawRect(scrim, Offset(destination.left, bottom), Size(shownWidth, (destination.bottom - bottom).coerceAtLeast(0f)))
            drawRect(scrim, Offset(destination.left, top), Size((left - destination.left).coerceAtLeast(0f), bottom - top))
            drawRect(scrim, Offset(right, top), Size((destination.right - right).coerceAtLeast(0f), bottom - top))
            drawRect(Color.Black, Offset(left, top), Size(right - left, bottom - top), style = Stroke(3.dp.toPx()))
            drawRect(Color.White, Offset(left, top), Size(right - left, bottom - top), style = Stroke(1.dp.toPx()))
            for (index in 1..2) {
                val x = left + (right - left) * index / 3; val y = top + (bottom - top) * index / 3
                drawLine(Color.White.copy(alpha = 0.6f), Offset(x, top), Offset(x, bottom), 1.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.6f), Offset(left, y), Offset(right, y), 1.dp.toPx())
            }
            listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom), Offset((left + right) / 2, top), Offset((left + right) / 2, bottom), Offset(left, (top + bottom) / 2), Offset(right, (top + bottom) / 2)).forEach { position ->
                drawCircle(Color.Black, 7.dp.toPx(), position); drawCircle(Color.White, 5.dp.toPx(), position)
            }
        }
    }
}

private fun cropHandle(crop: PhotoCrop, point: PhotoPoint, xTolerance: Float, yTolerance: Float): PhotoCropHandle {
    val left = abs(point.x - crop.left) <= xTolerance; val right = abs(point.x - crop.right) <= xTolerance
    val top = abs(point.y - crop.top) <= yTolerance; val bottom = abs(point.y - crop.bottom) <= yTolerance
    return when {
        left && top -> PhotoCropHandle.TopLeft; right && top -> PhotoCropHandle.TopRight
        left && bottom -> PhotoCropHandle.BottomLeft; right && bottom -> PhotoCropHandle.BottomRight
        left && point.y in crop.top..crop.bottom -> PhotoCropHandle.Left
        right && point.y in crop.top..crop.bottom -> PhotoCropHandle.Right
        top && point.x in crop.left..crop.right -> PhotoCropHandle.Top
        bottom && point.x in crop.left..crop.right -> PhotoCropHandle.Bottom
        point.x in crop.left..crop.right && point.y in crop.top..crop.bottom -> PhotoCropHandle.Move
        else -> PhotoCropHandle.New
    }
}

@Composable
private fun PhotoEditorCoordinates(session: PhotoEditorSession, onDismiss: () -> Unit, onApply: (PhotoEditorEvent) -> Unit) {
    val cropMode = session.tool == PhotoEditorTool.Crop
    val turns = session.history.current.quarterTurns
    val crop = PhotoEditing.displayCrop(session.history.current.crop, turns)
    val minX = (32f / (if (turns % 2 == 0) session.sourceWidth else session.sourceHeight).coerceAtLeast(1)).coerceIn(0.01f, 1f)
    val minY = (32f / (if (turns % 2 == 0) session.sourceHeight else session.sourceWidth).coerceAtLeast(1)).coerceIn(0.01f, 1f)
    var x1 by rememberSaveable(session.id) { mutableFloatStateOf(if (cropMode) crop.left else 0.25f) }
    var y1 by rememberSaveable(session.id) { mutableFloatStateOf(if (cropMode) crop.top else 0.5f) }
    var x2 by rememberSaveable(session.id) { mutableFloatStateOf(if (cropMode) crop.right else 0.75f) }
    var y2 by rememberSaveable(session.id) { mutableFloatStateOf(if (cropMode) crop.bottom else 0.5f) }
    val title = stringResource(if (cropMode) R.string.photo_editor_adjust_crop else if (session.tool == PhotoEditorTool.Draw) R.string.photo_editor_draw_line else R.string.photo_editor_erase_line)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            @Composable fun coordinate(labelId: Int, value: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
                val label = stringResource(labelId)
                Text(stringResource(R.string.photo_editor_coordinate_value, label, (value * 100).roundToInt()))
                Slider(value, change, valueRange = range, modifier = Modifier.semantics { contentDescription = label }.testTag("photo.editor.coordinate.$labelId"))
            }
            coordinate(if (cropMode) R.string.photo_editor_left else R.string.photo_editor_start_x, x1, 0f..if (cropMode) (x2 - minX).coerceAtLeast(0f) else 1f) { x1 = it }
            coordinate(if (cropMode) R.string.photo_editor_top else R.string.photo_editor_start_y, y1, 0f..if (cropMode) (y2 - minY).coerceAtLeast(0f) else 1f) { y1 = it }
            coordinate(if (cropMode) R.string.photo_editor_right else R.string.photo_editor_end_x, x2, (if (cropMode) (x1 + minX).coerceAtMost(1f) else 0f)..1f) { x2 = it }
            coordinate(if (cropMode) R.string.photo_editor_bottom else R.string.photo_editor_end_y, y2, (if (cropMode) (y1 + minY).coerceAtMost(1f) else 0f)..1f) { y2 = it }
        }
    }, confirmButton = { TextButton({
        val turns = session.history.current.quarterTurns
        if (cropMode) onApply(PhotoEditorEvent.Crop(PhotoEditing.sourceCrop(PhotoCrop(x1, y1, x2, y2), turns)))
        else onApply(PhotoEditorEvent.Stroke(listOf(PhotoEditing.fromDisplay(PhotoPoint(x1, y1), session.history.current.crop, turns), PhotoEditing.fromDisplay(PhotoPoint(x2, y2), session.history.current.crop, turns))))
    }, modifier = Modifier.testTag("photo.editor.coordinates.apply")) { Text(stringResource(R.string.photo_editor_apply)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } })
}
