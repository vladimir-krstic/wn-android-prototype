package dev.ipf.whitenoise.model

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Coordinates are always in the EXIF-oriented original, independent of canvas size or RTL. */
data class PhotoPoint(val x: Float, val y: Float) {
    init { require(x.isFinite() && y.isFinite()) }
    fun clamped() = PhotoPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

data class PhotoCrop(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() && it in 0f..1f })
        require(right > left && bottom > top)
    }
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

enum class PhotoEditorTool { Crop, Draw, Erase }
enum class PhotoCropPreset(val ratio: Float?) { Free(null), Original(null), Square(1f), FourThree(4f / 3), ThreeFour(3f / 4), SixteenNine(16f / 9), NineSixteen(9f / 16) }
enum class PhotoPenWidth(val fraction: Float) { Small(0.004f), Medium(0.008f), Large(0.016f), ExtraLarge(0.032f) }
/** Literal annotation colors from production; these do not become app theme colors. */
enum class PhotoPenColor(val argb: Int) { Red(0xffff3b30.toInt()), Yellow(0xffffcc00.toInt()), Green(0xff34c759.toInt()), Blue(0xff007aff.toInt()), White(0xffffffff.toInt()) }
enum class PhotoEditLimit { Strokes, StrokePoints, TotalPoints }
enum class PhotoCropHandle { TopLeft, Top, TopRight, Left, Right, BottomLeft, Bottom, BottomRight, Move, New }

data class PhotoStroke(val id: Long, val points: List<PhotoPoint>, val width: PhotoPenWidth, val color: PhotoPenColor, val erase: Boolean = false) {
    init { require(points.isNotEmpty()) }
}
data class PhotoEditRecipe(val crop: PhotoCrop = PhotoCrop(), val quarterTurns: Int = 0, val strokes: List<PhotoStroke> = emptyList()) {
    init { require(quarterTurns in 0..3) }
    val isOriginal: Boolean get() = crop == PhotoCrop() && quarterTurns == 0 && strokes.isEmpty()
    val pointCount: Int get() = strokes.sumOf { it.points.size }
}

data class PhotoEditHistory(
    val initial: PhotoEditRecipe = PhotoEditRecipe(),
    val current: PhotoEditRecipe = initial,
    val undo: List<PhotoEditRecipe> = emptyList(),
    val redo: List<PhotoEditRecipe> = emptyList(),
) {
    fun commit(recipe: PhotoEditRecipe): PhotoEditHistory = if (recipe == current) this else copy(current = recipe, undo = (undo + current).takeLast(50), redo = emptyList())
    fun undo(): PhotoEditHistory = undo.lastOrNull()?.let { copy(current = it, undo = undo.dropLast(1), redo = (redo + current).takeLast(50)) } ?: this
    fun redo(): PhotoEditHistory = redo.lastOrNull()?.let { copy(current = it, undo = (undo + current).takeLast(50), redo = redo.dropLast(1)) } ?: this
    fun reset(): PhotoEditHistory = commit(initial)
    fun add(stroke: PhotoStroke): Pair<PhotoEditHistory, PhotoEditLimit?> {
        if (current.strokes.size >= PhotoEditing.MaximumStrokes) return this to PhotoEditLimit.Strokes
        val remaining = PhotoEditing.MaximumTotalPoints - current.pointCount
        if (remaining <= 0) return this to PhotoEditLimit.TotalPoints
        val points = buildList<PhotoPoint> {
            stroke.points.forEach { raw ->
                val point = raw.clamped()
                if (isEmpty() || hypot((last().x - point.x).toDouble(), (last().y - point.y).toDouble()) >= 0.0005) add(point)
            }
        }
        val limit = min(remaining, PhotoEditing.MaximumStrokePoints)
        val reached = if (points.size > limit) {
            if (remaining < PhotoEditing.MaximumStrokePoints) PhotoEditLimit.TotalPoints else PhotoEditLimit.StrokePoints
        } else null
        return commit(current.copy(strokes = current.strokes + stroke.copy(points = points.take(limit)))) to reached
    }
}

object PhotoEditing {
    const val MaximumStrokes = 256
    const val MaximumStrokePoints = 2048
    const val MaximumTotalPoints = 100_000
    const val MaximumSourceEdge = 32_768
    const val MaximumSourcePixels = 200_000_000L
    const val MaximumOutputPixels = 12_000_000L
    const val MaximumOutputEdge = 4096
    const val PreviewEdge = 1536

