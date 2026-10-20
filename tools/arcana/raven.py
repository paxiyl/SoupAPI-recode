"""The Raven Claw.

A crescent blade on a twisted grip. The crescent is a spine: twelve segment bones, each the child of
the one before, so rotating them together curls the claw shut or unfurls it open, and every segment
carries its own piece of frame, inner edge, a curved rib and a fan of spectral feathers, which ride
with it. Purple roses, a bird-skull gem and a short prong sit where the crescent meets the grip, and
the grip ends in a hooked, forked claw.
"""
import math

import numpy as np

import paint
from geo import Material, Model
from rig import Rig

ID = 'raven_claw'
NAME = 'Raven Claw'
TEX = 256
SEGMENTS = 12

FINISHES = {
    '': dict(frame=(0.82, 0.82, 0.86), edge=(0.95, 0.96, 1.0), inner=(0.28, 0.27, 0.32), bone=(0.80, 0.78, 0.72),
             feather_a=(0.03, 0.18, 0.22), feather_b=(0.35, 0.95, 0.95), petal_a=(0.95, 0.55, 1.0),
             petal_b=(0.35, 0.05, 0.55), gem=(0.85, 0.35, 1.0), leather=(0.20, 0.17, 0.16)),
    'obsidian': dict(frame=(0.18, 0.17, 0.22), edge=(0.55, 0.50, 0.65), inner=(0.06, 0.05, 0.08), bone=(0.25, 0.22, 0.28),
                     feather_a=(0.02, 0.02, 0.05), feather_b=(0.55, 0.35, 1.0), petal_a=(0.75, 0.35, 1.0),
                     petal_b=(0.15, 0.02, 0.25), gem=(0.75, 0.3, 1.0), leather=(0.06, 0.05, 0.07)),
    'molten': dict(frame=(0.35, 0.25, 0.20), edge=(1.0, 0.75, 0.35), inner=(0.12, 0.06, 0.04), bone=(0.45, 0.30, 0.22),
                   feather_a=(0.15, 0.02, 0.0), feather_b=(1.0, 0.55, 0.1), petal_a=(1.0, 0.5, 0.15),
                   petal_b=(0.4, 0.05, 0.0), gem=(1.0, 0.45, 0.1), leather=(0.12, 0.06, 0.04)),
    'spectral': dict(frame=(0.75, 0.88, 0.95), edge=(0.85, 1.0, 1.0), inner=(0.30, 0.45, 0.60), bone=(0.80, 0.92, 1.0),
                     feather_a=(0.10, 0.25, 0.45), feather_b=(0.75, 0.98, 1.0), petal_a=(0.70, 0.85, 1.0),
                     petal_b=(0.20, 0.30, 0.65), gem=(0.6, 0.9, 1.0), leather=(0.18, 0.22, 0.30)),
    'dark_metal': dict(frame=(0.30, 0.31, 0.35), edge=(0.70, 0.72, 0.78), inner=(0.10, 0.10, 0.12), bone=(0.40, 0.40, 0.42),
                       feather_a=(0.05, 0.05, 0.06), feather_b=(0.95, 0.20, 0.25), petal_a=(0.9, 0.2, 0.3),
                       petal_b=(0.25, 0.02, 0.05), gem=(1.0, 0.2, 0.25), leather=(0.08, 0.08, 0.09)),
}


def materials():
    return {
        'frame': Material('frame', 0, 0, 64, 64, 3), 'edge': Material('edge', 64, 0, 64, 64, 3),
        'inner': Material('inner', 128, 0, 64, 64, 3), 'bone': Material('bone', 192, 0, 64, 64, 3),
        'feather': Material('feather', 0, 64, 64, 64, 6), 'petal': Material('petal', 64, 64, 64, 64, 8),
        'gem': Material('gem', 128, 64, 64, 64, 8), 'leather': Material('leather', 192, 64, 64, 64, 3),
        'wrap': Material('wrap', 0, 128, 64, 64, 3), 'arm': Material('arm', 192, 192, 64, 64, 1),
    }


