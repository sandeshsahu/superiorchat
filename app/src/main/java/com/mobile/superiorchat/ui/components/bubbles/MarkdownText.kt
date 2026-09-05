package com.mobile.superiorchat.ui.components.bubbles

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceContainerHighest
import com.mobile.superiorchat.theme.SurfaceLevel2

// ──────────────────────────────────────────────────────────────
// Markdown String Annotation Tags
// ──────────────────────────────────────────────────────────────
const val TAG_INLINE_CODE = "INLINE_CODE"
const val TAG_MARKDOWN_SPOILER = "MARKDOWN_SPOILER"
const val TAG_MARKDOWN_LINK = "MARKDOWN_LINK"

/**
 * Data class representing the parsed state of a Markdown text,
 * including the formatted AnnotatedString and ranges of quote blocks.
 */
data class ParsedMarkdown(
    val annotatedString: AnnotatedString,
    val quoteRanges: List<IntRange>
)

/**
 * High-performance, Telegram-grade Markdown text renderer.
 * Supports:
 * - Bold: **bold** or *bold*
 * - Italic: __italic__ or _italic_
 * - Monospace / Code: `code` or ```code block``` (touch-to-copy enabled)
 * - Strikethrough: ~~strike~~ or ~strike~
 * - Underline: <u>underline</u>
 * - Spoilers: ||spoiler|| (conceal by default, tap exact text to reveal)
 * - Blockquotes: > quote text (vertical accent bar, background indent)
 * - Links: [Label](url) and autolinks (https://..., t.me/...)
 */
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    isFromMe: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = TextStyle.Default,
    pressedCodeRange: IntRange? = null,
    revealedSpoilerIds: Set<String> = emptySet(),
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    val codeBg = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                 else SurfaceContainerHighest
    val codePressBg = if (isFromMe) MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.40f)
                      else PrimaryLight.copy(alpha = 0.40f)
    val spoilerBg = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val quoteBarColor = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer
                        else PrimaryLight
    val quoteBg = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
                  else SurfaceLevel2.copy(alpha = 0.60f)
    val linkColor = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer
                    else PrimaryLight

    val parsed = remember(text, codeBg, codePressBg, spoilerBg, linkColor, pressedCodeRange, revealedSpoilerIds, maxLines) {
        parseMarkdown(
            raw = text,
            textColor = color,
            codeBg = codeBg,
            codePressBg = codePressBg,
            spoilerBg = spoilerBg,
            linkColor = linkColor,
            pressedCodeRange = pressedCodeRange,
            revealedSpoilerIds = revealedSpoilerIds,
            singleLineMode = maxLines == 1
        )
    }

    var textLayoutResultState: TextLayoutResult? = null

    val drawModifier = if (parsed.quoteRanges.isNotEmpty() && maxLines > 1) {
        Modifier.drawBehind {
            val lr = textLayoutResultState ?: return@drawBehind
            for (range in parsed.quoteRanges) {
                if (range.first >= lr.layoutInput.text.length) continue
                val startOffset = range.first.coerceIn(0, lr.layoutInput.text.length - 1)
                val endOffset = range.last.coerceIn(0, lr.layoutInput.text.length - 1)
                val startLine = lr.getLineForOffset(startOffset)
                val endLine = lr.getLineForOffset(endOffset)
                val top = lr.getLineTop(startLine)
                val bottom = lr.getLineBottom(endLine)
                val h = bottom - top

                // Background container
                drawRoundRect(
                    color = quoteBg,
                    topLeft = Offset(0f, top - 2.dp.toPx()),
                    size = Size(size.width, h + 4.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Left accent vertical bar
                drawRoundRect(
                    color = quoteBarColor,
                    topLeft = Offset(0f, top),
                    size = Size(3.dp.toPx(), h),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }
        }
    } else {
        Modifier
    }

    Text(
        text = parsed.annotatedString,
        color = color,
        modifier = modifier.then(drawModifier),
        maxLines = maxLines,
        overflow = overflow,
        style = style,
        onTextLayout = {
            textLayoutResultState = it
            onTextLayout?.invoke(it)
        }
    )
}

// ──────────────────────────────────────────────────────────────
// Hit-Testing Helpers (Precise Glyph-Level Bounding Box Matching)
// ──────────────────────────────────────────────────────────────

/**
 * Finds the INLINE_CODE annotation at the specified tap offset.
 * Checks character position and glyph bounding box within a ±2f tolerance.
 */
fun findCodeAnnotationAt(
    bubbleOffset: Offset,
    bubbleCoords: LayoutCoordinates?,
    textCoords: LayoutCoordinates?,
    lr: TextLayoutResult?,
    rawText: String?
): AnnotatedString.Range<String>? {
    if (rawText.isNullOrEmpty() || bubbleCoords == null || textCoords == null || lr == null) return null
    if (!bubbleCoords.isAttached || !textCoords.isAttached) return null
    val localOffset = textCoords.localPositionOf(bubbleCoords, bubbleOffset)
    if (localOffset.x < 0 || localOffset.x > textCoords.size.width ||
        localOffset.y < 0 || localOffset.y > textCoords.size.height) {
        return null
    }
    val charIndex = lr.getOffsetForPosition(localOffset)
    val parsed = parseMarkdown(rawText, Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
    val allAnnotations = parsed.annotatedString.getStringAnnotations(tag = TAG_INLINE_CODE, start = 0, end = parsed.annotatedString.length)
    val codeAnn = allAnnotations.firstOrNull { charIndex >= it.start && charIndex < it.end } ?: return null

    val glyphBounds = lr.getBoundingBox(charIndex)
    if (localOffset.x < (glyphBounds.left - 2f) || localOffset.x > (glyphBounds.right + 2f) ||
        localOffset.y < (glyphBounds.top - 2f) || localOffset.y > (glyphBounds.bottom + 2f)) {
        return null
    }
    return codeAnn
}

/**
 * Finds the MARKDOWN_SPOILER annotation at the specified tap offset.
 * Strictly verifies glyph bounds to ensure ONLY tapping the exact spoiler text triggers revelation.
 */
fun findSpoilerAnnotationAt(
    bubbleOffset: Offset,
    bubbleCoords: LayoutCoordinates?,
    textCoords: LayoutCoordinates?,
    lr: TextLayoutResult?,
    rawText: String?
): AnnotatedString.Range<String>? {
    if (rawText.isNullOrEmpty() || bubbleCoords == null || textCoords == null || lr == null) return null
    if (!bubbleCoords.isAttached || !textCoords.isAttached) return null
    val localOffset = textCoords.localPositionOf(bubbleCoords, bubbleOffset)
    if (localOffset.x < 0 || localOffset.x > textCoords.size.width ||
        localOffset.y < 0 || localOffset.y > textCoords.size.height) {
        return null
    }
    val charIndex = lr.getOffsetForPosition(localOffset)
    val parsed = parseMarkdown(rawText, Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
    val allAnnotations = parsed.annotatedString.getStringAnnotations(tag = TAG_MARKDOWN_SPOILER, start = 0, end = parsed.annotatedString.length)
    val spoilerAnn = allAnnotations.firstOrNull { charIndex >= it.start && charIndex < it.end } ?: return null

    val glyphBounds = lr.getBoundingBox(charIndex)
    if (localOffset.x < (glyphBounds.left - 2f) || localOffset.x > (glyphBounds.right + 2f) ||
        localOffset.y < (glyphBounds.top - 2f) || localOffset.y > (glyphBounds.bottom + 2f)) {
        return null
    }
    return spoilerAnn
}

/**
 * Finds the MARKDOWN_LINK annotation at the specified tap offset.
 * Strictly verifies glyph bounds so tapping outside links does not trigger browser navigation.
 */
fun findLinkAnnotationAt(
    bubbleOffset: Offset,
    bubbleCoords: LayoutCoordinates?,
    textCoords: LayoutCoordinates?,
    lr: TextLayoutResult?,
    rawText: String?
): AnnotatedString.Range<String>? {
    if (rawText.isNullOrEmpty() || bubbleCoords == null || textCoords == null || lr == null) return null
    if (!bubbleCoords.isAttached || !textCoords.isAttached) return null
    val localOffset = textCoords.localPositionOf(bubbleCoords, bubbleOffset)
    if (localOffset.x < 0 || localOffset.x > textCoords.size.width ||
        localOffset.y < 0 || localOffset.y > textCoords.size.height) {
        return null
    }
    val charIndex = lr.getOffsetForPosition(localOffset)
    val parsed = parseMarkdown(rawText, Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
    val allAnnotations = parsed.annotatedString.getStringAnnotations(tag = TAG_MARKDOWN_LINK, start = 0, end = parsed.annotatedString.length)
    val linkAnn = allAnnotations.firstOrNull { charIndex >= it.start && charIndex < it.end } ?: return null

    val glyphBounds = lr.getBoundingBox(charIndex)
    if (localOffset.x < (glyphBounds.left - 2f) || localOffset.x > (glyphBounds.right + 2f) ||
        localOffset.y < (glyphBounds.top - 2f) || localOffset.y > (glyphBounds.bottom + 2f)) {
        return null
    }
    return linkAnn
}

// ──────────────────────────────────────────────────────────────
// Parser Data Models & AST Lexer
// ──────────────────────────────────────────────────────────────

private data class StyleState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrike: Boolean = false,
    val isUnderline: Boolean = false,
    val isCode: Boolean = false,
    val isPre: Boolean = false,
    val isSpoiler: Boolean = false,
    val spoilerId: String? = null,
    val linkUrl: String? = null
)

private data class FormattedSegment(
    val text: String,
    val style: StyleState
)

private enum class MatchType {
    PRE, CODE, SPOILER, MARKDOWN_LINK, AUTOLINK, UNDERLINE, BOLD, ITALIC, STRIKE
}

private data class MatchResultData(
    val start: Int,
    val end: Int,
    val inner: String,
    val extra: String? = null,
    val type: MatchType
)

private val REGEX_PRE = Regex("""```(?:[a-zA-Z0-9_-]+)?\n?([\s\S]*?)```""")
private val REGEX_INLINE_CODE = Regex("""`([^`\n]+)`""")
private val REGEX_SPOILER = Regex("""\|\|([\s\S]+?)\|\|""")
private val REGEX_MARKDOWN_LINK = Regex("""\[([^\]\n]+)\]\(((?:https?://|t\.me/|tg://)[^\s\)]+)\)""")
private val REGEX_AUTOLINK = Regex("""(?<!\()(https?://[a-zA-Z0-9.\-_~:/?#\[\]@!$&'()*+,;=%]+|t\.me/[a-zA-Z0-9_]+)""")
private val REGEX_UNDERLINE = Regex("""<u>([\s\S]+?)</u>""", RegexOption.IGNORE_CASE)
private val REGEX_BOLD_DOUBLE = Regex("""\*\*([^\*\n]+?)\*\*""")
private val REGEX_ITALIC_DOUBLE = Regex("""__([^_\n]+?)__""")
private val REGEX_STRIKE_DOUBLE = Regex("""~~([^~\n]+?)~~""")
private val REGEX_BOLD_SINGLE = Regex("""(?<=\s|^|[^\w*])\*(?!\s)([^*\n]+?)(?<!\s)\*(?=\s|$|[^\w*])""")
private val REGEX_ITALIC_SINGLE = Regex("""(?<=\s|^|[^\w_])_(?!\s)([^_\n]+?)(?<!\s)_(?=\s|$|[^\w_])""")
private val REGEX_STRIKE_SINGLE = Regex("""(?<=\s|^|[^\w~])~(?!\s)([^~\n]+?)(?<!\s)~(?=\s|$|[^\w~])""")

private fun findEarliestMatch(text: String): MatchResultData? {
    val matches = mutableListOf<MatchResultData>()

    REGEX_PRE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.PRE)) }
    REGEX_INLINE_CODE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.CODE)) }
    REGEX_SPOILER.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.SPOILER)) }
    REGEX_MARKDOWN_LINK.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], it.groupValues[2], MatchType.MARKDOWN_LINK)) }
    REGEX_AUTOLINK.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], it.groupValues[1], MatchType.AUTOLINK)) }
    REGEX_UNDERLINE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.UNDERLINE)) }
    REGEX_BOLD_DOUBLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.BOLD)) }
    REGEX_ITALIC_DOUBLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.ITALIC)) }
    REGEX_STRIKE_DOUBLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.STRIKE)) }
    REGEX_BOLD_SINGLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.BOLD)) }
    REGEX_ITALIC_SINGLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.ITALIC)) }
    REGEX_STRIKE_SINGLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], null, MatchType.STRIKE)) }

    return matches.minByOrNull { it.start }
}

