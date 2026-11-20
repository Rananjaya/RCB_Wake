#!/usr/bin/env python3
"""Generates RCB Wake's weather icons as Android vector drawables.

All geometry is drawn here from simple primitives (scalloped sun, union-of-circles cloud,
crescent moon, drops, flakes, bolt), so the icons are original work under the project licence.

Usage:
    python3 scripts/generate-weather-icons.py [preview.svg]

Writes android/app/src/main/res/drawable/ic_wx_*.xml and, optionally, an SVG contact sheet.
"""
import math
import os
import sys

PREVIEW = sys.argv[1] if len(sys.argv) > 1 else None
OUT = "android/app/src/main/res/drawable"
VIEW = 48


def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def circle(cx, cy, r):
    # Clockwise, matching rounded_rect, so overlapping subpaths union under non-zero filling.
    return f"M{f(cx - r)},{f(cy)}a{f(r)},{f(r)} 0 1,1 {f(2 * r)},0a{f(r)},{f(r)} 0 1,1 {f(-2 * r)},0z"


def rounded_rect(x, y, w, h, r):
    return (
        f"M{f(x + r)},{f(y)}h{f(w - 2 * r)}a{f(r)},{f(r)} 0 0,1 {f(r)},{f(r)}v{f(h - 2 * r)}"
        f"a{f(r)},{f(r)} 0 0,1 {f(-r)},{f(r)}h{f(-(w - 2 * r))}a{f(r)},{f(r)} 0 0,1 {f(-r)},{f(-r)}"
        f"v{f(-(h - 2 * r))}a{f(r)},{f(r)} 0 0,1 {f(r)},{f(-r)}z"
    )


def scalloped(cx, cy, radius, bumps=12, depth=0.09, steps=192):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        r = radius * (1 + depth * math.cos(bumps * t))
        pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    return "M" + "L".join(f"{f(x)},{f(y)}" for x, y in pts) + "z"


def cloud(x, y, w):
    """A cloud as one path of overlapping subpaths; non-zero filling unions them seamlessly."""
    s = w / 40
    return " ".join(
        [
            rounded_rect(x, y + 10 * s, 40 * s, 14 * s, 7 * s),
            circle(x + 12 * s, y + 12 * s, 8 * s),
            circle(x + 24 * s, y + 8 * s, 10.5 * s),
            circle(x + 33 * s, y + 15 * s, 7 * s),
        ]
    )


