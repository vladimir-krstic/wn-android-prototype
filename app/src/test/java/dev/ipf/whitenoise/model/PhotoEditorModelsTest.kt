package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PhotoEditorModelsTest {
    private fun stroke(id: Long = 1, count: Int = 2) = PhotoStroke(id, List(count) { PhotoPoint(if (it % 2 == 0) 0f else 1f, 0.5f) }, PhotoPenWidth.Medium, PhotoPenColor.Red)
    @Test fun displayAndSourceCoordinatesRoundTripThroughEveryRotationAndCrop() {
        val crop = PhotoCrop(0.1f, 0.2f, 0.8f, 0.95f)
        for (turns in 0..3) for (point in listOf(PhotoPoint(.1f,.2f), PhotoPoint(.3f,.6f), PhotoPoint(.8f,.95f))) {
            val restored = PhotoEditing.fromDisplay(PhotoEditing.toDisplay(point, crop, turns), crop, turns)
            assertEquals(point.x, restored.x, .00001f); assertEquals(point.y, restored.y, .00001f)
            val roundTrip = PhotoEditing.sourceCrop(PhotoEditing.displayCrop(crop, turns), turns)
            assertEquals(crop.left, roundTrip.left, .00001f); assertEquals(crop.bottom, roundTrip.bottom, .00001f)
        }
        assertEquals(PhotoPoint(1f,0f), PhotoEditing.toDisplay(PhotoPoint(0f,0f), turns = 1))
    }
    @Test fun presetRatiosDescribeTheRotatedOutputAndStayInsideSource() {
        for (turns in 0..3) for ((w,h) in listOf(4000 to 3000, 3000 to 4000, 32000 to 400)) for (preset in PhotoCropPreset.entries) {
            val crop = PhotoEditing.preset(preset,w,h,turns,PhotoCrop(.7f,.6f,.9f,.9f))
            preset.ratio?.let { ratio ->
                val aspect = w * crop.width / (h * crop.height)
                assertEquals(ratio, if (turns % 2 == 0) aspect else 1 / aspect, .0001f)
            }
            assertTrue(crop.left >= 0 && crop.right <= 1 && crop.top >= 0 && crop.bottom <= 1)
        }
    }
    @Test fun draggingAnyHandleCannotInvertOrEscapeTheSource() {
        val crop = PhotoCrop(.2f,.3f,.7f,.8f)
        for (handle in PhotoCropHandle.entries) for (x in listOf(0f,.1f,.5f,1f)) for (y in listOf(0f,.1f,.5f,1f)) {
            val result = PhotoEditing.dragCrop(crop,handle,PhotoPoint(.3f,.4f),PhotoPoint(x,y),.04f,.03f)
            assertTrue(result.width >= .04f - .000001f && result.height >= .03f - .000001f)
            if (handle == PhotoCropHandle.Move) { assertEquals(crop.width,result.width,.000001f); assertEquals(crop.height,result.height,.000001f) }
        }
    }
    @Test fun historyUndoRedoBranchAndResetReturnToOpeningRecipe() {
        val initial = PhotoEditRecipe(crop = PhotoCrop(.2f,.1f,.8f,.9f))
        val rotated = initial.copy(quarterTurns = 1)
        val edited = PhotoEditHistory(initial).commit(rotated).add(stroke()).first
        assertEquals(rotated,edited.undo().current); assertEquals(edited.current,edited.undo().redo().current)
        val branch = edited.undo().commit(rotated.copy(quarterTurns = 2))
        assertTrue(branch.redo.isEmpty()); assertEquals(initial,branch.reset().current)
        assertEquals(branch.current,branch.reset().undo().current)
    }
    @Test fun historyIsBoundedAndNoOpDoesNotDiscardRedo() {
        var history = PhotoEditHistory()
        repeat(80) { history = history.commit(history.current.copy(quarterTurns = (it + 1) % 4)) }
        assertEquals(50,history.undo.size)
        val undone = history.undo(); assertEquals(undone,undone.commit(undone.current))
    }
    @Test fun pointsCoalesceClampAndReportPerStrokeLimit() {
        val duplicate = PhotoStroke(1,listOf(PhotoPoint(-1f,.5f),PhotoPoint(0f,.5f),PhotoPoint(.0001f,.5f),PhotoPoint(2f,.5f)),PhotoPenWidth.Small,PhotoPenColor.White)
        assertEquals(listOf(PhotoPoint(0f,.5f),PhotoPoint(1f,.5f)),PhotoEditHistory().add(duplicate).first.current.strokes.single().points)
        val (history,limit) = PhotoEditHistory().add(stroke(count = 3000))
        assertEquals(PhotoEditLimit.StrokePoints,limit); assertEquals(2048,history.current.pointCount)
    }
    @Test fun strokeAndTotalPointCapsPreserveAcceptedWork() {
        val full = PhotoEditHistory(current = PhotoEditRecipe(strokes = List(256) { stroke(it.toLong()) }))
        assertEquals(full to PhotoEditLimit.Strokes,full.add(stroke()))
        val almost = PhotoEditHistory(current = PhotoEditRecipe(strokes = List(48) { stroke(it.toLong(),2048) } + stroke(49,1600)))
        val (filled,limit) = almost.add(stroke(50,2048))
        assertEquals(100000,filled.current.pointCount); assertEquals(PhotoEditLimit.TotalPoints,limit)
        assertEquals(filled to PhotoEditLimit.TotalPoints,filled.add(stroke()))
    }
    @Test fun sourceLimitsRejectOverflowAndExtremeAspectWithoutRejectingValidEdges() {
        assertTrue(PhotoEditing.validSource(32768,1000)); assertTrue(PhotoEditing.validSource(10000,20000))
        for ((w,h) in listOf(0 to 100, -1 to 100, Int.MAX_VALUE to Int.MAX_VALUE, 32769 to 1000, 20000 to 20000, 10001 to 100)) assertFalse(PhotoEditing.validSource(w,h))
    }
    @Test fun originalQualityRemainsOriginalOnlyWithoutEdits() {
        val recipe = PhotoEditRecipe(quarterTurns = 1)
        assertEquals(PhotoQuality.Original,PhotoEditing.effectiveQuality(PhotoQuality.Original,PhotoEditRecipe()))
        assertEquals(PhotoQuality.High,PhotoEditing.effectiveQuality(PhotoQuality.Original,recipe))
        for (quality in listOf(PhotoQuality.Low,PhotoQuality.Standard,PhotoQuality.High)) assertEquals(quality,PhotoEditing.effectiveQuality(quality,recipe))
    }
}
