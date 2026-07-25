#!/usr/bin/env python3
import json, os, re, sys, time, gc, threading, urllib.parse, urllib.request
from pathlib import Path

# === DATA ===
DATA_DIR = Path(os.environ.get("XDG_DATA_HOME", os.path.expanduser("~/.local/share"))) / "lumen"
DATA_DIR.mkdir(parents=True, exist_ok=True)
SETTINGS_FILE = DATA_DIR / "settings.json"
BOOKMARKS_FILE = DATA_DIR / "bookmarks.json"
HISTORY_FILE = DATA_DIR / "history.json"

DEFAULT_SETTINGS = {
    "start_page": "http://127.0.0.1:8888/", "block_ads": True, "block_trackers": True,
    "block_webrtc": True, "load_images": True, "javascript_enabled": True,
    "save_history": True, "force_dark": False,
}

_ads_blocked = 0
_trackers_blocked = 0

AD_HOSTS = {
    "doubleclick.net","googleadservices.com","googlesyndication.com",
    "pagead2.googlesyndication.com","googletagservices.com","adservice.google.com",
    "admob.com","amazon-adsystem.com","adnxs.com","criteo.com","criteo.net",
    "pubmatic.com","rubiconproject.com","openx.net","moatads.com",
    "serving-sys.com","advertising.com","adsrvr.org","casalemedia.com",
    "smartadserver.com","exoclick.com","propellerads.com","popads.net",
    "taboola.com","outbrain.com","revcontent.com","mgid.com","zedo.com",
    "adtech.de","gumgum.com","quantserve.com","scorecardresearch.com",
    "applovin.com","chartboost.com","unityads.unity3d.com","inmobi.com",
    "mopub.com","adroll.com","adcolony.com","vungle.com","ironsrc.com",
    "startapp.com","yieldmo.com","sekindo.com","tribalfusion.com",
    "contextweb.com","bidsxchange.com","inneractive.com"," Undertone.com",
    "mediavine.com","adspeed.com","adzerk.com","buysellads.com",
}

TRACKER_HOSTS = {
    "google-analytics.com","analytics.google.com","googletagmanager.com",
    "segment.io","mixpanel.com","amplitude.com","hotjar.com","fullstory.com",
    "logrocket.com","mouseflow.com","luckyorange.com","clarity.ms",
    "facebook.net","connect.facebook.net","nr-data.net","newrelic.com",
    "branch.io","adjust.com","appsflyer.com","kochava.com","bluekai.com",
    "demdex.net","omtrdc.net","tealium.com","quantcast.com","comscore.com",
    "optimizely.com","crazyegg.com","kissmetrics.com","heap.io",
    "fingerprintjs.com","xandr.com","appnexus.com","scorecardresearch.com",
    "chartbeat.com","statcounter.com","clicky.com","woopra.com",
    "piwik.org","matomo.org","matomo.cloud","snowplowanalytics.com",
    "dynamicyield.com","permutive.com","mparticle.com","sentry.io",
    "rumcdn.com","evidon.com","ensighten.com","eyeota.com",
}

BLOCKED_PATHS = ["/ads/","/adserver/","/advert","/banner","/popup","/prebid",
    "/tracker","/tracking","/beacon","/analytics","/gampad","/gtm.js",
    "/pixel.gif","/log.gif","/collect","/adsense","/adclick","/adcall",
    "/impression","/sync","/stats","/metrics","/telemetry"]

BLOCKED_HOSTS_ALL = AD_HOSTS | TRACKER_HOSTS

def should_block(url, settings):
    global _ads_blocked, _trackers_blocked
    if not url: return False
    p = urllib.parse.urlparse(url)
    host = (p.hostname or "").lower()
    if not host: return False
    path = p.path or ""
    if settings.get("block_ads", True):
        for h in AD_HOSTS:
            if host == h or host.endswith("." + h):
                _ads_blocked += 1; return True
        for bp in BLOCKED_PATHS:
            if path.startswith(bp):
                _ads_blocked += 1; return True
    if settings.get("block_trackers", True):
        for h in TRACKER_HOSTS:
            if host == h or host.endswith("." + h):
                _trackers_blocked += 1; return True
    return False

