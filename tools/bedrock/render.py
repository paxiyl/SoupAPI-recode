"""Rasterises a posed Bedrock model to a PNG so the convention can be checked by eye.

Usage: python3 -I render.py <pack-root> <weapon> <clip> <time> <out.png> [view]
"""
import sys
import math

import numpy as np
from PIL import Image

sys.path.insert(0, __file__.rsplit('/', 1)[0])
import bedrock as bd

W = H = 640


def project(points, fov, aspect, near=0.05, far=64.0):
    f = 1.0 / math.tan(math.radians(fov) / 2.0)
    out = []
    for p in points:
        x, y, z = p[0], p[1], p[2]
        if -z < near:
            out.append(None)
            continue
        sx = (f / aspect) * x / -z
        sy = f * y / -z
        out.append((W * 0.5 * (1 + sx), H * 0.5 * (1 - sy), -z))
    return out


def draw(tris, texture, out_path):
    colour = np.zeros((H, W, 3), dtype=np.float32)
    depth = np.full((H, W), 1e9, dtype=np.float32)
    tex = np.asarray(texture.convert('RGBA'), dtype=np.float32) / 255.0
    th, tw = tex.shape[0], tex.shape[1]

    for (a, b, c), (ua, ub, uc), shade in tris:
        xs = [a[0], b[0], c[0]]
        ys = [a[1], b[1], c[1]]
        x0, x1 = max(0, int(min(xs))), min(W - 1, int(max(xs)) + 1)
        y0, y1 = max(0, int(min(ys))), min(H - 1, int(max(ys)) + 1)
        if x1 < x0 or y1 < y0:
            continue
        area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1])
        if abs(area) < 1e-9:
            continue
        yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1]
        px = xx + 0.5
        py = yy + 0.5
        w0 = ((b[0] - a[0]) * (py - a[1]) - (px - a[0]) * (b[1] - a[1])) / area
        w1 = ((px - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (py - a[1])) / area
        w2 = 1.0 - w0 - w1
        inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
        if not inside.any():
            continue
        z = w2 * a[2] + w1 * b[2] + w0 * c[2]
        sub = depth[y0:y1 + 1, x0:x1 + 1]
        hit = inside & (z < sub)
        if not hit.any():
            continue
        u = (w2 * ua[0] + w1 * ub[0] + w0 * uc[0]) % 1.0
        v = (w2 * ua[1] + w1 * ub[1] + w0 * uc[1]) % 1.0
        tx = np.clip((u * tw).astype(np.int32), 0, tw - 1)
        ty = np.clip((v * th).astype(np.int32), 0, th - 1)
        rgba = tex[ty, tx]
        solid = hit & (rgba[..., 3] > 0.35)
        if not solid.any():
            continue
        sub[solid] = z[solid]
        patch = colour[y0:y1 + 1, x0:x1 + 1]
        patch[solid] = rgba[..., :3][solid] * shade
    Image.fromarray((np.clip(colour, 0, 1) * 255).astype(np.uint8)).save(out_path)


def build(pack, weapon, clip_name, time, view, rot_sign, pos_sign, order):
    geo = bd.Geo('%s/assets/delta_wt/geo_models/melee/%s_geo.json' % (pack, weapon))
    clips = bd.load_clips('%s/assets/delta_wt/animations/melee/%s.animation.json' % (pack, weapon))
    clip = clips.get(clip_name)
    mats = bd.pose(geo, clip, time, rot_sign, pos_sign, order)

    quads = []
    for bone in geo.bones:
        if not bone.cubes:
            continue
        m = mats[bone.name]
        for cube in bone.cubes:
            for pts, uvs, normal in bd.cube_quads(cube, geo):
                world = [m @ p for p in pts]
                quads.append((world, uvs, normal, m))

    # model units -> blocks, then the view transform
    pts = np.array([[p[0], p[1], p[2]] for q in quads for p in q[0]])
    centre = (pts.min(axis=0) + pts.max(axis=0)) / 2.0
    radius = float(np.linalg.norm(pts.max(axis=0) - pts.min(axis=0))) / 2.0

    yaw, pitch = view
    cam = bd.rot_x(pitch) @ bd.rot_y(yaw)
    dist = radius * 2.4

    tris = []
    for world, uvs, normal, m in quads:
        eye = []
        for p in world:
            q = np.array([p[0] - centre[0], p[1] - centre[1], p[2] - centre[2], 1.0])
            q = cam @ q
            eye.append((q[0], q[1], q[2] - dist))
        n = m[:3, :3] @ np.array(normal)
        n = cam[:3, :3] @ n
        norm = np.linalg.norm(n)
        if norm > 1e-6:
            n = n / norm
        shade = 0.45 + 0.55 * max(0.0, float(np.dot(n, np.array([0.3, 0.6, 0.74]))))
        s = project(eye, 55.0, 1.0)
        if any(v is None for v in s):
            continue
        tris.append(((s[0], s[1], s[2]), (uvs[0], uvs[1], uvs[2]), shade))
        tris.append(((s[0], s[2], s[3]), (uvs[0], uvs[2], uvs[3]), shade))
    return tris


if __name__ == '__main__':
    pack, weapon, clip_name, time, out = sys.argv[1:6]
    view = (float(sys.argv[6]), float(sys.argv[7])) if len(sys.argv) > 7 else (25.0, 10.0)
    texture = Image.open('%s/assets/delta_wt/textures/melee/%s_uv.png' % (pack, weapon))
    tris = build(pack, weapon, clip_name, float(time), view, (1, 1, 1), (1, 1, 1), 'zyx')
    draw(tris, texture, out)
    print('wrote', out, 'tris', len(tris))
