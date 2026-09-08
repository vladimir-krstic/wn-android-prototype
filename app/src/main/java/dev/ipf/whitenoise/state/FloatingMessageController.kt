package dev.ipf.whitenoise.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ipf.whitenoise.model.*

class FloatingMessageController(
    private val profiles: () -> List<Profile>,
    private val activeId: () -> String?,
    private val signedIn: () -> Set<String>,
    private val now: () -> Long,
) {
    private var stacks by mutableStateOf<Map<String, FloatingMessageStack>>(emptyMap())
    private fun resolve(key: FloatingMessageKey) = FloatingMessages.resolve(key, profiles(), signedIn(), now())
    val entries: List<FloatingMessageEntry> get() = stacks[activeId()]?.keys.orEmpty().mapNotNull(::resolve)
    val selected: FloatingMessageKey? get() = stacks[activeId()]?.selected?.takeIf { key -> entries.any { it.key == key } } ?: entries.firstOrNull()?.key

    fun keep(profileId: String, chatId: String, messageId: String): Boolean {
        if (profileId != activeId()) return false
        val key = FloatingMessageKey(profileId, chatId, messageId)
        if (resolve(key) == null) return false
        stacks = stacks + (profileId to (stacks[profileId] ?: FloatingMessageStack()).add(key))
        return true
    }
    fun select(key: FloatingMessageKey) {
        if (key.profileId != activeId()) return
        stacks[key.profileId]?.let { stacks = stacks + (key.profileId to it.select(key)) }
    }
    fun remove(key: FloatingMessageKey) {
        if (key.profileId != activeId()) return
        stacks[key.profileId]?.let { stacks = stacks + (key.profileId to it.remove(key)) }
    }
    fun clear() { activeId()?.let { stacks = stacks - it } }
    fun reconcile() {
        stacks = stacks.filterKeys { it in signedIn() }.mapValues { (_, stack) ->
            stack.retain(stack.keys.filter { resolve(it) != null }.toSet())
        }.filterValues { it.keys.isNotEmpty() }
    }
}
