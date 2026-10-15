package com.dude555afk.xeno.ui.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders streamed answer text as Compose spans.
 *
 * A markdown parser would be overkill — the answer prompt constrains the model
 * to plain paragraphs plus inline `[n]` citations, which is all this handles.
 * Handling less markup means fewer rendering bugs on partially-streamed text,
 * which is the state the user actually looks at 90% of the time.
 */
object AnswerText {

    data class Parsed(
        val blocks: List<Block>
    ) {
        sealed interface Block {
            data class Heading(val text: String) : Block
            data class Paragraph(val text: String) : Block
            data class Bullet(val text: String) : Block
        }
    }

    fun parse(raw: String): Parsed {
        val lines = raw.split('\n')
        val blocks = mutableListOf<Parsed.Block>()
        val paragraph = StringBuilder()

        fun flush() {
            if (paragraph.isNotBlank()) blocks += Parsed.Block.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }

        lines.forEach { lineRaw ->
            val line = lineRaw.trim()
            when {
                line.isEmpty() -> flush()
                line.startsWith("###") || line.startsWith("##") -> {
                    flush()
                    blocks += Parsed.Block.Heading(line.trimStart('#', ' '))
                }
                line.startsWith("•") || line.startsWith("-") || line.startsWith("*") -> {
                    flush()
                    blocks += Parsed.Block.Bullet(line.trimStart('•', '-', '*', ' '))
                }
                else -> paragraph.append(' ').append(line)
            }
        }
        flush()
        return Parsed(blocks)
    }

    /**
     * Build styled spans, bolding `**text**` and tagging `[n]` citations so the
     * renderer can make them tappable.
     */
    fun annotated(
        text: String,
        accent: Color,
        onCitation: (Int) -> Unit
    ): AnnotatedString = buildAnnotatedString {
        val tokenRegex = Regex("\\*\\*(.+?)\\*\\*|\\[(\\d{1,2})]")
        var cursor = 0

        tokenRegex.findAll(text).forEach { match ->
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }

            val bold = match.groupValues[1]
            val citation = match.groupValues[2]

            if (bold.isNotEmpty()) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            } else if (citation.isNotEmpty()) {
                val index = citation.toIntOrNull() ?: 0
                pushStringAnnotation(tag = CITE_TAG, annotation = index.toString())
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
                    append("[$citation]")
                }
                pop()
            }
            cursor = match.range.last + 1
        }

        if (cursor < text.length) append(text.substring(cursor))
    }

    const val CITE_TAG = "xeno.citation"
}

/**
 * A citation pill that scrolls the source card into view when tapped.
 *
 * The tiny affordance is the whole trust loop — it turns "an AI said so" into
 * "a page said so".
 */
@Composable
fun CitationChip(
    index: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "[$index]",
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}
