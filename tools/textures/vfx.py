"""Textures for the overhaul's smoke, portal, kill-effect and sky passes.

    python3 vfx.py ../../src/main/resources/assets/soupapi/textures/vfx

Two kinds, following common.py's conventions:

  * white-with-alpha (write_white_alpha) for anything drawn with normal alpha blending. Smoke has to
    be: black smoke under the additive blend the other particles use adds nothing at all. The vertex
    colour supplies the darkness, the alpha supplies the shape.
  * greyscale-on-black "base" art (write_base) for the additive passes: edge light, embers, glyphs,
    rings, shards.

Every texture is a power of two (gl4es on PojavLauncher needs it for mipmapped sampling), and every
noise is periodic, built in the frequency domain, so the tiling ones tile with no seam.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from common import grid, write_base, write_white_alpha, write_rgba

RNG = np.random.default_rng(1729)


def periodic_noise(n, beta=2.0, seed=None, aspect=(1.0, 1.0)):
    """Tileable fractal noise in [0, 1]: white noise shaped by 1/f^beta in the frequency domain."""
    rng = np.random.default_rng(seed) if seed is not None else RNG
    white = rng.standard_normal((n, n))
    f = np.fft.fft2(white)
    fy = np.fft.fftfreq(n)[:, None] * aspect[1]
    fx = np.fft.fftfreq(n)[None, :] * aspect[0]
    radius = np.sqrt(fx * fx + fy * fy)
    radius[0, 0] = 1.0
    shaped = np.real(np.fft.ifft2(f / radius ** (beta / 2.0)))
    shaped -= shaped.min()
    return shaped / max(1e-9, shaped.max())


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def warp(field, nx, ny, amount):
    """Domain warp by two noise fields, for the curling look of real smoke."""
    n = field.shape[0]
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    sx = (xx + (nx - 0.5) * amount * n) % n
    sy = (yy + (ny - 0.5) * amount * n) % n
    return field[sy.astype(int), sx.astype(int)]


def blur(field, radius):
    img = Image.fromarray((np.clip(field, 0, 1) * 255).astype(np.uint8))
    return np.asarray(img.filter(ImageFilter.GaussianBlur(radius))).astype(np.float32) / 255.0


def rune_glyph(d, cx, cy, size, rng, fill, width):
    """A runic glyph from strokes: a stem and two or three branches, Futhark-like, no font needed."""
    h = size * 0.5
    d.line([(cx, cy - h), (cx, cy + h)], fill=fill, width=width)
    for _ in range(rng.integers(1, 4)):
        y0 = cy + rng.uniform(-h, h * 0.4)
        side = rng.choice([-1, 1])
        dy = rng.choice([-1, 1]) * h * rng.uniform(0.35, 0.7)
        d.line([(cx, y0), (cx + side * size * 0.38, y0 + dy)], fill=fill, width=width)
    if rng.random() < 0.35:
        d.line([(cx - size * 0.3, cy - h * 0.2), (cx + size * 0.3, cy + h * 0.2)], fill=fill, width=width)


def puff(n, seed):
    """One smoke puff: a billowing silhouette, dense in the middle, ragged and thin at the edge.

    Returns (alpha, edge): the alpha the smoke is drawn with, and an additive mask that is bright
    only in the thin band just inside the silhouette, which is what lights the rim.
    """
    x, y, r, a = grid(n)
    big = periodic_noise(n, 3.0, seed)
    mid = periodic_noise(n, 2.5, seed + 1)
    fine = periodic_noise(n, 2.0, seed + 2)
    w1 = periodic_noise(n, 3.4, seed + 3)
    w2 = periodic_noise(n, 3.4, seed + 4)
    # Mostly large, soft structure; only a trace of fine grain, or it reads as hair, not smoke.
    detail = warp(0.5 * big + 0.4 * mid + 0.1 * fine, w1, w2, 0.3)
    # Lobes around the rim make it billow rather than read as a disc.
    lobes = 0.12 * np.sin(a * 3 + seed) + 0.08 * np.sin(a * 5 + seed * 1.7) + 0.05 * np.sin(a * 9 + seed)
    reach = 0.95 + lobes + (detail - 0.5) * 0.5
    # A soft falloff all the way out, not a plateau with a cliff: overlapping puffs then build
    # density smoothly, which is what reads as volume rather than as stacked cut-outs.
    falloff = np.clip(1.0 - r / np.maximum(0.2, reach), 0.0, 1.0)
    density = falloff ** 1.35 * (0.45 + 0.9 * detail)
    alpha = np.clip(density * 1.25, 0, 1)
    alpha = blur(alpha, n / 64.0)
    # Rim light from above: where the density falls away going up the image, broken up by noise,
    # so only the upper contour catches light and never as a closed outline.
    gy = np.zeros_like(alpha)
    gy[1:-1, :] = alpha[2:, :] - alpha[:-2, :]
    contour = (4.0 * alpha * (1.0 - alpha)) ** 2.0      # peaks where the puff thins out
    rim = np.clip(gy * 16.0, 0, 1) * contour * smoothstep(0.3, 0.75, fine + 0.3 * mid)
    edge = np.clip(blur(rim, n / 90.0) * 1.2, 0, 1)
    return alpha, edge


def wisp(w, h, seed):
    """A long, streaked strand of smoke for things moving through the air."""
    n = max(w, h)
    stretched = periodic_noise(n, 2.8, seed, aspect=(0.25, 1.0))
    stretched = np.array(Image.fromarray((stretched * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)) / 255.0
    ys = (np.arange(h) + 0.5) / h * 2 - 1
    xs = (np.arange(w) + 0.5) / w * 2 - 1
    X, Y = np.meshgrid(xs, ys)
    taper = (1 - smoothstep(0.2, 1.0, np.abs(X))) * (1 - smoothstep(0.15, 0.95, np.abs(Y + 0.15 * np.sin(X * 3 + seed))))
    alpha = np.clip(taper ** 1.3 * (0.35 + 0.95 * stretched), 0, 1)
    return blur(alpha, 2.0)


def soft_glow(n, power=2.2):
    _, _, r, _ = grid(n)
    return np.clip(1 - r, 0, 1) ** power


def ember(n):
    _, _, r, _ = grid(n)
    core = np.exp(-(r / 0.12) ** 2)
    halo = np.exp(-(r / 0.45) ** 2) * 0.45
    return np.clip(core + halo, 0, 1)


def spark(w, h):
    xs = (np.arange(w) + 0.5) / w * 2 - 1
    ys = (np.arange(h) + 0.5) / h * 2 - 1
    X, Y = np.meshgrid(xs, ys)
    body = np.exp(-(Y / 0.18) ** 2) * (1 - smoothstep(0.0, 1.0, np.abs(X))) ** 0.8
    head = np.exp(-((X - 0.55) ** 2 + (Y / 0.6) ** 2) / 0.03)
    return np.clip(body + head, 0, 1)


def shard(n, seed):
    """A glass fragment: a sharp polygon with bright edges and a faint, streaked interior."""
    rng = np.random.default_rng(seed)
    ss = 4
    img = Image.new('L', (n * ss, n * ss), 0)
    d = ImageDraw.Draw(img)
    c = n * ss / 2
    pts = []
    k = rng.integers(3, 6)
    for i in range(k):
        ang = i * 2 * math.pi / k + rng.uniform(-0.4, 0.4)
        rad = rng.uniform(0.35, 0.95) * c
        if i == 0:
            rad = 0.98 * c  # one long point
        pts.append((c + rad * math.cos(ang), c + rad * math.sin(ang)))
    d.polygon(pts, fill=70)
    d.line(pts + [pts[0]], fill=255, width=int(ss * n / 40) + 1, joint='curve')
    # A crack line across the face.
    d.line([pts[0], (c + rng.uniform(-0.2, 0.2) * c, c + rng.uniform(-0.2, 0.2) * c)], fill=200, width=ss)
    img = img.resize((n, n), Image.LANCZOS).filter(ImageFilter.GaussianBlur(0.6))
    lum = np.asarray(img).astype(np.float32) / 255.0
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(n / 18))).astype(np.float32) / 255.0
    return np.clip(lum + glow * 0.6, 0, 1)


def feather(n):
    ss = 4
    img = Image.new('L', (n * ss, n * ss), 0)
    d = ImageDraw.Draw(img)
    N = n * ss
    # Shaft from bottom-left to top-right, vanes either side.
    for i in range(60):
        t = i / 59
        cx = N * (0.2 + 0.6 * t)
        cy = N * (0.85 - 0.7 * t)
        length = N * 0.28 * math.sin(math.pi * min(1, t * 1.15)) + 1
        for side in (-1, 1):
            ang = math.radians(-45 + side * 60 + 10 * math.sin(t * 9))
            d.line([(cx, cy), (cx + length * math.cos(ang) * 0.8, cy - length * math.sin(ang) * 0.8 * side)],
                   fill=int(120 + 100 * (1 - t)), width=ss)
    d.line([(N * 0.15, N * 0.9), (N * 0.82, N * 0.12)], fill=255, width=ss * 2)
    img = img.resize((n, n), Image.LANCZOS).filter(ImageFilter.GaussianBlur(0.8))
    lum = np.asarray(img).astype(np.float32) / 255.0
    return np.clip(lum * 1.2, 0, 1)


def rune_ring(n, glyphs, seed, thickness=0.06):
    """A ring of glyphs between two bands: the runic rings of the portal and the arcane effects."""
    ss = 2
    N = n * ss
    img = Image.new('L', (N, N), 0)
    d = ImageDraw.Draw(img)
    c = N / 2
    r_out = N * 0.48
    r_in = N * (0.48 - thickness * 2.2)
    w = max(2, int(N * 0.006))
    d.ellipse([c - r_out, c - r_out, c + r_out, c + r_out], outline=255, width=w)
    d.ellipse([c - r_in, c - r_in, c + r_in, c + r_in], outline=200, width=max(1, w // 2 + 1))
    rng = np.random.default_rng(seed)
    count = glyphs
    mid = (r_out + r_in) / 2
    glyph_size = (r_out - r_in) * 0.62
    for i in range(count):
        ang = i / count * 2 * math.pi
        layer = Image.new('L', (int(N * 0.12), int(N * 0.12)), 0)
        ld = ImageDraw.Draw(layer)
        rune_glyph(ld, layer.size[0] / 2, layer.size[1] / 2, glyph_size, rng,
                   int(rng.uniform(190, 255)), max(2, int(N * 0.005)))
        layer = layer.rotate(-math.degrees(ang) - 90, resample=Image.BICUBIC)
        px = int(c + mid * math.cos(ang) - layer.size[0] / 2)
        py = int(c + mid * math.sin(ang) - layer.size[1] / 2)
        img.paste(layer, (px, py), layer)
    # Tick marks on the outer band.
    for i in range(count * 3):
        ang = i / (count * 3) * 2 * math.pi
        a0 = r_out + N * 0.004
        a1 = r_out - N * 0.012
        d.line([(c + a0 * math.cos(ang), c + a0 * math.sin(ang)), (c + a1 * math.cos(ang), c + a1 * math.sin(ang))],
               fill=230, width=max(1, w // 2))
    img = img.resize((n, n), Image.LANCZOS)
    lum = np.asarray(img).astype(np.float32) / 255.0
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(n / 120))).astype(np.float32) / 255.0
    return np.clip(lum + glow * 0.7, 0, 1)


def fracture(n, seed):
    """Branching cracks radiating from the centre: the rift's first tear, and the glass collapse."""
    rng = np.random.default_rng(seed)
    ss = 2
    N = n * ss
    img = Image.new('L', (N, N), 0)
    d = ImageDraw.Draw(img)
    c = N / 2

    def branch(x, y, ang, length, width, depth):
        if depth > 5 or length < N * 0.02:
            return
        steps = 6
        px, py = x, y
        for _ in range(steps):
            ang += rng.uniform(-0.35, 0.35)
            nx = px + math.cos(ang) * length / steps
            ny = py + math.sin(ang) * length / steps
            d.line([(px, py), (nx, ny)], fill=int(255 - depth * 25), width=max(1, int(width)))
            px, py = nx, ny
            if rng.random() < 0.28:
                branch(px, py, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.1), length * 0.5, width * 0.6, depth + 1)
        branch(px, py, ang, length * 0.6, width * 0.75, depth + 1)

    for i in range(7):
        branch(c, c, i / 7 * 2 * math.pi + rng.uniform(-0.3, 0.3), N * 0.22, N * 0.012, 0)
    img = img.resize((n, n), Image.LANCZOS)
    lum = np.asarray(img).astype(np.float32) / 255.0
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(n / 64))).astype(np.float32) / 255.0
    _, _, r, _ = grid(n)
    core = np.exp(-(r / 0.08) ** 2)
    return np.clip(lum + glow * 0.9 + core, 0, 1)


