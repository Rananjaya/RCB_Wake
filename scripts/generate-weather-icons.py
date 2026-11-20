#!/usr/bin/env python3
"""Generates RCB Wake's 3D-style weather icons as Android vector drawables.

Shapes come from Google's Material Symbols (rounded, filled; Apache License 2.0), installed with
`npm pack @material-symbols/svg-400` and extracted to a folder. Gradients, shading and
composition are RCB Wake's own.

Usage:
    python3 scripts/generate-weather-icons.py <path-to-material-symbols-package> [preview.svg]

Writes android/app/src/main/res/drawable/ic_wx_*.xml and, optionally, an SVG contact sheet.
"""
import os
import re
import sys

SYMBOLS = sys.argv[1]
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
OUT = "android/app/src/main/res/drawable"

# Gradients are expressed in Material Symbols' own coordinate space (x 0..960, y -960..0).
GRADIENTS = {
    "sun": ("radial", dict(cx=430, cy=-530, r=360), ["#FFF6B0", "#FFC93C", "#FF9F1C"]),
    "moon": ("radial", dict(cx=380, cy=-600, r=420), ["#FFFBE6", "#FFE08A", "#F2B84B"]),
    "cloud": ("linear", dict(x1=480, y1=-760, x2=480, y2=-200), ["#FFFFFF", "#EEF3F9", "#C3D0DE"]),
    "storm_cloud": ("linear", dict(x1=480, y1=-760, x2=480, y2=-200), ["#C9D3DF", "#95A3B5", "#6B7A8F"]),
    "back_cloud": ("linear", dict(x1=480, y1=-760, x2=480, y2=-200), ["#DCE4EE", "#B4C2D3", "#93A3B8"]),
    "drop": ("linear", dict(x1=480, y1=-880, x2=480, y2=-80), ["#9BE7FF", "#3FB6F2", "#1C7ED6"]),
    "snow": ("linear", dict(x1=480, y1=-880, x2=480, y2=-80), ["#FFFFFF", "#D6F1FF", "#9AD4F5"]),
    "bolt": ("linear", dict(x1=480, y1=-900, x2=480, y2=-60), ["#FFF3A0", "#FFC93C", "#FF8A00"]),
    "fog": ("linear", dict(x1=0, y1=-480, x2=960, y2=-480), ["#E8EEF5", "#C8D3E0", "#E8EEF5"]),
}

SHADOW = "#40000000"  # soft drop shadow under each shape

# (symbol, gradient, scale, translateX, translateY, shadow)
ICONS = {
    "ic_wx_clear_day": [("sunny", "sun", 1.0, 0, 0, True)],
    "ic_wx_clear_night": [("bedtime", "moon", 0.95, 30, 20, True)],
    "ic_wx_partly_day": [("sunny", "sun", 0.62, 0, 20, False), ("cloud", "cloud", 0.82, 175, 215, True)],
    "ic_wx_partly_night": [("bedtime", "moon", 0.55, 30, 50, False), ("cloud", "cloud", 0.82, 175, 215, True)],
    "ic_wx_cloudy": [("cloud", "back_cloud", 0.62, 330, 90, False), ("cloud", "cloud", 0.85, 70, 230, True)],
    "ic_wx_fog": [("cloud", "cloud", 0.7, 145, 40, True), ("mist", "fog", 0.6, 190, 480, False)],
    "ic_wx_rain": [
        ("cloud", "cloud", 0.8, 95, 0, True),
        ("water_drop", "drop", 0.3, 150, 600, False),
        ("water_drop", "drop", 0.3, 335, 640, False),
        ("water_drop", "drop", 0.3, 520, 600, False),
    ],
    "ic_wx_snow": [
        ("cloud", "cloud", 0.8, 95, 0, True),
        ("ac_unit", "snow", 0.28, 250, 650, False),
        ("ac_unit", "snow", 0.28, 560, 650, False),
    ],
    "ic_wx_thunder": [("cloud", "storm_cloud", 0.8, 95, 0, True), ("bolt", "bolt", 0.5, 240, 450, True)],
}

SHADOW_OFFSET = (18, 30)


def paths(name):
    with open(os.path.join(SYMBOLS, "rounded", f"{name}-fill.svg")) as f:
        return re.findall(r'<path d="([^"]+)"', f.read())


