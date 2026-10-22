"""A small procedural mesh library, and the binary format the mod reads.

The Arcana weapons were boxes, which is why they looked like toys: a box has no curve in it and no
edge that is not a right angle. Everything here exists to build the shapes the concept art actually
has - tendrils that curl, shafts braided out of three strands, feather blades, rose petals, a bird
skull, a flame-shaped blade - as triangles with smooth normals.

Spaces. Meshes are built in the weapon's own space: the grip at the origin, the weapon running up
+Y, +X to its right and +Z forward, in Bedrock model units (sixteen to a block). That is the space
`rig.at()` places into, so a part built here drops straight onto a bone.

Texturing. A part carries a material name rather than coordinates. Its UVs are built in its own
unit square, and `atlas.pack()` later squeezes every material into one texture and rewrites them,
so a part can be moved between materials without touching its geometry.
"""
import math
import struct

import numpy as np


def v3(x, y, z):
    return np.array([x, y, z], dtype=float)


def norm(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else np.array([0.0, 1.0, 0.0])


class Mesh:
    """Triangles with positions, UVs and normals, all in the weapon's space."""

    def __init__(self, material='steel'):
        self.pos = []
        self.uv = []
        self.nrm = []
        self.tris = []
        self.material = material

    # ---- building ------------------------------------------------------------------------
    def vert(self, p, u, v, n=None):
        self.pos.append(np.asarray(p, dtype=float))
        self.uv.append((float(u), float(v)))
        self.nrm.append(None if n is None else norm(np.asarray(n, dtype=float)))
        return len(self.pos) - 1

    def tri(self, a, b, c):
        if a != b and b != c and a != c:
            self.tris.append((a, b, c))

    def quad(self, a, b, c, d):
        self.tri(a, b, c)
        self.tri(a, c, d)

    def grid(self, rows, cols, closed_v=False):
        """Indices for a (rows x cols) lattice of vertices added row-major."""
        for i in range(rows - 1):
            for j in range(cols - 1 if not closed_v else cols):
                j2 = (j + 1) % cols
                a = i * cols + j
                b = i * cols + j2
                c = (i + 1) * cols + j2
                d = (i + 1) * cols + j
                self.quad(a, b, c, d)

    # ---- transforms ----------------------------------------------------------------------
    def transform(self, m):
        """Applies a 4x4 (or 3x3) matrix to positions, and its inverse-transpose to normals."""
        m = np.asarray(m, dtype=float)
        if m.shape == (3, 3):
            big = np.eye(4)
            big[:3, :3] = m
            m = big
        rot = m[:3, :3]
        inv_t = np.linalg.inv(rot).T
        for i, p in enumerate(self.pos):
            self.pos[i] = (m @ np.append(p, 1.0))[:3]
        for i, n in enumerate(self.nrm):
            if n is not None:
                self.nrm[i] = norm(inv_t @ n)
        return self

    def translate(self, x, y=0.0, z=0.0):
        d = np.asarray(x, dtype=float) if np.ndim(x) else v3(x, y, z)
        for i, p in enumerate(self.pos):
            self.pos[i] = p + d
        return self

    def scale(self, sx, sy=None, sz=None):
        if sy is None:
            sy = sz = sx
        return self.transform(np.diag([sx, sy, sz]))

    def rotate(self, axis, degrees):
        a = norm(np.asarray(axis, dtype=float))
        t = math.radians(degrees)
        c, s = math.cos(t), math.sin(t)
        x, y, z = a
        r = np.array([
            [c + x * x * (1 - c), x * y * (1 - c) - z * s, x * z * (1 - c) + y * s],
            [y * x * (1 - c) + z * s, c + y * y * (1 - c), y * z * (1 - c) - x * s],
            [z * x * (1 - c) - y * s, z * y * (1 - c) + x * s, c + z * z * (1 - c)],
        ])
        return self.transform(r)

    def mirror_x(self):
        """Mirrors in X and flips the winding so the copy still faces outward."""
        out = self.copy()
        for i, p in enumerate(out.pos):
            out.pos[i] = v3(-p[0], p[1], p[2])
        for i, n in enumerate(out.nrm):
            if n is not None:
                out.nrm[i] = v3(-n[0], n[1], n[2])
        out.tris = [(c, b, a) for a, b, c in out.tris]
        return out

    def copy(self):
        out = Mesh(self.material)
        out.pos = [p.copy() for p in self.pos]
        out.uv = list(self.uv)
        out.nrm = [None if n is None else n.copy() for n in self.nrm]
        out.tris = list(self.tris)
        return out

    def merge(self, other):
        base = len(self.pos)
        self.pos.extend(p.copy() for p in other.pos)
        self.uv.extend(other.uv)
        self.nrm.extend(None if n is None else n.copy() for n in other.nrm)
        self.tris.extend((a + base, b + base, c + base) for a, b, c in other.tris)
        return self

    # ---- normals -------------------------------------------------------------------------
    def smooth(self, weld=0.02):
        """Averages normals over welded positions: what turns facets into a surface."""
        keys = {}
        groups = []
        for i, p in enumerate(self.pos):
            key = (round(p[0] / weld), round(p[1] / weld), round(p[2] / weld))
            groups.append(keys.setdefault(key, len(keys)))
        acc = np.zeros((len(keys), 3))
        for a, b, c in self.tris:
            n = np.cross(self.pos[b] - self.pos[a], self.pos[c] - self.pos[a])
            for i in (a, b, c):
                acc[groups[i]] += n
        for i in range(len(self.pos)):
            n = acc[groups[i]]
            if np.linalg.norm(n) > 1e-9:
                self.nrm[i] = norm(n)
            elif self.nrm[i] is None:
                self.nrm[i] = v3(0, 1, 0)
        return self

    def flat(self):
        """Per-face normals, for anything that should read as cut or faceted - gems, plates."""
        pos, uv, tris = [], [], []
        nrm = []
        for a, b, c in self.tris:
            n = np.cross(self.pos[b] - self.pos[a], self.pos[c] - self.pos[a])
            n = norm(n)
            base = len(pos)
            for i in (a, b, c):
                pos.append(self.pos[i].copy())
                uv.append(self.uv[i])
                nrm.append(n.copy())
            tris.append((base, base + 1, base + 2))
        self.pos, self.uv, self.nrm, self.tris = pos, uv, nrm, tris
        return self

    def bounds(self):
        if not self.pos:
            return None
        a = np.array(self.pos)
        return a.min(axis=0), a.max(axis=0)

    def __len__(self):
        return len(self.tris)


# ---- paths --------------------------------------------------------------------------------

def catmull(points, samples=48, closed=False):
    """A smooth curve through the given points, which is how every tendril and horn is drawn."""
    p = [np.asarray(q, dtype=float) for q in points]
    if closed:
        p = [p[-1]] + p + [p[0], p[1]]
    else:
        p = [p[0] + (p[0] - p[1])] + p + [p[-1] + (p[-1] - p[-2])]
    out = []
    spans = len(p) - 3
    for s in range(spans):
        p0, p1, p2, p3 = p[s], p[s + 1], p[s + 2], p[s + 3]
        n = max(2, samples // spans)
        for i in range(n):
            t = i / n
            t2, t3 = t * t, t * t * t
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t
                              + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * t3))
    out.append(p[-2])
    return out