def portal_core(n):
    """The dark centre of the rift: a swirl that is black in the middle with a lit, turning rim."""
    x, y, r, a = grid(n)
    noise = periodic_noise(n, 2.2, 77)
    swirl = 0.5 + 0.5 * np.sin(a * 5 + r * 14 + noise * 4)
    alpha = np.clip(1 - smoothstep(0.55, 0.98, r + (noise - 0.5) * 0.15), 0, 1)
    light = smoothstep(0.35, 0.9, r) * (1 - smoothstep(0.85, 1.0, r)) * (0.4 + 0.6 * swirl)
    rgb = np.stack([light, light, light], -1)
    return rgb, alpha


def great_eye(n=256):
    """A lidded eye, for the Weeping Eye's summon: sclera, a fibrous iris, a slit pupil.

    Drawn as greyscale-on-black for the additive pass, with the lids as hard darkness rather than
    transparency, so the eye can be opened by scaling the quad and still read as a closing lid.
    """
    x, y, r, th = grid(n)
    out = np.zeros_like(r)

    # The almond: two circular arcs meeting at the corners.
    lid = np.clip(1.0 - (np.abs(y) / (0.42 * np.sqrt(np.maximum(1e-4, 1.0 - (x / 0.95) ** 2)))) ** 2.2, 0, 1)
    lid *= (np.abs(x) < 0.95)
    sclera = smoothstep(0.0, 0.25, lid) * 0.55

    # Veins crawling in from the corners.
    veins = periodic_noise(n, 2.8, 77)
    vein_lines = np.exp(-((veins - 0.5) ** 2) / 0.0009) * np.clip((np.abs(x) - 0.25) * 2.2, 0, 1)
    sclera = np.clip(sclera + vein_lines * lid * 0.5, 0, 1)

    # The iris: fibres running out from the pupil, bright, with a dark limbal ring.
    ir = np.sqrt(x ** 2 + (y / 0.98) ** 2)
    fibres = 0.5 + 0.5 * np.sin(th * 44 + periodic_noise(n, 2.0, 91) * 9)
    iris = np.clip(1 - (ir / 0.34) ** 3, 0, 1) * (0.45 + 0.55 * fibres)
    iris *= smoothstep(0.36, 0.30, ir)
    limbal = np.exp(-((ir - 0.33) ** 2) / 0.0007) * 0.8
    pupil = smoothstep(0.16, 0.06, np.sqrt((x / 0.42) ** 2 + y ** 2))

    out = np.clip(sclera + iris * 1.15 - limbal, 0, 1)
    out = np.clip(out * (1 - pupil), 0, 1)
    out += np.exp(-(((x - 0.12) ** 2 + (y - 0.14) ** 2)) / 0.004) * 0.9     # the catchlight
    out *= smoothstep(0.02, 0.18, lid)
    return np.clip(blur(out, n / 256.0), 0, 1)


