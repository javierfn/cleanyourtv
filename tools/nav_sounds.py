#!/usr/bin/env python3
"""Synthesise Home's navigation sounds (NavSound.java): a soft tick and an app-open pop.

Pure tones shaped by envelopes, written by this script, so the WAVs carry nobody's copyright (CC0).
    python3 tools/nav_sounds.py   ->  launcher/app/src/main/res/raw/nav_tick.wav, open_pop.wav
"""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "launcher/app/src/main/res/raw"


def write(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    peak = max(abs(s) for s in samples) or 1.0
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(s / peak * 0.9 * 32767)) for s in samples))


def tick():
    """28 ms: a 1.9 kHz sine with a 2 ms attack and a fast decay, plus a quieter octave for a woody edge."""
    n = int(RATE * 0.028)
    out = []
    for i in range(n):
        t = i / RATE
        env = min(1.0, t / 0.002) * math.exp(-t / 0.006)
        out.append(env * (math.sin(2 * math.pi * 1900 * t) + 0.25 * math.sin(2 * math.pi * 3800 * t)))
    return out


def pop():
    """140 ms: a bubble-like sine gliding 520 -> 1150 Hz with a soft attack and a rounded decay."""
    n = int(RATE * 0.14)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        f = 520 + 630 * (1 - math.exp(-t / 0.025))
        phase += 2 * math.pi * f / RATE
        env = min(1.0, t / 0.006) * math.exp(-t / 0.035)
        out.append(env * (math.sin(phase) + 0.15 * math.sin(2 * phase)))
    return out


if __name__ == "__main__":
    write("nav_tick.wav", tick())
    write("open_pop.wav", pop())
    print("wrote", OUT / "nav_tick.wav", OUT / "open_pop.wav")
