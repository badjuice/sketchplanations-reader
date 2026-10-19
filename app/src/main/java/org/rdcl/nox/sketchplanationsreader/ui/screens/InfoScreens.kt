package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.rdcl.nox.sketchplanationsreader.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.ui.components.CardPill
import org.rdcl.nox.sketchplanationsreader.ui.components.Hairline
import org.rdcl.nox.sketchplanationsreader.ui.components.InkRule
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalTablet
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.PressRow
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.readMeasure
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.hand
import org.rdcl.nox.sketchplanationsreader.ui.theme.heading
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

private class Section(val key: String, val title: String, val paras: List<String>, val links: List<Pair<String, String>>)

private const val REPO = "https://github.com/badjuice/sketchplanations-reader"
private const val REPORT = "$REPO/issues"

/** All credits in one place. The podcast hosts are credited here and nowhere else. */
private val sections = listOf(
    Section("what", "What this app is", listOf(
        "An unofficial reader for Sketchplanations. It fetches the sketches and their words, organises them for a phone, and keeps whatever you choose available offline.",
        "That is the whole job. No account, no feed, no ads. It reads what the site already publishes."), emptyList()),
    Section("who", "Who made the sketches", listOf(
        "Every sketch, and every word explaining it, is by Jono Hey. He has drawn one a week since 2011 and publishes them free on sketchplanations.com.",
        "The podcast is his too, made with Rob Bell and Tom Pellereau."),
        listOf("sketchplanations.com" to "https://sketchplanations.com", "The podcast" to "https://podcast.sketchplanations.com", "Buy Jono a coffee" to "https://www.buymeacoffee.com/sketchplanator")),
    Section("indie", "Not affiliated", listOf(
        "This app is built and maintained independently. It is not made, endorsed or supported by Jono Hey or anyone connected to Sketchplanations.",
        "Anything wrong with the app is the app’s fault, so please report it here rather than to him. Anything wrong with a sketch is best raised on the website."),
        listOf("Report an app problem" to REPORT)),
    Section("app", "Who made this app", listOf(
        "Built and maintained by BadJuice, independently. Not a company, not a product, no analytics, no account.",
        "If something in the app is broken it is the app’s to fix. If a sketch is wrong, that is one for the website."),
        listOf("BadJuice on GitHub" to "https://github.com/badjuice", "Report an app problem" to REPORT, "App source" to REPO)),
    Section("credits", "Credits & licence", listOf(
        "Sketches and words are Creative Commons BY-NC 4.0. Anything you share or save from this app carries its credit line (Jono Hey, plus the address of that sketch on the site) because the licence asks for it.",
        "Sketches, words and podcast © Jono Hey and the show’s hosts, used as published.",
        "The app’s own code is free software under the GPL-3.0. Interface debt owed to the xkcd Android reader by zjn0505.",
        "Typefaces: Nunito, Patrick Hand, Space Mono, Lora and Karla, all under the SIL Open Font License."),
        listOf("Sketch licence (CC BY-NC 4.0)" to "https://sketchplanations.com/licence", "App licence (GPL-3.0)" to "$REPO/blob/main/LICENSE")),
)

@Composable
fun AboutScreen(m: AppModel) {
    val tablet = LocalTablet.current
    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().readMeasure(tablet)) {
                Image(
                    painterResource(R.drawable.logo_reader), contentDescription = "Sketchplanations Reader logo",
                    modifier = Modifier.width(112.dp), contentScale = ContentScale.FillWidth,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    T("A reader for Sketchplanations", heading(26, 1.2f))
                    Spacer(Modifier.height(6.dp))
                    T("Unofficial, independent, and built for reading offline. The sketches belong to Jono Hey.", sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    T("Combining 🧠 curiosity, ♥️ heart, and ✍️ art since 2013", hand(18, 1.4f, italic = true), color = Sk.colors.textSecondary)
                }
            }
        }
        InkRule()

        // Accordion: one section open at a time.
        sections.forEach { s ->
            val open = m.acc == s.key
            Column(Modifier.fillMaxWidth().readMeasure(tablet)) {
                PressRow({ m.acc = if (open) "" else s.key }, Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        T(s.title, sans(17, 700), Modifier.weight(1f))
                        Box(Modifier.size(28.dp).clip(CircleShape).border(2.dp, Sk.colors.borderDefault, CircleShape), contentAlignment = Alignment.Center) {
                            T(if (open) "–" else "+", sans(16, 700, 1f))
                        }
                    }
                }
                AnimatedVisibility(open, enter = expandVertically(tween(Sk.motion.base)) + fadeIn(tween(Sk.motion.base)), exit = shrinkVertically(tween(Sk.motion.base)) + fadeOut(tween(Sk.motion.fast))) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp)) {
                        s.paras.forEach { T(it, sans(16, 400, 1.55f)); Spacer(Modifier.height(14.dp)) }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            s.links.forEach { (name, url) -> CardPill(name, { m.openUrl(url) }, trailing = Lucide.External, size = 15, trailingSize = 14.dp) }
                        }
                    }
                }
                Hairline()
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}
