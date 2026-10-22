"""The Nine-Veil Lotus's clips.

The flower is the whole weapon: nine blades, three per rank, each one its own bone. The hook here
opens and shuts it. Closed, the blades stand up over the heart; open, they fall back and the heart
is bare. A cut throws it open on the swing and it draws shut again behind the stroke.
"""
import math

from clips_common import build as common

SND = 'arcana:melee/lotus_glaive/'
PETALS = 9
# Which rank each blade belongs to: four outside, three in the middle, two over the heart.
RANK_OF = (0, 0, 0, 0, 1, 1, 1, 2, 2)


def _flower(clip, t, open_amount, spin=0.0):
    """`open_amount` 0 is shut over the heart, 1 is thrown fully back."""
    for i in range(PETALS):
        rank = RANK_OF[i]
        # The outer rank travels furthest and leads; the inner barely moves and lags.
        lead = 1.0 - rank * 0.3
        lay = -52.0 * open_amount * lead
        twist = spin * (18.0 + rank * 9.0)
        clip.key('petal_%d' % i, 'rotation', t, (lay, twist, 0))
        s = 1.0 + 0.1 * open_amount * lead
        clip.key('petal_%d' % i, 'scale', t, (s, 1.0, s))
    glow = 1.0 + 0.55 * open_amount + 0.06 * math.sin(t * 3.4)
    clip.key('heart_illuminated', 'scale', t, (glow, glow, glow))
    clip.key('heart_illuminated', 'rotation', t, (0, t * 48.0, 0))


def extra(clip, name, t, phase):
    if name == 'melee':
        # Thrown open through the cut, shut behind it.
        swell = max(0.0, math.sin(min(1.0, phase * 1.35) * math.pi))
        _flower(clip, t, swell, spin=swell * 0.6)
    elif name == 'flourish':
        # Fully open, turning, the whole way round.
        swell = math.sin(min(1.0, phase) * math.pi) ** 0.6
        _flower(clip, t, swell, spin=swell)
    elif name == 'inspect':
        held = 1.0 if 0.2 < phase < 0.8 else 0.0
        _flower(clip, t, 0.15 + 0.75 * held, spin=0.0)
    elif name == 'draw':
        _flower(clip, t, min(1.0, phase * 1.6) * (1.0 - max(0.0, phase - 0.6) * 2.0))
    elif name == 'put_away':
        _flower(clip, t, 0.0)
    else:
        # The hold: it breathes, never quite shut and never quite open.
        _flower(clip, t, 0.22 + 0.16 * math.sin(math.tau * phase), spin=0.08 * math.sin(math.tau * phase * 2))


def build(rig):
    return common(rig, SND, extra, idle_length=6.2, weight=1.2)
