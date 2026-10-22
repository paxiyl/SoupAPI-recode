"""The Raven Claw — a crescent built as triangles, from the concept art.

What the art has that boxes could not carry: a crescent whose frame is a drawn-out blade rather
than a stack of bars, a row of long black feathers fanning off its back, roses opening at the
junction, a crow's skull set into them, chains hanging off the guard, and a grip bound in leather
that ends in a forked hook.

The bone names are the ones `clips_raven` already drives — `spine_0..11` chained so the whole
crescent curls shut, `rib_k` and `feather_k` riding each segment, `crown`, `skull_illuminated`,
`prong`, `grip`, `hook` — so the animation written for the box version moves this one unchanged.
"""
import math

import numpy as np

import mesh as M
from geo import Material, Model
from rig import Rig

ID = 'raven_claw'
NAME = 'Raven Claw'
TEX = 256
SEGMENTS = 12

MATERIALS = ['frame', 'edge', 'inner', 'bone', 'bone_dark', 'feather', 'feather_dark',
             'petal', 'petal_dark', 'gem', 'leather', 'wrap']
EMISSIVE = {'edge', 'gem'}

ATLAS = M.Atlas(TEX)
ATLAS.pack(MATERIALS + ['arm'])

FINISHES = ['', 'obsidian', 'molten', 'spectral', 'dark_metal']

