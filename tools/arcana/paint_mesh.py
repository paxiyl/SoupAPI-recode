"""Material tiles for the mesh weapons.

A mesh part carries a material name, not coordinates; `mesh.Atlas` gives every material a tile of
the texture and rewrites the part's UVs into it. This paints those tiles — one procedural swatch
each, plus the matching glow mask so the emissive pass knows what is meant to be lit.

Each swatch is painted to be read at any scale: the UVs that land on it run along a part's length,
so a tile's vertical axis is "from the root to the tip" and its horizontal axis is "around". That
is why the gradients here run the way they do.
"""
import numpy as np

import paint


def _noise(w, h, beta, seed):
    return paint.periodic_noise(w, h, beta, seed)


def metal(c, x, y, w, h, tone, seed, polish=0.55, scratches=0.5, glow=0.0):
    """Worked metal: a soft length-wise sheen, grain, and fine scratches across it."""
    along = np.linspace(0, 1, h)[:, None]
    around = np.linspace(0, 1, w)[None, :]
    # A highlight band around the form, which is what makes a tube read as round.
    sheen = np.exp(-((around - 0.3) ** 2) / 0.05) * polish
    sheen += np.exp(-((around - 0.78) ** 2) / 0.02) * polish * 0.5
    grain = _noise(w, h, 1.8, seed)
    wear = _noise(w, h, 2.6, seed + 11)
    shade = 0.55 + sheen + 0.30 * grain + 0.12 * (1 - along)
    scratch = (wear > 0.72).astype(np.float32) * scratches * (0.35 + 0.65 * _noise(w, h, 3.0, seed + 5))
    rgb = np.asarray(tone, float)[None, None, :] * (shade - scratch * 0.25)[..., None]
    rgb += scratch[..., None] * 0.22
    c.put(x, y, np.clip(rgb, 0, 1),
          glow=np.full((h, w), glow, np.float32) if glow else None)


def flesh(c, x, y, w, h, base, vein, seed, glow=0.0):
    """Sclera: a wet off-white with blood vessels crawling in from the rim."""
    around = np.linspace(-1, 1, w)[None, :]
    along = np.linspace(-1, 1, h)[:, None]
    r = np.sqrt(around ** 2 + along ** 2)
    body = np.asarray(base, float)[None, None, :] * (1.06 - 0.22 * r)[..., None]
    v = _noise(w, h, 2.9, seed)
    # Vessels: thin ridges of the noise, kept to the outside of the eye.
    lines = np.exp(-((v - 0.5) ** 2) / 0.0012) * np.clip((r - 0.25) * 1.6, 0, 1)
    body = body * (1 - 0.75 * lines[..., None]) + np.asarray(vein, float)[None, None, :] * (0.75 * lines)[..., None]
    gloss = np.exp(-(((around - 0.35) ** 2 + (along + 0.35) ** 2)) / 0.06) * 0.5
    body += gloss[..., None]
    c.put(x, y, np.clip(body, 0, 1), glow=np.full((h, w), glow, np.float32) if glow else None)


def iris(c, x, y, w, h, inner, outer, seed, glow=0.9):
    """An iris: fibres running out from the pupil, hot at the centre."""
    around = np.linspace(-1, 1, w)[None, :]
    along = np.linspace(-1, 1, h)[:, None]
    r = np.clip(np.sqrt(around ** 2 + along ** 2), 0, 1)
    a = np.arctan2(along, around)
    fibres = 0.5 + 0.5 * np.sin(a * 36 + _noise(w, h, 2.2, seed) * 8)
    t = r ** 0.7
    rgb = (np.asarray(inner, float)[None, None, :] * (1 - t)[..., None]
           + np.asarray(outer, float)[None, None, :] * t[..., None])
    rgb *= (0.72 + 0.5 * fibres * (0.3 + 0.7 * r))[..., None]
    rgb *= (1.5 - 0.6 * r)[..., None]
    dark = np.clip((r - 0.86) * 7, 0, 1)
    rgb *= (1 - 0.85 * dark)[..., None]
    g = np.clip((1 - r) ** 1.4 * glow, 0, 1)
    c.put(x, y, np.clip(rgb, 0, 1), glow=g.astype(np.float32))


def stone(c, x, y, w, h, tone, seed, glow=0.45):
    """A cut gem: facet shading and a bright core, so a faceted mesh still reads as lit glass."""
    around = np.linspace(-1, 1, w)[None, :]
    along = np.linspace(-1, 1, h)[:, None]
    r = np.sqrt(around ** 2 + along ** 2)
    facet = _noise(w, h, 1.2, seed)
    rgb = np.asarray(tone, float)[None, None, :] * (0.45 + 0.9 * facet + 0.5 * (1 - r))[..., None]
    spark = np.exp(-(((around + 0.3) ** 2 + (along - 0.3) ** 2)) / 0.02)
    rgb += spark[..., None] * 0.6
    c.put(x, y, np.clip(rgb, 0, 1), glow=np.clip((1 - r) * glow + spark * 0.4, 0, 1).astype(np.float32))


