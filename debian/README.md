# Lumen Browser - Debian Bookworm

A lightweight, private web browser built with Python + PyQt6 + QtWebEngine.
Designed to use minimal RAM and CPU.

## Requirements

- Debian 12 (Bookworm) or compatible
- Python 3.11+ (pre-installed on Bookworm)
- `python3-pyqt6` and `python3-pyqt6.qtwebengine` from apt

## Install

### Option 1: .deb package (recommended)
```bash
./build-deb.sh
sudo dpkg -i lumen_1.0.0_all.deb
```
Then launch from your app menu or run `lumen` in terminal.

### Option 2: User install (no sudo)
```bash
./install.sh
```
Installs to `~/.local/bin/lumen` and `~/.local/share/lumen/`.

## Features

- **Ad blocker** - 90+ ad host blocklist + URL path heuristics
- **Tracker blocker** - 70+ tracker host blocklist
- **WebRTC disable** - prevents IP leaks through VPNs
- **Lumen Search** - custom meta-search engine (web/images/videos/news) aggregating DuckDuckGo, Bing, and Wikipedia
- **Search suggestions** - live autocomplete
- **Tabs** - with background tab suspension for low memory
- **Bookmarks & History** - stored as JSON
- **Find in page** - injected JS find bar
- **Dark mode** - system or forced
- **Extension support** - Firefox .xpi content-script injection
- **Low resource mode** - single-process, GPU disabled, 10MB cache, tab limit

## Low Resource Optimizations

| Optimization | Effect |
|---|---|
| Single-process mode | ~100-200MB less RAM |
| GPU disabled | No GPU overhead |
| Renderer limit = 1 | Max 1 renderer process |
| Tab suspension | Background tabs paused |
| 10MB cache cap | Minimal disk usage |
| Periodic GC | Every 30s |
| Background networking off | No background fetch |
| Low-end device mode | All Chromium low-memory flags |

## Uninstall

```bash
# If installed via .deb:
sudo dpkg -r lumen

# If installed via install.sh:
rm -rf ~/.local/share/lumen ~/.local/bin/lumen ~/.local/share/applications/lumen.desktop
```
