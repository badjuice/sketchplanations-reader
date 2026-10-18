package org.rdcl.nox.sketchplanationsreader.data

import java.time.LocalDate

/**
 * Stand-in catalogue, carried over verbatim from the design prototype. Titles, dates and
 * tags have the right shape; prose and episode notes are placeholders written to length.
 * The real source is the site's public Prismic CMS — see SketchplanationsExplode.md.
 */
object Catalogue {
    /** Size of the real archive, used by first-run sync and bulk-download estimates. */
    const val TOTAL_POSTS = 972
    const val TOTAL_EPISODES = 59

    private fun s(n: Int, t: String, d: String, tags: List<String>, lead: String, vararg body: String) =
        Sketch(n, t, LocalDate.parse(d), tags, lead, body.toList())

    val sketches: List<Sketch> = listOf(
        s(589, "The Diderot Effect", "2026-07-26", listOf("Behaviour", "Money"), "One new thing quietly pulls the next one in behind it.",
            "Diderot got a beautiful new dressing gown and suddenly his desk looked shabby, then his chair, then the rug. Piece by piece he replaced a room he had been perfectly happy in.",
            "The same pull drives [book|Trading Up], and it is a close cousin of [sketch|The Hedonic Treadmill] — each new level quietly becomes the floor.",
            "The pull is real, but it is not compulsory. Naming it out loud is usually enough to stop the chain at one purchase. We took it apart properly on [pod|Episode 48]."),
        s(585, "Chesterton's Fence", "2026-07-19", listOf("Thinking", "Work"), "Before you tear down the fence, find out why it was put there.",
            "Coming fresh to something, the useless-looking rule is the first thing you want gone. But somebody built it, and they had a reason — possibly a very good one that nobody wrote down. [web|Chesterton put it in an essay] in 1929 and it has outlived almost everything else he wrote that year.",
            "Learn the reason first. Then remove it with confidence, or leave it alone. It pairs well with [sketch|Second-order Thinking]."),
        s(578, "Survivorship Bias", "2026-07-12", listOf("Thinking", "Data"), "The planes that came back cannot tell you where the danger is.",
            "Armour the parts of the returning bombers that were shot up, and you armour the parts a plane can survive being hit. The holes that matter are on the aircraft that never made it home — [paper|Wald wrote it up in 1943] and it took decades to become common sense.",
            "Whenever you study winners, ask who is missing from the sample. [book|Thinking, Fast and Slow] is good on why we so rarely do."),
        s(571, "Occam's Razor", "2026-07-05", listOf("Thinking"), "When explanations compete, back the one with fewest moving parts.",
            "Not the simplest answer — the one that needs the fewest assumptions to hold together. Extra assumptions are extra places to be wrong. [sketch|Hanlon’s Razor] is the same move applied to other people, and the [web|Stanford Encyclopedia] has the long version."),
        s(564, "Goodhart's Law", "2026-06-28", listOf("Work", "Data"), "When a measure becomes a target, it stops being a good measure.",
            "The number was a proxy for the thing you cared about. Pay people for the number and they will move the number, sometimes at the expense of the thing. [paper|Goodhart said it first in 1975]; [sketch|The Cobra Effect] is what it looks like when it goes badly."),
        s(551, "Bikeshedding", "2026-06-21", listOf("Work", "Behaviour"), "The trivial decision gets all the debate because everyone can have an opinion on it.",
            "Nobody argues about the reactor design; everybody has views on the colour of the bike shed. Watch for the meeting that spends fifty minutes on the cheapest item."),
        s(540, "Dunbar's Number", "2026-06-14", listOf("Behaviour"), "About 150 — the number of people you can hold a real relationship with.",
            "Beyond it, groups need rules, hierarchies and names on doors to do what a small group does by knowing each other."),
        s(533, "The Cobra Effect", "2026-06-07", listOf("Behaviour", "Money"), "A bounty on cobras produced more cobras.",
            "Pay for dead snakes and someone starts farming them. Incentives are answered exactly as written, not as intended."),
        s(522, "Hanlon's Razor", "2026-05-31", listOf("Thinking", "Behaviour"), "Never attribute to malice what is adequately explained by a bad day.",
            "Most of what feels aimed at you is someone rushing, distracted, or missing what you know."),
        s(510, "Ikigai", "2026-05-24", listOf("Work", "Health"), "A reason to get up in the morning — smaller and more everyday than a life purpose.",
            "In its home use it is not the four-circle diagram. It is the small thing worth rising for: the garden, the class, the coffee with a friend."),
        s(498, "Kintsugi", "2026-05-17", listOf("Design", "Nature"), "Repair the bowl with gold so the break becomes part of its history.",
            "Hiding a repair pretends nothing happened. Kintsugi says the crack is where the story is, and traces it in something precious."),
        s(487, "The Eisenhower Matrix", "2026-05-10", listOf("Work"), "Urgent and important are not the same axis.",
            "Everything urgent shouts. The important-not-urgent box is quiet, keeps getting postponed, and is where almost all the value sits. [book|The 7 Habits] built a whole chapter on it, and [video|Eisenhower said it plainly] years earlier."),
        s(470, "Zeigarnik Effect", "2026-04-26", listOf("Learning", "Behaviour"), "Unfinished things stay in your head; finished ones let go.",
            "A task left mid-sentence keeps knocking. Useful: stop work somewhere obviously incomplete and tomorrow you start with a running engine."),
        s(455, "Nudge Theory", "2026-04-12", listOf("Behaviour", "Design"), "Change the setting, not the person.",
            "Move the fruit to eye level, make the good option the default. No ban, no lecture — just an easier path to the better choice."),
        s(441, "The Overton Window", "2026-03-29", listOf("Thinking"), "The range of ideas it is currently acceptable to say out loud.",
            "It moves. What was unthinkable becomes radical, then sensible, then policy — usually because someone kept saying it."),
        s(430, "Rubber Duck Debugging", "2026-03-15", listOf("Tech", "Learning"), "Explain the problem out loud to something that cannot help you.",
            "Half the time the answer arrives mid-sentence. The duck does nothing; the explaining does everything."),
        s(418, "Sunk Cost Fallacy", "2026-03-01", listOf("Thinking", "Money"), "What you already spent is gone either way.",
            "The only question is whether the next hour or the next pound is worth it from here. Finishing something bad does not refund the start of it."),
        s(402, "The Curse of Knowledge", "2026-02-15", listOf("Learning", "Design"), "Once you know it, you cannot remember not knowing it.",
            "Your explanation skips the step that took you a week to see, because it is now obvious. That step is exactly the one they need."),
        s(390, "Wabi-sabi", "2026-02-01", listOf("Design", "Nature"), "Beauty in the worn, the uneven and the impermanent.",
            "The hand-thrown cup with the thumbprint, the wall that has weathered. Nothing lasts, nothing is finished, nothing is perfect."),
        s(377, "Beginner's Mind", "2026-01-18", listOf("Learning"), "In the beginner’s mind there are many possibilities; in the expert’s, few.",
            "Expertise is a set of shortcuts that stop you looking. Coming back as a beginner is how you notice what the shortcuts skip."),
        s(364, "The Feynman Technique", "2026-01-04", listOf("Learning"), "If you cannot explain it simply, you have not got it yet.",
            "Write it as if for a twelve-year-old. Every place you reach for jargon is a place you are still hiding a hole."),
        s(350, "Loss Aversion", "2025-12-14", listOf("Behaviour", "Money"), "Losing ten pounds hurts about twice as much as finding ten pounds pleases.",
            "Which is why free trials that end feel like a robbery, and why people hold losing shares far too long."),
        s(338, "Parkinson's Law", "2025-11-30", listOf("Work"), "Work expands to fill the time available for its completion.",
            "Give a one-hour job a week and it will take the week — plus the meeting about it."),
        s(325, "Anchoring", "2025-11-16", listOf("Behaviour", "Money"), "The first number you hear sets the whole conversation.",
            "It works even when the anchor is obviously arbitrary. Say your number first, or reset the frame entirely."),
    )

