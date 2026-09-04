package dev.ipf.whitenoise.model

/** Every displayed UTF-16 unit points into the unchanged authored message. */
data class SourceText(val text: String, val offsets: List<Int>, val ends: List<Int> = offsets.map { it + 1 }) {
    init { require(text.length == offsets.size && ends.size == offsets.size) }
    fun slice(start: Int, end: Int = text.length) = SourceText(text.substring(start, end), offsets.subList(start, end), ends.subList(start, end))
    operator fun plus(other: SourceText) = SourceText(text + other.text, offsets + other.offsets, ends + other.ends)
    companion object { fun from(text: String, offset: Int = 0) = SourceText(text, text.indices.map { offset + it }) }
}
data class DocumentStyle(val strong: Boolean = false, val emphasis: Boolean = false, val strike: Boolean = false, val code: Boolean = false)
data class DocumentRun(val source: SourceText, val style: DocumentStyle = DocumentStyle(), val destination: String? = null, val namedLink: Boolean = false)
enum class DocumentAlignment { Start, Center, End }
sealed interface DocumentBlock {
    data class Paragraph(val runs: List<DocumentRun>) : DocumentBlock
    data class Heading(val level: Int, val runs: List<DocumentRun>) : DocumentBlock
    data class Code(val source: SourceText, val language: String = "") : DocumentBlock
    data class Quote(val blocks: List<DocumentBlock>) : DocumentBlock
    data class ListBlock(val items: List<DocumentListItem>, val ordered: Boolean) : DocumentBlock
    data class Table(val header: List<List<DocumentRun>>, val rows: List<List<List<DocumentRun>>>, val alignments: List<DocumentAlignment>) : DocumentBlock
    data class Details(val summary: List<DocumentRun>, val blocks: List<DocumentBlock>, val initiallyOpen: Boolean) : DocumentBlock
    data class Blank(val count: Int) : DocumentBlock
    data object Divider : DocumentBlock
}
data class DocumentListItem(val number: Int?, val checked: Boolean?, val blocks: List<DocumentBlock>)
data class MessageDocument(val source: String, val blocks: List<DocumentBlock>)
data class MessagePassage(val text: String, val sourceStart: Int, val sourceEnd: Int)

object MessageDocuments {
    private val fence = Regex("^ {0,3}(`{3,}|~{3,})(.*)$")
    private val heading = Regex("^ {0,3}(#{1,6})[ \\t]+(.*)$")
    private val list = Regex("^( *)([-+*]|[0-9]{1,9}[.)])[ \\t]+(.*)$")
    private val rule = Regex("^ {0,3}((\\*[ \\t]*){3,}|(-[ \\t]*){3,}|(_[ \\t]*){3,})$")
    private val task = Regex("^\\[([ xX])]\\s+(.*)$")
    private val detailOpen = Regex("^\\s*<details(?:\\s+open)?\\s*>\\s*$", RegexOption.IGNORE_CASE)
    private val summary = Regex("^\\s*<summary>(.*)</summary>\\s*$", RegexOption.IGNORE_CASE)
    private val alignment = Regex(":?-{3,}:?")
    private val email = Regex("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$")

    fun parse(source: String): MessageDocument = MessageDocument(source, blocks(lines(SourceText.from(source)), 0))
    fun inline(source: String): List<DocumentRun> = inline(SourceText.from(source))

