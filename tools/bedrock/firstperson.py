"""First person with the weapon on the pack's anchor and the arms aimed from vanilla's shoulder."""
import sys, os, glob
import numpy as np
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bedrock as bd
import fp
import armaim

SKIN = (0.76, 0.56, 0.42)


def wrist_of(geo, mats, bone, origin):
    c = geo.by_name[bone].cubes[0]
    ox, oy, oz = c.origin
    sx, sy, sz = c.size
    q = (mats[bone] @ np.array([ox + sx / 2.0, oy, oz + sz / 2.0, 1.0]))[:3]
    v = (q - origin) / 16.0
    return np.array([-v[0], v[1], v[2]])


def build(pack, weapon, clip_name, time, left=True):
    geo = bd.Geo('%s/assets/delta_wt/geo_models/melee/%s_geo.json' % (pack, weapon))
    clips = bd.load_clips('%s/assets/delta_wt/animations/melee/%s.animation.json' % (pack, weapon))
    clip = clips.get(clip_name) or clips.get('static_idle')
    mats = bd.pose(geo, clip, time)
    origin = fp.eye(geo)

    tris = []
    for bone in geo.bones:
        if not bone.cubes or bone.name.endswith('hand_pos'):
            continue
        m = mats[bone.name]
        for cube in bone.cubes:
            for pts, uvs, normal in bd.cube_quads(cube, geo):
                eyepts = []
                for p in pts:
                    q = (m @ p)[:3]
                    v = (q - origin) / 16.0
                    eyepts.append((-v[0], v[1], v[2]))
                n = m[:3, :3] @ np.array(normal)
                n = np.array([-n[0], n[1], n[2]])
                ln = np.linalg.norm(n)
                if ln > 1e-6:
                    n = n / ln
                shade = 0.35 + 0.65 * max(0.0, float(np.dot(n, np.array([0.3, 0.5, 0.81]))))
                s = fp.project(eyepts)
                if any(v is None for v in s):
                    continue
                tris.append(((s[0], s[1], s[2]), (uvs[0], uvs[1], uvs[2]), shade, None))
                tris.append(((s[0], s[2], s[3]), (uvs[0], uvs[2], uvs[3]), shade, None))

    arms = [('righthand_pos', 1.0)]
    if left:
        arms.append(('lefthand_pos', -1.0))
    for bone, side in arms:
        shoulder = armaim.SHOULDER * np.array([side, 1.0, 1.0])
        wrist = wrist_of(geo, mats, bone, origin)
        for corners, normal in armaim.box(shoulder, wrist):
            ln = np.linalg.norm(normal)
            n = normal / ln if ln > 1e-6 else normal
            shade = 0.35 + 0.65 * max(0.0, float(np.dot(n, np.array([0.3, 0.5, 0.81]))))
            s = fp.project([tuple(c) for c in corners])
            if any(v is None for v in s):
                continue
            tris.append(((s[0], s[1], s[2]), ((0, 0), (0, 0), (0, 0)), shade, SKIN))
            tris.append(((s[0], s[2], s[3]), ((0, 0), (0, 0), (0, 0)), shade, SKIN))
    return tris


def one(pack, weapon, clip='static_idle', time=0.0, label=None):
    tris = build(pack, weapon, clip, time)
    im = fp.draw(tris, Image.open('%s/assets/delta_wt/textures/melee/%s_uv.png' % (pack, weapon)))
    ImageDraw.Draw(im).text((10, 10), label or '%s  %s t=%.2f' % (weapon, clip, time),
                            fill=(255, 215, 110))
    return im


def sheet(pack, out, clip='static_idle', time=0.0, cols=3):
    weapons = sorted(os.path.basename(f).split('_geo')[0]
                     for f in glob.glob(pack + '/assets/delta_wt/geo_models/melee/*_geo.json'))
    rows = (len(weapons) + cols - 1) // cols
    big = Image.new('RGB', (fp.W * cols, fp.H * rows), (10, 10, 14))
    for i, w in enumerate(weapons):
        big.paste(one(pack, w, clip, time), ((i % cols) * fp.W, (i // cols) * fp.H))
    big.resize((fp.W * cols // 2, fp.H * rows // 2)).save(out)
    print('wrote', out)


if __name__ == '__main__':
    sheet(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else 'static_idle',
          float(sys.argv[4]) if len(sys.argv) > 4 else 0.0)
