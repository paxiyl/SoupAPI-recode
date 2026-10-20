"""The symbol set used by the particle effects.

Heavier than the first pass: thick strokes, filled shapes with cut-outs and double outlines, so a
symbol still reads as a symbol at the size a particle is actually drawn. Same convention as the rest
of particle/: opaque greyscale on black, for additive blending.
"""
import math, os, sys
import numpy as np
from PIL import Image, ImageFilter
from common import *

OUT = sys.argv[1]
N = 256
os.makedirs(OUT, exist_ok=True)
n = N * SS
C = n / 2.0
R = n * 0.40
W = n * 0.085          # heavy stroke
WT = n * 0.055         # thin stroke


def emit(name, build, glow=0.55):
    m, d = mask(N)
    build(d)
    lum = lum_of(shrink(m, N))
    img = Image.fromarray((lum * 255).astype(np.uint8))
    b1 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 64.0))).astype(np.float32) / 255.0
    b2 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 22.0))).astype(np.float32) / 255.0
    lum = np.clip(lum + glow * b1 + glow * 0.5 * b2, 0, 1)
    write_base(lum, f'{OUT}/{name}.png')
    print('   ', name)


def ring(d, r, w=W, fill=255):
    d.ellipse([C - r, C - r, C + r, C + r], outline=fill, width=int(round(w)))


def disc(d, cx, cy, r, fill=255):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)


def poly_pts(r, sides, rot=0.0, cx=None, cy=None):
    cx = C if cx is None else cx
    cy = C if cy is None else cy
    return [(cx + r * math.cos(rot + i * 2 * math.pi / sides),
             cy + r * math.sin(rot + i * 2 * math.pi / sides)) for i in range(sides)]


# ---------------------------------------------------------------- the set --

def skull(d):
    # Dome and jaw, with sockets cut out of them.
    d.ellipse([C - R * 0.78, C - R * 0.95, C + R * 0.78, C + R * 0.45], fill=255)
    d.rectangle([C - R * 0.45, C + R * 0.2, C + R * 0.45, C + R * 0.8], fill=255)
    for sx in (-0.34, 0.34):
        disc(d, C + R * sx, C - R * 0.22, R * 0.26, fill=0)
    d.polygon([(C, C + R * 0.05), (C - R * 0.14, C + R * 0.34), (C + R * 0.14, C + R * 0.34)], fill=0)
    for i in range(3):
        x = C - R * 0.3 + i * R * 0.3
        d.rectangle([x - R * 0.05, C + R * 0.42, x + R * 0.05, C + R * 0.8], fill=0)


def circuit(d):
    # Traces running to pads around a core.
    disc(d, C, C, R * 0.18)
    for i in range(6):
        a = i * math.pi / 3
        x1, y1 = C + R * 0.18 * math.cos(a), C + R * 0.18 * math.sin(a)
        mx, my = C + R * 0.55 * math.cos(a), C + R * 0.55 * math.sin(a)
        thick_line(d, [(x1, y1), (mx, my)], WT, caps=False)
        ea = a + (math.pi / 4 if i % 2 == 0 else -math.pi / 4)
        ex, ey = mx + R * 0.3 * math.cos(ea), my + R * 0.3 * math.sin(ea)
        thick_line(d, [(mx, my), (ex, ey)], WT, caps=False)
        disc(d, ex, ey, R * 0.1)


def wave(d):
    for row, amp in ((-0.3, 0.22), (0.05, 0.26), (0.4, 0.2)):
        pts = []
        for i in range(81):
            t = i / 80.0
            pts.append((C - R + 2 * R * t, C + R * row + math.sin(t * math.pi * 3) * R * amp))
        d.line(pts, fill=255, width=int(round(WT)), joint='curve')


def biohazard(d):
    ring(d, R * 0.2, WT)
    for i in range(3):
        a = -math.pi / 2 + i * 2 * math.pi / 3
        cx, cy = C + R * 0.42 * math.cos(a), C + R * 0.42 * math.sin(a)
        ring(d, R * 0.3, W * 0.8) if False else None
        d.ellipse([cx - R * 0.3, cy - R * 0.3, cx + R * 0.3, cy + R * 0.3],
                  outline=255, width=int(round(W * 0.7)))
        disc(d, cx, cy, R * 0.06, fill=0)
    disc(d, C, C, R * 0.12)


def blossom(d):
    petals = 5
    for i in range(petals):
        a = -math.pi / 2 + i * 2 * math.pi / petals
        cx, cy = C + R * 0.42 * math.cos(a), C + R * 0.42 * math.sin(a)
        d.ellipse([cx - R * 0.3, cy - R * 0.24, cx + R * 0.3, cy + R * 0.24], fill=255)
        disc(d, cx, cy, R * 0.09, fill=0)
    disc(d, C, C, R * 0.16)


