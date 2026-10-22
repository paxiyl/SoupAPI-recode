"""The clips every Arcana weapon is built on, written the way the delta pack's knives are.

Measured from those knives, the things that make them feel held rather than posed:

  * The hand and the weapon move as one. Their weapon and right hand are siblings under one group;
    here that is the `wrist` bone, and every cut, turn and sighting is keyed on it, so the arm goes
    wherever the weapon goes.
  * The weapon still turns in the fingers on its own. Their weapon's rotation range is two to five
    times the hand's in an inspect - the knife flips while the hand only rolls. That is the
    `weapon` bone here: flips, twirls, tosses.
  * The left hand is never idle in an inspect. Theirs is keyed as densely as the right and does as
    much: it reaches in, steadies, takes the blade. A one-handed weapon's left arm waits below the
    view and comes up for the inspect; a two-handed weapon's stays on the shaft and lets go for the
    twirls.
  * The camera breathes with it - a degree or two through an inspect, a kick on every strike.
  * Everything is a curve, keyed about twenty times a second. Nothing arrives at a pose and stops.
  * Inspects are long and have beats: theirs run seven to thirteen seconds. These run nine.

`extra(clip, name, t, phase)` is called at every sampled key so a weapon can lay its own moving
parts over the top; `phase` runs 0..1 over whatever the clip is doing.
"""
import math

from anim import Clip, sample, smooth, ease_out, ease_in, window, spring, mix, add, scale, clamp01

RATE = 20.0
# A one-handed weapon's left arm, brought up into view (model units from where it is parked).
LEFT_UP = (-4.5, 10.0, -3.0)


def _kick(t, at, amount, decay=7.0):
    """A camera kick: a sharp rise at `at` and a ring-down after it."""
    if t < at:
        return 0.0
    x = t - at
    return amount * math.exp(-decay * x) * math.cos(x * 22.0)