private fun parseMarkdownSegments(
    text: String,
    currentStyle: StyleState = StyleState(),
    offsetTracker: Int = 0
): List<FormattedSegment> {
    if (text.isEmpty()) return emptyList()

    val earliest = findEarliestMatch(text) ?: return listOf(FormattedSegment(text, currentStyle))

    val result = mutableListOf<FormattedSegment>()

    // Text before the match
    if (earliest.start > 0) {
        result.addAll(parseMarkdownSegments(text.substring(0, earliest.start), currentStyle, offsetTracker))
    }

    val matchGlobalStart = offsetTracker + earliest.start

    // Process matched inner content
    when (earliest.type) {
        MatchType.PRE -> {
            result.add(FormattedSegment(earliest.inner, currentStyle.copy(isPre = true)))
        }
        MatchType.CODE -> {
            result.add(FormattedSegment(earliest.inner, currentStyle.copy(isCode = true)))
        }
        MatchType.SPOILER -> {
            val spoilerId = "sp_${matchGlobalStart}_${matchGlobalStart + earliest.inner.length}"
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isSpoiler = true, spoilerId = spoilerId), matchGlobalStart))
        }
        MatchType.MARKDOWN_LINK -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(linkUrl = earliest.extra), matchGlobalStart))
        }
        MatchType.AUTOLINK -> {
            result.add(FormattedSegment(earliest.inner, currentStyle.copy(linkUrl = earliest.extra)))
        }
        MatchType.UNDERLINE -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isUnderline = true), matchGlobalStart))
        }
        MatchType.BOLD -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isBold = true), matchGlobalStart))
        }
        MatchType.ITALIC -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isItalic = true), matchGlobalStart))
        }
        MatchType.STRIKE -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isStrike = true), matchGlobalStart))
        }
    }

    // Text after the match
    if (earliest.end < text.length) {
        result.addAll(parseMarkdownSegments(text.substring(earliest.end), currentStyle, offsetTracker + earliest.end))
    }

    return result
}

