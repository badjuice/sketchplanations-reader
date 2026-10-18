package org.rdcl.nox.sketchplanationsreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.rdcl.nox.sketchplanationsreader.data.Catalogue
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.Screen
import org.rdcl.nox.sketchplanationsreader.ui.components.ArtBlock
import org.rdcl.nox.sketchplanationsreader.ui.components.CardPill
import org.rdcl.nox.sketchplanationsreader.ui.components.Hairline
import org.rdcl.nox.sketchplanationsreader.ui.components.Icon
import org.rdcl.nox.sketchplanationsreader.ui.components.InkCard
import org.rdcl.nox.sketchplanationsreader.ui.components.InkRule
import org.rdcl.nox.sketchplanationsreader.ui.components.LocalTablet
import org.rdcl.nox.sketchplanationsreader.ui.components.Lucide
import org.rdcl.nox.sketchplanationsreader.ui.components.Pill
import org.rdcl.nox.sketchplanationsreader.ui.components.PressRow
import org.rdcl.nox.sketchplanationsreader.ui.components.PrimaryButton
import org.rdcl.nox.sketchplanationsreader.ui.components.T
import org.rdcl.nox.sketchplanationsreader.ui.components.noRippleClick
import org.rdcl.nox.sketchplanationsreader.ui.components.readMeasure
import org.rdcl.nox.sketchplanationsreader.ui.theme.Art
import org.rdcl.nox.sketchplanationsreader.ui.theme.Shadow
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk
import org.rdcl.nox.sketchplanationsreader.ui.theme.hand
import org.rdcl.nox.sketchplanationsreader.ui.theme.heading
import org.rdcl.nox.sketchplanationsreader.ui.theme.sans

private class Section(val key: String, val title: String, val paras: List<String>, val links: List<Pair<String, String>>)

private const val REPORT = "https://forms.gle/c6LymLW5vLx9FUeW6"

/** All credits, one place. The podcast hosts are credited once — here. */
private val sections = listOf(
    Section("what", "What this app is", listOf(
        "An unofficial reader for Sketchplanations. It fetches the sketches and their words, organises them for a phone, and keeps whatever you download available offline.",
        "That is the whole job. No account, no feed, no ads — just a nicer way to read work that already exists on the web."), emptyList()),
    Section("who", "Who made the sketches", listOf(
        "Every sketch, and every word explaining it, is by Jono Hey. He has drawn one a week since 2011 and publishes them free on sketchplanations.com.",
        "The podcast is his too, made with Rob Bell and Tom Pellereau."),
        listOf("sketchplanations.com" to "https://sketchplanations.com", "The podcast" to "https://podcast.sketchplanations.com")),
    Section("indie", "Not affiliated", listOf(
        "This app is built and maintained independently. It is not made, endorsed or supported by Jono Hey or anyone connected to Sketchplanations.",
        "Anything wrong with the app is the app’s fault — please report it here rather than to him. Anything wrong with a sketch is best raised on the website."),
        listOf("Report an app problem" to REPORT)),
    Section("hosts", "The podcast hosts", listOf(
        "Jono Hey draws the sketches and brings each one to the table.",
        "Rob Bell asks the awkward question — the one that finds the edge of the idea.",
        "Tom Pellereau brings the example from ordinary life that makes it land.",
        "All three make the show independently of this app."),
        listOf("The podcast" to "https://podcast.sketchplanations.com", "Follow the show" to "https://podcast.sketchplanations.com/follow/")),
    Section("app", "Who made this app", listOf(
        "Built and maintained independently, mostly for personal use on the train. Not a company, not a product, no analytics, no account.",
        "If something in the app is broken it is the app’s to fix. If a sketch is wrong, that is one for the website."),
        listOf("Report an app problem" to REPORT)),
    Section("support", "Supporting the work", listOf(
        "If the sketches are useful to you, support them at the source — a paid Substack subscription, a coffee, the Redbubble shop or the book. Nothing you spend there passes through this app, and this app takes nothing."),
        emptyList()),
    Section("credits", "Credits & licence", listOf(
        "Sketches and words are Creative Commons BY-NC 4.0. Anything you share or save from this app carries its credit line — Jono Hey, plus the address of that sketch on the site — because the licence asks for it.",
        "Sketches, words and podcast © Jono Hey and the show’s hosts, used as published.",
        "App shell open source. Interface debt owed to the xkcd Android reader by zjn0505."),
        listOf("Licence" to "https://sketchplanations.com/licence")),
)

