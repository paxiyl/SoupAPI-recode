# SoupAPI 1.8.9 — Visual Overhaul: working report

## 1. Audit — root causes found before changing anything

Each item below was traced in the source, not inferred from symptoms.

### RGB never animated
`Palette.getColor(position)` maps a caller-supplied position onto the four colour pickers and has
**no time input at all**. There is no hue rotation anywhere in the colour pipeline; the "Rainbow"
theme is four fixed colours written into `c1..c4`. Each module that "animates" passes its own
geometry-derived position, so nothing moves over time and there is no shared clock to synchronise
on. Callers also pass positions outside 0..1 (`getColor(1f)`, phases of `p + 1`), which `QUARTET`
extrapolated past the last colour instead of wrapping.

### Animated skies
1. **Wrong place in the frame.** The sky was a screen-space quad drawn from
   `RenderWorldLastEvent`, *after* ambience, halo, trails, jump circles and target render in the same
   pass. Its base layer is opaque at the far plane, so every one of those effects that does not write
   depth was painted over wherever it sat against the sky.
2. **GL state leak.** `AnimatedSky.render()` ends with an unconditional `enableLighting()`. The bridge
   draws `PlayerEffects`, `FxParticles` (hit/kill/landing effects) and `FriendsHighlight` after the
   sky, so whenever a moving sky was active those were drawn with fixed-function lighting on and came
   out dark.
3. **Not differentiated.** 21 styles map onto 10 textures; GRID, HEX_GRID, SCANLINES, SPEED_LINES and
   CIRCUIT_SKY are literally the same scrolling `cyber.png`.
4. **Not attached to the world.** A screen-space quad with yaw/pitch-derived UV offsets: the "sky"
   slides with the view rather than staying put while the camera turns.

### Kill effects
1. Deaths were found by polling `loadedEntityList` and fired **only for entities still present** with
   health 0. The server's destroy packet routinely arrives in the same tick as the death status, and
   minigame servers often remove a killed player without any death status — both were silently
   missed.
2. The class comment says "1.8.9 sends no death packet a client can hook". It does: entity status 3
   runs `EntityLivingBase.onDeath()` on the client thread, and Forge's patch fires
   `LivingDeathEvent` from there (verified in Forge's `EntityLivingBase.java.patch` and
   `EntityPlayer.java.patch`). The entity is still in the world when it fires.
3. No world-change handling: the id set survived a world switch, and entity ids are reused per world.
4. All effects were additive sprites, so nothing dark (smoke) could be drawn at all.

### Particles / smoke
`FxParticles` draws every sprite with `GL_SRC_ALPHA, GL_ONE` (additive). Additive black adds nothing,
so black smoke cannot exist in that pool; there is also no sprite rotation, no depth sorting and a
`MatrixStack` allocation per sprite.

### Melee weapons
1. **Effects were dead code.** `MeleeEffects.track()` and `MeleeEffects.render()` have no callers:
   the trail, sparks, pulse rings and runes never drew. Outline, chroma and phantom have settings,
   preset entries and README text but no draw path at all.
2. **Animation snap at the end of every swing/inspect.** `MeleeState.apply()` samples the outgoing
   clip at full weight for the whole fade-out, so the last frame is held and then the pose snaps to
   idle; bones only the outgoing clip animated also pop at the end of a cross-fade.
3. **Equip only once.** `drawn` is never reset when the player switches to a non-weapon item, so the
   draw clip did not replay when switching back.
4. `MeleeEffects.advance()` steps a fixed 0.05 s per frame, so effects ran at frame-rate speed.

### Presets
`Preset.owns()` claims every `melee*` field, so applying a preset reset the player's weapon placement
(size, distance, offsets, yaw/pitch/roll, hand placement). Numeric tuning fields such as
`killEffectsCount` were not owned, so the same preset over two configs could still differ.

### Hotbar
Highlight easing is a fixed 35 % per frame (frame-rate dependent); a single style with no selection
animation beyond that.

### Toolchain constraint in this environment
`maven.minecraftforge.net`, `libraries.minecraft.net` and the Mojang hosts are blocked by this
environment's network policy, so ForgeGradle cannot run here. Compilation is instead checked with
`javac --release 8` against the real Gson/Guava/LWJGL/Log4j/JNA jars from Maven Central plus
signature-only stubs generated from the MCP 1.8.9 and Forge 1.8.9 sources (see §9).

## 2. Bugs fixed