    fun validSource(width: Int, height: Int): Boolean = width > 0 && height > 0 && max(width, height) <= MaximumSourceEdge &&
        width.toLong() * height <= MaximumSourcePixels && max(width, height).toDouble() / min(width, height) <= 100.0

    fun preset(preset: PhotoCropPreset, width: Int, height: Int, turns: Int, current: PhotoCrop): PhotoCrop {
        if (preset == PhotoCropPreset.Free) return current
        if (preset == PhotoCropPreset.Original) return PhotoCrop()
        require(width > 0 && height > 0)
        val outputRatio = checkNotNull(preset.ratio)
        val ratio = if (turns % 2 == 0) outputRatio else 1 / outputRatio
        val sourceRatio = width.toFloat() / height
        val w = min(1f, ratio / sourceRatio)
        val h = min(1f, sourceRatio / ratio)
        val left = ((current.left + current.right - w) / 2).coerceIn(0f, 1f - w)
        val top = ((current.top + current.bottom - h) / 2).coerceIn(0f, 1f - h)
        return PhotoCrop(left, top, (left + w).coerceAtMost(1f), (top + h).coerceAtMost(1f))
    }

    fun toDisplay(point: PhotoPoint, crop: PhotoCrop = PhotoCrop(), turns: Int = 0): PhotoPoint {
        val x = (point.x - crop.left) / crop.width; val y = (point.y - crop.top) / crop.height
        return when (turns % 4) { 1 -> PhotoPoint(1 - y, x); 2 -> PhotoPoint(1 - x, 1 - y); 3 -> PhotoPoint(y, 1 - x); else -> PhotoPoint(x, y) }
    }
    fun fromDisplay(point: PhotoPoint, crop: PhotoCrop = PhotoCrop(), turns: Int = 0): PhotoPoint {
        val p = point.clamped()
        val source = when (turns % 4) { 1 -> PhotoPoint(p.y, 1 - p.x); 2 -> PhotoPoint(1 - p.x, 1 - p.y); 3 -> PhotoPoint(1 - p.y, p.x); else -> p }
        return PhotoPoint(crop.left + source.x * crop.width, crop.top + source.y * crop.height).clamped()
    }
    fun displayCrop(crop: PhotoCrop, turns: Int): PhotoCrop = bounds(listOf(toDisplay(PhotoPoint(crop.left, crop.top), turns = turns), toDisplay(PhotoPoint(crop.right, crop.bottom), turns = turns)))
    fun sourceCrop(display: PhotoCrop, turns: Int): PhotoCrop = bounds(listOf(fromDisplay(PhotoPoint(display.left, display.top), turns = turns), fromDisplay(PhotoPoint(display.right, display.bottom), turns = turns)))
    private fun bounds(points: List<PhotoPoint>) = PhotoCrop(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })

    fun dragCrop(before: PhotoCrop, handle: PhotoCropHandle, start: PhotoPoint, end: PhotoPoint, minimumX: Float, minimumY: Float): PhotoCrop {
        val dx = end.x - start.x; val dy = end.y - start.y
        val minX = minimumX.coerceIn(0.001f, 1f); val minY = minimumY.coerceIn(0.001f, 1f)
        if (handle == PhotoCropHandle.Move) {
            val x = dx.coerceIn(-before.left, 1 - before.right); val y = dy.coerceIn(-before.top, 1 - before.bottom)
            return PhotoCrop((before.left + x).coerceIn(0f, 1f), (before.top + y).coerceIn(0f, 1f), (before.right + x).coerceIn(0f, 1f), (before.bottom + y).coerceIn(0f, 1f))
        }
        if (handle == PhotoCropHandle.New) {
            val a = start.clamped(); val b = end.clamped()
            val w = max(minX, abs(a.x - b.x)); val h = max(minY, abs(a.y - b.y))
            val l = min(a.x, b.x).coerceIn(0f, 1 - w); val t = min(a.y, b.y).coerceIn(0f, 1 - h)
            return PhotoCrop(l, t, (l + w).coerceAtMost(1f), (t + h).coerceAtMost(1f))
        }
        val left = handle in setOf(PhotoCropHandle.TopLeft, PhotoCropHandle.Left, PhotoCropHandle.BottomLeft)
        val right = handle in setOf(PhotoCropHandle.TopRight, PhotoCropHandle.Right, PhotoCropHandle.BottomRight)
        val top = handle in setOf(PhotoCropHandle.TopLeft, PhotoCropHandle.Top, PhotoCropHandle.TopRight)
        val bottom = handle in setOf(PhotoCropHandle.BottomLeft, PhotoCropHandle.Bottom, PhotoCropHandle.BottomRight)
        return PhotoCrop(
            if (left) (before.left + dx).coerceIn(0f, (before.right - minX).coerceAtLeast(0f)) else before.left,
            if (top) (before.top + dy).coerceIn(0f, (before.bottom - minY).coerceAtLeast(0f)) else before.top,
            if (right) (before.right + dx).coerceIn((before.left + minX).coerceAtMost(1f), 1f) else before.right,
            if (bottom) (before.bottom + dy).coerceIn((before.top + minY).coerceAtMost(1f), 1f) else before.bottom,
        )
    }

    fun effectiveQuality(requested: PhotoQuality, recipe: PhotoEditRecipe): PhotoQuality = when {
        recipe.isOriginal -> requested
        requested == PhotoQuality.Original -> PhotoQuality.High
        else -> requested
    }
}

