"""The Fractured-Eye Katana.

A long, slightly curved central blade of fractured purple glass with dark metal edges, two scythe-like
secondary blades sweeping out of its edge toward the tip and a small hook rising off its spine, a
floral guard, a gold collar, a wrapped handle and a pommel. Seventeen eyes, modelled in layers - gold
rim, sclera, iris, slit pupil - sit along every blade on both faces; each eye's iris and pupil live
in their own `_illuminated` bone so the mod can make them glow, dilate and dart.
"""
import math

import numpy as np

import paint
from geo import Material, Model
from rig import Rig

ID = 'fractured_eye_katana'
TEX = 512

FINISHES = {
    # name: glass_a, glass_b, energy, crack, edge, spine, guard, glow
    '': dict(glass_a=(0.42, 0.10, 0.62), glass_b=(0.95, 0.30, 0.78), energy=(0.85, 0.45, 1.0), crack=(0.08, 0.02, 0.12),
             edge=(0.62, 0.60, 0.70), spine=(0.12, 0.10, 0.16), guard=(0.40, 0.18, 0.55), glow=1.0),
    'obsidian': dict(glass_a=(0.05, 0.04, 0.08), glass_b=(0.22, 0.12, 0.30), energy=(0.70, 0.30, 1.0), crack=(0.0, 0.0, 0.0),
                     edge=(0.35, 0.33, 0.40), spine=(0.06, 0.05, 0.08), guard=(0.12, 0.08, 0.16), glow=0.9),
    'molten': dict(glass_a=(0.12, 0.03, 0.02), glass_b=(0.45, 0.08, 0.02), energy=(1.0, 0.55, 0.08), crack=(0.02, 0.0, 0.0),
                   edge=(0.30, 0.22, 0.18), spine=(0.10, 0.05, 0.03), guard=(0.35, 0.12, 0.05), glow=1.2),
    'spectral': dict(glass_a=(0.45, 0.80, 0.95), glass_b=(0.75, 0.70, 1.0), energy=(0.70, 0.95, 1.0), crack=(0.10, 0.20, 0.35),
                     edge=(0.85, 0.90, 0.95), spine=(0.30, 0.40, 0.55), guard=(0.40, 0.55, 0.75), glow=1.1),
    'dark_metal': dict(glass_a=(0.16, 0.17, 0.20), glass_b=(0.30, 0.31, 0.36), energy=(0.95, 0.15, 0.20), crack=(0.02, 0.02, 0.03),
                       edge=(0.70, 0.72, 0.78), spine=(0.08, 0.08, 0.10), guard=(0.20, 0.20, 0.24), glow=0.8),
}

STRIPS = {'main': (0, 0, 512, 48), 'ext_a': (0, 48, 512, 40), 'ext_b': (0, 88, 512, 36), 'hook': (0, 124, 512, 32)}


def materials():
    t = 160
    return {
        'edge': Material('edge', 0, t, 64, 64, 3), 'spine': Material('spine', 64, t, 64, 64, 3),
        'guard': Material('guard', 128, t, 64, 64, 3), 'gold': Material('gold', 192, t, 64, 64, 3),
        'wrap': Material('wrap', 256, t, 64, 64, 4), 'grip': Material('grip', 320, t, 64, 64, 3),
        'pommel': Material('pommel', 384, t, 64, 64, 3), 'rim': Material('rim', 448, t, 64, 64, 3),
        'sclera': Material('sclera', 0, 224, 64, 64, 10), 'iris': Material('iris', 64, 224, 64, 64, 20),
        'pupil': Material('pupil', 128, 224, 64, 64, 6), 'glass': Material('glass', 0, 0, 512, 48, 4),
        'arm': Material('arm', 448, 448, 64, 64, 1),
    }