def helix(turns, height, radius, samples=80, phase=0.0, radius_end=None):
    """A spiral up +Y: one strand of a braided shaft, or a curl of filigree."""
    r1 = radius if radius_end is None else radius_end
    out = []
    for i in range(samples + 1):
        t = i / samples
        a = phase + turns * math.tau * t
        r = radius + (r1 - radius) * t
        out.append(v3(math.cos(a) * r, height * t, math.sin(a) * r))
    return out


def frames(path):
    """Parallel-transport frames along a path: a sweep that does not twist on its own."""
    pts = [np.asarray(p, dtype=float) for p in path]
    tangents = []
    for i in range(len(pts)):
        if i == 0:
            t = pts[1] - pts[0]
        elif i == len(pts) - 1:
            t = pts[-1] - pts[-2]
        else:
            t = pts[i + 1] - pts[i - 1]
        tangents.append(norm(t))
    up = v3(0, 0, 1)
    if abs(np.dot(tangents[0], up)) > 0.9:
        up = v3(1, 0, 0)
    normals = [norm(np.cross(up, tangents[0]))]
    for i in range(1, len(pts)):
        n = normals[-1] - tangents[i] * np.dot(normals[-1], tangents[i])
        if np.linalg.norm(n) < 1e-6:
            n = np.cross(tangents[i], v3(0, 0, 1))
        normals.append(norm(n))
    binormals = [norm(np.cross(tangents[i], normals[i])) for i in range(len(pts))]
    return pts, tangents, normals, binormals


