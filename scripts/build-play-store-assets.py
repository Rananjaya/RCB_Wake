#!/usr/bin/env python3
"""Builds the Google Play Store graphics for RCB Wake from real device screenshots.

Inputs  (play-store/source/):
  raw/*.png        Unedited screenshots captured with `adb exec-out screencap -p` on a phone.
  fonts/Roboto.ttf Roboto variable font (Apache License 2.0), used for headings.

Outputs (play-store/):
  icon/play-store-icon-512.png              512 x 512, full-bleed square (Play applies the mask)
  feature-graphic/feature-graphic-1024x500.png
  screenshots/NN-*.png                      1080 x 1920 (9:16) compositions (01-09)
  promotional/pink-dream-promo.png          1080 x 1920

Screenshots are only cropped (system status and navigation bars removed), scaled and placed on
a branded background with a short heading. The app UI itself is never redrawn or edited.

Usage: python3 scripts/build-play-store-assets.py
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..", "play-store")
SRC = os.path.join(ROOT, "source")
RAW = os.path.join(SRC, "raw")
FONT = os.path.join(SRC, "fonts", "Roboto.ttf")

# Brand colours (Midnight Purple theme, src/theme/themes.ts).
BG_TOP = (40, 33, 59)
BG_BOTTOM = (16, 13, 28)
LAVENDER = (196, 181, 253)
PRIMARY = (168, 85, 247)
WHITE = (255, 255, 255)
# Pink Dream theme.
PINK_TOP = (255, 246, 241)
PINK_BOTTOM = (251, 227, 234)
PINK_TEXT = (61, 43, 63)
PINK_PRIMARY = (194, 87, 127)

W, H = 1080, 1920
EXTRA = {"06-stopwatch"}
STATUS_BAR = 110   # px removed from the top of a 1080 x 2400 capture
NAV_BAR = 126      # px removed from the bottom

SCREENSHOTS = [
    ("01-alarm-dashboard", "Alarms that ring on time", "Even with the app closed, the screen off or after a restart"),
    ("02-create-alarm", "Set it your way", "Repeat days, label, sound, vibration, gradual volume and snooze"),
    ("03-ringing-screen", "Clear, full-screen alarm", "Shown over the lock screen, with snooze and dismiss"),
    ("04-math-challenge", "Wake-up challenge", "Solve math questions or scan any QR code to dismiss"),
    ("05-timer-pomodoro", "Timers and Pomodoro", "Focus sessions and breaks that keep running in the background"),
    ("06-stopwatch", "Stopwatch with laps", "Fastest and slowest laps highlighted"),
    ("07-world-clock", "World clock", "See the time and the difference in other cities"),
    ("08-bedtime-reminder", "Bedtime reminder", "A gentle nudge based on your next alarm and sleep goal"),
    ("09-themes", "Make it yours", "Midnight Purple, Pink Dream and accent colours"),
]


def font(size, weight=400):
    f = ImageFont.truetype(FONT, size)
    f.set_variation_by_axes([weight, 100, 0])
    return f


def gradient(w, h, top, bottom):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        c = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return img


def glow(img, center, radius, color, strength):
    layer = Image.new("RGB", img.size, (0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = center
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=color)
    layer = layer.filter(ImageFilter.GaussianBlur(radius * 0.6))
    return Image.blend(img, Image.composite(layer, img, Image.new("L", img.size, 255)), strength)


def rounded(img, r):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], r, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def wrap(draw, text, fnt, width):
    words, lines, line = text.split(), [], ""
    for w in words:
        trial = f"{line} {w}".strip()
        if draw.textlength(trial, font=fnt) <= width:
            line = trial
        else:
            lines.append(line)
            line = w
    lines.append(line)
    return lines


def crop_bars(raw):
    w, h = raw.size
    return raw.crop((0, STATUS_BAR, w, h - NAV_BAR))


def compose(raw_name, title, subtitle, pink=False):
    top, bottom = (PINK_TOP, PINK_BOTTOM) if pink else (BG_TOP, BG_BOTTOM)
    title_c, sub_c = (PINK_TEXT, PINK_PRIMARY) if pink else (WHITE, LAVENDER)
    canvas = gradient(W, H, top, bottom)
    canvas = glow(canvas, (W // 2, 180), 420, PINK_PRIMARY if pink else PRIMARY, 0.18)
    d = ImageDraw.Draw(canvas)
    y = 110
    tf, sf = font(70, 700), font(36, 400)
    for line in wrap(d, title, tf, W - 160):
        d.text((W / 2, y), line, font=tf, fill=title_c, anchor="ma")
        y += 84
    y += 10
    for line in wrap(d, subtitle, sf, W - 200):
        d.text((W / 2, y), line, font=sf, fill=sub_c, anchor="ma")
        y += 48

    shot = crop_bars(Image.open(os.path.join(RAW, raw_name + ".png")).convert("RGB"))
    top_y = max(y + 50, 400)
    target_h = H - top_y - 70
    scale = target_h / shot.size[1]
    shot = shot.resize((round(shot.size[0] * scale), target_h), Image.LANCZOS)
    card = rounded(shot, 44)
    x = (W - card.size[0]) // 2
    # Soft shadow and a thin outline so the screenshot stands off the background.
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([x, top_y + 18, x + card.size[0], top_y + 18 + card.size[1]], 44, fill=(0, 0, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    base = canvas.convert("RGBA")
    base.alpha_composite(shadow)
    base.alpha_composite(card, (x, top_y))
    ImageDraw.Draw(base).rounded_rectangle(
        [x - 1, top_y - 1, x + card.size[0], top_y + card.size[1]], 45,
        outline=(236, 221, 249, 90) if pink else (196, 181, 253, 70), width=3,
    )
    return base.convert("RGB")


# ---- Icon: the default launcher icon from scripts/generate-app-icons.py, drawn at 512 px ----

def render_icon(size=512, ss=4):
    n = size * ss
    # Map the adaptive-icon viewport 12..96 (84 units) onto the square, leaving a margin for the
    # rounded mask Google Play applies.
    k = n / 84.0

    def p(x, y):
        return ((x - 12) * k, (y - 12) * k)

    stops = [(0.0, (0xC0, 0x84, 0xFC)), (0.5, (0x93, 0x33, 0xEA)), (1.0, (0x5B, 0x21, 0xB6))]
    img = Image.new("RGB", (n, n))
    px = img.load()
    for yy in range(n):
        for xx in range(0, n):
            # Diagonal gradient, top-left to bottom-right, like the launcher icon.
            t = (xx + yy) / (2 * (n - 1))
            for i in range(len(stops) - 1):
                if stops[i][0] <= t <= stops[i + 1][0]:
                    a, ca = stops[i]
                    b, cb = stops[i + 1]
                    u = (t - a) / (b - a)
                    px[xx, yy] = tuple(round(ca[j] + (cb[j] - ca[j]) * u) for j in range(3))
                    break
    d = ImageDraw.Draw(img)
    white, face = (255, 255, 255), (0x7C, 0x3A, 0xED)

    def disc(cx, cy, r, fill):
        x0, y0 = p(cx - r, cy - r)
        x1, y1 = p(cx + r, cy + r)
        d.ellipse([x0, y0, x1, y1], fill=fill)

    def half_disc(cx, cy, r, facing_deg, fill):
        pts = [p(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
               for a in [facing_deg - 90 + i * 2 for i in range(91)]]
        d.polygon(pts, fill=fill)

    def stroke(x0, y0, x1, y1, w, fill):
        a, b = p(x0, y0), p(x1, y1)
        d.line([a, b], fill=fill, width=round(w * k))
        for c in (a, b):
            r = w * k / 2
            d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], fill=fill)

    cx, cy, r = 54.0, 57.0, 19.0
    half_disc(40.65, 34.15, 8.6, 225, white)   # bells
    half_disc(67.35, 34.15, 8.6, 315, white)
    stroke(cx - 12.5, cy + 16, cx - 16, cy + 21, 3.4, white)  # feet
    stroke(cx + 12.5, cy + 16, cx + 16, cy + 21, 3.4, white)
    stroke(cx - 3, cy - r - 2.5, cx + 3, cy - r - 2.5, 3.4, white)  # crown
    disc(cx, cy, r, white)
    disc(cx, cy, r - 4.2, face)
    stroke(cx, cy, cx, cy - 10, 3.2, white)  # hands
    stroke(cx, cy, cx + 7.5, cy + 4.5, 3.2, white)
    disc(cx, cy, 2.2, white)
    return img.resize((size, size), Image.LANCZOS)


def feature_graphic(icon):
    fw, fh = 1024, 500
    img = gradient(fw, fh, BG_TOP, BG_BOTTOM)
    img = glow(img, (300, 120), 300, PRIMARY, 0.25)
    d = ImageDraw.Draw(img)
    ic = rounded(icon.resize((168, 168), Image.LANCZOS), 40)
    base = img.convert("RGBA")
    base.alpha_composite(ic, (80, 96))
    d = ImageDraw.Draw(base)
    d.text((80, 300), "RCB Wake", font=font(76, 700), fill=WHITE)
    d.text((82, 392), "Wake up your way.", font=font(34, 400), fill=LAVENDER)
    # A real screenshot (alarm list) on the right, kept clear of the edges.
    shot = crop_bars(Image.open(os.path.join(RAW, "01-alarm-dashboard.png")).convert("RGB"))
    shot = shot.crop((0, 0, shot.size[0], round(shot.size[0] * 1.15)))
    sh = 400
    shot = shot.resize((round(shot.size[0] * sh / shot.size[1]), sh), Image.LANCZOS)
    card = rounded(shot, 26)
    x, y = fw - card.size[0] - 70, (fh - sh) // 2
    shadow = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([x, y + 12, x + card.size[0], y + 12 + sh], 26, fill=(0, 0, 0, 160))
    base.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(18)))
    base.alpha_composite(card, (x, y))
    d.rounded_rectangle([x - 1, y - 1, x + card.size[0], y + sh], 27, outline=(196, 181, 253, 80), width=2)
    return base.convert("RGB")


def main():
    for sub in ("icon", "feature-graphic", "screenshots", "promotional"):
        os.makedirs(os.path.join(ROOT, sub), exist_ok=True)
    icon = render_icon()
    # Google Play wants a 32-bit PNG icon; the feature graphic must have no alpha channel.
    icon.convert("RGBA").save(os.path.join(ROOT, "icon", "play-store-icon-512.png"), optimize=True)
    feature_graphic(icon).save(os.path.join(ROOT, "feature-graphic", "feature-graphic-1024x500.png"), optimize=True)
    for name, title, subtitle in SCREENSHOTS:
        # Google Play shows at most 8 phone screenshots; extras go to promotional/.
        folder = "promotional" if name in EXTRA else "screenshots"
        compose(name, title, subtitle).save(os.path.join(ROOT, folder, name + ".png"), optimize=True)
    compose("pink-dream", "Pink Dream theme", "A soft, light look — same reliable alarms", pink=True).save(
        os.path.join(ROOT, "promotional", "pink-dream-promo.png"), optimize=True)
    print("Play Store assets written to", os.path.abspath(ROOT))


if __name__ == "__main__":
    main()
