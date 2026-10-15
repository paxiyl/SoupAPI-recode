# SoupAPI — Minecraft 1.8.9 port

This branch ports SoupAPI from Fabric 1.21.4 to **Legacy Fabric 1.8.9**, with
PojavLauncher (Android) as a first-class target.

## Build

```
./gradlew build
```

Produces `build/libs/SoupAPI-<version>.jar`. Requirements:

- Minecraft **1.8.9** with **Legacy Fabric Loader** ≥ 0.15 (<https://legacyfabric.net>).
- Nothing else. Fabric API, ModMenu and Cloth Config are **not** required — see below.
- Output is Java 8 bytecode, so it loads on the Java 8 runtimes PojavLauncher uses for 1.8.9.

Open the settings screen with **right-ctrl + right-alt**.

## How the port is structured

The 1.21.4 sources are kept as close to original as possible. Rather than rewriting
every call site, the port provides a compatibility layer in
`me.Padej_.soupapi.compat` whose classes carry the **same names and method
signatures** as the 1.21.4 classes they replace, so most files needed only an import
change.

| 1.21.4 | 1.8.9 port | Why |
|---|---|---|
| `org.joml.*` | `compat.Mat4`, `Quaternionf`, `Vector3f/4f` | JOML does not exist; the quaternion follows JOML's intrinsic `rotateX/Y/Z` convention, which the billboarded particles depend on |
| `MatrixStack` | `compat.MatrixStack` | 1.8.9 has only the GL matrix stack; vertices are transformed on the CPU, as `vertex(matrix, x, y, z)` already implied, which also cuts fixed-function matrix traffic on gl4es |
| `RenderSystem` | `compat.RenderSystem` | maps to `GlStateManager`; there is no shader colour uniform, so `setShaderColor` is folded into vertex colours by the buffer builder, and `setShader` becomes texture enable/disable |
| `BufferBuilder` / `Tessellator` | `compat.*` | wraps the immediate-mode tessellator, handling implicit vertex termination and attribute ordering |
| `DrawContext` | `compat.DrawContext` | 1.8.9 draws GUIs through static `DrawableHelper` with no context or matrix stack |
| `RenderLayer` / `VertexConsumer(Provider)` | `compat.*` | reduced to what a layer means on a fixed-function pipeline |
| `Camera` | `compat.Camera` + `Cameras` | 1.8.9's `Camera` is a static utility with no `getPos`/`getYaw`/`getPitch` |
| `Screen` | `compat.SoupScreen` | 1.8.9 has different render/input signatures, no scroll callback and no title |
| Cloth Config / AutoConfig | `config.*` + `config.annotation.*` | no 1.8.9 build; the annotations mirror Cloth's nesting exactly, so the 747-line config class ported with an import change and still reads the same `config/soupapi.json` |
| Fabric API events | `event.SoupEvents` + `mixin.event.*` | Legacy Fabric API does not provide the modules used; driving them from the mod's own mixins also keeps this a single-file install, which matters on Pojav |

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
  font renderer falls back to vanilla's bitmap font.
- **Line width.** gl4es clamps `glLineWidth` to 1.0; `Caps.thickLinesSupported()` reports
  this so callers can draw geometry instead.
- **Guarded input probing.** Pojav's LWJGL 2 stub does not implement every query.
- **Discord Rich Presence is disabled on Android** — no native discord-rpc build exists
  for it, and JNA cannot load one.

`Caps.describe()` is logged at startup and reports all of the above for the running device.

## Features that cannot be ported as designed

These depend on game content or engine structure that does not exist in 1.8.9:

| Feature | Status |
|---|---|
| Totem pop particles | No Totem of Undying before 1.11 — nothing can trigger it |
| Crossbow trajectories | Crossbows are 1.14+ |
| Off-hand item rendering / off-hand trajectories | The off-hand is 1.9+ |
| Elytra ("glider") trails | Elytra are 1.9+ |
| Glowing-entity detection in target render | The Glowing effect is 1.9+; armour is used as the only tell for invisible entities |
| Attack-cooldown term in the crit check | The cooldown is 1.9+, so every 1.8.9 swing is full strength |
| Cursor shape changes | LWJGL 2 has no standard-cursor API, and Pojav is touch-driven |
| `CustomPhysicParticle` / the `star` particle type | Removed: 1.8.9 has no particle registry, and nothing ever spawned it upstream |

## Missing texture assets (pre-existing, not caused by the port)

**39 textures referenced by the code were never committed to this repository.** They are
absent from the full git history, so these features render the missing-texture placeholder
on 1.21.4 as well as here. They need to be added to
`src/main/resources/assets/soupapi/textures/`:

- `textures/particle/` — `star`, `heart`, `snowflake`, `dollar`, `firefly`, `firefly_alt`,
  `firefly_glow`, `firefly_alt_glow` and their `_unblack` variants (affects hit particles,
  jump particles, ambient particles and totem particles)
- `textures/particle/glyph/` — `abs`, `arrow`, `arrow_line`, `circle`, `cross`, `flower`,
  `forever`, `line`, `quad`, `star`, `triangle`, `zigzag`
- `textures/capes/optifine/` — the ten OptiFine cape colours
- `textures/jump_circles/` — `portal_2`, `soup`, `soup_unblack`
- `textures/gui/hitbox.png`, `textures/i.png` (the charged-player swirl overlay)
