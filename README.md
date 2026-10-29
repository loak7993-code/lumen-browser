# Lumen Browser

A lightweight, privacy-focused Android browser built on Chromium WebView. Brave-style ad and tracker blocking with cosmetic filtering, anti-fingerprinting, and no analytics anywhere in the app.

## Install

Download the signed APK from [Releases](https://github.com/loak7993-code/lumen-browser/releases) (latest: v1.8.7). Requires Android 8.0 (API 26) or newer. Install by opening the APK after allowing "install unknown apps" for your file manager or browser.

## Features

- **Chromium-based** — Android's WebView (powered by Chromium) under the hood
- **Network-level ad blocking** — 300+ ad host blocklist plus URL path heuristics
- **Network-level tracker blocking** — 400+ tracker host blocklist
- **Cosmetic filtering** — CSS element hiding removes ad placeholders after page load, the uBlock Origin/Brave approach
- **Anti-fingerprinting** — canvas/WebGL spoofing, navigator property normalization
- **WebRTC disabled** — prevents IP leaks through VPNs
- **HTTPS upgrade** — plain HTTP requests upgraded before they leave the device
- **Lumen Search** — meta-search aggregating DuckDuckGo, Bing, and Wikipedia
- **Search suggestions** — live autocomplete
- **Tabs** — multi-tab browsing with tab management
- **Bookmarks & history** — stored locally as JSON
- **Find in page** — injected JS find bar
- **Dark mode** — system or forced
- **Downloads** — in-app download manager
- **Extension support** — Chrome MV3 extensions with popup support
- **Home-screen widget** — quick search widget

## Permissions

Only 3:

- `INTERNET` — load web pages
- `ACCESS_NETWORK_STATE` — check connectivity
- `DOWNLOAD_WITHOUT_NOTIFICATION` — save downloaded files

No tracking, no analytics, no background services.

## Build

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17, Android SDK (compileSdk 35, minSdk 26, targetSdk 35).

## License

MIT.
