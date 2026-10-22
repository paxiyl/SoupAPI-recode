"""The Nine-Veil Lotus — a glaive whose head is a flower.

Nine blades of cut glass stand round a burning heart, in three ranks: the outer rank lies open, the
middle stands half up, the inner is almost closed over the heart. Each blade is its own bone, so the
flower opens on a cut and closes again after it. Below them a collar of gold leaves, and a long haft
bound in silk with a tassel at its foot.
"""
import math

import mesh as M
from geo import Material, Model
from rig import Rig, shrink

ID = 'lotus_glaive'
NAME = 'Nine-Veil Lotus'
TEX = 256
PETALS = 9

MATERIALS = ['glass', 'glass_deep', 'gold', 'dark', 'heart', 'silk', 'leaf']
EMISSIVE = {'heart', 'glass'}

ATLAS = M.Atlas(TEX)
ATLAS.pack(MATERIALS + ['arm'])

FINISHES = ['', 'obsidian', 'molten', 'spectral', 'dark_metal']

PALETTES = {
    '':           dict(glass=(0.70, 0.90, 1.00), deep=(0.20, 0.45, 0.85), gold=(0.95, 0.78, 0.35),
                       dark=(0.14, 0.14, 0.22), heart=(1.00, 0.55, 0.85), silk=(0.55, 0.14, 0.42),
                       leaf=(0.35, 0.80, 0.60)),
    'obsidian':   dict(glass=(0.55, 0.35, 0.90), deep=(0.16, 0.05, 0.30), gold=(0.55, 0.40, 0.70),
                       dark=(0.06, 0.05, 0.09), heart=(0.85, 0.30, 1.00), silk=(0.26, 0.05, 0.34),
                       leaf=(0.35, 0.20, 0.55)),
    'molten':     dict(glass=(1.00, 0.70, 0.35), deep=(0.55, 0.16, 0.03), gold=(1.00, 0.72, 0.22),
                       dark=(0.12, 0.05, 0.03), heart=(1.00, 0.42, 0.10), silk=(0.44, 0.10, 0.03),
                       leaf=(0.75, 0.42, 0.12)),
    'spectral':   dict(glass=(0.88, 0.98, 1.00), deep=(0.45, 0.70, 0.90), gold=(0.75, 0.92, 1.00),
                       dark=(0.32, 0.42, 0.55), heart=(0.85, 1.00, 1.00), silk=(0.38, 0.55, 0.68),
                       leaf=(0.60, 0.88, 0.92)),
    'dark_metal': dict(glass=(0.80, 0.30, 0.34), deep=(0.32, 0.05, 0.07), gold=(0.70, 0.62, 0.42),
                       dark=(0.08, 0.08, 0.10), heart=(1.00, 0.22, 0.26), silk=(0.30, 0.05, 0.07),
                       leaf=(0.55, 0.26, 0.26)),
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
    t(c, ATLAS, 'glass', TEX, pm.energy, (1.0, 1.0, 1.0), p['glass'], 71, glow=0.55)
    t(c, ATLAS, 'glass_deep', TEX, pm.stone, p['deep'], 72, glow=0.35)
    t(c, ATLAS, 'gold', TEX, pm.metal, p['gold'], 73, polish=0.9, scratches=0.2, glow=0.08)
    t(c, ATLAS, 'dark', TEX, pm.metal, p['dark'], 74, polish=0.25)
    t(c, ATLAS, 'heart', TEX, pm.iris, (1.0, 1.0, 1.0), p['heart'], 75)
    t(c, ATLAS, 'silk', TEX, pm.sinew, p['silk'], 76, wet=0.4)
    t(c, ATLAS, 'leaf', TEX, pm.petal_tile, p['leaf'], tuple(v * 0.4 for v in p['leaf']), 77)
    paint.flat(c, *_rect('arm'), (0.55, 0.42, 0.34), seed=1)
    return c


HEAD = 13.5


def build():
    m = Model(TEX, TEX, seed=23)
    ax, ay, aw, ah = _rect('arm')
    m.mats = {'arm': Material('arm', ax, ay, aw, ah, 1)}
    rig = Rig(m, grip_eye=(0.24, -0.44, -0.56), blade_dir_eye=(-0.48, 0.74, -0.47),
              face_normal_eye=(0.22, 0.26, 0.94), shoulder_eye=(0.50, -1.10, -0.10),
              left_shoulder_eye=(-0.44, -1.10, -0.10), left_grip_local=(0, -3.9, 0))
    G = rig.G
    groups = {}

    def part(bone, *meshes):
        for mesh in meshes:
            mesh.translate(G[0], G[1], G[2])
        groups.setdefault(bone, []).extend(meshes)

    # ---- the haft ------------------------------------------------------------------------------
    m.bone('handle', 'weapon', G)
    haft = [M.tube(M.catmull([M.v3(0, -11.5, 0), M.v3(0, 0.5, 0), M.v3(0, HEAD - 2.5, 0)], 24),
                   [0.55, 0.72, 0.66, 0.48], 8, 'dark').smooth()]
    haft.append(M.tube(M.catmull([M.v3(0, -5.0, 0), M.v3(0, -1.2, 0), M.v3(0, 2.6, 0)], 14),
                       [0.80, 0.88, 0.80], 9, 'silk').smooth())
    for y in (-5.2, 2.8, HEAD - 2.8):
        band = M.lathe([(0.66, 0), (0.90, 0.20), (0.90, 0.70), (0.66, 0.90)], 13, 'gold')
        band.translate(0, y, 0)
        haft.append(band.smooth())
    # The tassel: three silk cords off the binding.
    for k in range(3):
        a = k / 3 * math.tau
        cord = M.ribbon(M.catmull([M.v3(math.cos(a) * 0.8, -5.4, math.sin(a) * 0.8),
                                   M.v3(math.cos(a) * 1.6, -7.6, math.sin(a) * 1.2),
                                   M.v3(math.cos(a) * 1.1, -10.2, math.sin(a) * 1.8)], 14),
                        0.70, 'silk', thickness=0.04, twist=0.6, taper=0.55)
        haft.append(cord)
    foot = M.lathe([(0.0, -13.4), (0.70, -12.6), (0.52, -11.6), (0.0, -11.2)], 12, 'gold',
                   closed_top=True, closed_bottom=True).smooth()
    haft.append(foot)
    part('handle', *haft)

    # ---- the collar the flower sits in ---------------------------------------------------------
    m.bone('head', 'weapon', (G[0], G[1] + HEAD, G[2]))
    collar = [M.lathe([(0.0, -2.2), (1.3, -1.8), (1.9, -0.4), (1.5, 0.9), (0.0, 1.3)], 16, 'gold',
                      closed_top=True, closed_bottom=True).smooth()]
    for k in range(6):
        a = k / 6 * math.tau
        leaf = M.petal(3.4, 2.1, 0.55, 7, 'leaf', 0.05, cols=5)
        leaf.rotate(M.v3(1, 0, 0), 118)
        leaf.rotate(M.v3(0, 1, 0), math.degrees(a))
        leaf.translate(0, -0.6, 0)
        collar.append(leaf)
    for mesh in collar:
        mesh.translate(0, HEAD, 0)
    part('head', *collar)

    # ---- nine blades, in three ranks ------------------------------------------------------------
    # Each rank stands further up than the one outside it; a rank is three blades, and each blade is
    # its own bone so the clips can open and shut the flower.
    ranks = ((0, 4, 11.5, 5.2, 80, 'glass_deep'),
             (1, 3, 9.2, 4.2, 52, 'glass'),
             (2, 2, 7.2, 3.2, 22, 'glass'))
    index = 0
    for rank, count, length, width, lean, material in ranks:
        for k in range(count):
            a = (k / count) * math.tau + rank * 0.72
            bone = 'petal_%d' % index
            m.bone(bone, 'head', (G[0], G[1] + HEAD, G[2]))
            # A long leaf blade, cut to a point, faceted so it reads as glass.
            outline = [(0.0, 0.0), (width * 0.55, length * 0.22), (width * 0.5, length * 0.58),
                       (0.0, length), (-width * 0.5, length * 0.58), (-width * 0.55, length * 0.22)]
            leaf = M.blade(outline, width * 0.16, material, bevel=0.30, taper=0.8)
            spine = M.tube(M.catmull([M.v3(0, 0.2, 0), M.v3(0, length * 0.5, 0.05),
                                      M.v3(0, length * 0.96, 0)], 10),
                           [0.15, 0.10, 0.01], 5, 'gold').smooth()
            leaf.merge(spine)
            leaf.rotate(M.v3(1, 0, 0), lean)
            leaf.rotate(M.v3(0, 1, 0), math.degrees(a))
            leaf.translate(0, HEAD + 0.4, 0)
            part(bone, leaf.smooth())
            index += 1

    # ---- the heart --------------------------------------------------------------------------------
    m.bone('heart_illuminated', 'head', (G[0], G[1] + HEAD, G[2]))
    heart = [M.sphere(1.55, 14, 10, 'heart').smooth()]
    halo = M.torus(2.8, 0.11, 24, 6, 'heart')
    halo.rotate(M.v3(1, 0, 0), 14)
    heart.append(halo.smooth())
    for k in range(5):
        a = k / 5 * math.tau
        seed = M.gem(0.36, 0.58, 6, 'glass')
        seed.translate(math.cos(a) * 1.95, 0.6, math.sin(a) * 1.95)
        heart.append(seed)
    for mesh in heart:
        mesh.translate(0, HEAD + 1.1, 0)
    part('heart_illuminated', *heart)

    # Built at the size of the art, held at the size of a hand: see rig.shrink.
    shrink(m, groups, rig, 0.42)
    for meshes in groups.values():
        for mesh in meshes:
            ATLAS.apply(mesh)
    return m, rig, groups


PROFILE = {
    'profile': 'lotus_glaive',
    'petals': PETALS,
    'summon': True,
    'summon_style': 'bloom',
    'trail': [1.00, 0.55, 0.85],
    'trail2': [0.45, 0.85, 1.00],
    'emissive': [1.0, 0.6, 0.9],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}

if __name__ == '__main__':
    model, rig_, groups_ = build()
    print('bones', len(model.bones), 'groups', len(groups_),
          'triangles', sum(len(x) for v in groups_.values() for x in v))
