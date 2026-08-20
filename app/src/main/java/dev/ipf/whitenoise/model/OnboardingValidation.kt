package dev.ipf.whitenoise.model

object LoginPrototypeData {
    val privateKey: String = "nsec1" + "q".repeat(58)
}

enum class PrivateKeyState {
    Empty,
    Invalid,
    Valid,
}

object PrivateKeyValidator {
    fun normalize(value: String): String = value.trim()

    fun state(value: String): PrivateKeyState {
        val normalized = normalize(value)
        return when {
            normalized.isEmpty() -> PrivateKeyState.Empty
            normalized.startsWith("nsec") -> PrivateKeyState.Valid
            else -> PrivateKeyState.Invalid
        }
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
