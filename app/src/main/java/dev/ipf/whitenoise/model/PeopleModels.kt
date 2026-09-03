package dev.ipf.whitenoise.model

import java.text.Normalizer
import java.util.Locale

object PrivateContactDetails {
    fun nickname(value: String): String {
        val clean = Normalizer.normalize(value, Normalizer.Form.NFC)
            .filterNot { Character.getType(it) == Character.FORMAT.toInt() || (it.isISOControl() && !it.isWhitespace()) }
            .replace(Regex("\\s+"), " ").trim()
        return clean.take(80).let { if (it.lastOrNull()?.isHighSurrogate() == true) it.dropLast(1) else it }
    }
    fun notes(value: String): String = value.filterNot {
        Character.getType(it) == Character.FORMAT.toInt() || (it.isISOControl() && it != '\n' && it != '\t')
    }.trim()
}

fun Person.matchesPeopleQuery(query: String): Boolean {
    fun String.normalized() = Normalizer.normalize(trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    val needle = PrivateKeyValidator.normalize(query).normalized()
    return listOf(name, displayName, publicKey, nostrAddress).any { needle in it.normalized() }
}

enum class PeopleSearchScenario(val developerLabel: String) { Success("Complete results"), Partial("Partial results"), Unavailable("Search unavailable") }
enum class PeopleSearchStatus { Ready, InvalidIdentifier, AddressNotFound, NoResults, NoProfile, Partial, Unavailable }
enum class PersonSource { Chats, Following, Local, Network }
data class PeopleResult(val person: Person, val source: PersonSource)
data class PeopleSearchResult(val people: List<PeopleResult>, val status: PeopleSearchStatus)

object PeopleDiscovery {
    val directory = listOf(
        Person("river-song", "River Song", about = "Open conversations and quiet places.", nostrAddress = "river@whitenoise.example", isFollowing = false, lightningAddress = "river@payments.example", banner = ProfileAvatar.Asset(AvatarAsset.GardenClub)),
        Person("sam-green", "Sam Green", about = "Building things together.", nostrAddress = "sam@whitenoise.example", isFollowing = false),
    )
    fun local(profile: Profile, query: String): List<PeopleResult> = profile.people
        .filter { it.id != profile.id && it.id != "white-noise-support" && it.matchesPeopleQuery(query) }
        .sortedBy(Person::displayName).map { person ->
            val inChat = profile.chats.any { (it.kind as? ChatKind.Direct)?.personId == person.id || it.members.any { member -> member.personId == person.id } }
            PeopleResult(person, if (inChat) PersonSource.Chats else if (person.isFollowing) PersonSource.Following else PersonSource.Local)
        }
    fun resolve(profile: Profile, query: String, scenario: PeopleSearchScenario = PeopleSearchScenario.Success): PeopleSearchResult {
        val key = PrivateKeyValidator.normalize(query)
        val local = local(profile, query)
        if (key.isBlank()) return PeopleSearchResult(local, PeopleSearchStatus.Ready)
        val isKey = key.startsWith("npub", true)
        val isAddress = '@' in key
        val invalid = if (isAddress) !Regex("^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$").matches(key)
            else (isKey && PrivateKeyValidator.state(key) != PrivateKeyState.PublicKey) || (key.startsWith("nsec", true) || key.startsWith("ncryptsec", true)) || "://" in key
        if (invalid) return PeopleSearchResult(emptyList(), PeopleSearchStatus.InvalidIdentifier)
        if (scenario == PeopleSearchScenario.Unavailable) return PeopleSearchResult(local, PeopleSearchStatus.Unavailable)
        val remote = directory.filter { person ->
            person.id != profile.id && profile.people.none { it.publicKey == person.publicKey } &&
                if (isAddress) person.nostrAddress.equals(key, true) else person.matchesPeopleQuery(key)
        }.map { PeopleResult(it, PersonSource.Network) }
        val results = (local + if (scenario == PeopleSearchScenario.Partial) remote.take(1) else remote).distinctBy { it.person.publicKey }
        if (scenario == PeopleSearchScenario.Partial) return PeopleSearchResult(results, PeopleSearchStatus.Partial)
        if (results.isNotEmpty()) return PeopleSearchResult(results, PeopleSearchStatus.Ready)
        if (isKey) return PeopleSearchResult(listOf(PeopleResult(Person("resolved-$key", key.take(12) + "…" + key.takeLast(4), publicKey = key, nostrAddress = "", isFollowing = false), PersonSource.Network)), PeopleSearchStatus.NoProfile)
        return PeopleSearchResult(emptyList(), if (isAddress) PeopleSearchStatus.AddressNotFound else PeopleSearchStatus.NoResults)
    }
}

enum class GroupContactAction { Invite, Promote }
enum class GroupContactScenario(val developerLabel: String) { Success("Complete roster and actions"), PartialRoster("Partial roster"), UnavailableRoster("Roster unavailable"), PartialApply("Some group actions fail") }
data class GroupContactResult(val completed: List<String>, val failed: List<String>)
object GroupContactPolicy {
    fun eligible(profile: Profile, personId: String, action: GroupContactAction): List<Chat> = profile.chats.filter { chat ->
        chat.isGroup && chat.membership == ChatMembership.Active && personId != profile.id &&
            profile.people.any { it.id == personId } &&
            chat.members.any { it.personId == profile.id && it.role == GroupRole.Admin } &&
            when (action) {
                GroupContactAction.Invite -> chat.members.none { it.personId == personId }
                GroupContactAction.Promote -> chat.members.any { it.personId == personId && it.role == GroupRole.Member }
            }
    }
    fun unresolved(profile: Profile, scenario: GroupContactScenario): Set<String> {
        val groups = profile.chats.filter { it.isGroup && it.membership == ChatMembership.Active }
        return when (scenario) {
            GroupContactScenario.UnavailableRoster -> groups.map(Chat::id).toSet()
            GroupContactScenario.PartialRoster -> groups.lastOrNull()?.let { setOf(it.id) }.orEmpty()
            else -> emptySet()
        }
    }
}

data class CreatedChatOpen(val id: Long, val profileId: String, val origin: String, val chatId: String)
