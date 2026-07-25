#!/bin/bash
set -e

INSTALL_DIR="$HOME/.local/share/lumen"
BIN_DIR="$HOME/.local/bin"
APPS_DIR="$HOME/.local/share/applications"
ICONS_DIR="$HOME/.local/share/icons/hicolor/256x256/apps"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Lumen Browser - Debian Bookworm Installer ==="
echo ""

echo "[1/4] Checking system dependencies..."

MISSING=""

check_pkg() {
    if ! dpkg -s "$1" >/dev/null 2>&1; then
        MISSING="$MISSING $1"
    fi
}

check_pkg python3
check_pkg python3-pyqt6
check_pkg python3-pyqt6.qtwebengine

if [ -n "$MISSING" ]; then
    echo "  Missing packages:$MISSING"
    echo "  Installing via apt (requires sudo)..."
    sudo apt-get update -qq
    sudo apt-get install -y -qq$MISSING
    echo "  Done."
else
    echo "  All dependencies satisfied."
fi

echo ""
echo "[2/4] Installing Lumen to $INSTALL_DIR..."

mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$APPS_DIR" "$ICONS_DIR"

cp -r "$SCRIPT_DIR/src/lumen" "$INSTALL_DIR/"

cat > "$BIN_DIR/lumen" << 'WRAPPER'
#!/bin/bash
export QTWEBENGINE_DISABLE_SANDBOX="${QTWEBENGINE_DISABLE_SANDBOX:-0}"
export QTWEBENGINE_CHROMIUM_FLAGS="${QTWEBENGINE_CHROMIUM_FLAGS}"
cd "$HOME/.local/share/lumen"
exec python3 -m lumen "$@"
WRAPPER
chmod +x "$BIN_DIR/lumen"

echo "  Installed binary to $BIN_DIR/lumen"

echo ""
echo "[3/4] Installing desktop icon..."

cp "$SCRIPT_DIR/resources/lumen.svg" "$ICONS_DIR/lumen.svg"

cat > "$APPS_DIR/lumen.desktop" << EOF
[Desktop Entry]
Name=Lumen
Comment=Lightweight private browser
Exec=$BIN_DIR/lumen %u
Icon=lumen
Terminal=false
Type=Application
Categories=Network;WebBrowser;
StartupWMClass=Lumen
MimeType=text/html;text/xml;application/xhtml+xml;application/xml;x-scheme-handler/http;x-scheme-handler/https;
EOF

echo "  Installed .desktop file to $APPS_DIR/lumen.desktop"

echo ""
echo "[4/4] Done!"
echo ""
echo "  Launch with:  lumen"
echo "  Or find it in your application menu under 'Lumen'"
echo ""
echo "  Debian Bookworm packages used:"
echo "    python3-pyqt6"
echo "    python3-pyqt6.qtwebengine"
