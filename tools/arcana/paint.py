"""Procedural materials for the Arcana weapons.

Each painter fills a rectangle of an RGBA canvas and, where the material emits light, the same
rectangle of a glow mask. The glow mask becomes the weapon's `_s.png`, which the mod's glow pass
uses as its mask: only what is painted bright here lights up in game.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


def periodic_noise(w, h, beta, seed):
    rng = np.random.default_rng(seed)
    n = max(w, h)
    white = rng.standard_normal((n, n))
    f = np.fft.fft2(white)
    fy = np.fft.fftfreq(n)[:, None]
    fx = np.fft.fftfreq(n)[None, :]
    r = np.sqrt(fx * fx + fy * fy)
    r[0, 0] = 1
    out = np.real(np.fft.ifft2(f / r ** (beta / 2)))[:h, :w]
    out -= out.min()
    return out / max(1e-9, out.max())


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def mix(a, b, t):
    a = np.asarray(a, float)
    b = np.asarray(b, float)
    t = np.asarray(t, float)
    if t.ndim == 2:
        t = t[..., None]
    return a + (b - a) * t


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.rgb = np.zeros((h, w, 3), np.float32)
        self.a = np.zeros((h, w), np.float32)
        self.glow = np.zeros((h, w), np.float32)

    def put(self, x, y, rgb, alpha=None, glow=None):
        h, w = rgb.shape[:2]
        self.rgb[y:y + h, x:x + w] = np.clip(rgb, 0, 1)
        self.a[y:y + h, x:x + w] = 1.0 if alpha is None else alpha
        if glow is not None:
            self.glow[y:y + h, x:x + w] = np.clip(glow, 0, 1)

    def save(self, path, glow_path=None):
        rgba = np.dstack([self.rgb, self.a])
        Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), 'RGBA').save(path, optimize=True)
        if glow_path:
            g = (np.clip(self.glow, 0, 1) * 255 + 0.5).astype(np.uint8)
            Image.merge('RGB', [Image.fromarray(g)] * 3).save(glow_path, optimize=True)


def crack_mask(w, h, seed, density=1.0, branchy=0.3):
    """Fine branching fracture lines, as a [0,1] mask, plus a wider glow around them."""
    rng = np.random.default_rng(seed)
    ss = 3
    img = Image.new('L', (w * ss, h * ss), 0)
    d = ImageDraw.Draw(img)

    def branch(x, y, ang, length, width, depth):
        if depth > 4 or length < 3 * ss:
            return
        steps = 5
        for _ in range(steps):
            ang += rng.uniform(-0.45, 0.45)
            nx = x + math.cos(ang) * length / steps
            ny = y + math.sin(ang) * length / steps
            d.line([(x, y), (nx, ny)], fill=255, width=max(1, int(width)))
            x, y = nx, ny
            if rng.random() < branchy:
                branch(x, y, ang + rng.choice([-1, 1]) * rng.uniform(0.6, 1.2), length * 0.55, width * 0.7, depth + 1)

    count = int(max(2, w * h / 900 * density))
    for _ in range(count):
        branch(rng.uniform(0, w * ss), rng.uniform(0, h * ss), rng.uniform(0, math.tau),
               rng.uniform(0.25, 0.6) * max(w, h) * ss, ss * rng.uniform(0.7, 1.4), 0)
    img = img.resize((w, h), Image.LANCZOS)
    line = np.asarray(img).astype(np.float32) / 255
    halo = np.asarray(img.filter(ImageFilter.GaussianBlur(2.2))).astype(np.float32) / 255
    return line, np.clip(halo * 2.2, 0, 1)


# ---- materials ----------------------------------------------------------------------------------

def glass_strip(c, x, y, w, h, pal, seed, cracks=1.0, eyes_band=False):
    """The fractured blade: a gradient of glass along its length, cloudy depth, dark fracture lines
    and the energy showing through them."""
    u = np.linspace(0, 1, w)[None, :] * np.ones((h, 1))
    v = np.linspace(0, 1, h)[:, None] * np.ones((1, w))
    depth = periodic_noise(w, h, 2.4, seed)
    grain = periodic_noise(w, h, 1.3, seed + 1)
    base = mix(pal['glass_a'], pal['glass_b'], np.clip(u * 0.9 + (depth - 0.5) * 0.5, 0, 1))
    # Brighter in the middle of the width, darker toward the edges: a bevel.
    bevel = 1 - np.abs(v - 0.5) * 1.3
    base = base * (0.62 + 0.38 * bevel[..., None]) * (0.85 + 0.3 * grain[..., None])
    line, halo = crack_mask(w, h, seed + 7, density=cracks)
    glow = halo * 0.9 + line * 0.4
    rgb = mix(base, pal['energy'], halo * 0.55)
    rgb = mix(rgb, pal['crack'], line * 0.9)
    # Glints: a few sharp specular highlights, as on broken glass.
    glints = smoothstep(0.93, 0.99, periodic_noise(w, h, 0.9, seed + 9)) * 0.8
    rgb = np.clip(rgb + glints[..., None], 0, 1)
    c.put(x, y, rgb, glow=glow * pal.get('glow', 1.0))


def metal(c, x, y, w, h, tone, seed, streak=True, glow=0.0, edge_dark=True):
    n = periodic_noise(w, h, 1.6, seed)
    if streak:
        s = periodic_noise(w, h, 2.0, seed + 3)
        s = np.repeat(s[:1, :], h, axis=0) * 0.6 + s * 0.4
    else:
        s = n
    shade = 0.75 + 0.35 * s + 0.1 * (n - 0.5)
    rgb = np.asarray(tone, float)[None, None, :] * shade[..., None]
    if edge_dark:
        v = np.linspace(0, 1, h)[:, None]
        rgb *= (0.8 + 0.2 * (1 - np.abs(v - 0.5) * 2))[..., None]
    c.put(x, y, rgb, glow=np.full((h, w), glow, np.float32) if glow else None)


def wrap(c, x, y, w, h, cloth, diamond, seed):
    """A tsuka-ito wrap: diagonal cloth bands with diamond gaps showing the gold beneath."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    period = 8.0
    a = ((xx + yy) % period) / period
    b = ((xx - yy) % period) / period
    band = np.minimum(np.abs(a - 0.5), np.abs(b - 0.5)) * 2
    gap = smoothstep(0.55, 0.75, 1 - band)
    weave = periodic_noise(w, h, 1.2, seed) * 0.25
    rgb = mix(np.asarray(cloth) * (0.8 + weave[..., None]), diamond, gap)
    c.put(x, y, rgb, glow=gap * 0.15)