    private fun l(kind: LinkKind, label: String, href: String) = SketchLink(kind, label, href)

    val links: Map<Int, List<SketchLink>> = mapOf(
        589 to listOf(l(LinkKind.Book, "Trading Up — Silverstein & Fiske", "https://geni.us/trading-up"),
            l(LinkKind.Sketch, "The Hedonic Treadmill", "#s470"), l(LinkKind.Pod, "Episode 48 — The Diderot Effect", "#ep48")),
        585 to listOf(l(LinkKind.Web, "Chesterton's original essay", "https://www.chesterton.org"),
            l(LinkKind.Sketch, "Second-order Thinking", "#s441")),
        578 to listOf(l(LinkKind.Paper, "Wald, A Method of Estimating Plane Vulnerability", "https://apps.dtic.mil"),
            l(LinkKind.Book, "Thinking, Fast and Slow", "https://geni.us/thinking-fast-slow")),
        571 to listOf(l(LinkKind.Sketch, "Hanlon's Razor", "#s522"), l(LinkKind.Web, "Stanford Encyclopedia entry", "https://plato.stanford.edu")),
        564 to listOf(l(LinkKind.Paper, "Goodhart's original 1975 paper", "https://link.springer.com"),
            l(LinkKind.Sketch, "The Cobra Effect", "#s533")),
        487 to listOf(l(LinkKind.Book, "The 7 Habits of Highly Effective People", "https://geni.us/7-habits"),
            l(LinkKind.Video, "Eisenhower on urgency", "https://youtube.com")),
    )

