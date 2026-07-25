import json
import os
import re
import time
import zipfile
import shutil
from pathlib import Path

from .stores import DATA_DIR

EXT_DIR = DATA_DIR / "extensions"
EXT_DIR.mkdir(parents=True, exist_ok=True)

ENABLED_FILE = EXT_DIR / "enabled.json"


def _load_enabled():
    if ENABLED_FILE.exists():
        try:
            return set(json.loads(ENABLED_FILE.read_text()))
        except Exception:
            pass
    return set()


def _save_enabled(ids):
    ENABLED_FILE.write_text(json.dumps(list(ids)))


class MatchPattern:
    def __init__(self, pattern):
        self.pattern = pattern
        parts = pattern.split("://", 1)
        if len(parts) != 2:
            raise ValueError("Invalid match pattern: " + pattern)
        self.scheme = parts[0]
        host_path = parts[1].split("/", 1)
        self.host = host_path[0]
        self.path = "/" + (host_path[1] if len(host_path) > 1 else "")

    def matches(self, url):
        try:
            from urllib.parse import urlparse
            u = urlparse(url)
        except Exception:
            return False
        url_scheme = (u.scheme or "").lower()
        url_host = (u.hostname or "").lower()
        url_path = u.path or "/"

        if self.scheme != "*" and url_scheme != self.scheme:
            return False
        if url_scheme == "file" and self.scheme != "file":
            return False

        if self.host == "*":
            return True
        if self.host.startswith("*."):
            base = self.host[2:]
            if url_host == base or url_host.endswith("." + base):
                return True
            return False
        if url_host != self.host:
            return False

        return self._match_path(self.path, url_path)

    def _match_path(self, pattern_path, url_path):
        if pattern_path == "/" or not pattern_path:
            return True
        regex_str = re.escape(pattern_path).replace(r"\*", ".*")
        if re.match(regex_str, url_path, re.IGNORECASE):
            return True
        return url_path.startswith(pattern_path)


class ContentScript:
    def __init__(self, matches, exclude_matches, js_files, css_files, run_at, all_frames):
        self.matches = matches
        self.exclude_matches = exclude_matches
        self.js_files = js_files
        self.css_files = css_files
        self.run_at = run_at
        self.all_frames = all_frames

    def applies_to(self, url, run_at):
        if run_at and self.run_at and self.run_at != run_at:
            return False
        if not any(m.matches(url) for m in self.matches):
            return False
        if any(m.matches(url) for m in self.exclude_matches):
            return False
        return True


class Extension:
    def __init__(self, ext_id, name, version, description, content_scripts, enabled, base_dir):
        self.id = ext_id
        self.name = name
        self.version = version
        self.description = description
        self.content_scripts = content_scripts
        self.enabled = enabled
        self.base_dir = base_dir

    def scripts_for(self, url, run_at):
        if not self.enabled:
            return []
        return [cs for cs in self.content_scripts if cs.applies_to(url, run_at)]

    def load_file(self, path):
        p = self.base_dir / path
        if p.exists() and p.is_file():
            return p.read_text(encoding="utf-8", errors="ignore")
        return None


_extensions = []
_enabled_ids = _load_enabled()


def reload():
    global _extensions
    _extensions = []
    for d in EXT_DIR.iterdir():
        if d.is_dir():
            manifest = _find_manifest(d)
            if manifest:
                try:
                    ext = _from_manifest(d.name, manifest)
                    if ext:
                        _extensions.append(ext)
                except Exception:
                    pass


def _find_manifest(root):
    direct = root / "manifest.json"
    if direct.exists():
        return direct
    for f in root.rglob("manifest.json"):
        if f.is_file():
            return f
    return None


def _from_manifest(ext_id, manifest_file):
    data = json.loads(manifest_file.read_text(encoding="utf-8", errors="ignore"))
    base_dir = manifest_file.parent
    content_scripts = []

    cs_array = data.get("content_scripts", [])
    for cs in cs_array:
        matches = []
        for p in cs.get("matches", []):
            try:
                matches.append(MatchPattern(p))
            except Exception:
                pass
        exclude_matches = []
        for p in cs.get("exclude_matches", []):
            try:
                exclude_matches.append(MatchPattern(p))
            except Exception:
                pass
        js_files = cs.get("js", [])
        css_files = cs.get("css", [])
        run_at = cs.get("run_at", "document_idle")
        all_frames = cs.get("all_frames", False)
        if matches:
            content_scripts.append(ContentScript(matches, exclude_matches, js_files, css_files, run_at, all_frames))

    return Extension(
        ext_id=ext_id,
        name=data.get("name", ext_id),
        version=data.get("version", "1.0"),
        description=data.get("description", ""),
        content_scripts=content_scripts,
        enabled=ext_id in _enabled_ids,
        base_dir=base_dir,
    )


def all():
    return list(_extensions)


def is_enabled(ext_id):
    return ext_id in _enabled_ids


def set_enabled(ext_id, enabled):
    if enabled:
        _enabled_ids.add(ext_id)
    else:
        _enabled_ids.discard(ext_id)
    _save_enabled(_enabled_ids)
    for ext in _extensions:
        if ext.id == ext_id:
            ext.enabled = enabled


def install(xpi_path):
    ext_id = "ext_" + str(int(time.time() * 1000))
    target = EXT_DIR / ext_id
    target.mkdir(parents=True, exist_ok=True)
    try:
        with zipfile.ZipFile(xpi_path, "r") as zf:
            zf.extractall(target)
    except Exception:
        shutil.rmtree(target, ignore_errors=True)
        return None

    manifest = _find_manifest(target)
    if manifest is None:
        shutil.rmtree(target, ignore_errors=True)
        return None

    _enabled_ids.add(ext_id)
    _save_enabled(_enabled_ids)
    ext = _from_manifest(ext_id, manifest)
    if ext is None:
        shutil.rmtree(target, ignore_errors=True)
        _enabled_ids.discard(ext_id)
        _save_enabled(_enabled_ids)
        return None

    _extensions.append(ext)
    return ext


def uninstall(ext_id):
    global _extensions
    _extensions = [e for e in _extensions if e.id != ext_id]
    _enabled_ids.discard(ext_id)
    _save_enabled(_enabled_ids)
    d = EXT_DIR / ext_id
    if d.exists():
        shutil.rmtree(d, ignore_errors=True)


def scripts_for(url, run_at):
    result = []
    for ext in _extensions:
        for cs in ext.scripts_for(url, run_at):
            result.append((ext, cs))
    return result


def inject_scripts(web_view, url, run_at="document_idle"):
    scripts = scripts_for(url, run_at)
    if not scripts:
        return

    css_parts = []
    js_parts = []
    for ext, cs in scripts:
        for css_file in cs.css_files:
            content = ext.load_file(css_file)
            if content:
                css_parts.append(content)
        for js_file in cs.js_files:
            content = ext.load_file(js_file)
            if content:
                js_parts.append(content)

    if css_parts:
        combined_css = "\n".join(css_parts).replace("`", "\\`")
        css_js = "var s=document.createElement('style');s.textContent=`%s`;document.head.appendChild(s);" % combined_css
        web_view.page().runJavaScript("(function(){%s})();" % css_js)

    if js_parts:
        combined_js = "\n".join(js_parts)
        web_view.page().runJavaScript("(function(){%s})();" % combined_js)


reload()
