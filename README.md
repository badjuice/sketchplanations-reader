<p align="center"><img src="docs/logo.png" width="320" alt="Sketchplanations Reader logo"></p>

# Sketchplanations reader for Android

Unofficial offline reader for [sketchplanations.com](https://sketchplanations.com) and its podcast.
APKs are in [Releases](https://github.com/badjuice/sketchplanations-reader/releases).

## Build

```
git clone --recurse-submodules https://github.com/badjuice/sketchplanations-reader
./gradlew :app:assembleDebug
```

Needs JDK 17+ and Android SDK 34. Data comes from the
[SketchplanationsExplode](https://github.com/badjuice/SketchplanationsExplode) submodule.

## Release

```
npx git-cliff@2.6.1 --tag vX.Y.Z -o CHANGELOG.md
./gradlew :app:assembleRelease
gh release create vX.Y.Z app/build/outputs/apk/release/*.apk
```

The signing key lives in `~/.gradle/gradle.properties`, outside the repo.

## Licences

Code GPL-3.0, © 2026 [BadJuice](https://github.com/badjuice); fonts SIL OFL 1.1. Sketches © Jono Hey,
[CC BY-NC 4.0](https://sketchplanations.com/licence). Podcast © Sketchplanations - The Podcast, streamed from
its public feed. No scraped content is stored here.
