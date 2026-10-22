"""The Spectral Blade — a blade built as triangles, from the concept art.

What the art has that boxes could not carry: a long, slender blade whose edge is steel and whose
core is light, a fuller cut down it, a crossguard of silver filigree that curls up into wings
around a gem, a bound grip, a faceted pommel, and two ribbons of light trailing off the hilt.

The bone names are the ones `clips_spectral` already drives — `blade`, `blade_illuminated`,
`guard`, `guard_gem_illuminated`, `grip`, `pommel_illuminated` and the two seven-bone ribbon
chains — so the animation written for the box version moves this one unchanged.
"""
import math

import numpy as np

import mesh as M
from geo import Material, Model
from rig import Rig

ID = 'spectral_blade'
NAME = 'Spectral Blade'
TEX = 256

MATERIALS = ['core', 'glass', 'silver', 'dark', 'gem', 'ribbon', 'wrap']
EMISSIVE = {'core', 'gem', 'ribbon'}

ATLAS = M.Atlas(TEX)
ATLAS.pack(MATERIALS + ['arm'])

FINISHES = ['', 'obsidian', 'molten', 'spectral', 'dark_metal']

PALETTES = {
    '':           dict(core=(0.45, 0.95, 1.00), rune=(1.00, 1.00, 1.00), glass=(0.35, 0.65, 0.95),
                       silver=(0.82, 0.85, 0.90), dark=(0.16, 0.18, 0.24), gem=(0.35, 0.60, 1.00),
                       ribbon_a=(0.20, 0.60, 1.00), ribbon_b=(0.80, 1.00, 1.00)),
    'obsidian':   dict(core=(0.55, 0.35, 1.00), rune=(1.00, 0.85, 1.00), glass=(0.10, 0.06, 0.16),
                       silver=(0.35, 0.33, 0.42), dark=(0.05, 0.04, 0.07), gem=(0.70, 0.30, 1.00),
                       ribbon_a=(0.40, 0.10, 0.80), ribbon_b=(0.85, 0.60, 1.00)),
    'molten':     dict(core=(1.00, 0.60, 0.15), rune=(1.00, 0.95, 0.70), glass=(0.35, 0.08, 0.02),
                       silver=(0.55, 0.42, 0.32), dark=(0.12, 0.05, 0.03), gem=(1.00, 0.40, 0.10),
                       ribbon_a=(1.00, 0.30, 0.05), ribbon_b=(1.00, 0.85, 0.40)),
    'spectral':   dict(core=(0.80, 1.00, 1.00), rune=(0.60, 0.95, 1.00), glass=(0.65, 0.90, 1.00),
                       silver=(0.92, 0.96, 1.00), dark=(0.40, 0.50, 0.65), gem=(0.75, 0.95, 1.00),
                       ribbon_a=(0.60, 0.90, 1.00), ribbon_b=(1.00, 1.00, 1.00)),
    'dark_metal': dict(core=(1.00, 0.25, 0.30), rune=(1.00, 0.80, 0.80), glass=(0.14, 0.14, 0.17),
                       silver=(0.45, 0.46, 0.50), dark=(0.07, 0.07, 0.08), gem=(1.00, 0.20, 0.25),
                       ribbon_a=(0.80, 0.10, 0.15), ribbon_b=(1.00, 0.60, 0.60)),
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
    t(c, ATLAS, 'core', TEX, pm.energy, p['rune'], p['core'], 41)
    t(c, ATLAS, 'glass', TEX, pm.energy, p['core'], p['glass'], 42, glow=0.55)
    t(c, ATLAS, 'silver', TEX, pm.metal, p['silver'], 43, polish=0.75, scratches=0.25)
    t(c, ATLAS, 'dark', TEX, pm.metal, p['dark'], 44, polish=0.3)
    t(c, ATLAS, 'gem', TEX, pm.stone, p['gem'], 45, glow=0.95)
    t(c, ATLAS, 'ribbon', TEX, pm.energy, p['ribbon_b'], p['ribbon_a'], 46, glow=0.9)
    t(c, ATLAS, 'wrap', TEX, pm.sinew, tuple(v * 0.7 for v in p['dark']), 47, wet=0.25)
    paint.flat(c, *_rect('arm'), (0.55, 0.42, 0.34), seed=1)
    return c


BLADE_LENGTH = 27.0


def _blade_outline(length, width, waist=0.30):
    """Half a leaf blade, mirrored: a short ricasso, a swell, then a long draw to the point."""
    right = [(0.55, -1.2), (width * 0.72, 0.6), (width, length * waist),
             (width * 0.86, length * 0.62), (width * 0.42, length * 0.88), (0.0, length)]
    left = [(-x, y) for (x, y) in reversed(right[:-1])]
    return right + left


def build():
    m = Model(TEX, TEX, seed=13)
    ax, ay, aw, ah = _rect('arm')
    m.mats = {'arm': Material('arm', ax, ay, aw, ah, 1)}
    # Held point-up and angled across the view, the way a sword is carried at the ready.
    rig = Rig(m, grip_eye=(0.38, -0.56, -0.80), blade_dir_eye=(-0.34, 0.86, -0.38),
              face_normal_eye=(0.22, 0.16, 0.96), shoulder_eye=(0.36, -0.75, 0.32))
    G = rig.G
    groups = {}

    def part(bone, *meshes):
        for mesh in meshes:
            mesh.translate(G[0], G[1], G[2])
        groups.setdefault(bone, []).extend(meshes)

    # ---- the blade ----------------------------------------------------------------------------
    m.bone('blade_illuminated', 'weapon', rig.G)
    m.bone('blade', 'weapon', rig.G)

    steel = M.blade(_blade_outline(BLADE_LENGTH, 2.70), 0.34, 'silver', bevel=0.30, taper=0.80)
    steel.translate(0, 1.6, 0)
    blade_parts = [steel.smooth()]
    # The ricasso: a chased collar where the blade seats into the guard.
    collar = M.lathe([(1.1, 1.0), (1.75, 1.5), (1.85, 2.9), (1.25, 3.6), (1.05, 4.2)], 14, 'dark')
    collar.scale(1.0, 1.0, 0.42)
    blade_parts.append(collar.smooth())
    # Rune studs set down the fuller, alternating sides.
    for i in range(6):
        t = 0.16 + i * 0.12
        stud = M.gem(0.26, 0.36, 6, 'gem')
        stud.rotate(M.v3(1, 0, 0), 90 if i % 2 == 0 else -90)
        stud.translate(0, 2.0 + BLADE_LENGTH * t, (0.30 if i % 2 == 0 else -0.30))
        blade_parts.append(stud)
    part('blade', *blade_parts)

    # The light in it: a plate standing a little proud of the steel either side, a hot spine down
    # the middle of that, and a wider, fainter sheet of glass around the whole edge.
    glow = M.blade(_blade_outline(BLADE_LENGTH * 0.94, 1.62), 0.44, 'core', bevel=0.52, taper=1.0)
    glow.translate(0, 2.2, 0)
    spine = M.tube(M.catmull([M.v3(0, 1.4, 0), M.v3(0, BLADE_LENGTH * 0.5, 0),
                              M.v3(0, BLADE_LENGTH * 0.99, 0)], 16),
                   [0.40, 0.30, 0.02], 6, 'core', squash=1.6).smooth()
    halo = M.blade(_blade_outline(BLADE_LENGTH * 1.03, 3.35), 0.06, 'glass', bevel=0.10, taper=1.0)
    halo.translate(0, 1.5, 0)
    part('blade_illuminated', glow.smooth(), spine, halo)

    # ---- the crossguard: filigree wings around a gem -------------------------------------------
    m.bone('guard', 'weapon', rig.G)
    guard = []
    # The block the blade seats into.
    boss = M.lathe([(0.0, -1.6), (1.5, -1.3), (1.9, -0.2), (1.6, 1.0), (0.9, 1.7), (0.0, 1.9)],
                   16, 'silver', closed_top=True, closed_bottom=True).smooth()
    boss.scale(1.0, 1.0, 0.62)
    guard.append(boss)
    for side in (1, -1):
        # The arm sweeps out and curls up; the scroll on its end is the filigree curl from the art.
        arm_path = M.catmull([M.v3(side * 0.8, 0.2, 0), M.v3(side * 3.0, 0.9, 0),
                              M.v3(side * 5.2, 2.3, 0), M.v3(side * 6.0, 4.4, 0)], 18)
        guard.append(M.tube(arm_path, [0.85, 0.62, 0.42, 0.22], 7, 'silver', squash=0.62).smooth())
        curl = M.scroll(1.35, 1.9, 0.14, 0.34, 'silver', 28, plane=('x', 'y'))
        curl.translate(side * 6.4, 5.4, 0)
        guard.append(curl)
        inner_curl = M.scroll(1.1, 1.1, 0.10, 0.22, 'dark', 20, plane=('x', 'y'))
        inner_curl.translate(side * 3.4, 2.6, 0.32)
        guard.append(inner_curl)
        # A short fan of metal feathers along the top of the arm: the wing in the art.
        for j in range(3):
            t = 0.3 + j * 0.24
            vane = M.feather(2.9 - 0.4 * j, 1.15, 'silver', curve=0.2, cup=0.3,
                             rib='dark', rib_radius=0.05)
            vane.rotate(M.v3(0, 0, 1), -side * (24 + j * 13))
            vane.translate(side * (1.6 + t * 4.2), 1.1 + t * 2.0, -0.26)
            guard.append(vane)
        # A wing plate filling the arm, pierced by the curl.
        wing = M.blade([(side * 0.9, 0.3), (side * 3.4, 1.0), (side * 5.4, 2.6),
                        (side * 4.6, 2.9), (side * 2.8, 2.0), (side * 1.0, 1.5)], 0.12, 'dark',
                       bevel=0.4)
        guard.append(wing.smooth())
        # Three feathered barbs hanging under the arm.
        for j in range(3):
            t = 0.25 + j * 0.28
            barb = M.tube(M.catmull([M.v3(side * (1.4 + t * 4.0), 0.4 - t * 0.2, 0),
                                     M.v3(side * (1.9 + t * 4.2), -1.4 - t * 0.9, 0.1),
                                     M.v3(side * (1.7 + t * 4.4), -2.6 - t * 1.4, 0.2)], 12),
                          [0.34, 0.20, 0.02], 5, 'silver').smooth()
            guard.append(barb)
    part('guard', *guard)

    m.bone('guard_gem_illuminated', 'guard', rig.G)
    stone = M.gem(1.15, 1.9, 10, 'gem')
    stone.rotate(M.v3(1, 0, 0), 90).translate(0, 0.5, 0.95)
    back = M.gem(1.15, 1.9, 10, 'gem')
    back.rotate(M.v3(1, 0, 0), -90).translate(0, 0.5, -0.95)
    part('guard_gem_illuminated', stone, back)

    # ---- the grip and pommel --------------------------------------------------------------------
    m.bone('grip', 'weapon', rig.G)
    grip = [M.lathe([(0.0, -1.8), (0.95, -2.2), (1.10, -5.0), (0.95, -7.4), (0.0, -7.9)],
                    14, 'wrap', closed_top=True, closed_bottom=True).smooth()]
    for i in range(7):
        y = -2.5 - i * 0.75
        ring = M.torus(1.08 + 0.04 * math.sin(i), 0.13, 12, 5, 'silver')
        ring.rotate(M.v3(1, 0, 0), 90)
        ring.translate(0, y, 0)
        grip.append(ring.smooth())
    cap = M.lathe([(0.0, -7.6), (1.25, -8.0), (1.05, -8.9), (0.0, -9.3)], 14, 'silver',
                  closed_top=True, closed_bottom=True).smooth()
    grip.append(cap)
    part('grip', *grip)

    m.bone('pommel_illuminated', 'grip', rig.G)
    jewel = M.gem(0.95, 1.6, 8, 'gem')
    jewel.rotate(M.v3(1, 0, 0), 180).translate(0, -9.4, 0)
    part('pommel_illuminated', jewel)

    # ---- two ribbons of light --------------------------------------------------------------------
    for r, (side, y0, turn) in enumerate(((1, 3.0, 0.42), (-1, 9.0, -0.38))):
        parent = 'weapon'
        a = np.array([side * 1.4, y0])
        ang = math.pi / 2 + side * 0.9
        depth = side * 0.9
        for k in range(7):
            ang -= turn
            b = a + np.array([math.cos(ang), math.sin(ang)]) * 2.1
            bone = m.bone('ribbon_%d_%d_illuminated' % (r, k), parent, rig.at((a[0], a[1], 0)))
            z0 = depth - 0.25 * abs(k - 3) * side
            z1 = depth - 0.25 * abs(k + 1 - 3) * side
            strip = M.ribbon(M.catmull([M.v3(a[0], a[1], z0),
                                        M.v3((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (z0 + z1) / 2 + 0.25),
                                        M.v3(b[0], b[1], z1)], 7),
                             1.9 * (1 - k * 0.095), 'ribbon', thickness=0.05,
                             twist=0.22, taper=0.10)
            part(bone, strip)
            parent = bone
            a = b

    for meshes in groups.values():
        for mesh in meshes:
            ATLAS.apply(mesh)
    return m, rig, groups


PROFILE = {
    # Where the blade sits in the hand out of the box; the sliders still override it.
    'placement': [36, 66, 22, -30, 0, 0, 0, 0],
    'profile': 'spectral_blade',
    'trail': [0.35, 0.9, 1.0],
    'trail2': [0.85, 0.95, 1.0],
    'ribbons': 2,
    'summon': True,
    'summon_style': 'forge',
    'emissive': [0.45, 0.95, 1.0],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}

if __name__ == '__main__':
    model, rig_, groups_ = build()
    total = sum(len(x) for v in groups_.values() for x in v)
    print('bones', len(model.bones), 'groups', len(groups_), 'triangles', total)