def paint_atlas(finish):
    p = FINISHES[finish]
    c = paint.Canvas(TEX, TEX)
    for i, (name, (x, y, w, h)) in enumerate(STRIPS.items()):
        paint.glass_strip(c, x, y, w, h, p, seed=10 + i, cracks=1.2 if name == 'main' else 1.0)
    t = 160
    paint.metal(c, 0, t, 64, 64, p['edge'], 1)
    paint.metal(c, 64, t, 64, 64, p['spine'], 2)
    paint.metal(c, 128, t, 64, 64, p['guard'], 3, streak=False, glow=0.1)
    paint.metal(c, 192, t, 64, 64, (0.95, 0.72, 0.25), 4, glow=0.15)
    paint.wrap(c, 256, t, 64, 64, (0.05, 0.16, 0.16), (0.92, 0.70, 0.25), 5)
    paint.metal(c, 320, t, 64, 64, (0.06, 0.12, 0.12), 6, streak=False)
    paint.metal(c, 384, t, 64, 64, p['guard'], 7, streak=False)
    paint.metal(c, 448, t, 64, 64, (0.98, 0.78, 0.30), 8, glow=0.35)
    paint.radial(c, 0, 224, 64, 64, (1.0, 0.92, 0.45), (0.85, 0.55, 0.10), 1.3, glow_inner=0.3, seed=9)
    paint.radial(c, 64, 224, 64, 64, (1.0, 0.25, 0.10), (0.55, 0.0, 0.02), 0.9, glow_inner=1.0,
                 ring=(1.0, 0.65, 0.2), seed=10)
    paint.flat(c, 128, 224, 64, 64, (0.03, 0.0, 0.01), seed=11)
    paint.flat(c, 448, 448, 64, 64, (0.8, 0.7, 0.6))
    return c


def curve_points(start, angle0, length, bend, n, power=1.3):
    """A centreline that starts at `start` heading `angle0` (radians) and turns by `bend` over its
    length, the turn concentrated toward the end, as a scythe blade sweeps."""
    pts = [np.array(start, float)]
    angs = []
    seg = length / n
    for i in range(n):
        f = (i + 0.5) / n
        a = angle0 + bend * f ** power
        angs.append(a)
        pts.append(pts[-1] + seg * np.array([math.cos(a), math.sin(a)]))
    return pts, angs


def blade(m, bone, rig, pts, widths, strip, edge_side, thick=0.8, bevel=True, eyes=(), eye_scale=1.0):
    """Lays a layered blade along a centreline: glass core, a raised bevel ridge, a dark cutting edge and
    a dark spine. Returns the centre and tangent angle at each eye fraction."""
    mats = m.mats
    n = len(pts) - 1
    lengths = [np.linalg.norm(pts[i + 1] - pts[i]) for i in range(n)]
    total = sum(lengths)
    sx, sy, sw, sh = strip
    s_acc = 0.0
    for i in range(n):
        a, b = pts[i], pts[i + 1]
        d = (b - a) / max(1e-6, lengths[i])
        normal = np.array([-d[1], d[0]]) * edge_side   # points toward the spine
        w = widths[i]
        u0 = sx + s_acc / total * sw
        u1 = sx + (s_acc + lengths[i]) / total * sw
        vpad = sh * (1 - min(1.0, w / 2.0)) / 2
        strip_uv = (u0, sy + vpad, u1, sy + sh - vpad)
        A = rig.at((a[0], a[1], 0))
        B = rig.at((b[0], b[1], 0))
        z = rig.G[2]
        m.bar(bone, A[:2], B[:2], w, thick, mats['glass'], z=z, overlap=0.12, strip=strip_uv)
        if bevel and w > 0.5:
            off = normal * w * 0.12
            m.bar(bone, (A[0] + off[0], A[1] + off[1]), (B[0] + off[0], B[1] + off[1]), w * 0.5, thick + 0.3,
                  mats['glass'], z=z, overlap=0.1, strip=strip_uv)
        e = -normal * (w / 2 + 0.16)
        m.bar(bone, (A[0] + e[0], A[1] + e[1]), (B[0] + e[0], B[1] + e[1]), 0.4 if w > 0.3 else 0.2, thick * 0.4,
              mats['edge'], z=z, overlap=0.1)
        sp = normal * (w / 2 + 0.12)
        m.bar(bone, (A[0] + sp[0], A[1] + sp[1]), (B[0] + sp[0], B[1] + sp[1]), 0.32, thick * 0.9,
              mats['spine'], z=z, overlap=0.1)
        s_acc += lengths[i]
    out = []
    for f in eyes:
        target = f * total
        acc = 0.0
        for i in range(n):
            if acc + lengths[i] >= target:
                t = (target - acc) / lengths[i]
                p = pts[i] + (pts[i + 1] - pts[i]) * t
                d = pts[i + 1] - pts[i]
                out.append((p, math.atan2(d[1], d[0]), widths[i]))
                break
            acc += lengths[i]
    return out