# ---- primitives ---------------------------------------------------------------------------

def tube(path, radius, sides=10, material='steel', twist=0.0, caps=True, squash=1.0, uv_scale=1.0):
    """A tube swept along a path. `radius` is a number or one per station."""
    pts, tan, nor, bin_ = frames(path)
    n = len(pts)
    radii = radius if hasattr(radius, '__len__') else [radius] * n
    if len(radii) != n:
        # Resample a short radius list across the path, so callers can give a profile.
        src = list(radii)
        radii = [src[min(len(src) - 1, int(i / (n - 1) * (len(src) - 1)))] for i in range(n)]
    m = Mesh(material)
    for i in range(n):
        for j in range(sides):
            a = j / sides * math.tau + twist * (i / max(1, n - 1)) * math.tau
            offset = nor[i] * math.cos(a) * radii[i] + bin_[i] * math.sin(a) * radii[i] * squash
            p = pts[i] + offset
            m.vert(p, j / sides * uv_scale, i / (n - 1), offset)
    m.grid(n, sides, closed_v=True)
    if caps:
        for (station, flip) in ((0, True), (n - 1, False)):
            centre = m.vert(pts[station], 0.5, 0.0 if flip else 1.0,
                            -tan[station] if flip else tan[station])
            ring = [station * sides + j for j in range(sides)]
            for j in range(sides):
                a, b = ring[j], ring[(j + 1) % sides]
                m.tri(centre, b, a) if flip else m.tri(centre, a, b)
    return m


def lathe(profile, segments=20, material='steel', closed_top=False, closed_bottom=False):
    """Revolves a 2D profile of (radius, y) about +Y: orbs, collars, gem settings, beads."""
    m = Mesh(material)
    rows = len(profile)
    for i, (r, y) in enumerate(profile):
        for j in range(segments):
            a = j / segments * math.tau
            m.vert(v3(math.cos(a) * r, y, math.sin(a) * r), j / segments, i / max(1, rows - 1))
    m.grid(rows, segments, closed_v=True)
    if closed_bottom:
        c = m.vert(v3(0, profile[0][1], 0), 0.5, 0)
        for j in range(segments):
            m.tri(c, (j + 1) % segments, j)
    if closed_top:
        top = (rows - 1) * segments
        c = m.vert(v3(0, profile[-1][1], 0), 0.5, 1)
        for j in range(segments):
            m.tri(c, top + j, top + (j + 1) % segments)
    return m


def sphere(radius, segments=16, rings=12, material='steel'):
    profile = []
    for i in range(rings + 1):
        t = i / rings * math.pi
        profile.append((math.sin(t) * radius, -math.cos(t) * radius))
    return lathe(profile, segments, material)


def ellipsoid(rx, ry, rz, segments=16, rings=12, material='steel'):
    m = sphere(1.0, segments, rings, material)
    return m.scale(rx, ry, rz)


def torus(radius, thickness, segments=24, sides=10, material='steel'):
    path = [v3(math.cos(i / segments * math.tau) * radius, 0,
               math.sin(i / segments * math.tau) * radius) for i in range(segments + 1)]
    return tube(path, thickness, sides, material, caps=False)