def block_stats(): return _ads_blocked, _trackers_blocked
def reset_stats():
    global _ads_blocked, _trackers_blocked
    _ads_blocked = 0; _trackers_blocked = 0

def load_settings():
    if SETTINGS_FILE.exists():
        try:
            s = json.loads(SETTINGS_FILE.read_text())
            m = DEFAULT_SETTINGS.copy(); m.update(s); return m
        except: pass
    return DEFAULT_SETTINGS.copy()

def save_settings(s): SETTINGS_FILE.write_text(json.dumps(s, indent=2))

class Store:
    def __init__(self, path, mx=500):
        self.path = path; self.mx = mx
        self._items = self._load()
    def _load(self):
        if self.path.exists():
            try: return json.loads(self.path.read_text())
            except: pass
        return []
    def _save(self): self.path.write_text(json.dumps(self._items, indent=2))
    def all(self): return list(reversed(self._items))
    def add(self, item):
        self._items = [i for i in self._items if i.get("url") != item.get("url")]
        self._items.append(item)
        if len(self._items) > self.mx: self._items = self._items[-self.mx:]
        self._save()
    def remove(self, url):
        self._items = [i for i in self._items if i.get("url") != url]; self._save()
    def is_bookmarked(self, url): return any(i.get("url") == url for i in self._items)
    def clear(self): self._items = []; self._save()

# === SEARCH (self-hosted engine API) ===
UA = "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0 Lumen/1.0"
SEARCH_API = "http://127.0.0.1:8888"

def _api_get(path):
    req = urllib.request.Request(SEARCH_API + path, headers={
        "User-Agent": UA,
        "Accept": "application/json",
        "Accept-Language": "en-US,en;q=0.9"})
    with urllib.request.urlopen(req, timeout=12) as r:
        return r.read().decode("utf-8", "replace")

def search_web(query, cb, stype="WEB"):
    def w():
        try:
            raw = _api_get("/search?q=%s&type=%s" % (urllib.parse.quote(query), stype))
            data = json.loads(raw)
            data.setdefault("query", query)
            data.setdefault("related", [])
            cb(json.dumps(data))
        except Exception as e:
            cb(json.dumps({"query": query, "results": [], "related": [], "error": str(e)}))
    threading.Thread(target=w, daemon=True).start()

def get_suggestions(query, cb):
    def w():
        try:
            raw = _api_get("/suggest?q=%s" % urllib.parse.quote(query))
            data = json.loads(raw)
            cb(json.dumps({"query": query, "suggestions": data.get("suggestions", [])}))
        except:
            cb(json.dumps({"query": query, "suggestions": []}))
    threading.Thread(target=w, daemon=True).start()

# === BROWSER ===
from PyQt6.QtCore import Qt, QUrl, QTimer, pyqtSignal, QObject
from PyQt6.QtGui import QAction, QIcon
from PyQt6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QLineEdit,
    QPushButton, QTabWidget, QToolBar, QMenu, QDialog, QListWidget,
    QListWidgetItem, QLabel, QCheckBox, QMessageBox, QProgressBar,
    QStyle)
from PyQt6.QtWebEngineCore import (
    QWebEngineProfile, QWebEngineUrlRequestInterceptor,
    QWebEngineUrlRequestInfo, QWebEnginePage, QWebEngineSettings)
from PyQt6.QtWebEngineWidgets import QWebEngineView

ASSETS_DIR = Path(__file__).parent / "assets"
SEARCH_HTML = (ASSETS_DIR / "search.html").read_text() if (ASSETS_DIR / "search.html").exists() else "<h1>Lumen</h1>"

WEBRTC_JS = """(function(){
    if(window.__lumen_webrtc_blocked)return;
    window.__lumen_webrtc_blocked=true;
    try{
        window.RTCPeerConnection=undefined;
        window.webkitRTCPeerConnection=undefined;
        window.mozRTCPeerConnection=undefined;
        if(navigator&&navigator.mediaDevices){
            navigator.mediaDevices.getUserMedia=function(){return Promise.reject(new Error('WebRTC disabled'));};
            navigator.mediaDevices.enumerateDevices=function(){return Promise.resolve([]);};
        }
    }catch(e){}
})();"""

