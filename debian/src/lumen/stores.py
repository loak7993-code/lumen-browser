import json
import os
from pathlib import Path

DATA_DIR = Path(os.environ.get("XDG_DATA_HOME", os.path.expanduser("~/.local/share"))) / "lumen"
DATA_DIR.mkdir(parents=True, exist_ok=True)

SETTINGS_FILE = DATA_DIR / "settings.json"
BOOKMARKS_FILE = DATA_DIR / "bookmarks.json"
HISTORY_FILE = DATA_DIR / "history.json"
DOWNLOADS_FILE = DATA_DIR / "downloads.json"

DEFAULT_START_PAGE = "lumen://search"
DEFAULT_SETTINGS = {
    "start_page": DEFAULT_START_PAGE,
    "block_ads": True,
    "block_trackers": True,
    "block_webrtc": True,
    "load_images": True,
    "javascript_enabled": True,
    "save_history": True,
    "force_dark": False,
    "desktop_mode": False,
}


def load_settings():
    if SETTINGS_FILE.exists():
        try:
            s = json.loads(SETTINGS_FILE.read_text())
            merged = DEFAULT_SETTINGS.copy()
            merged.update(s)
            return merged
        except Exception:
            pass
    return DEFAULT_SETTINGS.copy()


def save_settings(settings):
    SETTINGS_FILE.write_text(json.dumps(settings, indent=2))


class Store:
    def __init__(self, path, max_items=500):
        self.path = path
        self.max_items = max_items
        self._items = self._load()

    def _load(self):
        if self.path.exists():
            try:
                return json.loads(self.path.read_text())
            except Exception:
                pass
        return []

    def _save(self):
        self.path.write_text(json.dumps(self._items, indent=2))

    def all(self):
        return list(reversed(self._items))

    def add(self, item):
        self._items = [i for i in self._items if i.get("url") != item.get("url")]
        self._items.append(item)
        if len(self._items) > self.max_items:
            self._items = self._items[-self.max_items:]
        self._save()

    def remove(self, url):
        self._items = [i for i in self._items if i.get("url") != url]
        self._save()

    def is_bookmarked(self, url):
        return any(i.get("url") == url for i in self._items)

    def clear(self):
        self._items = []
        self._save()
