package me.Padej_.soupapi.sounds;

import me.Padej_.soupapi.utils.EntityUtils;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Random;

public class CustomSounds {
    public static SoundEvent ON = SoundEvent.of(new Identifier("soupapi", "on"));
    public static SoundEvent OFF = SoundEvent.of(new Identifier("soupapi", "off"));
    public static SoundEvent GET = SoundEvent.of(new Identifier("soupapi", "get"));
    public static SoundEvent BUBBLE = SoundEvent.of(new Identifier("soupapi", "bubble"));
    public static SoundEvent BELL = SoundEvent.of(new Identifier("soupapi", "bell"));
    public static SoundEvent BONK = SoundEvent.of(new Identifier("soupapi", "bonk"));
    public static SoundEvent POK = SoundEvent.of(new Identifier("soupapi", "pok"));
    public static SoundEvent MAGIC_POK = SoundEvent.of(new Identifier("soupapi", "magic_pok"));

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
