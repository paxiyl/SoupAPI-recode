"""Score the first-person placement against screenshots of the source mod.

Given several screenshots of the SAME scene from the SAME camera with different weapons held,
the per-pixel median across them is the background, and anything far from it is the weapon and
the arms. That gives a real silhouette to score against instead of a colour guess.

Point SHOTS at the images and run it to sweep a convention. Be warned what it measured here: on
one set of nine frames from the source mod's showcase video, EVERY rotation convention, every
view-space offset and every FOV scored a mean IoU of 0.14-0.17. A ceiling that flat means the
rig is not discriminating - the frames carry camera jitter between them, so the background
median leaves scene edges in the mask, and there is no way to tell whether a showcase frame is
the resting pose or the middle of an inspect. Screenshots from a build of this mod, where the
pose is known, are worth far more than a video of another one.
""" 
import os, sys, itertools
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np
from PIL import Image
import bedrock as bd
import fp

P = os.environ.get('MELEE_PACK', 'src/main/resources/assets/soupapi/melee_packs/delta_wt')
U = os.environ.get('MELEE_SHOTS', '.')
SHOTS = [('e1029104', 'quenching'), ('20fcd4ed', 'treasure_kn'), ('6c3de40d', 'cobra_vanguard'),
         ('03bc25cb', 'mercy'), ('7eb5f596', 'thieve_sea'), ('ee7c8935', 'polaris'),
         ('17056eba', 'annihil'), ('7381f820', 'shadowkiller'), ('015f24ca', 'executor')]
fp.W, fp.H = 526, 270
fp.ASPECT = fp.W / float(fp.H)


def erode(m):
    out = m.copy()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out &= np.roll(m, (dy, dx), (0, 1))
    return out


def dilate(m):
    out = m.copy()
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out |= np.roll(m, (dy, dx), (0, 1))
    return out


def references():
    frames = np.stack([np.asarray(Image.open('%s/%s-image.jpg' % (U, u)).convert('RGB')
                                  .crop((236, 0, 2332, 1080)).resize((fp.W, fp.H)), dtype=np.float32)
                       for u, _ in SHOTS])
    bg = np.median(frames, axis=0)
    out = {}
    for (u, w), f in zip(SHOTS, frames):
        m = np.abs(f - bg).max(axis=2) > 38
        m = dilate(erode(erode(m)))          # drop the thin scene edges from camera jitter
        out[w] = m
    return out


def silhouette(weapon, rot_sign, pos_sign, order, clip_name='static_idle'):
    geo = bd.Geo('%s/assets/delta_wt/geo_models/melee/%s_geo.json' % (P, weapon))
    clips = bd.load_clips('%s/assets/delta_wt/animations/melee/%s.animation.json' % (P, weapon))
    clip = clips.get(clip_name) or clips.get('static_idle')
    mats = bd.pose(geo, clip, 0.0, rot_sign, pos_sign, order)
    origin = np.array((geo.by_name.get('idle_view') or geo.by_name['camera']).pivot, float)
    tris = []
    for bone in geo.bones:
        if not bone.cubes:
            continue
        m = mats[bone.name]
        for cube in bone.cubes:
            for pts, uvs, n in bd.cube_quads(cube, geo):
                eyepts = []
                for p in pts:
                    q = (m @ p)[:3]
                    v = (q - origin) / 16.0
                    eyepts.append((-v[0], v[1], v[2]))
                s = fp.project(eyepts)
                if any(x is None for x in s):
                    continue
                tris.append(((s[0], s[1], s[2]), ((0, 0),) * 3, 1.0, (1, 1, 1)))
                tris.append(((s[0], s[2], s[3]), ((0, 0),) * 3, 1.0, (1, 1, 1)))
    img = np.asarray(fp.draw(tris, Image.new('RGBA', (4, 4), (255, 255, 255, 255))),
                     dtype=np.float32).mean(axis=2)
    return img > 8


def iou(a, b):
    u = (a | b).sum()
    return 0.0 if u == 0 else float((a & b).sum()) / u


if __name__ == '__main__':
    refs = references()
    signs = [(1, 1, 1), (-1, -1, 1), (-1, 1, 1), (1, -1, 1), (1, 1, -1),
             (-1, -1, -1), (-1, 1, -1), (1, -1, -1)]
    orders = [''.join(p) for p in itertools.permutations('xyz')]
    rows = []
    for rs in signs:
        for order in orders:
            sc = [iou(silhouette(w, rs, (1, 1, 1), order), refs[w]) for _, w in SHOTS]
            rows.append((float(np.mean(sc)), rs, order, sc))
    rows.sort(reverse=True)
    print('%-14s %-5s %s' % ('rot sign', 'order', 'mean IoU vs footage'))
    for s, rs, order, sc in rows[:10]:
        print('%-14s %-5s %.3f   ' % (str(rs), order, s) + ' '.join('%.2f' % x for x in sc))
    cur = [r for r in rows if r[1] == (1, 1, 1) and r[2] == 'zyx'][0]
    print('\ncurrent (1,1,1) zyx: %.3f  (rank %d of %d)' % (cur[0], rows.index(cur) + 1, len(rows)))