def radial(c, x, y, w, h, inner, outer, power=1.0, glow_inner=0.0, ring=None, seed=0):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    r = np.hypot((xx + 0.5) / w * 2 - 1, (yy + 0.5) / h * 2 - 1)
    t = np.clip(r, 0, 1) ** power
    rgb = mix(inner, outer, t)
    if ring is not None:
        rgb = mix(rgb, ring, smoothstep(0.05, 0.0, np.abs(r - 0.7)))
    n = periodic_noise(w, h, 1.5, seed) * 0.15
    rgb = rgb * (0.92 + n[..., None])
    c.put(x, y, rgb, glow=np.clip(1 - r, 0, 1) * glow_inner)


def petals(c, x, y, w, h, light, dark, seed):
    n = periodic_noise(w, h, 2.0, seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    swirl = 0.5 + 0.5 * np.sin(np.arctan2(yy - h / 2, xx - w / 2) * 5 + np.hypot(yy - h / 2, xx - w / 2) * 0.4)
    rgb = mix(dark, light, np.clip(swirl * 0.6 + n * 0.5, 0, 1))
    c.put(x, y, rgb, glow=smoothstep(0.75, 1.0, swirl * n + 0.2) * 0.35)


def feather(c, x, y, w, h, root, tip, seed):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    along = xx / max(1, w - 1)
    barbs = 0.5 + 0.5 * np.sin(yy * 1.7 + xx * 0.25)
    n = periodic_noise(w, h, 1.4, seed)
    rgb = mix(root, tip, along) * (0.7 + 0.35 * barbs[..., None] + 0.15 * n[..., None])
    c.put(x, y, rgb, glow=smoothstep(0.7, 1.0, along) * 0.6 * barbs)


def leather(c, x, y, w, h, tone, seed):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    twist = 0.5 + 0.5 * np.sin((xx * 0.9 - yy * 0.6) * 0.9)
    n = periodic_noise(w, h, 1.5, seed)
    rgb = np.asarray(tone)[None, None, :] * (0.55 + 0.45 * twist[..., None] + 0.2 * n[..., None])
    c.put(x, y, rgb)


def runes_strip(c, x, y, w, h, core, rune, seed):
    """The spectral blade's core: bright energy with a line of runes down the fuller."""
    rng = np.random.default_rng(seed)
    ss = 3
    img = Image.new('L', (w * ss, h * ss), 0)
    d = ImageDraw.Draw(img)
    cx = 0
    while cx < w * ss:
        size = h * ss * 0.55
        gx = cx + size * 0.5
        gy = h * ss / 2
        d.line([(gx, gy - size / 2), (gx, gy + size / 2)], fill=255, width=ss)
        for _ in range(rng.integers(1, 3)):
            yy = gy + rng.uniform(-size / 2, size / 3)
            side = rng.choice([-1, 1])
            d.line([(gx, yy), (gx + side * size * 0.35, yy + rng.choice([-1, 1]) * size * 0.3)], fill=255, width=ss)
        cx += size * 1.3
    img = img.resize((w, h), Image.LANCZOS)
    glyphs = np.asarray(img).astype(np.float32) / 255
    v = np.linspace(0, 1, h)[:, None] * np.ones((1, w))
    flow = periodic_noise(w, h, 2.0, seed + 1)
    base = mix(core, np.asarray(core) * 0.55, np.abs(v - 0.5) * 1.6) * (0.85 + 0.25 * flow[..., None])
    rgb = mix(base, rune, glyphs)
    c.put(x, y, rgb, glow=np.clip(0.55 + glyphs * 0.45 + flow * 0.1, 0, 1))


def flat(c, x, y, w, h, rgb, glow=0.0, noise=0.08, seed=0):
    n = periodic_noise(w, h, 1.5, seed)
    out = np.asarray(rgb)[None, None, :] * (1 - noise + 2 * noise * n[..., None])
    c.put(x, y, out, glow=np.full((h, w), glow, np.float32) if glow else None)
