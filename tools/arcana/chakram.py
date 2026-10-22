"""Seraph's Halo — a winged chakram.

A ring of bright steel with a serrated outer edge, held by a bar across its middle like a
wind-and-fire wheel. Inside the blade runs a band of runes and four stones; off its rim spread two
golden wings. The ring itself is on its own bone, so it can turn in the hand while the grip stays
put: it idles slowly, whirs up through a strike, and is spun like a top on the finger to inspect.
"""
import math

import numpy as np

import mesh as M
from geo import Material, Model
from rig import Rig, shrink

ID = 'seraph_halo'
NAME = "Seraph's Halo"
TEX = 512
TEETH = 18

MATERIALS = ['steel', 'edge', 'gold', 'rune', 'gem', 'feather', 'feather_tip', 'wrap', 'dark']
EMISSIVE = {'rune', 'gem', 'edge'}

ATLAS = M.Atlas(TEX)
ATLAS.pack(MATERIALS + ['arm'])

FINISHES = ['', 'obsidian', 'molten', 'spectral', 'dark_metal']

PALETTES = {
    '':           dict(steel=(0.82, 0.84, 0.90), edge=(0.95, 0.98, 1.00), gold=(1.00, 0.78, 0.30),
                       rune=(0.40, 0.85, 1.00), gem=(0.30, 0.70, 1.00), feather=(1.00, 0.98, 0.92),
                       tip=(1.00, 0.80, 0.40), wrap=(0.25, 0.20, 0.45), dark=(0.12, 0.12, 0.18)),
    'obsidian':   dict(steel=(0.20, 0.18, 0.26), edge=(0.70, 0.50, 1.00), gold=(0.60, 0.40, 0.90),
                       rune=(0.75, 0.40, 1.00), gem=(0.70, 0.25, 1.00), feather=(0.35, 0.20, 0.55),
                       tip=(0.80, 0.30, 1.00), wrap=(0.10, 0.06, 0.16), dark=(0.04, 0.03, 0.06)),
    'molten':     dict(steel=(0.40, 0.28, 0.20), edge=(1.00, 0.75, 0.35), gold=(1.00, 0.62, 0.15),
                       rune=(1.00, 0.55, 0.10), gem=(1.00, 0.35, 0.05), feather=(1.00, 0.55, 0.15),
                       tip=(1.00, 0.25, 0.05), wrap=(0.30, 0.10, 0.04), dark=(0.10, 0.05, 0.03)),
    'spectral':   dict(steel=(0.75, 0.90, 0.98), edge=(0.85, 1.00, 1.00), gold=(0.75, 0.95, 1.00),
                       rune=(0.60, 1.00, 1.00), gem=(0.55, 0.95, 1.00), feather=(0.75, 0.95, 1.00),
                       tip=(0.40, 0.80, 1.00), wrap=(0.25, 0.35, 0.50), dark=(0.20, 0.28, 0.36)),
    'dark_metal': dict(steel=(0.32, 0.33, 0.37), edge=(0.80, 0.82, 0.88), gold=(0.70, 0.66, 0.55),
                       rune=(1.00, 0.30, 0.30), gem=(1.00, 0.20, 0.25), feather=(0.40, 0.38, 0.36),
                       tip=(0.90, 0.20, 0.25), wrap=(0.12, 0.10, 0.12), dark=(0.06, 0.06, 0.07)),
}


def _rect(name):
    x, y, w, h = ATLAS.rects[name]
    return (int(x * TEX), int(y * TEX), int(w * TEX), int(h * TEX))


def paint_atlas(finish):
    import paint
    import paint_mesh as pm
    p = PALETTES.get(finish or '', PALETTES[''])
    c = paint.Canvas(TEX, TEX)
    t = pm.tile
    t(c, ATLAS, 'steel', TEX, pm.metal, p['steel'], 81, polish=0.85, scratches=0.25)
    t(c, ATLAS, 'edge', TEX, pm.metal, p['edge'], 82, polish=1.0, scratches=0.1, glow=0.3)
    t(c, ATLAS, 'gold', TEX, pm.metal, p['gold'], 83, polish=0.9, scratches=0.15, glow=0.1)
    t(c, ATLAS, 'rune', TEX, pm.runes, p['dark'], p['rune'], 84, glow=0.95)
    t(c, ATLAS, 'gem', TEX, pm.stone, p['gem'], 85, glow=0.9)
    t(c, ATLAS, 'feather', TEX, pm.feather, p['feather'], p['tip'], 86, glow=0.35)
    t(c, ATLAS, 'feather_tip', TEX, pm.feather, p['tip'], p['feather'], 87, glow=0.45)
    t(c, ATLAS, 'wrap', TEX, pm.sinew, p['wrap'], 88, wet=0.3)
    t(c, ATLAS, 'dark', TEX, pm.metal, p['dark'], 89, polish=0.3)
    paint.flat(c, *_rect('arm'), (0.55, 0.42, 0.34), seed=1)
    return c


R_OUT, R_IN = 8.0, 6.2


