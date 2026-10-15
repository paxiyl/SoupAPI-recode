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

## Missing texture assets (pre-existing, not caused by the port)

**39 textures referenced by the code were never committed to this repository.** They are
absent from the entire git history, so these features render the missing-texture
placeholder on 1.21.4 as well as here. They need adding to
`src/main/resources/assets/soupapi/textures/`:

- `textures/particle/` — `star`, `heart`, `snowflake`, `dollar`, `firefly`, `firefly_alt`,
  `firefly_glow`, `firefly_alt_glow` and their `_unblack` variants (affects hit particles,
  jump particles, ambient particles and totem particles)
- `textures/particle/glyph/` — `abs`, `arrow`, `arrow_line`, `circle`, `cross`, `flower`,
  `forever`, `line`, `quad`, `star`, `triangle`, `zigzag`
- `textures/capes/optifine/` — the ten OptiFine cape colours
- `textures/jump_circles/` — `portal_2`, `soup`, `soup_unblack`
- `textures/gui/hitbox.png`, `textures/i.png` (the charged-player swirl overlay)

## What has and has not been verified

**Verified here:** the mod compiles cleanly against Forge 1.8.9/MCP, `./gradlew build`
succeeds, and the output jar is correctly reobfuscated to SRG names, carries Java 8
bytecode (major version 52), contains `mcmod.info`, all 182 assets, and the shaded JNA.

**Not verified here:** nothing has been run in-game. This environment has no 1.8.9
client and no Android device, so runtime behaviour and visual fidelity — especially the
custom HUD layout, particle positioning and the Pojav paths — still need testing on
both desktop and PojavLauncher.
