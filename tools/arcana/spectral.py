"""The Spectral Blade.

A slender, leaf-shaped blade whose core is light with runes running down it, wrapped in a translucent
energy layer and edged in silver; an ornate silver and dark-metal crossguard whose arms curl upward,
a gem at its heart; a ringed grip and a diamond pommel. Two spectral ribbons curve around the blade,
each a chain of seven bones so they can ripple.
"""
import math

import numpy as np

import paint
from geo import Material, Model
from rig import Rig

ID = 'spectral_blade'
NAME = 'Spectral Blade'
TEX = 256

FINISHES = {
    '': dict(core=(0.45, 0.95, 1.0), rune=(1.0, 1.0, 1.0), glass=(0.35, 0.65, 0.95), silver=(0.82, 0.85, 0.9),
             dark=(0.16, 0.18, 0.24), gem=(0.35, 0.6, 1.0), ribbon_a=(0.2, 0.6, 1.0), ribbon_b=(0.8, 1.0, 1.0)),
    'obsidian': dict(core=(0.55, 0.35, 1.0), rune=(1.0, 0.85, 1.0), glass=(0.10, 0.06, 0.16), silver=(0.35, 0.33, 0.42),
                     dark=(0.05, 0.04, 0.07), gem=(0.7, 0.3, 1.0), ribbon_a=(0.4, 0.1, 0.8), ribbon_b=(0.85, 0.6, 1.0)),
    'molten': dict(core=(1.0, 0.6, 0.15), rune=(1.0, 0.95, 0.7), glass=(0.35, 0.08, 0.02), silver=(0.55, 0.42, 0.32),
                   dark=(0.12, 0.05, 0.03), gem=(1.0, 0.4, 0.1), ribbon_a=(1.0, 0.3, 0.05), ribbon_b=(1.0, 0.85, 0.4)),
    'spectral': dict(core=(0.8, 1.0, 1.0), rune=(0.6, 0.95, 1.0), glass=(0.65, 0.9, 1.0), silver=(0.92, 0.96, 1.0),
                     dark=(0.40, 0.50, 0.65), gem=(0.75, 0.95, 1.0), ribbon_a=(0.6, 0.9, 1.0), ribbon_b=(1.0, 1.0, 1.0)),
    'dark_metal': dict(core=(1.0, 0.25, 0.3), rune=(1.0, 0.8, 0.8), glass=(0.14, 0.14, 0.17), silver=(0.45, 0.46, 0.5),
                       dark=(0.07, 0.07, 0.08), gem=(1.0, 0.2, 0.25), ribbon_a=(0.8, 0.1, 0.15), ribbon_b=(1.0, 0.6, 0.6)),
}

CORE_STRIP = (0, 0, 256, 24)
GLASS_STRIP = (0, 24, 256, 32)


def materials():
    return {
        'core': Material('core', 0, 0, 256, 24, 6), 'glass': Material('glass', 0, 24, 256, 32, 6),
        'silver': Material('silver', 0, 64, 64, 64, 3), 'dark': Material('dark', 64, 64, 64, 64, 3),
        'gem': Material('gem', 128, 64, 64, 64, 10), 'ribbon': Material('ribbon', 192, 64, 64, 64, 6),
        'wrap': Material('wrap', 0, 128, 64, 64, 4), 'arm': Material('arm', 192, 192, 64, 64, 1),
    }


def paint_atlas(finish):
    p = FINISHES[finish]
    c = paint.Canvas(TEX, TEX)
    paint.runes_strip(c, *CORE_STRIP, p['core'], p['rune'], 21)
    gp = dict(glass_a=p['glass'], glass_b=tuple(min(1, v * 1.4 + 0.1) for v in p['glass']), energy=p['core'],
              crack=tuple(v * 0.4 for v in p['glass']), glow=0.5)
    paint.glass_strip(c, *GLASS_STRIP, gp, seed=22, cracks=0.4)
    paint.metal(c, 0, 64, 64, 64, p['silver'], 23, glow=0.1)
    paint.metal(c, 64, 64, 64, 64, p['dark'], 24, streak=False)
    paint.radial(c, 128, 64, 64, 64, (1, 1, 1), p['gem'], 0.7, glow_inner=1.0, seed=25)
    paint.feather(c, 192, 64, 64, 64, p['ribbon_a'], p['ribbon_b'], 26)
    c.glow[64:128, 192:256] = np.clip(c.glow[64:128, 192:256] + 0.6, 0, 1)
    paint.wrap(c, 0, 128, 64, 64, tuple(v * 0.6 for v in p['dark']), p['silver'], 27)
    paint.flat(c, 192, 192, 64, 64, (0.8, 0.7, 0.6))
    return c