def build(rig, snd, extra=None, idle_length=6.0, weight=1.0, two_handed=None):
    two = rig.two_handed if two_handed is None else two_handed
    w = weight
    out = []

    def hook(clip, name, phase):
        def wrap(fn):
            def inner(t):
                pose = fn(t)
                if extra is not None:
                    extra(clip, name, t, phase(t))
                return pose
            return inner
        return wrap

    # ---- the hold: a slow breath and a smaller drift, layered so it never repeats exactly ----
    idle = Clip('static_idle', idle_length, loop=True)

    def idle_pose(t):
        ph = math.tau * t / idle_length
        return {
            'swing': {'rot': scale((1.3 * math.sin(ph), 0.9 * math.sin(2 * ph + 0.5), 0.8 * math.sin(ph + 1.1)), w),
                      'pos': (0.25 * math.sin(2 * ph), 0.45 * w * math.sin(ph), 0.2 * math.cos(ph))},
            'wrist': {'rot': (1.1 * math.sin(2 * ph + 1.0), 1.7 * math.sin(ph + 0.4), 1.2 * math.sin(3 * ph))},
        }
    sample(idle, 0, idle_length, hook(idle, 'static_idle', lambda t: t / idle_length)(idle_pose), 10.0)
    out.append(idle)

    # ---- draw: up from below, the weapon flipping into the hand as it lands --------------------
    draw = Clip('draw', 1.05)

    def draw_pose(t):
        s = spring(t / 0.95, 2.0, 5.0)
        flip = ease_out(t / 0.55)
        pose = {
            'swing': {'rot': mix((32, 14, -24), (0, 0, 0), s), 'pos': mix((0, -9 * w, 3), (0, 0, 0), s)},
            'wrist': {'rot': mix((10, -70, -30), (0, 0, 0), s)},
            'weapon': {'rot': (-180 * (1 - flip), 0, 0) if two else (0, -360 * (1 - flip), 0)},
            'camera': {'rot': (_kick(t, 0.5, 1.6), _kick(t, 0.5, 0.5), _kick(t, 0.5, -0.6))},
        }
        return pose
    sample(draw, 0, 1.05, hook(draw, 'draw', lambda t: t / 1.05)(draw_pose), 24.0)
    draw.sound(0, snd + 'draw')
    out.append(draw)

    # ---- the cuts -----------------------------------------------------------------------------
    # Four strikes from four quarters, so a combo never repeats one swing. Each has the knives'
    # shape: a short wind-up, a strike that is over in a seventh of a second, an overshoot past
    # the target, and a long recovery that settles with a little ring rather than stopping dead.
    shapes = (
        # wind (swing rot, swing pos, wrist rot), strike (...), overshoot (...)
        (((-20, 28, 32), (3, 2, 2), (0, -30, -24)),
         ((18, -46, -42), (-5, -2, -4), (8, 40, 44)),
         ((8, -22, -20), (-3, -1, -2), (4, 24, 26))),
        (((-14, -32, -28), (-3, 2, 2), (0, 30, 22)),
         ((14, 48, 40), (5, -2, -4), (6, -42, -40)),
         ((5, 22, 16), (3, -1, -2), (3, -22, -20))),
        (((28, 12, -20), (0, -3, 3), (-20, 10, 0)),
         ((-30, -16, 26), (0, 3, -5), (40, -10, -10)),
         ((-12, -6, 12), (0, 1, -2), (18, -4, -4))),
        (((16, 18, 12), (4, -2, 6), (-14, -20, 0)),
         ((-10, -8, -6), (-2, 1, -14), (8, 10, 0)),
         ((-4, -3, -2), (-1, 0, -8), (4, 4, 0))),
    )
    for n, (wind, strike, over) in enumerate(shapes):
        length = 1.0
        c = Clip('melee_%d' % (n + 1), length)

        def attack_pose(t, wind=wind, strike=strike, over=over):
            if t < 0.16:
                f = smooth(t / 0.16)
                pose = [mix((0, 0, 0), wind[i], f) for i in range(3)]
            elif t < 0.30:
                f = ease_out((t - 0.16) / 0.14)
                pose = [mix(wind[i], strike[i], f) for i in range(3)]
            elif t < 0.42:
                f = smooth((t - 0.30) / 0.12)
                pose = [mix(strike[i], over[i], f) for i in range(3)]
            else:
                f = spring((t - 0.42) / 0.58, 1.6, 4.0)
                pose = [mix(over[i], (0, 0, 0), f) for i in range(3)]
            sw_rot, sw_pos, wr_rot = pose
            sw_rot = scale(sw_rot, w)
            sw_pos = scale(sw_pos, w)
            # The edge turned into the cut: a little twist in the fingers through the strike.
            edge = 14 * math.sin(math.pi * clamp01((t - 0.12) / 0.3))
            k = _kick(t, 0.29, 2.4 * w)
            p = {'swing': {'rot': sw_rot, 'pos': sw_pos}, 'wrist': {'rot': wr_rot},
                 'weapon': {'rot': (0, edge, 0)},
                 'camera': {'rot': (k, k * 0.35, -k * 0.3)}}
            if not two:
                # The free hand counters the swing, the way a fighter's off hand does.
                bal = math.sin(math.pi * clamp01(t / 0.6))
                p['lefthand_pos'] = {'pos': scale((-1.2, 4.0, 0.5), bal), 'rot': scale((-12, 8, 6), bal)}
            return p
        sample(c, 0, length, hook(c, 'melee', lambda t: t / length)(attack_pose), RATE)
        c.sound(0.15, snd + ('swing_%d' % (n + 1) if n < 3 else 'thrust'))
        out.append(c)

    # ---- inspect ------------------------------------------------------------------------------
    ins = Clip('inspect', 9.0)
    if two:
        ins_fn = _inspect_two(w)
    else:
        ins_fn = _inspect_one(w)
    sample(ins, 0, 9.0, hook(ins, 'inspect', lambda t: t / 9.0)(ins_fn), RATE)
    ins.sound(0.5, snd + 'inspect')
    ins.sound(2.8, snd + 'flourish')
    ins.sound(4.75 if not two else 4.3, snd + 'thrust')
    out.append(ins)

    # ---- flourish: a circle carved with the arm, the weapon spinning in the fingers inside it ---
    fl = Clip('flourish', 1.75)

    def flourish_pose(t):
        f = smooth((t - 0.12) / 1.1)
        a = math.tau * f
        travel = window(t, 0.0, 0.15) * (1 - window(t, 1.25, 1.7))
        settle = spring((t - 1.25) / 0.5, 2.2, 5.0) if t > 1.25 else 0.0
        sw_rot = scale((-13 * math.cos(a) + 3, 18 * math.sin(a), -24 * math.sin(a - 0.7)), travel * w)
        sw_pos = scale((5.0 * math.sin(a), 3.4 - 5.6 * math.cos(a), -2.0 * math.sin(a * 2)), travel * w)
        return {'swing': {'rot': sw_rot, 'pos': sw_pos},
                'wrist': {'rot': (0, 40 * math.sin(a) * travel, 0)},
                'weapon': {'rot': (0, 0, -720 * f) if not two else (-360 * f, 0, 0)},
                'lefthand_pos': {'pos': scale((0, -7, 3) if two else (0, 0, 0), travel)},
                'camera': {'rot': (_kick(t, 1.25, 1.8) + 0 * settle, 0, 0)}}
    sample(fl, 0, 1.75, hook(fl, 'flourish', lambda t: t / 1.75 * 1.2)(flourish_pose), RATE)
    fl.sound(0.12, snd + 'flourish')
    out.append(fl)

    # ---- running and putting away --------------------------------------------------------------
    run = Clip('run', 0.7, loop=True)

    def run_pose(t):
        ph = math.tau * t / 0.7
        return {'swing': {'rot': (13 + 4 * abs(math.sin(ph)), 4 * math.sin(ph), -9),
                          'pos': (0.6 * math.sin(ph), -1.5 - abs(math.sin(ph)), 0)},
                'wrist': {'rot': (6 * math.sin(ph * 2), 0, -4)}}
    sample(run, 0, 0.7, hook(run, 'run', lambda t: t / 0.7)(run_pose), RATE)
    out.append(run)

    away = Clip('put_away', 0.65)

    def away_pose(t):
        f = ease_in(t / 0.65)
        return {'swing': {'rot': mix((0, 0, 0), (58, 18, -28), f), 'pos': mix((0, 0, 0), (0, -10 * w, 4), f)},
                'wrist': {'rot': mix((0, 0, 0), (0, 60, 20), f)}}
    sample(away, 0, 0.65, hook(away, 'put_away', lambda t: t / 0.65)(away_pose), RATE)
    away.sound(0, snd + 'put_away')
    out.append(away)

    return out


