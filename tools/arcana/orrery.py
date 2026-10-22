"""The Hour Unwound — an armillary scepter.

A slim rod of pale metal carrying three nested rings on its head, each tilted differently and each
turning at its own rate about its own axis, with a star burning in the middle of them. A crescent
blade stands off the back of the head so the thing can actually cut, six rune plates ride in orbit
around the whole assembly, and the haft is bound in silk and ends in a clawed foot holding a second,
smaller stone.

Every moving part is its own bone - `ring_0..2`, `core_illuminated`, `rune_0..5` - so the clips can
keep them turning through every other animation without touching the weapon's own pose.
"""
import math

import mesh as M
from geo import Material, Model
from rig import Rig

ID = 'orrery_scepter'
NAME = 'The Hour Unwound'
TEX = 256
RUNES = 6

MATERIALS = ['pale', 'dark', 'gold', 'star', 'glass', 'silk', 'rune']
EMISSIVE = {'star', 'glass', 'rune'}

ATLAS = M.Atlas(TEX)
ATLAS.pack(MATERIALS + ['arm'])

FINISHES = ['', 'obsidian', 'molten', 'spectral', 'dark_metal']

PALETTES = {
    '':           dict(pale=(0.86, 0.88, 0.94), dark=(0.18, 0.20, 0.30), gold=(0.95, 0.76, 0.32),
                       star=(1.00, 0.92, 0.60), glass=(0.45, 0.75, 1.00), silk=(0.32, 0.22, 0.52),
                       rune=(0.70, 0.90, 1.00)),
    'obsidian':   dict(pale=(0.28, 0.27, 0.34), dark=(0.07, 0.06, 0.10), gold=(0.55, 0.40, 0.70),
                       star=(0.80, 0.45, 1.00), glass=(0.45, 0.20, 0.80), silk=(0.14, 0.06, 0.24),
                       rune=(0.80, 0.50, 1.00)),
    'molten':     dict(pale=(0.60, 0.44, 0.30), dark=(0.14, 0.06, 0.03), gold=(1.00, 0.70, 0.20),
                       star=(1.00, 0.55, 0.12), glass=(1.00, 0.40, 0.08), silk=(0.38, 0.12, 0.03),
                       rune=(1.00, 0.72, 0.30)),
    'spectral':   dict(pale=(0.90, 0.96, 1.00), dark=(0.35, 0.46, 0.60), gold=(0.70, 0.92, 1.00),
                       star=(0.85, 1.00, 1.00), glass=(0.70, 0.95, 1.00), silk=(0.30, 0.48, 0.62),
                       rune=(0.85, 1.00, 1.00)),
    'dark_metal': dict(pale=(0.40, 0.41, 0.45), dark=(0.09, 0.09, 0.11), gold=(0.70, 0.65, 0.45),
                       star=(1.00, 0.30, 0.32), glass=(0.85, 0.25, 0.28), silk=(0.22, 0.07, 0.09),
                       rune=(1.00, 0.45, 0.48)),
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
    t(c, ATLAS, 'pale', TEX, pm.metal, p['pale'], 61, polish=0.8, scratches=0.2)
    t(c, ATLAS, 'dark', TEX, pm.metal, p['dark'], 62, polish=0.25)
    t(c, ATLAS, 'gold', TEX, pm.metal, p['gold'], 63, polish=0.9, scratches=0.15, glow=0.10)
    t(c, ATLAS, 'star', TEX, pm.stone, p['star'], 64, glow=1.0)
    t(c, ATLAS, 'glass', TEX, pm.energy, (1.0, 1.0, 1.0), p['glass'], 65, glow=0.8)
    t(c, ATLAS, 'silk', TEX, pm.sinew, p['silk'], 66, wet=0.35)
    t(c, ATLAS, 'rune', TEX, pm.runes, p['dark'], p['rune'], 67, glow=0.9)
    paint.flat(c, *_rect('arm'), (0.55, 0.42, 0.34), seed=1)
    return c


def build():
    m = Model(TEX, TEX, seed=19)
    ax, ay, aw, ah = _rect('arm')
    m.mats = {'arm': Material('arm', ax, ay, aw, ah, 1)}
    rig = Rig(m, grip_eye=(0.38, -0.56, -0.80), blade_dir_eye=(-0.30, 0.88, -0.36),
              face_normal_eye=(0.20, 0.16, 0.97), shoulder_eye=(0.36, -0.75, 0.32))
    G = rig.G
    groups = {}

    def part(bone, *meshes):
        for mesh in meshes:
            mesh.translate(G[0], G[1], G[2])
        groups.setdefault(bone, []).extend(meshes)

    HEAD = 15.0          # how far up the shaft the rings sit

    # ---- the haft -----------------------------------------------------------------------------
    m.bone('handle', 'weapon', G)
    haft = [M.tube(M.catmull([M.v3(0, -10.5, 0), M.v3(0, 1.0, 0), M.v3(0, HEAD - 3.0, 0)], 26),
                   [0.62, 0.78, 0.70, 0.52], 9, 'pale').smooth()]
    # Silk binding where the hand closes, and a gold collar either side of it.
    haft.append(M.tube(M.catmull([M.v3(0, -4.2, 0), M.v3(0, -1.0, 0), M.v3(0, 2.2, 0)], 14),
                       [0.84, 0.92, 0.84], 9, 'silk').smooth())
    for y in (-4.4, 2.4, HEAD - 3.4):
        collar = M.lathe([(0.70, 0), (0.95, 0.22), (0.95, 0.78), (0.70, 1.0)], 14, 'gold')
        collar.translate(0, y, 0)
        haft.append(collar.smooth())
    # The foot: four claws closed on a small stone.
    for k in range(4):
        a = k / 4 * math.tau + math.pi / 4
        root = M.v3(math.cos(a) * 0.8, -9.6, math.sin(a) * 0.8)
        mid = M.v3(math.cos(a) * 1.7, -11.2, math.sin(a) * 1.7)
        tip = M.v3(math.cos(a) * 0.9, -12.9, math.sin(a) * 0.9)
        haft.append(M.tube(M.catmull([root, mid, tip], 14), [0.34, 0.26, 0.04], 6, 'gold').smooth())
    pearl = M.sphere(0.95, 12, 9, 'glass').smooth()
    pearl.translate(0, -11.3, 0)
    haft.append(pearl)
    part('handle', *haft)

    # ---- the head: the crescent and the frame the rings hang in -------------------------------
    m.bone('head', 'weapon', (G[0], G[1] + HEAD, G[2]))
    head = []
    # The crescent: a disc with a second, smaller disc taken out of it, offset, which is what
    # gives a crescent its horns. An annulus sector - the first try - reads as a paper fan.
    R_OUT, R_IN, OFFSET = 7.2, 6.1, 2.6
    outer, inner = [], []
    for d in range(-104, 105, 7):
        a = math.radians(d)
        outer.append((math.cos(a) * R_OUT, math.sin(a) * R_OUT + HEAD))
    for d in range(104, -105, -7):
        a = math.radians(d)
        inner.append((math.cos(a) * R_IN, math.sin(a) * R_IN + OFFSET + HEAD))
    crescent = M.blade(outer + inner, 0.30, 'pale', bevel=0.26)
    head.append(crescent.smooth())
    # A gold edge chased along the outside of it.
    edge = M.tube(M.catmull([M.v3(x, y, 0.0) for (x, y) in outer], 30),
                  [0.10, 0.17, 0.10], 5, 'gold').smooth()
    head.append(edge)
    for horn, pt in ((0, outer[0]), (1, outer[-1])):
        tip = M.tube(M.catmull([M.v3(pt[0], pt[1], 0),
                                M.v3(pt[0] * 1.14, pt[1] + (1 if horn else -1) * 0.1, 0),
                                M.v3(pt[0] * 1.24, pt[1] + (2 if horn else -2) * 0.1, 0)], 10),
                     [0.30, 0.16, 0.02], 5, 'gold').smooth()
        head.append(tip)
    # The yoke the rings are pinned to.
    for side in (1, -1):
        post = M.tube(M.catmull([M.v3(side * 0.4, HEAD - 3.0, 0), M.v3(side * 1.2, HEAD - 1.2, 0),
                                 M.v3(side * 1.1, HEAD + 0.4, 0)], 10), [0.40, 0.34, 0.26], 6, 'gold').smooth()
        head.append(post)
    for mesh in head:
        mesh.translate(0, -HEAD, 0)
        mesh.translate(0, HEAD, 0)
    part('head', *head)

    # ---- three rings, each on its own bone so each can turn on its own ------------------------
    tilts = ((0, 0, 0), (62, 18, 0), (-48, -26, 14))
    radii = (5.6, 4.5, 3.4)
    for i in range(3):
        bone = 'ring_%d' % i
        m.bone(bone, 'head', (G[0], G[1] + HEAD, G[2]))
        ring = M.torus(radii[i], 0.22 + 0.04 * i, 30, 7, 'gold' if i == 1 else 'pale')
        ring.rotate(M.v3(1, 0, 0), tilts[i][0])
        ring.rotate(M.v3(0, 1, 0), tilts[i][1])
        ring.rotate(M.v3(0, 0, 1), tilts[i][2])
        pieces = [ring.smooth()]
        # Graduations: little studs round the band, so a turning ring reads as turning.
        for k in range(10):
            a = k / 10 * math.tau
            stud = M.ellipsoid(0.17, 0.34, 0.17, 7, 5, 'rune').smooth()
            stud.rotate(M.v3(1, 0, 0), 90)
            stud.translate(math.cos(a) * radii[i], 0, math.sin(a) * radii[i])
            stud.rotate(M.v3(1, 0, 0), tilts[i][0])
            stud.rotate(M.v3(0, 1, 0), tilts[i][1])
            stud.rotate(M.v3(0, 0, 1), tilts[i][2])
            pieces.append(stud)
        for piece in pieces:
            piece.translate(0, HEAD, 0)
            piece.translate(0, 0, 0.9)        # the rings ride in front of the crescent
        part(bone, *pieces)

    # ---- the star in the middle ----------------------------------------------------------------
    m.bone('core_illuminated', 'head', (G[0], G[1] + HEAD, G[2]))
    core = [M.sphere(1.5, 14, 10, 'star').smooth()]
    for k in range(8):
        a = k / 8 * math.tau
        tilt = 28 if k % 2 else -28
        spike = M.tube(M.catmull([M.v3(0, 0, 0), M.v3(math.cos(a) * 1.7, math.sin(tilt / 57.3) * 0.9, math.sin(a) * 1.7),
                                  M.v3(math.cos(a) * 3.4, math.sin(tilt / 57.3) * 1.7, math.sin(a) * 3.4)], 10),
                       [0.38, 0.22, 0.02], 5, 'star').smooth()
        core.append(spike)
    for mesh in core:
        mesh.translate(0, HEAD, 0)
        mesh.translate(0, 0, 0.9)
    part('core_illuminated', *core)

    # ---- rune plates in orbit -------------------------------------------------------------------
    for k in range(RUNES):
        bone = 'rune_%d' % k
        m.bone(bone, 'head', (G[0], G[1] + HEAD, G[2]))
        a = k / RUNES * math.tau
        r = 7.6
        plate = M.blade([(-1.15, -1.5), (1.15, -1.5), (1.45, 0), (1.15, 1.5), (-1.15, 1.5), (-1.45, 0)],
                        0.10, 'rune', bevel=0.55)
        plate.rotate(M.v3(0, 1, 0), -math.degrees(a))
        plate.translate(math.cos(a) * r, HEAD + math.sin(a * 2) * 1.6, math.sin(a) * r * 0.35 + 1.1)
        part(bone, plate.smooth())

    for meshes in groups.values():
        for mesh in meshes:
            ATLAS.apply(mesh)
    return m, rig, groups


PROFILE = {
    'placement': [46, 40, 22, -26, 0, 0, 0, 0],
    'profile': 'orrery_scepter',
    'rings': 3,
    'runes': RUNES,
    'summon': True,
    'summon_style': 'bloom',
    'trail': [0.98, 0.84, 0.40],
    'trail2': [0.40, 0.72, 1.00],
    'emissive': [1.0, 0.90, 0.55],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}

if __name__ == '__main__':
    model, rig_, groups_ = build()
    print('bones', len(model.bones), 'groups', len(groups_),
          'triangles', sum(len(x) for v in groups_.values() for x in v))
