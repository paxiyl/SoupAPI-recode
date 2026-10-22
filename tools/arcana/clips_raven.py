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


def _own(rig):
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

    # Flourish: the creepy one. The claw is held up across the face and the crescent is let go -
    # a wave travels the length of the spine, the ribs splay, the feathers flare - and then the
    # whole thing snaps shut on nothing. The arm carries it: it lifts, turns the claw over, and
    # recoils off the snap, because a spine writhing in a hand that is not moving reads as a
    # loading animation rather than as something alive.
    fl = Clip('flourish', 2.1)
    fl.pose(0, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 0, 0)}})
    spine(fl, 0, 0)
    # Raised and turned over, the opening towards the player.
    fl.pose(0.28, {'swing': {'rot': (-20, -14, 22), 'pos': (3.5, 5.0, -2.0)},
                   'weapon': {'rot': (0, -40, 18)}})
    spine(fl, 0.28, 10, ribs=-22, feathers=-16)
    # The writhe: eight steps of a wave running down the chain, the arm rolling under it.
    steps = 8
    for i in range(steps + 1):
        f = i / steps
        t = 0.28 + 1.02 * f
        ph = math.tau * f * 1.6
        fl.pose(t, {'swing': {'rot': (-20 + 5 * math.sin(ph), -14 + 7 * math.cos(ph * 0.7), 22 + 6 * math.sin(ph * 1.3)),
                              'pos': (3.5 + 1.2 * math.sin(ph), 5.0 + 0.9 * math.cos(ph), -2.0)},
                    'weapon': {'rot': (6 * math.sin(ph * 1.1), -40 + 150 * f, 18 + 9 * math.cos(ph))}})
        spine(fl, t, 9 + 3 * math.sin(ph), wave=13, phase=ph,
              ribs=-20 - 8 * math.sin(ph * 1.2), feathers=-14 + 10 * math.sin(ph * 0.8))
    # The snap: shut hard, nothing smoothed, and the arm takes the recoil.
    fl.pose(1.36, {'swing': {'rot': (-6, 4, 4), 'pos': (1.0, 1.0, -3.0)}, 'weapon': {'rot': (0, 120, 4)}})
    spine(fl, 1.36, -22, ribs=16, feathers=12, smooth=False)
    fl.pose(1.52, {'swing': {'rot': (10, 8, -10), 'pos': (-1.5, -2.0, 2.0)}, 'weapon': {'rot': (0, 112, -8)}})
    spine(fl, 1.52, -16, ribs=11, feathers=7)
    fl.pose(1.76, {'swing': {'rot': (-4, -3, 4), 'pos': (0.6, 0.8, -0.8)}, 'weapon': {'rot': (0, 122, 3)}})
    spine(fl, 1.76, 3, ribs=-3)
    fl.pose(2.1, {'swing': {'rot': (0, 0, 0), 'pos': (0, 0, 0)}, 'weapon': {'rot': (0, 120, 0)}})
    spine(fl, 2.1, 0)
    fl.sound(0.2, SND + 'open')
    fl.sound(1.36, SND + 'snap')
    fl.sound(1.5, SND + 'flourish')
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


def _bump(x, at, width):
    return max(0.0, 1 - abs(x - at) / width)


def _extra(clip, name, t, phase):
    """The claw is alive in every clip: its spine flexes through the hold, clenches on the strike
    and opens after it, unfurls to be looked at and writhes before it snaps shut."""
    if int(round(t * 20)) % 2:
        return
    if name == 'static_idle':
        ph = math.tau * t / 6.0
        grip = max(0.0, math.sin(ph - 1.2)) ** 6
        spine(clip, t, -grip * 7.0, wave=2.4, phase=ph * 2, ribs=grip * 6.0, feathers=math.sin(ph) * 3.0)
    elif name == 'melee':
        clench = _bump(phase, 0.3, 0.14)
        flare = _bump(phase, 0.5, 0.2)
        spine(clip, t, -16 * clench + 8 * flare, wave=4 * flare, phase=t * 9,
              ribs=12 * clench - 18 * flare, feathers=8 * clench - 14 * flare)
    elif name == 'inspect':
        open_ = max(0.0, min(1.0, (t - 1.0) / 0.6)) * (1 - max(0.0, min(1.0, (t - 2.8) / 0.5)))
        writhe = max(0.0, min(1.0, (t - 5.2) / 0.6)) * (1 - max(0.0, min(1.0, (t - 7.2) / 0.3)))
        snap = _bump(t, 7.35, 0.18)
        spine(clip, t, 10 * open_ + 6 * writhe - 20 * snap, wave=12 * writhe + 2, phase=t * 4.5,
              ribs=-22 * open_ - 16 * writhe + 14 * snap, feathers=-15 * open_ - 10 * writhe + 10 * snap)
    elif name == 'draw':
        spine(clip, t, 14 * (1 - phase) - 6 * _bump(phase, 0.55, 0.2), wave=5 * (1 - phase), phase=t * 8,
              ribs=-20 * (1 - phase), feathers=-14 * (1 - phase))
    elif name == 'summon':
        unfurl = max(0.0, min(1.0, (phase - 0.36) / 0.2))
        clench = _bump(phase, 0.62, 0.08)
        spine(clip, t, 26 * (1 - unfurl) + 4 - 22 * clench, wave=7 * unfurl * (1 - phase), phase=t * 6,
              ribs=-40 * (1 - unfurl) + 14 * clench, feathers=-26 * (1 - unfurl) + 10 * clench)
    elif name == 'run':
        spine(clip, t, 0, wave=3, phase=t * 9)


def build(rig):
    from clips_common import build as common, summon, merge
    base = common(rig, SND, extra=_extra, weight=1.0)
    out = merge(base, _own(rig), keep=('flourish', 'claw_open', 'claw_close'))
    out = [c for c in out if c.name != 'summon']
    out.append(summon(rig, SND, 'wing', 2.4, extra=_extra))
    return out
