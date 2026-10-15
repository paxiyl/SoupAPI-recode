"""gui/hitbox.png, capes/optifine/*.png and textures/i.png."""
import math, os, sys
import numpy as np
from PIL import Image, ImageDraw
from common import *

ROOT = sys.argv[1]


# ------------------------------------------------------------- gui/hitbox.png --
# The gui/ icons are all 512x512, pure white RGB with the artwork in alpha
# (verified against bubble/ham/heart/shield/potion), so this one matches that.
def hitbox():
    N = 512
    m, d = mask(N)
    n = N * SS
    w = n * 0.032
    pad = n * 0.055
    # front face
    fx0, fy0 = pad, pad + n * 0.145
    fx1, fy1 = n - pad - n * 0.145, n - pad
    off = n * 0.145
    # back face, offset up-right
    bx0, by0, bx1, by1 = fx0 + off, fy0 - off, fx1 + off, fy1 - off

    for (x0, y0, x1, y1) in ((bx0, by0, bx1, by1), (fx0, fy0, fx1, fy1)):
        d.rectangle([x0, y0, x1, y1], outline=255, width=int(round(w)))
    for (ax, ay, bx, by) in ((fx0, fy0, bx0, by0), (fx1, fy0, bx1, by0),
                             (fx0, fy1, bx0, by1), (fx1, fy1, bx1, by1)):
        thick_line(d, [(ax, ay), (bx, by)], w, caps=False)
    # corner pips, so it reads as a selection box rather than a plain cube
    r = n * 0.022
    for (px, py) in ((fx0, fy0), (fx1, fy0), (fx0, fy1), (fx1, fy1),
                     (bx0, by0), (bx1, by0), (bx1, by1)):
        d.ellipse([px - r, py - r, px + r, py + r], fill=255)
    return lum_of(shrink(m, N))


write_white_alpha(hitbox(), f'{ROOT}/gui/hitbox.png')


# ----------------------------------------------------- capes/optifine/*.png --
# Substitutes: flat colour capes named after OptiFine's donor colours rather
# than copies of OptiFine's own artwork. 64x32, the classic cape layout, with
# the cape UV block (x 0..22, y 0..17) and the 1.9 elytra block (x 22..45,
# y 0..21) filled so the file matches the shape of the capes already here.
CAPE_COLOURS = {
    'standard': (0x55, 0x5A, 0x64),
    'black':    (0x1A, 0x1A, 0x1C),
    'blue':     (0x28, 0x4E, 0xC8),
    'cyan':     (0x12, 0xA6, 0xB4),
    'gray':     (0x80, 0x80, 0x84),
    'green':    (0x2A, 0x9E, 0x3C),
    'purple':   (0x78, 0x32, 0xB4),
    'red':      (0xBE, 0x28, 0x28),
    'white':    (0xF0, 0xF0, 0xF0),
    'yellow':   (0xEB, 0xC8, 0x28),
}

os.makedirs(f'{ROOT}/capes/optifine', exist_ok=True)
for name, (r, g, b) in CAPE_COLOURS.items():
    im = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    px = im.load()
    for y in range(22):
        for x in range(46):
            # a slightly darker hem along the bottom of the cape faces
            hem = y >= 15 and x < 23
            k = 0.82 if hem else 1.0
            px[x, y] = (int(r * k), int(g * k), int(b * k), 255)
    im.save(f'{ROOT}/capes/optifine/{name}.png', optimize=True)


# ------------------------------------------------------------- textures/i.png --
# The energy-swirl overlay. CustomEnergySwirlOverlayFeatureRenderer binds it,
# scrolls the GL texture matrix and blends GL_ONE/GL_ONE, so the source factor
# ignores alpha: the artwork has to be emissive-on-black, and it has to tile
# seamlessly in both axes because the scroll wraps. Every spatial frequency
# below is an integer, which is what makes it seamless.
def swirl(n=128):
    t = (np.arange(n) + 0.5) / n
    u, v = np.meshgrid(t, t)
    TAU = 2 * math.pi
    f = (np.sin(TAU * (2 * u + 3 * v)) +
         np.sin(TAU * (3 * u - 2 * v) + 1.7) +
         np.sin(TAU * (1 * u - 4 * v) + 0.4) +
         0.6 * np.sin(TAU * (5 * u + 1 * v) + 2.3))
    f = (f - f.min()) / (f.max() - f.min())
    # thin bright filaments
    band = np.clip((f - 0.62) / 0.26, 0, 1) ** 1.6
    haze = np.clip((f - 0.30) / 0.45, 0, 1) ** 2.2 * 0.28
    i = np.clip(band + haze, 0, 1)
    # blue-cyan body going white in the core, like a charged-creeper swirl
    base = np.array([0.22, 0.52, 1.00])
    rgb = base[None, None, :] * i[..., None] + (1.0 - base)[None, None, :] * (i ** 4)[..., None]
    return np.clip(rgb, 0, 1), i


rgb, a = swirl()
# Alpha is unused by the additive path, but carrying the intensity there keeps
# the file sane if anything ever draws it with a normal blend func.
write_rgba(rgb, np.clip(a * 1.2, 0, 1), f'{ROOT}/i.png')
print('misc ok')