enum class PhotoEditorPhase { Loading, Ready, Saving, Failed }
enum class PhotoEditorFailure { Unavailable, InvalidSource, SaveFailed, SourceChanged, MemoryLimit }
enum class PhotoEditorScenario(val developerLabel: String) { Success("Success"), LoadFailure("Source unavailable"), SaveFailure("Save failure") }
data class PhotoEditorSession(
    val id: Long,
    val profileId: String,
    val chatId: String,
    val attachmentId: String,
    val imageIndex: Int,
    val expectedAttachment: MessageAttachment,
    val history: PhotoEditHistory,
    val requestedQuality: PhotoQuality,
    val initialQuality: PhotoQuality = requestedQuality,
    val phase: PhotoEditorPhase = PhotoEditorPhase.Loading,
    val revision: Long = 0,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val tool: PhotoEditorTool = PhotoEditorTool.Crop,
    val preset: PhotoCropPreset = PhotoCropPreset.Free,
    val color: PhotoPenColor = PhotoPenColor.Red,
    val width: PhotoPenWidth = PhotoPenWidth.Medium,
    val nextStrokeId: Long = 1,
    val limit: PhotoEditLimit? = null,
    val failure: PhotoEditorFailure? = null,
    val scenario: PhotoEditorScenario = PhotoEditorScenario.Success,
) {
    val dirty: Boolean get() = history.current != history.initial || requestedQuality != initialQuality
    val quality: PhotoQuality get() = PhotoEditing.effectiveQuality(requestedQuality, history.current)
    val editable: Boolean get() = phase == PhotoEditorPhase.Ready || (phase == PhotoEditorPhase.Failed && sourceWidth > 0 && failure in setOf(PhotoEditorFailure.SaveFailed, PhotoEditorFailure.MemoryLimit))
    val source: ProfileAvatar? get() = expectedAttachment.sourceImages.ifEmpty { expectedAttachment.images }.getOrNull(imageIndex)
}

sealed interface PhotoEditorEvent {
    data class Loaded(val revision: Long, val width: Int, val height: Int, val failure: PhotoEditorFailure? = null) : PhotoEditorEvent
    data class SelectTool(val tool: PhotoEditorTool) : PhotoEditorEvent
    data class SelectPreset(val preset: PhotoCropPreset) : PhotoEditorEvent
    data class SelectColor(val color: PhotoPenColor) : PhotoEditorEvent
    data class SelectWidth(val width: PhotoPenWidth) : PhotoEditorEvent
    data class SelectQuality(val quality: PhotoQuality) : PhotoEditorEvent
    data class Crop(val crop: PhotoCrop) : PhotoEditorEvent
    data class Stroke(val points: List<PhotoPoint>, val limited: Boolean = false) : PhotoEditorEvent
    data object Rotate : PhotoEditorEvent
    data object Undo : PhotoEditorEvent
    data object Redo : PhotoEditorEvent
    data object Reset : PhotoEditorEvent
    data object Save : PhotoEditorEvent
    data class Saved(val revision: Long, val attachment: MessageAttachment?, val failure: PhotoEditorFailure? = null) : PhotoEditorEvent
    data object Retry : PhotoEditorEvent
    data object Close : PhotoEditorEvent
}