def crescent(cx, cy, r):
    """Outer circle minus an offset inner circle, traced along their intersection points."""
    ox, oy, r2 = cx + 0.42 * r, cy - 0.36 * r, 0.82 * r
    d = math.hypot(ox - cx, oy - cy)
    a = (r * r - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(r * r - a * a)
    mx, my = cx + a * (ox - cx) / d, cy + a * (oy - cy) / d
    p1 = (mx + h * (oy - cy) / d, my - h * (ox - cx) / d)
    p2 = (mx - h * (oy - cy) / d, my + h * (ox - cx) / d)
    return (
        f"M{f(p1[0])},{f(p1[1])}A{f(r)},{f(r)} 0 1,1 {f(p2[0])},{f(p2[1])}"
        f"A{f(r2)},{f(r2)} 0 0,0 {f(p1[0])},{f(p1[1])}z"
    )


def drops(xs, y):
    return " ".join(f"M{f(x + 1.6)},{f(y)}l-2.6,6" for x in xs)


def flake(cx, cy, r):
    parts = []
    for k in range(3):
        a = math.radians(90 + 60 * k)
        dx, dy = r * math.cos(a), r * math.sin(a)
        parts.append(f"M{f(cx - dx)},{f(cy - dy)}L{f(cx + dx)},{f(cy + dy)}")
    return " ".join(parts)


def bolt(x, y):
    return f"M{f(x + 6)},{f(y)}L{f(x)},{f(y + 10)}h5l-3,9 9,-12h-5l3,-7z"


def fog_lines(y):
    return f"M8,{f(y)}h24M14,{f(y + 5)}h26M10,{f(y + 10)}h18"


GRADIENTS = {
    "sun": ("#FFE082", "#FFB300", (10, 8, 28, 28)),
    "moon": ("#F3EEFF", "#C4B5F4", (12, 6, 30, 28)),
    "cloud": ("#FAFAFC", "#B9BEC8", (8, 14, 44, 40)),
    "cloud_dark": ("#D5DAE2", "#8F97A6", (8, 14, 44, 40)),
    "bolt": ("#FFE57F", "#FF9800", (20, 30, 34, 48)),
}

# Each element: (kind, path, paint, extra)
#   kind "fill": paint is gradient key or colour; extra = alpha
#   kind "stroke": paint is colour; extra = width
SUN = ("fill", scalloped(17, 17, 9.5), "sun", 1)
SUN_SMALL = ("fill", scalloped(16, 15, 8.5), "sun", 1)
MOON = ("fill", crescent(18, 17, 10), "moon", 1)
MOON_SMALL = ("fill", crescent(16, 15, 8.5), "moon", 1)
CLOUD_FRONT = ("fill", cloud(6, 14, 38), "cloud", 0.94)
CLOUD_LOW = ("fill", cloud(5, 12, 38), "cloud", 0.94)
RAIN = ("stroke", drops([15, 23, 31], 39), "#7CC4FA", 2.6)
SNOW = ("stroke", flake(16, 41, 3.6) + " " + flake(28, 43, 3.6), "#E8EEF6", 2)

ICONS = {
    "ic_wx_clear_day": [("fill", scalloped(24, 24, 15), "sun", 1)],
    "ic_wx_clear_night": [("fill", crescent(24, 24, 15), "moon", 1)],
    "ic_wx_partly_day": [SUN, CLOUD_FRONT],
    "ic_wx_partly_night": [MOON, CLOUD_FRONT],
    "ic_wx_cloudy": [("fill", cloud(14, 8, 30), "cloud_dark", 0.9), CLOUD_FRONT],
    "ic_wx_fog": [CLOUD_LOW, ("stroke", fog_lines(37), "#D8DEE8", 2.6)],
    "ic_wx_rain": [CLOUD_LOW, RAIN],
    "ic_wx_snow": [CLOUD_LOW, SNOW],
    "ic_wx_thunder": [("fill", cloud(5, 8, 38), "cloud_dark", 0.96), ("fill", bolt(22, 28), "bolt", 1)],
    "ic_wx_showers_day": [SUN_SMALL, CLOUD_LOW, RAIN],
    "ic_wx_showers_night": [MOON_SMALL, CLOUD_LOW, RAIN],
    "ic_wx_snow_showers_day": [SUN_SMALL, CLOUD_LOW, SNOW],
    "ic_wx_snow_showers_night": [MOON_SMALL, CLOUD_LOW, SNOW],
}


def android_element(kind, d, paint, extra):
    if kind == "stroke":
        return (
            f'    <path android:pathData="{d}" android:strokeColor="{paint}" '
            f'android:strokeWidth="{extra}" android:strokeLineCap="round" />\n'
        )
    alpha = f' android:fillAlpha="{extra}"' if extra != 1 else ""
    if paint.startswith("#"):
        return f'    <path android:pathData="{d}" android:fillColor="{paint}"{alpha} />\n'
    start, end, (x1, y1, x2, y2) = GRADIENTS[paint]
    return (
        f'    <path android:pathData="{d}"{alpha}>\n'
        f'        <aapt:attr name="android:fillColor">\n'
        f'            <gradient android:type="linear" android:startX="{x1}" android:startY="{y1}" '
        f'android:endX="{x2}" android:endY="{y2}" android:startColor="{start}" android:endColor="{end}" />\n'
        f"        </aapt:attr>\n"
        f"    </path>\n"
    )


def svg_element(kind, d, paint, extra, uid):
    if kind == "stroke":
        return f'<path d="{d}" fill="none" stroke="{paint}" stroke-width="{extra}" stroke-linecap="round"/>'
    if paint.startswith("#"):
        return f'<path d="{d}" fill="{paint}" fill-opacity="{extra}"/>'
    start, end, (x1, y1, x2, y2) = GRADIENTS[paint]
    return (
        f'<defs><linearGradient id="{uid}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">'
        f'<stop offset="0" stop-color="{start}"/><stop offset="1" stop-color="{end}"/></linearGradient></defs>'
        f'<path d="{d}" fill="url(#{uid})" fill-opacity="{extra}"/>'
    )


cells = []
for i, (name, elements) in enumerate(ICONS.items()):
    body = "".join(android_element(*e) for e in elements)
    with open(os.path.join(OUT, f"{name}.xml"), "w") as out:
        out.write(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            "<!-- Generated by scripts/generate-weather-icons.py. Original artwork, project licence. -->\n"
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="24dp"\n    android:height="24dp"\n'
            f'    android:viewportWidth="{VIEW}"\n    android:viewportHeight="{VIEW}">\n{body}</vector>\n'
        )
    if PREVIEW:
        inner = "".join(svg_element(*e, uid=f"g{i}_{j}") for j, e in enumerate(elements))
        col, row = i % 5, i // 5
        cells.append(
            f'<g transform="translate({col * 120 + 10},{row * 130 + 10})">'
            f'<rect width="110" height="110" rx="10" fill="#1d2440"/>'
            f'<g transform="translate(7,5) scale(2)">{inner}</g>'
            f'<text x="55" y="122" font-size="8" fill="#ccc" text-anchor="middle" font-family="sans-serif">{name[6:]}</text></g>'
        )

if PREVIEW:
    with open(PREVIEW, "w") as out:
        out.write(
            f'<svg xmlns="http://www.w3.org/2000/svg" width="610" height="400" viewBox="0 0 610 400">'
            f'<rect width="100%" height="100%" fill="#10142a"/>{"".join(cells)}</svg>'
        )
print(f"wrote {len(ICONS)} icons")