class Interceptor(QWebEngineUrlRequestInterceptor):
    def __init__(self, settings):
        super().__init__(); self.settings = settings
    def set_settings(self, s): self.settings = s
    def interceptRequest(self, info):
        if should_block(info.requestUrl().toString(), self.settings):
            info.block(True)

class SearchBridge(QObject):
    results_ready = pyqtSignal(str)
    suggestions_ready = pyqtSignal(str)
    def fetchResults(self, q, t):
        search_web(q, lambda r: self.results_ready.emit(r), stype=t)
    def fetchSuggestions(self, q):
        get_suggestions(q, lambda r: self.suggestions_ready.emit(r))
    def getProtectionStats(self):
        a, t = block_stats()
        return "Blocked: %d ads, %d trackers" % (a, t)

class Tab(QWidget):
    def __init__(self, parent, url="http://127.0.0.1:8888/", profile=None):
        super().__init__()
        self.parent_browser = parent; self.settings = parent.settings
        self.title = ""; self.url = url; self.favicon = None
        self.suspended = False; self.last_url = url
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0); layout.setSpacing(0)
        self.web_view = QWebEngineView()
        page = QWebEnginePage(profile, self.web_view) if profile else QWebEnginePage(self.web_view)
        self.web_view.setPage(page)
        self.bridge = SearchBridge()
        page.setWebChannel(None)
        layout.addWidget(self.web_view)
        self.web_view.urlChanged.connect(self._on_url)
        self.web_view.titleChanged.connect(lambda t: self._on_title(t))
        self.web_view.iconChanged.connect(lambda i: setattr(self, "favicon", i))
        self.web_view.loadStarted.connect(lambda: parent.update_progress(1))
        self.web_view.loadProgress.connect(lambda p: parent.update_progress(p))
        self.web_view.loadFinished.connect(self._on_load)
        if url: self.load(url)

    def suspend(self):
        if self.suspended: return
        self.last_url = self.url
        try: self.web_view.page().setLifecycleState(QWebEnginePage.LifecycleState.Paused)
        except: self.web_view.setHtml("<html><body style='background:#121316'></body></html>")
        self.suspended = True

    def resume(self):
        if not self.suspended: return
        self.suspended = False
        try: self.web_view.page().setLifecycleState(QWebEnginePage.LifecycleState.Active)
        except: self.load(self.last_url)

    def load(self, url):
        self.url = url
        if url == "lumen://search" or url.startswith("lumen://search"):
            url = "http://127.0.0.1:8888/"
            if url.startswith("lumen://search?q="):
                q = urllib.parse.unquote(url.split("q=", 1)[1])
                url = "http://127.0.0.1:8888/#q=" + urllib.parse.quote(q)
        self.web_view.setUrl(QUrl(url))

    def _on_url(self, url):
        self.url = url.toString(); self.parent_browser.update_url_bar(self.url)
    def _on_title(self, title):
        self.title = title; self.parent_browser.update_tab_title(self)
    def _on_load(self, ok):
        self.parent_browser.update_progress(100)
        if self.settings.get("block_webrtc", True):
            self.web_view.page().runJavaScript(WEBRTC_JS)
        if self.url.startswith("http"):
            if self.settings.get("save_history", True):
                Store(HISTORY_FILE).add({"title": self.title or self.url, "url": self.url, "time": int(time.time())})