def eye(m, rig, parent, k, centre, angle, width, thick):
    """One eye on both faces of a blade: gold rim, sclera, and an `_illuminated` iris and pupil."""
    mats = m.mats
    size = min(1.0, width / 1.7)
    a, b = 0.85 * size, 0.42 * size
    ca, sa = math.cos(angle), math.sin(angle)
    C = rig.at((centre[0], centre[1], 0))
    name = 'eye_%d' % k
    m.bone(name, parent, C)
    iris = m.bone('eye_%d_iris_illuminated' % k, name, C)

    def to_world(e, f):
        return (C[0] + e * ca - f * sa, C[1] + e * sa + f * ca)

    for side in (1, -1):
        z0 = C[2] + side * (thick / 2)
        # Sclera: an almond from two bars.
        m.bar(name, to_world(-a * 0.85, 0), to_world(a * 0.85, 0), b * 1.3, 0.16, mats['sclera'], z=z0 + side * 0.06)
        m.bar(name, to_world(-a * 0.45, 0), to_world(a * 0.45, 0), b * 1.9, 0.16, mats['sclera'], z=z0 + side * 0.06)
        # Gold rim around the almond, raised above the sclera.
        ring = [(a * math.cos(t), b * 1.15 * math.sin(t)) for t in np.linspace(0, math.tau, 7)]
        for j in range(6):
            m.bar(name, to_world(*ring[j]), to_world(*ring[j + 1]), 0.2, 0.22, mats['rim'], z=z0 + side * 0.16,
                  overlap=0.08)
        # Iris and slit pupil, in the bone that animates.
        r = 0.42 * size
        m.bar(iris, to_world(-r, 0), to_world(r, 0), r * 2, 0.16, mats['iris'], z=z0 + side * 0.2)
        m.bar(iris, to_world(-0.1 * size, 0), to_world(0.1 * size, 0), r * 1.7, 0.12, mats['pupil'], z=z0 + side * 0.28)


