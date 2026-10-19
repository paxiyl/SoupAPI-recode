package me.Padej_.soupapi.config;

import me.Padej_.soupapi.modules.*;
import me.Padej_.soupapi.sounds.CustomSounds;
import me.Padej_.soupapi.utils.MC_Tiers;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.Weather;
import me.Padej_.soupapi.config.annotation.Config;
import me.Padej_.soupapi.config.annotation.ConfigEntry;

@Config(name = "soupapi")
public class SoupAPI_Config implements ConfigData {

    /**
     * MAIN
     **/
    @ConfigEntry.Category("main")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TargetHud.Config configPos_BUTTON = TargetHud.Config.CONFIG_POS;
    @ConfigEntry.Category("main")
    public boolean translatorBruhEnabled = false;

    @ConfigEntry.Gui.PrefixText // Friends
    @ConfigEntry.Category("main")
    public String[] friends = {};
    @ConfigEntry.Category("main")
    public boolean friendsHighlight = true;
    @ConfigEntry.Category("main")
    public boolean friendsHighlightOnlyArmor = true;
    @ConfigEntry.Category("main")
    public boolean friendsHighlightSyncColor = false;
    @ConfigEntry.Category("main")
    @ConfigEntry.ColorPicker
    public int friendCustomColor = 0x00ff00;

    @ConfigEntry.Gui.PrefixText // MC Tiers
    @ConfigEntry.Category("main")
    public boolean mctiersEnabled = false;
    @ConfigEntry.Category("main")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public MC_Tiers.TierGameModes mctiersGameMode = MC_Tiers.TierGameModes.SWORD;

    @ConfigEntry.Gui.PrefixText // Config GuiScreen
    @ConfigEntry.Category("main")
    public boolean blurShadowEnabled = true;

    @ConfigEntry.Gui.PrefixText // Watermark
    @ConfigEntry.Category("main")
    public boolean waterMarkEnabled = true;
    @ConfigEntry.Category("main")
    public int waterMarkX = 10;
    @ConfigEntry.Category("main")
    public int waterMarkY = 20;

    @ConfigEntry.Gui.PrefixText // Preset
    @ConfigEntry.Category("main")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public me.Padej_.soupapi.config.Preset presetStyle = me.Padej_.soupapi.config.Preset.NONE;
    @ConfigEntry.Category("main")
    public boolean applyPreset_BUTTON = false;

    @ConfigEntry.Gui.PrefixText // Theme
    @ConfigEntry.Category("main")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public me.Padej_.soupapi.utils.Theme themeStyle = me.Padej_.soupapi.utils.Theme.CUSTOM;
    @ConfigEntry.Category("main")
    public boolean themeParticles = true;
    @ConfigEntry.Category("main")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public me.Padej_.soupapi.utils.Symbol particleSymbol = me.Padej_.soupapi.utils.Symbol.RANDOM;
    @ConfigEntry.Category("main")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public me.Padej_.soupapi.utils.ParticleSprite particleSpriteOverride =
            me.Padej_.soupapi.utils.ParticleSprite.NONE;
    @ConfigEntry.Category("main")
    public boolean themeVisuals = true;
    @ConfigEntry.Category("main")
    public boolean themeWorld = true;
    @ConfigEntry.Category("main")
    public boolean themeAmbience = true;
    @ConfigEntry.Category("main")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Palette.ColorsStyle paletteStyle = Palette.ColorsStyle.QUARTET;
    @ConfigEntry.Category("main")
    @ConfigEntry.ColorPicker
    public int c1 = 0x66a0ff;
    @ConfigEntry.Category("main")
    @ConfigEntry.ColorPicker
    public int c2 = 0x5cf0ff;
    @ConfigEntry.Category("main")
    @ConfigEntry.ColorPicker
    public int c3 = 0xffb256;
    @ConfigEntry.Category("main")
    @ConfigEntry.ColorPicker
    public int c4 = 0xff4a4a;
    @ConfigEntry.Category("main")
    @ConfigEntry.ColorPicker
    public int textColor = 0xffffff;
    @ConfigEntry.Category("main")
    @ConfigEntry.ColorPicker
    public int backColor = 0x181a29;
    @ConfigEntry.Category("main")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int backAlpha = 80;

    /**
     * TRAILS
     **/
    @ConfigEntry.Category("trails")
    public boolean trailsEnabled = false;
    @ConfigEntry.Category("trails")
    public boolean trailsFirstPerson = false;
    @ConfigEntry.Category("trails")
    public boolean trailsRenderHalf = false;
    @ConfigEntry.Category("trails")
    public boolean trailsGlow = true;
    @ConfigEntry.Category("trails")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Trails.ColorMode trailsColorMode = Trails.ColorMode.GRADIENT_HUD;
    @ConfigEntry.Category("trails")
    public boolean trailsForGliders = false;
    @ConfigEntry.Category("trails")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Trails.Targets trailsTarget = Trails.Targets.BOTH;
    @ConfigEntry.Category("trails")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Trails.Style trailsStyle = Trails.Style.FADED;
    @ConfigEntry.Category("trails")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
    public int trailsLenght = 20;
    @ConfigEntry.Category("trails")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int trailsHeight = 100;
    @ConfigEntry.Category("trails")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int trailsAlphaFactor = 100;

