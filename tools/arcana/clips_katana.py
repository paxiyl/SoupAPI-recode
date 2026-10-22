"""The Fractured-Eye Katana's clips.

Bones: `swing` (arcs about the shoulder: +x pitches down, +y yaws right, +z rolls anticlockwise),
`weapon` (about the grip: +x tips the blade away, +y twists it, +z leans it left), `camera` (kick),
`lefthand_pos` (the second hand lets go for the showier moves), the secondary blades for flex and the
eyes' irises for darting. Sounds are this pack's own (see sounds.py).
"""
import math

import numpy as np

from anim import Clip, arc
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

    # The hold. Two cycles of different lengths are laid over each other - a slow breath at the
    # shoulder and a faster drift at the wrist - so the weapon never returns to exactly the same
    # place and never looks pinned to the screen. The blade's tip travels further than the grip,
    # which is what makes it read as something with weight being held rather than a still image.
    idle = Clip('static_idle', 6.0, loop=True)
    steps = 12
    for i in range(steps + 1):
        t = 6.0 * i / steps
        ph = math.tau * i / steps
        breath = math.sin(ph)
        drift = math.sin(ph * 1.5 + 0.7)
        lean = math.cos(ph * 0.5)
        idle.pose(t, {'swing': {'rot': (breath * 2.2 + drift * 0.5, lean * 1.1, drift * 0.9),
                                'pos': (lean * 0.3, breath * 0.55, math.cos(ph) * 0.3)},
                      'weapon': {'rot': (drift * 1.4, breath * 4.5, drift * 2.6)},
                      'ext_a': {'rot': (0, 0, drift * 3.2)}, 'ext_b': {'rot': (0, 0, -breath * 2.6)},
                      'hook': {'rot': (0, 0, math.sin(ph * 2 + 1.1) * 3.4)}})
    eyes_dart(idle, 1.4, 2.2, 21, 0.22, 4)
    eyes_dart(idle, 4.2, 5.0, 22, 0.22, 4)
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

    def attack(name, keys, hit, snd, seed, kick_amount=2.0):
        c = Clip(name, keys[-1][0])
        arc(c, keys)
        kick(c, hit, kick_amount)
        eyes_dart(c, 0, keys[-1][0], seed, 0.3, 5)
        c.pose(0, {'ext_a': {'rot': (0, 0, 0)}, 'ext_b': {'rot': (0, 0, 0)}})
        c.pose(hit, {'ext_a': {'rot': (0, 0, -7)}, 'ext_b': {'rot': (0, 0, -6)}})
        c.pose(keys[-1][0], {'ext_a': {'rot': (0, 0, 0)}, 'ext_b': {'rot': (0, 0, 0)}})
        c.sound(max(0.0, hit - 0.12), SND + snd)
        out.append(c)

    # Diagonal: wind up high right, cut down and across to low left. Arcs mostly by roll about the
    # shoulder (which sweeps across the screen) and lean at the wrist; yaw and pitch stay smaller,
    # as a pivot behind the camera throws the blade out of view for anything large. The shoulder is
    # pulled back and in on the wind-up and driven forward through the cut, which is what carries a
    # swing this size without it leaving the frame.
    attack('melee_1', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.12, (-15, 11, -36), (0, -17, -42), (2.5, 2.0, 3.0)),
                       (0.26, (9, -13, 54), (11, 13, 46), (-2.0, -2.5, -3.5)),
                       (0.34, (11, -14, 60), (12, 11, 42), (-2.2, -3.0, -3.0)),
                       (0.55, (3, -4, 17), (3, 3, 12), (-0.5, -0.8, -0.6)),
                       (0.9, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.26, 'swing_1', 11, 2.6)
    # Rising: from low left up across to high right, the heaviest of the three.
    attack('melee_2', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.13, (17, -13, 62), (0, 22, 44), (-2.0, -3.0, 3.0)),
                       (0.28, (-11, 7, -22), (-10, -19, -34), (1.5, 3.0, -3.5)),
                       (0.36, (-13, 8, -28), (-11, -16, -30), (1.6, 3.4, -3.0)),
                       (0.56, (-4, 3, -9), (-4, -6, -11), (0.4, 0.8, -0.6)),
                       (0.9, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.28, 'swing_2', 12, 2.6)
    # Horizontal: the blade laid flat by the wrist, swept right to left across the whole view.
    attack('melee_3', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.15, (-9, 15, 14), (0, -18, 78), (3.0, 1.0, 2.5)),
                       (0.32, (-11, -13, 11), (0, 10, 82), (-3.0, 0.5, -3.0)),
                       (0.42, (-11, -16, 10), (0, 14, 78), (-3.2, 0.6, -2.6)),
                       (0.62, (-5, -5, 5), (0, 4, 24), (-0.8, 0.2, -0.5)),
                       (0.95, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.3, 'swing_3', 13, 2.2)
    # Thrust: drawn back, then driven straight forward.
    t4 = Clip('melee_4', 0.85)
    t4.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    t4.pose(0.16, {'swing': {'rot': (-11, 11, 46), 'pos': (3, -1.5, 8)}, 'weapon': {'rot': (-62, 0, 0)}})
    t4.pose(0.28, {'swing': {'rot': (-5, -7, 38), 'pos': (-1.5, 1.5, -15)}, 'weapon': {'rot': (-70, 0, 0)}})
    t4.pose(0.46, {'swing': {'rot': (-4, -5, 32), 'pos': (-1, 1, -11)}, 'weapon': {'rot': (-64, 0, 0)}})
    t4.pose(0.85, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    kick(t4, 0.28, 3.0)
    eyes_dart(t4, 0, 0.85, 14, 0.3, 5)
    t4.sound(0.14, SND + 'thrust')
    out.append(t4)

    # Flourish: a staff twirl. The old one turned the weapon bone and left the arm where it was,
    # which is exactly what it looked like - a prop spinning in a hand that was not doing anything.
    # Here the shoulder carries the whole move: it drops, drives up and across, and comes round in
    # a circle while the staff turns twice end over end and three times about its own shaft. The
    # left hand lets go for the spin and is back for the catch.
    fl = Clip('flourish', 1.9)
    fl.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    # The dip the throw comes out of.
    fl.pose(0.14, {'swing': {'rot': (16, 10, 18), 'pos': (1.5, -4.5, 1.5)},
                   'weapon': {'rot': (-18, 0, 12)}})
    spin = 12
    for i in range(1, spin + 1):
        f = i / spin
        t = 0.14 + 1.06 * f
        ring = math.tau * f
        # The hand travels a circle of its own while the staff spins, a quarter-turn behind it.
        fl.pose(t, {
            'swing': {
                'rot': (-14 * math.cos(ring) + 4, 16 * math.sin(ring), -22 * math.sin(ring - 0.8)),
                'pos': (5.5 * math.sin(ring), 4.0 - 6.5 * math.cos(ring), -2.2 * math.sin(ring * 2)),
            },
            'weapon': {'rot': (720 * f, 1080 * f, 10 * math.sin(ring * 2))},
        })
    # The catch: a hard stop with the staff a touch past upright, then settled.
    fl.key('weapon', 'rotation', 1.26, (0, 0, 0), pre=(720, 1080, 0))
    fl.pose(1.34, {'swing': {'rot': (-10, -6, 14), 'pos': (-1.5, 3.0, -1.5)},
                   'weapon': {'rot': (14, 0, -16)}})
    fl.pose(1.52, {'swing': {'rot': (5, 3, -7), 'pos': (0.8, -1.2, 0.6)},
                   'weapon': {'rot': (-6, 0, 7)}})
    fl.pose(1.9, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    hand_off(fl, 0, 0.16, 1.24, 1.42)
    eyes_dart(fl, 0.14, 1.5, 15, 0.55, 11)
    kick(fl, 1.3, 2.2)
    fl.sound(0.14, SND + 'flourish')
    fl.sound(1.26, SND + 'thrust')
    out.append(fl)

    # A spin attack: the staff is swung round the body as one piece, the hand leading it.
    t5 = Clip('melee_5', 0.95)
    arc(t5, [
        (0.00, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
        (0.16, (-14, 34, 26), (0, -40, 18), (5, 2, 3)),
        (0.34, (10, -46, -42), (0, 150, -26), (-7, -2, -5)),
        (0.46, (16, -62, -54), (0, 250, -34), (-9, -3, -6)),
        (0.62, (6, -28, -24), (0, 330, -14), (-3, 0, -2)),
        (0.95, (0, 0, 0), (0, 360, 0), (0, 0, 0)),
    ])
    kick(t5, 0.36, 5.0)
    eyes_dart(t5, 0, 0.95, 21, 0.5, 7)
    t5.sound(0.16, SND + 'swing_3')
    out.append(t5)

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
