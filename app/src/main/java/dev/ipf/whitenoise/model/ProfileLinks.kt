package dev.ipf.whitenoise.model

import java.net.URI
import java.util.Locale

/** Public encoding/checksum handling only. This does not generate keys, sign or resolve a network identity. */
object PublicReferenceEncoding {
    private const val alphabet = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val generators = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
    private fun checksum(values: List<Int>): Int {
        var result = 1
        values.forEach { value ->
            val high = result ushr 25
            result = ((result and 0x1ffffff) shl 5) xor value
            for (i in 0..4) if (((high ushr i) and 1) != 0) result = result xor generators[i]
        }
        return result
    }
    private fun expand(prefix: String) = prefix.map { it.code ushr 5 } + listOf(0) + prefix.map { it.code and 31 }
    private fun convert(values: List<Int>, from: Int, to: Int, pad: Boolean): List<Int>? {
        var accumulator = 0; var bits = 0
        val output = mutableListOf<Int>(); val mask = (1 shl to) - 1
        for (value in values) {
            if (value < 0 || value ushr from != 0) return null
            accumulator = ((accumulator shl from) or value) and ((1 shl (from + to - 1)) - 1)
            bits += from
            while (bits >= to) { bits -= to; output += (accumulator ushr bits) and mask }
        }
        if (pad && bits > 0) output += (accumulator shl (to - bits)) and mask
        else if (!pad && (bits >= from || (accumulator shl (to - bits)) and mask != 0)) return null
        return output
    }
    fun encode(prefix: String, bytes: List<Int>): String {
        require(prefix in setOf("npub", "nprofile"))
        val data = requireNotNull(convert(bytes, 8, 5, true))
        val sum = checksum(expand(prefix) + data + List(6) { 0 }) xor 1
        return prefix + "1" + (data + (0..5).map { (sum ushr (5 * (5 - it))) and 31 }).map { alphabet[it] }.joinToString("")
    }
    fun decode(raw: String): Pair<String, List<Int>>? {
        if (raw.length !in 8..5000 || raw.any { it.code !in 33..126 } || (raw.any(Char::isLowerCase) && raw.any(Char::isUpperCase))) return null
        val value = raw.lowercase(Locale.ROOT); val separator = value.lastIndexOf('1')
        if (separator < 1 || separator + 7 > value.length) return null
        val prefix = value.take(separator)
        if (prefix !in setOf("npub", "nprofile")) return null
        val data = value.drop(separator + 1).map { alphabet.indexOf(it) }
        if (data.any { it < 0 } || checksum(expand(prefix) + data) != 1) return null
        return prefix to (convert(data.dropLast(6), 5, 8, false) ?: return null)
    }
    fun publicKey(raw: String): String? {
        val (prefix, bytes) = decode(raw) ?: return null
        if (prefix == "npub") return raw.lowercase(Locale.ROOT).takeIf { bytes.size == 32 }
        var offset = 0; var key: List<Int>? = null
        while (offset < bytes.size) {
            if (offset + 2 > bytes.size) return null
            val type = bytes[offset]; val length = bytes[offset + 1]; offset += 2
            if (offset + length > bytes.size) return null
            if (type == 0) { if (key != null || length != 32) return null; key = bytes.subList(offset, offset + length) }
            offset += length
        }
        return key?.let { encode("npub", it) }
    }
    /** Stable, public-only fixture bytes; deliberately unrelated to any secret. */
    fun fixtureKey(id: String): String {
        val seed = id.ifEmpty { "profile" }
        return encode("npub", List(32) { i -> (seed[i % seed.length].code + i * 37) and 255 })
    }
}

data class PublicProfileReference(val value: String, val fromQr: Boolean = false, val isAddress: Boolean = false) {
    val uri: String? get() = value.takeUnless { isAddress }?.let { "marmot://profile/$it" }
    val qrUri: String? get() = uri?.plus("?from=qr")
}

object ProfileLinks {
    private val schemes = setOf("marmot", "whitenoise", "whitenoise-staging", "whitenoise-dev")
    private val hosts = setOf("whitenoise.chat", "www.whitenoise.chat", "marmot.app", "www.marmot.app")
    fun parse(raw: String, recipient: Boolean = false): PublicProfileReference? {
        val input = raw.trim()
        if (input.isEmpty() || input.length > 8192 || input.any(Char::isISOControl)) return null
        var candidate = input; var fromQr = false
        if (input.startsWith("nostr:", true)) candidate = input.drop(6)
        else if (':' in input) {
            val uri = runCatching { URI(input) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            if (uri.rawUserInfo != null || uri.port != -1 || uri.rawFragment != null) return null
            fromQr = uri.rawQuery?.split('&')?.contains("from=qr") == true
            candidate = when {
                scheme in schemes && uri.isOpaque -> uri.schemeSpecificPart.trim('/').removePrefix("profile/").trim('/')
                scheme in schemes && uri.host.equals("profile", true) -> uri.path.orEmpty().trim('/')
                scheme in schemes && uri.path.isNullOrEmpty() -> uri.host ?: return null
                scheme in setOf("http", "https") && uri.host?.lowercase(Locale.ROOT) in hosts -> {
                    val segments = uri.path.orEmpty().trim('/').split('/')
                    when { segments.size == 2 && segments[0].equals("profile", true) -> segments[1]
                        segments.size == 1 -> segments[0]; else -> return null }
                }
                else -> return null
            }
        }
        PublicReferenceEncoding.publicKey(candidate)?.let { return PublicProfileReference(it, fromQr) }
        if (!recipient || candidate != input) return null
        if (Regex("[0-9a-fA-F]{64}").matches(candidate)) return PublicProfileReference(
            PublicReferenceEncoding.encode("npub", candidate.chunked(2).map { it.toInt(16) }))
        if (Regex("[^\\s@:/?#]+@[^\\s@:/?#.]+(?:\\.[^\\s@:/?#.]+)+").matches(candidate)) return PublicProfileReference(candidate, isAddress = true)
        return null
    }
    fun identifierIntent(raw: String): Boolean {
        val value = raw.trim()
        val knownWebHost = runCatching { URI(value) }.getOrNull()?.let {
            it.scheme?.lowercase(Locale.ROOT) in setOf("http", "https") && it.host?.lowercase(Locale.ROOT) in hosts
        } == true
        return knownWebHost || listOf("npub", "nprofile", "nsec", "ncryptsec", "nostr:", "marmot:", "whitenoise:", "whitenoise-dev:", "whitenoise-staging:").any { value.startsWith(it,true) } ||
            Regex("[0-9a-fA-F]{64}").matches(value) || ('@' in value && value.none(Char::isWhitespace))
    }
    fun normalizeRecipient(raw: String): String = parse(raw,recipient = true)?.value ?: PrivateKeyValidator.normalize(raw)
    fun forKey(key: String): PublicProfileReference? = PublicReferenceEncoding.publicKey(key)?.let(::PublicProfileReference)
}
