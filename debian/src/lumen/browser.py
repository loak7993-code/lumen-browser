import json
import os
import re
import urllib.parse
from pathlib import Path

from PyQt6.QtCore import Qt, QUrl, QTimer, pyqtSignal, QObject
from PyQt6.QtGui import QAction, QIcon, QColor, QFont, QPixmap
from PyQt6.QtWidgets import (
    QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QLineEdit,
    QPushButton, QTabWidget, QToolBar, QMenu, QDialog, QListWidget,
    QListWidgetItem, QLabel, QCheckBox, QMessageBox, QProgressBar,
    QStyle, QFileDialog, QScrollArea, QFrame, QSizePolicy, QApplication,
)
from PyQt6.QtWebEngineCore import QWebEngineProfile, QWebEngineUrlRequestInterceptor, QWebEngineUrlRequestInfo, QWebEnginePage, QWebEngineSettings
from PyQt6.QtWebEngineWidgets import QWebEngineView

from .stores import load_settings, save_settings, Store, BOOKMARKS_FILE, HISTORY_FILE, DATA_DIR
from .adblocker import should_block, stats as block_stats, reset as reset_stats
from . import search_engine
from . import extension_manager


ASSETS_DIR = Path(__file__).parent / "assets"
SEARCH_HTML = (ASSETS_DIR / "search.html").read_text()
WEBRTC_JS = """
(function(){
    if (window.__lumen_webrtc_blocked) return;
    window.__lumen_webrtc_blocked = true;
    try {
        window.RTCPeerConnection = undefined;
        window.webkitRTCPeerConnection = undefined;
        window.mozRTCPeerConnection = undefined;
        if (navigator && navigator.mediaDevices) {
            navigator.mediaDevices.getUserMedia = function(){return Promise.reject(new Error('WebRTC disabled'));};
            navigator.mediaDevices.enumerateDevices = function(){return Promise.resolve([]);};
        }
    } catch(e) {}
})();
"""


class AdblockInterceptor(QWebEngineUrlRequestInterceptor):
    def __init__(self, settings):
        super().__init__()
        self.settings = settings

    def set_settings(self, settings):
        self.settings = settings

    def interceptRequest(self, info):
        url = info.requestUrl().toString()
        if should_block(url, self.settings):
            info.block(True)


class LumenSearchBridge(QObject):
    results_ready = pyqtSignal(str)
    suggestions_ready = pyqtSignal(str)

    def __init__(self):
        super().__init__()

    @staticmethod
    def _to_json(obj):
        return json.dumps(obj, ensure_ascii=False)

    def fetchResults(self, query, type_str):
        cb = lambda r: self.results_ready.emit(self._to_json(r))
        t = type_str.upper()
        if t == "IMAGES":
            search_engine.search_images(query, cb)
        elif t == "VIDEOS":
            search_engine.search_videos(query, cb)
        elif t == "NEWS":
            search_engine.search_news(query, cb)
        else:
            search_engine.search_web(query, cb)

    def fetchSuggestions(self, query):
        cb = lambda r: self.suggestions_ready.emit(self._to_json(r))
        search_engine.get_suggestions(query, cb)

    def getProtectionStats(self):
        ads, trackers = block_stats()
        return "Protected - {} blocked".format(ads + trackers)


class SearchPage(QWebEnginePage):
    def __init__(self, parent=None):
        super().__init__(parent)

    def acceptNavigationRequest(self, url, nav_type, is_main):
        url_str = url.toString()
        if url_str.startswith("http://") or url_str.startswith("https://"):
            self.parent().parent().parent().open_url(url_str)
            return False
        return super().acceptNavigationRequest(url, nav_type, is_main)