def blade(outline, thickness, material='steel', bevel=0.35, taper=1.0):
    """A flat shape given as (x, y) points, given a thickness and a bevelled edge.

    Used for everything plate-like: sword blades, feather blades, the crescent's ribs, the star
    head. The bevel is what keeps an edge from reading as a slab - a ridge down the middle and a
    chamfer out to the rim.
    """
    m = Mesh(material)
    pts = [np.asarray(p, dtype=float) for p in outline]
    n = len(pts)
    centre = sum(pts) / n
    span = max(np.linalg.norm(p - centre) for p in pts) or 1.0

    def uvof(p):
        return (0.5 + (p[0] - centre[0]) / (2 * span), 0.5 + (p[1] - centre[1]) / (2 * span))

    for side in (1, -1):
        ridge = []
        rim = []
        for p in pts:
            d = p - centre
            inner = centre + d * bevel
            u, v = uvof(p)
            ridge.append(m.vert(v3(inner[0], inner[1], side * thickness * taper), u, v, v3(0, 0, side)))
            rim.append(m.vert(v3(p[0], p[1], 0.0), *uvof(p), n=v3(d[0], d[1], side * 0.6)))
        hub = m.vert(v3(centre[0], centre[1], side * thickness * taper), 0.5, 0.5, v3(0, 0, side))
        for i in range(n):
            j = (i + 1) % n
            if side > 0:
                m.tri(hub, ridge[i], ridge[j])
                m.quad(ridge[i], rim[i], rim[j], ridge[j])
            else:
                m.tri(hub, ridge[j], ridge[i])
                m.quad(ridge[j], rim[j], rim[i], ridge[i])
    return m


def gem(radius, height, facets=8, material='gem', table=0.45):
    """A cut stone: a flat table, a crown of facets and a point below. Always drawn faceted."""
    m = Mesh(material)
    top = []
    girdle = []
    for j in range(facets):
        a = j / facets * math.tau
        top.append(m.vert(v3(math.cos(a) * radius * table, height * 0.45, math.sin(a) * radius * table),
                          0.5 + 0.2 * math.cos(a), 0.5 + 0.2 * math.sin(a)))
    for j in range(facets):
        a = (j + 0.5) / facets * math.tau
        girdle.append(m.vert(v3(math.cos(a) * radius, 0.0, math.sin(a) * radius),
                             0.5 + 0.5 * math.cos(a), 0.5 + 0.5 * math.sin(a)))
    centre = m.vert(v3(0, height * 0.45, 0), 0.5, 0.5)
    tip = m.vert(v3(0, -height * 0.55, 0), 0.5, 0.0)
    for j in range(facets):
        m.tri(centre, top[j], top[(j + 1) % facets])
        m.tri(top[j], girdle[j], top[(j + 1) % facets])
        m.tri(girdle[j], girdle[(j + 1) % facets], top[(j + 1) % facets])
        m.tri(tip, girdle[(j + 1) % facets], girdle[j])
    return m.flat()


def petal(length, width, curl=0.6, samples=10, material='petal', thickness=0.06, cols=7):
    """A curved shell: rose petals, feather vanes, the claw's leaves."""
    m = Mesh(material)
    rows = samples
    for i in range(rows):
        t = i / (rows - 1)
        y = t * length
        half = width * math.sin(math.pi * min(1.0, t * 1.15)) * 0.5
        for j in range(cols):
            s = j / (cols - 1) * 2 - 1
            x = s * half
            # Curls away from the stem, more at the tip.
            z = curl * (s * s) * length * 0.25 + curl * t * t * length * 0.18
            m.vert(v3(x, y, z), (s + 1) * 0.5, t, v3(0, 0, 1))
    m.grid(rows, cols)
    back = Mesh(material)
    for i, p in enumerate(m.pos):
        back.vert(p - v3(0, 0, thickness), m.uv[i][0], m.uv[i][1], v3(0, 0, -1))
    back.tris = [(c, b, a) for a, b, c in m.tris]
    return m.merge(back)


