"""Volumetric smoke sprites: a ray-marched, self-shadowed billow, played as a flipbook.

    python3 smoke_volume.py ../../src/main/resources/assets/soupapi/textures/vfx

The smoke used to be drawn from blurred four-pointed blobs with no light in them, so however many
of them were stacked, a cloud of them read as a cloud of fuzzy stars. Smoke in a game without
shaders is only ever as good as its sprite, and the sprites that look like smoke are the ones a
VFX artist renders out of a real volume: billows with bright, sunlit tops and dark undersides in
their own shadow, soft wisping edges, and a flipbook of it churning and thinning as it ages.

So that is what this does, in numpy:

  * Density: a handful of overlapping billows, each a soft sphere, eroded and displaced by
    tileable 3D fractal noise. With age the billows swell, the noise eats further in, the whole
    thing thins, and the noise field is advected upward, so the puff visibly turns over itself.
  * Light: optical depth toward a sun high and slightly in front is accumulated through the
    volume, so every voxel knows how much of the smoke above it is shading it.
  * View: marched front to back, Beer-Lambert, with a sun term (shadowed), a cool ambient term and
    a little powder darkening on the thin edges, which is what gives billows their rims.

Outputs, both 1024 x 1024, an 8 x 8 grid of 128 px cells - four billow shapes, sixteen ages each
(shape s, age f at row 2s + f // 8, column f % 8):

  smoke_volume.png       straight-alpha RGBA: the lit smoke, for normal alpha blending
  smoke_volume_edge.png  greyscale on black: the sunlit shell alone, for the additive rim pass
"""
import math
import os
import sys

import numpy as np

from common import write_base, write_rgba

N = 128          # sprite size
D = 64           # depth samples through the volume
FRAMES = 16
SHAPES = 4


