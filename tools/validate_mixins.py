"""Check every registered mixin's target class and target methods against the 1.8.9 jar.

A mixin that names a class or method which does not exist fails at *class load* with a
mixin apply error, taking the game down before anything renders. Since this environment
cannot launch 1.8.9, this is the strongest check available: it reads the mixin sources,
resolves each @Mixin target and each @Inject/@Redirect/@ModifyArg/@ModifyReturnValue/
@ModifyExpressionValue/@WrapOperation `method = "..."` and `@Shadow` member, and verifies
them against the remapped Minecraft jar with javap.
"""
import json, re, subprocess, pathlib, sys

ROOT = pathlib.Path('/home/user/SoupAPI-recode')
JAR = ('/root/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/'
       '1.8.9-net.legacyfabric.yarn.1_8_9.1.8.9+build.604-v2/'
       'minecraft-merged-1.8.9-net.legacyfabric.yarn.1_8_9.1.8.9+build.604-v2.jar')

cfg = json.loads((ROOT / 'src/main/resources/soupapi.mixins.json').read_text())
pkg = cfg['package']
names = cfg.get('mixins', []) + cfg.get('client', []) + cfg.get('server', [])

_cache = {}
def members(fqcn):
    """Returns (method_names, field_names) for a class, or None if the class is absent."""
    if fqcn in _cache:
        return _cache[fqcn]
    r = subprocess.run(['javap', '-p', '-cp', JAR, fqcn], capture_output=True, text=True)
    if r.returncode != 0 or not r.stdout.strip():
        _cache[fqcn] = None
        return None
    methods, fields = set(), set()
    for line in r.stdout.splitlines():
        line = line.strip().rstrip(';')
        m = re.search(r'([\w$<>]+)\s*\(', line)
        if m:
            methods.add(m.group(1))
        else:
            m = re.search(r'(\w+)$', line)
            if m and '(' not in line and 'class' not in line:
                fields.add(m.group(1))
    _cache[fqcn] = (methods, fields)
    return _cache[fqcn]

MIXIN_TARGET = re.compile(r'@Mixin\s*\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class')
# method = "name" or method = "name(descriptor)V"
METHOD_REF = re.compile(r'method\s*=\s*"([^"(]+)')
SHADOW_BLOCK = re.compile(r'@Shadow[^;]*?\s(?:abstract\s+)?[\w.<>\[\],\s]*?\b(\w+)\s*[;(]', re.S)

problems, checked = [], 0
for name in names:
    rel = 'src/main/java/' + (pkg + '.' + name).replace('.', '/') + '.java'
    path = ROOT / rel
    if not path.exists():
        problems.append((name, 'mixin source file is missing: ' + rel))
        continue
    src = path.read_text()

    # Resolve the @Mixin target through the file's imports.
    tm = MIXIN_TARGET.search(src)
    if not tm:
        problems.append((name, 'no @Mixin(X.class) target found'))
        continue
    simple = tm.group(1)
    target = None
    if '.' in simple:
        target = simple
    else:
        for m in re.finditer(r'^import\s+([\w.]+\.' + re.escape(simple.split('.')[0]) + r')\s*;', src, re.M):
            target = m.group(1)
            if simple.count('.') == 0:
                break
        if target and simple.count('.'):
            target = target + '$' + simple.split('.', 1)[1].replace('.', '$')
    if not target:
        problems.append((name, 'could not resolve target class ' + simple))
        continue

    info = members(target)
    if info is None:
        problems.append((name, 'TARGET CLASS ABSENT in 1.8.9: ' + target))
        continue
    methods, fields = info
    checked += 1

    for ref in set(METHOD_REF.findall(src)):
        ref = ref.strip()
        if ref in ('<init>', '<clinit>'):
            if '<init>' not in methods and target.split('.')[-1] not in methods:
                problems.append((name, 'constructor not found on ' + target))
            continue
        if ref not in methods:
            problems.append((name, 'method "%s" NOT FOUND on %s' % (ref, target)))

    for shadowed in set(SHADOW_BLOCK.findall(src)):
        if shadowed not in methods and shadowed not in fields:
            problems.append((name, '@Shadow member "%s" not found on %s' % (shadowed, target)))

print('Registered mixins: %d   target classes resolved: %d' % (len(names), checked))
if problems:
    print('\nPROBLEMS (%d):' % len(problems))
    for n, p in problems:
        print('  %-42s %s' % (n, p))
    sys.exit(1)
print('\nAll registered mixin targets and members exist in 1.8.9.')
