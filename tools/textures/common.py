"""Shared helpers for generating the SoupAPI textures.

Two conventions are used, matching the ones already in the repository:

  * "base"    - fully opaque, greyscale art on black. Drawn with additive
                blending (SRC_ALPHA, ONE), so black adds nothing.
  * "_unblack"- the same art with RGB forced to white and alpha set to the
                base's luminance. Verified against the committed
                hexagon/circle/portal pairs (correlation 1.0000).
"""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

SS = 4  # supersampling factor


def mask(n):
    """A supersampled 8-bit mask and its draw handle."""
    img = Image.new('L', (n * SS, n * SS), 0)
    return img, ImageDraw.Draw(img)


def shrink(img, n):
    return img.resize((n, n), Image.LANCZOS)


def lum_of(img):
    """Mask image -> float luminance array in [0, 1]."""
    return np.asarray(img).astype(np.float32) / 255.0


def write_base(lum, path):
    """Opaque greyscale-on-black RGB png."""
    v = np.clip(lum, 0, 1)
    rgb = (v * 255.0 + 0.5).astype(np.uint8)
    Image.merge('RGB', [Image.fromarray(rgb)] * 3).save(path, optimize=True)


def write_unblack(lum, path):
    """White RGB, alpha = luminance."""
    v = np.clip(lum, 0, 1)
    a = (v * 255.0 + 0.5).astype(np.uint8)
    w = np.full_like(a, 255)
    Image.merge('RGBA', [Image.fromarray(x) for x in (w, w, w, a)]).save(path, optimize=True)


def write_white_alpha(lum, path):
    """Same bytes as write_unblack; named for textures whose *base* form is
    white-with-alpha because a non-additive blend func can reach them."""
    write_unblack(lum, path)


def write_rgba(rgb, alpha, path):
    """rgb: float HxWx3 in [0,1]; alpha: float HxW in [0,1]."""
    chans = [Image.fromarray((np.clip(rgb[..., i], 0, 1) * 255 + 0.5).astype(np.uint8)) for i in range(3)]
    chans.append(Image.fromarray((np.clip(alpha, 0, 1) * 255 + 0.5).astype(np.uint8)))
    Image.merge('RGBA', chans).save(path, optimize=True)


def grid(n):
    """Centred coordinate grid in [-1, 1], plus radius and angle."""
    t = (np.arange(n) + 0.5) / n * 2.0 - 1.0
    x, y = np.meshgrid(t, t)
    return x, y, np.hypot(x, y), np.arctan2(y, x)


def poly(cx, cy, r, sides, rot=0.0):
    return [(cx + r * math.cos(rot + i * 2 * math.pi / sides),
             cy + r * math.sin(rot + i * 2 * math.pi / sides)) for i in range(sides)]


def star_points(cx, cy, outer, inner, points=5, rot=-math.pi / 2):
    pts = []
    for i in range(points * 2):
        r = outer if i % 2 == 0 else inner
        a = rot + i * math.pi / points
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def thick_line(d, pts, width, fill=255, caps=True):
    d.line(pts, fill=fill, width=int(round(width)), joint='curve')
    if caps:
        r = width / 2.0
        for (px, py) in (pts[0], pts[-1]):
            d.ellipse([px - r, py - r, px + r, py + r], fill=fill)


def load_font(size, bold=True):
    candidates = [
        '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
        '/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf',
        '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',
    ]
    if not bold:
        candidates.insert(0, '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
    for c in candidates:
        try:
            return ImageFont.truetype(c, size)
        except OSError:
            continue
    raise RuntimeError('no usable font')
