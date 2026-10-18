# Bedrock model checking

The mod's Bedrock model and animation code (`me.Padej_.soupapi.bedrock`) was written against these
scripts: the same maths, in Python, with a software rasteriser, so a posed model could be rendered
to a PNG and looked at before any of it shipped. That is how the mirrored X axis was found, how the
cube-rotation handedness was pinned down against the pack's own slot icons, and how the first-person
anchor below was checked across all eleven weapons at once.

They are kept because the next pack that renders wrong is quicker to diagnose here than in game.

    python3 -I fp.py <unpacked-pack-root> out.png [clip] [time]
    python3 -I render.py <unpacked-pack-root> <weapon> <clip> <time> out.png [yaw pitch]

`fp.py` draws every weapon in a pack as a first-person contact sheet, placed the way the mod places
it. `render.py` draws one weapon from a chosen angle. `bedrock.py` is the parser and the pose
evaluator, and it is the file to keep in step with the Java: a convention that differs between the
two means one of them is drawing a shape the other never sees.

The pack root is the unpacked pack — the folder holding `assets/` and `gunpack.meta.json`. Nothing
here reads the mod or the game; it only needs `numpy` and `pillow`.

## What the conventions are

- A **bone**'s rotation keeps its signs. A **cube**'s rotation is the opposite handedness: negate X
  and Y, keep Z. Both compose Z, then Y, then X. Verified by rendering against the pack's own
  `textures/melee/slot/*.png` icons, which are the artist's own renders of the same models.
- One unit is a sixteenth of a block, authored, not fitted. The hand markers are the proof: each is
  a 4×12×4 box, which is a vanilla player arm exactly.
- The eye is the `idle_view` bone's pivot, and X is mirrored. Measured from there, every weapon in
  this pack puts its right wrist 0.5 to 1.1 blocks below the eye and 1.1 to 1.7 in front of it.
  `camera` is a shake anchor, not a position: its pivot differs by sixty units between weapons in
  the same pack, and the animations only ever move it by a degree or two.