class Window(QMainWindow):
    MAX_TABS = 10
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Lumen"); self.setMinimumSize(600, 400); self.resize(1000, 700)
        self.settings = load_settings()
        self.bookmarks = Store(BOOKMARKS_FILE)
        self.history = Store(HISTORY_FILE)
        self.profile = QWebEngineProfile("lumen", self)
        self.interceptor = Interceptor(self.settings)
        self.profile.setUrlRequestInterceptor(self.interceptor)
        self.profile.setCachePath(str(DATA_DIR / "cache"))
        self.profile.setPersistentStoragePath(str(DATA_DIR / "storage"))
        self.profile.setHttpCacheMaximumSize(5 * 1024 * 1024)
        self.profile.setHttpCacheType(QWebEngineProfile.HttpCacheType.MemoryHttpCache)
        ps = self.profile.settings()
        ps.setAttribute(QWebEngineSettings.WebAttribute.JavascriptEnabled, self.settings.get("javascript_enabled", True))
        ps.setAttribute(QWebEngineSettings.WebAttribute.AutoLoadImages, self.settings.get("load_images", True))
        ps.setAttribute(QWebEngineSettings.WebAttribute.AllowRunningInsecureContent, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.AllowGeolocationOnInsecureOrigins, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.WebGLEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.Accelerated2dCanvasEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.PlaybackRequiresUserGesture, True)
        ps.setAttribute(QWebEngineSettings.WebAttribute.JavascriptCanOpenWindows, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.JavascriptCanAccessClipboard, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.FullScreenSupportEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.PrintElementBackgrounds, False)
        ps.setDefaultTextEncoding("utf-8")
        c = QWidget(); self.setCentralWidget(c)
        ml = QVBoxLayout(c); ml.setContentsMargins(0, 0, 0, 0); ml.setSpacing(0)
        tb = QToolBar(); tb.setMovable(False)
        self.btn_back = QPushButton(); self.btn_back.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowBack))
        self.btn_back.setFixedSize(32, 32); self.btn_back.clicked.connect(self._back); tb.addWidget(self.btn_back)
        self.btn_fwd = QPushButton(); self.btn_fwd.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowForward))
        self.btn_fwd.setFixedSize(32, 32); self.btn_fwd.clicked.connect(self._fwd); tb.addWidget(self.btn_fwd)
        self.btn_reload = QPushButton(); self.btn_reload.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_BrowserReload))
        self.btn_reload.setFixedSize(32, 32); self.btn_reload.clicked.connect(self._reload); tb.addWidget(self.btn_reload)
        self.btn_home = QPushButton(); self.btn_home.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_DirHomeIcon))
        self.btn_home.setFixedSize(32, 32); self.btn_home.clicked.connect(self._home); tb.addWidget(self.btn_home)
        self.url_bar = QLineEdit(); self.url_bar.setPlaceholderText("Search or type URL")
        self.url_bar.returnPressed.connect(self._navigate); tb.addWidget(self.url_bar)
        self.btn_menu = QPushButton("\u2630"); self.btn_menu.setFixedSize(32, 32)
        self.btn_menu.clicked.connect(self._menu); tb.addWidget(self.btn_menu)
        ml.addWidget(tb)
        self.progress = QProgressBar(); self.progress.setMaximumHeight(3)
        self.progress.setTextVisible(False); self.progress.setRange(0, 100); self.progress.hide()
        ml.addWidget(self.progress)
        self.tab_widget = QTabWidget(); self.tab_widget.setTabsClosable(True)
        self.tab_widget.tabCloseRequested.connect(self._close_tab)
        self.tab_widget.currentChanged.connect(self._tab_changed)
        ml.addWidget(self.tab_widget)
        self.btn_new = QPushButton("+"); self.btn_new.setFixedSize(28, 28)
        self.btn_new.clicked.connect(lambda: self.new_tab())
        self.tab_widget.setCornerWidget(self.btn_new, Qt.Corner.TopRightCorner)
        self._theme(); self.new_tab()

    def _theme(self):
        if self.settings.get("force_dark", False):
            self.setStyleSheet("""
                QMainWindow,QWidget{background:#121316;color:#e8eaed;}
                QToolBar{background:#1e2024;border:none;}
                QPushButton{background:#2c2f34;border:none;border-radius:6px;color:#e8eaed;}
                QPushButton:hover{background:#3a3f44;}
                QLineEdit{background:#1e2024;color:#e8eaed;border:1px solid #2c2f34;border-radius:16px;padding:5px 12px;font-size:13px;}
                QTabWidget::pane{border:none;background:#121316;}
                QTabBar::tab{background:#1e2024;color:#9aa0a6;padding:6px 14px;border:none;border-radius:6px 6px 0 0;}
                QTabBar::tab:selected{background:#121316;color:#3b82f6;}
                QProgressBar{background:#2c2f34;border:none;}QProgressBar::chunk{background:#3b82f6;}
            """)
        else:
            self.setStyleSheet("""
                QMainWindow,QWidget{background:#fff;color:#111418;}
                QToolBar{background:#f5f6f8;border:none;border-bottom:1px solid #e3e5e8;}
                QPushButton{background:#e3e5e8;border:none;border-radius:6px;color:#111418;}
                QPushButton:hover{background:#d3d5d8;}
                QLineEdit{background:#f5f6f8;color:#111418;border:1px solid #e3e5e8;border-radius:16px;padding:5px 12px;font-size:13px;}
                QTabWidget::pane{border:none;background:#fff;}
                QTabBar::tab{background:#f5f6f8;color:#5a6068;padding:6px 14px;border:none;border-radius:6px 6px 0 0;}
                QTabBar::tab:selected{background:#fff;color:#3b82f6;}
                QProgressBar{background:#e3e5e8;border:none;}QProgressBar::chunk{background:#3b82f6;}
            """)

    def new_tab(self, url="http://127.0.0.1:8888/"):
        if self.tab_widget.count() >= self.MAX_TABS:
            self.tab_widget.removeTab(0)
        t = Tab(self, url, self.profile)
        idx = self.tab_widget.addTab(t, "New Tab")
        self.tab_widget.setCurrentIndex(idx); self._tab_changed(idx); return t

    def open_url(self, url):
        t = self.current_tab()
        if t: t.load(url)
    def current_tab(self):
        w = self.tab_widget.currentWidget()
        return w if isinstance(w, Tab) else None
    def current_view(self):
        t = self.current_tab(); return t.web_view if t else None
    def _back(self):
        v = self.current_view()
        if v and v.history().canGoBack(): v.back()
    def _fwd(self):
        v = self.current_view()
        if v and v.history().canGoForward(): v.forward()
    def _reload(self): self.current_view().reload()
    def _home(self):
        t = self.current_tab()
        if t: t.load(self.settings.get("start_page", "http://127.0.0.1:8888/"))
    def _navigate(self):
        text = self.url_bar.text().strip()
        if not text: return
        if text.startswith("http://") or text.startswith("https://"):
            url = text
        elif " " in text or not re.match(r"^[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}", text):
            url = "http://127.0.0.1:8888/#q=" + urllib.parse.quote(text)
        else:
            url = "https://" + text
        self.open_url(url)
    def _tab_changed(self, idx):
        for i in range(self.tab_widget.count()):
            w = self.tab_widget.widget(i)
            if isinstance(w, Tab) and i != idx: w.suspend()
        t = self.current_tab()
        if t:
            t.resume()
            if not self.url_bar.hasFocus():
                self.url_bar.setText("" if t.url.startswith("lumen://") or t.url.startswith("http://127.0.0.1:8888") else t.url)
            v = self.current_view()
            self.btn_back.setEnabled(v is not None and v.history().canGoBack())
            self.btn_fwd.setEnabled(v is not None and v.history().canGoForward())
        gc.collect()

    def update_url_bar(self, url):
        if not self.url_bar.hasFocus():
            self.url_bar.setText("" if url.startswith("lumen://") or url.startswith("http://127.0.0.1:8888") else url)
        v = self.current_view()
        self.btn_back.setEnabled(v is not None and v.history().canGoBack())
        self.btn_fwd.setEnabled(v is not None and v.history().canGoForward())
    def update_tab_title(self, tab):
        idx = self.tab_widget.indexOf(tab)
        if idx >= 0: self.tab_widget.setTabText(idx, (tab.title[:25] if tab.title else "New Tab"))
    def update_progress(self, v):
        if v < 100: self.progress.show(); self.progress.setValue(v)
        else: QTimer.singleShot(500, self.progress.hide)
    def _close_tab(self, idx):
        w = self.tab_widget.widget(idx)
        if self.tab_widget.count() <= 1: self.close(); return
        if isinstance(w, Tab):
            try: w.web_view.page().runJavaScript("try{window.stop()}catch(e){}")
            except: pass
        self.tab_widget.removeTab(idx)
        try:
            w.web_view.setHtml("<html></html>")
            w.web_view.page().deleteLater(); w.web_view.deleteLater()
        except: pass

    def _menu(self):
        m = QMenu(self)
        m.setStyleSheet("QMenu{background:#1e2024;color:#e8eaed;border-radius:8px;padding:6px;}QMenu::item{padding:6px 28px 6px 16px;border-radius:6px;}QMenu::item:selected{background:#3b82f6;}QMenu::separator{height:1px;background:#2c2f34;margin:4px 10px;}")
        m.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowBack), "Back", self._back)
        m.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowForward), "Forward", self._fwd)
        m.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_BrowserReload), "Reload", self._reload)
        m.addSeparator()
        m.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_DirHomeIcon), "Home", self._home)
        m.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_FileDialogNewFolder), "New Tab", lambda: self.new_tab())
        url = self.current_tab().url if self.current_tab() else ""
        if url and url.startswith("http"):
            if self.bookmarks.is_bookmarked(url):
                m.addAction("Remove Bookmark", lambda: self.bookmarks.remove(url))
            else:
                t = self.current_tab()
                m.addAction("Bookmark This Page", lambda: self.bookmarks.add({"title": t.title if t else url, "url": url}))
        m.addAction("Bookmarks", self._bookmarks)
        m.addAction("History", self._history)
        m.addAction("Find in Page", self._find)
        m.addSeparator()
        a, tr = block_stats()
        m.addAction("Blocked: %d ads, %d trackers" % (a, tr)).setEnabled(False)
        m.addSeparator()
        m.addAction("Settings", self._settings)
        m.addAction("Exit", self.close)
        m.exec(self.btn_menu.mapToGlobal(self.btn_menu.rect().bottomLeft()))

    def _bookmarks(self):
        d = QDialog(self); d.setWindowTitle("Bookmarks"); d.setMinimumSize(400, 300)
        l = QVBoxLayout(d); lw = QListWidget()
        for i in self.bookmarks.all():
            e = QListWidgetItem(i.get("title", i["url"])); e.setData(Qt.ItemDataRole.UserRole, i["url"]); lw.addItem(e)
        lw.itemDoubleClicked.connect(lambda i: (self.open_url(i.data(Qt.ItemDataRole.UserRole)), d.close()))
        l.addWidget(lw)
        b = QPushButton("Remove Selected")
        b.clicked.connect(lambda: self.bookmarks.remove(lw.currentItem().data(Qt.ItemDataRole.UserRole)) if lw.currentItem() else None)
        l.addWidget(b); d.exec()

    def _history(self):
        d = QDialog(self); d.setWindowTitle("History"); d.setMinimumSize(400, 300)
        l = QVBoxLayout(d); lw = QListWidget()
        for i in self.history.all():
            e = QListWidgetItem(i.get("title", i["url"])); e.setData(Qt.ItemDataRole.UserRole, i["url"]); lw.addItem(e)
        lw.itemDoubleClicked.connect(lambda i: (self.open_url(i.data(Qt.ItemDataRole.UserRole)), d.close()))
        l.addWidget(lw)
        b = QPushButton("Clear History")
        b.clicked.connect(lambda: (self.history.clear(), lw.clear())); l.addWidget(b); d.exec()

    def _find(self):
        v = self.current_view()
        if v:
            v.page().runJavaScript("""(function(){
                var e=document.getElementById('__lumen_find');
                if(e){e.remove();return;}
                var b=document.createElement('div');
                b.id='__lumen_find';
                b.style.cssText='position:fixed;top:8px;right:8px;z-index:999999;background:#1e2024;color:#e8eaed;padding:6px;border-radius:8px;display:flex;gap:6px;box-shadow:0 4px 12px rgba(0,0,0,.3);';
                var i=document.createElement('input');
                i.placeholder='Find';i.style.cssText='background:#2c2f34;color:#e8eaed;border:1px solid #3a3f44;border-radius:6px;padding:3px 6px;width:180px;';
                i.oninput=function(){window.find(i.value);};
                b.appendChild(i);
                var c=document.createElement('button');
                c.textContent='\u2715';c.style.cssText='background:none;border:none;color:#9aa0a6;font-size:16px;cursor:pointer;';
                c.onclick=function(){b.remove();if(window.getSelection)window.getSelection().removeAllRanges();};
                b.appendChild(c);
                document.body.appendChild(b);i.focus();
            })();""")

    def _settings(self):
        d = QDialog(self); d.setWindowTitle("Settings"); d.setMinimumWidth(400)
        l = QVBoxLayout(d)
        cb_dark = QCheckBox("Force dark mode"); cb_dark.setChecked(self.settings.get("force_dark", False)); l.addWidget(cb_dark)
        cb_img = QCheckBox("Load images"); cb_img.setChecked(self.settings.get("load_images", True)); l.addWidget(cb_img)
        cb_js = QCheckBox("Enable JavaScript"); cb_js.setChecked(self.settings.get("javascript_enabled", True)); l.addWidget(cb_js)
        cb_ads = QCheckBox("Block ads"); cb_ads.setChecked(self.settings.get("block_ads", True)); l.addWidget(cb_ads)
        cb_tr = QCheckBox("Block trackers"); cb_tr.setChecked(self.settings.get("block_trackers", True)); l.addWidget(cb_tr)
        cb_wr = QCheckBox("Disable WebRTC"); cb_wr.setChecked(self.settings.get("block_webrtc", True)); l.addWidget(cb_wr)
        cb_hist = QCheckBox("Save history"); cb_hist.setChecked(self.settings.get("save_history", True)); l.addWidget(cb_hist)
        a, t = block_stats()
        l.addWidget(QLabel("Blocked: %d ads, %d trackers" % (a, t)))
        cb = QPushButton("Clear browsing data")
        cb.clicked.connect(lambda: (Store(HISTORY_FILE).clear(), reset_stats(), QMessageBox.information(d, "Lumen", "Cleared.")))
        l.addWidget(cb)
        bl = QHBoxLayout(); bl.addStretch()
        sv = QPushButton("Save"); sv.clicked.connect(d.accept); bl.addWidget(sv)
        cn = QPushButton("Cancel"); cn.clicked.connect(d.reject); bl.addWidget(cn)
        l.addLayout(bl)
        if d.exec():
            self.settings["force_dark"] = cb_dark.isChecked()
            self.settings["load_images"] = cb_img.isChecked()
            self.settings["javascript_enabled"] = cb_js.isChecked()
            self.settings["block_ads"] = cb_ads.isChecked()
            self.settings["block_trackers"] = cb_tr.isChecked()
            self.settings["block_webrtc"] = cb_wr.isChecked()
            self.settings["save_history"] = cb_hist.isChecked()
            save_settings(self.settings)
            self.interceptor.set_settings(self.settings)
            self._theme()
            ps = self.profile.settings()
            ps.setAttribute(QWebEngineSettings.WebAttribute.JavascriptEnabled, self.settings.get("javascript_enabled", True))
            ps.setAttribute(QWebEngineSettings.WebAttribute.AutoLoadImages, self.settings.get("load_images", True))

def main():
    os.environ.setdefault("QTWEBENGINE_CHROMIUM_FLAGS", "--single-process --renderer-process-limit=1 --disable-gpu --disable-gpu-compositing --disable-extensions --disable-plugins --disable-notifications --disable-geolocation --disable-media-stream --disable-background-networking --disable-background-timer-throttling --disable-renderer-backgrounding --disable-backgrounding-occluded-windows --memory-pressure-off --disk-cache-size=5242880 --low-end-device-mode --no-sandbox")
    if os.geteuid() == 0:
        os.environ["QTWEBENGINE_DISABLE_SANDBOX"] = "1"
    from PyQt6.QtWidgets import QApplication
    app = QApplication(sys.argv)
    app.setApplicationName("Lumen")
    win = Window()
    win.show()
    sys.exit(app.exec())

if __name__ == "__main__":
    main()