def crow(w=128, h=128):
    """A crow in silhouette, wings up: the shape the claw's summon scatters."""
    img = Image.new('L', (w, h), 0)
    d = ImageDraw.Draw(img)
    cx, cy = w / 2, h / 2
    d.ellipse([cx - w * 0.07, cy - h * 0.16, cx + w * 0.07, cy + h * 0.20], fill=255)   # body
    d.ellipse([cx - w * 0.06, cy - h * 0.30, cx + w * 0.06, cy - h * 0.14], fill=255)   # head
    d.polygon([(cx + w * 0.05, cy - h * 0.26), (cx + w * 0.20, cy - h * 0.22),
               (cx + w * 0.05, cy - h * 0.19)], fill=255)                               # beak
    for side in (1, -1):
        d.polygon([(cx + side * w * 0.05, cy - h * 0.10),
                   (cx + side * w * 0.44, cy - h * 0.40),
                   (cx + side * w * 0.40, cy - h * 0.16),
                   (cx + side * w * 0.30, cy + h * 0.04),
                   (cx + side * w * 0.06, cy + h * 0.08)], fill=255)                    # wing
    d.polygon([(cx - w * 0.05, cy + h * 0.18), (cx + w * 0.05, cy + h * 0.18),
               (cx + w * 0.02, cy + h * 0.44), (cx - w * 0.02, cy + h * 0.44)], fill=255)
    a = np.asarray(img).astype(np.float32) / 255.0
    return np.clip(blur(a, 1.1), 0, 1)