| Area | Fix |
|---|---|
| RGB | One shared clock (`FxClock`) drives every animated colour. Five modes (Static, Flow, Rainbow, Pulse, Shift) with speed, direction, spread, saturation and brightness. Per-element switches (GUI, HUD, crosshair, weapon, trails, particles, sky, ambience) work by scoping each module to its element in the event bridge. Positions now wrap instead of extrapolating. Everything is saved in the config. |
| Sky | Drawn from `WorldProvider.setSkyRenderer` as a real dome at the sky stage, so it stays fixed while the camera turns and no longer paints over world effects. GL state is restored (lighting leak fixed). Every style has its own geometry or colour function; no two share a texture. |
| Kill effects | Triggered from `LivingDeathEvent`. Fallbacks catch targets that are removed without a death status, and players that respawn by teleport. State is cleared on world change; ids are not reused across worlds. A logic probe covers all of these cases. |
| Smoke | A separate alpha-blended, depth-sorted pool, so black smoke can exist at all, with rotation, layering and an edge-light pass. |
| Melee | Effects are wired in (trail, sparks, pulse, runes, outline, chroma, phantom). The end-of-clip snap is fixed by fading the outgoing clip's weight. The draw clip replays on every re-equip. Effects use real frame time. |
| Presets | Presets no longer touch the weapon or hand placement, sound and summon volume, the carried weapon or GUI settings. Everything a preset does own is reset from defaults first. A probe proves all 25 presets are deterministic (see §9). |
| Hotbar | The highlight is a spring integrated in fixed sub-steps, so it moves the same at any frame rate. |
| Text rendering | `FontRenderer` turned blending off after drawing the first glyph page. Any string spanning two pages (Cyrillic mixed with Latin, or a dash or guillemets) drew its second part on opaque black boxes. It now restores state once, after all pages. |
| Russian | Every setting, category and Atlas screen string has a Russian translation (576 keys in each language, 0 missing). Counters use a `{n}` placeholder, because Minecraft's `I18n.format` would turn a bare `%s` into a format error. A glyph the bundled font lacks (`❤`) was removed. Atlas was rendered with `ru_RU.lang` and checked. |
| GUI | Translucent gradients no longer use `renderRoundedGradientRect`, which clears the framebuffer's alpha and could not blend translucently. A broken saved layout falls back to the default instead of leaving nothing selected. |

## 3. Main files

New: `utils/FxClock`, `render/SmokeFx`, `render/SkyRenderer`, `melee/WeaponFx`, `melee/MeleePortal`,
`melee/MeleeFinish`, `gui/atlas/{AtlasScreen,Glass,Spring}`, `modules/Crosshair`,
`tools/arcana/*` (weapon generator), `tools/textures/vfx.py`, and the built-in pack
`assets/soupapi/melee_packs/arcana/**` (models, clips, display profiles, textures, synthesised sounds).

Rewritten or extended: `utils/Palette`, `utils/SkyStyle`, `utils/Ambience`, `modules/{AnimatedSky,
KillEffects,WorldAmbience,BetterHudStyles,PlayerEffects,ColoredSky}`, `render/BetterHudRenderer`,
`melee/{MeleeState,MeleeRenderer,MeleeEffects,MeleeWeapon,MeleePack,MeleeSounds,Melee}`,
`bedrock/{GeoModel,GeoPose}`, `config/{SoupAPI_Config,Preset}`, `gui/option/{Option,ConfigSchema}`,
`event/ForgeEventBridge`, `SoupAPI`, `screen/SoupSettingsShortcut`, the lang files and `sounds.json`.

## 4. Weapons (built-in pack `arcana`)

| Weapon | Geometry | Behaviour |
|---|---|---|
| Fractured-Eye Katana | 48 bones, 672 cubes | Eyes along the blade glance about in idle. They go frenzied (darting independently, dilating, flaring blood red) on swing, summon, flourish and inspect. Finishes: Default, Obsidian, Molten, Spectral, Dark Metal. |
| Raven Claw | 43 bones, 156 cubes | An animated spine that opens on draw and snaps shut, a travelling breathing flex, and feathers that never settle. |
| Spectral Blade | 27 bones, 147 cubes | Flickering ribbons and an emissive core. |

Each weapon has these clips: static idle, draw, four attacks, flourish, inspect, run and put-away (the katana also has summon). Emissive `_illuminated` bones are lit in an additive pass. Each weapon has its own synthesised sounds, with a volume setting. The choreography of the reference katana pack was studied, but none of its assets were copied (that pack is All Rights Reserved).

## 5. Portal summoning

The katana is summoned in five stages on the summon clip's playhead: rift, runes, convergence, emergence and release. Settings:
- enable or disable it
- summon on equip
- duration, intensity, size, glow, particles and smoke
- summon volume

The **K** key replays it. Attacking or switching items skips it safely: the portal state is dropped and the weapon falls through to its normal idle.

## 6. Smoke

The layered smoke system has six variants: `abyssal`, `siphon`, `phantomDrift`, `rift`, `infernalAsh` and `spectral`. It also has these settings:
- quality (Low/Medium/High/Ultra particle caps)
- density, speed, turbulence, size, opacity and edge glow
- colour, and an accent that can follow the theme

