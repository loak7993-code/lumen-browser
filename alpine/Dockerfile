FROM alpine:3.21

RUN echo "https://dl-cdn.alpinelinux.org/alpine/v3.21/community" >> /etc/apk/repositories && \
    apk update -q && \
    apk add --no-cache \
        python3 \
        py3-qt6 \
        py3-pyqt6-webengine \
        py3-pyqt6-sip \
        qt6-qtwebengine \
        mesa-egl \
        mesa-gles \
        libstdc++ \
        fontconfig \
        freetype \
        ttf-dejavu \
        dbus \
        && rm -rf /var/cache/apk/*

COPY src/lumen.py /usr/lib/lumen/lumen.py
COPY src/assets /usr/lib/lumen/assets
COPY resources/lumen.svg /usr/share/icons/hicolor/256x256/apps/lumen.svg

RUN mkdir -p /usr/share/applications /usr/share/icons/hicolor/256x256/apps && \
    printf '#!/bin/sh\ncd /usr/lib/lumen\nexec python3 lumen.py "$@"\n' > /usr/bin/lumen && \
    chmod +x /usr/bin/lumen && \
    printf '[Desktop Entry]\nName=Lumen\nComment=Lightweight private browser\nExec=/usr/bin/lumen %%u\nIcon=lumen\nTerminal=false\nType=Application\nCategories=Network;WebBrowser;\n' \
    > /usr/share/applications/lumen.desktop

CMD ["lumen"]
