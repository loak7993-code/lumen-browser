#!/bin/sh
set -e

INSTALL_DIR="$HOME/.local/share/lumen"
BIN_DIR="$HOME/.local/bin"
APPS_DIR="$HOME/.local/share/applications"
ICONS_DIR="$HOME/.local/share/icons/hicolor/256x256/apps"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Lumen Browser - Alpine Installer ==="
echo ""

echo "[1/4] Checking dependencies..."

MISSING=""
for pkg in python3 py3-qt6 py3-pyqt6-webengine; do
	if ! apk -e info "$pkg" >/dev/null 2>&1; then
		MISSING="$MISSING $pkg"
	fi
done

if [ -n "$MISSING" ]; then
	if ! grep -q "community" /etc/apk/repositories 2>/dev/null; then
		echo "  Enabling community repository..."
		VER=$(cat /etc/alpine-release 2>/dev/null | cut -d. -f1,2)
		if [ -z "$VER" ]; then
			echo "  https://dl-cdn.alpinelinux.org/alpine/v3.21/community" >> /etc/apk/repositories
		else
			echo "  https://dl-cdn.alpinelinux.org/alpine/v${VER}/community" >> /etc/apk/repositories
		fi
		apk update -q
	fi
	echo "  Installing:$MISSING"
	su root -c "apk add$MISSING" 2>/dev/null || sudo apk add$MISSING || apk add$MISSING
	echo "  Done."
else
	echo "  All dependencies satisfied."
fi

echo ""
echo "[2/4] Installing to $INSTALL_DIR..."
mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$APPS_DIR" "$ICONS_DIR"
cp "$SCRIPT_DIR/src/lumen.py" "$INSTALL_DIR/lumen.py"
cp -r "$SCRIPT_DIR/src/assets" "$INSTALL_DIR/assets"

cat > "$BIN_DIR/lumen" << 'EOF'
#!/bin/sh
cd "$HOME/.local/share/lumen"
exec python3 lumen.py "$@"
EOF
chmod +x "$BIN_DIR/lumen"
echo "  Binary: $BIN_DIR/lumen"

echo ""
echo "[3/4] Installing desktop entry..."
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

echo ""
echo "[4/4] Done!"
echo ""
echo "  Run:  lumen"
echo "  Or find 'Lumen' in your app menu"
