"""The first-person rig every Arcana weapon shares.

    root
     |- idle_view            the eye (model origin); the mod places this at the camera
     |- camera               swings bake their kick into this bone's rotation
     '- swing                pivots at the right shoulder: big arcs move arm and weapon together
         |- righthand_pos    the player's right arm, aimed from shoulder to grip (drawn by the mod)
         |- lefthand_pos     optional left arm, for two-handed weapons
         '- hold             rest rotation puts the weapon in the hand, pointing where it should
             '- weapon       wrist twists and flicks, about the grip
                 '- ...      the weapon's own parts, built in weapon-local axes from the grip

The arm boxes are the vanilla 4x12x4 arm: the fist is the low-Y face, the shoulder the high-Y face.
"""
import numpy as np

from geo import solve_rest


def eye_to_bedrock(p):
    """Eye space (blocks; +x right, +y up, -z forward) to Bedrock model units."""
    return (-p[0] * 16.0, p[1] * 16.0, p[2] * 16.0)


class Rig:
    def __init__(self, model, grip_eye, blade_dir_eye, face_normal_eye, shoulder_eye=(0.36, -0.7, 0.3),
                 left_shoulder_eye=None, left_grip_local=None):
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
        m.bone('weapon', 'hold', self.G)

        # Right arm: fist at the grip, the box's +Y aimed back at the shoulder.
        arm_dir = np.array(shoulder_eye, float) - np.array(grip_eye, float)
        arm_rest, _ = solve_rest(self.G, arm_dir, (0.0, 0.3, 1.0))
        m.bone('righthand_pos', 'swing', self.G, arm_rest)
        gx, gy, gz = self.G
        m.cube('righthand_pos', (gx - 2, gy - 2.0, gz - 2), (4, 12, 4), m.mats['arm'])

        self.left = False
        if left_shoulder_eye is not None and left_grip_local is not None:
            # The left fist on the handle, below the right: found by running the rest rotation.
            from geo import bone_local, eye_space
            lg = (gx + left_grip_local[0], gy + left_grip_local[1], gz + left_grip_local[2])
            chain = [bone_local(self.S, (0, 0, 0)), bone_local(self.G, hold_rest)]
            left_eye = eye_space(chain, lg)
            ldir = np.array(left_shoulder_eye, float) - left_eye
            lrest, _ = solve_rest(lg, ldir, (0.0, 0.3, 1.0))
            lgb = eye_to_bedrock(left_eye)
            m.bone('lefthand_pos', 'swing', lgb, lrest)
            m.cube('lefthand_pos', (lgb[0] - 2, lgb[1] - 2.0, lgb[2] - 2), (4, 12, 4), m.mats['arm'])
            self.left = True

    def at(self, local):
        """A weapon-local point (grip at the origin, blade along +Y) in absolute model units."""
        return (self.G[0] + local[0], self.G[1] + local[1], self.G[2] + local[2])
