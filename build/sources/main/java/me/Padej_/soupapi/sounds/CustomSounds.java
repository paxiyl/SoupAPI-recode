package me.Padej_.soupapi.sounds;

import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.compat.SoundEvent;
import net.minecraft.util.ResourceLocation;

public class CustomSounds {
    public static SoundEvent ON = SoundEvent.of(new ResourceLocation("soupapi", "on"));
    public static SoundEvent OFF = SoundEvent.of(new ResourceLocation("soupapi", "off"));
    public static SoundEvent GET = SoundEvent.of(new ResourceLocation("soupapi", "get"));
    public static SoundEvent BUBBLE = SoundEvent.of(new ResourceLocation("soupapi", "bubble"));
    public static SoundEvent BELL = SoundEvent.of(new ResourceLocation("soupapi", "bell"));
    public static SoundEvent BONK = SoundEvent.of(new ResourceLocation("soupapi", "bonk"));
    public static SoundEvent POK = SoundEvent.of(new ResourceLocation("soupapi", "pok"));
    public static SoundEvent MAGIC_POK = SoundEvent.of(new ResourceLocation("soupapi", "magic_pok"));

    public enum SoundType {
        ON,
        OFF,
        GET,
        BUBBLE,
        BELL,
        BONK,
        POK,
        MAGIC_POK;
    }
}