/**
 * Parses raw text into an AnnotatedString and tracks quote block ranges.
 */
fun parseMarkdown(
    raw: String,
    textColor: Color,
    codeBg: Color,
    codePressBg: Color,
    spoilerBg: Color,
    linkColor: Color = PrimaryLight,
    pressedCodeRange: IntRange? = null,
    revealedSpoilerIds: Set<String> = emptySet(),
    singleLineMode: Boolean = false
): ParsedMarkdown {
    if (raw.isEmpty()) return ParsedMarkdown(AnnotatedString(""), emptyList())

    val quoteRanges = mutableListOf<IntRange>()

    val annotated = buildAnnotatedString {
        val lines = raw.lines()
        var currentOffset = 0
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val isQuoteLine = line.trimStart().startsWith(">") && !singleLineMode

            if (isQuoteLine) {
                // Collect consecutive quote lines into one block
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    val stripped = lines[i].trimStart().removePrefix(">").removePrefix(" ")
                    quoteLines.add(stripped)
                    i++
                }
                val quoteBlockText = quoteLines.joinToString("\n")
                val blockStart = currentOffset

                withStyle(ParagraphStyle(textIndent = TextIndent(firstLine = 12.sp, restLine = 12.sp))) {
                    val segments = parseMarkdownSegments(quoteBlockText, offsetTracker = blockStart)
                    for (seg in segments) {
                        val segStart = currentOffset
                        appendSegment(seg, textColor, codeBg, codePressBg, spoilerBg, linkColor, pressedCodeRange, revealedSpoilerIds, currentOffset)
                        currentOffset += seg.text.length
                    }
                }
                val blockEnd = currentOffset
                quoteRanges.add(blockStart until blockEnd)

                if (i < lines.size) {
                    append("\n")
                    currentOffset += 1
                }
            } else {
                // Regular line
                val cleanLine = if (singleLineMode && line.trimStart().startsWith(">")) {
                    line.trimStart().removePrefix(">").trimStart()
                } else {
                    line
                }
                val lineStart = currentOffset
                val segments = parseMarkdownSegments(cleanLine, offsetTracker = lineStart)
                for (seg in segments) {
                    appendSegment(seg, textColor, codeBg, codePressBg, spoilerBg, linkColor, pressedCodeRange, revealedSpoilerIds, currentOffset)
                    currentOffset += seg.text.length
                }
                i++
                if (i < lines.size) {
                    append("\n")
                    currentOffset += 1
                }
            }
        }
    }

    return ParsedMarkdown(annotated, quoteRanges)
}