def noise3(shape, beta, seed):
    """Tileable 3D fractal noise in [0, 1]: white noise shaped by 1/f^beta in frequency space."""
    rng = np.random.default_rng(seed)
    white = rng.standard_normal(shape)
    f = np.fft.rfftn(white)
    kz = np.fft.fftfreq(shape[0])[:, None, None]
    ky = np.fft.fftfreq(shape[1])[None, :, None]
    kx = np.fft.rfftfreq(shape[2])[None, None, :]
    k = np.sqrt(kx * kx + ky * ky + kz * kz)
    k[0, 0, 0] = 1.0
    f *= 1.0 / k ** beta
    f[0, 0, 0] = 0.0
    v = np.fft.irfftn(f, s=shape)
    v -= v.min()
    v /= v.max()
    return v.astype(np.float32)


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def render(shape_seed):
    """Sixteen frames of one billow shape, as (rgb, alpha, rim) arrays."""
    rng = np.random.default_rng(shape_seed)
    # Coordinates: x right, y up, z toward the viewer, all in [-1, 1].
    zs = np.linspace(1.0, -1.0, D, dtype=np.float32)[:, None, None]        # front first
    ys = np.linspace(1.0, -1.0, N, dtype=np.float32)[None, :, None]        # top row first
    xs = np.linspace(-1.0, 1.0, N, dtype=np.float32)[None, None, :]

    # The billows: a main body and a few lobes off it, lower ones a little wider (smoke spreads
    # where it is dense and rises where it is hot).
    blobs = [((0.0, -0.05, 0.0), 0.42)]
    for _ in range(rng.integers(5, 9)):
        a = rng.uniform(0, math.tau)
        el = rng.uniform(-0.6, 0.9)
        d = rng.uniform(0.2, 0.4)
        c = (math.cos(a) * d * math.cos(el), math.sin(el) * d * 0.9 - 0.02, math.sin(a) * d * math.cos(el))
        blobs.append((c, rng.uniform(0.2, 0.32)))

    big = noise3((D, N, N), 1.55, shape_seed * 7 + 1)      # large churn
    fine = noise3((D, N, N), 1.25, shape_seed * 7 + 2)     # small wisps

    sun = np.array([0.30, 0.86, 0.42], np.float32)
    sun /= np.linalg.norm(sun)
    dz = 2.0 / D
    dy = 2.0 / N

    frames = []
    for f in range(FRAMES):
        age = f / (FRAMES - 1)
        grow = 1.0 + 0.42 * age ** 0.8
        # Rising churn: the noise is read further down as the puff ages, so features drift up.
        shift = int(round(age * N * 0.22))
        n_big = np.roll(big, shift, axis=1)
        n_fine = np.roll(fine, int(round(shift * 1.7)), axis=1)

        # Signed: negative outside every billow, so the noise below can only erode and roughen
        # the smoke that is there, never conjure a haze of specks where there is none.
        field = np.full((D, N, N), -1.0, np.float32)
        for (cx, cy, cz), r in blobs:
            rr = r * grow
            d = np.sqrt((xs - cx * grow) ** 2 + (ys - cy * grow - 0.12 * age) ** 2 + (zs - cz * grow) ** 2)
            field = np.maximum(field, 1.0 - d / rr)

        # Erosion: the noise eats in from the edge, further as the puff ages and thins.
        # Erosion: the noise eats in from the edge, further as the puff ages and thins. Strongest
        # at the surface (where `field` is small), so the outline is cauliflowered and wisping
        # while the body stays whole.
        surface = np.exp(-np.abs(field) * 3.0)
        erode = 0.55 + 0.45 * age
        density = field + ((n_big - 0.5) * erode * 1.75 + (n_fine - 0.5) * (0.35 + 0.45 * age)) * (0.45 + 0.55 * surface)
        density = smoothstep(0.0, 0.42 - 0.12 * age, density)
        density *= (1.0 - 0.6 * age ** 1.4)
        # Keep everything inside the cell, with a soft margin.
        edge = np.sqrt(xs ** 2 + ys ** 2)
        density *= smoothstep(1.0, 0.82, edge)
        sigma = density * 10.0

        # Optical depth toward the sun: accumulated from the top down (the sun is high) plus a
        # share from the front (it is a little in front), each a cumulative sum along its axis.
        from_top = np.cumsum(sigma, axis=1) * dy
        from_front = np.cumsum(sigma, axis=0) * dz
        tau_sun = from_top * sun[1] + from_front * sun[2] * 0.8
        t_sun = np.exp(-tau_sun * 0.62)

        # Front-to-back march toward the viewer.
        trans = np.ones((N, N), np.float32)
        col = np.zeros((N, N, 3), np.float32)
        rim = np.zeros((N, N), np.float32)
        sun_c = np.array([1.00, 0.97, 0.92], np.float32)
        amb_c = np.array([0.50, 0.54, 0.62], np.float32)
        for k in range(D):
            s = sigma[k]
            absorb = 1.0 - np.exp(-s * dz)
            if not absorb.any():
                continue
            # Powder: thin smoke is darker than Beer's law alone says, which is what draws the
            # bright-edged, dark-cored look of real billows.
            powder = 1.0 - np.exp(-s * dz * 2.4)
            lit = t_sun[k] * (0.72 + 0.28 * powder)
            # A little ambient occlusion from how deep in the body this voxel is.
            ao = np.exp(-from_front[k] * 0.35)
            light = lit[..., None] * sun_c * 1.05 + (ao[..., None] * amb_c)
            col += (trans * absorb)[..., None] * light
            rim += trans * absorb * lit * (1.0 - smoothstep(0.0, 0.9, from_top[k]))
            trans *= np.exp(-s * dz)
        alpha = 1.0 - trans
        rgb = np.where(alpha[..., None] > 1e-4, col / np.maximum(alpha[..., None], 1e-4), 0.0)
        frames.append((np.clip(rgb, 0, 1), np.clip(alpha, 0, 1), np.clip(rim * 1.6, 0, 1)))
    return frames


def main(out):
    os.makedirs(out, exist_ok=True)
    size = N * 8
    rgb_atlas = np.zeros((size, size, 3), np.float32)
    a_atlas = np.zeros((size, size), np.float32)
    rim_atlas = np.zeros((size, size), np.float32)
    for s in range(SHAPES):
        for f, (rgb, alpha, rim) in enumerate(render(101 + s * 13)):
            row, col = 2 * s + f // 8, f % 8
            y0, x0 = row * N, col * N
            rgb_atlas[y0:y0 + N, x0:x0 + N] = rgb
            a_atlas[y0:y0 + N, x0:x0 + N] = alpha
            rim_atlas[y0:y0 + N, x0:x0 + N] = rim
        print('shape', s, 'done')
    write_rgba(rgb_atlas, a_atlas, os.path.join(out, 'smoke_volume.png'))
    write_base(rim_atlas, os.path.join(out, 'smoke_volume_edge.png'))
    print('wrote smoke_volume.png and smoke_volume_edge.png to', out)


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'smoke_out')
