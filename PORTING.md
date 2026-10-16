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

**157 selectable styles across 283 options.**

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

**Themes.** Every colour in the mod comes through `Palette`, so a theme simply answers in place of
the four colour pickers — one setting restyles the HUD, trails, particles, circles and effects
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

**Not verified here:** nothing has been run in-game. This environment has no 1.8.9
client and no Android device, so runtime behaviour and visual fidelity — especially the
custom HUD layout, particle positioning and the Pojav paths — still need testing on
both desktop and PojavLauncher.
