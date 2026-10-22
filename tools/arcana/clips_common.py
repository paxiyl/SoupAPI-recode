"""The clips a weapon gets for free.

The three original weapons each have their clips written by hand, because each one has a part that
moves in a way nothing else does - a spine that writhes, ribbons that trail, a crown of eyes that
darts. A new weapon should not have to restate the other nine clips to get one of those, so this
writes the whole set from a rig and a handful of numbers, and takes an `extra` hook that is called
with (clip, name, t, phase) at every key so a weapon can lay its own moving parts over the top.

`phase` runs 0..1 across whatever the clip is doing, so a hook can be written once and used by the
idle, the attacks and the flourish alike.
"""
import math

from anim import Clip, arc


def build(rig, snd, extra=None, idle_length=5.8, weight=1.0):
    """The ten clips. `weight` scales every arc: 1 is a sword, 1.4 a two-handed staff."""
    out = []

    def hook(clip, name, t, phase):
        if extra is not None:
            extra(clip, name, t, phase)

    w = weight

    # ---- the hold ---------------------------------------------------------------------------
    idle = Clip('static_idle', idle_length, loop=True)
    steps = 16
    for i in range(steps + 1):
        f = i / steps
        t = idle_length * f
        ph = math.tau * f
        breath = math.sin(ph)
        drift = math.sin(ph * 2.0 + 0.7)
        idle.pose(t, {'swing': {'rot': (breath * 2.2 * w, math.cos(ph * 0.5) * 1.3, drift * 1.1),
                                'pos': (drift * 0.5, breath * 0.8 * w, math.cos(ph) * 0.35)},
                      'weapon': {'rot': (drift * 1.6, breath * 2.6, math.cos(ph * 1.5) * 2.0)}})
        hook(idle, 'static_idle', t, f)
    out.append(idle)

    # ---- drawn and put away ------------------------------------------------------------------
    draw = Clip('draw', 1.2)
    for t, f, sw, we in ((0.0, 0.0, (34, 14, -26), (0, -140, 0)),
                         (0.5, 0.45, (-8, -3, 6), (0, 12, 0)),
                         (0.8, 0.7, (3, 1, -2), (0, -4, 0)),
                         (1.2, 1.0, (0, 0, 0), (0, 0, 0))):
        draw.pose(t, {'swing': {'rot': sw, 'pos': (0, -8 * (1 - f) * w, 3 * (1 - f))},
                      'weapon': {'rot': we}})
        hook(draw, 'draw', t, f)
    draw.sound(0, snd + 'draw')
    out.append(draw)

    away = Clip('put_away', 0.65)
    away.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}})
    away.pose(0.65, {'swing': {'rot': (58, 18, -28), 'pos': (0, -10 * w, 4)}})
    hook(away, 'put_away', 0.0, 0.0)
    hook(away, 'put_away', 0.65, 1.0)
    away.sound(0, snd + 'put_away')
    out.append(away)

    # ---- the cuts ----------------------------------------------------------------------------
    # Three arcs that come from different quarters, so a combo does not look like one swing
    # repeated: a downward diagonal, its return, and a rising cut from under.
    shapes = (
        ((-20, 26, 30), (16, -44, -40), (6, -18, -16), (0, 150, 0), (0, -55, 0)),
        ((-14, -30, -26), (12, 46, 38), (4, 20, 14), (0, -150, 0), (0, 55, 0)),
        ((26, 10, -18), (-28, -14, 24), (-10, -5, 10), (0, 40, 120), (0, -16, -40)),
    )
    for n, (wind, cut, over, cw, ww) in enumerate(shapes):
        c = Clip('melee_%d' % (n + 1), 0.78)
        keys = [
            (0.00, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
            (0.15, tuple(v * w for v in wind), tuple(v * 0.35 for v in ww), (3 * w, 2 * w, 2)),
            (0.33, tuple(v * w for v in cut), cw, (-5 * w, -2 * w, -4)),
            (0.45, tuple(v * w for v in over), tuple(v * 1.15 for v in cw), (-3 * w, -1 * w, -2)),
            (0.78, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
        ]
        arc(c, keys)
        for t, f in ((0.0, 0.0), (0.15, 0.2), (0.33, 0.45), (0.45, 0.62), (0.78, 1.0)):
            hook(c, 'melee', t, f)
        c.sound(0.14, snd + 'swing_%d' % (n + 1))
        out.append(c)

    thrust = Clip('melee_4', 0.8)
    arc(thrust, [
        (0.00, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
        (0.17, (14 * w, 16, 10), (0, -26, 0), (4 * w, -2, 6 * w)),
        (0.30, (-10 * w, -8, -5), (0, 10, 0), (-2 * w, 1, -14 * w)),
        (0.44, (-4 * w, -3, -2), (0, 4, 0), (-1 * w, 0, -8 * w)),
        (0.80, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
    ])
    for t, f in ((0.0, 0.0), (0.17, 0.2), (0.30, 0.42), (0.44, 0.6), (0.80, 1.0)):
        hook(thrust, 'melee', t, f)
    thrust.sound(0.14, snd + 'thrust')
    out.append(thrust)

    # ---- the flourish: a circle carved with the arm, the weapon turning inside it -------------
    fl = Clip('flourish', 1.75)
    fl.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    hook(fl, 'flourish', 0.0, 0.0)
    steps = 12
    for i in range(1, steps + 1):
        f = i / steps
        t = 0.12 + 1.05 * f
        a = math.tau * f
        fl.pose(t, {
            'swing': {'rot': (-13 * math.cos(a) * w + 3, 18 * math.sin(a), -24 * math.sin(a - 0.7)),
                      'pos': (5.0 * math.sin(a) * w, 3.4 - 5.6 * math.cos(a) * w, -2.0 * math.sin(a * 2))},
            'weapon': {'rot': (0, 0, -720 * f)},
        })
        hook(fl, 'flourish', t, f)
    fl.key('weapon', 'rotation', 1.26, (0, 0, 0), pre=(0, 0, -720))
    fl.pose(1.38, {'swing': {'rot': (-8, -7, 12), 'pos': (-1.8, 2.4, -1.2)}, 'weapon': {'rot': (0, 0, 14)}})
    hook(fl, 'flourish', 1.38, 1.05)
    fl.pose(1.75, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    hook(fl, 'flourish', 1.75, 1.2)
    fl.sound(0.12, snd + 'flourish')
    out.append(fl)

    # ---- inspect: held up, turned over, sighted along, returned -------------------------------
    ins = Clip('inspect', 4.9)
    beats = ((0.0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
             (0.7, (0, -9, 11), (0, 0, 60), (2, 3, -2)),
             (1.9, (0, -9, 11), (0, 180, 60), (2, 3, -2)),
             (2.7, (0, -8, 9), (0, 190, 57), (2, 3, -2)),
             (3.5, (-6, -4, 5), (-34, 360, 8), (0, 3, 1)),
             (4.9, (0, 0, 0), (0, 360, 0), (0, 0, 0)))
    for t, sw, we, pos in beats:
        ins.pose(t, {'swing': {'rot': sw, 'pos': pos}, 'weapon': {'rot': we}})
        hook(ins, 'inspect', t, t / 4.9)
    ins.sound(0.6, snd + 'inspect')
    out.append(ins)

    # ---- running ------------------------------------------------------------------------------
    run = Clip('run', 0.7, loop=True)
    for t, ph in ((0, 0), (0.175, 1), (0.35, 0), (0.525, -1), (0.7, 0)):
        run.pose(t, {'swing': {'rot': (13 + abs(ph) * 4, ph * 4, -9),
                               'pos': (ph * 0.6, -1.5 - abs(ph), 0)}})
        hook(run, 'run', t, t / 0.7)
    out.append(run)

    return out
