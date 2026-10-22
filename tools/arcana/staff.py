"""The Weeping Eye — a staff built as triangles, from the concept art.

What the art has, and what the box version could not: a shaft braided from three strands that wind
around each other, a four-pointed steel head with a living eye set into it, a crown of tendrils
behind that head each carrying an eye of its own, a runed collar with a cabochon set in it, a
clawed pommel closed around a dark stone, and blood running off all of it.

The bone names are the ones the clips already drive — `ext_a`, `ext_b` and `hook` are the three
tendril clusters that flex on a hit, `eye_K_iris_illuminated` are the irises that dart — so the
animation written for the old shape still moves the new one, only now there is something worth
moving.
"""
import math

import numpy as np

import mesh as M
from geo import Model
from rig import Rig, shrink

ID = 'fractured_eye_katana'
NAME = 'The Weeping Eye'
TEX = 512

# How many eyes ride the tendrils, plus the great eye in the head.
TENDRILS = 14

MATERIALS = ['steel', 'steel_dark', 'vein_red', 'vein_blue', 'sclera', 'iris',
             'pupil', 'gem_red', 'blood', 'rune', 'gold', 'lid', 'fang',
             'iris_cyan', 'iris_violet', 'iris_amber', 'iris_green']
EMISSIVE = {'iris', 'gem_red', 'iris_cyan', 'iris_violet', 'iris_amber', 'iris_green'}
# The tendril eyes are not all the same eye: each takes one of these, so the crown reads as a
# crowd of different things looking at you rather than one eye repeated.
TENDRIL_IRISES = ('iris', 'iris_cyan', 'iris_violet', 'iris_amber', 'iris_green')


def _star_outline(points=4, outer=7.4, inner=2.6, waist=0.42):
    """The head: a four-pointed star with concave flanks, drawn as a closed outline."""
    out = []
    for i in range(points):
        a0 = -math.pi / 2 + i / points * math.tau
        a1 = a0 + math.tau / points
        mid = (a0 + a1) / 2
        # The long point, then a pulled-in flank before the next one.
        reach = outer * (1.35 if i % 2 == 0 else 1.0)
        out.append((math.cos(a0) * reach, math.sin(a0) * reach))
        out.append((math.cos(a0 + 0.26) * inner * 1.5, math.sin(a0 + 0.26) * inner * 1.5))
        out.append((math.cos(mid) * inner * waist, math.sin(mid) * inner * waist))
        out.append((math.cos(a1 - 0.26) * inner * 1.5, math.sin(a1 - 0.26) * inner * 1.5))
    return out


def _braid(parts, bottom, top, radius, turns=2.6):
    """Three strands wound about the shaft's axis, plus the dark core they wind around."""
    core = M.tube(M.catmull([(0, bottom, 0), (0, (bottom + top) / 2, 0), (0, top, 0)], 24),
                  radius * 0.62, 8, 'steel_dark')
    parts.append(core.smooth())
    strands = ('steel', 'vein_red', 'vein_blue')
    for k, mat in enumerate(strands):
        path = M.helix(turns, top - bottom, radius, samples=52, phase=k * math.tau / 3)
        path = [p + M.v3(0, bottom, 0) for p in path]
        # Each strand thins towards the head, the way a braid is pulled tight.
        radii = [radius * (0.40 - 0.12 * (i / 52)) for i in range(53)]
        parts.append(M.tube(path, radii, 7, mat).smooth())


def _tendril(parts, bones, index, root, direction, length, curl, mats, eye_material='iris'):
    """One curling tendril with an eye on its end. Returns the eye's bone name."""
    d = M.norm(np.asarray(direction, dtype=float))
    side = np.cross(d, M.v3(0, 0, 1))
    if np.linalg.norm(side) < 1e-6:
        side = M.v3(1, 0, 0)
    side = M.norm(side)
    # A path that leaves the head, sweeps over and curls back on itself.
    pts = [np.asarray(root, dtype=float)]
    here = np.asarray(root, dtype=float)
    heading = d
    for i in range(5):
        step = length / 5
        here = here + heading * step
        pts.append(here.copy())
        # Curl: bend the heading a little further each step, which is what makes the hook.
        heading = M.norm(heading + side * curl * (0.35 + i * 0.22) + M.v3(0, 0.06, 0))
    path = M.catmull(pts, 22)
    radii = [0.58 * (1 - 0.72 * (i / 21)) + 0.08 for i in range(22)]
    parts.append(M.tube(path, radii, 6, mats).smooth())
    return path[-1], M.norm(path[-1] - path[-4])


