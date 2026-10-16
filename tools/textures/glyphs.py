"""textures/particle/glyph/*.png - the twelve rune glyphs.

All twelve are only ever bound by Render2D.drawGlyphs, which blends additively
(SRC_ALPHA, ONE), so they follow the repository's "base" convention: opaque
greyscale line-art on black.
"""
import math, os, sys
import numpy as np
from PIL import ImageFilter, Image
from common import *

OUT = sys.argv[1]
N = 256
os.makedirs(OUT, exist_ok=True)
n = N * SS
C = n / 2.0
W = n * 0.082          # stroke width - heavier than the first pass, which read as hairlines
R = n * 0.40           # nominal glyph radius


def emit(name, build, glow=0.5):
    m, d = mask(N)
    build(d)
    lum = lum_of(shrink(m, N))
    img = Image.fromarray((lum * 255).astype(np.uint8))
    b1 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 64.0))).astype(np.float32) / 255.0
    b2 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 22.0))).astype(np.float32) / 255.0
    lum = np.clip(lum + glow * b1 + glow * 0.55 * b2, 0, 1)
    write_base(lum, f'{OUT}/{name}.png')


def ring(d, r, w=W, fill=255):
    d.ellipse([C - r, C - r, C + r, C + r], outline=fill, width=int(round(w)))


# abs - the absolute-value bars with a slash between them
def g_abs(d):
    for sx in (-0.62, 0.62):
        x = C + R * sx
        thick_line(d, [(x, C - R), (x, C + R)], W)
    thick_line(d, [(C - R * 0.30, C + R * 0.80), (C + R * 0.30, C - R * 0.80)], W)


# arrow - solid arrowhead on a shaft
def g_arrow(d):
    head = R * 0.62
    d.polygon([(C, C - R), (C + head, C - R + head * 1.25), (C + head * 0.34, C - R + head * 1.25),
               (C + head * 0.34, C + R), (C - head * 0.34, C + R), (C - head * 0.34, C - R + head * 1.25),
               (C - head, C - R + head * 1.25)], fill=255)


# arrow_line - the same arrow drawn as line art
def g_arrow_line(d):
    thick_line(d, [(C, C + R), (C, C - R)], W)
    for s in (-1, 1):
        thick_line(d, [(C + s * R * 0.50, C - R * 0.42), (C, C - R)], W)
        thick_line(d, [(C + s * R * 0.34, C - R * 0.02), (C, C - R * 0.44)], W * 0.8)


def g_circle(d):
    ring(d, R * 0.92)
    ring(d, R * 0.34, W * 0.75)


def g_cross(d):
    k = R * 0.80
    thick_line(d, [(C - k, C - k), (C + k, C + k)], W)
    thick_line(d, [(C - k, C + k), (C + k, C - k)], W)


def g_flower(d):
    pr = R * 0.46
    for i in range(6):
        a = i * math.pi / 3
        px, py = C + pr * 1.05 * math.cos(a), C + pr * 1.05 * math.sin(a)
        d.ellipse([px - pr, py - pr, px + pr, py + pr], outline=255, width=int(round(W * 0.85)))
    r = R * 0.22
    d.ellipse([C - r, C - r, C + r, C + r], fill=255)


# forever - a lemniscate
def g_forever(d):
    pts = []
    for i in range(361):
        t = math.radians(i)
        den = 1 + math.sin(t) ** 2
        pts.append((C + R * 0.95 * math.cos(t) / den, C + R * 0.68 * math.sin(t) * math.cos(t) / den))
    d.line(pts, fill=255, width=int(round(W)), joint='curve')


def g_line(d):
    thick_line(d, [(C - R * 0.62, C + R * 0.92), (C + R * 0.62, C - R * 0.92)], W)
    for sx, sy in ((-0.62, 0.92), (0.62, -0.92)):
        thick_line(d, [(C + R * sx - R * 0.26, C + R * sy * 0.86), (C + R * sx + R * 0.26, C + R * sy * 0.86)],
                   W * 0.8)


def g_quad(d):
    k = R * 0.86
    d.rectangle([C - k, C - k, C + k, C + k], outline=255, width=int(round(W)))
    k2 = R * 0.40
    d.rectangle([C - k2, C - k2, C + k2, C + k2], outline=255, width=int(round(W * 0.7)))


def g_star(d):
    pts = star_points(C, C, R * 0.98, R * 0.40)
    d.line(pts + [pts[0]], fill=255, width=int(round(W)), joint='curve')


def g_triangle(d):
    pts = poly(C, C, R * 0.98, 3, -math.pi / 2)
    d.line(pts + [pts[0]], fill=255, width=int(round(W)), joint='curve')
    inner = poly(C, C, R * 0.44, 3, -math.pi / 2)
    d.line(inner + [inner[0]], fill=255, width=int(round(W * 0.7)), joint='curve')


def g_zigzag(d):
    pts = []
    steps = 6
    for i in range(steps + 1):
        t = i / steps
        pts.append((C - R * 0.92 + 2 * R * 0.92 * t, C + (R * 0.72 if i % 2 == 0 else -R * 0.72)))
    thick_line(d, pts, W)


for name, fn in (('abs', g_abs), ('arrow', g_arrow), ('arrow_line', g_arrow_line),
                 ('circle', g_circle), ('cross', g_cross), ('flower', g_flower),
                 ('forever', g_forever), ('line', g_line), ('quad', g_quad),
                 ('star', g_star), ('triangle', g_triangle), ('zigzag', g_zigzag)):
    emit(name, fn)

print('glyphs ok')
