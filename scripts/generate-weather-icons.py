#!/usr/bin/env python3
"""Generates RCB Wake's weather icons as Android vector drawables (with gradients).

Original artwork drawn here from simple geometry — a scalloped sun, a two-lobe cloud, a crescent
moon, teardrops, asterisk snowflakes and a lightning bolt — in a soft, colourful, flat-gradient
style. No third-party icon is copied or traced.

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

# Palette
SUN = ("#F9D45C", "#EDA23C")          # top -> bottom
MOON = ("#A9C8FF", "#3566E0")         # top-right -> bottom-left
CLOUD_LIGHT = ("#FFFFFF", "#E3E4EA")  # cloud in front of the sun or moon
CLOUD_GREY = ("#F1F1F3", "#B5B6BC")   # rain / snow cloud
DROP = ("#4D8BF5", "#1F4FC9")
BOLT = ("#FBD24B", "#F08A24")
SNOW = "#7A7B80"
FOG = "#9C9DA3"


def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


# ---------------------------------------------------------------- geometry

def circle(cx, cy, r):
    return f"M{f(cx - r)},{f(cy)}a{f(r)},{f(r)} 0 1,1 {f(2 * r)},0a{f(r)},{f(r)} 0 1,1 {f(-2 * r)},0z"


def rounded_rect(x, y, w, h, r):
    return (
        f"M{f(x + r)},{f(y)}h{f(w - 2 * r)}a{f(r)},{f(r)} 0 0,1 {f(r)},{f(r)}v{f(h - 2 * r)}"
        f"a{f(r)},{f(r)} 0 0,1 {f(-r)},{f(r)}h{f(-(w - 2 * r))}a{f(r)},{f(r)} 0 0,1 {f(-r)},{f(-r)}"
        f"v{f(-(h - 2 * r))}a{f(r)},{f(r)} 0 0,1 {f(r)},{f(-r)}z"
    )


def scalloped(cx, cy, radius, bumps=12, depth=0.055, steps=240):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        r = radius * (1 + depth * math.cos(bumps * t))
        pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    return "M" + "L".join(f"{f(x)},{f(y)}" for x, y in pts) + "z"


def cloud(x, y, w):
    """Flat-bottomed cloud: a small left lobe and a large dome on the right.
    Bounding box: x..x+w horizontally, y..y+0.6w vertically."""
    s = w / 40
    return " ".join([
        rounded_rect(x, y + 12 * s, 40 * s, 12 * s, 6 * s),
        circle(x + 11 * s, y + 15 * s, 8 * s),
        circle(x + 25 * s, y + 12 * s, 12 * s),
    ])


def cloud_box(x, y, w):
    return (x, y, x + w, y + 0.6 * w)


def crescent(cx, cy, r):
    ox, oy, r2 = cx + 0.48 * r, cy - 0.34 * r, 0.84 * r
    d = math.hypot(ox - cx, oy - cy)
    a = (r * r - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(r * r - a * a)
    mx, my = cx + a * (ox - cx) / d, cy + a * (oy - cy) / d
    p1 = (mx + h * (oy - cy) / d, my - h * (ox - cx) / d)
    p2 = (mx - h * (oy - cy) / d, my + h * (ox - cx) / d)
    return (
        f"M{f(p1[0])},{f(p1[1])}A{f(r)},{f(r)} 0 1,0 {f(p2[0])},{f(p2[1])}"
        f"A{f(r2)},{f(r2)} 0 0,1 {f(p1[0])},{f(p1[1])}z"
    )


def drop(cx, cy, r):
    """Teardrop with its point up; (cx, cy) is the centre of the round part."""
    d = 1.75 * r
    b = math.acos(r / d)
    tx, ty = r * math.sin(b), -r * math.cos(b)
    return (
        f"M{f(cx)},{f(cy - d)}L{f(cx + tx)},{f(cy + ty)}"
        f"A{f(r)},{f(r)} 0 1,1 {f(cx - tx)},{f(cy + ty)}z"
    )


def asterisk(cx, cy, r):
    parts = []
    for deg in (90, 30, 150):
        a = math.radians(deg)
        dx, dy = r * math.cos(a), r * math.sin(a)
        parts.append(f"M{f(cx - dx)},{f(cy - dy)}L{f(cx + dx)},{f(cy + dy)}")
    return "".join(parts)


def bolt(x, y, w, h):
    pts = [(0.62, 0.0), (0.08, 0.56), (0.44, 0.56), (0.26, 1.0), (0.94, 0.38), (0.56, 0.38), (0.80, 0.0)]
    return "M" + "L".join(f"{f(x + px * w)},{f(y + py * h)}" for px, py in pts) + "z"


def waves(x, y, w, rows, gap):
    parts = []
    seg = w / 4
    for i in range(rows):
        yy = y + i * gap
        xs = x + (seg / 2 if i % 2 else 0)
        n = 4 if i % 2 == 0 else 3
        d = f"M{f(xs)},{f(yy)}"
        for k in range(n):
            up = -1.4 if k % 2 == 0 else 1.4
            d += f"q{f(seg / 2)},{f(up)} {f(seg)},0"
        parts.append(d)
    return "".join(parts)


# ---------------------------------------------------------------- shapes (fill/gradient/stroke)

def grad(colors, x1, y1, x2, y2):
    return {"grad": (colors, x1, y1, x2, y2)}


def sun_shape(cx, cy, r):
    return {"d": scalloped(cx, cy, r), **grad(SUN, cx, cy - r, cx, cy + r)}


def moon_shape(cx, cy, r):
    return {"d": crescent(cx, cy, r), **grad(MOON, cx + r, cy - r, cx - r, cy + r)}


def cloud_shape(x, y, w, colors, alpha=1.0):
    x1, y1, x2, y2 = cloud_box(x, y, w)
    return {"d": cloud(x, y, w), **grad(colors, x1, y1, x1 + 0.2 * w, y2), "alpha": alpha}


def drops_shape(points, r=2.7):
    return {"d": " ".join(drop(x, y, r) for x, y in points), **grad(DROP, 0, points[0][1] - 4, 0, points[-1][1] + 3)}


def snow_shape(points, r=3.4):
    return {"d": "".join(asterisk(x, y, r) for x, y in points), "stroke": SNOW, "width": 1.6}


def bolt_shape(x, y, w, h):
    return {"d": bolt(x, y, w, h), **grad(BOLT, x, y, x, y + h)}


def fog_shape():
    return {"d": waves(7, 15, 34, 4, 6.5), "stroke": FOG, "width": 2.6}


# Layouts on a 48-unit canvas.
def celestial(night, cx, cy, r):
    return moon_shape(cx, cy, r) if night else sun_shape(cx, cy, r)


def big_body(night):
    return [moon_shape(25, 24, 17)] if night else [sun_shape(24, 24, 17)]


def mostly_clear(night):
    return [celestial(night, 21, 21, 15), cloud_shape(23, 27, 22, CLOUD_LIGHT, 0.97)]


def partly(night):
    return [celestial(night, 18, 18, 13), cloud_shape(12, 20, 33, CLOUD_LIGHT, 0.95)]


def peek_cloud(night):
    """Small sun or moon peeking out from behind a grey rain cloud."""
    return [celestial(night, 15, 14, 9), cloud_shape(8, 9, 36, CLOUD_GREY)]


GENERAL_CLOUD = cloud_shape(6, 6, 36, CLOUD_GREY)
RAIN = [(17, 29), (31, 29), (24, 38)]
HEAVY = [(12, 29), (24, 29), (36, 29), (18, 38), (30, 38)]

ICONS = {
    "clear_day": big_body(False),
    "clear_night": big_body(True),
    "mostly_clear_day": mostly_clear(False),
    "mostly_clear_night": mostly_clear(True),
    "partly_day": partly(False),
    "partly_night": partly(True),
    "cloudy": [cloud_shape(4, 12, 40, CLOUD_GREY)],
    "fog": [fog_shape()],
    "drizzle": [GENERAL_CLOUD, drops_shape([(24, 36)])],
    "rain": [GENERAL_CLOUD, drops_shape(RAIN)],
    "heavy_rain": [GENERAL_CLOUD, drops_shape(HEAVY, 2.5)],
    "showers_day": peek_cloud(False) + [drops_shape([(18, 31), (32, 31), (25, 40)])],
    "showers_night": peek_cloud(True) + [drops_shape([(18, 31), (32, 31), (25, 40)])],
    "snow": [GENERAL_CLOUD, snow_shape([(16, 34), (31, 32), (24, 42)])],
    "snow_showers_day": peek_cloud(False) + [snow_shape([(18, 35), (33, 33), (26, 42)])],
    "snow_showers_night": peek_cloud(True) + [snow_shape([(18, 35), (33, 33), (26, 42)])],
    "sleet": [GENERAL_CLOUD, drops_shape([(17, 34)]), snow_shape([(31, 35)])],
    "thunder": [GENERAL_CLOUD, drops_shape([(11, 33), (16, 41)]), bolt_shape(21, 24, 15, 22)],
    "thunder_day": peek_cloud(False) + [drops_shape([(13, 35), (18, 43)]), bolt_shape(23, 26, 14, 20)],
    "thunder_night": peek_cloud(True) + [drops_shape([(13, 35), (18, 43)]), bolt_shape(23, 26, 14, 20)],
}


# ---------------------------------------------------------------- output

def xml_path(s):
    a = s.get("alpha", 1.0)
    alpha = f' android:fillAlpha="{f(a)}"' if a < 1 else ""
    if "stroke" in s:
        return (
            f'    <path android:pathData="{s["d"]}" android:strokeColor="{s["stroke"]}" '
            f'android:strokeWidth="{f(s["width"])}" android:strokeLineCap="round" android:strokeLineJoin="round" />\n'
        )
    colors, x1, y1, x2, y2 = s["grad"]
    return (
        f'    <path android:pathData="{s["d"]}"{alpha}>\n'
        f'        <aapt:attr name="android:fillColor">\n'
        f'            <gradient android:type="linear" android:startX="{f(x1)}" android:startY="{f(y1)}" '
        f'android:endX="{f(x2)}" android:endY="{f(y2)}">\n'
        f'                <item android:offset="0" android:color="{colors[0]}" />\n'
        f'                <item android:offset="1" android:color="{colors[1]}" />\n'
        f"            </gradient>\n"
        f"        </aapt:attr>\n"
        f"    </path>\n"
    )


def write_xml(name, shapes):
    body = "".join(xml_path(s) for s in shapes)
    xml = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by scripts/generate-weather-icons.py. Original RCB Wake artwork. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        f'    android:width="{VIEW}dp" android:height="{VIEW}dp" '
        f'android:viewportWidth="{VIEW}" android:viewportHeight="{VIEW}">\n'
        f"{body}</vector>\n"
    )
    with open(os.path.join(OUT, f"ic_wx_{name}.xml"), "w") as fh:
        fh.write(xml)


def svg_group(shapes, prefix):
    defs, body = [], []
    for i, s in enumerate(shapes):
        if "stroke" in s:
            body.append(
                f'<path d="{s["d"]}" fill="none" stroke="{s["stroke"]}" stroke-width="{f(s["width"])}" '
                f'stroke-linecap="round" stroke-linejoin="round"/>'
            )
            continue
        colors, x1, y1, x2, y2 = s["grad"]
        gid = f"{prefix}{i}"
        defs.append(
            f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{f(x1)}" y1="{f(y1)}" '
            f'x2="{f(x2)}" y2="{f(y2)}"><stop offset="0" stop-color="{colors[0]}"/>'
            f'<stop offset="1" stop-color="{colors[1]}"/></linearGradient>'
        )
        body.append(f'<path d="{s["d"]}" fill="url(#{gid})" fill-opacity="{f(s.get("alpha", 1.0))}"/>')
    return "".join(defs), "".join(body)


def write_preview(path):
    cols, cell = 5, 120
    rows = math.ceil(len(ICONS) / cols)
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{cols * cell}" height="{rows * (cell + 20)}">',
           f'<rect width="100%" height="100%" fill="#10131A"/>']
    for i, (name, shapes) in enumerate(ICONS.items()):
        x, y = (i % cols) * cell, (i // cols) * (cell + 20)
        defs, body = svg_group(shapes, f"g{i}_")
        out.append(f"<defs>{defs}</defs>")
        out.append(f'<g transform="translate({x + 12},{y + 8}) scale(2)">{body}</g>')
        out.append(f'<text x="{x + cell / 2}" y="{y + cell + 10}" fill="#AAA" font-size="11" '
                   f'font-family="sans-serif" text-anchor="middle">{name}</text>')
    out.append("</svg>")
    with open(path, "w") as fh:
        fh.write("".join(out))


if __name__ == "__main__":
    for old in os.listdir(OUT):
        if old.startswith("ic_wx_") and old.endswith(".xml"):
            os.remove(os.path.join(OUT, old))
    for name, shapes in ICONS.items():
        write_xml(name, shapes)
    if PREVIEW:
        write_preview(PREVIEW)
    print(f"Wrote {len(ICONS)} icons to {OUT}")
