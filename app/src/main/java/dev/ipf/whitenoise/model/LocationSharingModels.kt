package dev.ipf.whitenoise.model

import java.util.Locale

/** A fixed point, never a tracking session or an inferred device position. */
data class SharedLocation(val latitude: Double, val longitude: Double, val accuracyMeters: Int? = null) {
    val valid: Boolean get() = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
    val coordinates: String get() = "${LocationSharing.coordinate(latitude)}, ${LocationSharing.coordinate(longitude)}"
    val mapsLink: String get() = "https://maps.google.com/maps?q=${LocationSharing.coordinate(latitude)},${LocationSharing.coordinate(longitude)}"
    val geoUri: String get() = "geo:${LocationSharing.coordinate(latitude)},${LocationSharing.coordinate(longitude)}?q=${LocationSharing.coordinate(latitude)},${LocationSharing.coordinate(longitude)}"
    val messageText: String get() = "Location: $mapsLink"
}

object LocationSharing {
    private val numeric = Regex("[+-]?(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)")
    private val locationBody = Regex("""\s*(?:Location:\s*)?https://maps\.google\.com/(?:maps)?\?q=(-?\d+(?:\.\d+)?)(?:,|%2C)(-?\d+(?:\.\d+)?)\s*""", RegexOption.IGNORE_CASE)
    fun coordinate(value: Double): String = String.format(Locale.US, "%.6f", if (kotlin.math.abs(value) < .0000005) 0.0 else value)
    fun number(text: String, latitude: Boolean): Double? {
        val value = text.trim().takeIf { it.length <= 32 && numeric.matches(it) }?.replace(',', '.')?.toDoubleOrNull() ?: return null
        return value.takeIf { it.isFinite() && it in if (latitude) -90.0..90.0 else -180.0..180.0 }
    }
    fun point(latitude: String, longitude: String): SharedLocation? {
        val lat = number(latitude, true) ?: return null
        val lon = number(longitude, false) ?: return null
        return SharedLocation(coordinate(lat).toDouble(), coordinate(lon).toDouble())
    }
    fun parse(text: String): SharedLocation? {
        val match = locationBody.matchEntire(text) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        return SharedLocation(lat, lon).takeIf { it.valid }
    }
    fun fromMessage(message: ChatMessage): SharedLocation? = if (message.isDeleted ||
        message.expiresAtMillis?.let { it <= MessageForwarding.nowMillis } == true || message.attachments.isNotEmpty()) null else parse(message.text)
}

enum class LocationScenario(val developerLabel: String) {
    Unavailable("Current location unavailable"),
    PermissionDenied("Location access denied"),
    ServicesOff("Location services off"),
    Approximate("Approximate current location"),
    Precise("Precise current location"),
    RequestFailure("Current location failure, then retry"),
    SendFailure("Location send failure, then retry"),
}
enum class LocationPhase { Editing, Locating, Review, Sending, Closed }
enum class LocationFailure { Unavailable, PermissionDenied, ServicesOff, RequestFailed, SendFailed, SourceChanged }
sealed interface LocationEvent {
    data class Latitude(val value: String) : LocationEvent
    data class Longitude(val value: String) : LocationEvent
    data object Locate : LocationEvent
    data class Located(val revision: Long) : LocationEvent
    data object Review : LocationEvent
    data object Edit : LocationEvent
    data object Send : LocationEvent
    data class Sent(val revision: Long) : LocationEvent
    data object Back : LocationEvent
    data object Close : LocationEvent
}
data class LocationSession(
    val id: Long,
    val profileId: String,
    val chatId: String,
    val expectedReply: ChatMessage? = null,
    val scenario: LocationScenario = LocationScenario.Unavailable,
    val latitude: String = "",
    val longitude: String = "",
    val accuracyMeters: Int? = null,
    val phase: LocationPhase = LocationPhase.Editing,
    val revision: Long = 0,
    val failure: LocationFailure? = null,
    val sendFailurePending: Boolean = scenario == LocationScenario.SendFailure,
) {
    val point: SharedLocation? get() = LocationSharing.point(latitude, longitude)?.copy(accuracyMeters = accuracyMeters)
    fun reduce(event: LocationEvent): LocationSession {
        if (phase == LocationPhase.Closed) return this
        val next = when (event) {
            is LocationEvent.Latitude -> if (phase == LocationPhase.Editing && event.value.take(32) != latitude) copy(latitude = event.value.take(32), accuracyMeters = null, failure = null) else this
            is LocationEvent.Longitude -> if (phase == LocationPhase.Editing && event.value.take(32) != longitude) copy(longitude = event.value.take(32), accuracyMeters = null, failure = null) else this
            LocationEvent.Locate -> if (phase == LocationPhase.Editing) copy(phase = LocationPhase.Locating, failure = null) else this
            is LocationEvent.Located -> if (phase != LocationPhase.Locating || revision != event.revision) this else {
                val reason = when (scenario) {
                    LocationScenario.Unavailable -> LocationFailure.Unavailable
                    LocationScenario.PermissionDenied -> LocationFailure.PermissionDenied
                    LocationScenario.ServicesOff -> LocationFailure.ServicesOff
                    LocationScenario.RequestFailure -> LocationFailure.RequestFailed
                    else -> null
                }
                if (reason != null) copy(phase = LocationPhase.Editing, failure = reason,
                    scenario = if (scenario == LocationScenario.RequestFailure) LocationScenario.Precise else scenario)
                else {
                    // Authored public-coordinate examples are reachable only through Developer Tools.
                    val point = if (scenario == LocationScenario.Approximate) SharedLocation(37.42, -122.08, 1500) else SharedLocation(37.421999, -122.084057, 10)
                    copy(phase = LocationPhase.Editing, latitude = LocationSharing.coordinate(point.latitude), longitude = LocationSharing.coordinate(point.longitude), accuracyMeters = point.accuracyMeters)
                }
            }
            LocationEvent.Review -> if (phase == LocationPhase.Editing && point != null) copy(phase = LocationPhase.Review, failure = null) else this
            LocationEvent.Edit -> if (phase == LocationPhase.Review) copy(phase = LocationPhase.Editing, failure = null) else this
            LocationEvent.Send -> if (phase == LocationPhase.Review && point != null && failure != LocationFailure.SourceChanged) copy(phase = LocationPhase.Sending, failure = null) else this
            is LocationEvent.Sent -> if (phase != LocationPhase.Sending || revision != event.revision) this else if (sendFailurePending)
                copy(phase = LocationPhase.Review, failure = LocationFailure.SendFailed, sendFailurePending = false) else copy(phase = LocationPhase.Closed)
            LocationEvent.Back -> if (phase == LocationPhase.Review) copy(phase = LocationPhase.Editing, failure = null) else copy(phase = LocationPhase.Closed)
            LocationEvent.Close -> copy(phase = LocationPhase.Closed)
        }
        return if (next == this) this else next.copy(revision = revision + 1)
    }
}