PALETTES = {
    '':           dict(frame=(0.78, 0.79, 0.84), edge=(0.92, 0.96, 1.00), inner=(0.24, 0.24, 0.29),
                       bone=(0.82, 0.79, 0.72), feather_a=(0.03, 0.14, 0.19), feather_b=(0.40, 0.95, 0.95),
                       petal_a=(0.95, 0.45, 1.00), petal_b=(0.30, 0.04, 0.48),
                       gem=(0.85, 0.35, 1.00), leather=(0.19, 0.16, 0.16)),
    'obsidian':   dict(frame=(0.19, 0.18, 0.23), edge=(0.58, 0.50, 0.70), inner=(0.06, 0.05, 0.08),
                       bone=(0.27, 0.24, 0.30), feather_a=(0.02, 0.02, 0.05), feather_b=(0.55, 0.35, 1.00),
                       petal_a=(0.72, 0.30, 1.00), petal_b=(0.13, 0.02, 0.24),
                       gem=(0.72, 0.28, 1.00), leather=(0.07, 0.06, 0.08)),
    'molten':     dict(frame=(0.36, 0.26, 0.20), edge=(1.00, 0.76, 0.34), inner=(0.12, 0.06, 0.04),
                       bone=(0.46, 0.31, 0.22), feather_a=(0.14, 0.02, 0.00), feather_b=(1.00, 0.55, 0.10),
                       petal_a=(1.00, 0.48, 0.14), petal_b=(0.38, 0.05, 0.00),
                       gem=(1.00, 0.45, 0.10), leather=(0.12, 0.06, 0.04)),
    'spectral':   dict(frame=(0.74, 0.87, 0.95), edge=(0.86, 1.00, 1.00), inner=(0.28, 0.43, 0.58),
                       bone=(0.80, 0.92, 1.00), feather_a=(0.09, 0.24, 0.44), feather_b=(0.76, 0.98, 1.00),
                       petal_a=(0.70, 0.85, 1.00), petal_b=(0.18, 0.28, 0.62),
                       gem=(0.60, 0.90, 1.00), leather=(0.17, 0.21, 0.29)),
    'dark_metal': dict(frame=(0.31, 0.32, 0.35), edge=(0.72, 0.74, 0.80), inner=(0.10, 0.10, 0.12),
                       bone=(0.40, 0.40, 0.42), feather_a=(0.05, 0.05, 0.06), feather_b=(0.95, 0.20, 0.25),
                       petal_a=(0.90, 0.20, 0.30), petal_b=(0.24, 0.02, 0.05),
                       gem=(1.00, 0.20, 0.25), leather=(0.08, 0.08, 0.09)),
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
    t(c, ATLAS, 'frame', TEX, pm.metal, p['frame'], 21)
    t(c, ATLAS, 'edge', TEX, pm.metal, p['edge'], 22, polish=0.85, scratches=0.2, glow=0.22)
    t(c, ATLAS, 'inner', TEX, pm.metal, p['inner'], 23, polish=0.25)
    t(c, ATLAS, 'bone', TEX, pm.metal, p['bone'], 24, polish=0.18, scratches=0.8)
    t(c, ATLAS, 'bone_dark', TEX, pm.metal, tuple(v * 0.45 for v in p['bone']), 25, polish=0.1)
    t(c, ATLAS, 'feather', TEX, pm.feather, p['feather_a'], p['feather_b'], 26)
    t(c, ATLAS, 'feather_dark', TEX, pm.feather, tuple(v * 0.5 for v in p['feather_a']),
      tuple(v * 0.6 for v in p['feather_b']), 27)
    t(c, ATLAS, 'petal', TEX, pm.petal_tile, p['petal_a'], p['petal_b'], 28)
    t(c, ATLAS, 'petal_dark', TEX, pm.petal_tile, p['petal_b'], tuple(v * 0.4 for v in p['petal_a']), 29)
    t(c, ATLAS, 'gem', TEX, pm.stone, p['gem'], 30, glow=0.85)
    t(c, ATLAS, 'leather', TEX, pm.sinew, p['leather'], 31, wet=0.18)
    t(c, ATLAS, 'wrap', TEX, pm.sinew, tuple(v * 1.5 for v in p['leather']), 32, wet=0.3)
    paint.flat(c, *_rect('arm'), (0.55, 0.42, 0.34), seed=1)
    return c


# The crescent, in weapon-local units: centre, radius, where it starts and how far it sweeps.
C = np.array([-1.2, 10.5])
R = 8.5
A0 = math.radians(-80)
SWEEP = math.radians(215)


def build():
    m = Model(TEX, TEX, seed=11)
    ax, ay, aw, ah = _rect('arm')
    m.mats = {'arm': Material('arm', ax, ay, aw, ah, 1)}
    # Carried claw-up and out to the right, the crescent's opening turned towards the target.
    rig = Rig(m, grip_eye=(0.36, -0.54, -0.78), blade_dir_eye=(-0.24, 0.90, -0.36),
              face_normal_eye=(0.18, 0.14, 0.97), shoulder_eye=(0.36, -0.75, 0.32))
    G = rig.G
    groups = {}

    def part(bone, *meshes):
        for mesh in meshes:
            mesh.translate(G[0], G[1], G[2])
        groups.setdefault(bone, []).extend(meshes)

    def p3(p, z=0.0):
        return M.v3(p[0], p[1], z)

    angles = [A0 + SWEEP * i / SEGMENTS for i in range(SEGMENTS + 1)]
    pts = [C + R * np.array([math.cos(a), math.sin(a)]) for a in angles]

    # ---- the crescent -------------------------------------------------------------------------
    parent = 'weapon'
    for k in range(SEGMENTS):
        P, Q = pts[k], pts[k + 1]
        seg = m.bone('spine_%d' % k, parent, rig.at((P[0], P[1], 0)))
        parent = seg
        inward = C - (P + Q) / 2
        inward /= np.linalg.norm(inward)
        outward = -inward
        tangent = (Q - P) / np.linalg.norm(Q - P)
        taper = 1.0 if k < SEGMENTS - 3 else max(0.20, 1 - (k - (SEGMENTS - 4)) / 3.4)
        meshes = []

        # The frame: a thick spine down the back of the crescent, thinning towards the point.
        spine_path = M.catmull([p3(P - tangent * 0.4), p3((P + Q) / 2), p3(Q + tangent * 0.4)], 8)
        meshes.append(M.tube(spine_path, [1.15 * taper, 1.05 * taper, 0.95 * taper], 8, 'frame',
                             squash=0.52).smooth())

        # The cutting edge: a bevelled plate hung off the inside, which is what actually reads as
        # a blade. Drawn as a quad between this segment's two stations, pulled inward.
        reach = 2.3 * taper
        quad = [P + inward * 0.2, Q + inward * 0.2, Q + inward * reach, P + inward * reach * 1.05]
        plate = M.blade([(q[0], q[1]) for q in quad], 0.26 * taper, 'edge', bevel=0.30)
        meshes.append(plate.smooth())

        # A second, darker frame line inside the opening, and the struts that tie it to the spine.
        if 1 <= k <= 8:
            f = inward * 3.3
            strut = M.tube(M.catmull([p3(P + f), p3((P + Q) / 2 + f * 1.04), p3(Q + f)], 7),
                           0.30, 6, 'inner').smooth()
            meshes.append(strut)
            tie = M.tube(M.catmull([p3(P + inward * reach * 0.9), p3(P + f * 0.98)], 5),
                         [0.26, 0.18], 5, 'inner').smooth()
            meshes.append(tie)
        part(seg, *meshes)

        if k <= 8:
            # The rib: a bone spur curving off the back of the segment.
            rib_bone = m.bone('rib_%d' % k, seg, rig.at((P[0], P[1], 0)))
            length = 3.6 - k * 0.19
            path = []
            here = P.copy()
            heading = outward.copy()
            for j in range(4):
                path.append(p3(here, 0.0))
                heading = heading + tangent * 0.26
                heading /= np.linalg.norm(heading)
                here = here + heading * length / 3
            rib = M.tube(M.catmull(path, 14), [0.62, 0.40, 0.22, 0.05], 6, 'bone').smooth()
            knuckle = M.sphere(0.46, 9, 7, 'bone_dark').smooth()
            knuckle.translate(p3(P + outward * 0.25))
            part(rib_bone, rib, knuckle)

            # The feathers: three long vanes off the back, each turned a little further and set a
            # little deeper, so the row reads as a wing rather than a comb.
            fb = m.bone('feather_%d' % k, seg, rig.at((P[0], P[1], 0)))
            plumes = []
            for j in range(2):
                t = (j + 0.5) / 2
                base = P + (Q - P) * t + outward * 0.35
                lean = math.degrees(math.atan2(outward[1], outward[0])) - 90 - 16 - j * 9
                vane = M.feather(5.6 + 1.2 * math.sin(j + k), 2.05 - 0.1 * j,
                                 'feather' if j != 1 else 'feather_dark',
                                 curve=0.26, cup=0.26, rib='bone_dark', rib_radius=0.065)
                vane.rotate(M.v3(0, 0, 1), lean)
                vane.translate(p3(base, -0.42 + j * 0.62))
                plumes.append(vane)
            part(fb, *plumes)

    # The point: one last spike carrying on past the final segment.
    P, Q = pts[-2], pts[-1]
    d = (Q - P) / np.linalg.norm(Q - P)
    tip_bone = m.bone('spine_tip', parent, rig.at((Q[0], Q[1], 0)))
    tip_end = Q + d * 3.1 + (C - Q) / R * 0.7
    spike = M.tube(M.catmull([p3(Q - d * 0.3), p3((Q + tip_end) / 2), p3(tip_end)], 12),
                   [0.42, 0.26, 0.02], 6, 'edge', squash=0.6).smooth()
    part(tip_bone, spike)

    # ---- the junction: roses, the skull, the prong, the chains -------------------------------
    m.bone('crown', 'weapon', rig.G)
    crown = []
    # The guard the crescent is socketed into.
    socket = M.lathe([(0.0, -1.1), (1.5, -0.9), (2.0, 0.4), (1.7, 2.3), (1.1, 3.0), (0.0, 3.2)],
                     18, 'frame', closed_top=True, closed_bottom=True).smooth()
    socket.rotate(M.v3(1, 0, 0), 8)
    socket.translate(-0.9, 0.6, -0.1)
    crown.append(socket)
    for i, (rx, ry, rr) in enumerate(((0.9, 2.7, 1.20), (-0.8, 3.4, 1.00), (-2.1, 2.6, 0.85),
                                      (1.9, 3.9, 0.78))):
        bloom = M.rose(rr, layers=3, per=4, material='petal', heart='petal_dark')
        bloom.rotate(M.v3(1, 0, 0), -74)                 # roses face the player, not the sky
        bloom.rotate(M.v3(0, 0, 1), i * 29)
        bloom.translate(rx, ry, 0.95 + 0.12 * (i % 3))
        crown.append(bloom)
        leaf = M.petal(rr * 1.5, rr * 0.8, 0.5, 7, 'petal_dark', 0.04)
        leaf.rotate(M.v3(0, 0, 1), 140 + i * 37)
        leaf.translate(rx, ry - rr * 0.4, 0.55)
        crown.append(leaf)
    part('crown', *crown)

    # The crow's skull, which the mod lights on a hit.
    m.bone('skull_illuminated', 'crown', rig.G)
    skull = M.bird_skull(5.2, 'bone', 'bone_dark')
    skull.rotate(M.v3(1, 0, 0), -90)      # looking down the grip, beak towards the pommel
    skull.rotate(M.v3(0, 0, 1), 8)
    skull.translate(0.4, 1.0, 1.25)
    socket_gem = M.gem(0.55, 0.8, 8, 'gem')
    socket_gem.rotate(M.v3(1, 0, 0), 90).translate(0.4, 2.4, 1.6)
    part('skull_illuminated', skull, socket_gem)

    # The prong: a hooked spur off the guard, opposite the crescent.
    m.bone('prong', 'crown', rig.at((2.0, 2.0, 0)))
    prong_path = M.catmull([p3(np.array([1.5, 1.9])), p3(np.array([2.9, 3.2])),
                            p3(np.array([3.6, 5.1])), p3(np.array([3.4, 6.4]))], 16)
    part('prong', M.tube(prong_path, [0.85, 0.55, 0.30, 0.04], 7, 'frame', squash=0.7).smooth())

    # Chains, hung off the guard and swinging with it.
    chains = []
    for i, (cx, drop) in enumerate(((-2.2, -3.4), (2.3, -2.6))):
        link_path = [p3(np.array([cx, 1.2]), 0.4), p3(np.array([cx * 1.25, drop * 0.5]), 0.8),
                     p3(np.array([cx * 0.9, drop]), 0.5)]
        chains.append(M.chain(link_path, 5, 0.38, 0.10, 'inner'))
    part('crown', *chains)

    # ---- the grip ------------------------------------------------------------------------------
    m.bone('grip', 'weapon', rig.G)
    spine = [p3(np.array([0.9 * math.sin((0.5 - i * 1.05) * 0.32), 0.5 - i * 1.05])) for i in range(12)]
    core = M.tube(M.catmull(spine, 26), [1.25, 1.45, 1.35, 1.1], 8, 'leather', squash=0.82).smooth()
    grip = [core]
    for i in range(9):
        t = i / 8
        y = 0.1 - t * 10.4
        band = M.torus(1.42 - 0.08 * abs(t - 0.5) * 2, 0.17, 12, 5, 'wrap')
        band.rotate(M.v3(1, 0, 0), 90)
        band.rotate(M.v3(0, 0, 1), 16 + 8 * math.sin(i))
        band.translate(0.9 * math.sin(y * 0.32), y, 0)
        grip.append(band.smooth())
    part('grip', *grip)

    # The forked hook the grip ends in.
    last = np.array([0.9 * math.sin((0.5 - 11 * 1.05) * 0.32), 0.5 - 11 * 1.05])
    m.bone('hook', 'grip', rig.at((last[0], last[1], 0)))
    hook_pts = []
    a = last.copy()
    ang = -math.pi / 2
    for j in range(6):
        hook_pts.append(p3(a))
        ang += 0.52
        a = a + np.array([math.cos(ang), math.sin(ang)]) * 1.25
    hook = M.tube(M.catmull(hook_pts, 20), [1.05, 0.78, 0.5, 0.22, 0.04], 7, 'frame').smooth()
    fork = M.tube(M.catmull([p3(a - np.array([0.2, 0.2])), p3(a + np.array([-1.0, 0.7]))], 8),
                  [0.34, 0.03], 6, 'edge').smooth()
    part('hook', hook, fork)

    for meshes in groups.values():
        for mesh in meshes:
            ATLAS.apply(mesh)
    return m, rig, groups


PROFILE = {
    # Where the claw sits in the hand out of the box: far enough out that the crescent clears the
    # crosshair, low enough that the roses are not in the way. The sliders still override it.
    'placement': [42, 66, 24, -30, 0, 0, 0, 0],
    'profile': 'raven_claw',
    'spine': SEGMENTS,
    'trail': [0.3, 0.95, 1.0],
    'trail2': [0.75, 0.35, 1.0],
    'feathers': True,
    'summon': True,
    'summon_style': 'wing',
    'emissive': [0.85, 0.4, 1.0],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}

if __name__ == '__main__':
    model, rig_, groups_ = build()
    total = sum(len(x) for v in groups_.values() for x in v)
    print('bones', len(model.bones), 'groups', len(groups_), 'triangles', total)