class BrowserTab(QWidget):
    def __init__(self, parent_browser, url="lumen://search", profile=None):
        super().__init__()
        self.parent_browser = parent_browser
        self.settings = parent_browser.settings
        self.title = ""
        self.url = url
        self.favicon = None
        self.suspended = False
        self.last_url = url

        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        self.web_view = QWebEngineView()

        if profile:
            page = QWebEnginePage(profile, self.web_view)
        else:
            page = QWebEnginePage(self.web_view)
        self.web_view.setPage(page)

        self.bridge = LumenSearchBridge()
        page.setWebChannel(None)

        layout.addWidget(self.web_view)

        self.web_view.urlChanged.connect(self._on_url_changed)
        self.web_view.titleChanged.connect(lambda t: self._on_title_changed(t))
        self.web_view.iconChanged.connect(lambda icon: setattr(self, "favicon", icon))
        self.web_view.loadStarted.connect(lambda: self.parent_browser.update_progress(1))
        self.web_view.loadProgress.connect(lambda p: self.parent_browser.update_progress(p))
        self.web_view.loadFinished.connect(lambda ok: self._on_load_finished(ok))

        if url:
            self.load_url(url)

    def suspend(self):
        if self.suspended:
            return
        try:
            self.last_url = self.url
            self.web_view.page().runJavaScript("document.documentElement.outerHTML", self._on_html_saved)
        except Exception:
            pass
        try:
            self.web_view.page().setLifecycleState(QWebEnginePage.LifecycleState.Paused)
        except Exception:
            self.web_view.setHtml("<html><body style='background:#121316'></body></html>")
        self.suspended = True

    def _on_html_saved(self, _html):
        pass

    def resume(self):
        if not self.suspended:
            return
        self.suspended = False
        try:
            self.web_view.page().setLifecycleState(QWebEnginePage.LifecycleState.Active)
        except Exception:
            self.load_url(self.last_url)

    def load_url(self, url):
        self.url = url
        if url == "lumen://search" or url.startswith("lumen://search"):
            self._load_search_page()
        elif url.startswith("lumen://search?q="):
            query = urllib.parse.unquote(url.split("q=", 1)[1])
            self._load_search_page(query)
        else:
            self.web_view.setUrl(QUrl(url))

    def _load_search_page(self, query=None):
        html = SEARCH_HTML
        self.web_view.setHtml(html, QUrl("lumen://search/"))
        if query:
            QTimer.singleShot(300, lambda: self.web_view.page().runJavaScript("doSearch('{}')".format(query.replace("'", "\\'"))))

    def _on_url_changed(self, url):
        self.url = url.toString()
        self.parent_browser.update_url_bar(self.url)

    def _on_title_changed(self, title):
        self.title = title
        self.parent_browser.update_tab_title(self)

    def _on_load_finished(self, ok):
        self.parent_browser.update_progress(100)
        if self.settings.get("block_webrtc", True):
            self.web_view.page().runJavaScript(WEBRTC_JS)
        if self.url.startswith("http"):
            extension_manager.inject_scripts(self.web_view, self.url, "document_idle")
            if self.settings.get("save_history", True):
                from .stores import Store, HISTORY_FILE
                hist = Store(HISTORY_FILE)
                hist.add({"title": self.title or self.url, "url": self.url, "time": int(__import__("time").time())})


class BrowserWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Lumen")
        self.setMinimumSize(800, 600)
        self.resize(1200, 800)
        self.settings = load_settings()
        self.bookmarks = Store(BOOKMARKS_FILE)
        self.history = Store(HISTORY_FILE)

        self.profile = QWebEngineProfile("lumen", self)
        self.interceptor = AdblockInterceptor(self.settings)
        self.profile.setUrlRequestInterceptor(self.interceptor)
        self.profile.setCachePath(str(DATA_DIR / "cache"))
        self.profile.setPersistentStoragePath(str(DATA_DIR / "storage"))
        self.profile.setHttpCacheMaximumSize(10 * 1024 * 1024)
        self.profile.setHttpCacheType(QWebEngineProfile.HttpCacheType.MemoryHttpCache)

        ps = self.profile.settings()
        ps.setAttribute(QWebEngineSettings.WebAttribute.JavascriptEnabled, self.settings.get("javascript_enabled", True))
        ps.setAttribute(QWebEngineSettings.WebAttribute.AutoLoadImages, self.settings.get("load_images", True))
        ps.setAttribute(QWebEngineSettings.WebAttribute.SpatialNavigationEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.AllowRunningInsecureContent, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.AllowGeolocationOnInsecureOrigins, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.ScrollAnimatorEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.WebGLEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.Accelerated2dCanvasEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.LocalStorageEnabled, True)
        ps.setAttribute(QWebEngineSettings.WebAttribute.PlaybackRequiresUserGesture, True)
        ps.setAttribute(QWebEngineSettings.WebAttribute.JavascriptCanOpenWindows, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.JavascriptCanAccessClipboard, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.FullScreenSupportEnabled, False)
        ps.setAttribute(QWebEngineSettings.WebAttribute.PrintElementBackgrounds, False)
        ps.setDefaultTextEncoding("utf-8")

        central = QWidget()
        self.setCentralWidget(central)
        main_layout = QVBoxLayout(central)
        main_layout.setContentsMargins(0, 0, 0, 0)
        main_layout.setSpacing(0)

        toolbar = QToolBar()
        toolbar.setMovable(False)
        toolbar.setIconSize(toolbar.iconSize())

        self.btn_back = QPushButton()
        self.btn_back.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowBack))
        self.btn_back.setFixedSize(36, 36)
        self.btn_back.clicked.connect(self._go_back)
        toolbar.addWidget(self.btn_back)

        self.btn_forward = QPushButton()
        self.btn_forward.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowForward))
        self.btn_forward.setFixedSize(36, 36)
        self.btn_forward.clicked.connect(self._go_forward)
        toolbar.addWidget(self.btn_forward)

        self.btn_refresh = QPushButton()
        self.btn_refresh.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_BrowserReload))
        self.btn_refresh.setFixedSize(36, 36)
        self.btn_refresh.clicked.connect(self._refresh)
        toolbar.addWidget(self.btn_refresh)

        self.btn_home = QPushButton()
        self.btn_home.setIcon(self.style().standardIcon(QStyle.StandardPixmap.SP_DirHomeIcon))
        self.btn_home.setFixedSize(36, 36)
        self.btn_home.clicked.connect(self._go_home)
        toolbar.addWidget(self.btn_home)

        self.url_bar = QLineEdit()
        self.url_bar.setPlaceholderText("Search or type URL")
        self.url_bar.returnPressed.connect(self._navigate)
        toolbar.addWidget(self.url_bar)

        self.btn_menu = QPushButton()
        self.btn_menu.setText("\u2630")
        self.btn_menu.setFixedSize(36, 36)
        self.btn_menu.clicked.connect(self._show_menu)
        toolbar.addWidget(self.btn_menu)

        main_layout.addWidget(toolbar)

        self.progress = QProgressBar()
        self.progress.setMaximumHeight(3)
        self.progress.setTextVisible(False)
        self.progress.setRange(0, 100)
        self.progress.hide()
        main_layout.addWidget(self.progress)

        self.tab_widget = QTabWidget()
        self.tab_widget.setTabsClosable(True)
        self.tab_widget.tabCloseRequested.connect(self._close_tab)
        self.tab_widget.currentChanged.connect(self._on_tab_changed)
        main_layout.addWidget(self.tab_widget)

        self.btn_new_tab = QPushButton("+")
        self.btn_new_tab.setFixedSize(32, 32)
        self.btn_new_tab.clicked.connect(lambda: self.new_tab())
        self.tab_widget.setCornerWidget(self.btn_new_tab, Qt.Corner.TopRightCorner)

        self._apply_theme()
        self.new_tab()

        self.cleanup_timer = QTimer(self)
        self.cleanup_timer.timeout.connect(self._periodic_cleanup)
        self.cleanup_timer.start(30000)

    def _periodic_cleanup(self):
        import gc
        gc.collect()

    def _apply_theme(self):
        if self.settings.get("force_dark", False):
            self.setStyleSheet("""
                QMainWindow, QWidget { background-color: #121316; color: #e8eaed; }
                QToolBar { background-color: #1e2024; border: none; }
                QPushButton { background-color: #2c2f34; border: none; border-radius: 6px; color: #e8eaed; }
                QPushButton:hover { background-color: #3a3f44; }
                QLineEdit { background-color: #1e2024; color: #e8eaed; border: 1px solid #2c2f34; border-radius: 18px; padding: 6px 14px; font-size: 14px; }
                QTabWidget::pane { border: none; background: #121316; }
                QTabBar::tab { background: #1e2024; color: #9aa0a6; padding: 8px 16px; border: none; border-radius: 8px 8px 0 0; }
                QTabBar::tab:selected { background: #121316; color: #3b82f6; }
                QProgressBar { background-color: #2c2f34; border: none; }
                QProgressBar::chunk { background-color: #3b82f6; }
            """)
        else:
            self.setStyleSheet("""
                QMainWindow, QWidget { background-color: #ffffff; color: #111418; }
                QToolBar { background-color: #f5f6f8; border: none; border-bottom: 1px solid #e3e5e8; }
                QPushButton { background-color: #e3e5e8; border: none; border-radius: 6px; color: #111418; }
                QPushButton:hover { background-color: #d3d5d8; }
                QLineEdit { background-color: #f5f6f8; color: #111418; border: 1px solid #e3e5e8; border-radius: 18px; padding: 6px 14px; font-size: 14px; }
                QTabWidget::pane { border: none; background: #ffffff; }
                QTabBar::tab { background: #f5f6f8; color: #5a6068; padding: 8px 16px; border: none; border-radius: 8px 8px 0 0; }
                QTabBar::tab:selected { background: #ffffff; color: #3b82f6; }
                QProgressBar { background-color: #e3e5e8; border: none; }
                QProgressBar::chunk { background-color: #3b82f6; }
            """)

    MAX_TABS = 12

    def new_tab(self, url="lumen://search"):
        if self.tab_widget.count() >= self.MAX_TABS:
            self.tab_widget.removeTab(0)
        tab = BrowserTab(self, url, self.profile)
        idx = self.tab_widget.addTab(tab, "New Tab")
        self.tab_widget.setCurrentIndex(idx)
        self._on_tab_changed(idx)
        return tab

    def open_url(self, url):
        tab = self.current_tab()
        if tab:
            tab.load_url(url)

    def current_tab(self):
        w = self.tab_widget.currentWidget()
        return w if isinstance(w, BrowserTab) else None

    def current_view(self):
        tab = self.current_tab()
        return tab.web_view if tab else None

    def _go_back(self):
        v = self.current_view()
        if v and v.history().canGoBack():
            v.back()

    def _go_forward(self):
        v = self.current_view()
        if v and v.history().canGoForward():
            v.forward()

    def _refresh(self):
        self.current_view().reload()

    def _go_home(self):
        tab = self.current_tab()
        if tab:
            tab.load_url(self.settings.get("start_page", "lumen://search"))

    def _navigate(self):
        text = self.url_bar.text().strip()
        if not text:
            return
        url = self._normalize_url(text)
        self.open_url(url)

    def _normalize_url(self, text):
        if text.startswith("http://") or text.startswith("https://"):
            return text
        if " " in text or not re.match(r"^[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}", text):
            return "lumen://search?q=" + urllib.parse.quote(text)
        return "https://" + text

    def _on_tab_changed(self, idx):
        for i in range(self.tab_widget.count()):
            w = self.tab_widget.widget(i)
            if isinstance(w, BrowserTab) and i != idx:
                w.suspend()
        tab = self.current_tab()
        if tab:
            tab.resume()
            self.url_bar.setText(self._display_url(tab.url))
            self._update_nav_buttons()
        import gc
        gc.collect()

    def _display_url(self, url):
        if url.startswith("lumen://"):
            return ""
        return url

    def _update_nav_buttons(self):
        v = self.current_view()
        self.btn_back.setEnabled(v and v.history().canGoBack())
        self.btn_forward.setEnabled(v and v.history().canGoForward())

    def update_url_bar(self, url):
        if not self.url_bar.hasFocus():
            self.url_bar.setText(self._display_url(url))
        self._update_nav_buttons()

    def update_tab_title(self, tab):
        idx = self.tab_widget.indexOf(tab)
        if idx >= 0:
            title = tab.title[:30] if tab.title else "New Tab"
            self.tab_widget.setTabText(idx, title)

    def update_progress(self, value):
        if value < 100:
            self.progress.show()
            self.progress.setValue(value)
        else:
            QTimer.singleShot(500, self.progress.hide)

    def _close_tab(self, idx):
        w = self.tab_widget.widget(idx)
        if isinstance(w, BrowserTab):
            try:
                w.web_view.page().runJavaScript("try{window.stop()}catch(e){}")
            except Exception:
                pass
        if self.tab_widget.count() <= 1:
            self.close()
        else:
            self.tab_widget.removeTab(idx)
            try:
                w.web_view.setHtml("<html></html>")
                w.web_view.page().deleteLater()
                w.web_view.deleteLater()
            except Exception:
                pass

    def _show_menu(self):
        menu = QMenu(self)
        menu.setStyleSheet("""
            QMenu { background-color: #1e2024; color: #e8eaed; border-radius: 12px; padding: 8px; }
            QMenu::item { padding: 8px 32px 8px 20px; border-radius: 8px; }
            QMenu::item:selected { background-color: #3b82f6; }
            QMenu::separator { height: 1px; background: #2c2f34; margin: 4px 12px; }
        """)

        url = self.current_tab().url if self.current_tab() else ""
        bookmarked = self.bookmarks.is_bookmarked(url) if url and url.startswith("http") else False

        menu.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowBack), "Back", self._go_back)
        menu.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_ArrowForward), "Forward", self._go_forward)
        menu.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_BrowserReload), "Reload", self._refresh)
        menu.addSeparator()
        menu.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_DirHomeIcon), "Home", self._go_home)
        menu.addAction(self.style().standardIcon(QStyle.StandardPixmap.SP_FileDialogNewFolder), "New Tab", lambda: self.new_tab())
        if bookmarked:
            menu.addAction("Remove Bookmark", lambda: self._toggle_bookmark(url))
        else:
            menu.addAction("Bookmark This Page", lambda: self._toggle_bookmark(url))
        menu.addAction("Bookmarks", self._show_bookmarks)
        menu.addAction("History", self._show_history)
        menu.addAction("Find in Page", self._find_in_page)
        menu.addSeparator()
        menu.addAction("Share URL", self._share_url)
        menu.addAction("Open in External Browser", self._open_external)
        menu.addSeparator()
        menu.addAction("Add Extension (.xpi/.zip)", self._install_extension)
        menu.addAction("Manage Extensions", self._manage_extensions)
        menu.addSeparator()
        menu.addAction("Settings", self._show_settings)
        menu.addAction("Exit", self.close)

        menu.exec(self.btn_menu.mapToGlobal(self.btn_menu.rect().bottomLeft()))

    def _toggle_bookmark(self, url):
        tab = self.current_tab()
        if self.bookmarks.is_bookmarked(url):
            self.bookmarks.remove(url)
        else:
            self.bookmarks.add({"title": tab.title if tab else url, "url": url})

    def _show_bookmarks(self):
        dlg = QDialog(self)
        dlg.setWindowTitle("Bookmarks")
        dlg.setMinimumSize(500, 400)
        layout = QVBoxLayout(dlg)
        list_widget = QListWidget()
        for item in self.bookmarks.all():
            entry = QListWidgetItem(item.get("title", item["url"]))
            entry.setData(Qt.ItemDataRole.UserRole, item["url"])
            list_widget.addItem(entry)
        list_widget.itemDoubleClicked.connect(lambda i: (self.open_url(i.data(Qt.ItemDataRole.UserRole)), dlg.close()))
        layout.addWidget(list_widget)
        delete_btn = QPushButton("Remove Selected")
        delete_btn.clicked.connect(lambda: self.bookmarks.remove(list_widget.currentItem().data(Qt.ItemDataRole.UserRole)) if list_widget.currentItem() else None)
        layout.addWidget(delete_btn)
        dlg.exec()

    def _show_history(self):
        dlg = QDialog(self)
        dlg.setWindowTitle("History")
        dlg.setMinimumSize(500, 400)
        layout = QVBoxLayout(dlg)
        list_widget = QListWidget()
        for item in self.history.all():
            entry = QListWidgetItem(item.get("title", item["url"]))
            entry.setData(Qt.ItemDataRole.UserRole, item["url"])
            list_widget.addItem(entry)
        list_widget.itemDoubleClicked.connect(lambda i: (self.open_url(i.data(Qt.ItemDataRole.UserRole)), dlg.close()))
        layout.addWidget(list_widget)
        clear_btn = QPushButton("Clear History")
        clear_btn.clicked.connect(lambda: (self.history.clear(), list_widget.clear()))
        layout.addWidget(clear_btn)
        dlg.exec()

    def _find_in_page(self):
        v = self.current_view()
        if v:
            v.page().runJavaScript("""
                (function(){
                    var existing = document.getElementById('__lumen_find');
                    if (existing) { existing.remove(); return; }
                    var bar = document.createElement('div');
                    bar.id = '__lumen_find';
                    bar.style.cssText = 'position:fixed;top:10px;right:10px;z-index:999999;background:#1e2024;color:#e8eaed;padding:8px;border-radius:12px;display:flex;gap:8px;align-items:center;box-shadow:0 4px 16px rgba(0,0,0,.3);';
                    var input = document.createElement('input');
                    input.placeholder='Find'; input.style.cssText='background:#2c2f34;color:#e8eaed;border:1px solid #3a3f44;border-radius:8px;padding:4px 8px;width:200px;';
                    input.oninput=function(){ window.find(input.value); };
                    bar.appendChild(input);
                    var close = document.createElement('button');
                    close.textContent='\\u2715'; close.style.cssText='background:none;border:none;color:#9aa0a6;font-size:18px;cursor:pointer;';
                    close.onclick=function(){ bar.remove(); if(window.getSelection) window.getSelection().removeAllRanges(); };
                    bar.appendChild(close);
                    document.body.appendChild(bar);
                    input.focus();
                })();
            """)

    def _share_url(self):
        url = self.current_tab().url if self.current_tab() else ""
        if url:
            clipboard = QApplication.clipboard()
            clipboard.setText(url)

    def _open_external(self):
        url = self.current_tab().url if self.current_tab() else ""
        if url and url.startswith("http"):
            import subprocess
            subprocess.Popen(["xdg-open", url])

    def _show_settings(self):
        from .settings_dialog import SettingsDialog
        dlg = SettingsDialog(self.settings, self)
        if dlg.exec():
            self.settings = dlg.settings
            save_settings(self.settings)
            self.interceptor.set_settings(self.settings)
            self._apply_theme()
            self.profile.settings().setAttribute(QWebEngineSettings.WebAttribute.JavascriptEnabled, self.settings.get("javascript_enabled", True))
            self.profile.settings().setAttribute(QWebEngineSettings.WebAttribute.AutoLoadImages, self.settings.get("load_images", True))

    def _install_extension(self):
        path, _ = QFileDialog.getOpenFileName(
            self, "Select Extension", "", "Extensions (*.xpi *.zip);;All Files (*)"
        )
        if not path:
            return
        ext = extension_manager.install(path)
        if ext:
            QMessageBox.information(self, "Lumen", "Installed: %s v%s" % (ext.name, ext.version))
        else:
            QMessageBox.warning(self, "Lumen", "Failed to install extension. Make sure it has a valid manifest.json.")

    def _manage_extensions(self):
        dlg = QDialog(self)
        dlg.setWindowTitle("Extensions")
        dlg.setMinimumSize(550, 400)
        layout = QVBoxLayout(dlg)

        exts = extension_manager.all()
        if not exts:
            label = QLabel("No extensions installed.\n\nUse 'Add Extension' to load a .xpi or .zip file.")
            label.setStyleSheet("color: #9aa0a6; padding: 40px; font-size: 14px;")
            layout.addWidget(label)
        else:
            for ext in exts:
                row = QHBoxLayout()
                info = QLabel("%s\nv%s — %s" % (ext.name, ext.version, ext.description or "No description"))
                info.setWordWrap(True)
                info.setStyleSheet("padding: 8px;")
                row.addWidget(info, 1)

                cb = QCheckBox("Enabled")
                cb.setChecked(ext.enabled)
                cb.stateChanged.connect(lambda state, eid=ext.id: extension_manager.set_enabled(eid, state == 2))
                row.addWidget(cb)

                btn_remove = QPushButton("Remove")
                btn_remove.clicked.connect(lambda _, eid=ext.id: (extension_manager.uninstall(eid), dlg.close(), self._manage_extensions()))
                row.addWidget(btn_remove)

                container = QFrame()
                container.setLayout(row)
                container.setStyleSheet("QFrame { border-bottom: 1px solid #2c2f34; }")
                layout.addWidget(container)

        btn_add = QPushButton("Add Extension (.xpi/.zip)")
        btn_add.clicked.connect(lambda: (dlg.close(), self._install_extension()))
        layout.addWidget(btn_add)

        btn_close = QPushButton("Close")
        btn_close.clicked.connect(dlg.close)
        layout.addWidget(btn_close)

        dlg.exec()
