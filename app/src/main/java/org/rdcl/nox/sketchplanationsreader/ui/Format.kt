package org.rdcl.nox.sketchplanationsreader.ui

import org.rdcl.nox.sketchplanations.explode.Episode
import org.rdcl.nox.sketchplanations.explode.SketchImage
import org.rdcl.nox.sketchplanationsreader.data.db.SketchSummary
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val day = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK).withZone(ZoneOffset.UTC)

fun formatDate(epochMillis: Long): String = day.format(Instant.ofEpochMilli(epochMillis))

/** Sketches are numbered in publication order; the site stores no number of its own. */
val SketchSummary.num get() = "#$ordinal"
val SketchSummary.date get() = formatDate(publishedAt)
val SketchSummary.year get() = date.takeLast(4)
val SketchSummary.image get() = SketchImage(imageUrl, imageAlt, null, null)

val Episode.dateLabel get() = publishedAt?.let { formatDate(it.toEpochMilli()) }.orEmpty()
val Episode.lengthLabel get() = durationSeconds?.let { "${(it + 30) / 60} min" }.orEmpty()
/** Numbers restart each season, so both are shown. */
val Episode.code get() = listOfNotNull(season?.let { "S$it" }, number?.let { "EP $it" }).joinToString(" · ")
