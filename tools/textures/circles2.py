"""More jump-circle / hit-bubble rings: rune, sakura, snowflake, magic and shatter.

Same convention as the rest of jump_circles/: opaque greyscale on black, for additive blending,
with a matching _unblack (white RGB, alpha = luminance) beside it.
"""
import math, os, sys
import numpy as np
from PIL import Image, ImageFilter
from common import *

OUT = sys.argv[1]
N = 1024
os.makedirs(OUT, exist_ok=True)
n = N * SS
C = n / 2.0


def bloom(lum, k1=0.55, k2=0.35):
    img = Image.fromarray((np.clip(lum, 0, 1) * 255).astype(np.uint8))
    b1 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 256.0))).astype(np.float32) / 255.0
    b2 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 64.0))).astype(np.float32) / 255.0
    return np.clip(lum + k1 * b1 + k2 * b2, 0, 1)


def emit(name, build, glow=(0.55, 0.35)):
    m, d = mask(N)
    build(d)
    lum = bloom(lum_of(shrink(m, N)), *glow)
    write_base(lum, f'{OUT}/{name}.png')
    write_unblack(lum, f'{OUT}/{name}_unblack.png')
    print('wrote', name)


def ring(d, radius, width, fill=255):
    d.ellipse([C - radius, C - radius, C + radius, C + radius], outline=fill,
              width=int(round(width)))


def radial(d, a, r0, r1, width, fill=255):
    thick_line(d, [(C + r0 * math.cos(a), C + r0 * math.sin(a)),
                   (C + r1 * math.cos(a), C + r1 * math.sin(a))], width, fill, caps=False)


# A ring of glyph-like marks between two circles.
def rune(d):
    w = n * 0.006
    ring(d, n * 0.46, w)
    ring(d, n * 0.355, w * 0.7)
    ring(d, n * 0.20, w * 0.7)
    marks = 16
    for i in range(marks):
        a = i * 2 * math.pi / marks
        # a small upright tick with a crossbar - reads as writing at a glance
        r0, r1 = n * 0.375, n * 0.44
        radial(d, a, r0, r1, w * 1.1)
        bar = n * 0.022
        mid = (r0 + r1) / 2
        px, py = C + mid * math.cos(a), C + mid * math.sin(a)
        thick_line(d, [(px - bar * math.sin(a), py + bar * math.cos(a)),
                       (px + bar * math.sin(a), py - bar * math.cos(a))], w, caps=False)
    for i in range(marks * 2):
        a = i * math.pi / marks + math.pi / (marks * 2)
        radial(d, a, n * 0.205, n * 0.235, w * 0.6)


# Petals around a small core.
def sakura(d):
    petals = 10
    pr = n * 0.17
    for i in range(petals):
        a = i * 2 * math.pi / petals
        px, py = C + math.cos(a) * n * 0.3, C + math.sin(a) * n * 0.3
        d.ellipse([px - pr, py - pr * 0.6, px + pr, py + pr * 0.6], outline=255,
                  width=int(round(n * 0.006)))
    ring(d, n * 0.455, n * 0.005)
    r = n * 0.07
    d.ellipse([C - r, C - r, C + r, C + r], fill=255)


# Six-fold snowflake geometry inside a ring.
def snowflake_ring(d):
    w = n * 0.007
    ring(d, n * 0.465, w)
    arm = n * 0.42
    for i in range(6):
        a = i * math.pi / 3
        radial(d, a, 0, arm, w)
        for frac, blen in ((0.45, 0.16), (0.7, 0.12)):
            bx = C + arm * frac * math.cos(a)
            by = C + arm * frac * math.sin(a)
            for sgn in (+1, -1):
                ba = a + sgn * math.pi / 3.2
                thick_line(d, [(bx, by), (bx + arm * blen * math.cos(ba),
                                          by + arm * blen * math.sin(ba))], w * 0.7, caps=False)


# Two rings, a square and spokes - a summoning circle.
def magic(d):
    w = n * 0.006
    ring(d, n * 0.47, w)
    ring(d, n * 0.44, w * 0.6)
    ring(d, n * 0.26, w * 0.8)
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        pts = [(C + n * 0.40 * math.cos(a + k * math.pi / 2),
                C + n * 0.40 * math.sin(a + k * math.pi / 2)) for k in range(4)]
        d.line(pts + [pts[0]], fill=255, width=int(round(w)), joint='curve')
        break
    for i in range(12):
        a = i * 2 * math.pi / 12
        radial(d, a, n * 0.27, n * 0.43, w * 0.8)
    for i in range(3):
        a = i * 2 * math.pi / 3
        pts = [(C + n * 0.25 * math.cos(a + k * 2 * math.pi / 3),
                C + n * 0.25 * math.sin(a + k * 2 * math.pi / 3)) for k in range(3)]
        d.line(pts + [pts[0]], fill=255, width=int(round(w)), joint='curve')
        break


# A ring broken into shards with cracks running outward.
def shatter(d):
    import random as _r
    rng = _r.Random(7)
    w = n * 0.008
    pieces = 14
    for i in range(pieces):
        a0 = i * 2 * math.pi / pieces + rng.uniform(-0.05, 0.05)
        a1 = (i + 0.72) * 2 * math.pi / pieces
        r = n * (0.40 + rng.uniform(-0.03, 0.05))
        steps = 8
        pts = [(C + r * math.cos(a0 + (a1 - a0) * s / steps),
                C + r * math.sin(a0 + (a1 - a0) * s / steps)) for s in range(steps + 1)]
        d.line(pts, fill=255, width=int(round(w)), joint='curve')
        mid = (a0 + a1) / 2
        radial(d, mid, r, r + n * rng.uniform(0.03, 0.08), w * 0.8)
        radial(d, mid, n * 0.18, r, w * 0.5)


emit('rune_ring', rune)
emit('sakura_ring', sakura)
emit('snowflake_ring', snowflake_ring)
emit('magic_ring', magic)
emit('shatter_ring', shatter)