def build():
    m = Model(TEX, TEX, seed=21)
    m.mats = materials()
    mats = m.mats
    rig = Rig(m, grip_eye=(0.34, -0.48, -0.74), blade_dir_eye=(-0.34, 0.84, -0.42),
              face_normal_eye=(0.25, 0.2, 0.95), shoulder_eye=(0.36, -0.75, 0.32))
    z = rig.G[2]

    def W(x, y):
        return rig.at((x, y, 0))[:2]

    # ---- the blade: rune core, energy layers either side, silver edges ----------------------------
    m.bone('blade_illuminated', 'weapon', rig.G)
    m.bone('blade', 'weapon', rig.G)
    L, n, base = 24.0, 18, 2.4
    for i in range(n):
        f0, f1 = i / n, (i + 1) / n
        y0, y1 = base + L * f0, base + L * f1
        fm = (f0 + f1) / 2
        w = 2.4 * (1 - fm) ** 0.55 * (1 + 0.25 * math.sin(fm * math.pi)) + 0.15
        cu = (CORE_STRIP[0] + f0 * 256, CORE_STRIP[1] + 4, CORE_STRIP[0] + f1 * 256, CORE_STRIP[1] + 20)
        gu = (GLASS_STRIP[0] + f0 * 256, GLASS_STRIP[1], GLASS_STRIP[0] + f1 * 256, GLASS_STRIP[1] + 32)
        m.bar('blade_illuminated', W(0, y0), W(0, y1), w * 0.42, 0.45, mats['core'], z=z, overlap=0.1, strip=cu)
        for side in (1, -1):
            m.bar('blade', W(0, y0), W(0, y1), w, 0.25, mats['glass'], z=z + side * 0.28, overlap=0.1, strip=gu)
            m.bar('blade', W(side * (w / 2 + 0.1), y0), W(side * (w / 2 + 0.1), y1), 0.22, 0.4, mats['silver'],
                  z=z, overlap=0.15)
    m.bar('blade', W(0, base + L - 0.2), W(0, base + L + 1.4), 0.2, 0.35, mats['silver'], z=z)

    # ---- crossguard ----------------------------------------------------------------------------
    m.bone('guard', 'weapon', rig.G)
    m.cube('guard', rig.at((-1.6, 0.6, -0.8)), (3.2, 1.6, 1.6), mats['silver'])
    m.cube('guard', rig.at((-1.0, 0.4, -1.0)), (2.0, 2.0, 2.0), mats['dark'])
    m.bone('guard_gem_illuminated', 'guard', rig.G)
    for side in (1, -1):
        m.cube('guard_gem_illuminated', rig.at((-0.55, 0.85, side * 1.05 - 0.25)), (1.1, 1.1, 0.5), mats['gem'])
        # Arms that sweep out then curl up toward the blade.
        a = np.array([side * 1.6, 1.4])
        ang = 0.0 if side > 0 else math.pi
        for j in range(7):
            ang += side * 0.32
            b = a + np.array([math.cos(ang), math.sin(ang)]) * 1.15
            m.bar('guard', W(*a), W(*b), 0.75 - j * 0.07, 0.8, mats['silver'], z=z, overlap=0.15)
            if j < 5:
                m.bar('guard', W(*a), W(*b), 0.3, 1.1, mats['dark'], z=z, overlap=0.1)
            a = b
        m.cube('guard', (W(*a)[0] - 0.35, W(*a)[1] - 0.35, z - 0.35), (0.7, 0.7, 0.7), mats['gem'])
        # A small downward spike under each arm.
        m.bar('guard', W(side * 1.4, 0.7), W(side * 2.4, -0.6), 0.35, 0.5, mats['silver'], z=z)

    # ---- grip and pommel -----------------------------------------------------------------------
    m.bone('grip', 'weapon', rig.G)
    m.cube('grip', rig.at((-0.65, -6.5, -0.65)), (1.3, 7.0, 1.3), mats['wrap'])
    for y in (-6.4, -4.6, -2.8, -1.0, 0.3):
        m.cube('grip', rig.at((-0.8, y, -0.8)), (1.6, 0.35, 1.6), mats['silver'])
    m.cube('grip', rig.at((-0.7, -8.0, -0.7)), (1.4, 1.4, 1.4), mats['silver'], rotation=[0, 0, 45],
           pivot=list(rig.at((0, -7.3, 0))))
    m.bone('pommel_illuminated', 'grip', rig.G)
    m.cube('pommel_illuminated', rig.at((-0.4, -7.7, -0.75)), (0.8, 0.8, 1.5), mats['gem'])
    m.cube('grip', rig.at((-0.3, -9.2, -0.3)), (0.6, 0.9, 0.6), mats['silver'])

    # ---- two spectral ribbons, each a chain of seven bones ----------------------------------------
    for r, (side, y0, turn) in enumerate(((1, 3.0, 0.42), (-1, 9.0, -0.38))):
        parent = 'weapon'
        a = np.array([side * 1.4, y0])
        ang = math.pi / 2 + side * 0.9
        for k in range(7):
            ang -= turn
            b = a + np.array([math.cos(ang), math.sin(ang)]) * 2.1
            bone = m.bone('ribbon_%d_%d_illuminated' % (r, k), parent, rig.at((a[0], a[1], 0)))
            m.bar(bone, W(*a), W(*b), 0.95 * (1 - k * 0.09), 0.12, mats['ribbon'],
                  z=z + side * (0.9 - 0.25 * abs(k - 3)), overlap=0.25)
            parent = bone
            a = b
    return m, rig, 0


PROFILE = {
    'profile': 'spectral_blade',
    'trail': [0.35, 0.9, 1.0],
    'trail2': [0.85, 0.95, 1.0],
    'ribbons': 2,
    'emissive': [0.45, 0.95, 1.0],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}

if __name__ == '__main__':
    m, rig, _ = build()
    print('cubes', m.cube_count, 'bones', len(m.bones), 'hold', rig.hold_rest, rig.hold_err)