def _inspect_one(w):
    """A one-handed weapon's inspect, in six beats.

    Raised and turned face-on while the left hand comes up to it; turned over in the fingers to
    show the other face; spun twice; tossed, turning end over end, and caught with a dip; tipped
    to sight down its length with the left hand at the far end; and let back down.

    As in the knives, the big turns are all the weapon's own, in the fingers: the wrist only ever
    turns a few tens of degrees, because the arm hangs off it and a wrist rolled half over swings
    the whole forearm through the camera.
    """
    def pose(t):
        up = window(t, 0.0, 1.1) * (1 - window(t, 7.3, 8.6))
        sight = window(t, 5.3, 6.4) * (1 - window(t, 7.3, 8.4))

        sw_rot = add(scale((-6, -9, 8), up), scale((3, -2, -3), sight))
        sw_pos = add(scale((1.2, 2.0, -1.5), up), scale((-1.8, -0.4, -1), sight))
        wr_rot = (-18 * sight + 4 * math.sin(t * 1.3) * up,
                  -28 * up + 10 * math.sin(t * 0.9) * up,
                  26 * up - 16 * sight)

        # In the fingers: turned over, spun twice, tossed, turned back - 1080 in all, so it ends
        # exactly where it began.
        face = 180 * smooth((t - 1.1) / 1.5)
        spin = 720 * smooth((t - 2.7) / 1.2)
        back = 180 * smooth((t - 7.3) / 1.4)
        toss_up = ease_out((t - 3.95) / 0.4) * (1 - ease_in((t - 4.35) / 0.4))
        tumble = 360 * smooth((t - 4.0) / 0.7)
        tilt = -50 * sight
        catch = spring((t - 4.75) / 0.6, 2.4, 6.0) if t >= 4.75 else 1.0
        dip = 1 - catch
        sw_pos = add(sw_pos, (0, -1.4 * dip, 0))
        sw_rot = add(sw_rot, (4 * dip, 0, 0))

        # The left hand: up to the weapon, back for the spin, out to the far end to sight along it.
        lh_up = window(t, 0.3, 1.3) * (1 - window(t, 2.6, 3.0)) + window(t, 5.2, 6.2) * (1 - window(t, 7.2, 8.0))
        lh_reach = window(t, 5.2, 6.4) * (1 - window(t, 7.2, 8.0))
        lh_pos = add(scale(LEFT_UP, lh_up), scale((-2.0, 2.0, -3), lh_reach),
                     scale((0, 0.8 * math.sin(t * 2.3), 0), lh_up))
        # Translation only: turning a parked arm swings its far end up into the sky.
        lh_rot = (0, 0, 0)

        cam = 1.0 * math.sin(t * 0.9) * up
        k = _kick(t, 4.75, 2.4)
        return {
            'swing': {'rot': scale(sw_rot, w), 'pos': scale(sw_pos, w)},
            'wrist': {'rot': wr_rot},
            'weapon': {'rot': (tumble + tilt, face + spin + back, 0), 'pos': (0, 7 * toss_up, 0)},
            'lefthand_pos': {'pos': lh_pos, 'rot': lh_rot},
            'camera': {'rot': (cam * 0.6 + k, cam * 0.4 + k * 0.3, cam * 0.3 - k * 0.3)},
        }
    return pose