private fun AnnotatedString.Builder.appendSegment(
    seg: FormattedSegment,
    textColor: Color,
    codeBg: Color,
    codePressBg: Color,
    spoilerBg: Color,
    linkColor: Color,
    pressedCodeRange: IntRange?,
    revealedSpoilerIds: Set<String>,
    currentOffset: Int
) {
    var span = SpanStyle()

    if (seg.style.isBold) {
        span = span.copy(fontWeight = FontWeight.Bold)
    }
    if (seg.style.isItalic) {
        span = span.copy(fontStyle = FontStyle.Italic)
    }
    if (seg.style.isStrike && seg.style.isUnderline) {
        span = span.copy(textDecoration = TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline)))
    } else if (seg.style.isStrike) {
        span = span.copy(textDecoration = TextDecoration.LineThrough)
    } else if (seg.style.isUnderline) {
        span = span.copy(textDecoration = TextDecoration.Underline)
    }

    val isCodeBlock = seg.style.isCode || seg.style.isPre
    val isThisCodePressed = isCodeBlock && pressedCodeRange != null && (currentOffset in pressedCodeRange)
    if (isCodeBlock) {
        span = span.copy(
            fontFamily = FontFamily.Monospace,
            background = if (isThisCodePressed) codePressBg else codeBg
        )
    }

    // Spoiler styling: if concealed, hide text completely with Color.Transparent
    if (seg.style.isSpoiler) {
        val isRevealed = seg.style.spoilerId != null && revealedSpoilerIds.contains(seg.style.spoilerId)
        if (isRevealed) {
            span = span.copy(
                background = spoilerBg.copy(alpha = 0.15f)
            )
        } else {
            span = span.copy(
                color = Color.Transparent,
                background = spoilerBg
            )
        }
    }

    // Link styling
    if (seg.style.linkUrl != null) {
        span = span.copy(
            color = linkColor,
            textDecoration = TextDecoration.Underline
        )
    }

    val startOffset = currentOffset
    withStyle(span) {
        append(seg.text)
    }

    // Attach string annotations
    if (isCodeBlock) {
        addStringAnnotation(
            tag = TAG_INLINE_CODE,
            annotation = seg.text,
            start = startOffset,
            end = startOffset + seg.text.length
        )
    }

    if (seg.style.isSpoiler && seg.style.spoilerId != null) {
        addStringAnnotation(
            tag = TAG_MARKDOWN_SPOILER,
            annotation = seg.style.spoilerId,
            start = startOffset,
            end = startOffset + seg.text.length
        )
    }

    if (seg.style.linkUrl != null) {
        addStringAnnotation(
            tag = TAG_MARKDOWN_LINK,
            annotation = seg.style.linkUrl,
            start = startOffset,
            end = startOffset + seg.text.length
        )
    }
}
