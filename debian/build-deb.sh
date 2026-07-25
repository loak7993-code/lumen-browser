#!/bin/bash
set -e

VERSION="1.0.0"
ARCH="all"
PKG_DIR="/tmp/lumen-deb"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Building lumen_${VERSION}_${ARCH}.deb for Debian Bookworm..."

rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR/DEBIAN"
mkdir -p "$PKG_DIR/usr/lib/lumen"
mkdir -p "$PKG_DIR/usr/bin"
mkdir -p "$PKG_DIR/usr/share/applications"
mkdir -p "$PKG_DIR/usr/share/icons/hicolor/256x256/apps"

cp -r "$SCRIPT_DIR/src/lumen" "$PKG_DIR/usr/lib/lumen/"

cat > "$PKG_DIR/usr/bin/lumen" << 'EOF'
#!/bin/bash
cd /usr/lib/lumen
exec python3 -m lumen "$@"
EOF
chmod +x "$PKG_DIR/usr/bin/lumen"

cp "$SCRIPT_DIR/resources/lumen.svg" "$PKG_DIR/usr/share/icons/hicolor/256x256/apps/lumen.svg"

cat > "$PKG_DIR/usr/share/applications/lumen.desktop" << EOF
[Desktop Entry]
Name=Lumen
Comment=Lightweight private browser
Exec=/usr/bin/lumen %u
Icon=lumen
Terminal=false
Type=Application
Categories=Network;WebBrowser;
StartupWMClass=Lumen
MimeType=text/html;text/xml;application/xhtml+xml;application/xml;x-scheme-handler/http;x-scheme-handler/https;
EOF

cat > "$PKG_DIR/DEBIAN/control" << EOF
Package: lumen
Version: $VERSION
Section: web
Priority: optional
Architecture: $ARCH
Depends: python3 (>= 3.11), python3-pyqt6, python3-pyqt6.qtwebengine
Maintainer: Lumen <lumen@localhost>
Description: Lumen - Lightweight private browser for Debian Bookworm
 A minimal, fast, privacy-focused web browser built with PyQt6 and QtWebEngine.
 .
 Features: ad/tracker blocking, WebRTC disable, custom meta-search engine,
 tabs, bookmarks, history, dark mode, extension (content-script) support.
EOF

cat > "$PKG_DIR/DEBIAN/postinst" << 'EOF'
#!/bin/bash
update-desktop-database -q 2>/dev/null || true
gtk-update-icon-cache -q /usr/share/icons/hicolor 2>/dev/null || true
EOF
chmod +x "$PKG_DIR/DEBIAN/postinst"

dpkg-deb --build "$PKG_DIR" "$SCRIPT_DIR/lumen_${VERSION}_${ARCH}.deb"
rm -rf "$PKG_DIR"

echo "Built: $SCRIPT_DIR/lumen_${VERSION}_${ARCH}.deb"
echo "Install with: sudo dpkg -i lumen_${VERSION}_${ARCH}.deb"
