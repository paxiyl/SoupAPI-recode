"""First person with a fit-to-frame anchor: the idle pose's bounding box is scaled and
placed so the weapon and both hands sit in frame, whatever the weapon or the pack.
"""
import sys, os, glob, math
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, __file__.rsplit('/', 1)[0])
import bedrock as bd
import render as rd
from fp3 import mesh_points

DIST = 0.95        # blocks from the eye to the model's centre
FILL = 0.78        # fraction of the view the model spans
OFFSET = np.array([0.10, -0.12, 0.0])


def fit(geo, clips, fov=70.0):
    idle = clips.get('static_idle') or next(iter(clips.values()))
    mats = bd.pose(geo, idle, 0.0)
    pts = mesh_points(geo, mats)
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    centre = (lo + hi) / 2.0
    span = float(max(hi[0] - lo[0], hi[1] - lo[1]))
    view_h = 2.0 * DIST * math.tan(math.radians(fov) / 2.0)
    scale = (view_h * FILL) / span if span > 1e-6 else 1.0
    target = np.array([OFFSET[0], OFFSET[1], -DIST])
    return scale, target - centre * scale


def build(pack, weapon, clip_name, time, fov=70.0):
    geo = bd.Geo('%s/assets/delta_wt/geo_models/melee/%s_geo.json' % (pack, weapon))
    clips = bd.load_clips('%s/assets/delta_wt/animations/melee/%s.animation.json' % (pack, weapon))
    scale, anchor = fit(geo, clips, fov)
    clip = clips.get(clip_name) or clips.get('static_idle')
    mats = bd.pose(geo, clip, time)

    tris = []
    for bone in geo.bones:
        if not bone.cubes:
            continue
        m = mats[bone.name]
        for cube in bone.cubes:
            for pts, uvs, normal in bd.cube_quads(cube, geo):
                eyepts = []
                for p in pts:
                    q = m @ p
                    eyepts.append((-q[0] / 16.0 * scale + anchor[0],
                                   q[1] / 16.0 * scale + anchor[1],
                                   q[2] / 16.0 * scale + anchor[2]))
                n = m[:3, :3] @ np.array(normal)
                n = np.array([-n[0], n[1], n[2]])
                ln = np.linalg.norm(n)
                if ln > 1e-6:
                    n = n / ln
                shade = 0.35 + 0.65 * max(0.0, float(np.dot(n, np.array([0.3, 0.5, 0.81]))))
                s = rd.project(eyepts, fov, 1.0)
                if any(v is None for v in s):
                    continue
                tris.append(((s[0], s[1], s[2]), (uvs[0], uvs[1], uvs[2]), shade))
                tris.append(((s[0], s[2], s[3]), (uvs[0], uvs[2], uvs[3]), shade))
    return tris, scale


def sheet(pack, out, clip_name='static_idle', time=0.0, weapons=None, cols=4):
    weapons = weapons or sorted(os.path.basename(f).split('_geo')[0]
                                for f in glob.glob(pack + '/assets/delta_wt/geo_models/melee/*_geo.json'))
    rows = (len(weapons) + cols - 1) // cols
    im_sheet = Image.new('RGB', (rd.W * cols, rd.H * rows), (10, 10, 14))
    for i, w in enumerate(weapons):
        tris, scale = build(pack, w, clip_name, time)
        tmp = './_f4_%s.png' % w
        rd.draw(tris, Image.open('%s/assets/delta_wt/textures/melee/%s_uv.png' % (pack, w)), tmp)
        im = Image.open(tmp)
        ImageDraw.Draw(im).text((10, 10), '%s  %s t=%.2f  fit=%.3f' % (w, clip_name, time, scale),
                                fill=(255, 215, 110))
        im_sheet.paste(im, ((i % cols) * rd.W, (i // cols) * rd.H))
    im_sheet.resize((rd.W * cols // 2, rd.H * rows // 2)).save(out)
    print('wrote', out)


if __name__ == '__main__':
    sheet(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else 'static_idle',
          float(sys.argv[4]) if len(sys.argv) > 4 else 0.0)
