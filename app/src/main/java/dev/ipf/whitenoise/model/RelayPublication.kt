package dev.ipf.whitenoise.model

enum class PublishedRelayList { Posting, Inbox }
enum class RelayProjectionPhase { Published, Missing, Unavailable }
enum class RelayPublicationScenario(val developerLabel: String) {
    Current("Current accepted status"),
    Published("All lists published"),
    MissingPosting("Where I post is missing"),
    MissingInbox("Where I receive is missing"),
    MissingBoth("Both lists are missing"),
    Unavailable("List status unavailable"),
    Failure("Refresh or publication fails"),
}
enum class RelayPublicationOperation { Refresh, PublishMissing }
enum class RelayPublicationWorkPhase { Running, Complete, Failed, Unavailable }

data class RelayListSignature(
    val posting: Set<String>,
    val inbox: Set<String>,
) {
    companion object {
        fun capture(relays: List<ProfileRelay>) = RelayListSignature(
            posting = relays.filter { !it.isReadOnly && RelayRole.Profile in it.roles }.mapTo(sortedSetOf()) { it.url },
            inbox = relays.filter { !it.isReadOnly && RelayRole.Inbox in it.roles }.mapTo(sortedSetOf()) { it.url },
        )
    }
    fun changedKinds(after: RelayListSignature): Set<PublishedRelayList> = buildSet {
        if (posting != after.posting) add(PublishedRelayList.Posting)
        if (inbox != after.inbox) add(PublishedRelayList.Inbox)
    }
}

data class RelayPublicationProjection(
    val phase: RelayProjectionPhase = RelayProjectionPhase.Published,
    val missing: Set<PublishedRelayList> = emptySet(),
    val revision: Long = 0,
) {
    init {
        require((phase == RelayProjectionPhase.Missing) == missing.isNotEmpty())
    }
    fun status(kind: PublishedRelayList): RelayProjectionPhase = when {
        phase == RelayProjectionPhase.Unavailable -> RelayProjectionPhase.Unavailable
        kind in missing -> RelayProjectionPhase.Missing
        else -> RelayProjectionPhase.Published
    }
    fun changed(kinds: Set<PublishedRelayList>) = if (kinds.isEmpty()) this else copy(
        phase = RelayProjectionPhase.Missing,
        missing = missing + kinds,
        revision = revision + 1,
    )
}

data class RelayPublicationWork(
    val id: Long,
    val profileId: String,
    val surface: String,
    val operation: RelayPublicationOperation,
    val scenario: RelayPublicationScenario,
    val phase: RelayPublicationWorkPhase,
    val projectionRevision: Long,
    val signature: RelayListSignature,
)
