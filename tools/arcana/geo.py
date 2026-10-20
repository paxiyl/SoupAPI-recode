"""Bedrock geometry builder for the Arcana weapons, with the mod's own conventions built in.

Everything here is in Bedrock model units (a sixteenth of a block). The maths at the bottom mirrors
me.Padej_.soupapi.bedrock exactly (X mirrored at load, cube rotations negated on X and Y, bone
rotations composed Z*Y*X) so a pose can be solved and checked here before any of it reaches Java.

In the view the mod draws, eye space is: +x screen right, +y up, -z forward. A Bedrock point maps to
Java space as (-x, y, z), so Bedrock -x is screen right.
"""
import json
import math
import random

import numpy as np


class Material:
    """A region of the weapon's texture atlas that faces sample from."""

    def __init__(self, name, x, y, w, h, scale=2.0, mode='random'):
        self.name, self.x, self.y, self.w, self.h = name, x, y, w, h
        self.scale = scale      # texels per model unit
        self.mode = mode        # 'random' patch, or 'strip' for blade unwraps


class Model:
    def __init__(self, tex_w, tex_h, seed=1):
        self.tex_w, self.tex_h = tex_w, tex_h
        self.bones = []
        self.by_name = {}
        self.rng = random.Random(seed)
        self.cube_count = 0

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        b = {'name': name, 'pivot': [round(v, 4) for v in pivot], 'cubes': []}
        if parent:
            b['parent'] = parent
        if rotation and any(abs(r) > 1e-6 for r in rotation):
            b['rotation'] = [round(r, 4) for r in rotation]
        self.bones.append(b)
        self.by_name[name] = b
        return name

    def cube(self, bone, origin, size, mat, rotation=None, pivot=None, inflate=0.0, strip=None,
             faces=None):
        """Adds a box. `strip` = (u0, v0, u1, v1) texels for the north/south faces of a blade
        segment, so consecutive segments read as one continuous painted blade."""
        c = {'origin': [round(v, 4) for v in origin], 'size': [round(max(0.0, v), 4) for v in size]}
        if rotation and any(abs(r) > 1e-6 for r in rotation):
            c['rotation'] = [round(r, 4) for r in rotation]
            c['pivot'] = [round(v, 4) for v in (pivot if pivot is not None else
                                                 [origin[i] + size[i] / 2 for i in range(3)])]
        if inflate:
            c['inflate'] = round(inflate, 4)
        sx, sy, sz = size
        dims = {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy),
                'up': (sx, sz), 'down': (sx, sz)}
        uv = {}
        for face, (fw, fh) in dims.items():
            if faces is not None and face not in faces:
                continue
            if strip is not None and face in ('north', 'south'):
                u0, v0, u1, v1 = strip
                uv[face] = {'uv': [round(u0, 3), round(v0, 3)], 'uv_size': [round(u1 - u0, 3), round(v1 - v0, 3)]}
                continue
            uv[face] = self._patch(mat, fw, fh)
        c['uv'] = uv
        self.by_name[bone]['cubes'].append(c)
        self.cube_count += 1
        return c

    def _patch(self, mat, fw, fh):
        w = max(0.5, fw * mat.scale)
        h = max(0.5, fh * mat.scale)
        w = min(w, mat.w)
        h = min(h, mat.h)
        u = mat.x + self.rng.uniform(0, max(0.0, mat.w - w))
        v = mat.y + self.rng.uniform(0, max(0.0, mat.h - h))
        return {'uv': [round(u, 3), round(v, 3)], 'uv_size': [round(w, 3), round(h, 3)]}

    def bar(self, bone, a, b, width, thickness, mat, z=0.0, overlap=0.0, strip=None, lift=0.0):
        """A box lying in the XY plane from point a to point b (Bedrock units), `width` across,
        `thickness` along Z centred on `z`. Rotation about Z only, which is handedness-safe."""
        ax, ay = a
        bx, by = b
        dx, dy = bx - ax, by - ay
        length = math.hypot(dx, dy) + overlap
        ang = math.degrees(math.atan2(dy, dx))
        cx, cy = (ax + bx) / 2, (ay + by) / 2
        origin = [cx - length / 2, cy - width / 2, z - thickness / 2 + lift]
        size = [length, width, thickness]
        # The loader mirrors X but keeps a cube's Z rotation, so a box meant to lie at `ang` in
        # Bedrock XY must be written at -ang to come out along the mirrored line.
        return self.cube(bone, origin, size, mat, rotation=[0, 0, -ang], pivot=[cx, cy, z + lift], strip=strip)

    def to_json(self, identifier='geometry.arcana'):
        geo = {
            'description': {'identifier': identifier, 'texture_width': self.tex_w,
                            'texture_height': self.tex_h, 'visible_bounds_width': 8,
                            'visible_bounds_height': 6, 'visible_bounds_offset': [0, 0, 0]},
            'bones': self.bones,
        }
        return {'format_version': '1.12.0', 'minecraft:geometry': [geo]}