def _inspect_two(w):
    """A two-handed weapon's inspect, in six beats.

    Brought across the body; turned over between the hands; twirled end over end while the left
    hand lets go; planted forward hard; tipped to bring its head round to the camera; returned.
    The wrist stays within a few tens of degrees for the same reason as above - both arms hang
    off it here.
    """
    def pose(t):
        up = window(t, 0.0, 1.1) * (1 - window(t, 7.3, 8.6))
        plant = window(t, 4.1, 4.35) * (1 - window(t, 4.9, 5.6))
        look = window(t, 5.4, 6.5) * (1 - window(t, 7.3, 8.4))

        sw_rot = add(scale((-8, -12, 12), up), scale((10, -3, -3), plant), scale((-4, -6, 3), look))
        sw_pos = add(scale((1.5, 3.0, -1.5), up), scale((0, -2.0, -3), plant), scale((-2, 0.8, 0.8), look))
        wr_rot = (24 * plant - 22 * look + 3 * math.sin(t * 1.2) * up,
                  -24 * up + 8 * math.sin(t * 0.8) * up,
                  24 * up - 14 * look)

        face = 180 * smooth((t - 1.1) / 1.5)
        twirl = 360 * smooth((t - 2.6) / 1.3)
        back = 180 * smooth((t - 7.3) / 1.4)
        tilt = -55 * look
        # The left hand lets go for the twirl and is back on the shaft for the plant.
        off = window(t, 2.5, 2.8) * (1 - window(t, 3.8, 4.1))
        lh_pos = scale((0, -5, 2.5), off)
        lh_rot = scale((18, 0, -12), off)

        k = _kick(t, 4.35, 3.0)
        cam = 0.9 * math.sin(t * 0.8) * up
        return {
            'swing': {'rot': scale(sw_rot, w), 'pos': scale(sw_pos, w)},
            'wrist': {'rot': wr_rot},
            # The twirl is in the plane of the view: end over end toward the camera would bring
            # the head of a long weapon straight through it.
            'weapon': {'rot': (tilt, face + back, twirl)},
            'lefthand_pos': {'pos': lh_pos, 'rot': lh_rot},
            'camera': {'rot': (cam * 0.6 + k, cam * 0.3, -k * 0.4)},
        }
    return pose