def moon(d):
    disc(d, C + R * 0.1, C, R * 0.8)
    disc(d, C + R * 0.42, C - R * 0.14, R * 0.68, fill=0)


def sun(d):
    disc(d, C, C, R * 0.4)
    disc(d, C, C, R * 0.28, fill=0)
    for i in range(12):
        a = i * math.pi / 6
        thick_line(d, [(C + R * 0.52 * math.cos(a), C + R * 0.52 * math.sin(a)),
                       (C + R * 0.95 * math.cos(a), C + R * 0.95 * math.sin(a))],
                   W * (1.0 if i % 2 == 0 else 0.55), caps=False)


def fang(d):
    d.polygon([(C - R * 0.5, C - R * 0.7), (C + R * 0.5, C - R * 0.7),
               (C + R * 0.2, C + R * 0.9), (C, C + R * 0.2), (C - R * 0.2, C + R * 0.9)], fill=255)
    d.polygon([(C - R * 0.26, C - R * 0.45), (C + R * 0.26, C - R * 0.45),
               (C + R * 0.1, C + R * 0.1), (C - R * 0.1, C + R * 0.1)], fill=0)


def crown(d):
    base = C + R * 0.55
    d.polygon([(C - R * 0.85, base), (C + R * 0.85, base),
               (C + R * 0.85, C + R * 0.2), (C + R * 0.5, C - R * 0.35),
               (C + R * 0.25, C + R * 0.1), (C, C - R * 0.75),
               (C - R * 0.25, C + R * 0.1), (C - R * 0.5, C - R * 0.35),
               (C - R * 0.85, C + R * 0.2)], fill=255)
    d.rectangle([C - R * 0.85, base - R * 0.1, C + R * 0.85, base], fill=0)
    d.rectangle([C - R * 0.75, base - R * 0.08, C + R * 0.75, base - R * 0.02], fill=255)


def eye(d):
    pts_top = [(C - R, C)]
    for i in range(41):
        t = i / 40.0
        pts_top.append((C - R + 2 * R * t, C - math.sin(t * math.pi) * R * 0.62))
    pts_bottom = [(C + R, C)]
    for i in range(41):
        t = i / 40.0
        pts_bottom.append((C + R - 2 * R * t, C + math.sin(t * math.pi) * R * 0.62))
    d.polygon(pts_top + pts_bottom, fill=255)
    disc(d, C, C, R * 0.3, fill=0)
    disc(d, C, C, R * 0.14)


def leaf(d):
    pts = []
    for i in range(61):
        t = i / 60.0
        pts.append((C - R * 0.55 + 1.1 * R * t, C - math.sin(t * math.pi) * R * 0.55))
    for i in range(61):
        t = i / 60.0
        pts.append((C + R * 0.55 - 1.1 * R * t, C + math.sin(t * math.pi) * R * 0.2))
    d.polygon(pts, fill=255)
    thick_line(d, [(C - R * 0.5, C - R * 0.05), (C + R * 0.55, C - R * 0.3)], WT * 0.8, fill=0)


def bolt(d):
    d.polygon([(C + R * 0.15, C - R), (C - R * 0.45, C + R * 0.12),
               (C - R * 0.05, C + R * 0.12), (C - R * 0.3, C + R),
               (C + R * 0.5, C - R * 0.2), (C + R * 0.08, C - R * 0.2)], fill=255)


def spade(d):
    d.polygon([(C, C - R * 0.9), (C - R * 0.7, C + R * 0.1), (C + R * 0.7, C + R * 0.1)], fill=255)
    disc(d, C - R * 0.36, C + R * 0.1, R * 0.36)
    disc(d, C + R * 0.36, C + R * 0.1, R * 0.36)
    d.polygon([(C - R * 0.18, C + R * 0.9), (C + R * 0.18, C + R * 0.9),
               (C + R * 0.06, C + R * 0.3), (C - R * 0.06, C + R * 0.3)], fill=255)


def clover(d):
    for a in (0, math.pi / 2, math.pi, 3 * math.pi / 2):
        disc(d, C + R * 0.36 * math.cos(a), C + R * 0.36 * math.sin(a) - R * 0.1, R * 0.34)
    d.polygon([(C - R * 0.1, C + R * 0.9), (C + R * 0.1, C + R * 0.9),
               (C + R * 0.04, C + R * 0.2), (C - R * 0.04, C + R * 0.2)], fill=255)


