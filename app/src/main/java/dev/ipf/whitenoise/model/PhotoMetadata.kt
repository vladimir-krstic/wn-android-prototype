package dev.ipf.whitenoise.model

import java.io.ByteArrayOutputStream

/** Allowlist container reconstruction. Unknown containers never pass through unchanged. */
object PhotoMetadata {
    fun strippedOriginal(bytes: ByteArray): ByteArray? = runCatching {
        when {
            bytes.size >= 4 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() -> jpeg(bytes)
            bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)) -> png(bytes)
            else -> null
        }
    }.getOrNull()

    fun strippedGif(bytes: ByteArray): ByteArray? = runCatching {
        if (bytes.size < 14 || bytes.copyOfRange(0, 6).toString(Charsets.US_ASCII) !in setOf("GIF87a", "GIF89a")) return null
        val out = ByteArrayOutputStream()
        var position = 13
        val globalFlags = bytes[10].toInt() and 255
        if (globalFlags and 128 != 0) position += 3 * (1 shl ((globalFlags and 7) + 1))
        if (position >= bytes.size) return null
        out.write(bytes, 0, position)
        var hasImage = false
        fun skipBlocks(): Boolean {
            while (position < bytes.size) {
                val length = bytes[position++].toInt() and 255
                if (length == 0) return true
                if (length > bytes.size - position) return false
                position += length
            }
            return false
        }
        while (position < bytes.size) {
            val start = position
            when (bytes[position++].toInt() and 255) {
                0x3b -> { out.write(0x3b); return out.toByteArray().takeIf { hasImage && position == bytes.size } }
                0x2c -> {
                    if (position + 9 >= bytes.size) return null
                    val flags = bytes[position + 8].toInt() and 255
                    position += 9
                    if (flags and 128 != 0) position += 3 * (1 shl ((flags and 7) + 1))
                    if (position >= bytes.size) return null
                    position++ // LZW minimum code size.
                    if (!skipBlocks()) return null
                    out.write(bytes, start, position - start); hasImage = true
                }
                0x21 -> {
                    if (position >= bytes.size) return null
                    val label = bytes[position++].toInt() and 255
                    val loop = label == 0xff && position + 12 < bytes.size && bytes[position].toInt() == 11 &&
                        bytes.copyOfRange(position + 1, position + 12).toString(Charsets.US_ASCII) == "NETSCAPE2.0"
                    if (!skipBlocks()) return null
                    if (label == 0xf9 || loop) out.write(bytes, start, position - start)
                }
                else -> return null
            }
        }
        null
    }.getOrNull()

    private fun jpeg(bytes: ByteArray): ByteArray? {
        val out = ByteArrayOutputStream()
        out.write(bytes, 0, 2)
        var position = 2
        var hasFrame = false
        while (position + 4 <= bytes.size) {
            if (bytes[position].toInt() and 255 != 255) return null
            val marker = bytes[position + 1].toInt() and 255
            // Adobe APP14 can change color interpretation: re-encode instead of stripping it blindly.
            if (marker == 0xee) return null
            if (marker == 0xda) {
                if (!hasFrame || bytes.takeLast(2) != listOf(0xff.toByte(), 0xd9.toByte())) return null
                // Entropy payload may contain another scan. Reject trailing APP/COM segments,
                // including between progressive scans, instead of leaking embedded metadata.
                var cursor = position + 2
                while (cursor + 1 < bytes.size) {
                    if (bytes[cursor].toInt() and 255 == 255) {
                        val code = bytes[cursor + 1].toInt() and 255
                        if (code in 0xe0..0xef || code == 0xfe) return null
                    }
                    cursor++
                }
                out.write(bytes, position, bytes.size - position)
                return out.toByteArray()
            }
            val length = ((bytes[position + 2].toInt() and 255) shl 8) + (bytes[position + 3].toInt() and 255)
            if (length < 2 || length > bytes.size - position - 2) return null
            if (marker in setOf(0xc0, 0xc1, 0xc2)) hasFrame = true
            // Keep structural markers only; APP0 thumbnails and COM may identify a source too.
            if (marker !in 0xe0..0xef && marker != 0xfe) out.write(bytes, position, length + 2)
            position += length + 2
        }
        return null
    }

    private fun png(bytes: ByteArray): ByteArray? {
        val out = ByteArrayOutputStream()
        out.write(bytes, 0, 8)
        var position = 8
        var header = false
        var pixels = false
        val allowed = setOf("IHDR", "PLTE", "IDAT", "IEND", "tRNS", "acTL", "fcTL", "fdAT", "sRGB", "gAMA", "cHRM")
        while (position + 12 <= bytes.size) {
            var length = 0L
            repeat(4) { length = (length shl 8) or (bytes[position + it].toLong() and 255) }
            if (length > bytes.size - position - 12) return null
            val size = length.toInt()
            val type = bytes.copyOfRange(position + 4, position + 8).toString(Charsets.US_ASCII)
            if (!header && type != "IHDR") return null
            if (type == "IHDR") { if (header || size != 13) return null; header = true }
            if (type == "IDAT") pixels = true
            // Unknown critical chunks are not safe to discard.
            if (type !in allowed && type.first().isUpperCase()) return null
            if (type in allowed) out.write(bytes, position, size + 12)
            position += size + 12
            if (type == "IEND") return out.toByteArray().takeIf { pixels && size == 0 && position == bytes.size }
        }
        return null
    }
}