def _lids(radius, material, lashes=False, look=(0, 0, 1), detail=1.0):
    """An upper and a lower lid round an eyeball of `radius` looking along `look`.

    Each lid is a cap of a slightly larger sphere, so between them they cover everything but an
    almond across the front of the eye - which is what stops an eyeball reading as a ball with a
    sticker on it. The great eye also gets a row of lashes along its upper lid.
    """
    r = radius * 1.07
    out = []
    for upper, reach in ((True, 58), (False, 62)):
        rows = max(4, int(8 * detail))
        profile = []
        for i in range(rows + 1):
            th = math.radians(reach * i / rows)
            profile.append((math.sin(th) * r, math.cos(th) * r))
        cap = M.lathe(profile, max(8, int(16 * detail)), material, closed_top=False).smooth()
        if not upper:
            cap.rotate(M.v3(1, 0, 0), 180)
        # Tip the caps toward the front a little, so the almond opens toward the look direction.
        cap.rotate(M.v3(1, 0, 0), -14 if upper else 14)
        out.append(cap)
        if upper and lashes:
            th = math.radians(reach)
            for k in range(9):
                az = math.radians(-60 + 120 * k / 8)
                base = M.v3(math.sin(th) * r * math.sin(az), math.cos(th) * r, math.sin(th) * r * math.cos(az))
                base = _rot_x(base, -14)
                out_dir = M.norm(base + M.v3(0, 0.6, 0.4))
                lash = M.tube(M.catmull([base, base + out_dir * radius * 0.22,
                                         base + out_dir * radius * 0.36 + M.v3(0, radius * 0.08, 0)], 6),
                              [radius * 0.035, radius * 0.006], 4, 'steel_dark')
                out.append(lash)
    d = M.norm(np.asarray(look, dtype=float))
    up = M.v3(0, 0, 1)
    axis = np.cross(up, d)
    if np.linalg.norm(axis) > 1e-6:
        angle = math.degrees(math.acos(max(-1.0, min(1.0, float(np.dot(up, d))))))
        for mesh in out:
            mesh.rotate(axis, angle)
    return out


def _rot_x(p, degrees):
    a = math.radians(degrees)
    c, s_ = math.cos(a), math.sin(a)
    return M.v3(p[0], p[1] * c - p[2] * s_, p[1] * s_ + p[2] * c)


ATLAS = M.Atlas(TEX)
ATLAS.pack(MATERIALS + ['arm'])

FINISHES = ['', 'obsidian', 'molten', 'spectral', 'dark_metal']

