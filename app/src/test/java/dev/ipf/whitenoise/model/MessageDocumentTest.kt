package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class MessageDocumentTest {
    @Test fun headingLevelsSetextAndThematicBreaksRemainDistinct() {
        val doc = MessageDocuments.parse((1..6).joinToString("\n") { "${"#".repeat(it)} Heading $it" } + "\n\nSubtitle\n===\n\n***")
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 1), doc.blocks.filterIsInstance<DocumentBlock.Heading>().map { it.level })
        assertTrue(doc.blocks.last() is DocumentBlock.Divider)
    }
    @Test fun quotesNestedListsTasksAndOriginalOrderedNumberArePreserved() {
        val source = "> ## Plan\n> 3. First\n>    - Nested\n> 4. Next\n\n- [x] Done\n- [ ] Later"
        val doc = MessageDocuments.parse(source)
        val quote = doc.blocks.first() as DocumentBlock.Quote
        val ordered = quote.blocks[1] as DocumentBlock.ListBlock
        assertTrue(ordered.ordered); assertEquals(listOf(3, 4), ordered.items.map { it.number })
        assertTrue(ordered.items.first().blocks[1] is DocumentBlock.ListBlock)
        assertEquals(listOf(true, false), (doc.blocks.last() as DocumentBlock.ListBlock).items.map { it.checked })
    }
    @Test fun fencedIndentedAndMathCodeStayLiteralRatherThanExecutingOrFetching() {
        val source = "```kotlin\n**literal**\nval link = \"https://example.org\"\n```\n\n    indented\n\n${'$'}${'$'}\nx^2 + y^2\n${'$'}${'$'}"
        val code = MessageDocuments.parse(source).blocks.filterIsInstance<DocumentBlock.Code>()
        assertEquals(3, code.size); assertEquals("kotlin", code.first().language)
        assertTrue(code.first().source.text.startsWith("**literal**")); assertTrue(code.last().source.text.contains("x^2"))
    }
    @Test fun tablesRetainAlignmentAndEscapedOrCodePipesInsideCells() {
        val table = MessageDocuments.parse("| Left | Center | Right |\n| :--- | :---: | ---: |\n| a\\|b | `x|y` | **bold** |").blocks.single() as DocumentBlock.Table
        assertEquals(listOf(DocumentAlignment.Start, DocumentAlignment.Center, DocumentAlignment.End), table.alignments)
        assertEquals(3, table.rows.single().size)
        assertEquals("a|b", table.rows.single()[0].joinToString("") { it.source.text })
        assertEquals("x|y", table.rows.single()[1].single().source.text)
    }
    @Test fun nestedDisclosuresKeepTheirSummaryAndFullBodyAndUnclosedTagsStayLiteral() {
        val source = "<details open>\n<summary>**More**</summary>\n## Inside\n<details>\n<summary>Nested</summary>\nBody\n</details>\n</details>"
        val detail = MessageDocuments.parse(source).blocks.single() as DocumentBlock.Details
        assertTrue(detail.initiallyOpen); assertTrue(detail.summary.single().style.strong)
        assertTrue(detail.blocks.last() is DocumentBlock.Details)
        assertTrue(MessageDocuments.plainText(source).contains("Nested\nBody"))
        assertTrue(MessageDocuments.plainText("<details>\n<summary>Unclosed</summary>").contains("<details>"))
    }
    @Test fun nestedFormattingEscapesEntitiesInlineCodeAndImagesKeepTextAndOffsets() {
        val source = "**bold *inside*** \\*literal\\* &amp; &#x1F9AB; `**code**` ![Alt](https://example.org/image.png)"
        val runs = MessageDocuments.inline(source)
        assertTrue(runs.any { it.source.text == "inside" && it.style.strong && it.style.emphasis })
        assertTrue(runs.any { it.source.text == "**code**" && it.style.code })
        assertEquals("bold inside *literal* & 🦫 **code** Alt", runs.joinToString("") { it.source.text })
        assertEquals("https://example.org/image.png", runs.last().destination)
        runs.forEach { run -> assertEquals(run.source.text.length, run.source.offsets.size) }
    }
    @Test fun onlySupportedDestinationsAreActionableAndNamedLinksRemainIdentified() {
        val source = "[Safe](https://example.org/a_(b)) [Unsafe](javascript:alert(1)) <name@example.org> https://example.org."
        val links = MessageDocuments.inline(source).filter { it.destination != null }
        assertEquals(listOf("https://example.org/a_(b)", "mailto:name@example.org", "https://example.org"), links.map { it.destination })
        assertTrue(links.first().namedLink); assertFalse(links.last().namedLink)
        assertNull(MessageDocuments.openableLink("https://user:password@example.org"))
        assertNull(MessageDocuments.openableLink("https://example.org/\u202Ehidden"))
        assertTrue(MessageDocuments.plainText(source).contains("Unsafe"))
    }
    @Test fun selectionOfSecondRepeatedWordUsesItsOwnSourceRange() {
        val source = "**echo** and *echo*"
        val run = MessageDocuments.inline(source).last().source
        val passage = MessageDocuments.passage(source, run, 0, 4)!!
        assertEquals("echo", passage.text); assertEquals(source.lastIndexOf("echo"), passage.sourceStart)
        assertEquals(source.lastIndexOf("echo") + 4, passage.sourceEnd)
    }
    @Test fun reversedRangesAndSurrogateBoundariesNeverSplitEmojiOrEntitySources() {
        val source = "a🦫b &amp;"
        val display = MessageDocuments.inline(source).map { it.source }.reduce(SourceText::plus)
        val emoji = MessageDocuments.passage(source, display, 3, 2)!!
        assertEquals("🦫", emoji.text); assertEquals(1, emoji.sourceStart); assertEquals(3, emoji.sourceEnd)
        val entity = MessageDocuments.passage(source, display, display.text.length - 1, display.text.length)!!
        assertEquals("&", entity.text); assertEquals(source.indexOf('&'), entity.sourceStart); assertEquals(source.length, entity.sourceEnd)
        assertNull(MessageDocuments.passage(source, display, 0, 0))
    }
    @Test fun deeplyNestedInputFallsBackToVisibleSourceWithoutDroppingAuthoredContent() {
        val source = "> ".repeat(30) + "Still here"
        val doc = MessageDocuments.parse(source)
        assertEquals(source, doc.source); assertTrue(MessageDocuments.plainText(doc.blocks).contains("Still here"))
    }
    @Test fun longOrdinaryInputCoalescesIntoOneRunAndKeepsItsWholeSource() {
        val source = "A long message with repeated words. ".repeat(1000)
        val runs = MessageDocuments.inline(source)
        assertEquals(1, runs.size); assertEquals(source, runs.single().source.text)
        assertEquals(source.lastIndex, runs.single().source.offsets.last())
    }
}
