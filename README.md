# Sketchplanations — Android reader

An unofficial reader for [sketchplanations.com](https://sketchplanations.com) and its podcast, built in Kotlin and
Jetpack Compose from the Claude Design handoff (`Sketchplanations App.dc.html` + the Sketchplanations Browser
Design System).

```
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug         # onto a connected device or emulator
```

Needs JDK 17+ and the Android SDK (compileSdk 34, minSdk 26).

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
