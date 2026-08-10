# Cairn

> A **cairn** is the stack of rocks that marks the trail and tells you other
> travelers came this way. Every Dark Rock Studios app carries one; it points
> to the rest of the family.

Cairn is the shared "About" screen for [Dark Rock Studios](https://darkrockstudios.com)
apps, as a Compose Multiplatform library: the studio identity, the current
app, the family of sibling apps, and support links — wearing the
darkrockstudios.com design language. The survey grid glows under your finger,
seams ignite as they scroll into view, the horizon foil gleams as you tilt
the phone, and opening it etches the grid over your app's screen before the
basalt floods in.

**You almost certainly don't want this library.** It exists for Dark Rock
Studios applications: the app catalog, branding, and links are baked in. The
*code* is Apache-2.0 and you're welcome to learn from or fork it for your own
studio's about screen — but the Dark Rock Studios name, logo, and app icons
are not openly licensed (see [NOTICE](NOTICE)).

- Targets: Android, iOS (arm64 + simulator), Desktop (JVM), WebAssembly
- Zero third-party dependencies, zero permissions, zero network requests
- Design spec and interaction prototype live in the AboutDarkrock workspace

## Usage

```kotlin
dependencies {
    implementation("com.darkrockstudios:cairn:<version>")
}
```

Overlay presentation (recommended — the entrance plays over your app's own
screen). Place it as the last child of a full-screen container:

```kotlin
var aboutVisible by remember { mutableStateOf(false) }

Box(Modifier.fillMaxSize()) {
    MyAppContent(onAboutClick = { aboutVisible = true })

    CairnAboutOverlay(
        visible = aboutVisible,
        config = CairnConfig(
            currentAppId = CairnAppId.FastTrack,   // YOUR app
            versionName = BuildConfig.VERSION_NAME,
        ),
        onDismissed = { aboutVisible = false },
    )
}
```

Or as a standalone destination: `CairnAboutScreen(config, onClose = { ... })`.

### CairnConfig

| Parameter | Default | Notes |
|---|---|---|
| `currentAppId` | — | `CairnAppId`: `FastTrack`, `Hammer`, `SnapSafe`, `Fugitive`, `C2paVerify`, `CleanCopy` |
| `versionName` | — | shown in the version stamp |
| `entrance` | `Full` | `Full` (~1.1s etch/flood/ignite) · `Quick` (~0.7s) · `None` (fade). Reduced-motion settings force `None`. |
| `soundDefault` | `true` | initial sound state; the user's mute toggle persists and wins |
| `hapticsDefault` | `true` | haptic feedback on/off |
| `storeOverride` | `null` | force GET buttons to `Play`/`FDroid`/`AppStore` instead of runtime install-source detection |
| `extraLinks` | empty | extra chips in the identity block |
| `extraDebugInfo` | empty | appended to the tap-to-copy debug info |

### Behavior notes for hosts

- **Back handling**: while the overlay is visible it intercepts system back
  and plays the exit ceremony, then calls `onDismissed`.
- **Installed detection (Android)**: the library AAR ships the `<queries>`
  entries; your app inherits them via manifest merge. Nothing to add.
- **Installed detection (iOS)**: add the family URL schemes to
  `LSApplicationQueriesSchemes` in your Info.plist if/when catalog apps
  declare schemes; without it detection degrades gracefully to "not
  installed".
- **Store routing**: GET buttons link to the store *this install came from*
  (Play/F-Droid detected at runtime; Aurora and sideloads route to the app's
  canonical page). Rate only appears on Play/App Store installs. Share only
  appears where a platform share sheet exists.
- **Audio**: sonification-class output — never requests audio focus, never
  ducks the user's media, respects silent-switch contexts. Users can mute
  from the screen's footer (persisted).
- **Sensors**: accelerometer only, permissionless on both platforms, sampled
  ~30Hz strictly while the screen is visible.

## Development

```bash
./gradlew :sampleApp:run                      # desktop sample (fake Fast Track host)
./gradlew :sampleApp:wasmJsBrowserDevelopmentRun   # browser sample
./gradlew :androidApp:installDebug            # Android sample
./gradlew :cairn:desktopTest                  # tests + render previews (build/preview/*.png)
./gradlew :sampleApp:updateDemo               # rebuild the GitHub Pages demo in docs/
```

In the desktop sample: arrow keys simulate device tilt (R resets), mouse
hover drives the flashlight/gleam/sheen, and the render-preview tests write
deterministic PNGs of every effect for visual regression.

The app catalog is hand-written in `CairnCatalog.kt` for now, with field
names mirroring the website's project frontmatter so a
website→Kotlin generator can replace it mechanically.

## License

Code: [Apache-2.0](LICENSE). Brand assets (Dark Rock Studios name, summit
logo, app icons): **not** openly licensed — see [NOTICE](NOTICE). Bundled
fonts: OFL-1.1 — see [NOTICE-FONTS.md](NOTICE-FONTS.md).