def feather(length, width, material='feather', curve=0.30, cup=0.22, thickness=0.05,
            rows=12, cols=7, rib='steel_dark', rib_radius=0.07):
    """A feather: a cupped vane that tapers to a point, with a rib down the middle.

    Grows along +Y from its quill. The vane is cupped about the rib and the whole thing sweeps in
    +Z as it rises, which is what stops a row of them reading as a row of leaves. Returns the vane
    and, when a rib material is given, the quill to go with it.
    """
    m = Mesh(material)
    for i in range(rows):
        t = i / (rows - 1)
        y = t * length
        # Narrow at the quill, widest a third of the way up, drawn to a point at the tip.
        half = width * 0.5 * math.sin(math.pi * min(1.0, 0.12 + t * 0.92)) ** 0.75
        sweep = curve * length * t * t
        for j in range(cols):
            sgn = j / (cols - 1) * 2 - 1
            x = sgn * half
            z = sweep + cup * half * (sgn * sgn) + (0.0 if abs(sgn) > 0.08 else rib_radius * 0.5)
            m.vert(v3(x, y, z), (sgn + 1) * 0.5, t, v3(0, 0, 1))
    m.grid(rows, cols)
    back = Mesh(material)
    for i, p in enumerate(m.pos):
        back.vert(p - v3(0, 0, thickness), m.uv[i][0], m.uv[i][1], v3(0, 0, -1))
    back.tris = [(c, b, a) for a, b, c in m.tris]
    m.merge(back)
    if rib is None:
        return m
    path = [v3(0, t / 7 * length, curve * length * (t / 7) ** 2) for t in range(8)]
    quill = tube(path, [rib_radius * (1.6 - 1.4 * (k / 7)) for k in range(8)], 5, rib).smooth()
    return m.merge(quill)


def rose(radius, layers=4, per=5, material='petal', heart='petal_dark'):
    """A rose: rings of petals, each ring smaller, tighter and more upright than the one outside."""
    m = Mesh(material)
    for ring in range(layers):
        f = ring / max(1, layers - 1)
        r = radius * (1.0 - 0.62 * f)
        lean = 78 - 62 * f          # outer petals lie open, inner ones stand up
        curl = 0.45 + 0.75 * f
        for k in range(per + ring):
            a = (k / (per + ring)) * 360 + ring * 37
            leaf = petal(r, r * 1.25, curl, 6, material, thickness=0.045, cols=5)
            leaf.rotate(v3(1, 0, 0), lean)
            leaf.rotate(v3(0, 1, 0), a)
            leaf.translate(0, f * radius * 0.30, 0)
            m.merge(leaf)
    bud = sphere(radius * 0.17, 8, 6, heart).smooth()
    bud.translate(0, radius * 0.34, 0)
    return m.merge(bud)


def bird_skull(length, material='bone', socket='bone_dark'):
    """A corvid skull: a domed cranium, a long tapering beak, and two deep sockets."""
    m = ellipsoid(length * 0.26, length * 0.23, length * 0.30, 16, 12, material).smooth()
    # The beak: a cone that flattens as it goes, with the curve of a crow's bill.
    path = catmull([v3(0, 0.02 * length, -length * 0.22),
                    v3(0, 0.00 * length, -length * 0.52),
                    v3(0, -0.07 * length, -length * 0.82)], 14)
    bill = tube(path, [length * 0.17, length * 0.10, length * 0.015], 8, material, squash=0.62)
    m.merge(bill.smooth())
    lower = tube(catmull([v3(0, -0.07 * length, -length * 0.20),
                          v3(0, -0.09 * length, -length * 0.46),
                          v3(0, -0.10 * length, -length * 0.70)], 12),
                 [length * 0.10, length * 0.065, length * 0.012], 7, material, squash=0.55)
    m.merge(lower.smooth())
    for side in (1, -1):
        socket_ring = torus(length * 0.115, length * 0.028, 16, 7, material)
        socket_ring.rotate(v3(1, 0, 0), 90)
        socket_ring.rotate(v3(0, 1, 0), side * 46)
        socket_ring.translate(side * length * 0.17, length * 0.05, -length * 0.14)
        m.merge(socket_ring.smooth())
        hollow = ellipsoid(length * 0.10, length * 0.10, length * 0.07, 12, 9, socket).smooth()
        hollow.rotate(v3(0, 1, 0), side * 46)
        hollow.translate(side * length * 0.17, length * 0.05, -length * 0.13)
        m.merge(hollow)
    return m


