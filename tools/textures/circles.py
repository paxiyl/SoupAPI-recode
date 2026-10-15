"""textures/jump_circles/{portal_2,soup,soup_unblack}.png

portal_2 is a HitBubbles style; soup is a JumpCircles style. Both are bound
under additive blending (SRC_ALPHA, ONE), so both follow the committed
convention of the sibling files: opaque greyscale on black. soup_unblack is
derived from soup with the verified transform.
"""
import math, os, sys
import numpy as np
from PIL import Image, ImageFilter
from common import *

OUT = sys.argv[1]
N = 1024
os.makedirs(OUT, exist_ok=True)


def bloom(lum, s1=N / 256.0, s2=N / 64.0, k1=0.55, k2=0.35):
    img = Image.fromarray((np.clip(lum, 0, 1) * 255).astype(np.uint8))
    b1 = np.asarray(img.filter(ImageFilter.GaussianBlur(s1))).astype(np.float32) / 255.0
    b2 = np.asarray(img.filter(ImageFilter.GaussianBlur(s2))).astype(np.float32) / 255.0
    return np.clip(lum + k1 * b1 + k2 * b2, 0, 1)


# ------------------------------------------------------------------ portal_2 --
# A twin-arm logarithmic vortex inside a broken outer ring - a sibling to the
# committed portal.png, which is a single-arm spiral.
def portal_2():
    x, y, r, th = grid(N)
    r = np.maximum(r, 1e-4)
    out = np.zeros_like(r)

    # two spiral arms
    phase = 2.0 * th + 7.5 * np.log(r)
    s = np.cos(phase)
    arm = np.clip((s - 0.55) / 0.45, 0, 1) ** 1.4
    window = np.clip((r - 0.10) / 0.16, 0, 1) * np.clip((0.86 - r) / 0.18, 0, 1)
    out += arm * window * 0.95

    # a thinner counter-rotating pair, dimmer
    s2 = np.cos(-3.0 * th + 5.0 * np.log(r))
    out += np.clip((s2 - 0.86) / 0.14, 0, 1) ** 1.6 * window * 0.45

    # outer ring, broken into 24 segments
    ring = np.exp(-((r - 0.925) / 0.012) ** 2)
    seg = np.cos(24 * th)
    out += ring * np.clip((seg + 0.35) / 0.5, 0, 1) * 1.0
    out += np.exp(-((r - 0.80) / 0.006) ** 2) * 0.55

    # core glow
    out += np.exp(-(r / 0.085) ** 2) * 0.9
    out *= np.clip((1.0 - r) / 0.05, 0, 1)
    return bloom(np.clip(out, 0, 1))


# ---------------------------------------------------------------------- soup --
# A rune ring around the mod's own motif: a bowl with rising steam.
def soup():
    m, d = mask(N)
    n = N * SS
    c = n / 2.0
    w = n * 0.013

    # outer ring + inner tick ring
    for rad, width in ((0.955, 1.0), (0.885, 0.55)):
        rr = c * rad
        d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=255, width=int(round(w * width)))
    ticks = 36
    for i in range(ticks):
        a = i * 2 * math.pi / ticks
        r0, r1 = c * 0.895, c * 0.945
        thick_line(d, [(c + r0 * math.cos(a), c + r0 * math.sin(a)),
                       (c + r1 * math.cos(a), c + r1 * math.sin(a))], w * 0.7, caps=False)

    # bowl: a half-ellipse with a foot
    bw, bh = c * 0.62, c * 0.40
    by = c + c * 0.05
    d.arc([c - bw, by - bh, c + bw, by + bh], start=0, end=180, fill=255, width=int(round(w * 1.6)))
    thick_line(d, [(c - bw, by), (c + bw, by)], w * 1.6)
    # soup surface
    d.arc([c - bw * 0.86, by - bh * 0.22, c + bw * 0.86, by + bh * 0.22],
          start=180, end=360, fill=255, width=int(round(w * 1.1)))
    # foot
    thick_line(d, [(c - bw * 0.34, by + bh * 1.02), (c + bw * 0.34, by + bh * 1.02)], w * 1.4)

    # three steam curls
    for k, sx in enumerate((-0.34, 0.0, 0.34)):
        pts = []
        amp = c * 0.085
        top = c * (0.50 if k == 1 else 0.40)
        for i in range(61):
            t = i / 60.0
            yy = by - bh * 0.30 - t * top
            xx = c + c * sx + amp * math.sin(t * math.pi * 2.1 + k)
            pts.append((xx, yy))
        d.line(pts, fill=255, width=int(round(w * 1.15)), joint='curve')

    return bloom(lum_of(shrink(m, N)), k1=0.6, k2=0.4)


p2 = portal_2()
write_base(p2, f'{OUT}/portal_2.png')

sp = soup()
write_base(sp, f'{OUT}/soup.png')
write_unblack(sp, f'{OUT}/soup_unblack.png')
print('circles ok')
