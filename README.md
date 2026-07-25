# Lumen Browser

A lightweight, privacy-focused browser with ad/tracker blocking, WebRTC protection, and a custom meta-search engine.

## Versions

### Android (`/android`)
- Kotlin + WebView, ViewBinding
- targetSdk 35, minSdk 26
- Tabs, bookmarks, history, downloads, find-in-page, dark mode
- Ad/tracker blocker, WebRTC blocker
- Lumen Search (meta-search: DDG + Bing + Wikipedia)
- Extension support (.xpi content-script injection)
- Home-screen search widget

### Debian Bookworm (`/debian`)
- Python 3 + PyQt6 + QtWebEngine
- Multi-file package with .deb builder
- Tabs with suspension, bookmarks, history, settings
- Ad/tracker blocker, WebRTC blocker
- Lumen Search, extension support, find-in-page

### Alpine (`/alpine`)
- Single-file Python (719 lines) + PyQt6
- Stripped for minimal size/resource usage
- Same features, less code
- Dockerfile + APKBUILD + install.sh

## Install

### Android
```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Debian
```bash
cd debian
./build-deb.sh
sudo dpkg -i lumen_1.0.0_all.deb
# Or: ./install.sh (user install, no sudo)
```

### Alpine
```bash
cd alpine
docker build -t lumen-alpine .
# Or: sh install.sh
```

## Features

- **Ad blocker** — 300+ ad host blocklist + URL path heuristics
- **Tracker blocker** — 400+ tracker host blocklist
- **WebRTC disable** — prevents IP leaks through VPNs
- **Lumen Search** — custom meta-search engine aggregating DuckDuckGo, Bing, and Wikipedia
- **Tabs** — with background tab suspension for low memory
- **Bookmarks & History** — stored as JSON
- **Find in page** — injected JS find bar
- **Dark mode** — system or forced
- **Extension support** — Firefox .xpi content-script injection
- **Low resource mode** — single-process, GPU disabled, small cache, tab limit

## Permissions (Android)

Only 3:
- `INTERNET` — load web pages
- `ACCESS_NETWORK_STATE` — check connectivity
- `DOWNLOAD_WITHOUT_NOTIFICATION` — save downloaded files

No tracking, no analytics, no background services.
