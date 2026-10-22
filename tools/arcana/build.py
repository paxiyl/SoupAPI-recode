"""Builds the Arcana weapon pack into the mod's resources.

    python3 tools/arcana/build.py

Writes assets/soupapi/melee_packs/arcana/ (models, clips, textures with their _s glow masks and
finish variants, display and index files, lang, __index.txt) and the weapons' sounds under
assets/soupapi/sounds/melee/<id>/ with their sounds.json entries. Everything is generated; nothing
here comes from another pack. Run from anywhere.
"""
import json
import os
import pathlib
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from geo import write_json  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
RES = ROOT / 'src/main/resources/assets/soupapi'
PACK = RES / 'melee_packs/arcana'
NS = 'arcana'


def weapons():
    # The katana's shape was replaced by the staff from the concept art; it keeps the same id,
    # sounds and clips, so a saved config and the animation work both carry over.
    import staff
    import clips_katana
    out = [(staff, clips_katana)]
    try:
        import raven
        import clips_raven
        out.append((raven, clips_raven))
    except ImportError:
        pass
    try:
        import spectral
        import clips_spectral
        out.append((spectral, clips_spectral))
    except ImportError:
        pass
    # The newer weapons, which take their clips from `clips_common` and only write their own
    # moving parts: the scepter's rings and the lotus's blades.
    for name in ('orrery', 'lotus'):
        try:
            module = __import__(name)
            clips = __import__('clips_' + name)
            out.append((module, clips))
        except ImportError:
            pass
    return out


def main():
    files = []

    def emit(rel, writer):
        path = PACK / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        writer(str(path))
        files.append(rel)

    lang = {'pack.arcana.name': 'Arcana', 'pack.arcana.desc': 'Original weapons built for SoupAPI.'}
    sounds_index = {}
    for module, clips in weapons():
        wid = module.ID
        model, rig, mesh_groups = module.build()
        emit('assets/%s/geo_models/melee/%s_geo.json' % (NS, wid),
             lambda p: write_json(p, model.to_json('geometry.%s' % wid)))
        for finish in module.FINISHES:
            suffix = '' if not finish else '_' + finish
            canvas = module.paint_atlas(finish)
            emit('assets/%s/textures/melee/%s%s.png' % (NS, wid, suffix), lambda p: canvas.save(p))
            emit('assets/%s/textures/melee/%s%s_s.png' % (NS, wid, suffix),
                 lambda p: canvas.save(p + '.tmp.png', p) or os.remove(p + '.tmp.png'))
        # Triangle geometry, where the weapon is built from meshes rather than boxes.
        if isinstance(mesh_groups, dict) and mesh_groups:
            from mesh import write_mesh
            rel = 'assets/%s/geo_models/melee/%s_geo.mesh' % (NS, wid)
            path = PACK / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            count = write_mesh(str(path), mesh_groups)
            files.append(rel)
            print('  %s: %d triangles' % (wid, count))

        from anim import bundle
        clip_list = clips.build(rig)
        emit('assets/%s/animations/melee/%s.animation.json' % (NS, wid),
             lambda p: write_json(p, bundle(clip_list)))
        display = {
            'model': '%s:melee/%s_geo' % (NS, wid), 'animation': '%s:melee/%s' % (NS, wid),
            'texture': '%s:melee/%s' % (NS, wid), 'slot_texture': '%s:melee/slot/%s' % (NS, wid),
            'soupapi': module.PROFILE,
        }
        emit('assets/%s/display/melee/%s.json' % (NS, wid), lambda p: write_json(p, display))
        emit('data/%s/index/melee/%s.json' % (NS, wid),
             lambda p: write_json(p, {'name': 'item.%s.%s' % (NS, wid), 'type': 'soupapi:cosmetic'}))
        lang['item.%s.%s' % (NS, wid)] = module.NAME
        slot = PACK / ('assets/%s/textures/melee/slot/%s.png' % (NS, wid))
        if slot.exists():
            files.append('assets/%s/textures/melee/slot/%s.png' % (NS, wid))
        try:
            import sounds
            for name in sounds.write(wid, RES / ('sounds/melee/%s' % wid)):
                sounds_index['melee.%s.%s' % (wid, name)] = {
                    'category': 'player',
                    'sounds': [{'name': 'soupapi:melee/%s/%s' % (wid, name), 'stream': False}]}
        except ImportError:
            pass
    emit('assets/%s/lang/en_us.json' % NS, lambda p: write_json(p, lang))
    emit('gunpack.meta.json', lambda p: write_json(p, {'namespace': NS}))
    (PACK / '__index.txt').write_text('\n'.join(sorted(set(files))) + '\n', encoding='utf-8')

    sj = RES / 'sounds.json'
    data = json.loads(sj.read_text(encoding='utf-8'))
    data = {k: v for k, v in data.items() if not any(k.startswith('melee.%s.' % m.ID) for m, _ in weapons())}
    data.update(sounds_index)
    sj.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('wrote', len(set(files)), 'pack files and', len(sounds_index), 'sound events')


if __name__ == '__main__':
    main()
