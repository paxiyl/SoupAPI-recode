"""The first-person rig every Arcana weapon shares.

    root
     |- idle_view            the eye (model origin); the mod places this at the camera
     |- camera               swings bake their kick into this bone's rotation
     |- lefthand_pos         a one-handed weapon's free left arm, parked below the view
     '- swing                pivots at the right shoulder: big arcs move arm and weapon together
         '- hold             rest rotation puts the weapon in the hand, pointing where it should
             '- wrist        the hand and the weapon together: cuts, turns, sighting along it
                 |- righthand_pos    the player's right arm, its fist on the grip
                 |- lefthand_pos     a two-handed weapon's left arm, on the shaft below the right
                 '- weapon           the weapon turning in the fingers: flips, twirls, tosses
                     '- ...          the weapon's own parts, built in weapon-local axes from the grip

This is how the knives in the delta pack are put together, and it is most of why they feel held:
their weapon and their right hand are siblings under one group, so anything that moves the group
moves both, while the weapon can still spin in the fingers on its own bone. The first version of
this rig hung the arm off the shoulder instead, so every wrist flick and every twirl turned the
weapon and left the hand where it was.

The arm boxes are the vanilla 4x12x4 arm: the fist is the low-Y face, the shoulder the high-Y face.

Axes: `swing` arcs about the shoulder (+x pitches down, +y yaws right, +z rolls anticlockwise);
`wrist` and `weapon` turn about the grip (+x tips the blade away, +y twists it about its length,
+z leans it left).
"""
import numpy as np

from geo import solve_rest, solve_rest_under


def eye_to_bedrock(p):
    """Eye space (blocks; +x right, +y up, -z forward) to Bedrock model units."""
    return (-p[0] * 16.0, p[1] * 16.0, p[2] * 16.0)


class Rig:
    def __init__(self, model, grip_eye, blade_dir_eye, face_normal_eye, shoulder_eye=(0.36, -0.7, 0.3),
                 left_shoulder_eye=None, left_grip_local=None, left_rest_eye=(-0.42, -1.02, -0.62)):
        self.model = model
        self.G = eye_to_bedrock(grip_eye)
        self.S = eye_to_bedrock(shoulder_eye)
        m = model
        m.bone('root', None, (0, 0, 0))
        m.bone('idle_view', 'root', (0, 0, 0))
        m.bone('camera', 'root', (0, 0, 0))
        m.bone('swing', 'root', self.S)

        hold_rest, err = solve_rest(self.G, blade_dir_eye, face_normal_eye)
        self.hold_rest = hold_rest
        self.hold_err = err
        m.bone('hold', 'swing', self.G, hold_rest)
        m.bone('wrist', 'hold', self.G)
        m.bone('weapon', 'wrist', self.G)

        # Right arm: fist at the grip, the box's +Y aimed back at the shoulder. It rides the wrist,
        # so its rest is found through the hold's rotation.
        arm_dir = np.array(shoulder_eye, float) - np.array(grip_eye, float)
        arm_rest, _ = solve_rest_under(hold_rest, arm_dir, (0.0, 0.3, 1.0))
        m.bone('righthand_pos', 'wrist', self.G, arm_rest)
        gx, gy, gz = self.G
        m.cube('righthand_pos', (gx - 2, gy - 2.0, gz - 2), (4, 12, 4), m.mats['arm'])

        self.two_handed = left_shoulder_eye is not None and left_grip_local is not None
        if self.two_handed:
            # The left fist on the shaft below the right, riding the wrist too, so a two-handed
            # weapon is carried by both hands through every cut.
            from geo import bone_local, eye_space
            lg = (gx + left_grip_local[0], gy + left_grip_local[1], gz + left_grip_local[2])
            chain = [bone_local(self.S, (0, 0, 0)), bone_local(self.G, hold_rest)]
            left_eye = eye_space(chain, lg)
            ldir = np.array(left_shoulder_eye, float) - left_eye
            lrest, _ = solve_rest_under(hold_rest, ldir, (0.0, 0.3, 1.0))
            m.bone('lefthand_pos', 'wrist', lg, lrest)
            m.cube('lefthand_pos', (lg[0] - 2, lg[1] - 2.0, lg[2] - 2), (4, 12, 4), m.mats['arm'])
            self.left_grip = lg
        else:
            # A one-handed weapon's left arm is free: parked below the bottom of the view, where
            # it waits until an inspect brings it up to turn the weapon over or run along it.
            lp = eye_to_bedrock(left_rest_eye)
            ldir = np.array((-0.44, -1.10, -0.10), float) - np.array(left_rest_eye, float)
            lrest, _ = solve_rest(lp, ldir, (0.0, 0.3, 1.0))
            m.bone('lefthand_pos', 'root', lp, lrest)
            m.cube('lefthand_pos', (lp[0] - 2, lp[1] - 2.0, lp[2] - 2), (4, 12, 4), m.mats['arm'])
            self.left_grip = None
        self.left = True

    def at(self, local):
        """A weapon-local point (grip at the origin, blade along +Y) in absolute model units."""
        return (self.G[0] + local[0], self.G[1] + local[1], self.G[2] + local[2])


def shrink(model, groups, rig, k):
    """Scales everything that hangs off `weapon` about the grip, leaving the arms alone.

    These weapons are drawn at the size of the concept art, two blocks and more, and held at that
    size they fill the screen. The pack placement used to fix that by shrinking the whole view
    model - which shrank the player's arms with it, down to a stub poking in from the corner. The
    knives hold a small weapon in a full-size hand; so does this, by building the weapon small.
    """
    G = rig.G
    under = set()
    names = {b['name']: b for b in model.bones}

    def descends(name):
        while name in names:
            if name == 'weapon':
                return True
            name = names[name].get('parent')
        return False

    for b in model.bones:
        if b['name'] != 'weapon' and descends(b['name']):
            under.add(b['name'])
            b['pivot'] = [round(G[i] + (b['pivot'][i] - G[i]) * k, 4) for i in range(3)]
    for bone, meshes in groups.items():
        for mesh in meshes:
            mesh.translate(-G[0], -G[1], -G[2])
            mesh.scale(k)
            mesh.translate(G[0], G[1], G[2])
    rig.scale = k
    return under
