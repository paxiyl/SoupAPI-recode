"""Synthesised sounds for the Arcana weapons - original, generated here, nothing sampled.

Mono 44.1 kHz, encoded to Ogg Vorbis with ffmpeg (Minecraft only pans mono sounds).
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
RNG = np.random.default_rng(99)


def t_axis(seconds):
    return np.arange(int(SR * seconds)) / SR


def env(t, attack, decay):
    return np.minimum(1, t / max(1e-4, attack)) * np.exp(-np.maximum(0, t - attack) / max(1e-4, decay))


def noise(n):
    return RNG.standard_normal(n)


def bandpass(x, lo, hi):
    f = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1 / SR)
    f[(freqs < lo) | (freqs > hi)] = 0
    return np.fft.irfft(f, len(x))


def sweep_filter(x, f0, f1):
    """A moving band: split into chunks, each band-passed around a frequency gliding f0 -> f1."""
    out = np.zeros_like(x)
    n = 24
    size = len(x) // n + 1
    for i in range(n):
        a, b = i * size, min(len(x), (i + 1) * size + 256)
        fc = f0 + (f1 - f0) * i / n
        out[a:b] += bandpass(x[a:b], fc * 0.6, fc * 1.6)[: b - a] * np.hanning(b - a)
    return out


def whoosh(seconds=0.45, f0=500, f1=2400, peak=0.35):
    t = t_axis(seconds)
    x = sweep_filter(noise(len(t)), f0, f1)
    shape = np.sin(np.pi * np.clip(t / seconds, 0, 1)) ** 2
    return x * shape / (np.abs(x).max() + 1e-9) * peak * 2.2


def ring(freq, seconds, decay, partials=(1, 2.76, 5.4, 8.93), amp=0.4):
    t = t_axis(seconds)
    x = sum(np.sin(2 * np.pi * freq * p * t + i) / (i + 1) for i, p in enumerate(partials))
    return x * env(t, 0.003, decay) * amp


def chime(freqs, seconds=1.6):
    t = t_axis(seconds)
    x = np.zeros_like(t)
    for i, f in enumerate(freqs):
        start = i * 0.06
        tt = np.clip(t - start, 0, None)
        x += np.sin(2 * np.pi * f * tt) * env(tt, 0.002, 0.5) * (t >= start)
    return x * 0.18


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    peak = np.abs(out).max()
    return out / peak * 0.8 if peak > 0.8 else out


def delay(x, seconds):
    return np.concatenate([np.zeros(int(SR * seconds)), x])


def click(seconds=0.05, freq=1800):
    t = t_axis(seconds)
    return bandpass(noise(len(t)), freq * 0.5, freq * 2) * env(t, 0.001, 0.012) * 0.9


def portal(seconds=2.6):
    """Rift forming, runes turning up, energy converging, the weapon breaking through."""
    t = t_axis(seconds)
    rumble = np.sin(2 * np.pi * (38 + 10 * t) * t) * np.clip(t / 1.2, 0, 1) * 0.35
    hiss = bandpass(noise(len(t)), 200, 900) * np.clip(t / 1.4, 0, 1) * 0.12
    shimmer = sum(np.sin(2 * np.pi * (300 + 520 * t / seconds) * k * t) / k for k in (1, 1.5, 2.25))
    shimmer *= np.clip((t - 0.5) / 0.8, 0, 1) * np.exp(-np.maximum(0, t - 1.9) * 4) * 0.12
    crack = delay(mix(click(0.08, 900), ring(220, 1.0, 0.35, amp=0.3)), 1.05)
    land = delay(mix(ring(660, 0.8, 0.2, amp=0.25), whoosh(0.35, 900, 3000, 0.25)), 1.95)
    return mix(rumble + hiss + shimmer, crack, land)


def write_ogg(samples, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav_path, '-ac', '1', '-c:a', 'libvorbis',
                    '-q:a', '3', path], check=True)
    os.remove(wav_path)


def sets():
    blade = {
        'draw': mix(whoosh(0.4, 400, 1800, 0.3), delay(ring(1250, 1.2, 0.5), 0.3)),
        'swing_1': whoosh(0.4, 600, 2600, 0.4),
        'swing_2': whoosh(0.38, 700, 3000, 0.4),
        'swing_3': whoosh(0.5, 450, 2000, 0.45),
        'thrust': mix(whoosh(0.3, 900, 3500, 0.35), delay(click(0.04, 2500), 0.12)),
        'flourish': mix(whoosh(0.3, 500, 2000, 0.3), delay(whoosh(0.3, 600, 2400, 0.3), 0.3), delay(ring(1500, 0.8, 0.3, amp=0.2), 0.75)),
        'inspect': chime([880, 1320, 1760, 2349]),
        'put_away': mix(whoosh(0.35, 1800, 500, 0.25), delay(ring(980, 0.6, 0.15, amp=0.25), 0.3)),
    }
    return {
        'fractured_eye_katana': dict(blade, summon=portal(),
                                     inspect=mix(chime([740, 1109, 1480, 1975]), delay(ring(311, 1.4, 0.6, amp=0.2), 0.2))),
        'raven_claw': dict(blade, open=mix(sweep_filter(noise(SR // 2), 150, 900) * 0.25 * np.hanning(SR // 2),
                                           delay(chime([1046, 1568]), 0.2)),
                           snap=mix(click(0.06, 1200), delay(click(0.05, 700), 0.03), ring(420, 0.4, 0.08, amp=0.25)),
                           draw=mix(whoosh(0.45, 300, 1600, 0.3), delay(click(0.05, 900), 0.4))),
        'spectral_blade': dict(blade, draw=mix(whoosh(0.5, 300, 4000, 0.3), delay(chime([1318, 1760, 2637], 1.2), 0.25)),
                               inspect=chime([1318, 1975, 2637, 3520], 2.0),
                               put_away=mix(whoosh(0.4, 4000, 400, 0.3), chime([2637, 1760], 0.8))),
    }


def write(weapon_id, folder):
    names = []
    for name, samples in sets()[weapon_id].items():
        write_ogg(samples, os.path.join(str(folder), name + '.ogg'))
        names.append(name)
    return names
