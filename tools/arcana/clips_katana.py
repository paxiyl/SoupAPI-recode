"""The Fractured-Eye Katana's clips.

Bones: `swing` (arcs about the shoulder: +x pitches down, +y yaws right, +z rolls anticlockwise),
`weapon` (about the grip: +x tips the blade away, +y twists it, +z leans it left), `camera` (kick),
`lefthand_pos` (the second hand lets go for the showier moves), the secondary blades for flex and the
eyes' irises for darting. Sounds are this pack's own (see sounds.py).
"""
import numpy as np

from anim import Clip
from geo import bone_local

SND = 'arcana:melee/fractured_eye_katana/'
EYES = 17


def hand_off(c, t0, t1, t2, t3):
    """The left hand drops away between t1 and t2, returning by t3."""
    c.pose(t0, {'lefthand_pos': {'pos': (0, 0, 0)}})
    c.pose(t1, {'lefthand_pos': {'pos': (0, -22, 6)}})
    c.pose(t2, {'lefthand_pos': {'pos': (0, -22, 6)}})
    c.pose(t3, {'lefthand_pos': {'pos': (0, 0, 0)}})


def eyes_dart(c, t0, t1, seed, amount=0.35, steps=6):
    rng = np.random.default_rng(seed)
    for k in range(EYES):
        bone = 'eye_%d_iris_illuminated' % k
        for i in range(steps + 1):
            t = t0 + (t1 - t0) * i / steps
            off = (0, 0, 0) if i in (0, steps) else (rng.uniform(-amount, amount), rng.uniform(-amount, amount) * 0.6, 0)
            c.key(bone, 'position', t, off, smooth=False)
            s = 1.0 if i in (0, steps) else rng.uniform(0.8, 1.35)
            c.key(bone, 'scale', t, (s, s, 1), smooth=False)


def kick(c, t, amount):
    c.pose(t - 0.04, {'camera': {'rot': (0, 0, 0)}})
    c.pose(t, {'camera': {'rot': (amount, amount * 0.4, -amount * 0.3)}})
    c.pose(t + 0.18, {'camera': {'rot': (0, 0, 0)}})


def portal_offset(rig, p_eye, swing_rot, swing_pos):
    """The `weapon` position offset that puts the grip at an eye-space point while the swing bone
    holds the given pose: solved through the whole chain, swing then hold."""
    M = bone_local(rig.S, (0, 0, 0), swing_pos, swing_rot) @ bone_local(rig.G, rig.hold_rest)
    g = np.array([-rig.G[0], rig.G[1], rig.G[2], 1.0])
    target = np.append(np.array(p_eye) * 16.0, 1.0)
    t = (np.linalg.inv(M) @ target)[:3] - g[:3]
    return (-t[0], t[1], t[2])