def build():
    m = Model(TEX, TEX, seed=7)
    m.mats = materials()
    # Grip right of centre and below; the blade up, left and away; its flat toward the camera.
    rig = Rig(m, grip_eye=(0.36, -0.47, -0.72), blade_dir_eye=(-0.38, 0.80, -0.46),
              face_normal_eye=(0.25, 0.25, 0.94), shoulder_eye=(0.36, -0.75, 0.32),
              left_shoulder_eye=(-0.30, -0.80, 0.30), left_grip_local=(0, -5.5, 0))
    mats = m.mats
    z = rig.G[2]
    gx, gy = rig.G[0], rig.G[1]

    # Handle: core, wrap bands, diamonds and the pommel.
    m.bone('handle', 'weapon', rig.G)
    m.cube('handle', (gx - 0.75, gy - 7.5, z - 0.6), (1.5, 7.5, 1.2), mats['grip'])
    for i in range(7):
        y = gy - 7.0 + i
        for side in (1, -1):
            m.bar('handle', (gx - 0.75, y), (gx + 0.75, y + 0.9), 0.55, 0.15, mats['wrap'], z=z + side * 0.62)
            m.bar('handle', (gx, y + 0.1), (gx, y + 0.85), 0.55, 0.12, mats['gold'], z=z + side * 0.66)
    m.cube('handle', (gx - 0.95, gy - 8.4, z - 0.75), (1.9, 0.95, 1.5), mats['pommel'])
    m.cube('handle', (gx - 0.6, gy - 8.8, z - 0.5), (1.2, 0.45, 1.0), mats['gold'])

    # Guard: a lobed disc perpendicular to the blade, and the collar.
    m.bone('guard', 'weapon', rig.G)
    m.cube('guard', (gx - 1.25, gy, z - 1.0), (2.5, 0.7, 2.0), mats['guard'])
    for k in range(8):
        m.cube('guard', (gx + 1.0, gy + 0.05, z - 0.5), (1.6, 0.6, 1.0), mats['guard'],
               rotation=[0, k * 45, 0], pivot=[gx, gy + 0.35, z])
        m.cube('guard', (gx + 0.7, gy + 0.62, z - 0.18), (0.6, 0.18, 0.36), mats['gold'],
               rotation=[0, k * 45 + 22.5, 0], pivot=[gx, gy + 0.35, z])
    m.cube('guard', (gx - 0.95, gy + 0.7, z - 0.7), (1.9, 1.3, 1.4), mats['gold'])

    # The main blade.
    m.bone('blade', 'weapon', rig.G)
    L = 21.0
    n = 36
    pts = [np.array([1.6 * (i / n) ** 2, 2.0 + L * i / n]) for i in range(n + 1)]
    widths = []
    for i in range(n):
        f = (i + 0.5) / n
        w = 1.9 - 0.4 * f
        if f > 0.9:
            w *= max(0.08, 1 - (f - 0.9) / 0.1) ** 0.8
        widths.append(w)
    eye_fracs = [0.08 + 0.07 * i for i in range(12)][:11]
    main_eyes = blade(m, 'blade', rig, pts, widths, STRIPS['main'], edge_side=1, eyes=eye_fracs)

    def branch_at(frac, side):
        p = pts[0] + (pts[-1] - pts[0]) * frac
        p = np.array([1.6 * frac ** 2, 2.0 + L * frac])
        d = np.array([3.2 * frac / L, 1.0]); d /= np.linalg.norm(d)
        normal = np.array([-d[1], d[0]])
        return p + normal * side * 0.8, math.atan2(d[1], d[0])

    eyes = [(c, a, w, 'blade', 0.8) for (c, a, w) in main_eyes]

    # Two scythe blades sweep out of the cutting edge (-x) toward the tip; a hook rises off the spine.
    for name, frac, length, nseg, w0, bend, strip, eye_fr in (
            ('ext_a', 0.40, 10.5, 16, 1.6, -2.4, STRIPS['ext_a'], (0.25, 0.5, 0.72)),
            ('ext_b', 0.62, 7.8, 12, 1.35, -2.2, STRIPS['ext_b'], (0.3, 0.62))):
        start, base_ang = branch_at(frac, -1)
        m.bone(name, 'blade', rig.at((start[0], start[1], 0)))
        a0 = base_ang + math.pi / 2 + 0.25      # away from the edge, a little back toward the guard
        bpts, _ = curve_points(start, a0, length, bend, nseg)
        bw = [w0 * (1 - (i + 0.5) / nseg) ** 0.7 + 0.08 for i in range(nseg)]
        for (c, a, w) in blade(m, name, rig, bpts, bw, strip, edge_side=-1, thick=0.7, eyes=eye_fr):
            eyes.append((c, a, w, name, 0.7))
    start, base_ang = branch_at(0.22, 1)
    m.bone('hook', 'blade', rig.at((start[0], start[1], 0)))
    hpts, _ = curve_points(start, base_ang - math.pi / 2 + 0.2, 5.0, 1.7, 8)
    hw = [1.1 * (1 - (i + 0.5) / 8) ** 0.8 + 0.08 for i in range(8)]
    for (c, a, w) in blade(m, 'hook', rig, hpts, hw, STRIPS['hook'], edge_side=1, thick=0.6, eyes=(0.4,)):
        eyes.append((c, a, w, 'hook', 0.6))

    for k, (c, a, w, parent, thick) in enumerate(eyes):
        eye(m, rig, parent, k, c, a, w, thick)

    return m, rig, len(eyes)


if __name__ == '__main__':
    m, rig, n = build()
    print('cubes', m.cube_count, 'bones', len(m.bones), 'eyes', n, 'hold rest', rig.hold_rest, 'err', rig.hold_err)

NAME = 'Fractured-Eye Katana'
# Read by the mod's weapon effects: which behaviours this weapon has.
PROFILE = {
    'profile': 'fractured_eye',
    'eyes': True,
    'summon': True,
    'trail': [0.85, 0.35, 1.0],
    'trail2': [1.0, 0.25, 0.55],
    'shards': True,
    'emissive': [1.0, 0.2, 0.15],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}
