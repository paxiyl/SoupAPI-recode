"""The Spectral Blade's clips: light, quick cuts, a materialising draw, ribbons that ripple."""
import math

from anim import Clip

SND = 'arcana:melee/spectral_blade/'


def ribbons(c, t, amp, phase, smooth=True):
    for r in range(2):
        for k in range(7):
            c.key('ribbon_%d_%d_illuminated' % (r, k), 'rotation', t,
                  (0, 0, amp * math.sin(phase - k * 0.7 + r * 1.3)), smooth)


def build(rig):
    out = []
    idle = Clip('static_idle', 3.2, loop=True)
    for i in range(9):
        t = i * 0.4
        ph = math.tau * i / 8
        idle.pose(t, {'swing': {'rot': (math.sin(ph) * 1.0, 0, math.cos(ph) * 0.8), 'pos': (0, math.sin(ph) * 0.3, 0)}})
        ribbons(idle, t, 9, ph)
    out.append(idle)

    draw = Clip('draw', 1.2)
    draw.pose(0, {'swing': {'rot': (25, 8, -15), 'pos': (0, -6, 2)}, 'blade_illuminated': {'scale': (1, 0.02, 1)},
                  'blade': {'scale': (1, 0.02, 1)}})
    draw.pose(0.25, {'swing': {'rot': (-3, -1, 2), 'pos': (0, 0.3, 0)}})
    draw.pose(0.3, {'blade_illuminated': {'scale': (1, 0.02, 1)}, 'blade': {'scale': (1, 0.02, 1)}})
    draw.pose(0.75, {'blade_illuminated': {'scale': (1.15, 1.05, 1.15)}, 'blade': {'scale': (1, 1.02, 1)}})
    draw.pose(0.95, {'swing': {'rot': (1, 0, -1)}, 'blade_illuminated': {'scale': (1, 1, 1)}, 'blade': {'scale': (1, 1, 1)}})
    draw.pose(1.2, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}})
    for i in range(7):
        ribbons(draw, i * 0.2, 25 - i * 3, i * 1.2)
    draw.sound(0, SND + 'draw')
    out.append(draw)

    def attack(name, keys, hit, snd):
        c = Clip(name, keys[-1][0])
        for t, sw, wp in keys:
            c.pose(t, {'swing': {'rot': sw}, 'weapon': {'rot': wp}})
        for i in range(6):
            t = keys[-1][0] * i / 5
            ribbons(c, t, 18 if 0 < i < 5 else 6, i * 1.6)
        c.pose(hit - 0.04, {'camera': {'rot': (0, 0, 0)}})
        c.pose(hit, {'camera': {'rot': (1.4, 0.5, -0.4)}})
        c.pose(hit + 0.16, {'camera': {'rot': (0, 0, 0)}})
        c.sound(max(0.0, hit - 0.1), SND + snd)
        out.append(c)

    attack('melee_1', [(0, (0, 0, 0), (0, 0, 0)), (0.12, (-6, 6, -14), (0, -10, -22)),
                       (0.26, (4, -7, 28), (6, 8, 26)), (0.4, (5, -6, 30), (5, 6, 22)), (0.7, (0, 0, 0), (0, 0, 0))],
           0.24, 'swing_1')
    attack('melee_2', [(0, (0, 0, 0), (0, 0, 0)), (0.12, (6, -6, 30), (0, 15, 26)),
                       (0.26, (-6, 4, -12), (-6, -12, -20)), (0.4, (-4, 3, -8), (-4, -8, -14)), (0.7, (0, 0, 0), (0, 0, 0))],
           0.24, 'swing_2')
    lunge = Clip('melee_3', 0.75)
    lunge.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    lunge.pose(0.15, {'swing': {'rot': (-6, 6, 36), 'pos': (2, -1, 4)}, 'weapon': {'rot': (-55, 0, 0)}})
    lunge.pose(0.26, {'swing': {'rot': (-3, -4, 32), 'pos': (-1, 1, -10)}, 'weapon': {'rot': (-62, 0, 0)}})
    lunge.pose(0.42, {'swing': {'rot': (-2, -3, 28), 'pos': (-1, 1, -8)}, 'weapon': {'rot': (-58, 0, 0)}})
    lunge.pose(0.75, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    for i in range(5):
        ribbons(lunge, i * 0.18, 22, i * 2)
    lunge.sound(0.12, SND + 'thrust')
    out.append(lunge)

    fl = Clip('flourish', 1.5)
    fl.pose(0, {'weapon': {'rot': (0, 0, 0)}})
    for i in range(1, 9):
        fl.pose(0.1 + 0.1 * i, {'weapon': {'rot': (0, 0, -90 * i)}})
    fl.key('weapon', 'rotation', 0.95, (0, 0, 0), pre=(0, 0, -720))
    fl.pose(1.5, {'weapon': {'rot': (0, 0, 0)}})
    for i in range(8):
        ribbons(fl, i * 0.2, 30, i * 2)
    fl.sound(0.1, SND + 'flourish')
    out.append(fl)

    ins = Clip('inspect', 4.8)
    ins.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    ins.pose(0.7, {'swing': {'rot': (0, -8, 10), 'pos': (2, 3, -2)}, 'weapon': {'rot': (0, 0, 62)}})
    ins.pose(1.9, {'weapon': {'rot': (0, 180, 62)}})
    ins.pose(2.7, {'weapon': {'rot': (0, 185, 60)}})
    ins.pose(3.5, {'swing': {'rot': (-6, -4, 5), 'pos': (0, 3, 1)}, 'weapon': {'rot': (-35, 360, 8)}})
    ins.pose(4.8, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 360, 0)}})
    for i in range(13):
        ribbons(ins, i * 0.4, 14, i)
    ins.sound(0.6, SND + 'inspect')
    out.append(ins)

    run = Clip('run', 0.7, loop=True)
    for t, ph in ((0, 0), (0.175, 1), (0.35, 0), (0.525, -1), (0.7, 0)):
        run.pose(t, {'swing': {'rot': (12 + abs(ph) * 4, ph * 4, -8), 'pos': (ph * 0.6, -1.5 - abs(ph), 0)}})
        ribbons(run, t, 20, ph * 3)
    out.append(run)

    away = Clip('put_away', 0.6)
    away.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'blade_illuminated': {'scale': (1, 1, 1)}, 'blade': {'scale': (1, 1, 1)}})
    away.pose(0.35, {'blade_illuminated': {'scale': (1, 0.02, 1)}, 'blade': {'scale': (1, 0.02, 1)}})
    away.pose(0.6, {'swing': {'rot': (35, 12, -20), 'pos': (0, -9, 3)}})
    away.sound(0, SND + 'put_away')
    out.append(away)
    return out
