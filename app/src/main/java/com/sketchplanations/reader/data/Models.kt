package com.sketchplanations.reader.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Sketch(
    val n: Int,
    val title: String,
    val raw: LocalDate,
    val tags: List<String>,
    /** Standfirst. */
    val lead: String,
    /** Body paragraphs. Links are written inline as `[kind|label]`. */
    val body: List<String>,
) {
    val num get() = "#$n"
    val date: String get() = raw.format(DisplayDate)
    val year get() = raw.year.toString()
    /** Leading articles are ignored for A–Z: The Diderot Effect files under D. */
    val sortKey: String get() = title.replace(Regex("^(The|A|An)\\s+"), "")
    val size: String get() = String.format(Locale.UK, "%.1f MB", 1.2 + (n % 9) * 0.3)
}

private val DisplayDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

enum class LinkKind(val label: String, val internal: Boolean) {
    Sketch("Sketchplanation", true),
    Pod("Podcast episode", true),
    Book("Book (affiliate)", false),
    Paper("Paper / study", false),
    Video("Video", false),
    Web("External site", false);

    companion object {
        fun of(s: String) = entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: Web
    }
}

/** An outbound link from a sketch. `href` is a URL, `#s<n>` for a sketch or `#ep<n>` for an episode. */
data class SketchLink(val kind: LinkKind, val label: String, val href: String) {
    val sketchRef: Int? get() = Regex("^#s(\\d+)$").find(href)?.groupValues?.get(1)?.toInt()
    val episodeRef: Int? get() = Regex("^#ep(\\d+)$").find(href)?.groupValues?.get(1)?.toInt()
    val host: String get() = href.split('/').getOrNull(2)?.removePrefix("www.") ?: ""
}

data class Chapter(val at: Int, val label: String)

data class Episode(
    val title: String,
    val num: Int,
    val len: String,
    val date: String,
    val notes: List<String>,
    val chapters: List<Chapter>,
) {
    /** Placeholder runtime: every stand-in episode runs 41 minutes, as in the prototype. */
    val seconds: Int get() = 41 * 60
}

data class Topic(val name: String, val count: Int)

/** A run of prose: plain text, or an inline link to be badged by kind. */
sealed interface ProsePart {
    data class Plain(val text: String) : ProsePart
    data class Link(val kind: LinkKind, val text: String, val target: SketchLink?) : ProsePart
}

/**
 * Splits `[kind|label]` markers out of a paragraph. Inline markers pair with the sketch's
 * Links list by kind and order: the first inline `book` is the first `book` link, and so on.
 */
fun parseBody(body: List<String>, links: List<SketchLink>): List<List<ProsePart>> {
    val used = HashMap<LinkKind, Int>()
    return body.map { parseProse(it, links, used) }
}

private fun parseProse(paragraph: String, links: List<SketchLink>, used: HashMap<LinkKind, Int>): List<ProsePart> {
    val out = mutableListOf<ProsePart>()
    var i = 0
    Regex("\\[([a-z]+)\\|([^\\]]+)]").findAll(paragraph).forEach { m ->
        if (m.range.first > i) out += ProsePart.Plain(paragraph.substring(i, m.range.first))
        val kind = LinkKind.of(m.groupValues[1])
        val nth = used.getOrDefault(kind, 0)
        used[kind] = nth + 1
        out += ProsePart.Link(kind, m.groupValues[2], links.filter { it.kind == kind }.getOrNull(nth))
        i = m.range.last + 1
    }
    if (i < paragraph.length) out += ProsePart.Plain(paragraph.substring(i))
    return out
}

/** Prose with the markers removed, for search and previews. */
fun plainProse(paragraph: String) = paragraph.replace(Regex("\\[[a-z]+\\|([^\\]]+)]"), "$1")
