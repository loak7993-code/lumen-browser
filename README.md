# Lumen Browser

A lightweight, privacy-focused Android browser built on Chromium (Android WebView) — Brave-style ad/tracker blocking with cosmetic filtering and anti-fingerprinting.

## Features

- **Chromium-based** — uses Android's WebView (powered by Chromium)
- **Network-level ad blocking** — 300+ ad host blocklist + URL path heuristics
- **Network-level tracker blocking** — 400+ tracker host blocklist
- **Cosmetic filtering** — CSS element hiding (removes ad placeholders after page loads, like uBlock Origin/Brave)
- **Anti-fingerprinting** — canvas/WebGL spoofing, navigator property normalization
- **WebRTC disable** — prevents IP leaks through VPNs
- **HTTPS upgrade** — HTTP requests upgraded to HTTPS
- **Lumen Search** — custom meta-search engine aggregating DuckDuckGo, Bing, and Wikipedia
- **Search suggestions** — live autocomplete
- **Tabs** — multi-tab browsing with tab management
- **Bookmarks & History** — stored as JSON
- **Find in page** — injected JS find bar
- **Dark mode** — system or forced
- **Downloads** — in-app download manager
- **Extension support** — Chrome MV3 extension loading with popup support
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

Requirements: JDK 17, Android SDK (targetSdk 35, minSdk 26).
