"""Caps the mod's textures at a sensible size for how they are drawn.

The artwork came over from the 1.21.4 mod at the size it was authored, not the size it is used: the
MCTiers icons are 3375x3375 for an eighteen-pixel HUD icon, and the capes are 2048x1024 for a region
a player model samples at 10x16. A GPU does not care what a PNG weighs, it cares what the texture
costs once it is uploaded - four bytes a pixel, every pixel - and the set as shipped came to over
500 MB of texture memory. On a phone running gl4es that is not a download problem, it is a will-it-
run problem.

Each group below is capped at a few times the largest size it is ever drawn at, which leaves room
for GUI scale 4 and for a ring filling the screen, and is still invisible against the originals.

    python3 tools/shrink_textures.py [--dry-run]
"""
import glob
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'src/main/resources/assets/soupapi/textures')

# folder -> longest edge allowed
CAPS = [
    ('gui/mctiers', 128),      # drawn at 16-18 px in the watermark
    ('capes', 512),            # the cape region of a 64x32 skin layout
    ('jump_circles', 512),     # a ring on the ground, a few blocks across
    ('target_render', 512),    # a sprite around the target
]


def main(argv):
    dry = '--dry-run' in argv
    saved_pixels = 0
    changed = 0
    for folder, cap in CAPS:
        pattern = os.path.join(ROOT, folder, '**', '*.png')
        for path in sorted(glob.glob(pattern, recursive=True)):
            image = Image.open(path)
            width, height = image.size
            longest = max(width, height)
            if longest <= cap:
                continue
            scale = cap / float(longest)
            size = (max(1, int(round(width * scale))), max(1, int(round(height * scale))))
            saved_pixels += width * height - size[0] * size[1]
            changed += 1
            print('%-52s %sx%s -> %sx%s' % (os.path.relpath(path, ROOT), width, height,
                                            size[0], size[1]))
            if dry:
                continue
            resized = image.convert('RGBA').resize(size, Image.LANCZOS)
            resized.save(path, optimize=True)
    print('%d files, %.0f MB of texture memory saved'
          % (changed, saved_pixels * 4 / 1048576.0))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