def chain(path, links=8, radius=0.42, thickness=0.11, material='steel_dark'):
    """A run of links along a path, every other one turned a quarter so they interlock."""
    pts = [np.asarray(p, dtype=float) for p in catmull(path, max(links * 2, 16))]
    _, tan, nor, _ = frames(pts)
    m = Mesh(material)
    for k in range(links):
        i = int(k / max(1, links - 1) * (len(pts) - 1))
        link = torus(radius, thickness, 10, 5, material)
        link.scale(1.0, 1.0, 0.62)              # links are ovals, not rings
        link.rotate(v3(1, 0, 0), 90)
        if k % 2:
            link.rotate(v3(0, 0, 1), 90)
        # Stand it up on the path's tangent.
        axis = np.cross(v3(0, 0, 1), tan[i])
        if np.linalg.norm(axis) > 1e-6:
            angle = math.degrees(math.acos(max(-1.0, min(1.0, float(np.dot(v3(0, 0, 1), tan[i]))))))
            link.rotate(axis, angle)
        link.translate(pts[i])
        m.merge(link.smooth())
    return m


def ribbon(path, width, material='cloth', thickness=0.03, twist=1.2, taper=0.35):
    """A flat strip swept along a path, twisting as it goes: tassels, sashes, trailing cloth."""
    pts, tan, nor, bin_ = frames(path)
    n = len(pts)
    cols = 5
    m = Mesh(material)
    for i in range(n):
        t = i / (n - 1)
        w = width * (1.0 - taper * t)
        a = twist * t * math.tau
        across = nor[i] * math.cos(a) + bin_[i] * math.sin(a)
        for j in range(cols):
            s = j / (cols - 1) * 2 - 1
            m.vert(pts[i] + across * s * w * 0.5, (s + 1) * 0.5, t, bin_[i])
    m.grid(n, cols)
    back = Mesh(material)
    for i, p in enumerate(m.pos):
        back.vert(p, m.uv[i][0], m.uv[i][1], -m.nrm[i] if m.nrm[i] is not None else None)
    back.tris = [(c, b, a) for a, b, c in m.tris]
    for i in range(len(back.pos)):
        back.pos[i] = back.pos[i] - (m.nrm[i] if m.nrm[i] is not None else v3(0, 0, 1)) * thickness
    return m.merge(back)


def spiral2d(turns, r0, r1, samples=40, plane=('x', 'y')):
    """A flat spiral in one plane: the curl a piece of filigree is drawn as."""
    out = []
    for i in range(samples + 1):
        t = i / samples
        a = t * turns * math.tau
        r = r0 + (r1 - r0) * t
        c, s = math.cos(a) * r, math.sin(a) * r
        p = {'x': 0.0, 'y': 0.0, 'z': 0.0}
        p[plane[0]] = c
        p[plane[1]] = s
        out.append(v3(p['x'], p['y'], p['z']))
    return out


def scroll(turns, r0, r1, thickness, material='steel', samples=36, plane=('x', 'y'), sides=6):
    """A tapering curl of filigree: a spiral swept with a tube that thins as it winds in."""
    path = spiral2d(turns, r0, r1, samples, plane)
    radii = [thickness * (1.0 - 0.72 * (i / samples)) for i in range(samples + 1)]
    return tube(path, radii, sides, material).smooth()


def disc(radius, segments=18, material='steel', y=0.0):
    m = Mesh(material)
    c = m.vert(v3(0, y, 0), 0.5, 0.5, v3(0, 1, 0))
    ring = []
    for j in range(segments):
        a = j / segments * math.tau
        ring.append(m.vert(v3(math.cos(a) * radius, y, math.sin(a) * radius),
                           0.5 + 0.5 * math.cos(a), 0.5 + 0.5 * math.sin(a), v3(0, 1, 0)))
    for j in range(segments):
        m.tri(c, ring[j], ring[(j + 1) % segments])
    return m