def sinew(c, x, y, w, h, tone, seed, glow=0.0, wet=0.5):
    """A tendril: ropey length-wise strands with a wet highlight down one side."""
    around = np.linspace(0, 1, w)[None, :]
    strands = 0.5 + 0.5 * np.sin(around * np.pi * 2 * 5 + _noise(w, h, 2.4, seed) * 3)
    grain = _noise(w, h, 2.0, seed + 2)
    shade = 0.45 + 0.45 * strands + 0.25 * grain
    rgb = np.asarray(tone, float)[None, None, :] * shade[..., None]
    gloss = np.exp(-((around - 0.32) ** 2) / 0.012) * wet
    rgb += gloss[..., None] * 0.45
    c.put(x, y, np.clip(rgb, 0, 1), glow=np.full((h, w), glow, np.float32) if glow else None)


def liquid(c, x, y, w, h, tone, seed, glow=0.08):
    """Blood: deep, glossy, darker at the edges."""
    around = np.linspace(-1, 1, w)[None, :]
    body = np.broadcast_to(np.asarray(tone, float)[None, None, :], (h, w, 3)).copy()
    body = body * (0.55 + 0.5 * (1 - np.abs(around)))[..., None]
    body = body * (0.85 + 0.3 * _noise(w, h, 1.6, seed))[..., None]
    gloss = np.exp(-((around + 0.4) ** 2) / 0.02) * 0.55
    body += gloss[..., None]
    c.put(x, y, np.clip(body, 0, 1), glow=np.full((h, w), glow, np.float32))


def runes(c, x, y, w, h, tone, mark, seed, glow=0.5):
    """A runed band: metal with cut marks that catch light."""
    metal(c, x, y, w, h, tone, seed, polish=0.35, scratches=0.3)
    base = c.rgb[y:y + h, x:x + w].copy()
    g = np.zeros((h, w), np.float32)
    rng = np.random.default_rng(seed)
    for _ in range(max(3, w // 10)):
        cx = rng.integers(4, max(5, w - 4))
        cy = rng.integers(4, max(5, h - 4))
        hgt = int(rng.integers(5, max(6, h // 3)))
        wid = int(rng.integers(3, 6))
        for k in range(rng.integers(2, 4)):
            ax = int(cx + rng.integers(-wid, wid + 1))
            ay = int(cy + rng.integers(-hgt // 2, hgt // 2 + 1))
            bx = int(np.clip(ax + rng.integers(-wid, wid + 1), 0, w - 1))
            by = int(np.clip(ay + rng.integers(-hgt // 2, hgt // 2 + 1), 0, h - 1))
            steps = max(abs(bx - ax), abs(by - ay), 1)
            for s in range(steps + 1):
                px = int(np.clip(ax + (bx - ax) * s / steps, 0, w - 1))
                py = int(np.clip(ay + (by - ay) * s / steps, 0, h - 1))
                base[py, px] = mark
                g[py, px] = 1.0
    c.put(x, y, base, glow=np.clip(g * glow, 0, 1))


def feather(c, x, y, w, h, root, tip, seed, glow=0.0):
    """A feather vane: barbs angled off a central rib, shading from root to tip."""
    around = np.linspace(-1, 1, w)[None, :]
    along = np.linspace(0, 1, h)[:, None]
    barbs = 0.5 + 0.5 * np.sin((np.abs(around) * 9 - along * 14) * np.pi)
    rib = np.exp(-(around ** 2) / 0.004)
    rgb = (np.asarray(root, float)[None, None, :] * (1 - along)[..., None]
           + np.asarray(tip, float)[None, None, :] * along[..., None])
    rgb *= (0.62 + 0.45 * barbs * (0.4 + 0.6 * np.abs(around)))[..., None]
    rgb += rib[..., None] * 0.25
    rgb *= (1 - 0.5 * np.clip((np.abs(around) - 0.82) * 6, 0, 1))[..., None]
    c.put(x, y, np.clip(rgb, 0, 1), glow=np.full((h, w), glow, np.float32) if glow else None)


def energy(c, x, y, w, h, core, edge, seed, glow=1.0):
    """A blade of light: a white-hot spine fading out into colour, with flame wisps in it."""
    around = np.linspace(-1, 1, w)[None, :]
    along = np.linspace(0, 1, h)[:, None]
    flame = _noise(w, h, 2.2, seed)
    spine = np.exp(-(around ** 2) / (0.03 + 0.08 * along))
    body = np.clip(1 - np.abs(around) ** 1.3, 0, 1) * (0.55 + 0.45 * flame)
    rgb = (np.asarray(core, float)[None, None, :] * spine[..., None]
           + np.asarray(edge, float)[None, None, :] * body[..., None])
    rgb *= (1.15 - 0.35 * along)[..., None]
    c.put(x, y, np.clip(rgb, 0, 1),
          glow=np.clip((spine + body * 0.8) * glow, 0, 1).astype(np.float32))


def tile(c, atlas, name, size, fn, *args, **kwargs):
    """Paints one material's rectangle, given the atlas layout in UV space."""
    rect = atlas.rects.get(name)
    if rect is None:
        return
    x = int(round(rect[0] * size))
    y = int(round(rect[1] * size))
    w = int(round(rect[2] * size))
    h = int(round(rect[3] * size))
    fn(c, x, y, w, h, *args, **kwargs)
