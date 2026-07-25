from PyQt6.QtWidgets import (
    QDialog, QVBoxLayout, QHBoxLayout, QLabel, QCheckBox,
    QLineEdit, QPushButton, QGroupBox, QFormLayout, QMessageBox
)
from PyQt6.QtCore import Qt


class SettingsDialog(QDialog):
    def __init__(self, settings, parent=None):
        super().__init__(parent)
        self.settings = settings.copy()
        self.setWindowTitle("Settings")
        self.setMinimumWidth(500)

        layout = QVBoxLayout(self)

        display_group = QGroupBox("Display")
        display_layout = QVBoxLayout(display_group)
        self.cb_dark = QCheckBox("Force dark mode")
        self.cb_dark.setChecked(settings.get("force_dark", False))
        display_layout.addWidget(self.cb_dark)
        self.cb_images = QCheckBox("Load images")
        self.cb_images.setChecked(settings.get("load_images", True))
        display_layout.addWidget(self.cb_images)
        self.cb_js = QCheckBox("Enable JavaScript")
        self.cb_js.setChecked(settings.get("javascript_enabled", True))
        display_layout.addWidget(self.cb_js)
        layout.addWidget(display_group)

        privacy_group = QGroupBox("Privacy & Security")
        privacy_layout = QVBoxLayout(privacy_group)
        self.cb_ads = QCheckBox("Block ads")
        self.cb_ads.setChecked(settings.get("block_ads", True))
        privacy_layout.addWidget(self.cb_ads)
        self.cb_trackers = QCheckBox("Block trackers")
        self.cb_trackers.setChecked(settings.get("block_trackers", True))
        privacy_layout.addWidget(self.cb_trackers)
        self.cb_webrtc = QCheckBox("Disable WebRTC (prevent IP leak)")
        self.cb_webrtc.setChecked(settings.get("block_webrtc", True))
        privacy_layout.addWidget(self.cb_webrtc)
        self.cb_history = QCheckBox("Save browsing history")
        self.cb_history.setChecked(settings.get("save_history", True))
        privacy_layout.addWidget(self.cb_history)

        from .adblocker import stats
        ads, trackers = stats()
        stats_label = QLabel("Blocked: {} ads, {} trackers".format(ads, trackers))
        stats_label.setStyleSheet("color: #3b82f6; font-weight: bold; padding: 8px;")
        privacy_layout.addWidget(stats_label)

        clear_btn = QPushButton("Clear browsing data")
        clear_btn.clicked.connect(self._clear_data)
        privacy_layout.addWidget(clear_btn)
        layout.addWidget(privacy_group)

        general_group = QGroupBox("General")
        general_form = QFormLayout(general_group)
        self.start_page_input = QLineEdit(settings.get("start_page", "lumen://search"))
        general_form.addRow("Start page:", self.start_page_input)
        layout.addWidget(general_group)

        btn_layout = QHBoxLayout()
        btn_layout.addStretch()
        save_btn = QPushButton("Save")
        save_btn.clicked.connect(self._save)
        btn_layout.addWidget(save_btn)
        cancel_btn = QPushButton("Cancel")
        cancel_btn.clicked.connect(self.reject)
        btn_layout.addWidget(cancel_btn)
        layout.addLayout(btn_layout)

    def _clear_data(self):
        from .stores import HISTORY_FILE, Store
        from .adblocker import reset
        Store(HISTORY_FILE).clear()
        reset()
        QMessageBox.information(self, "Lumen", "Browsing data cleared.")

    def _save(self):
        self.settings["force_dark"] = self.cb_dark.isChecked()
        self.settings["load_images"] = self.cb_images.isChecked()
        self.settings["javascript_enabled"] = self.cb_js.isChecked()
        self.settings["block_ads"] = self.cb_ads.isChecked()
        self.settings["block_trackers"] = self.cb_trackers.isChecked()
        self.settings["block_webrtc"] = self.cb_webrtc.isChecked()
        self.settings["save_history"] = self.cb_history.isChecked()
        sp = self.start_page_input.text().strip()
        if sp:
            self.settings["start_page"] = sp
        self.accept()