def shuriken(d):
    for i in range(4):
        a = i * math.pi / 2
        d.polygon([(C + R * 0.12 * math.cos(a + 1.2), C + R * 0.12 * math.sin(a + 1.2)),
                   (C + R * 0.95 * math.cos(a), C + R * 0.95 * math.sin(a)),
                   (C + R * 0.12 * math.cos(a - 1.2), C + R * 0.12 * math.sin(a - 1.2))], fill=255)
    disc(d, C, C, R * 0.2)
    disc(d, C, C, R * 0.09, fill=0)


def pentagram(d):
    pts = star_points(C, C, R * 0.95, R * 0.95, points=5)
    order = [0, 4, 8, 2, 6, 0]
    path = [pts[i] for i in order]
    d.line(path, fill=255, width=int(round(W * 0.8)), joint='curve')
    ring(d, R * 0.98, WT * 0.8)


def triquetra(d):
    r = R * 0.5
    for i in range(3):
        a = -math.pi / 2 + i * 2 * math.pi / 3
        cx, cy = C + r * 0.55 * math.cos(a), C + r * 0.55 * math.sin(a)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=255, width=int(round(W * 0.7)))


def ankh(d):
    thick_line(d, [(C, C - R * 0.1), (C, C + R * 0.95)], W)
    thick_line(d, [(C - R * 0.5, C + R * 0.1), (C + R * 0.5, C + R * 0.1)], W)
    d.ellipse([C - R * 0.42, C - R * 0.95, C + R * 0.42, C - R * 0.05],
              outline=255, width=int(round(W)))


def rune_algiz(d):
    thick_line(d, [(C, C - R * 0.95), (C, C + R * 0.95)], W)
    thick_line(d, [(C, C - R * 0.1), (C - R * 0.6, C - R * 0.8)], W)
    thick_line(d, [(C, C - R * 0.1), (C + R * 0.6, C - R * 0.8)], W)


def rune_thurisaz(d):
    thick_line(d, [(C - R * 0.35, C - R * 0.95), (C - R * 0.35, C + R * 0.95)], W)
    d.polygon([(C - R * 0.3, C - R * 0.5), (C + R * 0.6, C), (C - R * 0.3, C + R * 0.5)], fill=255)


def atom(d):
    disc(d, C, C, R * 0.16)
    for i, tilt in enumerate((0, math.pi / 3, -math.pi / 3)):
        box = [C - R * 0.95, C - R * 0.36, C + R * 0.95, C + R * 0.36]
        layer, ld = mask(N)
        ld.ellipse(box, outline=255, width=int(round(WT)))
        layer = layer.rotate(math.degrees(tilt), resample=Image.BILINEAR, center=(C, C))
        d.bitmap((0, 0), layer, fill=255)


def cross_swords(d):
    for s in (-1, 1):
        x1, y1 = C - R * 0.8 * s, C + R * 0.8
        x2, y2 = C + R * 0.7 * s, C - R * 0.7
        thick_line(d, [(x1, y1), (x2, y2)], W * 0.8)
        # hilt
        hx, hy = C - R * 0.55 * s, C + R * 0.55
        px, py = -(y2 - y1), (x2 - x1)
        plen = math.hypot(px, py)
        px, py = px / plen * R * 0.3, py / plen * R * 0.3
        thick_line(d, [(hx - px, hy - py), (hx + px, hy + py)], W * 0.6)


def infinity_bold(d):
    pts = []
    for i in range(241):
        t = i * math.pi / 120.0
        den = 1 + math.sin(t) ** 2
        pts.append((C + R * 0.95 * math.cos(t) / den, C + R * 0.68 * math.sin(t) * math.cos(t) / den))
    d.line(pts, fill=255, width=int(round(W)), joint='curve')


def flame(d):
    pts = [(C, C - R * 0.95)]
    for i in range(1, 40):
        t = i / 39.0
        width = math.sin(t * math.pi) * R * 0.55 * (1 - t * 0.2)
        pts.append((C + width, C - R * 0.95 + t * R * 1.8))
    for i in range(39, 0, -1):
        t = i / 39.0
        width = math.sin(t * math.pi) * R * 0.55 * (1 - t * 0.2)
        pts.append((C - width, C - R * 0.95 + t * R * 1.8))
    d.polygon(pts, fill=255)
    inner = [(C, C - R * 0.2)]
    for i in range(1, 25):
        t = i / 24.0
        width = math.sin(t * math.pi) * R * 0.22
        inner.append((C + width, C - R * 0.2 + t * R * 1.0))
    for i in range(24, 0, -1):
        t = i / 24.0
        width = math.sin(t * math.pi) * R * 0.22
        inner.append((C - width, C - R * 0.2 + t * R * 1.0))
    d.polygon(inner, fill=0)


