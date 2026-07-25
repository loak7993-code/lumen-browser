#!/usr/bin/env python3
import sys
import os

_src = os.path.join(os.path.dirname(__file__), "..", "src")
if os.path.isdir(_src):
    sys.path.insert(0, _src)

os.environ.setdefault("QTWEBENGINE_DISABLE_SANDBOX", "1")

CHROMIUM_FLAGS = " ".join([
    "--no-sandbox",
    "--single-process",
    "--disable-gpu",
    "--disable-gpu-compositing",
    "--disable-software-rasterizer",
    "--disable-extensions",
    "--disable-plugins",
    "--disable-notifications",
    "--disable-geolocation",
    "--disable-media-stream",
    "--disable-pepper-3d",
    "--disable-accelerated-2d-canvas",
    "--disable-accelerated-video-decode",
    "--disable-smooth-scrolling",
    "--renderer-process-limit=1",
    "--disable-background-networking",
    "--disable-background-timer-throttling",
    "--disable-renderer-backgrounding",
    "--disable-backgrounding-occluded-windows",
    "--disable-ipc-flooding-protection",
    "--memory-pressure-off",
    "--disk-cache-size=10485760",
    "--disable-default-apps",
    "--disable-features=TranslateUI,BlinkGenPropertyTrees,InstalledApp,BackgroundFetch,WebOTP,WebPayments,WebUSB,PictureInPicture,AutofillServerCommunication,CalculateNativeWinOcclusion",
    "--enable-features=NetworkServiceInProcess,LazyFrameLoading",
    "--low-end-device-mode",
])
os.environ.setdefault("QTWEBENGINE_CHROMIUM_FLAGS", CHROMIUM_FLAGS)

from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import QApplication
QApplication.setAttribute(Qt.ApplicationAttribute.AA_ShareOpenGLContexts)

from PyQt6.QtWebEngineCore import QWebEngineCore
from lumen.browser import BrowserWindow


def main():
    app = QApplication(sys.argv)
    app.setApplicationName("Lumen")
    app.setOrganizationName("Lumen")
    app.setApplicationDisplayName("Lumen Browser")

    window = BrowserWindow()
    window.show()

    sys.exit(app.exec())


if __name__ == "__main__":
    main()
