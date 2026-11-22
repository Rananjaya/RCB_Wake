#!/usr/bin/env python3
"""Checks play-store/ against Google Play's graphic asset requirements.

Usage: python3 scripts/validate-play-store-assets.py   (exit code 1 if anything fails)
"""
import glob
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "play-store")
failures = []


def check(path, ok, message):
    status = "OK  " if ok else "FAIL"
    print(f"  [{status}] {message}")
    if not ok:
        failures.append(f"{os.path.relpath(path, ROOT)}: {message}")


def inspect(path):
    size = os.path.getsize(path)
    with Image.open(path) as img:
        img.verify()  # raises if the file is corrupt
    with Image.open(path) as img:
        img.load()
        return img.format, img.size, img.mode, size


def icon():
    path = os.path.join(ROOT, "icon", "play-store-icon-512.png")
    print(os.path.relpath(path, ROOT))
    fmt, (w, h), mode, size = inspect(path)
    check(path, fmt == "PNG", f"format {fmt} (PNG required)")
    check(path, (w, h) == (512, 512), f"{w} x {h} (512 x 512 required)")
    check(path, mode == "RGBA", f"mode {mode} (32-bit PNG required)")
    check(path, size <= 1024 * 1024, f"{size / 1024:.0f} KB (max 1024 KB)")


def feature_graphic():
    path = os.path.join(ROOT, "feature-graphic", "feature-graphic-1024x500.png")
    print(os.path.relpath(path, ROOT))
    fmt, (w, h), mode, size = inspect(path)
    check(path, fmt in ("PNG", "JPEG"), f"format {fmt} (PNG or JPEG)")
    check(path, (w, h) == (1024, 500), f"{w} x {h} (1024 x 500 required)")
    check(path, mode == "RGB", f"mode {mode} (no alpha channel)")
    check(path, size <= 15 * 1024 * 1024, f"{size / 1024:.0f} KB (max 15 MB)")


def screenshots():
    files = sorted(glob.glob(os.path.join(ROOT, "screenshots", "*.png"))) + sorted(
        glob.glob(os.path.join(ROOT, "promotional", "*.png")))
    shots = [f for f in files if "/screenshots/" in f]
    print(f"screenshots: {len(shots)} phone screenshots")
    check(ROOT, 2 <= len(shots) <= 8, f"{len(shots)} phone screenshots (Google Play accepts 2-8)")
    for path in files:
        print(os.path.relpath(path, ROOT))
        fmt, (w, h), mode, size = inspect(path)
        check(path, fmt in ("PNG", "JPEG"), f"format {fmt} (PNG or JPEG)")
        check(path, 320 <= min(w, h) and max(w, h) <= 3840, f"{w} x {h} (each side 320-3840 px)")
        check(path, max(w, h) <= 2 * min(w, h), f"aspect {max(w, h) / min(w, h):.2f}:1 (max 2:1)")
        check(path, h > w, "portrait")
        check(path, mode == "RGB", f"mode {mode} (no alpha)")
        check(path, size <= 8 * 1024 * 1024, f"{size / 1024:.0f} KB (max 8 MB)")


icon()
feature_graphic()
screenshots()
print()
if failures:
    print("FAILED:")
    for f in failures:
        print(" -", f)
    sys.exit(1)
print("All Play Store assets pass.")
