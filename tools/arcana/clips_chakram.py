"""Seraph's Halo's clips.

Everything but the halo's own motion comes from `clips_common`. The hook turns the ring on its own
bone - slowly while held, whirring up through a strike, spun like a top for the inspect - and beats
the wings open on the cut and folds them after.
"""
import math

from clips_common import build as common, summon

SND = 'arcana:melee/seraph_halo/'


def extra(clip, name, t, phase):
    if int(round(t * 20)) % 2:
        return
    if name == 'static_idle':
        spin = 360.0 * t / 6.0                      # one turn a loop, so the loop is seamless
    elif name == 'melee':
        spin = 360.0 * (t * 0.6 + 2.2 * max(0.0, min(1.0, (phase - 0.1) / 0.45)))
    elif name == 'inspect':
        # Spun up like a top for the middle of the inspect, and let run down.
        whirl = max(0.0, min(1.0, (t - 2.6) / 0.5)) * (1 - max(0.0, min(1.0, (t - 6.0) / 1.5)))
        spin = 60.0 * t + 1440.0 * whirl * (t - 2.6) / 3.4
    elif name == 'flourish':
        spin = 360.0 * 3 * min(1.0, phase)
    else:
        spin = 60.0 * t
    clip.key('ring_spin', 'rotation', t, (0, 0, spin))
    beat = 0.0
    if name == 'melee':
        beat = max(0.0, 1 - abs(phase - 0.35) * 3.5)
    elif name in ('summon', 'draw'):
        beat = max(0.0, 1 - abs(phase - 0.6) * 3)
    elif name == 'static_idle':
        beat = 0.15 * (0.5 + 0.5 * math.sin(math.tau * t / 6.0 * 2))
    elif name == 'inspect':
        beat = 0.4 * max(0.0, 1 - abs(t - 1.5) / 1.0) + 0.6 * max(0.0, 1 - abs(t - 6.2) / 0.8)
    for side, bone in ((1, 'wing_r'), (-1, 'wing_l')):
        clip.key(bone, 'rotation', t, (0, side * 28 * beat, -side * 34 * beat))


def build(rig):
    return common(rig, SND, extra, idle_length=6.0, weight=0.95) + [summon(rig, SND, 'wing', 2.4, extra=extra)]