@Composable
fun AboutScreen(m: AppModel) {
    val tablet = LocalTablet.current
    var email by rememberSaveable { mutableStateOf("") }
    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().readMeasure(tablet)) {
                InkCard(Modifier.size(92.dp, 108.dp), bg = Art.paper, shadow = Shadow.md, radius = 12.dp) {}
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    T("A reader for Sketchplanations", heading(26, 1.2f))
                    Spacer(Modifier.height(6.dp))
                    T("Unofficial, independent, and built for reading offline. The sketches belong to Jono Hey.", sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    T("Combining 🧠 curiosity, ♥️ heart, and ✍️ art since 2013", hand(18, 1.4f, italic = true), color = Sk.colors.textSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
            CardPill("Support the author", { m.go(Screen.Prints) }, Modifier.readMeasure(tablet), size = 15)
        }
        InkRule()

        // Accordion — one section open at a time.
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
                            if (s.key == "support") CardPill("Ways to support", { m.go(Screen.Prints) }, size = 15)
                        }
                    }
                }
                Hairline()
            }
        }

        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 28.dp)) {
            InkCard(Modifier.fillMaxWidth(), shadow = Shadow.md, shadowColor = Sk.colors.penYellow) {
                Column(Modifier.padding(22.dp)) {
                    T("Get a sketch in your inbox", sans(23, 800, 1.25f))
                    Spacer(Modifier.height(6.dp))
                    T("Jono's own weekly newsletter, straight from the website. This just hands your address to his signup — nothing is stored here.", sans(15, 400, 1.5f), color = Sk.colors.textSecondary)
                    Spacer(Modifier.height(16.dp))
                    Field(null, email, { email = it }, "you@example.com", KeyboardType.Email)
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Sign up", { m.subscribe(email) }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    T("the sketches are free — support them at the source ✎", hand(18, 1.4f), color = Sk.colors.textSecondary)
                }
            }
        }
    }
}

// ---- Support the author -------------------------------------------------------------------