# Each finish restates only what it changes; the rest is the weapon as drawn in the art.
PALETTES = {
    '':           {'steel': (0.30, 0.30, 0.36), 'dark': (0.09, 0.08, 0.11), 'gold': (0.96, 0.72, 0.28),
                   'red': (0.62, 0.06, 0.12), 'blue': (0.30, 0.10, 0.48),
                   'iris': (1.00, 0.16, 0.10), 'gem': (0.85, 0.04, 0.16), 'lid': (0.55, 0.20, 0.24)},
    'obsidian':   {'steel': (0.14, 0.13, 0.18), 'dark': (0.05, 0.04, 0.07), 'gold': (0.62, 0.42, 0.85),
                   'red': (0.42, 0.04, 0.22), 'blue': (0.22, 0.06, 0.40),
                   'iris': (0.85, 0.20, 1.00), 'gem': (0.55, 0.06, 0.70), 'lid': (0.30, 0.12, 0.30)},
    'molten':     {'steel': (0.28, 0.18, 0.12), 'dark': (0.10, 0.05, 0.03), 'gold': (1.00, 0.62, 0.15),
                   'red': (0.85, 0.22, 0.04), 'blue': (0.55, 0.12, 0.02),
                   'iris': (1.00, 0.55, 0.08), 'gem': (1.00, 0.30, 0.04), 'lid': (0.55, 0.22, 0.10)},
    'spectral':   {'steel': (0.48, 0.58, 0.66), 'dark': (0.12, 0.18, 0.24), 'gold': (0.75, 0.95, 1.00),
                   'red': (0.16, 0.55, 0.70), 'blue': (0.32, 0.30, 0.85),
                   'iris': (0.35, 0.95, 1.00), 'gem': (0.25, 0.75, 0.95), 'lid': (0.30, 0.42, 0.55)},
    'dark_metal': {'steel': (0.22, 0.22, 0.25), 'dark': (0.06, 0.06, 0.07), 'gold': (0.70, 0.66, 0.58),
                   'red': (0.50, 0.06, 0.08), 'blue': (0.20, 0.10, 0.26),
                   'iris': (0.95, 0.12, 0.16), 'gem': (0.60, 0.04, 0.10), 'lid': (0.32, 0.14, 0.16)},
}


def paint_atlas(finish):
    import paint
    import paint_mesh as pm
    p = PALETTES.get(finish or '', PALETTES[''])
    c = paint.Canvas(TEX, TEX)
    t = pm.tile
    t(c, ATLAS, 'steel', TEX, pm.metal, p['steel'], 3, polish=0.75, scratches=0.35)
    t(c, ATLAS, 'steel_dark', TEX, pm.metal, p['dark'], 4, polish=0.3)
    t(c, ATLAS, 'gold', TEX, pm.metal, p['gold'], 13, polish=0.95, scratches=0.15, glow=0.12)
    t(c, ATLAS, 'vein_red', TEX, pm.sinew, p['red'], 5, glow=0.28, wet=0.75)
    t(c, ATLAS, 'vein_blue', TEX, pm.sinew, p['blue'], 6, glow=0.22, wet=0.7)
    t(c, ATLAS, 'sclera', TEX, pm.flesh, (0.94, 0.88, 0.82), (0.75, 0.06, 0.10), 7)
    t(c, ATLAS, 'lid', TEX, pm.sinew, p['lid'], 14, wet=0.55)
    t(c, ATLAS, 'fang', TEX, pm.metal, (0.92, 0.88, 0.78), 15, polish=0.4, scratches=0.6)
    t(c, ATLAS, 'iris', TEX, pm.iris, (1.0, 0.92, 0.55), p['iris'], 8)
    t(c, ATLAS, 'iris_cyan', TEX, pm.iris, (0.9, 1.0, 1.0), (0.05, 0.75, 0.95), 16)
    t(c, ATLAS, 'iris_violet', TEX, pm.iris, (1.0, 0.85, 1.0), (0.55, 0.12, 0.95), 17)
    t(c, ATLAS, 'iris_amber', TEX, pm.iris, (1.0, 1.0, 0.75), (1.0, 0.55, 0.05), 18)
    t(c, ATLAS, 'iris_green', TEX, pm.iris, (0.9, 1.0, 0.8), (0.2, 0.9, 0.35), 19)
    paint.flat(c, *_rect('pupil'), (0.03, 0.02, 0.03), seed=9)
    t(c, ATLAS, 'gem_red', TEX, pm.stone, p['gem'], 10)
    t(c, ATLAS, 'blood', TEX, pm.liquid, (0.33, 0.03, 0.05), 11)
    t(c, ATLAS, 'rune', TEX, pm.runes, p['dark'], p['gold'], 12, glow=0.8)
    paint.flat(c, *_rect('arm'), (0.55, 0.42, 0.34), seed=1)
    return c


def _rect(name):
    x, y, w, h = ATLAS.rects[name]
    return (int(x * TEX), int(y * TEX), int(w * TEX), int(h * TEX))


