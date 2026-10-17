"""Bundles a TaCZ-style melee pack into the mod's own resources.

The mod can read packs from disk, but a pack built for 1.20 does not belong in a 1.8.9 `mods`
folder, so the one this port ships with is built in instead: the models, animations, textures and
item index go under `assets/soupapi/melee_packs/<namespace>/` exactly as they shipped, and the
sounds move into the mod's own `assets/soupapi/sounds/` so the game's sound engine can find them.

    python3 tools/bundle_melee_pack.py <pack.jar|pack-folder>

Re-run it to update or add a pack. Files it writes are listed in `__index.txt` inside the pack
folder, which is what the built-in reader walks instead of enumerating the classpath.
"""
import collections
import json
import pathlib
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
RESOURCES = ROOT / 'src/main/resources/assets/soupapi'
PACKS = RESOURCES / 'melee_packs'
SOUNDS = RESOURCES / 'sounds/melee'
SOUNDS_JSON = RESOURCES / 'sounds.json'

# Left out on purpose: the Lua state machines (a runtime 1.8.9 does not have, and the mod
# reproduces what they do from the clip names), the pack's own mod metadata, which would only
# confuse anyone reading the jar, and the "- 副本" duplicate slot icons.
SKIP_DIRS = ('scripts/', 'META-INF/', 'data/%s/recipes/')
SKIP_MARKERS = (' - 副本',)

# Files that only matter to the loader the pack was built for.
SKIP_FILES = ('pack.mcmeta', 'logo.png')
SKIP_SUFFIXES = ('gunpack_info.json', 'sounds.json', 'lang/zh_cn.json')


def shrink_json(blob):
    """Re-emits a model or animation file without whitespace, rounding to four decimals.

    A pack writes its JSON pretty-printed, and an animation is tens of thousands of numbers given to
    seven decimals - half a degree of rotation at the tenth of a millimetre. The models and clips
    here come to 5.5 MB pretty-printed and under 2 MB compact, which is the difference between a
    download that works on a phone and one that does not. Nothing visible changes: four decimals of
    a degree is a thousandth of a pixel at arm's length.
    """
    def round_floats(value):
        if isinstance(value, float):
            rounded = round(value, 4)
            return int(rounded) if rounded == int(rounded) else rounded
        if isinstance(value, list):
            return [round_floats(v) for v in value]
        if isinstance(value, dict):
            return collections.OrderedDict((k, round_floats(v)) for k, v in value.items())
        return value

    parsed = json.loads(blob.decode('utf-8-sig'), object_pairs_hook=collections.OrderedDict)
    return json.dumps(round_floats(parsed), ensure_ascii=False,
                      separators=(',', ':')).encode('utf-8')


def shrink_ogg(blob, name):
    """Re-encodes a sound to mono at a bitrate suited to a game effect.

    The pack ships 48 kHz stereo at about 320 kbps, which is mastering quality for a knife swing and
    6.9 MB of the download. Minecraft pans mono sounds and plays stereo ones flat, so mono is also
    the more correct choice here. Needs ffmpeg; without it the original is kept.
    """
    with tempfile.TemporaryDirectory() as directory:
        source = pathlib.Path(directory) / 'in.ogg'
        target = pathlib.Path(directory) / 'out.ogg'
        source.write_bytes(blob)
        try:
            subprocess.run(
                ['ffmpeg', '-v', 'error', '-y', '-i', str(source),
                 '-ac', '1', '-ar', '44100', '-c:a', 'libvorbis', '-b:a', '80k', str(target)],
                check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        except (OSError, subprocess.CalledProcessError) as error:
            print('  keeping %s as it is (%s)' % (name, error))
            return blob
        out = target.read_bytes()
        return out if 0 < len(out) < len(blob) else blob


def pack_files(source):
    """Yields (path-inside-pack, bytes) for a jar or a folder."""
    if source.is_dir():
        for path in sorted(source.rglob('*')):
            if path.is_file():
                yield path.relative_to(source).as_posix(), path.read_bytes()
        return
    with zipfile.ZipFile(source) as archive:
        for name in sorted(archive.namelist()):
            if not name.endswith('/'):
                yield name, archive.read(name)


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 1
    source = pathlib.Path(argv[1]).resolve()
    if not source.exists():
        print('no such pack:', source)
        return 1

    files = dict(pack_files(source))
    meta = files.get('gunpack.meta.json')
    if meta is None:
        print('not a gun pack: no gunpack.meta.json')
        return 1
    namespace = json.loads(meta.decode('utf-8'))['namespace']

    base = PACKS / namespace
    if base.exists():
        shutil.rmtree(base)
    base.mkdir(parents=True)
    if SOUNDS.exists():
        shutil.rmtree(SOUNDS)
    SOUNDS.mkdir(parents=True)

    sound_prefix = 'assets/%s/tacz_sounds/' % namespace
    written = []
    events = collections.OrderedDict()
    sound_bytes = 0

    for name, blob in sorted(files.items()):
        if any(skip % namespace in name if '%s' in skip else skip in name for skip in SKIP_DIRS):
            continue
        if any(marker in name for marker in SKIP_MARKERS):
            continue
        if name in SKIP_FILES or name.endswith(SKIP_SUFFIXES):
            continue

        if name.startswith(sound_prefix) and name.endswith('.ogg'):
            # melee/mercy/draw.ogg -> assets/soupapi/sounds/melee/mercy/draw.ogg
            relative = name[len(sound_prefix):]
            target = SOUNDS.parent / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            blob = shrink_ogg(blob, relative)
            target.write_bytes(blob)
            sound_bytes += len(blob)
            # The path already begins with the pack's own melee/ folder, so the event is just
            # that path with dots: melee/mercy/draw.ogg -> melee.mercy.draw, which is exactly what
            # MeleeSounds builds from the clip's cue.
            event = relative[:-4].replace('/', '.')
            events[event] = collections.OrderedDict([
                ('category', 'player'),
                ('sounds', [collections.OrderedDict([
                    ('name', 'soupapi:' + relative[:-4]),
                    ('stream', False),
                ])]),
            ])
            continue

        if name.endswith('.json'):
            try:
                blob = shrink_json(blob)
            except ValueError as error:
                print('  leaving %s alone (%s)' % (name, error))
        target = base / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(blob)
        written.append(name)

    (base / '__index.txt').write_text('\n'.join(written) + '\n', encoding='utf-8')

    # Merge the sound events into the mod's own sounds.json, keeping what is already there.
    existing = collections.OrderedDict()
    if SOUNDS_JSON.is_file():
        existing = json.loads(SOUNDS_JSON.read_text(encoding='utf-8'),
                             object_pairs_hook=collections.OrderedDict)
    for key in [k for k in existing if k.startswith('melee.')]:
        del existing[key]
    existing.update(events)
    SOUNDS_JSON.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + '\n',
                           encoding='utf-8')

    pack_bytes = sum((base / f).stat().st_size for f in written)
    print('bundled %s: %d files (%.1f MB), %d sounds (%.1f MB), %d events'
          % (namespace, len(written), pack_bytes / 1048576.0,
             len(events), sound_bytes / 1048576.0, len(events)))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