def eyeball(radius, material='sclera', iris_material='iris', look=(0, 0, 1), iris=0.55):
    """A sphere with an iris set into it, used by the dozen eyes on the staff."""
    ball = sphere(radius, 14, 10, material).smooth()
    d = norm(np.asarray(look, dtype=float))
    plate = disc(radius * iris, 14, iris_material)
    # Stand the iris up on the look direction and lift it just off the sphere.
    up = v3(0, 1, 0)
    axis = np.cross(up, d)
    if np.linalg.norm(axis) > 1e-6:
        angle = math.degrees(math.acos(max(-1.0, min(1.0, np.dot(up, d)))))
        plate.rotate(axis, angle)
    plate.translate(d * radius * 0.86)
    return ball, plate


# ---- the atlas ------------------------------------------------------------------------------

class Atlas:
    """Packs each material into a row of one texture and rewrites the meshes' UVs to match."""

    def __init__(self, size=256):
        self.size = size
        self.rects = {}

    def pack(self, materials):
        """Lays materials out in a grid and records each one's rectangle in UV space."""
        n = len(materials)
        cols = 4
        rows = (n + cols - 1) // cols
        w = 1.0 / cols
        h = 1.0 / rows
        for i, name in enumerate(materials):
            self.rects[name] = ((i % cols) * w, (i // cols) * h, w, h)
        return self.rects

    def apply(self, mesh):
        """Squeezes a mesh's own 0..1 UVs into its material's rectangle, with a small inset."""
        rect = self.rects.get(mesh.material)
        if rect is None:
            return mesh
        x, y, w, h = rect
        pad = 0.06
        for i, (u, v) in enumerate(mesh.uv):
            u = min(1.0, max(0.0, u))
            v = min(1.0, max(0.0, v))
            mesh.uv[i] = (x + w * (pad + u * (1 - 2 * pad)), y + h * (pad + v * (1 - 2 * pad)))
        return mesh


# ---- export ---------------------------------------------------------------------------------

SCALE = 1.0 / 16.0   # stored units per model unit: a sixteenth, which is well under visible


def write_mesh(path, groups):
    """Writes {bone: [Mesh, ...]} as the binary the mod reads."""
    out = bytearray()
    out += b'SMSH'
    out += struct.pack('>H', 1)
    out += struct.pack('>f', SCALE)
    named = [(name, meshes) for name, meshes in groups.items() if meshes]
    out += struct.pack('>H', len(named))
    total = 0
    for name, meshes in named:
        merged = Mesh()
        for m in meshes:
            merged.merge(m)
        for i, n in enumerate(merged.nrm):
            if n is None:
                merged.nrm[i] = v3(0, 1, 0)
        verts, indices, seen = [], [], {}
        for tri in merged.tris:
            for i in tri:
                p = merged.pos[i]
                u, v = merged.uv[i]
                nv = merged.nrm[i]
                key = (round(p[0] / SCALE), round(p[1] / SCALE), round(p[2] / SCALE),
                       round(u * 65535), round(v * 65535),
                       round(nv[0] * 127), round(nv[1] * 127), round(nv[2] * 127))
                idx = seen.get(key)
                if idx is None:
                    idx = len(verts)
                    seen[key] = idx
                    verts.append(key)
                indices.append(idx)
        if len(verts) > 65535:
            raise ValueError('%s has %d vertices; split the bone' % (name, len(verts)))
        encoded = name.encode('utf-8')
        out += struct.pack('>H', len(encoded)) + encoded
        out += struct.pack('>H', len(verts))
        for key in verts:
            out += struct.pack('>hhhHHbbbb',
                               clamp16(key[0]), clamp16(key[1]), clamp16(key[2]),
                               key[3] & 0xFFFF, key[4] & 0xFFFF,
                               clamp8(key[5]), clamp8(key[6]), clamp8(key[7]), 0)
        out += struct.pack('>HH', len(indices) & 0xFFFF, len(indices) >> 16)
        for i in indices:
            out += struct.pack('>H', i)
        total += len(indices) // 3
    with open(path, 'wb') as f:
        f.write(bytes(out))
    return total


def clamp16(v):
    return max(-32768, min(32767, int(v)))


def clamp8(v):
    return max(-127, min(127, int(v)))