def build():
    m = Model(TEX, TEX, seed=7)
    from geo import Material
    ax, ay, aw, ah = _rect('arm')
    m.mats = {'arm': Material('arm', ax, ay, aw, ah, 1)}
    # Held low and to the right, the head up and away: a staff is carried, not pointed.
    # Held like the knives in the delta pack: the hand low and right but well inside the frame,
    # the arm a long diagonal from the corner, the weapon carried across the body and pointed at
    # what you are looking at. Two hands, the left lower on the shaft.
    rig = Rig(m, grip_eye=(0.24, -0.44, -0.56), blade_dir_eye=(-0.50, 0.74, -0.45),
              face_normal_eye=(0.22, 0.28, 0.93), shoulder_eye=(0.50, -1.10, -0.10),
              left_shoulder_eye=(-0.44, -1.10, -0.10), left_grip_local=(0, -3.6, 0))
    G = rig.G
    groups = {}

    def part(bone, *meshes):
        groups.setdefault(bone, []).extend(meshes)

    # ---- shaft, collar, pommel -------------------------------------------------------------
    m.bone('handle', 'weapon', G)
    shaft = []
    _braid(shaft, -9.5, 13.0, 0.95)

    # The runed collar, a little above the grip.
    collar = M.lathe([(0.95, 0), (1.45, 0.25), (1.5, 3.4), (0.95, 3.7)], 20, 'rune')
    collar.translate(0, 1.4, 0)
    shaft.append(collar.smooth())
    for y in (1.75, 4.6):
        band = M.lathe([(1.5, 0), (1.72, 0.18), (1.72, 0.62), (1.5, 0.8)], 20, 'steel')
        band.translate(0, y, 0)
        shaft.append(band.smooth())
    stone = M.gem(0.72, 0.95, 10, 'gem_red')
    stone.rotate(M.v3(1, 0, 0), 90).translate(0, 3.1, 1.45)
    shaft.append(stone)

    # Pommel: four claws closed around a dark stone.
    orb = M.sphere(1.5, 14, 10, 'gem_red').smooth()
    orb.translate(0, -10.6, 0)
    shaft.append(orb)
    for k in range(4):
        a = k / 4 * math.tau + math.pi / 4
        root = M.v3(math.cos(a) * 1.1, -8.9, math.sin(a) * 1.1)
        tip = M.v3(math.cos(a) * 1.5, -12.4, math.sin(a) * 1.5)
        mid = M.v3(math.cos(a) * 2.3, -10.6, math.sin(a) * 2.3)
        claw = M.tube(M.catmull([root, mid, tip], 16), [0.52, 0.38, 0.06], 7, 'steel')
        shaft.append(claw.smooth())
    cap = M.lathe([(0, -13.6), (0.5, -13.0), (0.35, -12.4), (0.0, -12.2)], 12, 'steel_dark')
    shaft.append(cap.smooth())
    for mesh in shaft:
        mesh.translate(G[0], G[1], G[2])
    part('handle', *shaft)

    # ---- the head ---------------------------------------------------------------------------
    m.bone('head', 'weapon', (G[0], G[1] + 15.5, G[2]))
    head = []
    plate = M.blade(_star_outline(), 0.62, 'steel', bevel=0.38)
    head.append(plate.smooth())
    # A raised rim around the socket, and the brow ridges either side of it.
    rim = M.torus(2.95, 0.42, 26, 8, 'steel_dark')
    rim.rotate(M.v3(1, 0, 0), 90).translate(0, 0, 0.55)
    head.append(rim.smooth())
    for side in (1, -1):
        brow = M.tube(M.catmull([(side * 2.2, 2.4, 0.4), (side * 5.0, 2.0, 0.5),
                                 (side * 7.4, 0.4, 0.3)], 18), [0.52, 0.34, 0.05], 6, 'steel')
        head.append(brow.smooth())
    # A gold rim chased round the whole star, and a filigree curl on each long point.
    outline = _star_outline()
    rim_path = [M.v3(x, y, 0.34) for (x, y) in outline] + [M.v3(outline[0][0], outline[0][1], 0.34)]
    head.append(M.tube(M.catmull(rim_path, 96, closed=False), 0.13, 5, 'gold').smooth())
    for i in range(4):
        a = -math.pi / 2 + i / 4 * math.tau
        reach = 7.4 * (1.35 if i % 2 == 0 else 1.0) * 0.62
        curl = M.scroll(1.3, 1.2, 0.18, 0.17, 'gold', 22, plane=('x', 'y'))
        curl.rotate(M.v3(0, 0, 1), math.degrees(a))
        curl.translate(math.cos(a) * reach, math.sin(a) * reach, 0.42)
        head.append(curl)
    # Fangs round the socket, all pointing in at the eye.
    for k in range(12):
        a = k / 12 * math.tau
        fang = M.lathe([(0.0, 0.0), (0.22, 0.05), (0.14, 0.5), (0.0, 0.85)], 7, 'fang', closed_bottom=True)
        fang.rotate(M.v3(0, 0, 1), math.degrees(a) + 90)
        fang.translate(math.cos(a) * 3.25, math.sin(a) * 3.25, 0.55)
        head.append(fang.smooth())
    # Blood strands hanging off the bottom of the plate.
    for k, (dx, length) in enumerate(((-1.6, 4.2), (0.4, 5.6), (2.2, 3.4))):
        strand = M.ribbon(M.catmull([M.v3(dx, -2.6, 0.2), M.v3(dx + 0.25, -2.6 - length * 0.5, 0.35),
                                     M.v3(dx - 0.1, -2.6 - length, 0.3)], 12),
                          0.32, 'blood', thickness=0.05, twist=0.3, taper=0.7)
        head.append(strand)
    for mesh in head:
        mesh.translate(G[0], G[1] + 15.5, G[2])
    part('head', *head)

    # The great eye, in its own bone so it can widen and dart.
    m.bone('eye_0', 'head', (G[0], G[1] + 15.5, G[2]))
    m.bone('eye_0_iris_illuminated', 'eye_0', (G[0], G[1] + 15.5, G[2]))
    ball, iris = M.eyeball(2.6, 'sclera', 'iris', look=(0, 0, 1), iris=0.62)
    veins = []
    for k in range(7):
        a = k / 7 * math.tau
        start = M.v3(math.cos(a) * 0.9, math.sin(a) * 0.9, 2.3)
        end = M.v3(math.cos(a + 0.5) * 2.3, math.sin(a + 0.5) * 2.3, 1.2)
        mid = (start + end) * 0.5 + M.v3(0, 0, 0.5)
        veins.append(M.tube(M.catmull([start, mid, end], 10), [0.1, 0.07, 0.03], 5, 'vein_red').smooth())
    pupil = M.ellipsoid(0.22, 1.05, 0.16, 10, 8, 'pupil').smooth()
    pupil.translate(0, 0, 2.62 * 0.62 + 0.12)
    lids = _lids(2.6, 'lid', lashes=True)
    for mesh in [ball] + veins + lids:
        mesh.translate(G[0], G[1] + 15.5, G[2])
    part('eye_0', ball, *veins, *lids)
    for mesh in (iris, pupil):
        mesh.translate(G[0], G[1] + 15.5, G[2])
    part('eye_0_iris_illuminated', iris, pupil)

    # ---- the crown of tendrils ----------------------------------------------------------------
    clusters = (('ext_a', -1), ('hook', 0), ('ext_b', 1))
    for name, lean in clusters:
        m.bone(name, 'head', (G[0] + lean * 2.0, G[1] + 17.5, G[2] - 0.3))
    eye_index = 1
    for t in range(TENDRILS):
        f = t / (TENDRILS - 1)
        # Fanned across the back of the head, leaning out as they go.
        spread = (f - 0.5) * 2
        cluster = 'ext_a' if spread < -0.33 else ('ext_b' if spread > 0.33 else 'hook')
        lean = -1 if cluster == 'ext_a' else (1 if cluster == 'ext_b' else 0)
        origin = M.v3(G[0] + lean * 2.0, G[1] + 17.5, G[2] - 0.3)
        root = M.v3(spread * 3.4, 1.2 + abs(spread) * -0.8, -0.2)
        direction = M.v3(spread * 1.3, 1.0, -0.25)
        curl = (1 if t % 2 else -1) * (0.55 + 0.3 * abs(spread))
        mats = 'vein_red' if t % 2 == 0 else 'vein_blue'
        parts = []
        tip, facing = _tendril(parts, m, t, root, direction, 5.6 + 2.4 * (1 - abs(spread)), curl, mats)
        for mesh in parts:
            mesh.translate(origin)
        part(cluster, *parts)

        # The eye on its end, with its own iris bone.
        eye_bone = 'eye_%d' % eye_index
        iris_bone = 'eye_%d_iris_illuminated' % eye_index
        centre = origin + tip
        m.bone(eye_bone, cluster, (centre[0], centre[1], centre[2]))
        m.bone(iris_bone, eye_bone, (centre[0], centre[1], centre[2]))
        r = 0.62 + 0.24 * (1 - abs(spread))
        look = (facing[0], facing[1], facing[2] + 0.6)
        ball, iris = M.eyeball(r, 'sclera', TENDRIL_IRISES[t % len(TENDRIL_IRISES)], look=look, iris=0.6)
        lid = _lids(r, 'lid', lashes=False, look=look, detail=0.55)
        ball.translate(centre)
        iris.translate(centre)
        for mesh in lid:
            mesh.translate(centre)
        part(eye_bone, ball, *lid)
        part(iris_bone, iris)
        eye_index += 1

    # ---- three eyes in orbit about the head ----------------------------------------------------
    for k in range(3):
        bone = 'orbit_%d' % k
        m.bone(bone, 'head', (G[0], G[1] + 15.5, G[2]))
        a = k / 3 * math.tau + 0.4
        radius = 6.4 + 0.6 * k
        centre = M.v3(math.cos(a) * radius, math.sin(a) * radius, 1.6 + 0.4 * k)
        iris_mat = ('iris_cyan', 'iris_violet', 'iris_amber')[k]
        ball, iris = M.eyeball(0.85, 'sclera', iris_mat, look=(0, 0, 1), iris=0.62)
        halo = M.torus(1.25, 0.08, 18, 5, 'gold')
        halo.rotate(M.v3(1, 0, 0), 90)
        pieces = [ball, iris, halo.smooth()] + _lids(0.85, 'lid', lashes=False, detail=0.55)
        for mesh in pieces:
            mesh.translate(centre)
            mesh.translate(G[0], G[1] + 15.5, G[2])
        part(bone, *pieces)

    # ---- blood ---------------------------------------------------------------------------------
    drops = []
    for k, (dx, dy) in enumerate(((-4.2, -3.4), (-1.6, -4.6), (1.9, -4.2), (4.4, -3.0), (0.2, -6.2))):
        drop = M.lathe([(0, 0), (0.34, 0.36), (0.26, 0.9), (0, 1.35)], 10, 'blood')
        drop.rotate(M.v3(1, 0, 0), 180)
        drop.translate(G[0] + dx, G[1] + 15.5 + dy - 0.5 * (k % 2), G[2] + 0.3)
        drops.append(drop.smooth())
    part('head', *drops)

    # Built at the size of the art, held at the size of a hand: see rig.shrink.
    shrink(m, groups, rig, 0.42)
    for meshes in groups.values():
        for mesh in meshes:
            ATLAS.apply(mesh)
    return m, rig, groups


if __name__ == '__main__':
    model, rig_, groups_ = build()
    total = sum(len(x) for v in groups_.values() for x in v)
    print('bones', len(model.bones), 'groups', len(groups_), 'triangles', total)


# Read by the mod's weapon effects: which behaviours this weapon has.
PROFILE = {
    # Where the staff sits in the hand before the player moves anything. It is a long weapon, so
    # it is pushed further away and down than a blade would be.
    'profile': 'fractured_eye',
    'eyes': True,
    'summon': True,
    'summon_style': 'eye',
    'trail': [0.85, 0.20, 0.30],
    'trail2': [0.35, 0.55, 0.95],
    'shards': True,
    'emissive': [1.0, 0.18, 0.18],
    'finishes': ['obsidian', 'molten', 'spectral', 'dark_metal'],
}