Kill effects, the portal and the smoky ambience styles all use it.

## 7. GUI, HUD, crosshair, sky, ambience, kill effects

- **Atlas settings GUI.** A port of Myau Atlas's design (GPL-3.0, credited):
  - Layout: sidebar of groups, page list with master switches, and a detail pane with in-place widgets (switches, sliders, dropdowns with type-to-filter, RGB colour editors, hex/number/text editing, key capture).
  - Search: typing anywhere searches every setting.
  - Motion: spring-driven pills and switches, and a pop-open window.
  - Window: drag to move and resize, and the layout is remembered.
  - Right-click a setting to reset it. A starfield sits behind the window.
  - Rendering: fixed-function glass layers instead of shaders, so it is gl4es-safe.
  - `guiStyle = CLASSIC` brings back the original screen, and there is a button in the header for it.
- **Hotbar.** Styles: Glow, Simple, Glass, Floating, Neon and Minimal. Spring highlight with stretch, a pop on slot change, an RGB border, held-item durability and panel opacity.
- **Crosshair.** Styles: Minimal, Four-segment, Dot, Circular, Rune and Weapon. Settings: size, thickness, gap, rotation, spin, dynamic spread, opacity, outline, glow, theme/RGB or fixed colour, hit confirmation (flash and kick) and third-person visibility. Vanilla's crosshair is suppressed only while it is enabled.
- **Skies.** 32 styles including Plain. The originals were rebuilt on the dome renderer, plus new styles: Storm Front, Circuit Sky, Dimensional Rift, Infernal, Cyberpunk, Arcane, Shattered Glass, Void Energy, *Bleeding Clouds* and *Suspended* (from your reference images), and **Dreamcore** and **Dream** from this round:
  - Dreamcore: pastel cloudscape, white doors with light spilling from the gap, a stairway into the clouds, and floating eyes that blink.
  - Dream: violet and pink clouds with a vast soft face that breathes and slowly blinks.
- **Ambience.** 11 new styles: Abyssal / Spectral / Infernal smoke, Floating Runes, Magic Dust, Spectral Currents, Violet Fireflies, Constellations (joined by lines), Glass Fragments, Occult Symbols and Void Distortions. Embers, petals and ice already existed. Quality and draw-distance settings were added, with a fade at the edge.
- **Kill effects.** The 7 original styles were re-staged, and 12 new ones were added, from Ash and Soul Siphon to Rift Closure. All of them render through the shared particle and smoke systems.

## 8. Presets

The new presets are: Riftblade, Nightfall, Bloodglass, Astral, Inferno Enhanced, Abyss Enhanced, Vaporwave Enhanced, Occult Enhanced, Crystal Storm, Frost Specter, Minimal PvP, Cinematic Showcase, Low-End Performance, and a bonus Dreamscape.

Each one sets a theme, then names its own sky, ambience, smoke, kill effect, finish, crosshair and hotbar style. They never change:
- keybinds
- the carried weapon
- weapon or hand placement
- volumes
- the GUI style

## 9. Build and verification

**No jar was built in this environment.** ForgeGradle needs `maven.minecraftforge.net` and the Mojang library hosts, and the network policy blocks them. Verification was done this way instead:

- **Compilation.** Every source file compiles with `javac --release 8` against the real third-party jars plus signature stubs generated from the MCP/Forge 1.8.9 sources (zero errors at the end).
- **Headless rendering.** A software-rasteriser harness (SoftGL) runs the mod's real render code, and its output was inspected as images. It covered every sky (including Dreamcore and Dream), every kill effect, the weapons and summon sequence, the Atlas GUI (default, search and crosshair pages), all six crosshairs, and the new ambience styles.
- **Probes:**
  - kill-trigger logic: all cases pass
  - preset determinism and player-field preservation: all 25 presets OK
  - `sounds.json`: all 108 referenced `.ogg` files exist
  - lang: every config field has an English label

**Not done: any in-game testing.** Nothing here was run inside Minecraft. These are untested on a real client:
- GL behaviour on real drivers and gl4es/PojavLauncher
- performance
- server interaction
- sound playback
- the final feel of the animations

To build on a machine with normal internet access:

```
./gradlew setupDecompWorkspace build
```

The jar is written to `build/libs/`.

## 10. Known limitations

- Atlas's refracting "liquid glass" is approximated with translucent layers, because the GLSL and framebuffer copy it needs are not safe on gl4es.
- Ambient smoke uses the shared smoke system, so it also follows the Smoke on/off switch and quality cap.
- The Dream face sits in the southern sky at a fixed place. It is a set piece, not something that follows the player.
- Dropdown values (enum names such as style names) are shown as prettified English in both languages, as in the original screen.
- The weapons are original procedural models. They are detailed, but they are not hand-sculpted art.
