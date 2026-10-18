# Sketchplanations — Android reader

An unofficial reader for [sketchplanations.com](https://sketchplanations.com) and its podcast, built in Kotlin and
Jetpack Compose from the Claude Design handoff (`Sketchplanations App.dc.html` + the Sketchplanations Browser
Design System).

```
git clone --recurse-submodules https://github.com/badjuice/sketchplanations-reader
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug         # onto a connected device or emulator
```

Needs JDK 17+ and the Android SDK (compileSdk 34, minSdk 26). Content comes through
[SketchplanationsExplode](https://github.com/badjuice/SketchplanationsExplode), included as the `explode`
git submodule and built from source; after pulling, run `git submodule update --init`.

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

Then build and publish the APKs (one per architecture plus a universal one):

```
./gradlew :app:assembleRelease      # app/build/outputs/apk/release/app-<abi>-release.apk
git push --follow-tags
gh release create vX.Y.Z app/build/outputs/apk/release/*.apk --notes "…"
```

Release builds are signed with a private key whose location and password live in `~/.gradle/gradle.properties`
(`SKETCH_RELEASE_STORE`, `SKETCH_RELEASE_PASSWORD`, `SKETCH_RELEASE_ALIAS`), never in the repository. Keep a
backup of the keystore: installed copies only accept updates signed with the same key. Without it, builds fall
back to the debug key.

## Layout

| Path | What |
| --- | --- |
| `ui/theme/` | Design tokens: light/dark colours, the never-inverting artwork constants, fonts and typeface packs, motion |
| `ui/components/` | Lucide icons, ink buttons with hard offset shadows, chips, toggles, rows, long-press tooltips |
| `ui/App.kt` | Shell: app bar, bottom tabs, drawer (phone) / pinned rail (tablet, sw600dp), mini player, overlays |
| `ui/screens/` | Every screen from the prototype |
| `state/AppModel.kt` | Navigation stack, search debounce, downloads, player, settings actions |
| `data/` | Room catalogue and sync (`ContentRepository`), DataStore settings and save lists, connectivity |

## What is real and what is still simulated

Real: the whole catalogue (every sketch with its words, tags, related ideas and image versions, plus the
podcast feed), fetched once through SketchplanationsExplode and stored on the device, refreshed by edit date;
images through Coil with a large disk cache; offline search over the stored words; navigation, theme,
typefaces, settings and the save lists; sharing with the licence's credit line; external links.

Still simulated: podcast playback (the player runs a clock, no audio yet) and downloads (Download post, Save
image and the bulk downloads mark items as kept but don't yet store files). Both are the next milestones.

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
