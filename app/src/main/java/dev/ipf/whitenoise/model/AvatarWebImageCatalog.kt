package dev.ipf.whitenoise.model

import java.net.URI

data class AvatarWebImageChoice(
    val id: String,
    val asset: AvatarAsset,
    val accessibilityLabel: String,
)

object AvatarWebImageCatalog {
    val choices = listOf(
        AvatarWebImageChoice("badger", AvatarAsset.Badger, "Badger"),
        AvatarWebImageChoice("3TLl_97HNJo", AvatarAsset.WebAionyHaust, "Portrait, blue lighting"),
        AvatarWebImageChoice("open-circuit", AvatarAsset.OpenCircuit, "Open circuit"),
        AvatarWebImageChoice("fox", AvatarAsset.Fox, "Fox"),
        AvatarWebImageChoice("rDEOVtE7vOs", AvatarAsset.WebChristopherCampbell, "Portrait, red hair by a lake"),
        AvatarWebImageChoice("cipher-wheel", AvatarAsset.CipherWheel, "Cipher wheel"),
        AvatarWebImageChoice("marmot", AvatarAsset.Marmot, "Marmot"),
        AvatarWebImageChoice("d1UPkiFd04A", AvatarAsset.WebIanDooley, "Portrait, black hat"),
        AvatarWebImageChoice("pebble", AvatarAsset.Pebble, "Pebbles"),
        AvatarWebImageChoice("ostrich", AvatarAsset.Ostrich, "Ostrich"),
        AvatarWebImageChoice("c_GmwfHBDzk", AvatarAsset.WebSergioDePaula, "Black-and-white portrait, striped shirt"),
        AvatarWebImageChoice("open-quill", AvatarAsset.OpenQuill, "Open quill"),
        AvatarWebImageChoice("sloth", AvatarAsset.Sloth, "Sloth"),
        AvatarWebImageChoice("sibVwORYqs0", AvatarAsset.WebAyoOgunseinde, "Portrait, colorful mural"),
        AvatarWebImageChoice("free-signal", AvatarAsset.FreeSignal, "Free signal"),
        AvatarWebImageChoice("garden-club", AvatarAsset.GardenClub, "Garden club"),
        AvatarWebImageChoice("j3lf-Jn6deo", AvatarAsset.WebVinceFleming, "Portrait, patterned shirt"),
        AvatarWebImageChoice("liberty-relay", AvatarAsset.LibertyRelay, "Liberty relay"),
        AvatarWebImageChoice("public-voice", AvatarAsset.PublicVoice, "Public voice"),
        AvatarWebImageChoice("5aGUyCW_PJw", AvatarAsset.WebPhilipMartin, "Portrait, red beanie on a rooftop"),
        AvatarWebImageChoice("marmota", AvatarAsset.Marmota, "Marmot portrait"),
    )

    fun choice(id: String): AvatarWebImageChoice? = choices.firstOrNull { it.id == id }

    fun displayUrl(choice: AvatarWebImageChoice): String =
        "https://example.com/images/${choice.id}.jpg"

    fun choiceMatchingUrl(input: String): AvatarWebImageChoice? {
        val normalized = input.trim()
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if ((scheme != "http" && scheme != "https") || uri.host.isNullOrBlank()) return null

        val pathId = uri.path.substringAfterLast('/').substringBeforeLast('.')
        choice(pathId)?.let { return it }
        choices.firstOrNull { normalized.contains(it.id, ignoreCase = true) }?.let { return it }
        return choices[deterministicOffset(normalized)]
    }

    fun results(query: String): List<AvatarWebImageChoice> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return choices
        val offset = deterministicOffset(normalized)
        return choices.drop(offset) + choices.take(offset)
    }

    private fun deterministicOffset(value: String): Int {
        var offset = 0
        var index = 0
        while (index < value.length) {
            val codePoint = Character.codePointAt(value, index)
            offset = (offset + codePoint) % choices.size
            index += Character.charCount(codePoint)
        }
        return offset
    }
}
