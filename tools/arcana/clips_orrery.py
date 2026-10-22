"""The Hour Unwound's clips.

Everything but the moving parts comes from `clips_common`; the hook below is the weapon itself.
The three rings turn the whole time, on three different axes at three different rates, and the six
rune plates ride round the head a little behind them. The rates are chosen so a looping clip ends
on a whole number of turns for every ring, or the loop would snap.
"""
import math

from clips_common import build as common

SND = 'arcana:melee/orrery_scepter/'
RUNES = 6

# Turns per second for each ring, and the axis each one turns about.
RATES = (0.5, -0.75, 1.0)
AXES = ((0, 1, 0), (1, 0, 0), (0, 0, 1))


def _spin(clip, name, t, phase, scale=1.0, flare=0.0):
    for i in range(3):
        degrees = RATES[i] * 360.0 * t * scale
        ax, ay, az = AXES[i]
        clip.key('ring_%d' % i, 'rotation', t, (degrees * ax, degrees * ay, degrees * az))
        if flare:
            s = 1.0 + flare * (0.18 + 0.1 * i)
            clip.key('ring_%d' % i, 'scale', t, (s, s, s))
    # The runes ride the head, each a sixth of a turn apart, drifting out when the weapon moves.
    for k in range(RUNES):
        a = t * 0.35 * 360.0 + k * (360.0 / RUNES)
        clip.key('rune_%d' % k, 'rotation', t, (0, a, 0))
        out = 1.0 + flare * 0.55
        clip.key('rune_%d' % k, 'scale', t, (out, out, out))
    # The star breathes, and flares on a cut.
    pulse = 1.0 + 0.08 * math.sin(t * 3.1) + flare * 0.45
    clip.key('core_illuminated', 'scale', t, (pulse, pulse, pulse))


def extra(clip, name, t, phase):
    if int(round(t * 20)) % 2:
        return
    if name == 'melee':
        # The rings race on the cut and the star flares with it.
        kick = max(0.0, 1.0 - abs(phase - 0.45) * 3.0)
        _spin(clip, name, t, phase, scale=1.0 + 2.4 * kick, flare=kick)
    elif name == 'flourish':
        # Thrown open: the rings fly out and spin up, then are gathered back in.
        swell = math.sin(min(1.0, phase) * math.pi)
        _spin(clip, name, t, phase, scale=1.0 + 3.2 * swell, flare=swell)
    elif name == 'inspect':
        # Stopped dead and turned over by hand, then let go again.
        held = 1.0 if 0.15 < phase < 0.75 else 0.0
        _spin(clip, name, t, phase, scale=1.0 - 0.95 * held, flare=0.0)
    else:
        _spin(clip, name, t, phase)


def build(rig):
    from clips_common import summon
    return common(rig, SND, extra, idle_length=6.0, weight=1.15) + [summon(rig, SND, 'bloom', 2.6, extra=extra)]
