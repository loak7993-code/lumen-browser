# Lumen Browser

A lightweight, privacy-focused Android browser built with Kotlin + WebView.

## Features

- **Tabs** — multi-tab browsing with tab management
- **Ad blocker** — ad host blocklist + URL path heuristics
- **Tracker blocker** — tracker host blocklist
- **WebRTC disable** — prevents IP leaks through VPNs
- **Lumen Search** — custom meta-search engine aggregating DuckDuckGo, Bing, and Wikipedia
- **Search suggestions** — live autocomplete
- **Tabs** — web/images/videos/news search tabs
- **Bookmarks & History** — stored as JSON
- **Find in page** — injected JS find bar
- **Dark mode** — system or forced
- **Downloads** — in-app download manager
- **Extension support** — Firefox .xpi content-script injection
- **Home-screen widget** — quick search widget

## Permissions

Only 3:
- `INTERNET` — load web pages
- `ACCESS_NETWORK_STATE` — check connectivity
- `DOWNLOAD_WITHOUT_NOTIFICATION` — save downloaded files

No tracking, no analytics, no background services.

## Build

```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17, Android SDK (targetSdk 35, minSdk 26).
