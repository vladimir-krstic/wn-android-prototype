package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class LocationSharingModelsTest {
    @Test fun formattingRemainsDotDecimalAndRoundTripsUnderCommaLocales() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val point = SharedLocation(-33.865143, 151.2099)
            assertEquals("Location: https://maps.google.com/maps?q=-33.865143,151.209900", point.messageText)
            assertEquals(point, LocationSharing.parse(point.messageText))
            assertEquals("0.000000", LocationSharing.coordinate(-0.0))
        } finally { Locale.setDefault(previous) }
    }
    @Test fun coordinateFieldsAcceptLocaleDecimalsAndRealZeroButRejectNonFiniteAndOutOfBounds() {
        assertEquals(SharedLocation(0.0, 0.0), LocationSharing.point("0", "0"))
        assertEquals(SharedLocation(-90.0, 180.0), LocationSharing.point("-90", "+180"))
        assertEquals(SharedLocation(12.345679, -12.5), LocationSharing.point("12,3456789", " -12,5 "))
        listOf("", " ", "NaN", "Infinity", "1e1", "--1", "91", "1,2,3", "1.2.3").forEach { assertNull(it, LocationSharing.point(it, "0")) }
        assertNull(LocationSharing.point("0", "180.00001"))
        assertNull(LocationSharing.point("-90.00001", "0"))
    }
    @Test fun legacyWireFormsAreRecognizedWithoutHidingSurroundingProse() {
        assertEquals(SharedLocation(1.2, -3.4), LocationSharing.parse("  LOCATION: HTTPS://maps.google.com/?q=1.2%2C-3.4 \n"))
        assertNotNull(LocationSharing.parse("https://maps.google.com/maps?q=0,0"))
        listOf("Meet here https://maps.google.com/maps?q=1,2", "https://maps.google.com/maps?q=1,2 please", "https://maps.google.com.evil/maps?q=1,2", "http://maps.google.com/maps?q=1,2", "https://maps.google.com/maps?q=91,2", "https://maps.google.com/maps?q=1,NaN", "https://maps.google.com/maps?q=1,2&label=place").forEach { assertNull(it, LocationSharing.parse(it)) }
    }
    @Test fun cardsRequireAnAvailableLocationOnlyBody() {
        val message = ChatMessage("point", "person", 1, "Today", 1, "Now", SharedLocation(10.0, 20.0).messageText)
        assertNotNull(LocationSharing.fromMessage(message))
        assertNull(LocationSharing.fromMessage(message.copy(deletionState = MessageDeletionState.DeletedByOther)))
        assertNull(LocationSharing.fromMessage(message.copy(expiresAtMillis = MessageForwarding.nowMillis)))
        assertNull(LocationSharing.fromMessage(message.copy(attachments = listOf(MessageAttachment("photo", MessageAttachmentKind.Photo, "Photo")))))
    }
    @Test fun openingAndCancelNeverSelectOrSendTheFallbackOrigin() {
        val opened = LocationSession(1, "p", "c")
        assertNull(opened.point)
        assertEquals(opened, opened.reduce(LocationEvent.Review))
        assertEquals(opened, opened.reduce(LocationEvent.Send))
        val closed = opened.reduce(LocationEvent.Close)
        assertEquals(LocationPhase.Closed, closed.phase)
        assertEquals(closed, closed.reduce(LocationEvent.Located(0)))
    }
    @Test fun deniedUnavailableAndServicesOffRetainManuallyEnteredCoordinates() {
        mapOf(LocationScenario.Unavailable to LocationFailure.Unavailable, LocationScenario.PermissionDenied to LocationFailure.PermissionDenied, LocationScenario.ServicesOff to LocationFailure.ServicesOff).forEach { (scenario, failure) ->
            val waiting = LocationSession(1, "p", "c", scenario = scenario, latitude = "1", longitude = "2").reduce(LocationEvent.Locate)
            val result = waiting.reduce(LocationEvent.Located(waiting.revision))
            assertEquals(failure, result.failure)
            assertEquals(SharedLocation(1.0, 2.0), result.point)
            assertEquals(LocationPhase.Review, result.reduce(LocationEvent.Review).phase)
        }
    }
    @Test fun approximateAccuracyIsRetainedUntilTheUserChangesThePoint() {
        val waiting = LocationSession(1, "p", "c", scenario = LocationScenario.Approximate).reduce(LocationEvent.Locate)
        val result = waiting.reduce(LocationEvent.Located(waiting.revision))
        assertEquals(result, result.reduce(LocationEvent.Latitude(result.latitude)))
        assertEquals(result, result.reduce(LocationEvent.Longitude(result.longitude)))
        assertEquals(1500, result.point!!.accuracyMeters)
        assertEquals(37.42, result.point!!.latitude, 0.0)
        assertNull(result.reduce(LocationEvent.Latitude("37.421")).accuracyMeters)
    }
    @Test fun currentFailureRetryRecoversAndRejectsTheEarlierRequest() {
        val waiting = LocationSession(1, "p", "c", scenario = LocationScenario.RequestFailure).reduce(LocationEvent.Locate)
        val failed = waiting.reduce(LocationEvent.Located(waiting.revision))
        assertEquals(LocationFailure.RequestFailed, failed.failure)
        val retry = failed.reduce(LocationEvent.Locate)
        assertEquals(retry, retry.reduce(LocationEvent.Located(waiting.revision)))
        assertNotNull(retry.reduce(LocationEvent.Located(retry.revision)).point)
    }
    @Test fun reviewEditAndBackPreserveInputWhileSendRequiresReview() {
        val edited = LocationSession(1, "p", "c").reduce(LocationEvent.Latitude("45,25")).reduce(LocationEvent.Longitude("19,84"))
        assertEquals(edited, edited.reduce(LocationEvent.Send))
        val reviewed = edited.reduce(LocationEvent.Review)
        val back = reviewed.reduce(LocationEvent.Back)
        assertEquals(LocationPhase.Editing, back.phase)
        assertEquals(edited.point, back.point)
        assertEquals(LocationPhase.Closed, back.reduce(LocationEvent.Back).phase)
    }
    @Test fun sendFailurePreservesPointAndRetryCompletesOnlyOnce() {
        val sending = LocationSession(1, "p", "c", scenario = LocationScenario.SendFailure, latitude = "1", longitude = "2").reduce(LocationEvent.Review).reduce(LocationEvent.Send)
        val failed = sending.reduce(LocationEvent.Sent(sending.revision))
        assertEquals(LocationFailure.SendFailed, failed.failure)
        assertEquals(sending.point, failed.point)
        val retry = failed.reduce(LocationEvent.Send)
        assertEquals(retry, retry.reduce(LocationEvent.Sent(sending.revision)))
        val complete = retry.reduce(LocationEvent.Sent(retry.revision))
        assertEquals(LocationPhase.Closed, complete.phase)
        assertEquals(complete, complete.reduce(LocationEvent.Sent(retry.revision)))
    }
}
