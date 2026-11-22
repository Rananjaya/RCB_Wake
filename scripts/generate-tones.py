#!/usr/bin/env python3
"""Synthesises RCB Wake's bundled alarm tones.

The tones are generated from simple sine waves, so they are original works distributed under the
project licence. Run from the repository root:  python3 scripts/generate-tones.py
"""
import math
import struct
import wave

RATE = 22050
OUT = "android/app/src/main/res/raw"


def tone(freq, dur, vol=0.6, decay=0.0, harmonics=(1.0,)):
    n = int(RATE * dur)
    out = []
    for i in range(n):
        t = i / RATE
        env = math.exp(-decay * t) if decay else 1.0
        # 8 ms attack/release ramps avoid clicks at note boundaries and at the loop point.
        ramp = min(1.0, i / (0.008 * RATE), (n - i) / (0.008 * RATE))
        s = sum(a * math.sin(2 * math.pi * freq * (k + 1) * t) for k, a in enumerate(harmonics))
        out.append(vol * env * ramp * s / sum(harmonics))
    return out


def silence(dur):
    return [0.0] * int(RATE * dur)


def write(name, samples):
    with wave.open(f"{OUT}/{name}.wav", "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))


bell = (1.0, 0.5, 0.25, 0.12)

# Dawn: a gentle rising arpeggio (C major 7).
dawn = []
for f in (523.25, 659.25, 783.99, 987.77):
    dawn += tone(f, 0.38, 0.55, decay=3.0, harmonics=bell)
dawn += silence(0.6)
write("tone_dawn", dawn)

# Pulse: two short soft beeps.
pulse = tone(880, 0.12, 0.6) + silence(0.1) + tone(880, 0.12, 0.6) + silence(0.7)
write("tone_pulse", pulse)

# Chime: two bell strikes with long decay.
chime = tone(659.25, 0.9, 0.6, decay=2.5, harmonics=bell) + tone(523.25, 1.2, 0.6, decay=2.0, harmonics=bell)
write("tone_chime", chime)

# Classic: fast alternating digital-style beeps.
classic = []
for _ in range(4):
    classic += tone(1046.5, 0.07, 0.55) + silence(0.05)
classic += silence(0.55)
write("tone_classic", classic)


# ---- Additional tones (all synthesised here from sine waves; no samples or recordings) ----

def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(n)]


def partials(freq, dur, ratios_amps, vol, decay):
    """A struck sound from (frequency ratio, amplitude, decay multiplier) partials."""
    n = int(RATE * dur)
    total = sum(a for _, a, _ in ratios_amps)
    out = []
    for i in range(n):
        t = i / RATE
        ramp = min(1.0, i / (0.004 * RATE), (n - i) / (0.01 * RATE))
        s = sum(a * math.exp(-decay * d * t) * math.sin(2 * math.pi * freq * r * t) for r, a, d in ratios_amps)
        out.append(vol * ramp * s / total)
    return out


def chirp(f0, f1, dur, vol):
    """Sine sweep from f0 to f1 with a smooth bell-shaped envelope (bird-like)."""
    n = int(RATE * dur)
    out, phase = [], 0.0
    for i in range(n):
        x = i / n
        f = f0 + (f1 - f0) * x
        phase += 2 * math.pi * f / RATE
        out.append(vol * math.sin(math.pi * x) ** 2 * math.sin(phase))
    return out


# Sunrise: a soft major-seventh chord that swells in and fades, very gentle.
n = int(RATE * 3.2)
sunrise = []
for i in range(n):
    t = i / RATE
    env = math.sin(math.pi * min(1.0, t / 3.2)) ** 2
    s = sum(math.sin(2 * math.pi * f * t) for f in (261.63, 329.63, 392.0, 493.88)) / 4
    sunrise.append(0.55 * env * s)
write("tone_sunrise", sunrise + silence(0.3))

# Birdsong: a few chirps rising and falling, then a pause.
bird = []
for f0, f1, d, gap in ((2600, 3600, 0.09, 0.05), (3600, 2800, 0.08, 0.05), (2700, 3800, 0.1, 0.18),
                       (3000, 4000, 0.07, 0.04), (4000, 3000, 0.07, 0.04), (3100, 4100, 0.08, 0.6)):
    bird += chirp(f0, f1, d, 0.5) + silence(gap)