    val topics: List<Topic> = listOf(
        "Thinking" to 84, "Behaviour" to 61, "Design" to 47, "Work" to 44, "Nature" to 38, "Money" to 31,
        "Learning" to 27, "Health" to 24, "Tech" to 19, "Language" to 16, "Cities" to 12, "Food" to 9,
    ).map { Topic(it.first, it.second) }

    private fun ch(vararg c: Pair<Int, String>) = c.map { Chapter(it.first, it.second) }

    val episodes: List<Episode> = listOf(
        Episode("The Diderot Effect", 48, "41 min", "24 Jul 2026", listOf(
            "Jono, Rob and Tom take the dressing gown apart: why one good purchase drags a whole room behind it, and what each of them has fallen for this year.",
            "Plus: the difference between an upgrade and a spiral, and a listener question about wedding budgets."),
            ch(0 to "Cold open", 180 to "What Diderot actually wrote", 720 to "The dressing gown problem", 1320 to "Upgrade or spiral?",
                1860 to "Listener question: wedding budgets", 2220 to "What we each fell for this year")),
        Episode("Chesterton's Fence", 47, "38 min", "17 Jul 2026", listOf(
            "A fence in a field, a rule in a company, a wire in a plug. Why the newcomer is the most dangerous person in the room, and the most useful.",
            "Plus: the one time Rob removed the fence and regretted it immediately."),
            ch(0 to "Cold open", 150 to "A fence in a field", 660 to "The newcomer problem", 1260 to "When Rob removed the fence",
                1920 to "Reading the room before you rebuild it")),
        Episode("Survivorship Bias", 46, "44 min", "10 Jul 2026", listOf(
            "Bullet holes, business books and why almost every success story you read is missing most of its evidence."),
            ch(0 to "Cold open", 210 to "Bullet holes and bombers", 900 to "Business books as a survivor sample", 1740 to "How to ask who is missing")),
        Episode("Goodhart's Law", 45, "36 min", "3 Jul 2026", listOf(
            "Targets, proxies and the quiet damage of measuring the wrong thing well."),
            ch(0 to "Cold open", 165 to "Measures and proxies", 780 to "Paying for the number", 1500 to "Designing a target that survives")),
        Episode("Bikeshedding", 44, "39 min", "26 Jun 2026", listOf(
            "Why the cheapest decision gets the longest meeting, and how to spot it happening to you."),
            ch(0 to "Cold open", 195 to "The colour of the bike shed", 810 to "Fifty minutes on the cheapest item", 1560 to "Naming it in the meeting")),
    )

    /** Verbatim from the site's components/SearchResults.js. */
    val searchMessages = listOf(
        "Finding some spectacular sketches…", "Sketching up a storm…", "Searching the archives…", "Let me find that for you…",
        "Flicking through the catalogue…", "Now, where did I put that sketch…", "I know it's here somewhere…", "Sifting through the vaults…",
        "Hmm, tum, tum. What did I do with that one?…", "Back in a jiffy…", "One does not simply walk into the sketch…",
        "A noble quest for the Holy Sketch…", "Just keep searching, just keep searching…", "Just one more thing…that sketch you want…",
        "Ze little grey cells are working on it, mon ami…", "This sketch ain't going to find itself…",
        "The streets are mean, but I'll find your sketch…", "The game is afoot, we'll have it soon, Watson…",
        "It's elementary, the sketch can't hide…", "Ah, the case of the missing sketch…",
        "My pipe Watson—this search may take a moment…", "No time for half measures, Watson, they need that sketch…",
    )

    /** Newest first. */
    val byDate: List<Sketch> = sketches.sortedByDescending { it.raw }

    fun sketch(n: Int) = sketches.firstOrNull { it.n == n }
    fun episodeIndex(num: Int) = episodes.indexOfFirst { it.num == num }
    fun linksFor(n: Int) = links[n].orEmpty()

    fun search(q: String): List<Sketch> {
        val needle = q.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        // Jump to a specific number: "589" or "#589".
        Regex("^#?(\\d+)$").find(needle)?.let { m -> return listOfNotNull(sketch(m.groupValues[1].toInt())) }
        return sketches.filter { ("${it.title} ${it.tags.joinToString(" ")} ${it.lead}").lowercase().contains(needle) }
    }

    fun slugOf(t: String) = t.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    /** The credit line the CC BY-NC licence asks for on every share and saved image. */
    fun credit(s: Sketch) = "${s.title} — by Jono Hey, sketchplanations.com/${slugOf(s.title)} (CC BY-NC 4.0)"
}