    /** Formatting may disappear; unrecognized or incomplete syntax never disappears with it. */
    fun inline(source: SourceText, style: DocumentStyle = DocumentStyle(), depth: Int = 0): List<DocumentRun> {
        if (source.text.isEmpty()) return emptyList()
        if (depth >= 64) return listOf(DocumentRun(source, style))
        val result = mutableListOf<DocumentRun>()
        val text = source.text
        val buffer = StringBuilder(); val offsets = mutableListOf<Int>(); val ends = mutableListOf<Int>()
        var bufferedStyle: DocumentStyle? = null; var bufferedDestination: String? = null; var bufferedNamed = false
        fun flush() {
            if (buffer.isNotEmpty()) result += DocumentRun(SourceText(buffer.toString(), offsets.toList(), ends.toList()), bufferedStyle!!, bufferedDestination, bufferedNamed)
            buffer.setLength(0); offsets.clear(); ends.clear()
        }
        fun add(value: SourceText, selectedStyle: DocumentStyle = style, destination: String? = null, named: Boolean = false) {
            if (bufferedStyle != selectedStyle || bufferedDestination != destination || bufferedNamed != named) flush()
            bufferedStyle = selectedStyle; bufferedDestination = destination; bufferedNamed = named
            buffer.append(value.text); offsets.addAll(value.offsets); ends.addAll(value.ends)
        }
        var i = 0
        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length && text[i + 1] in "\\`*_{}[]()#+-.!|>~$") {
                add(source.slice(i + 1, i + 2)); i += 2; continue
            }
            if (text[i] == '`') {
                val count = text.drop(i).takeWhile { it == '`' }.length
                val token = "`".repeat(count)
                val end = text.indexOf(token, i + count)
                if (end >= 0) {
                    var content = source.slice(i + count, end)
                    content = content.copy(text = content.text.replace('\n', ' '))
                    if (content.text.startsWith(' ') && content.text.endsWith(' ') && content.text.isNotBlank()) content = content.slice(1, content.text.length - 1)
                    add(content, style.copy(code = true)); i = end + count; continue
                }
            }
            val image = text.startsWith("![", i)
            val labelStart = if (image) i + 2 else i + 1
            if (image || text[i] == '[') {
                val labelEnd = closing(text, '[', ']', labelStart)
                if (labelEnd >= 0 && text.getOrNull(labelEnd + 1) == '(') {
                    val destinationEnd = closing(text, '(', ')', labelEnd + 2)
                    if (destinationEnd >= 0) {
                        val raw = text.substring(labelEnd + 2, destinationEnd).trim()
                        val destination = if (raw.startsWith('<') && '>' in raw) raw.substringAfter('<').substringBefore('>')
                            else raw.substringBefore(Regex("\\s+[\"']").find(raw)?.value ?: "\u0000")
                        val label = source.slice(labelStart, labelEnd)
                        val children = inline(label, style, depth + 1)
                        children.forEach { add(it.source, it.style, openableLink(destination), true) }
                        if (children.isEmpty()) add(source.slice(i, destinationEnd + 1))
                        i = destinationEnd + 1; continue
                    }
                }
            }
            if (text[i] == '<') {
                val end = text.indexOf('>', i + 1)
                if (end > i) {
                    val value = text.substring(i + 1, end)
                    val destination = openableLink(if (email.matches(value)) "mailto:$value" else value)
                    if (destination != null) { add(source.slice(i + 1, end), destination = destination); i = end + 1; continue }
                }
            }
            val bare = if (text.startsWith("https://", i, true) || text.startsWith("http://", i, true))
                Regex("(?i)^(https?://[^\\s<>]+)").find(text.substring(i)) else null
            if (bare != null) {
                var value = bare.value.trimEnd('.', ',', ';', ':', '!', '?')
                while (value.endsWith(')') && value.count { it == ')' } > value.count { it == '(' }) value = value.dropLast(1)
                add(source.slice(i, i + value.length), destination = openableLink(value)); i += value.length; continue
            }
            val marker = listOf("***", "___", "**", "__", "~~", "*", "_", "$").firstOrNull { text.startsWith(it, i) }
            if (marker != null && !(marker.contains('_') && i > 0 && text[i - 1].isLetterOrDigit())) {
                val end = closingMarker(text, marker, i + marker.length)
                if (end > i + marker.length && !text[i + marker.length].isWhitespace() && !text[end - 1].isWhitespace()) {
                    val nestedStyle = when (marker) {
                        "***", "___" -> style.copy(strong = true, emphasis = true)
                        "**", "__" -> style.copy(strong = true)
                        "~~" -> style.copy(strike = true)
                        "$" -> style.copy(code = true)
                        else -> style.copy(emphasis = true)
                    }
                    val content = source.slice(i + marker.length, end)
                    if (marker == "$") add(content, nestedStyle)
                    else inline(content, nestedStyle, depth + 1).forEach { add(it.source, it.style, it.destination, it.namedLink) }
                    i = end + marker.length; continue
                }
            }
            if (text[i] == '&') {
                val end = text.indexOf(';', i + 1).takeIf { it in (i + 2)..minOf(i + 12, text.lastIndex) }
                val decoded = end?.let { entity(text.substring(i + 1, it)) }
                if (decoded != null) { add(SourceText(decoded, List(decoded.length) { source.offsets[i] }, List(decoded.length) { source.ends[end] })); i = end + 1; continue }
            }
            add(source.slice(i, i + 1)); i++
        }
        flush()
        return result
    }

    private fun closingMarker(text: String, marker: String, start: Int): Int {
        var cursor = start
        while (cursor < text.length) {
            val found = text.indexOf(marker, cursor)
            if (found < 0) return -1
            val runEnd = found + text.drop(found).takeWhile { it == marker.first() }.length
            val available = runEnd - found
            if (found > start && text[found - 1] != '\\' && !text[found - 1].isWhitespace() &&
                (marker.length != 1 || available % 2 == 1 || marker == "$")) return runEnd - marker.length
            cursor = maxOf(runEnd, found + 1)
        }
        return -1
    }

    fun openableLink(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.any { it.isWhitespace() || it.code < 32 || it in '\u202a'..'\u202e' || it in '\u2066'..'\u2069' }) return null
        return runCatching {
            val uri = java.net.URI(trimmed)
            when (uri.scheme?.lowercase()) {
                "http", "https" -> trimmed.takeIf { !uri.host.isNullOrBlank() && uri.userInfo == null }
                "mailto", "tel" -> trimmed.takeIf { !uri.schemeSpecificPart.isNullOrBlank() }
                else -> null
            }
        }.getOrNull()
    }

    private fun entity(value: String): String? {
        val named = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00a0")
        return named[value] ?: runCatching {
            val code = when { value.startsWith("#x", true) -> value.drop(2).toInt(16); value.startsWith('#') -> value.drop(1).toInt(); else -> return null }
            if (!Character.isValidCodePoint(code) || code == 0 || code in 0xd800..0xdfff) null else String(Character.toChars(code))
        }.getOrNull()
    }

    private fun closing(text: String, open: Char, close: Char, start: Int): Int {
        var level = 0; var i = start
        while (i < text.length) {
            if (text[i] == '\\') { i += 2; continue }
            if (text[i] == close && level == 0) return i
            if (text[i] == open) level++ else if (text[i] == close) level--
            i++
        }
        return -1
    }

    private fun lines(source: SourceText): List<SourceText> {
        val result = mutableListOf<SourceText>(); var start = 0
        source.text.forEachIndexed { i, c -> if (c == '\n') { result += source.slice(start, i); start = i + 1 } }
        result += source.slice(start)
        return result
    }
    private fun joined(lines: List<SourceText>): SourceText = lines.reduceOrNull { a, b ->
        a + SourceText("\n", listOf((a.offsets.lastOrNull() ?: (b.offsets.firstOrNull() ?: 1) - 2) + 1)) + b
    } ?: SourceText.from("")
    private fun trim(source: SourceText): SourceText {
        val start = source.text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: source.text.length
        val end = source.text.indexOfLast { !it.isWhitespace() } + 1
        return source.slice(start, maxOf(start, end))
    }
    private fun cells(line: SourceText): List<SourceText> {
        val result = mutableListOf<SourceText>(); var start = 0; var code = false; var escaped = false
        line.text.forEachIndexed { i, c ->
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '`' -> code = !code
                c == '|' && !code -> { result += trim(line.slice(start, i)); start = i + 1 }
            }
        }
        result += trim(line.slice(start))
        if (line.text.trimStart().startsWith('|')) result.removeAt(0)
        if (line.text.trimEnd().endsWith('|') && result.isNotEmpty()) result.removeAt(result.lastIndex)
        return result
    }

    private fun blocks(lines: List<SourceText>, depth: Int): List<DocumentBlock> {
        if (depth >= 24) return listOf(DocumentBlock.Paragraph(listOf(DocumentRun(joined(lines)))))
        val result = mutableListOf<DocumentBlock>(); var i = 0
        fun interrupts(index: Int): Boolean {
            val text = lines[index].text
            return text.isBlank() || fence.matches(text) || heading.matches(text) || rule.matches(text) ||
                text.trimStart().startsWith('>') || list.matches(text) || detailOpen.matches(text) || text.trim() == "$$"
        }
        while (i < lines.size) {
            val line = lines[i]; val text = line.text
            if (text.isBlank()) {
                val start = i; while (i < lines.size && lines[i].text.isBlank()) i++
                if (result.isNotEmpty() && i < lines.size) result += DocumentBlock.Blank((i - start).coerceAtMost(6))
                continue
            }
            val fenceMatch = fence.matchEntire(text)
            if (fenceMatch != null || text.trim() == "$$") {
                val token = fenceMatch?.groupValues?.get(1) ?: "$$"; val start = ++i
                while (i < lines.size && !lines[i].text.trim().let { it.length >= token.length && it.all { c -> c == token[0] } }) i++
                result += DocumentBlock.Code(joined(lines.subList(start, i)), fenceMatch?.groupValues?.get(2)?.trim().orEmpty())
                if (i < lines.size) i++
                continue
            }
            if (text.startsWith("    ") || text.startsWith('\t')) {
                val code = mutableListOf<SourceText>()
                while (i < lines.size && (lines[i].text.startsWith("    ") || lines[i].text.startsWith('\t') || lines[i].text.isBlank())) {
                    val current = lines[i++]; code += current.slice(if (current.text.startsWith('\t')) 1 else minOf(4, current.text.length))
                }
                result += DocumentBlock.Code(joined(code)); continue
            }
            val headingMatch = heading.matchEntire(text)
            if (headingMatch != null) {
                val body = headingMatch.groups[2]!!; var content = line.slice(body.range.first, body.range.last + 1)
                val trailing = Regex("\\s+#+\\s*$").find(content.text); if (trailing != null) content = content.slice(0, trailing.range.first)
                result += DocumentBlock.Heading(headingMatch.groupValues[1].length, inline(content)); i++; continue
            }
            if (i + 1 < lines.size && Regex("^ {0,3}(=+|-+)[ \\t]*$").matches(lines[i + 1].text) && !list.matches(text)) {
                result += DocumentBlock.Heading(if (lines[i + 1].text.trim().startsWith('=')) 1 else 2, inline(line)); i += 2; continue
            }
            if (rule.matches(text)) { result += DocumentBlock.Divider; i++; continue }
            if (detailOpen.matches(text) && i + 1 < lines.size && summary.matches(lines[i + 1].text)) {
                var end = i + 2; var nesting = 1
                while (end < lines.size) {
                    if (detailOpen.matches(lines[end].text)) nesting++
                    if (lines[end].text.trim().equals("</details>", true)) nesting--
                    if (nesting == 0) break
                    end++
                }
                if (end < lines.size) {
                    val label = summary.matchEntire(lines[i + 1].text)!!.groups[1]!!
                    result += DocumentBlock.Details(inline(lines[i + 1].slice(label.range.first, label.range.last + 1)),
                        blocks(lines.subList(i + 2, end), depth + 1), text.contains("open", true))
                    i = end + 1; continue
                }
            }
            if (text.trimStart().startsWith('>')) {
                val quoted = mutableListOf<SourceText>()
                while (i < lines.size && lines[i].text.trimStart().startsWith('>')) {
                    val value = lines[i++]; val marker = value.text.indexOf('>') + 1
                    quoted += value.slice(marker + if (value.text.getOrNull(marker) == ' ') 1 else 0)
                }
                result += DocumentBlock.Quote(blocks(quoted, depth + 1)); continue
            }
            val firstItem = list.matchEntire(text)
            if (firstItem != null) {
                val indent = firstItem.groupValues[1].length; val ordered = firstItem.groupValues[2].first().isDigit()
                val items = mutableListOf<DocumentListItem>()
                while (i < lines.size) {
                    val match = list.matchEntire(lines[i].text) ?: break
                    if (match.groupValues[1].length != indent || match.groupValues[2].first().isDigit() != ordered) break
                    val body = match.groups[3]!!; val contentIndent = body.range.first
                    var first = lines[i++].slice(contentIndent)
                    val check = task.matchEntire(first.text)
                    val checked = check?.groupValues?.get(1)?.let { it.equals("x", true) }
                    if (check != null) first = first.slice(check.groups[2]!!.range.first)
                    val itemLines = mutableListOf(first)
                    while (i < lines.size) {
                        val current = lines[i]
                        if (current.text.isBlank()) {
                            if (i + 1 < lines.size && lines[i + 1].text.takeWhile { it == ' ' }.length > indent) { itemLines += current; i++; continue }
                            break
                        }
                        val spaces = current.text.takeWhile { it == ' ' }.length
                        if (spaces <= indent) break
                        itemLines += current.slice(minOf(contentIndent, spaces)); i++
                    }
                    items += DocumentListItem(if (ordered) match.groupValues[2].dropLast(1).toIntOrNull() else null, checked, blocks(itemLines, depth + 1))
                }
                result += DocumentBlock.ListBlock(items, ordered); continue
            }
            if (i + 1 < lines.size && '|' in text) {
                val separators = cells(lines[i + 1])
                if (separators.isNotEmpty() && separators.all { alignment.matches(it.text) }) {
                    val headers = cells(line).map { inline(it) }; val rows = mutableListOf<List<List<DocumentRun>>>()
                    i += 2
                    while (i < lines.size && lines[i].text.isNotBlank() && '|' in lines[i].text) rows += cells(lines[i++]).map { inline(it) }
                    result += DocumentBlock.Table(headers, rows, separators.map {
                        when { it.text.startsWith(':') && it.text.endsWith(':') -> DocumentAlignment.Center; it.text.endsWith(':') -> DocumentAlignment.End; else -> DocumentAlignment.Start }
                    }); continue
                }
            }
            val paragraph = mutableListOf(line); i++
            while (i < lines.size && !interrupts(i)) {
                if (i + 1 < lines.size && cells(lines[i + 1]).let { row -> row.isNotEmpty() && row.all { alignment.matches(it.text) } }) break
                paragraph += lines[i++]
            }
            result += DocumentBlock.Paragraph(inline(joined(paragraph)))
        }
        return result
    }

    fun plainText(source: String): String = plainText(parse(source).blocks)
    fun plainText(blocks: List<DocumentBlock>): String = blocks.joinToString("\n") { block ->
        fun text(runs: List<DocumentRun>) = runs.joinToString("") { it.source.text }
        when (block) {
            is DocumentBlock.Paragraph -> text(block.runs)
            is DocumentBlock.Heading -> text(block.runs)
            is DocumentBlock.Code -> block.source.text
            is DocumentBlock.Quote -> plainText(block.blocks)
            is DocumentBlock.ListBlock -> block.items.joinToString("\n") { item ->
                (item.number?.let { "$it. " } ?: if (item.checked == null) "• " else if (item.checked) "☑ " else "☐ ") + plainText(item.blocks)
            }
            is DocumentBlock.Table -> (listOf(block.header) + block.rows).joinToString("\n") { row -> row.joinToString(" | ") { text(it) } }
            is DocumentBlock.Details -> text(block.summary) + "\n" + plainText(block.blocks)
            is DocumentBlock.Blank -> "\n".repeat((block.count - 1).coerceAtLeast(0))
            DocumentBlock.Divider -> "—"
        }
    }

    fun passage(source: String, displayed: SourceText, start: Int, end: Int): MessagePassage? {
        var from = minOf(start, end).coerceIn(0, displayed.text.length)
        var until = maxOf(start, end).coerceIn(0, displayed.text.length)
        if (from == until) return null
        if (from > 0 && displayed.text[from].isLowSurrogate() && displayed.text[from - 1].isHighSurrogate()) from--
        if (until < displayed.text.length && displayed.text[until].isLowSurrogate() && displayed.text[until - 1].isHighSurrogate()) until++
        val offsets = displayed.offsets.subList(from, until).filter { it in source.indices }
        if (offsets.isEmpty()) return null
        val first = offsets.min()
        val sourceEnd = displayed.ends.subList(from, until).max().coerceAtMost(source.length)
        return MessagePassage(displayed.text.substring(from, until), first, sourceEnd)
    }
}