@Composable
fun PrintsScreen(m: AppModel) {
    val tablet = LocalTablet.current
    ScreenScroll(m, align = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().readMeasure(tablet).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)) {
            InkCard(Modifier.fillMaxWidth().padding(bottom = 22.dp), bg = Sk.colors.warningWash) {
                Column(Modifier.padding(16.dp)) {
                    T("Everything here happens on the website", sans(16, 800))
                    Spacer(Modifier.height(6.dp))
                    T("This app sells nothing and takes nothing. Every link below leaves for Substack, Buy Me a Coffee or Redbubble — the author's own pages, where the money actually reaches him.", sans(14, 400, 1.5f), color = Sk.colors.textSecondary)
                }
            }
            T("The sketches are free and stay free. If they have been useful, these are the ways the author has said you can help — in his order.", sans(17, 400, 1.5f), Modifier.padding(bottom = 24.dp))

            SupportBlock("The newsletter", "A sketch a week by email, free. A paid subscription is the thing Jono says makes the biggest difference.") {
                CardPill("Subscribe on Substack", { m.openUrl("https://sketchplanations.substack.com/subscribe") }, trailing = Lucide.External, size = 15, trailingSize = 14.dp)
            }
            SupportBlock("A coffee", "A one-off tip. No account, no subscription, no recurring anything.") {
                CardPill("Buy Me a Coffee", { m.openUrl("https://www.buymeacoffee.com/sketchplanator") }, trailing = Lucide.External, size = 15, trailingSize = 14.dp)
            }
            T("Prints and shirts", sans(21, 800, 1.3f), Modifier.padding(bottom = 12.dp))
            InkCard(Modifier.fillMaxWidth().height(170.dp).padding(bottom = 0.dp), bg = Art.paper, shadow = Shadow.md, shadowColor = Sk.colors.shadowInk) {
                Box(Modifier.fillMaxWidth().height(170.dp).padding(14.dp), contentAlignment = Alignment.BottomStart) { T("most sketches can be printed ✎", hand(17), color = Art.caption) }
            }
            Spacer(Modifier.height(14.dp))
            T("Sold through Redbubble rather than by Jono directly. Sizes, stock and prices are set there, so they are not mirrored here where they could go stale.", sans(15, 400, 1.55f), Modifier.padding(bottom = 14.dp), Sk.colors.textSecondary)
            CardPill("Open the Redbubble shop", { m.openUrl("https://www.redbubble.com/people/sketchplanator/explore?asc=u&page=1&sortOrder=top%20selling") }, trailing = Lucide.External, size = 15, trailingSize = 14.dp)
            Spacer(Modifier.height(26.dp))
            InkRule(); Spacer(Modifier.height(18.dp))

            T("The book", sans(21, 800, 1.3f), Modifier.padding(bottom = 12.dp))
            InkCard(Modifier.fillMaxWidth(), shadow = Shadow.md) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtBlock(Modifier.size(76.dp, 100.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        T("Big Ideas Little Pictures", sans(18, 800, 1.25f))
                        Spacer(Modifier.height(4.dp))
                        T("A collection of the best of them, arranged so one idea leads into the next.", sans(14, 400, 1.5f), color = Sk.colors.textSecondary)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.noRippleClick({ m.openUrl("https://geni.us/big-ideas-book") }).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                T("Find the book", sans(15, 700), color = Sk.colors.textLink)
                                Spacer(Modifier.width(6.dp))
                                Icon(Lucide.External, Sk.colors.textLink, 14.dp, stroke = 2.2f)
                            }
                            Spacer(Modifier.width(8.dp))
                            // Affiliate links must be badged as affiliate.
                            T("AFFILIATE", sans(11, 600, tracking = .04f), Modifier.clip(Pill).border(1.dp, Sk.colors.borderQuiet, Pill).padding(horizontal = 8.dp, vertical = 2.dp), Sk.colors.textTertiary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            InkRule(); Spacer(Modifier.height(18.dp))

            T("Free, and still worth a lot", sans(21, 800, 1.3f), Modifier.padding(bottom = 8.dp))
            T("passing one on is the oldest kind of support ✎", hand(18, 1.4f), Modifier.padding(bottom = 14.dp), Sk.colors.textSecondary)
            CardPill("Send a sketch to someone", { m.share(Catalogue.byDate[0]) }, trailing = Lucide.Share, size = 15, trailingSize = 16.dp)
            Spacer(Modifier.height(22.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, Sk.colors.borderQuiet, RoundedCornerShape(14.dp)).padding(horizontal = 15.dp, vertical = 13.dp)) {
                val note = buildAnnotatedString {
                    append("The sketches are Creative Commons BY-NC. Share them freely, credit Jono, don't sell them — this app adds the credit line for you whenever you share or save an image. ")
                    withStyle(SpanStyle(color = Sk.colors.textLink, fontWeight = FontWeight.Bold)) { append("Read the licence ↗") }
                }
                androidx.compose.foundation.text.BasicText(
                    note, Modifier.noRippleClick({ m.openUrl("https://sketchplanations.com/licence") }),
                    style = sans(13, 400, 1.55f).copy(color = Sk.colors.textTertiary),
                )
            }
        }
    }
}

@Composable
private fun SupportBlock(title: String, body: String, action: @Composable () -> Unit) {
    T(title, sans(21, 800, 1.3f), Modifier.padding(bottom = 12.dp))
    T(body, sans(15, 400, 1.55f), Modifier.padding(bottom = 14.dp), Sk.colors.textSecondary)
    action()
    Spacer(Modifier.height(26.dp))
    InkRule()
    Spacer(Modifier.height(18.dp))
}
