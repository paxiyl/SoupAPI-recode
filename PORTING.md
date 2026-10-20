# SoupAPI — Minecraft 1.8.9 (Forge)

This branch ports SoupAPI from Fabric 1.21.4 to **Minecraft Forge 1.8.9**, with
PojavLauncher (Android) as a first-class target.

## Install

Drop the jar into `.minecraft/mods/`. Requirements:

- Minecraft **1.8.9** with **Forge 11.15.1.2318** (or any 1.8.9 Forge build).
- Nothing else. No Fabric API, no ModMenu, no Cloth Config, **no Mixin, no coremod and
  no tweaker arguments** — it is a plain Forge mod jar.
- Client-only (`@Mod(clientSideOnly = true)`), so servers do not need it and will not
  reject you for having it.

Open the settings screen with **right-ctrl + right-alt**. Config is written to
`.minecraft/config/soupapi.json`.

## Build

```
./gradlew setupDecompWorkspace
./gradlew build
```

Output: `build/libs/SoupAPI-<version>.jar`.

The toolchain is pinned by necessity: **ForgeGradle 2.1** is the last version that
supports 1.8.9 (2.3 requires 1.12+), and it requires **Gradle 2.14.1** on a **Java 8
JDK**. Newer Gradle or JDKs will not run it. Mappings are MCP `stable_20`.

## How the port is structured

The 1.21.4 sources are kept as close to original as possible. Rather than rewriting
every call site, the port provides a compatibility layer in
`me.Padej_.soupapi.compat` whose classes carry the **same names and method
signatures** as the 1.21.4 classes they stand in for, so most files needed only an
import change.

| 1.21.4 | 1.8.9 Forge port | Why |
|---|---|---|
| `org.joml.*` | `compat.Mat4`, `Quaternionf`, `Vector3f/4f` | JOML does not exist; the quaternion follows JOML's intrinsic `rotateX/Y/Z` convention, which the billboarded particles depend on |
| `MatrixStack` | `compat.MatrixStack` | 1.8.9 has only the GL matrix stack; vertices are transformed on the CPU, as `vertex(matrix, x, y, z)` already implied, which also cuts fixed-function matrix traffic on gl4es |
| `RenderSystem` | `compat.RenderSystem` | maps to `GlStateManager`; there is no shader colour uniform, so `setShaderColor` is folded into vertex colours by the buffer builder, and `setShader` becomes texture enable/disable |
| `BufferBuilder` | `compat.BufferBuilder` | wraps MCP's `WorldRenderer` — which is the *vertex buffer* in this version — handling implicit vertex termination and attribute ordering |
| `DrawContext` | `compat.DrawContext` | 1.8.9 draws GUIs through static `Gui` helpers with no context or matrix stack |
| `Screen` | `compat.SoupScreen` | MCP's `GuiScreen` has `drawScreen`/`keyTyped`/`mouseClickMove`, no scroll callback and no title |
| `Camera` | `compat.Camera` + `Cameras` | 1.8.9 has no camera object with position and rotation accessors |
| Cloth Config / AutoConfig | `config.*` + `config.annotation.*` | no 1.8.9 build; the annotations mirror Cloth's nesting exactly, so the 747-line config class ported with an import change |
| Fabric API events | Forge's event bus, via `event.ForgeEventBridge` | see below |

### No mixins

The Fabric port needed 30 mixins. **This port has none.** Forge 1.8.9 already publishes
an event at every point the mod needs:

| Mod feature | Forge event |
|---|---|
| per-tick module updates | `TickEvent.ClientTickEvent` |
| world rendering | `RenderWorldLastEvent` |
| HUD rendering | `RenderGameOverlayEvent.Post(ALL)` |
| custom health/armour/food/air/XP bars | `RenderGameOverlayEvent.Pre(<element>)`, cancelled per element |
| hit particles, bubbles, sound | `AttackEntityEvent` |
| custom fog | `EntityViewRenderEvent.FogColors` / `FogDensity` |

The per-element HUD cancellation is worth calling out: **1.8.9 itself draws health,
armour, food and air inside one private method**, so the custom HUD styles are not
implementable on Legacy Fabric without bytecode surgery. Forge raises a separate
cancellable event for each, so on Forge the feature works properly.

## PojavLauncher / gl4es notes

1.8.9's rendering is fixed-function, which translates to GL ES far better than modern
Minecraft does. The port additionally avoids the things that do break:

- **No destination-alpha tricks.** The target HUD masked the player head by clearing the
  alpha channel and blending with `GL_DST_ALPHA`. Android EGL configs frequently have no
  alpha channel, and a mid-frame `glClear` is expensive on tiled GPUs.
- **No GL 3 core, VAOs, FBO blits or GLSL.** `GL40C` references became `GL11`; LWJGL 2 is
  the only binding available.
- **Power-of-two glyph atlases.** gl4es is only dependable with POT dynamic textures.
- **AWT font fallback.** 1.8.9 needs `BufferedImage`/`ImageIO` to boot so those always
  work, but `java.awt.Font` rasterisation depends on libfontmanager/freetype, which is
  not reliably present in Pojav's JREs. `Caps.canRasteriseAwtFonts()` probes it and the
  font renderer falls back to vanilla's bitmap font rather than losing all text.
- **Line width.** gl4es clamps `glLineWidth` to 1.0; `Caps.thickLinesSupported()` reports
  this so callers can draw geometry instead.
- **Guarded input probing.** Pojav's LWJGL 2 stub does not implement every query.
- **No tweaker or coremod**, which on Pojav would mean extra launch arguments.
- **Discord Rich Presence is disabled on Android** — no native discord-rpc build exists
  for it and JNA cannot load one.

`Caps.describe()` is logged at startup and reports all of the above for the running device.

## Features that cannot exist on 1.8.9

These depend on game content or engine structure that 1.8.9 does not have:

