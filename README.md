# Sketchplanations — Android reader

An unofficial reader for [sketchplanations.com](https://sketchplanations.com) and its podcast, built in Kotlin and
Jetpack Compose from the Claude Design handoff (`Sketchplanations App.dc.html` + the Sketchplanations Browser
Design System).

```
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug         # onto a connected device or emulator
```

Needs JDK 17+ and the Android SDK (compileSdk 34, minSdk 26).

## Requirements

- **Works fully offline.** Everything downloaded (sketch images and words, podcast episodes, favourites, the
  catalogue index, settings) opens with no connection. Nothing on launch or navigation may block on the
  network. Telling the user they are offline is optional; when something genuinely needs the network (an
  undownloaded sketch, a stream), say so and offer the fix instead of failing silently.
- Phone first, tablet layout included. iOS is out of scope for now.

## Releasing

Commits follow [Conventional Commits](https://www.conventionalcommits.org) (`feat:`, `fix:`, `refactor:`,
`docs:`, `chore:`, `build:`). To cut a release, bump `versionName`/`versionCode` in `app/build.gradle.kts`, then:

```
npx git-cliff@2.6.1 --tag vX.Y.Z -o CHANGELOG.md
git commit -am "chore(release): X.Y.Z" && git tag -a vX.Y.Z -m "X.Y.Z"
```

## Layout

| Path | What |
| --- | --- |
| `ui/theme/` | Design tokens: light/dark colours, the never-inverting artwork constants, fonts and typeface packs, motion |
| `ui/components/` | Lucide icons, ink buttons with hard offset shadows, chips, toggles, rows, long-press tooltips |
| `ui/App.kt` | Shell: app bar, bottom tabs, drawer (phone) / pinned rail (tablet, sw600dp), mini player, overlays |
| `ui/screens/` | Every screen from the prototype |
| `state/AppModel.kt` | Navigation stack, search debounce, downloads, player, settings actions |
| `data/` | Models, stand-in catalogue, DataStore persistence, connectivity (Wi-Fi / mobile / offline) |

## What is real and what is a stand-in

Real: navigation and history stack, theme, typeface packs, reduce motion, larger text, connectivity states,
search with debounce, archive sort/filter/jump rail, favourites and the three save lists (persisted), sharing
with the CC BY-NC credit line, external links (Custom Tabs or browser), device storage figures, folder picker.

Stand-ins, carried over from the prototype: the catalogue (24 sketches, 5 episodes) and all prose; artwork is
cream placeholder blocks; downloads, catalogue sync and playback are simulated timers — there is no network
client or audio yet. The data source is the site's public Prismic CMS plus the Acast RSS feed; see
`SketchplanationsExplode.md` in the design project before writing that layer.

## Licences

**App code:** GPL-3.0-only, see `LICENSE`. Copyright © 2026 BadJuice ([github.com/badjuice](https://github.com/badjuice)).

**Fonts:** SIL Open Font License 1.1, texts in `licenses/fonts/`.

**Content is not part of this program and is not covered by the GPL.**
- Sketches and their words are © Jono Hey under
  [CC BY-NC 4.0](https://sketchplanations.com/licence). The app may show, cache and share them as long as it
  credits the author and is never sold or monetised (no paid tier, no ads). The credit line the site asks for is
  `Image: Jono Hey, Sketchplanations` plus a link to the sketch's page. The author also asks that the
  Sketchplanations logo is not removed from the sketches, so full views must show the uncropped image.
- The podcast (audio and show notes) carries no open licence: "© Sketchplanations - The Podcast". The app
  streams and downloads episodes from the public feed the way any podcast player does, but must not re-host or
  redistribute them.
- Never commit scraped or downloaded content to this repository. Content is fetched into the app (or a local
  cache) at run time, so the GPL code and the CC BY-NC content stay separate works.
