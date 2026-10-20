# Texture generators

The 39 textures the code referenced but that were never committed upstream are generated
by these scripts, so the artwork can be regenerated or tweaked rather than only edited as
pixels. They need Python 3 with `pillow` and `numpy`.

```sh
A=src/main/resources/assets/soupapi/textures
cd tools/textures
python3 particles.py "../../$A/particle"
python3 glyphs.py    "../../$A/particle/glyph"
python3 circles.py   "../../$A/jump_circles"
python3 misc.py      "../../$A"            # gui/hitbox.png, capes/optifine/*, i.png
```

`common.py` holds the two pixel conventions the repository already used — opaque
greyscale-on-black "base" art for additive blending, and the `_unblack` form with RGB
forced to white and alpha set to the base's luminance. See the "Texture assets added by
this port" section of `PORTING.md` for which files depart from them and why.
