"""The Raven Claw's clips.

The crescent is a chain of `spine_k` bones: a negative Z turn on each curls the claw shut, positive
unfurls it. Ribs (`rib_k`) fan out when it opens; feathers (`feather_k`) flutter. Swings follow the
same rig as the other weapons (`swing` about the shoulder, `weapon` at the wrist).
"""
import math

from anim import Clip, arc

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
    # The hold. The claw is a thing that is alive, so its idle is not the blade's: the arm breathes
    # slowly while the crescent flexes against that breath on a shorter cycle, and every so often the
    # whole spine tightens as if it were about to close. The feathers run their own wave, slower
    # still, so nothing in it lines up.
    idle = Clip('static_idle', 6.0, loop=True)
    steps = 16
    for i in range(steps + 1):
        t = 6.0 * i / steps
        ph = math.tau * i / steps
        breath = math.sin(ph)
        flex = math.sin(ph * 2.0 + 0.9)
        # The tightening: a short squeeze a third of the way through the loop.
        grip = max(0.0, math.sin(ph * 1.0 - 1.2)) ** 6
        idle.pose(t, {'swing': {'rot': (breath * 2.0 + grip * 2.5, math.cos(ph * 0.5) * 1.0, flex * 0.8),
                                'pos': (0, breath * 0.5, math.cos(ph) * 0.25)},
                      'weapon': {'rot': (0, flex * 2.2, breath * 1.6)}})
        spine(idle, t, -grip * 7.0, wave=2.4, phase=ph * 1.5,
              ribs=grip * 6.0, feathers=math.sin(ph * 0.75) * 3.0)
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
        arc(c, keys)
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

    # A claw swings from the wrist more than the shoulder, so these lead with the weapon bone and
    # keep a snap at the end of each - the hand whipping through after the arm has stopped.
    attack('melee_1', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.11, (-12, 13, -30), (0, -26, -38), (2.5, 1.5, 2.5)),
                       (0.25, (8, -13, 48), (9, 20, 40), (-2.0, -2.0, -3.5)),
                       (0.32, (9, -15, 54), (10, 16, 34), (-2.2, -2.4, -3.0)),
                       (0.5, (3, -4, 15), (2, 5, 10), (-0.5, -0.5, -0.5)),
                       (0.82, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.25, 'swing_1')
    attack('melee_2', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.11, (13, -13, 52), (0, 32, 42), (-2.2, -2.5, 2.5)),
                       (0.25, (-10, 9, -26), (-8, -26, -34), (1.8, 2.5, -3.5)),
                       (0.32, (-11, 10, -30), (-9, -21, -28), (2.0, 2.8, -3.0)),
                       (0.5, (-3, 3, -8), (-3, -7, -9), (0.4, 0.6, -0.5)),
                       (0.82, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.25, 'swing_2')
    # An overhead rake: raised high, then dragged down through the centre and across.
    attack('melee_3', [(0, (0, 0, 0), (0, 0, 0), (0, 0, 0)),
                       (0.14, (-24, -7, 14), (-34, 0, 12), (0, 3.5, 3.0)),
                       (0.3, (18, -6, 10), (42, 0, 8), (0, -3.5, -4.0)),
                       (0.38, (20, -5, 8), (38, 0, 6), (0, -3.8, -3.4)),
                       (0.58, (6, -2, 4), (12, 0, 2), (0, -0.8, -0.6)),
                       (0.9, (0, 0, 0), (0, 0, 0), (0, 0, 0))], 0.3, 'swing_3')

    # Summon. The claw is not drawn from anywhere: it assembles. The spine arrives furled and far
    # too wide, the ribs sweep in behind it, the whole thing clenches once - hard enough to kick the
    # camera - and settles open in the hand. The arm stays low until the last beat so that what the
    # eye follows is the weapon putting itself together, not the hand coming up.
    summon = Clip('summon', 2.4)
    low_rot, low_pos = (34, 12, -14), (0, -7, 3)
    summon.pose(0, {'swing': {'rot': low_rot, 'pos': low_pos}, 'weapon': {'rot': (0, 60, -30)}})
    summon.key('weapon', 'scale', 0, (0.001, 0.001, 0.001), smooth=False)
    summon.key('weapon', 'scale', 0.35, (0.001, 0.001, 0.001), smooth=False)
    # Out of nothing at the wrist, overshooting its own size before it settles.
    summon.key('weapon', 'scale', 0.75, (1.35, 1.25, 1.35))
    summon.key('weapon', 'scale', 1.15, (0.92, 0.95, 0.92))
    summon.key('weapon', 'scale', 1.5, (1, 1, 1))
    spine(summon, 0.35, 26, ribs=-40, feathers=-26, smooth=False)
    spine(summon, 0.75, 14, wave=7, phase=0.5, ribs=-30, feathers=-18)
    # The clench.
    spine(summon, 1.15, -16, ribs=14, feathers=10, smooth=False)
    spine(summon, 1.5, 6, wave=3, phase=2.0, ribs=-12, feathers=-6)
    spine(summon, 2.4, 0)
    summon.pose(0.75, {'swing': {'rot': (28, 9, -10), 'pos': (0, -5, 2)}, 'weapon': {'rot': (0, 24, -12)}})
    summon.pose(1.15, {'swing': {'rot': (22, 6, -6), 'pos': (0, -3.5, 1)}, 'weapon': {'rot': (0, 8, -4)}})
    summon.pose(1.75, {'swing': {'rot': (-7, -4, 5), 'pos': (0, 0.6, 0)}, 'weapon': {'rot': (0, -6, 3)}})
    summon.pose(2.05, {'swing': {'rot': (3, 2, -2), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 3, -1)}})
    summon.pose(2.4, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    summon.pose(1.11, {'camera': {'rot': (0, 0, 0)}})
    summon.pose(1.17, {'camera': {'rot': (3.2, 1.2, -0.9)}})
    summon.pose(1.4, {'camera': {'rot': (0, 0, 0)}})
    summon.sound(0.3, SND + 'open')
    summon.sound(1.15, SND + 'snap')
    summon.sound(1.7, SND + 'draw')
    out.append(summon)

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