| Feature | Status |
|---|---|
| Totem pop particles | No Totem of Undying before 1.11 — nothing can trigger it |
| Crossbow trajectories (and Multishot's triple arc) | Crossbows are 1.14+ |
| Off-hand rendering / off-hand trajectories | The off-hand is 1.9+ |
| Elytra ("glider") trails | Elytra are 1.9+ |
| Glowing-entity detection in target render | The Glowing effect is 1.9+; armour is used as the only tell for invisible entities |
| Attack-cooldown term in the crit check | The cooldown is 1.9+, so every 1.8.9 swing is full strength |
| Infinite potion durations | Added in 1.19 |
| Custom aspect ratio | Forge exposes no hook for the projection's aspect; `FOVUpdateEvent` only carries field of view, and this port deliberately uses no bytecode surgery |
| Cursor shape changes | LWJGL 2 has no standard-cursor API, and Pojav is touch-driven |
| `CustomPhysicParticle` / the `star` particle type | Removed: 1.8.9 has no particle registry, and nothing ever spawned it upstream |
| Discord rich presence on Android/Pojav | No `discord-rpc` native exists for Android and the Discord app exposes no IPC socket; the module disables itself there instead of crashing (see below) |

## The models: a cube's rotation is not a bone's

The weapons were still wrong, and the reason was one sign.

A Bedrock model stores a rotation on each bone and, separately, a rotation on each cube — and the
two are in **opposite handedness**. A bone's X and Y read as written; a cube's do not. Applying the
bone convention to cubes turns every rotated cube the wrong way about two of its three axes, which
leaves a weapon as a scatter of slabs roughly where the weapon should be: the silhouette is about
right, every piece inside it is not.

It was settled against ground truth rather than guessed. The pack ships a rendered icon of each
weapon in `textures/melee/slot/`, so the model can be drawn from the bind pose and put next to the
picture the artist made of it. With X and Y negated on cube rotations only, the Creed's finger
holes, serrated spine and red inlays all land where the icon has them; Sea Gleam reads as the hammer
it is; Mercy's curved blade and brass fittings line up. Under any other combination they do not.

**The order matters too, and the first check could not see it.** Half the rotated cubes here — 2,787
of 5,520 — turn about more than one axis, and for those the order the three rotations are composed
in changes the result. The first comparison was drawn at 420 pixels with no anti-aliasing and on
Dark Star, which draws both of its two hidden halves in the bind pose and so looks like a mess
whatever the convention: it could not have told the orders apart, and reporting it as a match was
wrong. Drawn large, on the Executor, which has five hundred multi-axis cubes and no hidden halves,
the answer is plain: ZYX gives a clean knife with its guard notch and wrapped grip, XYZ gives
scattered slabs, ZXY gives a blade with a ragged spine. ZYX is what the code uses. Six weapons at
900 pixels against their icons now agree down to the inlays.

Bone rotations were checked the same way, numerically: for each weapon, under each convention, the
gap between the fist and the nearest point of the weapon. Unchanged signs win on eight of eleven
weapons and by a wide margin on the clear ones — the hand holds the grip. So bones keep their signs
and cubes are negated, which is the one combination that satisfies both tests.

**On converting to OBJ:** it would not help, and would cost the animations. An OBJ is triangles and
nothing else — no bones, no pivots, no hierarchy — and every one of these animations is a list of
rotations *per bone*. Converting would throw away the only thing the clips can be applied to. The
cube format was never the problem; one sign in how it was read was.

## Weapons and keys in the settings

The weapons were reachable through a button that opened a sub-screen, which is no use to someone who
does not already know the button is there. They have a page of their own in the sidebar now, one row
per weapon: the switch keeps it in the cycle, and switching one on also carries it. The list cannot
come from the config — the weapons are whatever packs are on disk, known only once those are read —
so the page is built beside the config's own.

The keys have a page too. They are in Minecraft's Controls screen as well, but reaching that from
PojavLauncher means leaving the mod, scrolling past two hundred vanilla bindings and coming back.
Tap a row, press a key; Escape unbinds; right-click resets. Rebinding rebuilds Minecraft's key-code
table, without which a changed binding keeps firing on the old key and not the new one.

## Hands that move with the weapon

The arms are hung off the pack's own hand bones now. The mesh stays off — it is posed around an
anchor this port cannot reuse — but the **bones** {@code lefthand_pos} and {@code righthand_pos} are
animated by every clip whether their mesh is drawn or not, so the player's own arm placed on that
matrix moves exactly with the animation instead of sitting still under a weapon that is swinging.
Their model, their skin, their arm width. Drawn inside the weapon's own matrix, so switching them on
cannot move the viewmodel.

## Spam-clicking keeps swinging

Two sources now, because neither alone is enough: the attack key's own edge, which catches a click
while an animation is already running, and the vanilla swing flag, which catches a hit made some
other way. A click landing mid-swing is held rather than dropped and spends itself as soon as the
current swing is far enough through — `Combo Window`, 30% by default — so spamming or holding the
button keeps the weapon swinging instead of playing one animation and ignoring the rest.

## Why the animated skies did nothing

They were gated behind Colored Sky being switched on, and Theme Sky &amp; Fog — which is what
switches that on — was off by default. So picking a sky style changed nothing and there was no way
to tell why. Picking a sky is enough to get a sky now, and a theme brings its world by default,
which is most of what made every theme feel like the same theme.

## Presets

Every option ships off, which is right for a mod that replaces your hand and paints the sky, and a
poor first impression: turn it on and nothing happens, then turn on six things and it looks like a
mess. Twelve presets — PvP, PvP with the weapon, Showcase, Matrix, Winter, Inferno, Abyss,
Vaporwave, Occult, Performance, Everything — each switching on a set chosen to work together, and
switching the rest **off**, which is the part that matters.

A preset owns the visual state and nothing else: not the friends list, the capes, the HUD layout,
which weapon is carried, which weapons are switched off, or the saved per-weapon placements.

The first version named the fields it reset, which is how it went wrong: the list missed a dozen
style enums and every melee slider, so the same preset over two different configs gave two different
results — the complaint exactly. It resets by a rule now, and a probe applies each preset twice,
once over defaults and once over a deliberately messy config, and fails if the two differ.

**Every toggle was audited** for a live code path: of forty-one, exactly one does nothing, and that
is the Totem shader, which cannot work on a version with no Totem of Undying and is already marked
unavailable in the settings with that reason.

## Two weapons were drawn with their biggest piece missing

The arms in these packs are the bones `lefthand_pos` and `righthand_pos`. They were matched by
looking for "hand" anywhere in the name — which also caught Desmoulins' Edge's `hand` (156 cubes: its
grip) and the Executor's `handle` (84 cubes). Both were hidden *and* left out of the fit, so those
two were drawn with their largest part missing and sized as though it were not there. The match is a
suffix now, which is what it should always have been.

**The glow had no mask on one weapon.** It is masked by the pack's specular map, and "Free Dark
Star" is the one weapon here that ships no `_s` file; the fallback was the base texture, which lights
the whole weapon evenly instead of its metal. It skips the pass instead.

**The trail drew a band beside the weapon.** It followed the pivot of the furthest bone, and a pivot
can sit anywhere — on some weapons that was a point out in space next to the model, so the ribbon ran
parallel to the blade and never touched it. It follows the same mesh vertex the alignment uses now,
and clears the moment a swing ends instead of hanging in the air.

**The weapon jumped when you ran.** The packs' `run` clips move the weapon a long way off the idle
pose, for the same reason the arms are wrong: they are posed around an anchor this port cannot
reuse. `Sprint Animation` is off by default.

**Spam-clicking dropped every click but the first.** A new swing would not start until the old one
finished, and these clips run one and a half to two seconds — so the weapon kept swinging on its own
and the clicks went nowhere. A swing can be cut into once it is past its own halfway point, which is
where the pack's own sound cues sit.

## Hands

With the pack's arms off the weapon floated with nothing holding it, which looks worse than either.
These are the arms Minecraft already has: the player's own model, their own skin, their own
slim-or-wide arm, drawn at a fixed place in the view and rising with the swing. They are posed to
reach the weapon rather than the other way round, so switching them on cannot move the viewmodel —
and they have their own reach, spread, pitch and offsets, because unlike the weapon's placement
there is nothing in any file to fit them against.

## Skies that move

A theme painted the sky one flat tint, which is most of why every theme ended up feeling like the
same theme: the biggest surface in the game was a single colour that only changed hue.

There are twenty-one moving skies now — aurora curtains, nebula clouds, bands, plasma, a breathing
void rift, a perspective grid, a honeycomb, scanlines, a vortex, an eclipse, falling code, heat off a
horizon, light shafts through water, lava cells, late sun, ripples, speed lines, a lattice, silk,
a storm front and circuit traces — plus scattered stars for the ones that suit them.

They are drawn as a forty-by-twenty-four grid of quads at the far plane, each corner coloured by a
function of where it is and what time it is. That is the only way to get a moving pattern across the
sky on this version: there is no shader to write a noise function into, and gl4es would not run one
if there were. Under a thousand quads costs nothing beside a single chunk, and because the colour is
evaluated per corner and interpolated across each quad it comes out smooth rather than blocky. Each
theme picks the sky that goes with its ambience — code rain gets a sky of falling code, embers get a
glowing horizon — and every pattern was rendered offline and checked before it shipped.

**Thirteen more ambience styles** (53 in all): fireworks, cathedral motes, slipstream, falling
hexagons, watching eyes, clockwork, ice, coins, arrow rain, a vigil of crosses, prisms and tumbling
infinities. **Six more hit effects** (16 in all): spiral, halo, gyro, shrapnel, nova and ascend.

## Pointing every blade the same way

Fixing the scale left some weapons still plainly wrong, and the reason is that fitting a weapon only
moved it and sized it — it never turned it. The packs do not agree on which way a weapon faces in
its own model. Measured here: seven of the eleven point their blade along -X, Dragon's Fang and Creed
point up and away, Sea Gleam points **down**, and Mercy points back at the camera. Nothing in the
files says which way is forward, and the view anchors that might have implied it are the ones that
already disagree by sixty units.

So the blade is found and the model is turned to match. The tip is the posed mesh vertex furthest
from the weapon's centre, which on a knife is the point; the direction from centre to tip is turned
onto one target — up, a little left, a little away — as a single rotation about the weapon's own
centre. The turns it works out range from 28 degrees for Creed to 143 for Sea Gleam, and afterwards
all eleven sit in the hand the same way. The animation plays on top of it untouched, because this is
one fixed rotation around a point, not a change to any bone. `Auto-Align The Blade` turns it off.

**Placement can be saved per weapon.** One set of sliders cannot suit eleven weapons of different
shapes: a position that puts a dagger where you want it puts a greatsword through your face. The
sliders stay global — they are what a weapon starts from — and `Save Placement For This Weapon`
gives the weapon in hand its own copy, which is what it is drawn with from then on. The other ten
keep following the sliders. Stored as one readable line per weapon in the config.

**The weapon only appears when you are holding one.** `Show When` defaults to Sword: the weapon
replaces the hand, so drawing it over a bow or a stack of blocks would hide the thing actually being
used. Sword, sword or axe, any tool, any item, or always.

## A scroll bar you can actually hit

PojavLauncher has no scroll wheel, and three pixels of decoration is not a control. The settings
list has a real rail now: an up arrow, a thumb you can drag or tap past, a down arrow, ten pixels
wide. Tapping the track pages by a screenful. The weapon list and the enum pickers — forty-three
themes, a hundred and twenty capes — get arrows of their own and can be dragged, which is what made
them reachable at all without a wheel.

## What the first screenshots showed

The weapons drew, and looked like shattered glass: enormous flat pink sheets across the screen with
something metallic buried inside. Three separate causes, each visible in the shots.

**The arms were setting the scale.** The fit normalised the whole model — and most of that model is
a metre of forearm reaching back toward the camera. Normalising that to a block left the blade tiny
and the sleeves vast, and since the arms pass the camera on their way back, what filled the screen
was the inside of a sleeve. The blade sets the scale now (0.55 blocks at Size 100), measured with
the hand bones excluded, and it is the blade that gets centred.

**The arms are off by default.** They are authored around the view anchor of the mod the pack was
built for, and this port cannot use that anchor — the packs disagree about where it is. Rendered
against a blade-centred fit they are simply in the wrong place. `Show The Pack's Arms` turns them
on and switches to a second fit measured at load: the whole assembly framed to a block and pushed a
third of a block further out, so the arms are seen from outside. Both were checked by rendering them
offline; the weapon-only mode is the one that reads as a weapon in a hand.

**The rim light was painting the model flat.** It used the inverted-hull trick — draw the model
slightly larger, keep only the far faces — which needs front-face culling. The model is drawn
mirrored in X, which flips the winding, so culling the front culled the half it was meant to keep
and left a solid sheet of accent colour over the artwork. It draws before the textured pass now,
additive and writing no depth, so the real model covers the middle and only the rim survives; no
culling games, and nothing to get backwards.

**And the effects were louder than the weapon.** Glow at 110% over the pack's specular map blew out
to white, the rim light sat on top of that, three phantom copies smeared the geometry across the
frame, and with a pink theme every one of those passes was pink. The weapon is the subject now: glow
at 45%, rim light, phantom copies, swing rings and chromatic split all off by default, and the
additive passes ordered so the artwork is drawn on top of them rather than under.

There is also a `Reset Placement` button, because size, distance, offsets and angles are the
settings most likely to end up somewhere unusable while being tuned.

## The download, and the 500 MB of texture memory behind it

Bundling the pack took the jar to 22 MB, which turned out to be too big to download on the phone it
is for. Going looking for the weight found a worse problem than the download.

**The textures were sized for the art, not for the screen.** The MCTiers icons were 3375x3375 for an
eighteen-pixel HUD icon. The capes were 2048x1024 for a region a player model samples at 10x16. A
GPU does not care what a PNG weighs on disk, it cares what it costs once uploaded — four bytes a
pixel, every pixel — and the set as shipped came to **528 MB of texture memory**. On a phone running
gl4es that is not a download problem, it is a will-it-run problem. `tools/shrink_textures.py` caps
each group at a few times the largest size it is ever drawn at (128 for the HUD icons, 512 for the
capes, rings and target sprites), which leaves room for GUI scale 4 and a ring filling the screen:
36 files, 506 MB of texture memory saved, and nothing visibly different.

**3.2 MB of textures nothing drew.** Every ring and particle shipped twice, as a base and an
`_unblack` twin. Under the additive blend every one of those draws uses the two are mathematically
identical — and the code bound the base every time; all eleven `_unblack` constants were declared and
never read. They are gone, and the generators no longer emit them.

**The pack's own files.** Its JSON is pretty-printed and gives rotations to seven decimals, which is
a thousandth of a pixel at arm's length: re-emitted compact and rounded to four, the models and clips
went from 5.0 MB to 3.2 MB. Its sounds were 48 kHz stereo at ~320 kbps — mastering quality for a
knife swing, and 6.9 MB of the download; at mono 80 kbps they are 1.8 MB, and mono is the more
correct choice anyway because Minecraft only pans mono sounds. Also dropped: the pack's loader
metadata, its logo, its recipes and its duplicate slot icons.

**JNA's twenty-odd platform stubs.** It is shaded in for Discord rich presence, which is desktop
only, and it was shipping native stubs for AIX, SunOS, MIPS, s390x and PPC. The jar task keeps the
four platforms Minecraft 1.8.9 runs on.

Together: **22.4 MB to 8.7 MB**, with the whole weapon pack in it — smaller than the jar was before
the pack existed.

## Weapons from a gun pack, with their own animations

The mod can now carry a melee weapon out of a TaCZ-style gun pack: the Bedrock model, the animation
set, the hand model that comes with it, and the pack's own idle, draw, swing, inspect and sprint
clips.

**The pack is built in.** A pack made for 1.20 *is* a 1.20 mod file — `META-INF/mods.toml`,
`lowcodefml`, a dependency on two mods that do not exist here — so telling a 1.8.9 player to drop one
in their `mods` folder is telling them to break their install. The DeltaForce pack is bundled
instead, under `assets/soupapi/melee_packs/delta_wt/`, laid out exactly as it shipped, with
`tools/bundle_melee_pack.py` to add or update one. Its sounds move into the mod's own
`assets/soupapi/sounds/` so the game's sound engine can find them.

A built-in pack carries a generated `__index.txt` listing its files, because the classpath cannot be
walked for resources the way a zip can; that one file is the difference between the reader working
on a folder, a zip and the jar itself.

Disk discovery still works, for packs the mod does not ship: a `gunpack.meta.json` inside any zip,
jar or folder in `soupapi/melee/`, `mods/`, `resourcepacks/`, `tacz/`, `tacz_packs/` or `gunpacks/`,
plus whatever `Extra Pack Folder` points at. A pack whose namespace is already built in is skipped
there, so the same pack on disk does not load twice.

The bundled copy leaves out the Lua state machines (a runtime 1.8.9 does not have, and the mod
reproduces what they do from the clip names), the pack's own mod metadata, and the duplicate slot
icons — 95 files, 6.9 MB of models, animations and textures, plus 6.9 MB of sound.

**On the licence:** the pack is marked All Rights Reserved by LesRaisins Studio, and the weapons
replicate another game's. Bundling it was the repository owner's decision, taken after that was
pointed out; the repository is being made private. Anyone forking this should bring their own pack
rather than redistributing this one.

### What had to be built

| Piece | What it does |
|---|---|
| `bedrock/GeoModel` | Parses Bedrock geometry — bones, pivots, per-face UVs, cube rotation, inflate — and compiles each bone's cubes into a display list |
| `bedrock/GeoChannel` | One animated channel, with all four keyframe shapes these packs use: a vector, a bare number, `pre`/`post` steps, and `catmullrom` smoothing |
| `bedrock/GeoClip` / `GeoAnimations` | A clip's per-bone tracks and its length, derived from the keyframes when the file does not declare one |
| `bedrock/GeoPose` | One frame as flat arrays, so a second clip can be layered over the first by writing over the bones it touches |
| `melee/MeleeState` | The state machine, read off the clip names rather than the pack's Lua |
| `melee/MeleePack` | Discovery, zip and folder reading, lang names, and on-demand texture registration |
| `melee/MeleeRenderer` | The first-person pass, the glow, the rim light, the phantom copies and the chromatic split |
| `melee/MeleeEffects` | The edge trail, the sparks, the swing ring and the orbiting symbols, all in the weapon's own space |

**The Lua state machines are not run, and do not need to be.** Each pack ships its logic as Lua for
a mod loader that is five major versions away. The clip names say what that logic does:
`static_idle` holds, `draw` plays once on equip, `melee_1..3` cycle one per swing, `inspect` plays
once, `run_start`/`run`/`run_end` wrap the sprint loop. That is the whole machine, and it is
reproduced as two tracks — a stance and whatever is happening over it — each cross-fading so a swing
starts from where the hands are rather than snapping to its first keyframe.

**Placement is fitted, not guessed.** The models carry their own view anchors (`camera`, `view`,
`idle_view`, `positioning`), and those anchors disagree between weapons in the same pack: measured
against `camera`, four of the eleven put the hands sixty units above the eye instead of ten below.
Taking any one of them as the eye leaves half the pack out of frame. Instead the idle pose is
measured when the weapon loads, normalised to a block and placed at a set distance in front of the
eye, which lands every weapon of every pack in the same place. Size, distance, three offsets and
three rotations then move it from there.

**Why display lists.** One weapon here reaches 953 cubes — nearly six thousand quads — and the pack
totals 5,586. Pushed through the tessellator every frame that is real cost on gl4es, where every
immediate-mode quad is CPU work; and the glow, the rim light and three phantom copies would each pay
it again. Compiled once per bone, a frame is forty-odd matrices and forty-odd `glCallList` runs, and
each extra pass is one more run per bone rather than one more rebuild.

**Controls.** `M` and `N` step through the weapons you have left switched on, `H` inspects, `J`
turns the weapon on and off. The settings screen's Melee page has a `Weapon List` button: click a
row to carry it, click the switch on the right to drop it out of the cycle.

**The effects** are separate toggles rather than one style: glow (masked by the pack's own specular
map, so the parts the artist marked shiny are the parts that light up), rim light, chromatic split,
phantom copies of the poses the swing actually held, an edge trail, sparks, a ring that leaves the
blade, and orbiting symbols. None of them touch the animation.

**The pack's own sounds play.** Each clip carries a timeline of cues naming a file —
`delta_wt:melee/mercy/draw` — which only became playable once the sounds were bundled: 1.8.9 builds
its sound registry when resources load, so a sound that is not in `sounds.json` by then cannot be
played later. The bundler writes an event per file and the state machine fires the cues the playhead
crosses, on the main track only, since a looping stance with a cue in it would retrigger several
times a second. A hundred cues across the pack, every one resolving to an event with a file behind
it.

## Ambience: what a theme puts in the air

A theme changed colours, then shapes; what it could not change was the air. Forty ambience styles now
fill the space around the player — Matrix rain as columns of glyphs brightest at the head, snow,
blizzard, embers, ash, fireflies, petals, leaves, rain, bubbles, spores, dust, sparks, starfield,
meteors, aurora, souls, hearts, confetti, glitch, void motes, plankton, sandstorm, feathers, blood
rain, frost shards, gold dust, neon rain, circuit motes, pentagrams, skulls, lanterns, jellyfish,
rune fall, digital snow, solar flares and thunderbugs among them.

It is a volume that travels with the camera, not a weather system: particles that leave the box wrap
to the far side of it, which reads as endless and costs a fixed number of sprites rather than
scaling with the render distance.

**Twenty-one new themes**, each naming its own ambience: Matrix, Matrix Red, Inferno, Arctic, Neon
Tokyo, Bloodmoon, Spirit, Phantom, Glacier, Desert, Jungle, Nebula, Supernova, Aurora, Cherry Night,
Arcade, Obsidian, Seafoam, Festival, Witchcraft and Thunder — forty-three in all.

**The sky actually changes now.** `Theme Sky & Fog` used to tint at a fixed 59% over whatever the
texture pack drew, which a Matrix theme cannot survive: green text over a blue sky is not a black
sky. The sky quad takes a colour and an opacity, a theme writes both when it is picked, and the ones
built around a black sky ask for 96%.

## Themes are presets now, not a lock

Picking a theme used to change nothing in the settings: the theme answered for the colours, the five
styles and the particle sprite at read time, so with Halloween active, switching the particle or a
colour in the GUI did exactly nothing — the theme went on winning, with no way to tell from the
screen that it would.

A theme is a preset applied to the settings instead. `Theme.apply(config)` copies its look into the
real fields when one is picked — colours always, the ring/bubble/trail/effect/halo styles with
`themeVisuals`, the particle sprite with `themeParticles`, the sky and fog with `themeWorld` — and
nothing overrides anything afterwards: `Palette` reads the four colour pickers, each module reads its
own style field, and `TexturesManager.themed()` reads a sprite setting.

`Themes.onEdited(field, config)` hangs off the single point every widget writes through
(`ConfigSchema.set`). It does three things: picking a theme (or toggling what a theme applies)
copies it in; editing one of the settings the active theme filled in, to a different value, drops
`themeStyle` to **Custom**; anything else — a radius, a speed, a count — leaves the theme's name
alone. The look is kept on the way to Custom, because the values are already in the settings; only
the label gives way, which is the honest thing for it to say once the settings are yours.

Re-picking the same value does not drop the theme, and a style the theme never set (because
`themeVisuals` was off) does not either.

**The theme's particle sprite became a real setting.** It was reachable only through a theme before;
it is `Particle Sprite` now — Firefly, Firefly Soft, Dollar, Snowflake, Heart, Star, or Symbol, which
follows the existing 43-symbol `Symbol` setting. `None` leaves every effect its own sprite. A theme
writes it when picked, and it can be changed or cleared without one.

A saved config that names a theme has it applied once at startup (`Themes.applySaved()`), so a
config written before this change keeps the look it had rather than reverting to the defaults with a
theme's name on it.

The three switches are relabelled to say what they do: **Theme Sets Particles**, **Theme Sets
Visuals**, **Theme Sets Sky & Fog**.

## Fifth round: shards that stick, and the last of the hairlines

**Hit particles stick to the body.** The impact burst used to spawn at the hit point and fly off
under its own motion, so on a moving target it was left behind within a tick or two. Particles can
now be anchored to an entity: `FxParticles.Particle.attach(entity, localX, localY, localZ)` stores
a body-local offset plus the entity's `renderYawOffset` at spawn time, `tick()` stops integrating
motion for an anchored particle, and `renderX/Y/Z` rotate the offset by how far the body has turned
since and add the entity's interpolated position. The result rides the target through knockback and
turns with it instead of hanging in the air where the hit happened.

`HitEffects.stick(...)` places them on the target's body cylinder — radius `max(0.2, width * 0.52)`,
height between 0.2 and 0.95 of the entity's height — and gives them four to five times the burst's
lifetime, so they read as embedded rather than as a flash. Shards, glass, arrows and runes each get
their own glyph driven into the body; sparks, rings, cross, burst, stars and slash stay as free
bursts, because a spark or a ring makes no sense stuck to a hitbox. The behaviour is a toggle,
`Stick To Body`, on by default, next to the existing `Follow Target`.

**The last three GL-line paths are gone.** The fourth round converted the box outlines and the
trajectory path; three queues were still drawing real lines. `OUTLINE_SIDE_QUEUE` (the trajectory's
landing face) now goes through a new `Render3D_Shapes.glowBoxSide(box, colour, lineWidth, side)`
that picks the four corners for the given `EnumFacing` and emits each edge as an `Fx.glowLine`
band; `DEBUG_LINE_QUEUE` draws as bands too; and `glowBox`'s edge width went from a flat 0.02 to
`0.035 * max(1, lineWidth)`, so a configured line width now actually changes the thickness. No
module reaches `DrawMode.LINES` any more — only `compat/VertexFormat` still names it, for the
1.21.4 API surface.

## Fourth round: combat effects, and lines that are no longer lines

Taken from the reference showcases, in-game visuals only — no GUI changes, no weapon rendering.

**Everything built from GL lines now draws as geometry.** The third round fixed this for the halo
and the player effects; the box outlines (friends highlight, trajectory landing) and the trajectory
path itself were still GL lines, which gl4es draws one pixel wide. `Render3D_Shapes` builds those
from `Fx.glowLine` bands now, so a trajectory arc and an outlined box glow instead of scratching.

**The ghost forms through you.** Copies within about two blocks of the camera used to be hidden
outright, to stop a model crossing the near plane filling the screen — which also meant the copy
never appeared to separate from its owner. It fades in with distance instead: invisible at half a
block, full by two and a bit, so the ghost materialises as the player walks out of it.

**Three new modules**, sharing one small particle pool (`FxParticles`, capped at 2000 so a bad
setting cannot eat the frame):

| Module | What it does | Styles |
|---|---|---|
| Hit Effects | A burst at the point of impact; Follow Target gives the particles the target's velocity so they travel through the knockback | 10: shards, glass, arrows, sparks, runes, rings, cross, burst, stars, slash |
| Kill Effects | Fires where something dies. 1.8.9 sends no death packet a client can hook, so living entities in range are remembered while alive and the effect fires the first tick one is found dead | 8: flame pillar, soul burst, explosion, portal, vortex, lightning, rings, ash |
| Landing Impact | Fires where a player lands, scaled by the height of the fall, with a minimum fall below which nothing draws | 7: ring, dust, shockwave, crack, splash, burst, runes |

**Symbol trails.** A trail can now be a line of symbols rather than a ribbon — the chain-trail idea
from the showcase — which with 43 symbols is 43 trails rather than one.

**157 selectable styles across 285 options.** (353 options and 40 ambience styles as of the melee round.)

## Third round: thick geometry, real symbols, themes that change the look

**Why the rings looked weak: gl4es ignores `glLineWidth`.** Every ring, halo, wing and beam was
built from GL lines, which means a one-pixel hairline on PojavLauncher no matter what width was
asked for (`Caps.thickLinesSupported()` already knew this; nothing acted on it). `Fx.glowRing`,
`Fx.glowArc` and `Fx.glowLine` build real geometry instead — soft-edged bands made of quads, any
width, on any driver — and the halo and the player effects are drawn with them now.

**The ghost ran instead of teleporting.** Two reasons, both fixed: snapshots were taken on a tick
interval, so at walking speed the copies were centimetres apart; and each copy was posed with the
limb swing it was recorded with, so a row of them animated like one runner. Poses are now recorded
by **distance travelled** (Ghost Spacing, in tenths of a block) and drawn in a still stance, so they
read as a line of clones left behind.

**The symbols were thin line drawings.** The twelve originals were redrawn with a much heavier
stroke, and 31 more were authored: skull, crown, eye, flame, shuriken, pentagram, triquetra, ankh,
two runes, atom, crossed swords, biohazard, circuit, wave, blossom, leaf, bolt, moon, sun, fang,
spade, clover, diamond, gear, target, heavy arrow, bold heart, bold infinity, snow crystal and bold
hexagon — **43 in all**, each selectable individually through a searchable picker rather than only
appearing at random.

**Themes now change the look, not just the colours.** Each theme additionally names the ring, the
trail style, the player effect, the halo and the particle symbol that suit it, behind a
**Theme Visuals** toggle. **Theme Sky & Fog** extends it to the world, which is what a theme needs
when a texture pack owns the sky. Seven more themes: Vaporwave, Emerald, Candy, Abyss, Solar,
Monochrome and Storm — 21 in all.

**Also:** five more halo styles (eclipse, galaxy, aura, shards, gyroscope — 11 in all) and trail
colour modes (HUD gradient, flowing palette, fixed).

The mod now offers **137 selectable styles** across 257 options.

## Themes, and the second round of visual work

**Themes.** (Superseded — see "Themes are presets now, not a lock" above; a theme writes the
settings rather than answering for them.) Every colour in the mod comes through `Palette`, so a
theme simply answers in place of the four colour pickers — one setting restyles the HUD, trails, particles, circles and effects
together, without overwriting anyone's custom colours. Fourteen presets plus `CUSTOM`: Halloween,
Christmas, Cyberpunk, Ocean, Lava, Toxic, Sakura, Midnight, Rainbow, Frost, Sunset, Blood, Gold,
Void. With **Theme Particles** on, the theme also picks the particle sprite (snowflakes for Frost
and Christmas, petals for Sakura, and so on).

**The ghost was rebuilt.** It was white, carried a broken held item, brought a name tag with it, and
whited out the whole screen when it caught up with its owner. All four came from re-running the
entity renderer: that resets the colour before drawing, draws held items and armour as separate
layers, draws a name tag and a shadow, and happily renders a model inside the camera's near plane —
where additive blending fills the screen. It now renders the player's **model** directly, which has
no held item, armour, name or shadow to begin with and keeps the colour it is given. Three rules
make it usable in a fight: ghosts of your own player are drawn **only behind you**, never within
2.2 blocks of the camera, and only when far enough from the previous ghost to read as a separate
copy rather than a smear.

**The china hat was upside down** — the cone was built brim-high, tip-low. Brim and tip swapped.

**The halo had one style**, the busy orbiting souls. Five more: a plain ring, two counter-rotating
rings, a spinning rune disc, a tilted Saturn ring and a crown of spikes.

**What the second round adds**

| Area | Added |
|---|---|
| Themes | 14 presets + themed particles |
| Player effects | 8 more (bubbles, petals, embers, starfall, tornado, magic circle, beam, heartbeat) — 20 in all |
| Halo | 5 more styles — 6 in all |
| Trails | 3 more styles (ribbon, dotted, neon) and a glow toggle — 8 in all |
| Jump circles | 5 new rings (rune, sakura, snowflake, magic, shatter) — 10 in all |
| Hit bubbles | the same 5 rings, plus 5 animations (expand, spin, pulse, shrink, flash) — 10 styles |

That is 73 style and theme choices across the mod, and 253 options in the settings screen.

## Fixes from in-game testing, and the effects added after it

**The world-space origin was wrong by eye height.** `Camera.getPos()` added `getEyeHeight()`, on
the reasoning that the camera sits at eye level. The camera does — but the *origin the world pass
is drawn around* does not: vanilla uses `RenderManager.viewerPos*`, which is the view entity's
interpolated position with no eye height, and every call site in the mod subtracts this value to
place geometry. So everything the mod drew in the world sat about 1.62 blocks too low: jump circles
and trails were under the ground where nothing could see them, and a hit bubble aimed at an
opponent's chest landed at their feet. It now reads vanilla's own `viewerPos*`, so the two cannot
drift apart.

**Trails threw every tick.** They stored their history on the entity through a mixin-implemented
`TrailEntity` interface. With no mixins nothing implements it, so the first player each tick raised
a `ClassCastException` — caught by the event guard, logged once, and silently skipped forever. The
history now lives in a weak-keyed side map. While in there: a **Apply To** option (self, others,
both — the interface existed upstream but was commented out of the config), two more styles
(`GRADIENT`, `PULSE`), and the line-of-sight test is gone, which used to make a trail vanish the
moment its owner stepped behind a wall.

**The china hat was drawn fully transparent.** Its alpha was
`(int) (1 - (chinaHatAlpha * 255 / 100f))` — negative for every setting above zero, and the vertex
colour takes floats, so the value was clamped to nothing. It is a 0..1 float now.

**Jump circles mostly did not fire.** The trigger required the jump key to still be held on the
tick the player left the ground. With a touch control that is a coin toss, so most jumps produced
nothing. It now fires on leaving the ground with upward motion.

### Player effects

A new page with twelve effects, one shared set of controls (effect, who it applies to, scale,
speed, opacity, density), and a first-person toggle:

| Effect | What it does |
|---|---|
| Ghost | Fading copies of the player left at the positions it held a moment ago — the afterimage effect |
| Orbit | Sprites circling at waist height |
| Helix | Two strands spiralling up around the player |
| Rings | Rings rising from the feet, shrinking as they go |
| Shockwave | A ring expanding across the ground on every landing |
| Flame | Flames rising from the feet |
| Snow | Snow drifting down around the player |
| Sparks | Sparks thrown outward while moving, pulled down by gravity |
| Wings | A pair of glowing wings that beat behind the shoulders |
| Crown | Shards orbiting above the head |
| Pulse Sphere | A wireframe sphere pulsing with the palette |
| Footprints | Glowing marks left where the player steps |

The geometric ones are computed from the world time each frame and keep no state. The emitted ones
spawn particles on the tick and age them out. Ghost is a real re-render of the player model, which
needs two guards: the mod's own feature layers skip while a ghost is drawing, and name tags and
drop shadows are suppressed for the duration — otherwise every ghost would stack another copy of
both on top of the player.

## The settings screen, and which options actually do anything

The screen was rebuilt (`gui/SoupSettingsScreen`, `gui/option/*`). The old one could only edit a
second, parallel copy of five modules' settings — about thirty options — and drew them as flat
grey boxes with no scrolling, so the rest sat off the bottom of the screen.

- **`ConfigSchema` reflects over the config.** This is the piece Cloth Config used to generate and
  that the port never had: categories in declaration order, `@Gui.PrefixText` as group captions,
  `@BoundedDiscrete` as sliders, `@ColorPicker` as a swatch plus an editable hex field, enums as
  cycle buttons (or a filtered picker past eight constants — the cape list has 116), `String[]`
  as its own list editor, and `*_BUTTON` fields as the action buttons they were always meant to
  be. Labels come from the `text.autoconfig.soupapi.*` keys the lang files already carried.
- **The duplicate config is gone.** `AmbientParticle`, `AspectRatio`, `CustomFog`, `Capes` and
  `BetterHudStyles` each read their own `Setting` objects and never looked at the config, so the
  config entries for ambient particles, fog, the aspect ratio, the hotbar and capes did nothing
  at all — including when edited in the config file by hand. They read the config now, the
  `Setting` subsystem is deleted, and `config/soupapi.json` is the only source of truth.
  `ConfigManager` keeps only which page was last open, in `config/soupapi_gui.json`.
- **Count:** 22 pages, 236 options, none unreachable (checked against the config class by
  reflection), of which **204 work** and 32 are marked.

### Features that were ported but never called

Each of these existed as working code with no caller, so the options for them did nothing:

| Feature | How it is wired now |
|---|---|
| Potions HUD | Drawn from the overlay event, gated on `hudBetterPotionsHudEnabled` |
| Swing Hand | `RenderHandEvent` is cancellable and `renderItemInFirstPerson` is public, so vanilla's call is replaced by the same call inside the configured transform |
| Capes | New `CapeFeatureRenderer`: vanilla's cape model and swing maths with the mod's texture, local player only, plus a threaded download for the custom-cape link |
| China hat | Registered through `RenderPlayer.addLayer`, which Forge makes public |
| Better hotbar / health / food / armour / air / XP | `BetterHudStyles` already cancelled vanilla's elements but nothing drew replacements, so turning it on deleted the HUD; `render/BetterHudRenderer` draws them in the mod's own style, using the `gui/` icons that were cut for exactly this |
| Custom fog shape | `EntityViewRenderEvent.RenderFogEvent`; the density event alone only covers the exponential curve, so sphere vs cylinder did nothing |
| Aspect ratio | Written off as impossible earlier in this port. It is not: `EntityViewRenderEvent.CameraSetup` fires after the perspective is built, so scaling the projection along X there is exactly what a different aspect ratio does |
| No fire overlay | `RenderBlockOverlayEvent` (`OverlayType.FIRE`) is cancellable; the replacement band is drawn in the HUD pass |
| Time and weather changer | Re-applied client-side each tick |
| Coloured sky | The sky is the only thing that does not write depth, so a far-plane quad with `GL_GEQUAL` covers the sky and nothing else |
| Friends highlight | Glowing is 1.9+, so friends get a drawn outline box instead |
| Jump-circle colour spin | Rotates the palette phase around the ring |
| Chat translator | Nothing fed `queueTranslation`; `ClientChatReceivedEvent` does, and the option no longer sits behind a hard-coded `return` |
| Hit-sound overwrite | 1.8.9 has no attacker-side hit sound, so this mutes the victim's hurt sound inside a short window after the player's own hit |

### Options the screen marks as unavailable

These need something 1.8.9 does not have. They stay visible, greyed, with the reason in the
footer, rather than looking functional:

| Options | Why |
|---|---|
| The whole Totem page (18) | The Totem of Undying is 1.11+; nothing can pop one |
| Hit-sound volumes per hit class (6) | Sweep, knockback, strong and weak hits need the 1.9 attack cooldown |
| Better scoreboard (4) | 1.8.9 draws the scoreboard inside the HUD with no hook, and this port uses no bytecode patching |
| Hit colour (4) | The damage flash is set inside `RendererLivingEntity`, which has no hook |

## Global GL state, the shared vertex buffer, and white models

Two faults showed up together on PojavLauncher: held items and block models rendered
white, and the client crashed in another mod's overlay handler with
`IllegalStateException: Already building!`. Both come from the same place — 1.8.9's
rendering state is global and sticky, where 1.21.4's is per-draw.

- **`RenderSystem.setShader` disabled texturing and nothing turned it back on.** Selecting
  a core shader in 1.21.4 changes no global state; the 1.8.9 equivalent for an untextured
  key (`POSITION_COLOR`, `RENDERTYPE_LINES`) is `glDisable(GL_TEXTURE_2D)`. Every rounded
  rect, trail and HUD background left texturing off, so the next thing vanilla drew
  sampled no texture and came out white. Every draw funnels through `BufferRenderer`,
  which now re-enables it after the draw.
- **A half-built buffer is fatal for whoever begins next.** 1.8.9 has one shared
  tessellator. A module that threw part-way through a draw — or any code path that began
  without drawing — left it "building", and the next `begin()` threw. The next `begin()`
  is usually another mod's, which is why the crash report named MbMotionblur rather than
  this mod. `Tessellator.begin` now discards an orphaned buffer and retries,
  `BufferRenderer` tolerates a draw with no begin, the event bridge flushes any open layer
  at the end of each pass, and `guard(...)` discards the buffer and restores state before
  it returns.
- **`RenderSystem.restoreDefaults()`** runs at the end of the world and HUD handlers:
  texture unit 0, texturing on, colour white, default blend func off, cull on, depth on,
  depth mask on, alpha test on, scissor off, line width 1. Lighting and the projection are
  left alone — those belong to whichever pass is running.

## Opening the settings screen

Default **Right Shift**, registered as a proper Forge `KeyBinding` so it appears in
Options → Controls under "SoupAPI" and can be rebound — the 1.21.4 version only had a
hard-coded Right Ctrl + Right Alt combination, which is close to unusable on PojavLauncher
where the modifiers live behind an on-screen keyboard. That combination still works.

The screen's labels needed the lang files converting: 1.8.9's `Locale` reads
`assets/<namespace>/lang/<xx_YY>.lang` as `key=value` and has no JSON loader, so every
`text.autoconfig.soupapi.*` key was invisible and the screen showed raw keys.
`tools/lang_to_1_8_9.py` generates `en_US.lang` and `ru_RU.lang` from the JSONs, which stay
the source of truth. For the same reason `PotionsHud` no longer parses `lang/en_us.json`
out of the resource manager (a file 1.8.9 does not have, so effect names fell back to keys
like `potion.moveSpeed`); it uses vanilla's own `I18n`, and therefore follows the player's
language.

## Discord rich presence and native loading

The first Pojav run crashed on the client tick with
`UnsatisfiedLinkError: dlopen failed: library "libdiscord-rpc.so" not found`. Two things
were wrong, both inherited from the original source:

1. The JNA binding was written the usual way — `DiscordRPC INSTANCE = Native.loadLibrary(...)`
   as a field on the interface — which puts the library load in the interface's static
   initialiser. The first touch of the type throws, and because a failed initialiser poisons
   the class, every touch afterwards throws `NoClassDefFoundError`. The load now lives in
   `DiscordNative`, which attempts it once, catches `Throwable`, caches the outcome and
   returns `null` when the native is unavailable. The attempt is skipped outright on Android,
   where no build of the native exists and there is no Discord IPC socket to talk to anyway.
   The jar ships `linux-x86-64` and `win32-x86-64` natives, so rich presence works on desktop
   Linux and Windows; macOS and any aarch64 host get the graceful disable.
2. A Forge event handler that throws crashes the game, so a single module could end the
   session. `ForgeEventBridge` now runs each module hook through `guard(...)`: the first
   failure per call site is logged in full, later ones are counted silently, and the rest of
   the frame carries on. `VirtualMachineError` and `ThreadDeath` are rethrown — those are not
   a module's to swallow. The module is not disabled on a throw, since a transient failure
   should not cost the feature for the whole session.

## Texture assets added by this port

**39 textures the code referenced were never committed to this repository** — they are
absent from the entire git history, so those features drew the missing-texture
placeholder on 1.21.4 as well. They have now been authored and added under
`src/main/resources/assets/soupapi/textures/`:

- `particle/` — `firefly`, `firefly_glow`, `firefly_alt`, `firefly_alt_glow`, and
  `dollar`, `snowflake`, `heart`, `star` each with its `_unblack` variant (12 files,
  256x256). Used by hit particles, jump particles, ambient particles, the target-HUD
  particles and the target-render soul trail.
- `particle/glyph/` — `abs`, `arrow`, `arrow_line`, `circle`, `cross`, `flower`,
  `forever`, `line`, `quad`, `star`, `triangle`, `zigzag` (12 files, 256x256).
- `jump_circles/` — `portal_2` (a HitBubbles style), `soup` and `soup_unblack`
  (a JumpCircles style), 1024x1024, matching the sizes of the committed siblings.
- `capes/optifine/` — `standard`, `black`, `blue`, `cyan`, `gray`, `green`, `purple`,
  `red`, `white`, `yellow` (64x32).
- `gui/hitbox.png` (512x512, matching the other `gui/` icons) and `i.png` (128x128),
  the charged-player energy-swirl overlay.

### Conventions these follow

The existing textures use two paired forms, which were determined by pixel analysis of
the committed `hexagon`/`circle`/`portal` pairs (correlation 1.0000 between a base's
luminance and its `_unblack` alpha):

- **base** — fully opaque, greyscale art on black. Correct for the additive blend
  (`SRC_ALPHA`, `ONE`) that every jump-circle, hit-bubble and glyph draw uses, where
  black contributes nothing.
- **`_unblack`** — the same art with RGB forced to white and alpha set to the base's
  luminance. Derived mechanically from each base.

Under additive blending the two are mathematically identical (`rgb*L * a` versus
`rgb * a*L`), so the choice only matters where a non-additive blend func can be reached.
Two sets therefore deliberately depart from the "base" form:

- The four `firefly*` sprites are white-with-alpha, because they are also the
  target-render *soul* texture, and `TargetRenderSoulStyle.setupBlendFunc()` falls back
  to `defaultBlendFunc()` for any style other than `SMOKE`/`PLASMA` — a black-backed
  sprite would paint black squares there.
- `i.png` is emissive artwork on black and tiles seamlessly in both axes, because
  `CustomEnergySwirlOverlayFeatureRenderer` blends `GL_ONE`/`GL_ONE` (so the source
  alpha is ignored) while scrolling the GL texture matrix, which wraps.

All of them are power-of-two, which gl4es on Pojav needs for mipmapped sampling.

**Substitution to be aware of:** the ten `capes/optifine/` files are flat colour capes
named after OptiFine's donor-cape colours, not reproductions of OptiFine's own cape
artwork (which is not ours to ship). They fill the classic cape UV block and the 1.9
elytra block, with a slightly darker hem, so they render as clean solid-colour capes.

**Still missing, and not a texture:** `assets/soupapi/sounds/pok.ogg`. `sounds.json`
declares the `pok` event and `CustomSounds.POK` references it, but the file was never
committed either; no audio has been fabricated for it, so that one sound stays silent.

## What has and has not been verified

**Verified here:** the mod compiles cleanly against Forge 1.8.9/MCP, `./gradlew build`
succeeds, and the output jar is correctly reobfuscated to SRG names, carries Java 8
bytecode (major version 52), contains `mcmod.info`, all 182 assets, and the shaded JNA.
The settings schema reaches all 353 config fields with none unreachable, all 43 themes apply
cleanly, and every ambience style names a sprite that exists.

For the weapon packs, a probe loads the built-in pack through the mod's own code and checks each
weapon end to end: all eleven parse (5,586 cubes, 118 clips), every clip poses at twenty points along
its length without a NaN or a runaway coordinate, every weapon has an idle, a draw and attack clips,
the fitted placement is finite and sane, and the hand meshes and a blade bone are found. All 100
sound cues resolve to an event in `sounds.json`, and all 81 events have their ogg file. With the
pack also sitting on disk it still loads once, not twice.

The geometry and animation maths was also checked by eye before it was written: the same transforms
were implemented offline in Python, the posed models rendered to PNGs with a software rasteriser,
and the convention corrected against what the images showed — which is how the mirrored X axis and
the disagreeing view anchors were found.

**Not verified here:** nothing has been run in-game. This environment has no 1.8.9
client and no Android device, so runtime behaviour and visual fidelity — especially the
custom HUD layout, particle positioning, the weapon's placement in the hand and the Pojav paths —
still need testing on both desktop and PojavLauncher. The weapon placement in particular is fitted
rather than matched to the mod the packs were built for, so the defaults are a starting point and
the size, distance, offset and rotation sliders exist because they will be wanted.
