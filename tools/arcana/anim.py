"""Animation clips for the Arcana weapons, written as Bedrock keyframes the mod's GeoChannel reads.

Keys are Catmull-Rom by default, so a handful of poses make a smooth motion; `snap` keys use a pre/post
step for a hard cut. Rotations are Bedrock degrees added to a bone's rest rotation; positions are
model units in the bone's parent frame.
"""


class Clip:
    def __init__(self, name, length, loop=False):
        self.name, self.length, self.loop = name, length, loop
        self.bones = {}
        self.sounds = {}

    def key(self, bone, channel, t, value, smooth=True, pre=None):
        ch = self.bones.setdefault(bone, {}).setdefault(channel, {})
        v = [round(float(x), 3) for x in value]
        if pre is not None:
            ch['%.4g' % t] = {'pre': [round(float(x), 3) for x in pre], 'post': v}
        elif smooth:
            ch['%.4g' % t] = {'post': v, 'lerp_mode': 'catmullrom'}
        else:
            ch['%.4g' % t] = v
        return self

    def pose(self, t, poses, smooth=True):
        """poses: {bone: {'rot': (x,y,z), 'pos': (x,y,z), 'scale': (x,y,z)}}"""
        for bone, p in poses.items():
            for ch, key in (('rot', 'rotation'), ('pos', 'position'), ('scale', 'scale')):
                if ch in p:
                    self.key(bone, key, t, p[ch], smooth)
        return self

    def sound(self, t, effect):
        self.sounds['%.4g' % t] = {'effect': effect}
        return self

    def to_json(self):
        out = {'animation_length': self.length, 'bones': self.bones}
        if self.loop:
            out['loop'] = True
        if self.sounds:
            out['sound_effects'] = self.sounds
        return out


def bundle(clips):
    return {'format_version': '1.8.0', 'animations': {c.name: c.to_json() for c in clips}}
