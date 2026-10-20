"""Draw the player's arm from Minecraft's own shoulder to the pack's wrist.

The pack's hand markers cannot be used as arms directly: their shoulder end sits at eye level
with the arm reaching down, where Minecraft's first-person shoulder is two thirds of a block
lower with the arm reaching up. Pinning one onto the other either grows an arm out of the middle
of the screen (pack anchor) or drops the whole weapon off the bottom (vanilla anchor) -- both
were rendered and both are wrong.

So only the shoulder is taken from Minecraft and only the wrist from the pack, and the arm is
drawn between them. The shoulder then enters the frame from the lower corner the way every other
held item does, and the hand is on the grip because that is where the pack posed it.
"""
import numpy as np

# ItemRenderer.func_178095_a + RenderPlayer.renderRightArm, swing 0, equip 0: where 1.8.9 puts
# the first-person shoulder and which way the arm's flat side faces.
SHOULDER = np.array([0.4853, -0.8315, -0.5254])
SIDE = np.array([-0.36795, -0.73329, -0.57175])   # the arm box's own +X in view space
HALF = 0.125           # a player arm is four units across


def frame(shoulder, wrist):
    """An orthonormal basis with +Y running shoulder -> wrist, rolled like vanilla's arm."""
    y = wrist - shoulder
    length = float(np.linalg.norm(y))
    if length < 1e-5:
        return None, 0.0
    y = y / length
    x = SIDE - y * float(np.dot(SIDE, y))
    if np.linalg.norm(x) < 1e-4:
        x = np.array([1.0, 0.0, 0.0]) - y * y[0]
    x /= np.linalg.norm(x)
    z = np.cross(x, y)
    return np.column_stack([x, y, z]), length


def box(shoulder, wrist, half=HALF):
    """The arm as six quads, as (4 view-space points, normal) pairs."""
    basis, length = frame(shoulder, wrist)
    if basis is None:
        return []
    def at(u, v, w):
        return shoulder + basis @ np.array([u * half, v * length, w * half])
    faces = [
        ([(-1, 0, -1), (1, 0, -1), (1, 1, -1), (-1, 1, -1)], (0, 0, -1)),
        ([(1, 0, 1), (-1, 0, 1), (-1, 1, 1), (1, 1, 1)], (0, 0, 1)),
        ([(-1, 0, 1), (-1, 0, -1), (-1, 1, -1), (-1, 1, 1)], (-1, 0, 0)),
        ([(1, 0, -1), (1, 0, 1), (1, 1, 1), (1, 1, -1)], (1, 0, 0)),
        ([(-1, 1, -1), (1, 1, -1), (1, 1, 1), (-1, 1, 1)], (0, 1, 0)),
        ([(-1, 0, 1), (1, 0, 1), (1, 0, -1), (-1, 0, -1)], (0, -1, 0)),
    ]
    out = []
    for corners, normal in faces:
        out.append(([at(*c) for c in corners], basis @ np.array(normal, float)))
    return out