def build():
    m = Model(TEX, TEX, seed=29)
    ax, ay, aw, ah = _rect('arm')
    m.mats = {'arm': Material('arm', ax, ay, aw, ah, 1)}
    # Held like a wind-and-fire wheel: the fist round the bar, the ring standing up out of it,
    # turned three quarters toward what it is about to be thrown at.
    rig = Rig(m, grip_eye=(0.24, -0.40, -0.58), blade_dir_eye=(-0.36, 0.86, -0.36),
              face_normal_eye=(0.30, 0.22, 0.93), shoulder_eye=(0.50, -1.10, -0.10))
    G = rig.G
    groups = {}

    def part(bone, *meshes):
        for mesh in meshes:
            mesh.translate(G[0], G[1], G[2])
        groups.setdefault(bone, []).extend(meshes)

    # The ring's centre sits above the fist; the bar the hand holds runs across its lower part.
    C = M.v3(0, R_IN - 1.2, 0)

    m.bone('grip', 'weapon', G)
    bar = M.tube(M.catmull([M.v3(-R_IN * 0.78, 0, 0), M.v3(0, -0.15, 0), M.v3(R_IN * 0.78, 0, 0)], 16),
                 [0.62, 0.72, 0.62], 9, 'wrap').smooth()
    caps = []
    for side in (1, -1):
        cap = M.lathe([(0.0, 0.0), (0.85, 0.15), (0.95, 0.7), (0.6, 1.0)], 12, 'gold', closed_bottom=True).smooth()
        cap.rotate(M.v3(0, 0, 1), -90 * side)
        cap.translate(side * R_IN * 0.66, 0, 0)
        caps.append(cap)
    part('grip', bar, *caps)

    # ---- the ring, on its own bone so it can turn in the hand ------------------------------------
    m.bone('ring_spin', 'weapon', (G[0] + C[0], G[1] + C[1], G[2] + C[2]))
    ring = []
    # Body: a flat band between the two radii, bevelled, built as wedges round the circle.
    segs = TEETH * 2
    for k in range(segs):
        a0 = k / segs * math.tau
        a1 = (k + 1) / segs * math.tau
        outline = [(math.cos(a0) * R_IN, math.sin(a0) * R_IN), (math.cos(a0) * R_OUT * 0.9, math.sin(a0) * R_OUT * 0.9),
                   (math.cos(a1) * R_OUT * 0.9, math.sin(a1) * R_OUT * 0.9), (math.cos(a1) * R_IN, math.sin(a1) * R_IN)]
        plate = M.blade(outline, 0.32, 'steel', bevel=0.6)
        ring.append(plate)
    # The teeth: hooked points round the outside, all raked the same way.
    for k in range(TEETH):
        a = k / TEETH * math.tau
        a2 = a + math.tau / TEETH * 0.55
        tip = a + math.tau / TEETH * 0.95
        tooth = [(math.cos(a) * R_OUT * 0.88, math.sin(a) * R_OUT * 0.88),
                 (math.cos(tip) * R_OUT * 1.14, math.sin(tip) * R_OUT * 1.14),
                 (math.cos(a2 + 0.08) * R_OUT * 0.88, math.sin(a2 + 0.08) * R_OUT * 0.88)]
        ring.append(M.blade(tooth, 0.16, 'edge', bevel=0.35))
    # Gold beads chased round the inner and outer edge of the band.
    for r, mat in ((R_IN, 'gold'), (R_OUT * 0.9, 'gold')):
        bead = M.torus(r, 0.16, 64, 6, mat)
        bead.rotate(M.v3(1, 0, 0), 90)
        ring.append(bead.smooth())
    # The rune band, standing just proud of each face.
    for side in (1, -1):
        band = M.torus((R_IN + R_OUT * 0.9) / 2, 0.12, 64, 5, 'rune')
        band.rotate(M.v3(1, 0, 0), 90)
        band.scale(1.0, 1.0, 0.6)
        band.translate(0, 0, side * 0.26)
        ring.append(band.smooth())
    for mesh in ring:
        mesh.translate(C)
    part('ring_spin', *[r.smooth() for r in ring])

    m.bone('ring_gems_illuminated', 'ring_spin', (G[0] + C[0], G[1] + C[1], G[2] + C[2]))
    gems = []
    for k in range(4):
        a = k / 4 * math.tau + math.pi / 4
        rr = (R_IN + R_OUT * 0.9) / 2
        for side in (1, -1):
            g = M.gem(0.55, 0.7, 8, 'gem')
            g.rotate(M.v3(1, 0, 0), 90 * side)
            g.translate(math.cos(a) * rr, math.sin(a) * rr, side * 0.42)
            g.translate(C)
            gems.append(g)
    part('ring_gems_illuminated', *gems)

    # ---- the wings, off the rim; on their own bones so they can open and beat --------------------
    for side, name in ((1, 'wing_r'), (-1, 'wing_l')):
        root = C + M.v3(side * R_OUT * 0.82, R_OUT * 0.35, 0)
        m.bone(name, 'weapon', (G[0] + root[0], G[1] + root[1], G[2] + root[2]))
        wing = []
        for j in range(6):
            length = 7.5 - j * 0.75
            vane = M.feather(length, 2.0 - j * 0.12, 'feather' if j < 4 else 'feather_tip',
                             curve=0.25, cup=0.25, rib='gold', rib_radius=0.07)
            vane.rotate(M.v3(0, 0, 1), -side * (18 + j * 17))
            vane.translate(root + M.v3(side * j * 0.25, -j * 0.35, -0.2 + j * 0.08))
            wing.append(vane)
        part(name, *wing)

    shrink(m, groups, rig, 0.42)
    for meshes in groups.values():
        for mesh in meshes:
            ATLAS.apply(mesh)
    return m, rig, groups


PROFILE = {
    'profile': 'seraph_halo',
    'summon': True,
    'summon_style': 'wing',
    'trail': [1.0, 0.82, 0.40],
    'trail2': [0.40, 0.85, 1.00],
    'feathers': True,
    'emissive': [0.5, 0.85, 1.0],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}

if __name__ == '__main__':
    model, rig_, groups_ = build()
    print('bones', len(model.bones), 'groups', len(groups_),
          'triangles', sum(len(x) for v in groups_.values() for x in v))