def paint_atlas(finish):
    p = FINISHES[finish]
    c = paint.Canvas(TEX, TEX)
    paint.metal(c, 0, 0, 64, 64, p['frame'], 1)
    paint.metal(c, 64, 0, 64, 64, p['edge'], 2, glow=0.25)
    paint.metal(c, 128, 0, 64, 64, p['inner'], 3, streak=False)
    paint.metal(c, 192, 0, 64, 64, p['bone'], 4, streak=False)
    paint.feather(c, 0, 64, 64, 64, p['feather_a'], p['feather_b'], 5)
    paint.petals(c, 64, 64, 64, 64, p['petal_a'], p['petal_b'], 6)
    paint.radial(c, 128, 64, 64, 64, (1.0, 0.9, 1.0), p['gem'], 0.7, glow_inner=1.0, seed=7)
    paint.leather(c, 192, 64, 64, 64, p['leather'], 8)
    paint.metal(c, 0, 128, 64, 64, tuple(v * 0.7 for v in p['frame']), 9)
    paint.flat(c, 192, 192, 64, 64, (0.8, 0.7, 0.6))
    return c


def build():
    m = Model(TEX, TEX, seed=11)
    m.mats = materials()
    mats = m.mats
    rig = Rig(m, grip_eye=(0.33, -0.46, -0.8), blade_dir_eye=(-0.22, 0.92, -0.32),
              face_normal_eye=(0.15, 0.12, 0.98), shoulder_eye=(0.36, -0.75, 0.32))
    z = rig.G[2]

    def W(x, y):
        return rig.at((x, y, 0))[:2]

    # ---- the crescent spine -------------------------------------------------------------------
    C = np.array([-1.2, 10.5])
    R = 8.5
    a0 = math.radians(-80)
    sweep = math.radians(215)
    angles = [a0 + sweep * i / SEGMENTS for i in range(SEGMENTS + 1)]
    pts = [C + R * np.array([math.cos(a), math.sin(a)]) for a in angles]
    parent = 'weapon'
    for k in range(SEGMENTS):
        P, Q = pts[k], pts[k + 1]
        seg = m.bone('spine_%d' % k, parent, rig.at((P[0], P[1], 0)))
        parent = seg
        inward = (C - (P + Q) / 2)
        inward /= np.linalg.norm(inward)
        taper = 1.0 if k < SEGMENTS - 3 else max(0.18, 1 - (k - (SEGMENTS - 4)) / 3.5)
        # Outer frame, inner cutting edge and (low on the claw) a second inner frame line.
        m.bar(seg, W(*P), W(*Q), 1.1 * taper, 1.0, mats['frame'], z=z, overlap=0.35)
        e = inward * (1.0 * taper)
        m.bar(seg, W(*(P + e)), W(*(Q + e)), 0.7 * taper, 0.35, mats['edge'], z=z, overlap=0.3)
        if 1 <= k <= 8:
            f = inward * 2.7
            m.bar(seg, W(*(P + f)), W(*(Q + f)), 0.35, 0.5, mats['inner'], z=z, overlap=0.3)
            mid = inward * 1.9
            m.bar(seg, W(*(P + e * 1.3)), W(*(P + f)), 0.3, 0.45, mats['inner'], z=z)
        # A curved rib off the outside, the "ribs" of the spine.
        if k <= 8:
            rib = m.bone('rib_%d' % k, seg, rig.at((P[0], P[1], 0)))
            out = -inward
            tangent = (Q - P) / np.linalg.norm(Q - P)
            length = 3.4 - k * 0.18
            a = P
            for j in range(3):
                d = out * math.cos(0.45 * j) + tangent * math.sin(0.45 * j)
                b = a + d * length / 3
                m.bar(rib, W(*a), W(*b), (0.55 - j * 0.15), 0.5, mats['bone'], z=z, overlap=0.15)
                a = b
            # Feathers between this rib and the next.
            fb = m.bone('feather_%d' % k, seg, rig.at((P[0], P[1], 0)))
            for j in range(3):
                t = (j + 0.5) / 3
                base = P + (Q - P) * t - inward * 0.5
                ang = math.atan2(out[1], out[0]) - 0.35 - j * 0.08
                tip = base + np.array([math.cos(ang), math.sin(ang)]) * (3.8 + 0.6 * math.sin(j + k))
                m.bar(fb, W(*base), W(*tip), 0.9, 0.18, mats['feather'], z=z - 0.15 + j * 0.12, overlap=0.1)
    # The tip: one last spike carrying on from the final segment.
    P, Q = pts[-2], pts[-1]
    d = (Q - P) / np.linalg.norm(Q - P)
    tip = m.bone('spine_tip', parent, rig.at((Q[0], Q[1], 0)))
    m.bar(tip, W(*Q), W(*(Q + d * 2.8 + (C - Q) / R * 0.6)), 0.25, 0.35, mats['edge'], z=z, overlap=0.2)

    # ---- the junction: roses, the skull gem, the prong ---------------------------------------
    m.bone('crown', 'weapon', rig.G)
    rose_spots = [(0.8, 2.6), (-0.4, 3.2), (-1.6, 2.7), (-2.6, 3.4), (1.6, 3.6)]
    for i, (rx, ry) in enumerate(rose_spots):
        for j in range(4):
            ang = j * 45 + i * 20
            cx, cy = W(rx, ry)
            m.cube('crown', (cx - 0.55, cy - 0.55, z + 0.25 + j * 0.08), (1.1, 1.1, 0.45), mats['petal'],
                   rotation=[0, 0, ang], pivot=[cx, cy, z + 0.5])
        cx, cy = W(rx, ry)
        m.cube('crown', (cx - 0.3, cy - 0.3, z + 0.7), (0.6, 0.6, 0.3), mats['gem'])
        m.cube('crown', (cx - 0.55, cy - 0.55, z - 0.95), (1.1, 1.1, 0.5), mats['petal'], rotation=[0, 0, 45 + i * 15],
               pivot=[cx, cy, z - 0.7])
    m.bone('skull_illuminated', 'crown', rig.G)
    sx, sy = W(0.4, 1.3)
    m.cube('skull_illuminated', (sx - 0.7, sy - 0.5, z + 0.7), (1.4, 1.2, 0.7), mats['gem'])
    m.bar('skull_illuminated', W(0.4, 0.8), W(0.4, -1.6), 0.55, 0.5, mats['gem'], z=z + 1.0)
    m.bar('skull_illuminated', W(0.4, -1.4), W(0.4, -2.4), 0.25, 0.35, mats['gem'], z=z + 1.0)
    m.bone('prong', 'crown', rig.at((2.0, 2.0, 0)))
    a = np.array([1.6, 2.0])
    for j in range(4):
        b = a + np.array([0.9, 1.15]) * (1 - j * 0.12)
        m.bar('prong', W(*a), W(*b), 0.9 - j * 0.2, 0.6, mats['frame'], z=z, overlap=0.2)
        a = b
    m.cube('crown', rig.at((-1.2, 0.0, -0.9)), (2.4, 2.2, 1.8), mats['frame'])

    # ---- the grip: an S-curve, wrapped, ending in a forked hook --------------------------------
    m.bone('grip', 'weapon', rig.G)
    prev = np.array([0.0, 0.5])
    for i in range(1, 12):
        y = 0.5 - i * 1.0
        cur = np.array([0.9 * math.sin(y * 0.32), y])
        m.bar('grip', W(*prev), W(*cur), 1.5, 1.3, mats['leather'], z=z, overlap=0.2)
        if i % 2 == 0:
            m.bar('grip', W(prev[0] - 0.8, prev[1]), W(cur[0] + 0.8, cur[1]), 0.3, 1.45, mats['wrap'], z=z)
        prev = cur
    m.bone('hook', 'grip', rig.at((prev[0], prev[1], 0)))
    a = prev
    ang = -math.pi / 2
    for j in range(5):
        ang += 0.55
        b = a + np.array([math.cos(ang), math.sin(ang)]) * 1.2
        m.bar('hook', W(*a), W(*b), 1.1 - j * 0.18, 0.9, mats['frame'], z=z, overlap=0.2)
        a = b
    fork = a + np.array([-0.9, 0.6])
    m.bar('hook', W(*a), W(*fork), 0.35, 0.6, mats['edge'], z=z)
    return m, rig, SEGMENTS


PROFILE = {
    'profile': 'raven_claw',
    'spine': SEGMENTS,
    'trail': [0.3, 0.95, 1.0],
    'trail2': [0.75, 0.35, 1.0],
    'feathers': True,
    'emissive': [0.85, 0.4, 1.0],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}

if __name__ == '__main__':
    m, rig, _ = build()
    print('cubes', m.cube_count, 'bones', len(m.bones), 'hold', rig.hold_rest, rig.hold_err)