# ---- summon: out of the portal and into the hand -------------------------------------------

# Where each summon's effect is centred, in eye blocks (the mod's SummonFx.PX/PY/PZ, plus the
# offset each style draws its focus at).
ORIGINS = {
    'eye': (-0.05, 0.12, -1.35),
    'rift': (-0.05, 0.12, -1.35),
    'wing': (-0.05, 0.12, -1.35),
    'forge': (-0.05, -0.10, -1.35),
    'bloom': (-0.05, -0.18, -1.35),
}


def _grip_at(rig, p_eye, swing_rot, swing_pos):
    """The `weapon` offset that puts the grip at an eye-space point while the swing bone holds the
    given pose: solved through the whole chain, swing then hold (the wrist is at rest)."""
    import numpy as np
    from geo import bone_local
    M = bone_local(rig.S, (0, 0, 0), swing_pos, swing_rot) @ bone_local(rig.G, rig.hold_rest)
    g = np.array([-rig.G[0], rig.G[1], rig.G[2], 1.0])
    target = np.append(np.array(p_eye) * 16.0, 1.0)
    t = (np.linalg.inv(M) @ target)[:3] - g[:3]
    return (-t[0], t[1], t[2])


def summon(rig, snd, style, length=2.6, extra=None):
    """The weapon comes out of the summon's own effect and is drawn into the hand.

    It appears at the centre of the portal, eye or whirl, tiny, and grows there turning over;
    the hand comes up and reaches toward it; the grip is held exactly in the portal - solved every
    key, so it stays put however the arm moves - until the hand closes on it; then the hand pulls
    it back to where it is carried, and the arm settles with the weight. Every beat is a fraction
    of the clip, which the mod scales the effect to, so the two cannot drift apart.
    """
    origin = ORIGINS.get(style, ORIGINS['rift'])
    L = length
    e0, e1, e2 = 0.36 * L, 0.56 * L, 0.70 * L
    low_rot, low_pos = (34, 12, -16), (0, -7, 3)
    reach_rot, reach_pos = (-12, -8, 8), (-2.0, 3.5, -5.0)
    c = Clip('summon', L)

    def pose(t):
        rise = smooth((t - e0 * 0.6) / (e1 - e0 * 0.6))
        back = spring((t - e2) / (L - e2), 2.0, 4.6) if t > e2 else 0.0
        sw_rot = mix(mix(low_rot, reach_rot, rise), (0, 0, 0), back)
        sw_pos = mix(mix(low_pos, reach_pos, rise), (0, 0, 0), back)
        grow = ease_out((t - e0) / (e1 - e0))
        fly = smooth((t - e1) / (e2 - e1))
        held = _grip_at(rig, origin, sw_rot, sw_pos)
        pos = scale(held, 1 - fly)
        s = max(0.001, grow)
        k = _kick(t, e2, 2.2)
        return {
            'swing': {'rot': sw_rot, 'pos': sw_pos},
            'weapon': {'pos': pos, 'scale': (s, s, s), 'rot': (0, 220 * (1 - grow) + 25 * (1 - fly), 0)},
            'camera': {'rot': (k, k * 0.3, -k * 0.35)},
        }

    def wrapped(t):
        p = pose(t)
        if extra is not None:
            extra(c, 'summon', t, t / L)
        return p
    sample(c, 0, L, wrapped, 24.0)
    c.sound(0, snd + 'summon')
    return c


def merge(base, own, keep):
    """The common clips, with a weapon's own versions of the ones named in `keep` in their place."""
    mine = {c.name: c for c in own if c.name in keep}
    out = [mine.pop(c.name, c) for c in base]
    out.extend(mine.values())
    return out
