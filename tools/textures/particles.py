"""textures/particle/*.png - the sprite set used by the particle modules."""
import math, os, sys
import numpy as np
from common import *

OUT = sys.argv[1]
N = 256
os.makedirs(OUT, exist_ok=True)


# ---------------------------------------------------------------- fireflies --
# These four are white-with-alpha rather than greyscale-on-black: the soul
# renderer can draw them with defaultBlendFunc (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)
# when targetRenderSoulStyle is neither SMOKE nor PLASMA, and a black-backed
# sprite would paint black squares there. Under the additive path the two
# conventions are mathematically identical.
def firefly(core, halo, halo_w, spikes=0.0):
    x, y, r, th = grid(N)
    a = np.exp(-(r / core) ** 2)
    a += halo * np.exp(-(r / halo_w) ** 2)
    if spikes:
        for ang in (0.0, math.pi / 2):
            u = x * math.cos(ang) + y * math.sin(ang)
            v = -x * math.sin(ang) + y * math.cos(ang)
            a += spikes * np.exp(-(v / 0.030) ** 2) * np.exp(-(u / 0.62) ** 2)
        for ang in (math.pi / 4, 3 * math.pi / 4):
            u = x * math.cos(ang) + y * math.sin(ang)
            v = -x * math.sin(ang) + y * math.cos(ang)
            a += 0.35 * spikes * np.exp(-(v / 0.022) ** 2) * np.exp(-(u / 0.38) ** 2)
    a *= np.clip((1.0 - r) / 0.12, 0, 1)        # keep the sprite off its own edge
    return np.clip(a, 0, 1)


write_white_alpha(firefly(0.17, 0.42, 0.52), f'{OUT}/firefly.png')
write_white_alpha(firefly(0.30, 0.75, 0.78), f'{OUT}/firefly_glow.png')
write_white_alpha(firefly(0.11, 0.30, 0.46, spikes=0.95), f'{OUT}/firefly_alt.png')
write_white_alpha(firefly(0.22, 0.70, 0.80, spikes=0.45), f'{OUT}/firefly_alt_glow.png')


# ------------------------------------------------------------------- shapes --
def soften(lum, glow=0.45):
    """Adds a small bloom so the sprite reads as emissive under additive blend."""
    from PIL import Image, ImageFilter
    img = Image.fromarray((np.clip(lum, 0, 1) * 255).astype(np.uint8))
    b1 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 64.0))).astype(np.float32) / 255.0
    b2 = np.asarray(img.filter(ImageFilter.GaussianBlur(N / 20.0))).astype(np.float32) / 255.0
    return np.clip(lum + glow * b1 + glow * 0.6 * b2, 0, 1)


def dollar():
    m, d = mask(N)
    n = N * SS
    font = load_font(int(n * 0.92))
    box = d.textbbox((0, 0), '$', font=font)
    d.text(((n - (box[2] - box[0])) / 2 - box[0], (n - (box[3] - box[1])) / 2 - box[1]),
           '$', font=font, fill=255)
    return lum_of(shrink(m, N))


def snowflake():
    m, d = mask(N)
    n = N * SS
    c = n / 2.0
    arm = n * 0.455
    w = n * 0.030
    for i in range(6):
        a = i * math.pi / 3
        ex, ey = c + arm * math.cos(a), c + arm * math.sin(a)
        thick_line(d, [(c, c), (ex, ey)], w)
        # side branches
        for frac, blen in ((0.42, 0.20), (0.66, 0.15), (0.86, 0.10)):
            bx, by = c + arm * frac * math.cos(a), c + arm * frac * math.sin(a)
            for s in (+1, -1):
                ba = a + s * math.pi / 3.2
                thick_line(d, [(bx, by), (bx + arm * blen * math.cos(ba), by + arm * blen * math.sin(ba))],
                           w * 0.72)
    r = n * 0.055
    d.ellipse([c - r, c - r, c + r, c + r], fill=255)
    return lum_of(shrink(m, N))


def heart():
    m, d = mask(N)
    n = N * SS
    pts = []
    for i in range(721):
        t = i * math.pi / 360.0
        hx = 16 * math.sin(t) ** 3
        hy = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((n / 2 + hx / 17.0 * (n * 0.46), n / 2 + hy / 17.0 * (n * 0.46)))
    d.polygon(pts, fill=255)
    return lum_of(shrink(m, N))


def star():
    m, d = mask(N)
    n = N * SS
    d.polygon(star_points(n / 2, n / 2 * 1.04, n * 0.47, n * 0.195), fill=255)
    return lum_of(shrink(m, N))


for name, fn in (('dollar', dollar), ('snowflake', snowflake), ('heart', heart), ('star', star)):
    lum = soften(fn())
    write_base(lum, f'{OUT}/{name}.png')
    write_unblack(lum, f'{OUT}/{name}_unblack.png')

print('particles ok')
