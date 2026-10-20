"""The Raven Claw's clips.

The crescent is a chain of `spine_k` bones: a negative Z turn on each curls the claw shut, positive
unfurls it. Ribs (`rib_k`) fan out when it opens; feathers (`feather_k`) flutter. Swings follow the
same rig as the other weapons (`swing` about the shoulder, `weapon` at the wrist).
"""
import math

from anim import Clip

N = 12
RIBS = 9
SND = 'arcana:melee/raven_claw/'


def spine(c, t, per, wave=0.0, phase=0.0, ribs=0.0, feathers=0.0, smooth=True):
    """The whole spine at one moment: `per` degrees on every segment plus an optional travelling wave."""
    for k in range(N):
        c.key('spine_%d' % k, 'rotation', t, (0, 0, per + wave * math.sin(phase - k * 0.55)), smooth)
    for k in range(RIBS):
        c.key('rib_%d' % k, 'rotation', t, (0, 0, ribs), smooth)
        c.key('feather_%d' % k, 'rotation', t, (0, 0, feathers + wave * 1.5 * math.sin(phase * 1.3 - k)), smooth)


def build(rig):
    out = []
    idle = Clip('static_idle', 4.0, loop=True)
    for i in range(9):
        t = i * 0.5
        ph = math.tau * i / 8
        idle.pose(t, {'swing': {'rot': (math.sin(ph) * 1.2, math.cos(ph) * 0.6, 0), 'pos': (0, math.sin(ph) * 0.25, 0)}})
        spine(idle, t, 0, wave=1.6, phase=ph, feathers=0)
    out.append(idle)

    draw = Clip('draw', 1.3)
    draw.pose(0, {'swing': {'rot': (30, 10, -20), 'pos': (0, -7, 3)}, 'weapon': {'rot': (0, -120, 0)}})
    spine(draw, 0, -15, ribs=10)
    draw.pose(0.55, {'swing': {'rot': (-6, -2, 4), 'pos': (0, 0.4, 0)}, 'weapon': {'rot': (0, 10, 0)}})
    spine(draw, 0.55, 6, ribs=-18, feathers=-10)
    draw.pose(0.85, {'swing': {'rot': (2, 1, -1)}, 'weapon': {'rot': (0, -3, 0)}})
    spine(draw, 0.85, -2, ribs=3)
    draw.pose(1.3, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    spine(draw, 1.3, 0)
    draw.sound(0, SND + 'draw')
    draw.sound(0.45, SND + 'open')
    out.append(draw)

    opening = Clip('claw_open', 1.4)
    spine(opening, 0, 0)
    spine(opening, 0.35, 10, ribs=-25, feathers=-14)
    spine(opening, 0.5, 8.5, ribs=-22, feathers=-12)
    spine(opening, 1.0, 8.5, wave=1.2, phase=2, ribs=-22, feathers=-12)
    spine(opening, 1.4, 0)
    opening.pose(0, {'swing': {'rot': (0, 0, 0)}})
    opening.pose(0.35, {'swing': {'rot': (-4, -3, 6)}})
    opening.pose(1.4, {'swing': {'rot': (0, 0, 0)}})
    opening.sound(0, SND + 'open')
    out.append(opening)

    closing = Clip('claw_close', 0.7)
    spine(closing, 0, 0)
    spine(closing, 0.1, -13, ribs=12, feathers=8, smooth=False)
    spine(closing, 0.3, -11, ribs=10, feathers=6)
    spine(closing, 0.7, 0)
    closing.pose(0, {'camera': {'rot': (0, 0, 0)}})
    closing.pose(0.1, {'camera': {'rot': (1.5, 0, 0.5)}})
    closing.pose(0.3, {'camera': {'rot': (0, 0, 0)}})
    closing.sound(0, SND + 'snap')
    out.append(closing)

    def attack(name, keys, hit, snd):
        c = Clip(name, keys[-1][0])
        for t, sw, wp in keys:
            c.pose(t, {'swing': {'rot': sw}, 'weapon': {'rot': wp}})
        spine(c, 0, 0)
        spine(c, hit - 0.12, 7, ribs=-15, feathers=-8)
        spine(c, hit, -11, ribs=10, feathers=6)
        spine(c, hit + 0.2, -6, ribs=5)
        spine(c, keys[-1][0], 0)
        c.pose(hit - 0.04, {'camera': {'rot': (0, 0, 0)}})
        c.pose(hit, {'camera': {'rot': (2, 0.8, -0.6)}})
        c.pose(hit + 0.2, {'camera': {'rot': (0, 0, 0)}})
        c.sound(max(0.0, hit - 0.12), SND + snd)
        out.append(c)

    attack('melee_1', [(0, (0, 0, 0), (0, 0, 0)), (0.15, (-6, 8, -16), (0, -15, -20)),
                       (0.3, (4, -8, 28), (5, 10, 22)), (0.48, (5, -6, 30), (4, 8, 20)), (0.8, (0, 0, 0), (0, 0, 0))],
           0.3, 'swing_1')
    attack('melee_2', [(0, (0, 0, 0), (0, 0, 0)), (0.15, (6, -8, 30), (0, 20, 25)),
                       (0.3, (-6, 6, -14), (-5, -15, -18)), (0.48, (-4, 4, -10), (-3, -10, -12)), (0.8, (0, 0, 0), (0, 0, 0))],
           0.3, 'swing_2')
    # An overhead rake: raised, then dragged down through the centre.
    attack('melee_3', [(0, (0, 0, 0), (0, 0, 0)), (0.18, (-14, -4, 10), (-20, 0, 8)),
                       (0.34, (10, -3, 8), (25, 0, 6)), (0.52, (8, -2, 6), (20, 0, 4)), (0.85, (0, 0, 0), (0, 0, 0))],
           0.34, 'swing_3')

    fl = Clip('flourish', 1.7)
    fl.pose(0, {'swing': {'rot': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    for i in range(1, 5):
        fl.pose(0.15 + 0.2 * i, {'weapon': {'rot': (0, 90 * i, 0)}})
    fl.key('weapon', 'rotation', 1.0, (0, 0, 0), pre=(0, 360, 0))
    fl.pose(1.7, {'swing': {'rot': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    for i in range(9):
        spine(fl, i * 0.2, 4 if i % 2 else -4, wave=4, phase=i, ribs=-8 if i % 2 else 6)
    spine(fl, 1.7, 0)
    fl.sound(0.15, SND + 'flourish')
    out.append(fl)

    ins = Clip('inspect', 5.4)
    ins.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    ins.pose(0.8, {'swing': {'rot': (-4, -10, 8), 'pos': (2, 2, -1)}, 'weapon': {'rot': (0, 0, 15)}})
    ins.pose(2.0, {'swing': {'rot': (-4, -10, 8)}, 'weapon': {'rot': (0, 170, 15)}})
    ins.pose(2.8, {'swing': {'rot': (-4, -9, 7)}, 'weapon': {'rot': (0, 190, 12)}})
    ins.pose(3.6, {'swing': {'rot': (-6, -6, 4), 'pos': (1, 2, -2)}, 'weapon': {'rot': (0, 360, 5)}})
    ins.pose(5.4, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 360, 0)}})
    spine(ins, 0, 0)
    spine(ins, 0.8, 2, wave=2, phase=0)
    spine(ins, 2.0, -3, wave=2, phase=2)
    spine(ins, 3.6, 0)
    spine(ins, 4.0, 10, ribs=-25, feathers=-14)
    spine(ins, 4.5, 10, ribs=-25, feathers=-14)
    spine(ins, 4.6, -13, ribs=12, feathers=8, smooth=False)
    spine(ins, 5.4, 0)
    ins.sound(0.7, SND + 'inspect')
    ins.sound(3.8, SND + 'open')
    ins.sound(4.55, SND + 'snap')
    out.append(ins)

    run = Clip('run', 0.7, loop=True)
    for t, ph in ((0, 0), (0.175, 1), (0.35, 0), (0.525, -1), (0.7, 0)):
        run.pose(t, {'swing': {'rot': (12 + abs(ph) * 4, ph * 4, -8), 'pos': (ph * 0.6, -1.5 - abs(ph), 0)}})
        spine(run, t, -3, wave=2, phase=ph * 2)
    out.append(run)

    away = Clip('put_away', 0.7)
    away.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}})
    away.pose(0.7, {'swing': {'rot': (40, 15, -25), 'pos': (0, -10, 4)}})
    spine(away, 0, 0)
    spine(away, 0.5, -14, ribs=10)
    away.sound(0, SND + 'put_away')
    out.append(away)
    return out
