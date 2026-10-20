"""First person the way the pack authors it: the `idle_view` bone is the eye.

Model space is already view space -- 1 unit = 1/16 block, Y up, -Z forward -- once the
`idle_view` pivot is taken as the origin and X is mirrored (the pack puts the right hand
on -X). Nothing is measured, fitted or auto-aligned, and this draws exactly what
MeleeRenderer.place() does.

The two `*hand_pos` bones hold one arm-shaped marker box each -- twelve units long, four
across, shoulder at the high-Y end: a vanilla player arm to the unit, which is how the
authored scale is known. The game hides them and draws the player's own arm there; here
they are drawn flat in skin colour, so a glance says whether the weapon is in the hand.

    python3 -I fp.py <unpacked-pack-root> out.png [clip] [time]
"""
import sys, os, glob, math
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, __file__.rsplit('/', 1)[0])
import bedrock as bd

W, H = 960, 540
FOV = 70.0
ASPECT = W / float(H)
SKIN = (0.76, 0.56, 0.42)


def eye(geo):
    b = geo.by_name.get('idle_view') or geo.by_name.get('camera')
    return np.array(b.pivot, float)


def project(points):
    f = 1.0 / math.tan(math.radians(FOV) / 2.0)
    out = []
    for x, y, z in points:
        if -z < 0.02:
            out.append(None)
            continue
        out.append((W * 0.5 * (1 + (f / ASPECT) * x / -z), H * 0.5 * (1 - f * y / -z), -z))
    return out


def build(pack, weapon, clip_name, time, hands=True, extra=np.eye(4)):
    geo = bd.Geo('%s/assets/delta_wt/geo_models/melee/%s_geo.json' % (pack, weapon))
    clips = bd.load_clips('%s/assets/delta_wt/animations/melee/%s.animation.json' % (pack, weapon))
    clip = clips.get(clip_name) or clips.get('static_idle')
    mats = bd.pose(geo, clip, time)
    origin = eye(geo)

    def to_view(p):
        q = (np.asarray(p[:3], float) - origin) / 16.0
        q = extra @ np.array([-q[0], q[1], q[2], 1.0])
        return (q[0], q[1], q[2])

    tris = []
    for bone in geo.bones:
        if not bone.cubes:
            continue
        hand = bone.name.endswith('hand_pos')
        if hand and not hands:
            continue
        m = mats[bone.name]
        for cube in bone.cubes:
            for pts, uvs, normal in bd.cube_quads(cube, geo):
                eyepts = [to_view(m @ p) for p in pts]
                n = m[:3, :3] @ np.array(normal)
                n = np.array([-n[0], n[1], n[2]])
                ln = np.linalg.norm(n)
                if ln > 1e-6:
                    n = n / ln
                shade = 0.35 + 0.65 * max(0.0, float(np.dot(n, np.array([0.3, 0.5, 0.81]))))
                s = project(eyepts)
                if any(v is None for v in s):
                    continue
                flat = SKIN if hand else None
                tris.append(((s[0], s[1], s[2]), (uvs[0], uvs[1], uvs[2]), shade, flat))
                tris.append(((s[0], s[2], s[3]), (uvs[0], uvs[2], uvs[3]), shade, flat))
    return tris


def draw(tris, texture, out_path=None):
    colour = np.zeros((H, W, 3), dtype=np.float32)
    depth = np.full((H, W), 1e9, dtype=np.float32)
    tex = np.asarray(texture.convert('RGBA'), dtype=np.float32) / 255.0
    th, tw = tex.shape[0], tex.shape[1]
    for (a, b, c), (ua, ub, uc), shade, flat in tris:
        xs = [a[0], b[0], c[0]]; ys = [a[1], b[1], c[1]]
        x0, x1 = max(0, int(min(xs))), min(W - 1, int(max(xs)) + 1)
        y0, y1 = max(0, int(min(ys))), min(H - 1, int(max(ys)) + 1)
        if x1 < x0 or y1 < y0:
            continue
        area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1])
        if abs(area) < 1e-9:
            continue
        yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1]
        px, py = xx + 0.5, yy + 0.5
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
        if flat is not None:
            solid = hit
            rgb = np.broadcast_to(np.array(flat, dtype=np.float32), solid.shape + (3,))
        else:
            u = (w2 * ua[0] + w1 * ub[0] + w0 * uc[0]) % 1.0
            v = (w2 * ua[1] + w1 * ub[1] + w0 * uc[1]) % 1.0
            tx = np.clip((u * tw).astype(np.int32), 0, tw - 1)
            ty = np.clip((v * th).astype(np.int32), 0, th - 1)
            rgba = tex[ty, tx]
            solid = hit & (rgba[..., 3] > 0.35)
            rgb = rgba[..., :3]
        if not solid.any():
            continue
        sub[solid] = z[solid]
        patch = colour[y0:y1 + 1, x0:x1 + 1]
        patch[solid] = rgb[solid] * shade
    im = Image.fromarray((np.clip(colour, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    d.line([(W // 2 - 7, H // 2), (W // 2 + 7, H // 2)], fill=(110, 110, 110))
    d.line([(W // 2, H // 2 - 7), (W // 2, H // 2 + 7)], fill=(110, 110, 110))
    if out_path:
        im.save(out_path)
    return im


def one(pack, weapon, clip_name='static_idle', time=0.0, hands=True, extra=np.eye(4), label=None):
    tris = build(pack, weapon, clip_name, time, hands, extra)
    im = draw(tris, Image.open('%s/assets/delta_wt/textures/melee/%s_uv.png' % (pack, weapon)))
    ImageDraw.Draw(im).text((10, 10), label or '%s  %s t=%.2f' % (weapon, clip_name, time),
                            fill=(255, 215, 110))
    return im


def sheet(pack, out, clip_name='static_idle', time=0.0, cols=3, hands=True, scale=2):
    weapons = sorted(os.path.basename(f).split('_geo')[0]
                     for f in glob.glob(pack + '/assets/delta_wt/geo_models/melee/*_geo.json'))
    rows = (len(weapons) + cols - 1) // cols
    big = Image.new('RGB', (W * cols, H * rows), (10, 10, 14))
    for i, w in enumerate(weapons):
        big.paste(one(pack, w, clip_name, time, hands), ((i % cols) * W, (i // cols) * H))
    big.resize((W * cols // scale, H * rows // scale)).save(out)
    print('wrote', out)


if __name__ == '__main__':
    sheet(sys.argv[1], sys.argv[2],
          sys.argv[3] if len(sys.argv) > 3 else 'static_idle',
          float(sys.argv[4]) if len(sys.argv) > 4 else 0.0)
