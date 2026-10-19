# Changelog

Generated from the commit history by [git-cliff](https://git-cliff.org). Versions follow
[Semantic Versioning](https://semver.org). 0.1.0 was the interactive prototype in Claude Design;
0.2.0 is the first native build.

## 0.7.0 (2026-10-19)

### Added

- Resume episodes and mark read sketches (f07fd48)
- **share:** Share the picture with its credit (4410881)

## 0.6.0 (2026-10-19)

### Added

- **offline:** Bulk downloads run in the background (54ceaa8)
- **viewer:** Pinch zoom, swipe to close, save any image (e35fd86)
- **nav:** Keep scroll position when going back (7b933c6)
- **sketch:** Roomier link badges and long press for the address (29146fd)
- **settings:** Each screen's settings in its top bar (3413225)
- **ui:** Motion for presses, progress, tabs and screens (c1e77b6)

### Fixed

- **offline:** Start resumes only the paused job of the same kind (93630d4)

### Changed

- Drop pages that copy the website (415fa3d)

### Documentation

- Shorten the README (cc437ff)

## 0.5.0 (2026-10-19)

### Added

- Use the Sketchplanations Reader logo as the app icon (39dedac)
- **nav:** Four tabs and a menu instead of a side drawer (e7f8dff)
- **archive:** Topics inside Archive, swipe between sketches (fa17457)
- **library:** One library for sketches and episodes (600dfcc)
- **sketch:** Three actions plus older and newer buttons (4b11e81)
- **settings:** Six groups and no dead toggles (38a56c2)

### Fixed

- **search:** Restore the site's searching message verbatim (5f12d60)

### Documentation

- Add the logo (78d542f)
- Plain wording without em dashes (ff3c2b4)
- Regenerate the changelog with the new heading format (384e197)
- Shorten the README (179685c)

## 0.4.0 (2026-10-18)

### Added

- **podcast:** Real playback with lock-screen controls (054e89e)
- Real downloads for sketches, images and episodes (fc3cae9)

### Documentation

- Document building and publishing release APKs (cba70f7)
- The app no longer simulates anything (23b056c)

### Maintenance

- Per-architecture release APKs and a private release key (6ee2d55)

## 0.3.0 (2026-10-18)

### Added

- **settings:** Let thumbnails show the whole sketch (7fdd885)
- **sync:** Store the real catalogue on the device (ba04c7c)
- Show the real archive on every screen (05860e1)
- **topics:** Capitalise topic names (8032b81)
- **search:** Rank title matches first (f2043bb)

### Fixed

- **about:** Credit the app to BadJuice and link the GitHub profile (009fd87)

### Documentation

- Record the offline requirement and the release steps (05da11c)
- Describe what is real now that the catalogue is (32920cd)

### Maintenance

- Name the exact commit in debug version names (3e3f320)
- Include SketchplanationsExplode as a submodule (628369e)

## 0.2.0 (2026-10-18)

### Added

- Initial Android reader built from the Claude Design handoff (e16477b)

### Fixed

- **about:** Credit Nox Fortes and point app reports at the app's repo (4d8612c)

### Changed

- Move to the org.rdcl.nox.sketchplanationsreader namespace (2a1f1f9)

### Maintenance

- License the app code under GPL-3.0 (e80ba69)
- Generate the changelog from Conventional Commits (659f295)