def heart_bold(d):
    pts = []
    for i in range(721):
        t = i * math.pi / 360.0
        hx = 16 * math.sin(t) ** 3
        hy = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((C + hx / 17.0 * R, C + hy / 17.0 * R))
    d.polygon(pts, fill=255)
    inner = [((p[0] - C) * 0.55 + C, (p[1] - C) * 0.55 + C) for p in pts]
    d.polygon(inner, fill=0)


def snow_crystal(d):
    for i in range(6):
        a = i * math.pi / 3
        thick_line(d, [(C, C), (C + R * 0.95 * math.cos(a), C + R * 0.95 * math.sin(a))], W * 0.7)
        for frac in (0.5, 0.78):
            bx, by = C + R * 0.95 * frac * math.cos(a), C + R * 0.95 * frac * math.sin(a)
            for sgn in (1, -1):
                ba = a + sgn * math.pi / 3
                thick_line(d, [(bx, by), (bx + R * 0.26 * math.cos(ba), by + R * 0.26 * math.sin(ba))],
                           W * 0.5)
    disc(d, C, C, R * 0.14)


def target(d):
    ring(d, R * 0.95, W * 0.6)
    ring(d, R * 0.6, W * 0.6)
    disc(d, C, C, R * 0.2)
    for i in range(4):
        a = i * math.pi / 2
        thick_line(d, [(C + R * 0.8 * math.cos(a), C + R * 0.8 * math.sin(a)),
                       (C + R * 1.0 * math.cos(a), C + R * 1.0 * math.sin(a))], W * 0.6, caps=False)


def diamond_suit(d):
    d.polygon([(C, C - R * 0.95), (C + R * 0.62, C), (C, C + R * 0.95), (C - R * 0.62, C)], fill=255)
    d.polygon([(C, C - R * 0.5), (C + R * 0.32, C), (C, C + R * 0.5), (C - R * 0.32, C)], fill=0)


def hexagon_bold(d):
    pts = poly_pts(R * 0.95, 6, math.pi / 6)
    d.polygon(pts, fill=255)
    inner = poly_pts(R * 0.62, 6, math.pi / 6)
    d.polygon(inner, fill=0)
    inner2 = poly_pts(R * 0.3, 6, math.pi / 6)
    d.polygon(inner2, fill=255)


def gear(d):
    teeth = 10
    for i in range(teeth):
        a = i * 2 * math.pi / teeth
        d.polygon([(C + R * 0.7 * math.cos(a - 0.12), C + R * 0.7 * math.sin(a - 0.12)),
                   (C + R * 0.98 * math.cos(a - 0.09), C + R * 0.98 * math.sin(a - 0.09)),
                   (C + R * 0.98 * math.cos(a + 0.09), C + R * 0.98 * math.sin(a + 0.09)),
                   (C + R * 0.7 * math.cos(a + 0.12), C + R * 0.7 * math.sin(a + 0.12))], fill=255)
    ring(d, R * 0.7, W * 0.9)
    ring(d, R * 0.28, W * 0.7)


def arrow_heavy(d):
    d.polygon([(C, C - R * 0.95), (C + R * 0.7, C - R * 0.1), (C + R * 0.28, C - R * 0.1),
               (C + R * 0.28, C + R * 0.95), (C - R * 0.28, C + R * 0.95),
               (C - R * 0.28, C - R * 0.1), (C - R * 0.7, C - R * 0.1)], fill=255)
    d.polygon([(C, C - R * 0.62), (C + R * 0.36, C - R * 0.22), (C - R * 0.36, C - R * 0.22)], fill=0)


print('symbols:')
for name, fn in (
        ('skull', skull), ('circuit', circuit), ('wave', wave), ('biohazard', biohazard),
        ('blossom', blossom), ('moon', moon), ('sun', sun), ('fang', fang), ('crown', crown),
        ('eye', eye), ('leaf', leaf), ('bolt', bolt), ('spade', spade), ('clover', clover),
        ('shuriken', shuriken), ('pentagram', pentagram), ('triquetra', triquetra), ('ankh', ankh),
        ('rune_algiz', rune_algiz), ('rune_thurisaz', rune_thurisaz), ('atom', atom),
        ('cross_swords', cross_swords), ('infinity_bold', infinity_bold), ('flame', flame),
        ('heart_bold', heart_bold), ('snow_crystal', snow_crystal), ('target', target),
        ('diamond_suit', diamond_suit), ('hexagon_bold', hexagon_bold), ('gear', gear),
        ('arrow_heavy', arrow_heavy)):
    emit(name, fn)
