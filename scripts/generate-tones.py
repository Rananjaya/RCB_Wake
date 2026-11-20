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
