# Bedrock model checking

The mod's Bedrock model and animation code (`me.Padej_.soupapi.bedrock`) was written against these
scripts: the same maths, in Python, with a software rasteriser, so a posed model could be rendered
to a PNG and looked at before any of it shipped. That is how the mirrored X axis was found, and how
the pack's disagreeing view anchors were found — neither is visible from the JSON.

They are kept because the next pack that renders wrong is quicker to diagnose here than in game.

    python3 -I fp4.py <unpacked-pack-root> out.png [clip] [time]

`fp4.py` draws every weapon in a pack as a contact sheet, posed and fitted the way the mod fits it.
`render.py` draws one weapon from six angles. `bedrock.py` is the parser and the pose evaluator.

The pack root is the unpacked pack — the folder holding `assets/` and `gunpack.meta.json`. Nothing
here reads the mod or the game; it only needs `numpy` and `pillow`.