def build(rig):
    out = []

    idle = Clip('static_idle', 4.0, loop=True)
    for t, ph in ((0, 0), (1, 1), (2, 0), (3, -1), (4, 0)):
        idle.pose(t, {'swing': {'rot': (ph * 1.2, ph * 0.6, 0), 'pos': (0, ph * 0.25, 0)},
                      'weapon': {'rot': (0, ph * 3, ph * 1.5)},
                      'ext_a': {'rot': (0, 0, ph * 2.5)}, 'ext_b': {'rot': (0, 0, -ph * 2)},
                      'hook': {'rot': (0, 0, ph * 3)}})
    out.append(idle)

    draw = Clip('draw', 1.25)
    draw.pose(0, {'swing': {'rot': (30, 12, -25), 'pos': (0, -8, 3)}, 'weapon': {'rot': (0, 200, 0)}})
    draw.pose(0.5, {'swing': {'rot': (-8, -4, 6), 'pos': (0, 0.5, 0)}, 'weapon': {'rot': (0, 15, -6)}})
    draw.pose(0.8, {'swing': {'rot': (3, 1, -2), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, -4, 2)}})
    draw.pose(1.25, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    hand_off(draw, 0, 0.05, 0.6, 1.0)
    draw.sound(0, SND + 'draw')
    out.append(draw)

    # Summon: the arm waits low; the weapon grows out of the rift hilt first, flies into the hand.
    low_rot, low_pos = (38, 10, -10), (0, -6, 2)
    P = portal_offset(rig, (-0.05, 0.12, -1.35), low_rot, low_pos)
    summon = Clip('summon', 2.6)
    hidden = {'scale': (0.001, 0.001, 0.001)}
    summon.pose(0, {'swing': {'rot': (38, 10, -10), 'pos': (0, -6, 2)},
                    'weapon': dict(hidden, pos=P, rot=(0, 90, 0))})
    summon.key('weapon', 'scale', 1.05, (0.001, 0.001, 0.001), smooth=False)
    summon.key('weapon', 'scale', 1.1, (0.6, 0.05, 0.6), smooth=False)
    summon.key('weapon', 'scale', 1.65, (1, 1, 1))
    summon.key('weapon', 'position', 1.05, P, smooth=False)
    summon.key('weapon', 'position', 1.65, P)
    summon.key('weapon', 'rotation', 1.05, (0, 90, 0))
    summon.key('weapon', 'rotation', 1.65, (-10, 20, 8))
    summon.pose(1.65, {'swing': {'rot': low_rot, 'pos': low_pos}})
    summon.pose(2.05, {'swing': {'rot': (-6, -3, 4), 'pos': (0, 0.4, 0)},
                       'weapon': {'pos': (0, 0, 0), 'rot': (0, -8, -4), 'scale': (1, 1, 1)}})
    summon.pose(2.3, {'swing': {'rot': (2, 1, -1)}, 'weapon': {'rot': (0, 3, 1)}})
    summon.pose(2.6, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    hand_off(summon, 0, 0.05, 1.9, 2.4)
    eyes_dart(summon, 1.6, 2.6, 3, 0.4, 8)
    kick(summon, 2.05, 2.5)
    summon.sound(0, SND + 'summon')
    out.append(summon)

    def attack(name, keys, hit, snd, seed):
        c = Clip(name, keys[-1][0])
        for t, sw, wp in keys:
            c.pose(t, {'swing': {'rot': sw}, 'weapon': {'rot': wp}})
        kick(c, hit, 2.0)
        eyes_dart(c, 0, keys[-1][0], seed, 0.3, 5)
        c.pose(0, {'ext_a': {'rot': (0, 0, 0)}, 'ext_b': {'rot': (0, 0, 0)}})
        c.pose(hit, {'ext_a': {'rot': (0, 0, -7)}, 'ext_b': {'rot': (0, 0, -6)}})
        c.pose(keys[-1][0], {'ext_a': {'rot': (0, 0, 0)}, 'ext_b': {'rot': (0, 0, 0)}})
        c.sound(max(0.0, hit - 0.12), SND + snd)
        out.append(c)

    # Diagonal: wind up high right, cut down and across to low left. Arcs mostly by roll about the
    # shoulder (which sweeps across the screen) and lean at the wrist; yaw and pitch stay small, as a
    # pivot behind the camera throws the blade out of view for anything large.
    attack('melee_1', [(0, (0, 0, 0), (0, 0, 0)), (0.16, (-8, 7, -18), (0, -10, -25)),
                       (0.32, (4, -8, 30), (6, 8, 26)), (0.5, (6, -7, 34), (6, 6, 24)),
                       (0.85, (0, 0, 0), (0, 0, 0))], 0.3, 'swing_1', 11)
    # Rising: from low left up across to high right.
    attack('melee_2', [(0, (0, 0, 0), (0, 0, 0)), (0.15, (10, -8, 44), (0, 15, 30)),
                       (0.33, (-6, 4, -12), (-6, -12, -20)), (0.5, (-4, 3, -8), (-4, -8, -14)),
                       (0.85, (0, 0, 0), (0, 0, 0))], 0.3, 'swing_2', 12)
    # Horizontal: the blade laid flat by the wrist, swept right to left.
    attack('melee_3', [(0, (0, 0, 0), (0, 0, 0)), (0.18, (-6, 8, 12), (0, -10, 64)),
                       (0.36, (-7, -6, 10), (0, 5, 66)), (0.55, (-6, -8, 8), (0, 8, 62)),
                       (0.9, (0, 0, 0), (0, 0, 0))], 0.33, 'swing_3', 13)
    # Thrust: drawn back, then driven straight forward.
    t4 = Clip('melee_4', 0.85)
    t4.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    t4.pose(0.18, {'swing': {'rot': (-8, 8, 40), 'pos': (2, -1, 5)}, 'weapon': {'rot': (-55, 0, 0)}})
    t4.pose(0.3, {'swing': {'rot': (-4, -5, 35), 'pos': (-1, 1, -9)}, 'weapon': {'rot': (-62, 0, 0)}})
    t4.pose(0.48, {'swing': {'rot': (-3, -4, 30), 'pos': (-1, 1, -7)}, 'weapon': {'rot': (-58, 0, 0)}})
    t4.pose(0.85, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    kick(t4, 0.3, 2.5)
    eyes_dart(t4, 0, 0.85, 14, 0.3, 5)
    t4.sound(0.16, SND + 'thrust')
    out.append(t4)

    # Flourish: a full turn about the grip, caught and settled.
    fl = Clip('flourish', 1.6)
    fl.pose(0, {'swing': {'rot': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    fl.pose(0.2, {'swing': {'rot': (-4, -4, 12)}, 'weapon': {'rot': (0, 0, 0)}})
    for i, a in enumerate((90, 180, 270, 360)):
        fl.pose(0.2 + 0.16 * (i + 1), {'weapon': {'rot': (a, 0, 0)}})
    fl.key('weapon', 'rotation', 0.9, (0, 0, 0), pre=(360, 0, 0))
    fl.pose(1.1, {'swing': {'rot': (6, 4, -5)}, 'weapon': {'rot': (0, -10, 0)}})
    fl.pose(1.6, {'swing': {'rot': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    hand_off(fl, 0, 0.1, 1.2, 1.55)
    eyes_dart(fl, 0.2, 1.4, 15, 0.45, 9)
    fl.sound(0.2, SND + 'flourish')
    out.append(fl)

    # Inspect: brought across the view, turned to show both faces, sighted along, returned.
    ins = Clip('inspect', 5.2)
    ins.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    ins.pose(0.7, {'swing': {'rot': (0, -8, 10), 'pos': (2, 3, -2)}, 'weapon': {'rot': (0, 0, 65)}})
    ins.pose(1.8, {'swing': {'rot': (0, -8, 10)}, 'weapon': {'rot': (0, 175, 65)}})
    ins.pose(2.6, {'swing': {'rot': (0, -8, 8)}, 'weapon': {'rot': (0, 185, 62)}})
    ins.pose(3.4, {'swing': {'rot': (-6, -4, 5), 'pos': (0, 3, 1)}, 'weapon': {'rot': (-40, 360, 10)}})
    ins.pose(4.3, {'swing': {'rot': (-5, -3, 4), 'pos': (0, 3, 1)}, 'weapon': {'rot': (-35, 360, 8)}})
    ins.pose(5.2, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 360, 0)}})
    hand_off(ins, 0, 0.3, 4.6, 5.1)
    eyes_dart(ins, 0.7, 4.6, 16, 0.4, 14)
    ins.sound(0.6, SND + 'inspect')
    out.append(ins)

    run = Clip('run', 0.7, loop=True)
    for t, ph in ((0, 0), (0.175, 1), (0.35, 0), (0.525, -1), (0.7, 0)):
        run.pose(t, {'swing': {'rot': (14 + abs(ph) * 4, ph * 4, -10), 'pos': (ph * 0.6, -1.5 - abs(ph), 0)}})
    out.append(run)

    away = Clip('put_away', 0.7)
    away.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}})
    away.pose(0.7, {'swing': {'rot': (60, 20, -30), 'pos': (0, -10, 4)}})
    away.sound(0, SND + 'put_away')
    out.append(away)
    return out
