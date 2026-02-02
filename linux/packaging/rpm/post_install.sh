#!/bin/sh

if [ -x /usr/bin/glib-compile-schemas ]; then
    glib-compile-schemas /usr/share/glib-2.0/schemas/ || true
fi

if [ -x /usr/bin/gtk-update-icon-cache ]; then
    gtk-update-icon-cache -f -t /usr/share/icons/hicolor || true
fi

if [ -x /usr/bin/update-desktop-database ]; then
    update-desktop-database /usr/share/applications || true
fi