# ---- the Java conventions, for solving and checking poses ----------------------------------------

def T(x, y, z):
    m = np.eye(4); m[0, 3], m[1, 3], m[2, 3] = x, y, z; return m


def S(x, y, z):
    m = np.eye(4); m[0, 0], m[1, 1], m[2, 2] = x, y, z; return m


def Rx(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a); m = np.eye(4)
    m[1, 1] = c; m[1, 2] = -s; m[2, 1] = s; m[2, 2] = c; return m


def Ry(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a); m = np.eye(4)
    m[0, 0] = c; m[0, 2] = s; m[2, 0] = -s; m[2, 2] = c; return m


def Rz(d):
    a = math.radians(d); c, s = math.cos(a), math.sin(a); m = np.eye(4)
    m[0, 0] = c; m[0, 1] = -s; m[1, 0] = s; m[1, 1] = c; return m


def bone_local(pivot, rest, anim_pos=(0, 0, 0), anim_rot=(0, 0, 0), anim_scale=(1, 1, 1)):
    """One bone's local matrix in Java space, exactly as GeoModel.computeGlobals builds it."""
    pv = (-pivot[0], pivot[1], pivot[2])
    rr = (-rest[0], -rest[1], rest[2])
    m = T(-anim_pos[0], anim_pos[1], anim_pos[2]) @ T(*pv)
    m = m @ Rz(rr[2] + anim_rot[2]) @ Ry(rr[1] - anim_rot[1]) @ Rx(rr[0] - anim_rot[0])
    m = m @ S(*anim_scale) @ T(-pv[0], -pv[1], -pv[2])
    return m


def to_java(p):
    return np.array([-p[0], p[1], p[2], 1.0])


def eye_space(chain, point):
    """Eye-space position in blocks of a Bedrock point under a list of bone matrices, outermost
    first (the view is scale(1/16) with idle_view at the origin)."""
    m = np.eye(4)
    for b in chain:
        m = m @ b
    q = m @ to_java(point)
    return q[:3] / 16.0


def solve_rest(pivot, want_dir, want_normal, local_dir=(0, 1, 0), local_normal=(0, 0, 1)):
    """Finds a bone's rest rotation (Bedrock degrees) that turns `local_dir` (in Bedrock axes) onto
    `want_dir` and `local_normal` toward `want_normal` (both in eye space)."""
    want_dir = np.array(want_dir, float); want_dir /= np.linalg.norm(want_dir)
    want_normal = np.array(want_normal, float); want_normal /= np.linalg.norm(want_normal)
    ld = to_java(local_dir)[:3]
    ln = to_java(local_normal)[:3]

    def err(r):
        m = bone_local((0, 0, 0), r)[:3, :3]
        d = m @ ld
        n = m @ ln
        return np.sum((d - want_dir) ** 2) * 4 + np.sum((n - want_normal) ** 2)

    best, best_e = None, 1e9
    rng = np.random.default_rng(3)
    for _ in range(400):
        r = rng.uniform(-180, 180, 3)
        step = 40.0
        e = err(r)
        while step > 0.01:
            improved = False
            for i in range(3):
                for sgn in (-1, 1):
                    t = r.copy(); t[i] += sgn * step
                    et = err(t)
                    if et < e:
                        r, e, improved = t, et, True
            if not improved:
                step *= 0.5
        if e < best_e:
            best, best_e = r, e
    return [float(((v + 180) % 360) - 180) for v in best], best_e


def write_json(path, data):
    def rnd(v):
        if isinstance(v, float):
            r = round(v, 4)
            return int(r) if r == int(r) else r
        if isinstance(v, list):
            return [rnd(x) for x in v]
        if isinstance(v, dict):
            return {k: rnd(x) for k, x in v.items()}
        return v
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(rnd(data), f, ensure_ascii=False, separators=(',', ':'))
