package dev.ipf.whitenoise.model

/** Compatibility projection for existing chat previews, search and inline-only surfaces. */
data class InlineMessageSegment(
    val text: String,
    val strong: Boolean = false,
    val emphasized: Boolean = false,
    val destination: String? = null,
    val strikethrough: Boolean = false,
    val code: Boolean = false,
)

object InlineMessageMarkup {
    fun segments(source: String): List<InlineMessageSegment> = MessageDocuments.inline(source).map {
        InlineMessageSegment(it.source.text, it.style.strong, it.style.emphasis, it.destination, it.style.strike, it.style.code)
    }
    fun plainText(source: String): String = MessageDocuments.plainText(source)
}