def write_white_alpha_safe(alpha, path):
    write_white_alpha(alpha, path)


def rune_sprite(n, seed):
    """One glowing rune on its own, for the floating runes and the glyph sparks."""
    rng = np.random.default_rng(seed)
    ss = 4
    img = Image.new('L', (n * ss, n * ss), 0)
    d = ImageDraw.Draw(img)
    rune_glyph(d, n * ss / 2, n * ss / 2, n * ss * 0.7, rng, 255, max(2, n * ss // 14))
    img = img.resize((n, n), Image.LANCZOS)
    lum = np.asarray(img).astype(np.float32) / 255.0
    glow = np.asarray(img.filter(ImageFilter.GaussianBlur(n / 14))).astype(np.float32) / 255.0
    return np.clip(lum + glow * 0.8, 0, 1)


def place(atlas, tile, x, y):
    h, w = tile.shape
    atlas[y:y + h, x:x + w] = tile


def main(out):
    os.makedirs(out, exist_ok=True)
    # Smoke atlas, 512x256: four puffs across the top row, two wisps (128x64) below.
    # The edge atlas has the same layout, so one UV rectangle serves both passes.
    smoke = np.zeros((256, 512), np.float32)
    edges = np.zeros((256, 512), np.float32)
    for i in range(4):
        alpha, edge = puff(128, 11 + i * 7)
        place(smoke, alpha, i * 128, 0)
        place(edges, edge, i * 128, 0)
    for i in range(2):
        w = wisp(128, 64, 3 + i * 5)
        place(smoke, w, i * 128, 128)
        # Lit along its upper side only, like the puffs, never as an outline.
        gy = np.zeros_like(w)
        gy[1:-1, :] = w[2:, :] - w[:-2, :]
        rim = np.clip(gy * 8, 0, 1) * (4 * w * (1 - w)) ** 2
        place(edges, np.clip(blur(rim, 1.2) * 1.5, 0, 1), i * 128, 128)
    write_white_alpha(smoke, os.path.join(out, 'smoke_atlas.png'))
    write_base(edges, os.path.join(out, 'smoke_edge_atlas.png'))

    # Additive atlas, 512x512 in 128-pixel cells:
    #   row 0: ember, glow, spark, feather     row 1: shard 0..2, fracture
    #   rows 2-3: eight runes
    fx = np.zeros((512, 512), np.float32)
    place(fx, np.array(Image.fromarray((ember(32) * 255).astype(np.uint8)).resize((128, 128), Image.BICUBIC)) / 255.0, 0, 0)
    place(fx, soft_glow(128), 128, 0)
    sp = np.zeros((128, 128), np.float32)
    place(sp, spark(128, 32), 0, 48)
    place(fx, sp, 256, 0)
    place(fx, feather(128), 384, 0)
    for i in range(3):
        place(fx, shard(128, 40 + i), i * 128, 128)
    place(fx, np.array(Image.fromarray((fracture(256, 3) * 255).astype(np.uint8)).resize((128, 128), Image.LANCZOS)) / 255.0, 384, 128)
    for i in range(8):
        place(fx, rune_sprite(128, 900 + i), (i % 4) * 128, 256 + (i // 4) * 128)
    write_base(fx, os.path.join(out, 'fx_atlas.png'))

    write_base(soft_glow(128), os.path.join(out, 'glow.png'))
    write_base(rune_ring(512, 18, 5, 0.055), os.path.join(out, 'rune_ring_0.png'))
    write_base(rune_ring(512, 24, 9, 0.045), os.path.join(out, 'rune_ring_1.png'))
    write_base(rune_ring(256, 12, 13, 0.07), os.path.join(out, 'rune_ring_2.png'))
    write_base(fracture(256, 3), os.path.join(out, 'fracture.png'))
    write_base(great_eye(256), os.path.join(out, 'great_eye.png'))
    # Summon atlas, 256x256 in 128-pixel cells: crow, crow (mirrored), sigil, shockwave.
    summon = np.zeros((256, 256), np.float32)
    bird = crow(128, 128)
    place(summon, bird, 0, 0)
    place(summon, bird[:, ::-1], 128, 0)
    place(summon, rune_ring(128, 8, 61, 0.10), 0, 128)
    ring = np.zeros((128, 128), np.float32)
    gx, gy, gr, _ = grid(128)
    ring += np.exp(-((gr - 0.78) ** 2) / 0.0024) * 1.0
    ring += np.exp(-((gr - 0.62) ** 2) / 0.012) * 0.35
    place(summon, np.clip(ring, 0, 1), 128, 128)
    write_base(summon, os.path.join(out, 'summon_atlas.png'))
    rgb, alpha = portal_core(256)
    write_rgba(rgb, alpha, os.path.join(out, 'portal_core.png'))
    # Tileable noise for the sky passes: soft cloud, fine grain and long streaks.
    write_base(periodic_noise(256, 2.4, 101), os.path.join(out, 'sky_cloud.png'))
    write_base(periodic_noise(256, 1.6, 202), os.path.join(out, 'sky_grain.png'))
    write_base(periodic_noise(256, 2.0, 303, aspect=(0.2, 1.0)), os.path.join(out, 'sky_streak.png'))
    print('wrote', len(os.listdir(out)), 'textures to', out)


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'vfx_out')