def android_gradient(key):
    kind, g, colors = GRADIENTS[key]
    items = "".join(
        f'\n                        <item android:color="{c}" android:offset="{i / (len(colors) - 1):.2f}" />'
        for i, c in enumerate(colors)
    )
    if kind == "radial":
        attrs = f'android:type="radial" android:centerX="{g["cx"]}" android:centerY="{g["cy"]}" android:gradientRadius="{g["r"]}"'
    else:
        attrs = f'android:type="linear" android:startX="{g["x1"]}" android:startY="{g["y1"]}" android:endX="{g["x2"]}" android:endY="{g["y2"]}"'
    return f"""
                <aapt:attr name="android:fillColor">
                    <gradient {attrs}>{items}
                    </gradient>
                </aapt:attr>"""


def android_part(symbol, grad, s, tx, ty, shadow):
    out = ""
    for d in paths(symbol):
        if shadow:
            out += f"""
    <group android:scaleX="{s}" android:scaleY="{s}" android:translateX="{tx + SHADOW_OFFSET[0] * s}" android:translateY="{ty + SHADOW_OFFSET[1] * s}">
        <group android:translateY="960">
            <path android:fillColor="{SHADOW}" android:pathData="{d}" />
        </group>
    </group>"""
        out += f"""
    <group android:scaleX="{s}" android:scaleY="{s}" android:translateX="{tx}" android:translateY="{ty}">
        <group android:translateY="960">
            <path android:pathData="{d}">{android_gradient(grad)}
            </path>
        </group>
    </group>"""
    return out


def svg_part(symbol, grad, s, tx, ty, shadow, uid):
    kind, g, colors = GRADIENTS[grad]
    stops = "".join(f'<stop offset="{i / (len(colors) - 1):.2f}" stop-color="{c}"/>' for i, c in enumerate(colors))
    if kind == "radial":
        gdef = f'<radialGradient id="{uid}" gradientUnits="userSpaceOnUse" cx="{g["cx"]}" cy="{g["cy"]}" r="{g["r"]}">{stops}</radialGradient>'
    else:
        gdef = f'<linearGradient id="{uid}" gradientUnits="userSpaceOnUse" x1="{g["x1"]}" y1="{g["y1"]}" x2="{g["x2"]}" y2="{g["y2"]}">{stops}</linearGradient>'
    out = f"<defs>{gdef}</defs>"
    for d in paths(symbol):
        if shadow:
            out += f'<g transform="translate({tx + SHADOW_OFFSET[0] * s},{ty + SHADOW_OFFSET[1] * s}) scale({s}) translate(0,960)"><path fill="#000" fill-opacity="0.25" d="{d}"/></g>'
        out += f'<g transform="translate({tx},{ty}) scale({s}) translate(0,960)"><path fill="url(#{uid})" d="{d}"/></g>'
    return out


cells = []
for i, (name, parts) in enumerate(ICONS.items()):
    symbols = sorted({p[0] for p in parts})
    body = "".join(android_part(*p) for p in parts)
    xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/generate-weather-icons.py. Shapes: Material Symbols ({", ".join(symbols)})
     by Google, Apache License 2.0. Shading and composition are RCB Wake's own. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960">{body}
</vector>
"""
    with open(os.path.join(OUT, f"{name}.xml"), "w") as f:
        f.write(xml)
    if PREVIEW:
        inner = "".join(svg_part(*p, uid=f"g{i}_{j}") for j, p in enumerate(parts))
        col, row = i % 3, i // 3
        cells.append(
            f'<g transform="translate({col * 1100 + 70},{row * 1150 + 70})">'
            f'<rect x="-40" y="-40" width="1040" height="1040" rx="60" fill="#2c3a5c"/>{inner}'
            f'<text x="480" y="1060" font-size="70" fill="#fff" text-anchor="middle" font-family="sans-serif">{name[6:]}</text></g>'
        )

if PREVIEW:
    with open(PREVIEW, "w") as f:
        f.write(
            '<svg xmlns="http://www.w3.org/2000/svg" width="3300" height="3450" viewBox="0 0 3300 3450">'
            f'<rect width="100%" height="100%" fill="#141a30"/>{"".join(cells)}</svg>'
        )
print(f"wrote {len(ICONS)} icons")
