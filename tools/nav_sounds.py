#!/usr/bin/env python3
"""Synthesise Home's navigation sounds (NavSound.java): a focus click and an app-open sound, in the spirit of tvOS.

Damped tones and filtered noise shaped by envelopes, written by this script, so the WAVs carry nobody's copyright
(CC0). Each sound has variants; the built-in pair is DEFAULT_TICK / DEFAULT_POP.
    python3 tools/nav_sounds.py              ->  launcher/app/src/main/res/raw/nav_tick.wav, open_pop.wav
    python3 tools/nav_sounds.py --variants D ->  every variant in D (tick_a.wav ... pop_c.wav), to try on a TV:
        adb push D/tick_b.wav /sdcard/Android/data/com.gathin.tvhome/files/nav_tick.wav  (then restart Home)
"""
import math
import random
import struct
import sys
import wave
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "launcher/app/src/main/res/raw"
DEFAULT_TICK = "a"
DEFAULT_POP = "a"


def write(path, samples, level=0.9):
    path.parent.mkdir(parents=True, exist_ok=True)
    peak = max(abs(s) for s in samples) or 1.0
    # 3 ms of silence after, so a resampler's tail never clicks.
    samples = list(samples) + [0.0] * int(RATE * 0.003)
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(s / peak * level * 32767)) for s in samples))


def damped(freq, tau, dur, attack=0.0004):
    n = int(RATE * dur)
    return [min(1.0, (i / RATE) / attack) * math.exp(-(i / RATE) / tau) * math.sin(2 * math.pi * freq * i / RATE)
            for i in range(n)]


def bandpass(x, freq, q):
    """RBJ biquad band-pass (constant peak gain)."""
    w0 = 2 * math.pi * freq / RATE
    alpha = math.sin(w0) / (2 * q)
    b0, b2 = alpha, -alpha
    a0, a1, a2 = 1 + alpha, -2 * math.cos(w0), 1 - alpha
    y, x1, x2, y1, y2 = [], 0.0, 0.0, 0.0, 0.0
    for s in x:
        o = (b0 * s + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1, y2, y1 = x1, s, y1, o
        y.append(o)
    return y


def noise_burst(dur, tau, seed=7):
    rnd = random.Random(seed)
    n = int(RATE * dur)
    return [math.exp(-(i / RATE) / tau) * (rnd.random() * 2 - 1) for i in range(n)]


def mix(*parts):
    n = max(len(p) for p, _ in parts)
    return [sum(g * p[i] for p, g in parts if i < len(p)) for i in range(n)]


# Focus clicks: a few ms long, dry and soft.
def tick_a():
    """Dry: a noise snap through a resonant 3.4 kHz band, with a faint 1.1 kHz body. About 12 ms."""
    snap = bandpass(noise_burst(0.012, 0.0018), 3400, 6)
    return mix((snap, 1.0), (damped(1100, 0.0025, 0.012), 0.25))


def tick_b():
    """Glassy: damped 4.2 kHz and 2.1 kHz tones, very fast decay. About 10 ms."""
    return mix((damped(4200, 0.0014, 0.010), 1.0), (damped(2100, 0.0024, 0.010), 0.45))


def tick_c():
    """Softer, woodier: 1.8 kHz damped tone with a short filtered click. About 15 ms."""
    snap = bandpass(noise_burst(0.015, 0.0012, seed=3), 2600, 4)
    return mix((damped(1800, 0.0035, 0.015), 1.0), (snap, 0.6))


# App-open sounds: short, airy, rising.
def pop_a():
    """A soft pop: a sine gliding 380 -> 980 Hz in 30 ms, a breath of filtered noise, gentle decay. About 120 ms."""
    n = int(RATE * 0.12)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        f = 380 + 600 * (1 - math.exp(-t / 0.012))
        phase += 2 * math.pi * f / RATE
        out.append(min(1.0, t / 0.004) * math.exp(-t / 0.03) * math.sin(phase))
    air = bandpass(noise_burst(0.06, 0.015, seed=11), 2400, 1.2)
    return mix((out, 1.0), (air, 0.35), (tick_a(), 0.25))


def pop_b():
    """A whoosh-select: filtered noise sweeping up with a light 1.3 kHz ping at the end. About 160 ms."""
    rnd = random.Random(5)
    n = int(RATE * 0.16)
    noise = [rnd.random() * 2 - 1 for _ in range(n)]
    out = []
    w0s = [2 * math.pi * (600 + 2400 * (i / n) ** 1.5) / RATE for i in range(n)]
    x1 = x2 = y1 = y2 = 0.0
    for i, s in enumerate(noise):
        w0 = w0s[i]
        alpha = math.sin(w0) / 3.0
        a0, a1, a2 = 1 + alpha, -2 * math.cos(w0), 1 - alpha
        o = (alpha * s - alpha * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1, y2, y1 = x1, s, y1, o
        t = i / RATE
        out.append(o * math.sin(math.pi * min(1.0, t / 0.16)) ** 2)
    ping = [0.0] * int(RATE * 0.07) + damped(1300, 0.03, 0.09, attack=0.002)
    return mix((out, 1.0), (ping, 0.5))


def pop_c():
    """A rounded bloop: 520 -> 1150 Hz glide with a soft octave. About 140 ms."""
    n = int(RATE * 0.14)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        f = 520 + 630 * (1 - math.exp(-t / 0.025))
        phase += 2 * math.pi * f / RATE
        out.append(min(1.0, t / 0.006) * math.exp(-t / 0.035) * (math.sin(phase) + 0.15 * math.sin(2 * phase)))
    return out


TICKS = {"a": tick_a, "b": tick_b, "c": tick_c}
POPS = {"a": pop_a, "b": pop_b, "c": pop_c}

if __name__ == "__main__":
    if len(sys.argv) == 3 and sys.argv[1] == "--variants":
        d = Path(sys.argv[2])
        for k, f in TICKS.items():
            write(d / f"tick_{k}.wav", f())
        for k, f in POPS.items():
            write(d / f"pop_{k}.wav", f())
        print("wrote variants to", d)
    else:
        write(OUT / "nav_tick.wav", TICKS[DEFAULT_TICK]())
        write(OUT / "open_pop.wav", POPS[DEFAULT_POP]())
        print("wrote", OUT / "nav_tick.wav", OUT / "open_pop.wav")
