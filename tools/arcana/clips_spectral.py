"""The Spectral Blade's clips: light, quick cuts, a materialising draw, ribbons that ripple."""
import math

from anim import Clip, arc

SND = 'arcana:melee/spectral_blade/'


def ribbons(c, t, amp, phase, smooth=True):
    for r in range(2):
        for k in range(7):
            c.key('ribbon_%d_%d_illuminated' % (r, k), 'rotation', t,
                  (0, 0, amp * math.sin(phase - k * 0.7 + r * 1.3)), smooth)


def build(rig):
    out = []
    # The hold. This blade weighs nothing, so where the other two breathe it floats: the grip drifts
    # on a long figure of eight, the blade lags behind that drift instead of following it, and the
    # ribbons run at their own rate. Larger travel than the heavier weapons, and slower.
    idle = Clip('static_idle', 5.6, loop=True)
    steps = 14
    for i in range(steps + 1):
        t = 5.6 * i / steps
        ph = math.tau * i / steps
        # A figure of eight: the horizontal runs at twice the vertical.
        fx = math.sin(ph * 2.0)
        fy = math.sin(ph)
        lag = math.sin(ph - 0.8)
        idle.pose(t, {'swing': {'rot': (fy * 1.6, fx * 1.0, lag * 1.4),
                                'pos': (fx * 0.6, fy * 0.8, math.cos(ph * 0.5) * 0.35)},
                      'weapon': {'rot': (lag * 2.0, fx * 3.0, lag * 3.2)}})
        ribbons(idle, t, 11 + 4 * math.sin(ph * 1.5), ph * 1.25)
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
        arc(c, keys)
        for i in range(6):
            t = keys[-1][0] * i / 5
            ribbons(c, t, 18 if 0 < i < 5 else 6, i * 1.6)
        c.pose(hit - 0.04, {'camera': {'rot': (0, 0, 0)}})
        c.pose(hit, {'camera': {'rot': (1.4, 0.5, -0.4)}})
        c.pose(hit + 0.16, {'camera': {'rot': (0, 0, 0)}})
        c.sound(max(0.0, hit - 0.1), SND + snd)
        out.append(c)

    # Quick and wide: a weightless blade can be thrown further and pulled back faster than a steel
    # one, so these are the largest arcs in the pack and the shortest.
    attack('melee_1', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.09, (-13, 10, -34), (0, -20, -46), (2.5, 1.8, 2.5)),
                       (0.21, (9, -13, 58), (12, 14, 50), (-2.2, -2.2, -3.5)),
                       (0.28, (10, -15, 64), (13, 11, 44), (-2.4, -2.6, -3.0)),
                       (0.44, (3, -4, 18), (4, 3, 13), (-0.6, -0.6, -0.6)),
                       (0.72, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.21, 'swing_1')
    attack('melee_2', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.09, (14, -11, 60), (0, 26, 48), (-2.2, -2.6, 2.5)),
                       (0.21, (-11, 7, -24), (-10, -22, -38), (1.8, 2.6, -3.5)),
                       (0.28, (-12, 8, -29), (-11, -18, -32), (2.0, 2.9, -3.0)),
                       (0.44, (-4, 3, -8), (-4, -6, -10), (0.5, 0.7, -0.6)),
                       (0.72, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.21, 'swing_2')
    lunge = Clip('melee_3', 0.75)
    lunge.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    lunge.pose(0.13, {'swing': {'rot': (-8, 9, 42), 'pos': (3, -1.5, 7)}, 'weapon': {'rot': (-62, 0, 0)}})
    lunge.pose(0.24, {'swing': {'rot': (-4, -6, 34), 'pos': (-1.5, 1.5, -17)}, 'weapon': {'rot': (-72, 0, 0)}})
    lunge.pose(0.4, {'swing': {'rot': (-3, -4, 29), 'pos': (-1, 1, -12)}, 'weapon': {'rot': (-64, 0, 0)}})
    lunge.pose(0.75, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    for i in range(5):
        ribbons(lunge, i * 0.18, 22, i * 2)
    lunge.sound(0.12, SND + 'thrust')
    out.append(lunge)

    # Summon. The blade is light before it is a blade: the hilt turns up in an open hand, the edge
    # draws itself out of it from the grip to the tip, overshoots, and snaps to length with the
    # ribbons whipping once behind it. Nothing is thrown or caught - the whole thing happens where
    # the hand already is, which is what separates it from the katana's.
    summon = Clip('summon', 2.2)
    summon.pose(0, {'swing': {'rot': (30, 14, -16), 'pos': (0, -7, 3)}, 'weapon': {'rot': (0, 40, -20)},
                    'blade_illuminated': {'scale': (0.3, 0.01, 0.3)}, 'blade': {'scale': (0.3, 0.01, 0.3)}})
    # The hilt turns over in the hand while the blade is still nothing.
    summon.pose(0.45, {'swing': {'rot': (20, 6, -8), 'pos': (0, -4, 1.5)}, 'weapon': {'rot': (0, -14, -6)}})
    summon.pose(0.5, {'blade_illuminated': {'scale': (0.6, 0.02, 0.6)}, 'blade': {'scale': (0.5, 0.02, 0.5)}})
    # Drawn out past its own length, then pulled back to it.
    summon.pose(1.05, {'blade_illuminated': {'scale': (1.5, 1.22, 1.5)}, 'blade': {'scale': (1.2, 1.18, 1.2)}})
    summon.pose(1.3, {'blade_illuminated': {'scale': (0.95, 0.96, 0.95)}, 'blade': {'scale': (0.97, 0.97, 0.97)}})
    summon.pose(1.55, {'blade_illuminated': {'scale': (1, 1, 1)}, 'blade': {'scale': (1, 1, 1)}})
    summon.pose(1.05, {'swing': {'rot': (6, -5, 14), 'pos': (0, -1, -2)}, 'weapon': {'rot': (0, -22, 10)}})
    summon.pose(1.45, {'swing': {'rot': (-8, -4, 6), 'pos': (0, 0.8, 0)}, 'weapon': {'rot': (0, 10, -5)}})
    summon.pose(1.8, {'swing': {'rot': (3, 2, -2), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, -3, 2)}})
    summon.pose(2.2, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    for i in range(12):
        t = i * 0.2
        # The ribbons are still until the blade starts drawing, then whip and settle.
        amp = 4 if t < 0.5 else (46 if t < 1.2 else 34 - (t - 1.2) * 22)
        ribbons(summon, t, max(4, amp), i * 1.7)
    ribbons(summon, 2.2, 9, 0)
    summon.pose(1.01, {'camera': {'rot': (0, 0, 0)}})
    summon.pose(1.08, {'camera': {'rot': (2.6, -1.0, 0.8)}})
    summon.pose(1.35, {'camera': {'rot': (0, 0, 0)}})
    summon.sound(0.35, SND + 'draw')
    summon.sound(1.0, SND + 'inspect')
    out.append(summon)

    # Flourish: a figure of eight carved with the whole arm, the blade turning twice flat inside
    # it and the ribbons a beat behind. The old one spun the weapon bone on the spot, which is
    # why it read as a turntable; the shoulder does the drawing here and the wrist only adds spin.
    fl = Clip('flourish', 1.8)
    fl.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    ribbons(fl, 0, 6, 0)
    steps = 14
    for i in range(steps + 1):
        f = i / steps
        t = 0.1 + 1.15 * f
        a = math.tau * f
        # A lemniscate at the shoulder: across twice for every pass up and down.
        fl.pose(t, {
            'swing': {
                'rot': (-12 * math.sin(a * 2), 26 * math.sin(a), -30 * math.sin(a) * math.cos(a)),
                'pos': (7.0 * math.sin(a), 3.0 * math.sin(a * 2), -1.8 * math.cos(a)),
            },
            'weapon': {'rot': (0, 0, -720 * f)},
        })
        # The ribbons run the same wave a fifth of a cycle late, which is what makes them trail.
        ribbons(fl, t, 26 + 12 * math.sin(a * 2), a * 2 - 1.3)
    fl.key('weapon', 'rotation', 1.25, (0, 0, 0), pre=(0, 0, -720))
    fl.pose(1.38, {'swing': {'rot': (-8, -10, 14), 'pos': (-2.0, 2.5, -1.0)}, 'weapon': {'rot': (0, 0, 16)}})
    ribbons(fl, 1.38, 34, 5.0)
    fl.pose(1.56, {'swing': {'rot': (5, 6, -8), 'pos': (1.2, -1.4, 0.6)}, 'weapon': {'rot': (0, 0, -8)}})
    ribbons(fl, 1.56, 16, 6.2)
    fl.pose(1.8, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    ribbons(fl, 1.8, 6, 7.0)
    fl.sound(0.1, SND + 'flourish')
    fl.sound(1.25, SND + 'thrust')
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
