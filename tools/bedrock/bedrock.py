"""Offline Bedrock geo/animation rasteriser.

Mirrors exactly the maths the Java renderer will use, so the convention (axis signs,
rotation order, pivot handling) can be checked by eye before it ships.
"""
import json
import math

import numpy as np


# ---- maths ---------------------------------------------------------------------------

def ident():
    return np.eye(4)


def translate(x, y, z):
    m = np.eye(4)
    m[0, 3], m[1, 3], m[2, 3] = x, y, z
    return m


def scale(x, y, z):
    m = np.eye(4)
    m[0, 0], m[1, 1], m[2, 2] = x, y, z
    return m


def rot_x(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[1, 1], m[1, 2], m[2, 1], m[2, 2] = c, -s, s, c
    return m


def rot_y(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[0, 0], m[0, 2], m[2, 0], m[2, 2] = c, s, -s, c
    return m


def rot_z(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[0, 0], m[0, 1], m[1, 0], m[1, 1] = c, -s, s, c
    return m


def rotate(rx, ry, rz, order="zyx", sign=(1, 1, 1)):
    rx, ry, rz = rx * sign[0], ry * sign[1], rz * sign[2]
    parts = {"x": rot_x(rx), "y": rot_y(ry), "z": rot_z(rz)}
    m = np.eye(4)
    for axis in order:
        m = m @ parts[axis]
    return m


# ---- geo -----------------------------------------------------------------------------

class Cube:
    def __init__(self, raw):
        self.origin = [float(v) for v in raw["origin"]]
        self.size = [float(v) for v in raw["size"]]
        self.pivot = [float(v) for v in raw.get("pivot", [0, 0, 0])]
        self.rotation = [float(v) for v in raw.get("rotation", [0, 0, 0])]
        self.inflate = float(raw.get("inflate", 0.0))
        self.uv = raw.get("uv")


class Bone:
    def __init__(self, raw):
        self.name = raw["name"]
        self.parent = raw.get("parent")
        self.pivot = [float(v) for v in raw.get("pivot", [0, 0, 0])]
        self.rotation = [float(v) for v in raw.get("rotation", [0, 0, 0])]
        self.cubes = [Cube(c) for c in raw.get("cubes", [])]


class Geo:
    def __init__(self, path):
        d = json.load(open(path))
        g = d["minecraft:geometry"][0]
        desc = g["description"]
        self.tex_w = float(desc.get("texture_width", 16))
        self.tex_h = float(desc.get("texture_height", 16))
        self.bones = [Bone(b) for b in g["bones"]]
        self.by_name = {b.name: b for b in self.bones}


# ---- animation -----------------------------------------------------------------------

def kf_value(kf, which):
    """A keyframe is a vector, or {pre, post, lerp_mode}, or a bare number."""
    if isinstance(kf, dict):
        v = kf.get(which) or kf.get("post") or kf.get("pre")
        return kf_value(v, which)
    if isinstance(kf, (int, float)):
        return [float(kf)] * 3
    return [float(x) if not isinstance(x, str) else 0.0 for x in kf]


class Channel:
    def __init__(self, raw, default):
        self.default = default
        self.times = []
        self.pre = []
        self.post = []
        self.smooth = []
        if isinstance(raw, dict):
            for t in sorted(raw.keys(), key=lambda s: float(s)):
                kf = raw[t]
                self.times.append(float(t))
                self.pre.append(kf_value(kf, "pre"))
                self.post.append(kf_value(kf, "post"))
                self.smooth.append(isinstance(kf, dict) and kf.get("lerp_mode") == "catmullrom")
        else:
            self.times.append(0.0)
            v = kf_value(raw, "post")
            self.pre.append(v)
            self.post.append(v)
            self.smooth.append(False)

    def at(self, time):
        if not self.times:
            return self.default
        if time <= self.times[0]:
            return self.post[0]
        if time >= self.times[-1]:
            return self.pre[-1]
        i = 0
        while i + 1 < len(self.times) and self.times[i + 1] <= time:
            i += 1
        t0, t1 = self.times[i], self.times[i + 1]
        a, b = self.post[i], self.pre[i + 1]
        f = 0.0 if t1 <= t0 else (time - t0) / (t1 - t0)
        if self.smooth[i] or self.smooth[i + 1]:
            p0 = self.post[i - 1] if i > 0 else a
            p3 = self.pre[i + 2] if i + 2 < len(self.times) else b
            return [catmullrom(p0[k], a[k], b[k], p3[k], f) for k in range(3)]
        return [a[k] + (b[k] - a[k]) * f for k in range(3)]


def catmullrom(p0, p1, p2, p3, t):
    t2 = t * t
    t3 = t2 * t
    return 0.5 * ((2 * p1) + (-p0 + p2) * t
                  + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                  + (-p0 + 3 * p1 - 3 * p2 + p3) * t3)


class Clip:
    def __init__(self, raw):
        self.length = float(raw.get("animation_length") or 0.0)
        self.loop = bool(raw.get("loop"))
        self.bones = {}
        for name, ch in raw.get("bones", {}).items():
            self.bones[name] = {
                "position": Channel(ch["position"], [0, 0, 0]) if "position" in ch else None,
                "rotation": Channel(ch["rotation"], [0, 0, 0]) if "rotation" in ch else None,
                "scale": Channel(ch["scale"], [1, 1, 1]) if "scale" in ch else None,
            }


def load_clips(path):
    d = json.load(open(path))
    return {k: Clip(v) for k, v in d["animations"].items()}


# ---- posing --------------------------------------------------------------------------

def pose(geo, clip, time, rot_sign=(1, 1, 1), pos_sign=(1, 1, 1), order="zyx"):
    """Returns {bone name: 4x4 model matrix}."""
    out = {}

    def matrix_of(bone):
        if bone.name in out:
            return out[bone.name]
        parent = geo.by_name.get(bone.parent) if bone.parent else None
        m = matrix_of(parent) if parent is not None else ident()

        ch = clip.bones.get(bone.name) if clip else None
        ap = ch["position"].at(time) if ch and ch["position"] else [0, 0, 0]
        ar = ch["rotation"].at(time) if ch and ch["rotation"] else [0, 0, 0]
        asc = ch["scale"].at(time) if ch and ch["scale"] else [1, 1, 1]

        px, py, pz = bone.pivot
        m = m @ translate(ap[0] * pos_sign[0], ap[1] * pos_sign[1], ap[2] * pos_sign[2])
        m = m @ translate(px, py, pz)
        m = m @ rotate(bone.rotation[0] + ar[0], bone.rotation[1] + ar[1],
                       bone.rotation[2] + ar[2], order, rot_sign)
        m = m @ scale(asc[0], asc[1], asc[2])
        m = m @ translate(-px, -py, -pz)
        out[bone.name] = m
        return m

    for b in geo.bones:
        matrix_of(b)
    return out


# Per face, the four model-space corners in texture order: top-left, top-right,
# bottom-right, bottom-left, as seen from outside the cube ("up" is +Y for the side
# faces, -Z for up and +Z for down), plus the outward normal.
#   c = (x0, y0, z0, x1, y1, z1)
FACE_CORNERS = {
    "north": lambda c: [(c[3], c[4], c[2]), (c[0], c[4], c[2]), (c[0], c[1], c[2]), (c[3], c[1], c[2])],
    "south": lambda c: [(c[0], c[4], c[5]), (c[3], c[4], c[5]), (c[3], c[1], c[5]), (c[0], c[1], c[5])],
    "west":  lambda c: [(c[0], c[4], c[5]), (c[0], c[4], c[2]), (c[0], c[1], c[2]), (c[0], c[1], c[5])],
    "east":  lambda c: [(c[3], c[4], c[2]), (c[3], c[4], c[5]), (c[3], c[1], c[5]), (c[3], c[1], c[2])],
    "up":    lambda c: [(c[0], c[4], c[2]), (c[3], c[4], c[2]), (c[3], c[4], c[5]), (c[0], c[4], c[5])],
    "down":  lambda c: [(c[0], c[1], c[5]), (c[3], c[1], c[5]), (c[3], c[1], c[2]), (c[0], c[1], c[2])],
}

FACE_NORMALS = {
    "north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0),
    "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0),
}


def cube_quads(cube, geo):
    """Yields (4 model-space points, 4 uv pairs, normal) per textured face."""
    x0, y0, z0 = cube.origin
    sx, sy, sz = cube.size
    g = cube.inflate
    x0, y0, z0 = x0 - g, y0 - g, z0 - g
    c = (x0, y0, z0, x0 + sx + 2 * g, y0 + sy + 2 * g, z0 + sz + 2 * g)

    local = ident()
    if any(cube.rotation):
        cx, cy, cz = cube.pivot
        # X and Y negated: a cube's rotation is the other handedness from a bone's.
        local = (translate(cx, cy, cz)
                 @ rotate(cube.rotation[0], cube.rotation[1], cube.rotation[2],
                          sign=(-1, -1, 1))
                 @ translate(-cx, -cy, -cz))

    for key, corners in FACE_CORNERS.items():
        face = cube.uv.get(key) if isinstance(cube.uv, dict) else None
        if face is None:
            continue
        u, v = face["uv"]
        du, dv = face["uv_size"]
        # texture order: top-left, top-right, bottom-right, bottom-left
        uvs = [(u, v), (u + du, v), (u + du, v + dv), (u, v + dv)]
        pts = [local @ np.array([p[0], p[1], p[2], 1.0]) for p in corners(c)]
        yield pts, [(a / geo.tex_w, b / geo.tex_h) for a, b in uvs], FACE_NORMALS[key]