    /**
     * HIT EFFECTS
     **/
    @ConfigEntry.Category("hit_effects")
    public boolean hitEffectsEnabled = false;
    @ConfigEntry.Category("hit_effects")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HitEffects.Style hitEffectsStyle = HitEffects.Style.SHARDS;
    @ConfigEntry.Category("hit_effects")
    public boolean hitEffectsRandomColor = false;
    @ConfigEntry.Category("hit_effects")
    public boolean hitEffectsOnlyCrit = false;
    @ConfigEntry.Category("hit_effects")
    public boolean hitEffectsOnlyPlayers = false;
    @ConfigEntry.Category("hit_effects")
    public boolean hitEffectsFollow = true;
    @ConfigEntry.Category("hit_effects")
    public boolean hitEffectsStick = true;
    @ConfigEntry.Category("hit_effects")
    @ConfigEntry.BoundedDiscrete(min = 3, max = 32)
    public int hitEffectsCount = 10;
    @ConfigEntry.Category("hit_effects")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 300)
    public int hitEffectsScale = 100;
    @ConfigEntry.Category("hit_effects")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 300)
    public int hitEffectsSpeed = 100;
    @ConfigEntry.Category("hit_effects")
    @ConfigEntry.BoundedDiscrete(min = 4, max = 60)
    public int hitEffectsDuration = 14;

    /**
     * KILL EFFECTS
     **/
    @ConfigEntry.Category("kill_effects")
    public boolean killEffectsEnabled = false;
    @ConfigEntry.Category("kill_effects")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public KillEffects.Style killEffectsStyle = KillEffects.Style.SOUL_BURST;
    @ConfigEntry.Category("kill_effects")
    public boolean killEffectsRandomColor = false;
    @ConfigEntry.Category("kill_effects")
    public boolean killEffectsOnlyPlayers = true;
    @ConfigEntry.Category("kill_effects")
    public boolean killEffectsIncludeSelf = true;
    @ConfigEntry.Category("kill_effects")
    @ConfigEntry.BoundedDiscrete(min = 3, max = 32)
    public int killEffectsCount = 12;
    @ConfigEntry.Category("kill_effects")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 300)
    public int killEffectsScale = 100;
    @ConfigEntry.Category("kill_effects")
    @ConfigEntry.BoundedDiscrete(min = 8, max = 80)
    public int killEffectsDuration = 26;

    /**
     * LANDING IMPACT
     **/
    @ConfigEntry.Category("landing_impact")
    public boolean landingImpactEnabled = false;
    @ConfigEntry.Category("landing_impact")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public LandingImpact.Style landingImpactStyle = LandingImpact.Style.SHOCKWAVE;
    @ConfigEntry.Category("landing_impact")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public PlayerEffects.Target landingImpactTarget = PlayerEffects.Target.BOTH;
    @ConfigEntry.Category("landing_impact")
    public boolean landingImpactRandomColor = false;
    @ConfigEntry.Category("landing_impact")
    @ConfigEntry.BoundedDiscrete(min = 3, max = 32)
    public int landingImpactCount = 10;
    @ConfigEntry.Category("landing_impact")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 300)
    public int landingImpactScale = 100;
    @ConfigEntry.Category("landing_impact")
    @ConfigEntry.BoundedDiscrete(min = 6, max = 60)
    public int landingImpactDuration = 16;
    @ConfigEntry.Category("landing_impact")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int landingImpactMinFall = 8;

    /**
     * PLAYER EFFECTS
     **/
    @ConfigEntry.Category("player_effects")
    public boolean playerEffectsEnabled = false;
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public PlayerEffects.Style playerEffectsStyle = PlayerEffects.Style.GHOST;
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public PlayerEffects.Target playerEffectsTarget = PlayerEffects.Target.BOTH;
    @ConfigEntry.Category("player_effects")
    public boolean playerEffectsFirstPerson = false;
    @ConfigEntry.Category("player_effects")
    public boolean playerEffectsRandomColor = false;

    @ConfigEntry.Gui.PrefixText // Shape
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 300)
    public int playerEffectsScale = 100;
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 300)
    public int playerEffectsSpeed = 100;
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100)
    public int playerEffectsAlpha = 70;
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.BoundedDiscrete(min = 3, max = 24)
    public int playerEffectsCount = 8;

    @ConfigEntry.Gui.PrefixText // Ghost
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
    public int playerEffectsGhostCount = 6;
    @ConfigEntry.Category("player_effects")
    @ConfigEntry.BoundedDiscrete(min = 2, max = 40)
    public int playerEffectsGhostSpacing = 14;

    /**
     * CHINA HAT
     **/
    @ConfigEntry.Category("china_hat")
    public boolean chinaHatEnabled = false;
    @ConfigEntry.Category("china_hat")
    public boolean chinaHatRenderHalf = false;
    @ConfigEntry.Category("china_hat")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int chinaHatAlpha = 90;
    @ConfigEntry.Category("china_hat")
    @ConfigEntry.BoundedDiscrete(min = -100, max = 100)
    public int chinaHatYOffset = 0;
    @ConfigEntry.Gui.PrefixText // tip
    @ConfigEntry.Category("china_hat")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int chinaHatTipHeight = 35;
    @ConfigEntry.Gui.PrefixText // base
    @ConfigEntry.Category("china_hat")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int chinaHatBaseRadius = 65;

    /**
     * HALO
     **/
    @ConfigEntry.Category("halo")
    public boolean haloEnabled = false;
    @ConfigEntry.Category("halo")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Halo.Style haloStyle = Halo.Style.RING;

    @ConfigEntry.Gui.PrefixText // soul
    @ConfigEntry.Category("halo")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Halo.SoulStyle haloSoulRenderSoulStyle = Halo.SoulStyle.SMOKE;
    @ConfigEntry.Category("halo")
    @ConfigEntry.BoundedDiscrete(min = 4, max = 50)
    public int haloSoulLenght = 12;
    @ConfigEntry.Category("halo")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int haloSoulFactor = 16;
    @ConfigEntry.Category("halo")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 50)
    public int haloSoulRadius = 30;
    @ConfigEntry.Category("halo")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 100)
    public int haloSoulStartSize = 50;
    @ConfigEntry.Category("halo")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 100)
    public int haloSoulEndSize = 20;
    @ConfigEntry.Category("halo")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int haloSoulScale = 25;
    @ConfigEntry.Category("halo")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int haloSoulSubdivision = 5;

    /**
     * JUMP CIRCLES
     **/
    @ConfigEntry.Category("jump_circles")
    public boolean jumpCirclesEnabled = false;
    @ConfigEntry.Category("jump_circles")
    public boolean jumpCirclesFadeOut = false;
    @ConfigEntry.Category("jump_circles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public JumpCircles.JumCircleStyle jumpCirclesStyle = JumpCircles.JumCircleStyle.CIRCLE;
    @ConfigEntry.Category("jump_circles")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100) // %
    public int jumpCirclesAlpha = 100;
    @ConfigEntry.Category("jump_circles")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 10)
    public int jumpCirclesColorSpinSpeed = 2;
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Category("jump_circles")
    @ConfigEntry.BoundedDiscrete(min = 50, max = 200)
    public int jumpCirclesScale = 100;
    @ConfigEntry.Category("jump_circles")
    @ConfigEntry.BoundedDiscrete(min = 2, max = 10)
    public int jumpCirclesLiveTime = 3;
    @ConfigEntry.Category("jump_circles")
    @ConfigEntry.BoundedDiscrete(min = -3, max = 3)
    public int jumpCirclesSpinSpeed = 3;

    /**
     * JUMP PARTICLES
     **/
    @ConfigEntry.Category("jump_particles")
    public boolean jumpParticlesEnabled = false;
    @ConfigEntry.Category("jump_particles")
    public boolean jumpParticlesIncludeFirefly = false;
    @ConfigEntry.Category("jump_particles")
    public boolean jumpParticlesIncludeDollar = false;
    @ConfigEntry.Category("jump_particles")
    public boolean jumpParticlesIncludeSnowflake = false;
    @ConfigEntry.Category("jump_particles")
    public boolean jumpParticlesIncludeHeart = false;
    @ConfigEntry.Category("jump_particles")
    public boolean jumpParticlesIncludeStar = false;
    @ConfigEntry.Category("jump_particles")
    public boolean jumpParticlesIncludeGlyphs = false;
    @ConfigEntry.Category("jump_particles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public JumpParticles.Physic jumpParticlesPhysic = JumpParticles.Physic.FLY;
    @ConfigEntry.Category("jump_particles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public JumpParticles.Disappear jumpParticlesDisappear = JumpParticles.Disappear.ALPHA;
    @ConfigEntry.Category("jump_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int jumpParticlesCount = 5;
    @ConfigEntry.Category("jump_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int jumpParticlesSpeed = 5;
    @ConfigEntry.Category("jump_particles")
    @ConfigEntry.BoundedDiscrete(min = 2, max = 10)
    public int jumpParticlesLiveTime = 2;
    @ConfigEntry.Category("jump_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int jumpParticlesScale = 3;

    /**
     * WORLD
     **/
    @ConfigEntry.Gui.PrefixText // ambient particles
    @ConfigEntry.Category("world")
    public boolean ambientParticlesEnabled = false;
    @ConfigEntry.Category("world")
    public boolean ambientParticlesRandomColor = true;
    @ConfigEntry.Category("world")
    public boolean ambientParticlesIncludeFirefly = false;
    @ConfigEntry.Category("world")
    public boolean ambientParticlesIncludeDollar = false;
    @ConfigEntry.Category("world")
    public boolean ambientParticlesIncludeSnowflake = false;
    @ConfigEntry.Category("world")
    public boolean ambientParticlesIncludeHeart = false;
    @ConfigEntry.Category("world")
    public boolean ambientParticlesIncludeStar = false;
    @ConfigEntry.Category("world")
    public boolean ambientParticlesIncludeGlyphs = false;
    @ConfigEntry.Category("world")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public AmbientParticle.Style ambientParticlesStyle = AmbientParticle.Style.BOTH;
    @ConfigEntry.Category("world")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public AmbientParticle.Physics ambientParticlesPhysic = AmbientParticle.Physics.FALL;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 30)
    public int ambientParticlesParticleWithTrailLenght = 10;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
    public int ambientParticlesDefaultParticleScale = 100;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
    public int ambientParticlesParticleWithTrailScale = 100;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int ambientParticlesDefaultParticleCount = 100;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 150)
    public int ambientParticlesParticleWithTrailCount = 30;

    @ConfigEntry.Gui.PrefixText // Time Changer
    @ConfigEntry.Category("world")
    public boolean timeChangerEnabled = false;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int timeChangerTime = 50;

    @ConfigEntry.Gui.PrefixText // Weather Changer
    @ConfigEntry.Category("world")
    public boolean weatherChangerEnabled = false;
    @ConfigEntry.Category("world")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Weather weatherType = Weather.RAIN;

    // The sky lives on its own page - "Animated Sky" in the category list - rather than as a group
    // inside World. It was a group there, with its master switch on the World page and its twenty
    // one patterns on another, and the first person to look for it could not find it.
    @ConfigEntry.Gui.PrefixText // Animated Sky
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public me.Padej_.soupapi.utils.SkyStyle skyStyle = me.Padej_.soupapi.utils.SkyStyle.PLAIN;
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 400)
    public int skySpeed = 100;
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 400)
    public int skyScale = 100;
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 400)
    public int skyStars = 140;

    @ConfigEntry.Gui.PrefixText // Sky Colors
    @ConfigEntry.Category("colored_sky")
    public boolean coloredSkyUseTheme = true;
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.ColorPicker
    public int coloredSkyColor = 0x66a0ff;
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.ColorPicker
    public int skyColor2 = 0x7a2fff;
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.ColorPicker
    public int skyDarkColor = 0x05060e;
    @ConfigEntry.Category("colored_sky")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int coloredSkyAlpha = 60;
    @ConfigEntry.Category("colored_sky")
    public boolean coloredSkyEnabled = false;

    @ConfigEntry.Gui.PrefixText // Custom Fog
    @ConfigEntry.Category("world")
    public boolean customFogEnabled = false;
    @ConfigEntry.Category("world")
    public boolean customFogThick = false;
    @ConfigEntry.Category("world")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public CustomFog.CustomFogShape customFogShape = CustomFog.CustomFogShape.SPHERE;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int customFogStart = 5;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int customFogEnd = 20;
    @ConfigEntry.Category("world")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int customFogDensity = 80;

    /**
     * TARGET RENDER
     **/
    @ConfigEntry.Category("target_render")
    public boolean targetRenderEnabled = false;
    @ConfigEntry.Category("target_render")
    public boolean targetRenderOnlyPlayers = true;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TargetRender.Style targetRenderStyle = TargetRender.Style.LEGACY;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 10)
    public int targetRenderLiveTime = 0;

    @ConfigEntry.Gui.PrefixText // legacy
    @ConfigEntry.Category("target_render")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TargetRender.LegacyTexture targetRenderLegacyTexture = TargetRender.LegacyTexture.LEGACY;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = -150, max = 150)
    public int targetRenderLegacyRollSpeed = 70;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int targetRenderLegacyScale = 60;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 50, max = 100)
    public int targetRenderLegacyAlpha = 80;

    @ConfigEntry.Gui.PrefixText // soul
    @ConfigEntry.Category("target_render")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TargetRender.TargetRenderSoulStyle targetRenderSoulStyle = TargetRender.TargetRenderSoulStyle.SMOKE;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TargetRender.SoulTexture targetRenderSoulTexture = TargetRender.SoulTexture.ALT;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 4, max = 20)
    public int targetRenderSoulLenght = 5;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
    public int targetRenderSoulFactor = 3; // spin speed
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
    public int targetRenderSoulShaking = 3;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int targetRenderSoulAmplitude = 3;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
    public int targetRenderSoulRadius = 100;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 100)
    public int targetRenderSoulStartSize = 50;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 100)
    public int targetRenderSoulEndSize = 20;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int targetRenderSoulScale = 40;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int targetRenderSoulSubdivision = 3;

    // spiral WIP

    @ConfigEntry.Gui.PrefixText // topka
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 30, max = 70)
    public int targetRenderTopkaRadius = 50;
    @ConfigEntry.Category("target_render")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 60)
    public int targetRenderTopkaSpeed = 30;

    /**
     * TARGET HUD
     **/
    @ConfigEntry.Category("target_hud")
    public boolean targetHudEnabled = false;
    @ConfigEntry.Category("target_hud")
    public boolean targetHudFollow = false;
    @ConfigEntry.Category("target_hud")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TargetHud.Style targetHudStyle = TargetHud.Style.ARES;
    @ConfigEntry.Category("target_hud")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
    public int targetHudRenderTime = 5;

    @ConfigEntry.Gui.PrefixText // Particles
    @ConfigEntry.Category("target_hud")
    public boolean targetHudParticles = true;
    @ConfigEntry.Category("target_hud")
    public boolean targetHudIncludeFirefly = false;
    @ConfigEntry.Category("target_hud")
    public boolean targetHudIncludeDollar = false;
    @ConfigEntry.Category("target_hud")
    public boolean targetHudIncludeSnowflake = false;
    @ConfigEntry.Category("target_hud")
    public boolean targetHudIncludeHeart = false;
    @ConfigEntry.Category("target_hud")
    public boolean targetHudIncludeStar = false;
    @ConfigEntry.Category("target_hud")
    public boolean targetHudIncludeGlyphs = false;
    @ConfigEntry.Category("target_hud")
    @ConfigEntry.BoundedDiscrete(min = 50, max = 120)
    public int targetHudParticleScale = 100;

    @ConfigEntry.Gui.PrefixText // Position
    @ConfigEntry.Category("target_hud")
    public int targetHudOffsetX = -25;
    @ConfigEntry.Category("target_hud")
    public int targetHudOffsetY = -80;
    @ConfigEntry.Category("target_hud")
    @ConfigEntry.BoundedDiscrete(min = -50, max = 50)
    public int targetHudEntityOffsetX = 20;
    @ConfigEntry.Category("target_hud")
    @ConfigEntry.BoundedDiscrete(min = -50, max = 50)
    public int targetHudEntityOffsetY = 0;

    /**
     * HUD
     **/
    @ConfigEntry.Gui.PrefixText // hotbar
    @ConfigEntry.Category("hud")
    public boolean hudBetterHotbarEnabled = false;
    @ConfigEntry.Category("hud")
    public boolean hudBetterHotbarShowArmor = true;
    @ConfigEntry.Category("hud")
    public boolean hudBetterHotbarSmooth = true;
    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public BetterHudStyles.HotbarStyle hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.SIMPLE;

    @ConfigEntry.Gui.PrefixText // scoreboard
    @ConfigEntry.Category("hud")
    public boolean hudBetterScoreboardEnabled = false;
    @ConfigEntry.Category("hud")
    public boolean hudBetterScoreboardGlow = false;
    @ConfigEntry.Category("hud")
    public boolean hudBetterScoreboardDarker = false;
    @ConfigEntry.Category("hud")
    public boolean hudBetterScoreboardColor = false;

    @ConfigEntry.Gui.PrefixText // potions hud
    @ConfigEntry.Category("hud")
    public boolean hudBetterPotionsHudEnabled = false;
    @ConfigEntry.Category("hud")
    public boolean hudBetterPotionsHudToRoman = true;
    @ConfigEntry.Category("hud")
    public int hudBetterPotionsHudX = 10;
    @ConfigEntry.Category("hud")
    public int hudBetterPotionsHudY = 115;

    /**
     * HIT BUBBLES
     **/
    @ConfigEntry.Category("hit_bubbles")
    public boolean hitBubblesEnabled = false;
    @ConfigEntry.Category("hit_bubbles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HitBubbles.Animation hitBubblesAnimation = HitBubbles.Animation.SPIN;
    @ConfigEntry.Category("hit_bubbles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HitBubbles.Style hitBubblesStyle = HitBubbles.Style.PORTAL;
    @ConfigEntry.Category("hit_bubbles")
    @ConfigEntry.BoundedDiscrete(min = 15, max = 100)
    public int hitBubblesRenderTime = 30;
    @ConfigEntry.Category("hit_bubbles")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 150)
    public int hitBubblesScale = 100;

    /**
     * HIT SOUND
     **/
    @ConfigEntry.Category("hit_sound")
    public boolean hitSoundEnabled = false;
    @ConfigEntry.Category("hit_sound")
    public boolean hitSoundRandomPitch = false;
    @ConfigEntry.Category("hit_sound")
    public boolean hitSoundOnlyCrit = false;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public CustomSounds.SoundType hitSoundType = CustomSounds.SoundType.GET;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int hitSoundVolume = 50;

    @ConfigEntry.Gui.PrefixText // overwrite sound
    @ConfigEntry.Category("hit_sound")
    public boolean hitSoundOverwriteEnabled = false;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int hitSoundOverwriteCritVolume = 50;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int hitSoundOverwriteSweepVolume = 50;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int hitSoundOverwriteNoDamageVolume = 50;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int hitSoundOverwriteKnockbackVolume = 50;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int hitSoundOverwriteStrongVolume = 50;
    @ConfigEntry.Category("hit_sound")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int hitSoundOverwriteWeakVolume = 50;

    /**
     * RPC
     **/
    @ConfigEntry.Category("rpc")
    public boolean rpcEnabled = true;
    @ConfigEntry.Category("rpc")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public RPC.State rpcState = RPC.State.NAME;
    @ConfigEntry.Category("rpc")
    public String rpcCustomStateText = "Have a nice day!";

    /**
     * SWING HAND
     **/
    @ConfigEntry.Category("swing_hand")
    public boolean swingHandEnabled = false;
    @ConfigEntry.Category("swing_hand")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public SwingHand.Config swingHandConfig_BUTTON = SwingHand.Config.CONFIG_HANDS;
    @ConfigEntry.Category("swing_hand")
    public float swingHand_xPos = 0.7f;
    @ConfigEntry.Category("swing_hand")
    public float swingHand_yPos = -0.4f;
    @ConfigEntry.Category("swing_hand")
    public float swingHand_zPos = -0.85f;
    @ConfigEntry.Category("swing_hand")
    public float swingHand_scale = 0.75f;
    @ConfigEntry.Category("swing_hand")
    public int swingHand_rotX = 0;
    @ConfigEntry.Category("swing_hand")
    public int swingHand_rotY = -13;
    @ConfigEntry.Category("swing_hand")
    public int swingHand_rotZ = 8;
    @ConfigEntry.Category("swing_hand")
    public int swingHand_xSwingRot = -55;
    @ConfigEntry.Category("swing_hand")
    public int swingHand_ySwingRot = 0;
    @ConfigEntry.Category("swing_hand")
    public int swingHand_zSwingRot = 90;
    @ConfigEntry.Category("swing_hand")
    public int swingHand_speed = 100;

    /**
     * ASPECT RATIO
     **/
    @ConfigEntry.Category("aspect_ratio")
    public boolean aspectRatioEnabled = false;
    @ConfigEntry.Category("aspect_ratio")
    public boolean aspectRatioUsePreset = false;
    @ConfigEntry.Category("aspect_ratio")
    @ConfigEntry.BoundedDiscrete(min = 120, max = 220)
    public int aspectRatioFactor = 150;
    @ConfigEntry.Category("aspect_ratio")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public AspectRatio.Preset aspectRatioPreset = AspectRatio.Preset._16_9_;

    /**
     * NO RENDER
     **/
    @ConfigEntry.Gui.PrefixText // no fire overlay
    @ConfigEntry.Category("no_render")
    public boolean noFireOverlayEnabled = false;
    @ConfigEntry.Category("no_render")
    public boolean noFireOverlayCustomColorEnabled = false;
    @ConfigEntry.Category("no_render")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int noFireOverlayY = 35;
    @ConfigEntry.Category("no_render")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int noFireOverlayAlpha = 90;
    @ConfigEntry.Category("no_render")
    @ConfigEntry.ColorPicker
    public int noFireOverlayCustomColor = 0xff0000;

    /**
     * TOTEM
     **/
    @ConfigEntry.Gui.PrefixText // small totem pop
    @ConfigEntry.Category("totem")
    public boolean totemOverwriteScaleEnable = false;
    @ConfigEntry.Category("totem")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int totemOverwriteScale = 30;

    @ConfigEntry.Gui.PrefixText // totem pop shader
    @ConfigEntry.Category("totem")
    public boolean totemPopShaderEnabled = false;
    @ConfigEntry.Category("totem")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int totemShaderAlpha = 80;

    @ConfigEntry.Gui.PrefixText // totem pop particles
    @ConfigEntry.Category("totem")
    public boolean totemPopParticlesEnabled = false;
    @ConfigEntry.Category("totem")
    public boolean totemPopDefaultColors = true;
    @ConfigEntry.Category("totem")
    public boolean totemPopParticlesIncludeFirefly = false;
    @ConfigEntry.Category("totem")
    public boolean totemPopParticlesIncludeDollar = false;
    @ConfigEntry.Category("totem")
    public boolean totemPopParticlesIncludeSnowflake = false;
    @ConfigEntry.Category("totem")
    public boolean totemPopParticlesIncludeHeart = false;
    @ConfigEntry.Category("totem")
    public boolean totemPopParticlesIncludeStar = false;
    @ConfigEntry.Category("totem")
    public boolean totemPopParticlesIncludeGlyphs = false;
    @ConfigEntry.Category("totem")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TotemPopParticles.Physic totemPopParticlesPhysic = TotemPopParticles.Physic.FLY;
    @ConfigEntry.Category("totem")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TotemPopParticles.Disappear totemPopParticlesDisappear = TotemPopParticles.Disappear.ALPHA;
    @ConfigEntry.Category("totem")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int totemPopParticlesCount = 2;
    @ConfigEntry.Category("totem")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int totemPopParticlesSpeed = 2;
    @ConfigEntry.Category("totem")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int totemPopParticlesRenderTime = 2;
    @ConfigEntry.Category("totem")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int totemPopParticlesScale = 3;

    /**
     * HIT PARTICLES
     **/
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticlesEnabled = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticlesSelf = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticlesCritOnly = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticlesLikeCrit = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticlesSplashSpawn = true;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticlesTextShowHeal = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticleIncludeFirefly = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticleIncludeDollar = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticleIncludeSnowflake = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticleIncludeHeart = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticleIncludeStar = false;
    @ConfigEntry.Category("hit_particles")
    public boolean hitParticleIncludeGlyphs = false;

    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HitParticle.HitTextMode hitParticlesTextMode = HitParticle.HitTextMode.DISABLED;
    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HitParticle.Disappear hitParticlesDisappear = HitParticle.Disappear.ALPHA;
    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HitParticle.Physic hitParticlesPhysic = HitParticle.Physic.FLY;

    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int hitParticlesCount = 2;
    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int hitParticlesSpeed = 2;
    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int hitParticlesRenderTime = 2;
    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
    public int hitParticlesTextScale = 1;
    @ConfigEntry.Category("hit_particles")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int hitParticlesScale = 3;

    /**
     * CAPES
     **/
    @ConfigEntry.Category("capes")
    public boolean capesEnabled = false;
    @ConfigEntry.Category("capes")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Capes.Config selectCape_BUTTON = Capes.Config.SELECT_CAPE;
    @ConfigEntry.Category("capes")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Capes.CapeTextures capesTexture = Capes.CapeTextures.CUSTOM_JAVA;

    @ConfigEntry.Gui.PrefixText // Custom cape
    @ConfigEntry.Category("capes")
    public boolean customCapesEnabled = false;
    @ConfigEntry.Category("capes")
    public String customCapesLink = "https://static.wikia.nocookie.net/minecraft_gamepedia/images/8/86/MINECON_2019_Cape_%28texture%29.png/revision/latest?cb=20220316113812";
    @ConfigEntry.Category("capes")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public Capes.Config updateCape_BUTTON = Capes.Config.UPDATE_CAPE;

    /**
     * MOUSE MOVE
     **/
    @ConfigEntry.Category("mouse_move")
    public boolean mouseMoveEnabled = false;
    @ConfigEntry.Category("mouse_move")
    public boolean mouseMoveBlur = false;
    @ConfigEntry.Gui.PrefixText // Position
    @ConfigEntry.Category("mouse_move")
    public int mouseMoveX = 900;
    @ConfigEntry.Category("mouse_move")
    public int mouseMoveY = 450;

    /**
     * HIT COLOR
     **/
    @ConfigEntry.Category("hit_color")
    public boolean hitColorEnabled = false;
    @ConfigEntry.Category("hit_color")
    public boolean hitColorCustomColor = true;
    @ConfigEntry.Category("hit_color")
    @ConfigEntry.ColorPicker
    public int hitColorColor = 0xFF0000;
    @ConfigEntry.Category("hit_color")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int hitColorAlpha = 90;

    /**
     * TRAJECTORIES
     **/
    @ConfigEntry.Category("trajectories")
    public boolean trajectoriesPreviewEnabled = false;
    @ConfigEntry.Category("trajectories")
    public boolean trajectoriesPreviewLandSideOutline = false;
    @ConfigEntry.Category("trajectories")
    public boolean trajectoriesPreviewLandSideFill = false;

    /**
     * MELEE
     **/
    @ConfigEntry.Gui.PrefixText // Melee
    @ConfigEntry.Category("melee")
    public boolean meleeEnabled = false;
    @ConfigEntry.Category("melee")
    public String meleeWeapon = "";
    @ConfigEntry.Category("melee")
    public String[] meleeDisabled = new String[0];
    @ConfigEntry.Category("melee")
    public String meleePackPath = "";
    @ConfigEntry.Category("melee")
    public boolean selectMelee_BUTTON = false;
    @ConfigEntry.Category("melee")
    public boolean rescanMeleePacks_BUTTON = false;
    @ConfigEntry.Category("melee")
    public boolean meleeAnnounce = true;
    @ConfigEntry.Category("melee")
    public boolean meleeThirdPerson = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public me.Padej_.soupapi.melee.MeleeTrigger meleeShowWhen =
            me.Padej_.soupapi.melee.MeleeTrigger.SWORD;
    @ConfigEntry.Gui.PrefixText // Melee Hands
    @ConfigEntry.Category("melee")
    public boolean meleeHands = true;
    @ConfigEntry.Category("melee")
    public boolean meleeLeftHand = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 20, max = 400)
    public int meleeHandsScale = 100;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -180, max = 180)
    public int meleeHandsPitch = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -180, max = 180)
    public int meleeHandsYaw = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -100, max = 100)
    public int meleeHandsOffsetX = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -100, max = 100)
    public int meleeHandsOffsetY = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -100, max = 100)
    public int meleeHandsOffsetZ = 0;
    @ConfigEntry.Category("melee")
    public boolean resetMeleePlacement_BUTTON = false;
    @ConfigEntry.Category("melee")
    public boolean meleeSounds = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int meleeSoundVolume = 100;

    @ConfigEntry.Gui.PrefixText // Melee Placement
    @ConfigEntry.Category("melee")
    public boolean meleeRunAnimation = false;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100)
    public int meleeComboWindow = 30;
    @ConfigEntry.Category("melee")
    public String[] meleePlacements = new String[0];
    @ConfigEntry.Category("melee")
    public boolean saveMeleePlacement_BUTTON = false;
    @ConfigEntry.Category("melee")
    public boolean forgetMeleePlacement_BUTTON = false;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int meleeSize = 100;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -200, max = 200)
    public int meleeDistance = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -200, max = 200)
    public int meleeOffsetX = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -200, max = 200)
    public int meleeOffsetY = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -200, max = 200)
    public int meleeOffsetZ = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -180, max = 180)
    public int meleeYaw = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -180, max = 180)
    public int meleePitch = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = -180, max = 180)
    public int meleeRoll = 0;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 300)
    public int meleeAnimationSpeed = 100;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int meleeCameraShake = 25;
    @ConfigEntry.Category("melee")
    public boolean meleeBob = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 300)
    public int meleeBobStrength = 100;
    @ConfigEntry.Category("melee")
    public boolean meleeSway = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 300)
    public int meleeSwayStrength = 100;

    @ConfigEntry.Gui.PrefixText // Melee Glow
    @ConfigEntry.Category("melee")
    public boolean meleeGlow = true;
    @ConfigEntry.Category("melee")
    public boolean meleeGlowUseTheme = true;
    // Tints the whole weapon toward the theme's colour — demonic red, angelic gold, matrix green —
    // so the blade itself changes with the theme, not only its glow.
    @ConfigEntry.Category("melee")
    public boolean meleeRecolor = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int meleeRecolorStrength = 45;
    @ConfigEntry.Category("melee")
    @ConfigEntry.ColorPicker
    public int meleeGlowColor = 0x5cf0ff;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 300)
    public int meleeGlowStrength = 45;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 300)
    public int meleeGlowPulse = 50;
    @ConfigEntry.Category("melee")
    public boolean meleeOutline = false;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 300)
    public int meleeOutlineStrength = 45;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 120)
    public int meleeOutlineWidth = 15;
    @ConfigEntry.Category("melee")
    public boolean meleeChroma = false;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int meleeChromaStrength = 35;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int meleeChromaSpread = 12;

    @ConfigEntry.Gui.PrefixText // Melee Motion Effects
    @ConfigEntry.Category("melee")
    public boolean meleePhantom = false;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 6)
    public int meleePhantomCount = 3;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 4)
    public int meleePhantomGap = 2;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int meleePhantomStrength = 40;
    @ConfigEntry.Category("melee")
    public boolean meleeTrail = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 2, max = 40)
    public int meleeTrailLength = 16;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int meleeTrailWidth = 55;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int meleeTrailStrength = 65;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int meleeTrailFade = 120;
    @ConfigEntry.Category("melee")
    public boolean meleeSparks = true;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 60)
    public int meleeSparkCount = 6;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int meleeSparkSize = 100;
    @ConfigEntry.Category("melee")
    public boolean meleePulse = false;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int meleePulseSize = 90;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int meleePulseStrength = 80;
    @ConfigEntry.Category("melee")
    public boolean meleeRunes = false;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
    public int meleeRuneCount = 5;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int meleeRuneRadius = 80;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int meleeRuneSize = 100;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 400)
    public int meleeRuneSpeed = 100;
    @ConfigEntry.Category("melee")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
    public int meleeRuneStrength = 80;

    /**
     * AMBIENCE
     **/
    @ConfigEntry.Gui.PrefixText // Ambience
    @ConfigEntry.Category("ambience")
    public boolean ambienceEnabled = false;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public me.Padej_.soupapi.utils.Ambience ambienceStyle = me.Padej_.soupapi.utils.Ambience.NONE;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 150)
    public int ambienceDensity = 45;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.BoundedDiscrete(min = 4, max = 48)
    public int ambienceRadius = 14;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.BoundedDiscrete(min = 3, max = 32)
    public int ambienceHeight = 9;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int ambienceSpeed = 100;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 400)
    public int ambienceSize = 100;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 100)
    public int ambienceAlpha = 80;
    @ConfigEntry.Category("ambience")
    public boolean ambienceUseTheme = true;
    @ConfigEntry.Category("ambience")
    @ConfigEntry.ColorPicker
    public int ambienceColor = 0x00ff41;
}
