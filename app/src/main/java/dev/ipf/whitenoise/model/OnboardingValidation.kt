package dev.ipf.whitenoise.model

import java.net.URI
import java.util.Locale
object LoginPrototypeData {
    val privateKey: String = "nsec1" + "q".repeat(58)
}

enum class PrivateKeyState {
    Empty,
    Invalid,
    PublicKey,
    EncryptedKey,
    Valid,
}

object PrivateKeyValidator {
    private val keyBody = Regex("^[ac-hj-np-z02-9]{58}$")
    private val encryptedBody = Regex("^[ac-hj-np-z02-9]{50,300}$")

    fun normalize(value: String): String = value.trim().let {
        if (it.startsWith("nostr:", ignoreCase = true)) it.drop(6).trim() else publicKeyFromLink(it) ?: it
    }

    fun state(value: String): PrivateKeyState {
        val normalized = normalize(value)
        return when {
            normalized.isEmpty() -> PrivateKeyState.Empty
            normalized.startsWith("nsec1") && keyBody.matches(normalized.drop(5)) -> PrivateKeyState.Valid
            normalized.startsWith("npub1") && keyBody.matches(normalized.drop(5)) -> PrivateKeyState.PublicKey
            normalized.startsWith("ncryptsec1") && encryptedBody.matches(normalized.drop("ncryptsec1".length)) -> PrivateKeyState.EncryptedKey
            else -> PrivateKeyState.Invalid
        }
    }

    fun scannedValue(value: String): String? = normalize(value).takeIf {
        state(it) !in setOf(PrivateKeyState.Empty, PrivateKeyState.Invalid)
    }

    private fun publicKeyFromLink(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val candidate = when (scheme) {
            "marmot", "whitenoise", "whitenoise-staging", "whitenoise-dev" -> when {
                uri.isOpaque -> uri.schemeSpecificPart.trim('/').removePrefix("profile/").trim('/')
                uri.host.equals("profile", ignoreCase = true) -> uri.path.orEmpty().trim('/')
                else -> uri.host
            }
            "http", "https" -> {
                if (uri.host?.lowercase(Locale.ROOT) !in setOf("whitenoise.chat", "www.whitenoise.chat", "marmot.app", "www.marmot.app")) return null
                val parts = uri.path.orEmpty().trim('/').split('/').filter(String::isNotBlank)
                if (parts.size >= 2 && parts.first().equals("profile", ignoreCase = true)) parts[1]
                else parts.singleOrNull()
            }
            else -> null
        }
        return candidate?.takeIf { it.startsWith("npub1") && keyBody.matches(it.drop(5)) }
    }
}

object VerifiedNostrAddress {
    fun normalize(value: String): String = value.trim()

    fun isValid(value: String): Boolean {
        val normalized = normalize(value)
        val parts = normalized.split('@')
        return parts.size == 2 &&
            parts[0].isNotEmpty() &&
            parts[1].contains('.') &&
            !parts[1].startsWith('.') &&
            !parts[1].endsWith('.')
    }

    fun isVerifiedDraft(
        value: String,
        matching: String,
        storedIsVerified: Boolean,
    ): Boolean = storedIsVerified && normalize(value) == normalize(matching)
}