write("tone_birdsong", bird)

# Marimba: wooden mallet notes (fundamental plus a fast-decaying 4th partial), a short tune.
marimba_partials = ((1.0, 1.0, 1.0), (3.93, 0.35, 3.5), (9.2, 0.1, 6.0))
marimba = []
for f, d in ((523.25, 0.22), (659.25, 0.22), (783.99, 0.22), (659.25, 0.22), (880.0, 0.44), (783.99, 0.7)):
    marimba += partials(f, d, marimba_partials, 0.7, 5.0)
write("tone_marimba", marimba + silence(0.4))

# Bells: inharmonic church-bell partials with a long ring.
bell_partials = ((0.5, 0.6, 0.6), (1.0, 1.0, 1.0), (1.19, 0.5, 1.4), (1.56, 0.4, 1.8), (2.0, 0.35, 2.0), (2.51, 0.2, 2.6))
bells = partials(392.0, 1.4, bell_partials, 0.7, 1.6) + partials(329.63, 1.8, bell_partials, 0.7, 1.4)
write("tone_bells", bells)

# Ripple: water-drop blips whose pitch falls quickly.
ripple = []
for f0, gap in ((1400, 0.12), (1100, 0.22), (1650, 0.1), (1250, 0.5)):
    ripple += chirp(f0, f0 * 0.55, 0.11, 0.6) + silence(gap)
write("tone_ripple", ripple)

# Ascend: a brisk arpeggio climbing two octaves.
ascend = []
for f in (392.0, 493.88, 587.33, 783.99, 987.77, 1174.66, 1567.98):
    ascend += tone(f, 0.12, 0.5, decay=6.0, harmonics=bell)
write("tone_ascend", ascend + silence(0.35))

# Beacon: a clear two-tone ping that is easy to hear.
beacon = tone(1318.5, 0.16, 0.55, decay=4.0, harmonics=(1.0, 0.3)) + tone(987.77, 0.3, 0.55, decay=3.0, harmonics=(1.0, 0.3))
write("tone_beacon", beacon + silence(0.6))

# Siren: an air-raid style wail — pitch slowly rises and falls, with the rough, hollow tone of a
# rotating siren (odd harmonics) and a slight flutter. Synthesised; not a recording.
n = int(RATE * 5.0)
siren, phase = [], 0.0
for i in range(n):
    t = i / RATE
    sweep = 0.5 - 0.5 * math.cos(2 * math.pi * t / 5.0)  # 0 -> 1 -> 0 over the loop
    f = 280 + 520 * sweep
    phase += 2 * math.pi * f / RATE
    flutter = 1 + 0.04 * math.sin(2 * math.pi * 23 * t)
    s = math.sin(phase) + 0.35 * math.sin(3 * phase) + 0.15 * math.sin(5 * phase)
    ramp = min(1.0, i / (0.05 * RATE), (n - i) / (0.05 * RATE))
    siren.append(0.5 * flutter * ramp * s / 1.5)
write("tone_siren", siren)

# Siren 2: a British WWII-style air-raid warning — a continuous rise-and-fall wail (4 s waves that
# never drop to the bottom), sounded as a two-rotor minor-third chord with a buzzy, square-ish
# timbre. Synthesised here; not a recording.
n = int(RATE * 8.0)
siren2, p1, p2 = [], 0.0, 0.0
for i in range(n):
    t = i / RATE
    f = 425 + 105 * math.sin(2 * math.pi * t / 4.0 - math.pi / 2)  # 320 Hz .. 530 Hz, loops cleanly
    p1 += 2 * math.pi * f / RATE
    p2 += 2 * math.pi * f * 1.19 / RATE  # second rotor a minor third above
    rotor = lambda p: math.sin(p) + math.sin(3 * p) / 3 + math.sin(5 * p) / 5 + math.sin(7 * p) / 7
    flutter = 1 + 0.06 * math.sin(2 * math.pi * 31 * t)
    s = (rotor(p1) + 0.8 * rotor(p2)) / 2.6
    ramp = min(1.0, i / (0.03 * RATE), (n - i) / (0.03 * RATE))
    siren2.append(0.55 * flutter * ramp * s)
write("tone_siren2", siren2)
