package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.SoupModule;
import me.Padej_.soupapi.screen.CapeSelectScreen;
import me.Padej_.soupapi.settings.impl.BooleanSetting;
import me.Padej_.soupapi.settings.impl.ButtonSetting;
import me.Padej_.soupapi.settings.impl.EnumSetting;
import me.Padej_.soupapi.settings.impl.StringSetting;
import net.minecraft.util.Identifier;

public class Capes extends SoupModule {
    public static BooleanSetting enabled = new BooleanSetting("Enabled", "desc", false);
    @SuppressWarnings("unused")
    public static ButtonSetting capeSelectButton = new ButtonSetting("Select Cape", "desc", () -> mc.setScreen(new CapeSelectScreen()));
    public static final EnumSetting<CapeTextures> cape = new EnumSetting<>("Cape", "description", CapeTextures.CUSTOM_JAVA, CapeTextures.class);

    public static BooleanSetting useCustom = new BooleanSetting("Custom", "desc", false);
    public static StringSetting link = new StringSetting("Link", "dessdas", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/8/86/MINECON_2019_Cape_%28texture%29.png/revision/latest?cb=20220316113812");

    public Capes() {
        super("Capes", Category.OTHER);
    }

    public enum Config {
        SELECT_CAPE, UPDATE_CAPE
    }

    public enum CapeTextures {
        // April Fools Caeps
        APRIL_AWESOME_CAEP(new Identifier("soupapi:textures/capes/april/awesome_caep.png")),
        APRIL_BLONK_CAEP(new Identifier("soupapi:textures/capes/april/blonk_caep.png")),
        APRIL_NO_CIRCLE_CAEP(new Identifier("soupapi:textures/capes/april/no_circle_caep.png")),
        APRIL_NYAN_CAEP(new Identifier("soupapi:textures/capes/april/nyan_caep.png")),
        APRIL_SQUID_CAEP(new Identifier("soupapi:textures/capes/april/squid_caep.png")),
        APRIL_VETERINARIAN_CAEP(new Identifier("soupapi:textures/capes/april/veterinarian_caep.png")),

        // Default Capes
        DEFAULT_15TH_ANNIVERSARY(new Identifier("soupapi:textures/capes/default/15th_anniversary.png")),
        DEFAULT_BACON(new Identifier("soupapi:textures/capes/default/bacon.png")),
        DEFAULT_BIRTHDAY(new Identifier("soupapi:textures/capes/default/birthday.png")),
        DEFAULT_CHERRY_BLOSSOM(new Identifier("soupapi:textures/capes/default/cherry_blossom.png")),
        DEFAULT_COBALT(new Identifier("soupapi:textures/capes/default/cobalt.png")),
        DEFAULT_DANNY_BSTYLE(new Identifier("soupapi:textures/capes/default/danny_bstyle.png")),
        DEFAULT_FOLLOWERS(new Identifier("soupapi:textures/capes/default/followers.png")),
        DEFAULT_MCC_15TH_YEARS(new Identifier("soupapi:textures/capes/default/mcc_15th_years.png")),
        DEFAULT_MIGRATOR(new Identifier("soupapi:textures/capes/default/migrator.png")),
        DEFAULT_MILLIONTH_CUSTOMER(new Identifier("soupapi:textures/capes/default/millionth_customer.png")),
        DEFAULT_MINECON_2011(new Identifier("soupapi:textures/capes/default/minecon_2011.png")),
        DEFAULT_MINECON_2012(new Identifier("soupapi:textures/capes/default/minecon_2012.png")),
        DEFAULT_MINECON_2013(new Identifier("soupapi:textures/capes/default/minecon_2013.png")),
        DEFAULT_MINECON_2014(new Identifier("soupapi:textures/capes/default/minecon_2014.png")),
        DEFAULT_MINECON_2015(new Identifier("soupapi:textures/capes/default/minecon_2015.png")),
        DEFAULT_MINECON_2016(new Identifier("soupapi:textures/capes/default/minecon_2016.png")),
        DEFAULT_MINECON_2017(new Identifier("soupapi:textures/capes/default/minecon_2017.png")),
        DEFAULT_MINECON_2018(new Identifier("soupapi:textures/capes/default/minecon_2018.png")),
        DEFAULT_MINECON_2019(new Identifier("soupapi:textures/capes/default/minecon_2019.png")),
        DEFAULT_MINECRAFT_EXPERIENCE(new Identifier("soupapi:textures/capes/default/minecraft_experience.png")),
        DEFAULT_MOJANG(new Identifier("soupapi:textures/capes/default/mojang.png")),
        DEFAULT_MOJANG_OLD(new Identifier("soupapi:textures/capes/default/mojang_old.png")),
        DEFAULT_MOJANG_STUDIOS(new Identifier("soupapi:textures/capes/default/mojang_studios.png")),
        DEFAULT_MOJIRA_MODERATOR(new Identifier("soupapi:textures/capes/default/mojira_moderator.png")),
        DEFAULT_NEW_YEARS_2010(new Identifier("soupapi:textures/capes/default/new_years_2010.png")),
        DEFAULT_NEW_YEARS_2011(new Identifier("soupapi:textures/capes/default/new_years_2011.png")),
        DEFAULT_PANCAKE(new Identifier("soupapi:textures/capes/default/pancake.png")),
        DEFAULT_PRISMARINE(new Identifier("soupapi:textures/capes/default/prismarine.png")),
        DEFAULT_PURPLE_HEART(new Identifier("soupapi:textures/capes/default/purple_heart.png")),
        DEFAULT_REALMS_NEW(new Identifier("soupapi:textures/capes/default/realms_new.png")),
        DEFAULT_REALMS_OLD(new Identifier("soupapi:textures/capes/default/realms_old.png")),
        DEFAULT_SCROLLS(new Identifier("soupapi:textures/capes/default/scrolls.png")),
        DEFAULT_SNOWMAN(new Identifier("soupapi:textures/capes/default/snowman.png")),
        DEFAULT_SPADE(new Identifier("soupapi:textures/capes/default/spade.png")),
        DEFAULT_TEST(new Identifier("soupapi:textures/capes/default/test.png")),
        DEFAULT_TRANSLATOR(new Identifier("soupapi:textures/capes/default/translator.png")),
        DEFAULT_TURTLE(new Identifier("soupapi:textures/capes/default/turtle.png")),
        DEFAULT_UNUSED_1(new Identifier("soupapi:textures/capes/default/unused_1.png")),
        DEFAULT_UNUSED_2(new Identifier("soupapi:textures/capes/default/unused_2.png")),
        DEFAULT_UNUSED_3(new Identifier("soupapi:textures/capes/default/unused_3.png")),
        DEFAULT_VALENTINE(new Identifier("soupapi:textures/capes/default/valentine.png")),
        DEFAULT_VANILLA(new Identifier("soupapi:textures/capes/default/vanilla.png")),
        DEFAULT_MENACE(new Identifier("soupapi:textures/capes/default/menace.png")),
        DEFAULT_HOME(new Identifier("soupapi:textures/capes/default/home.png")),
        DEFAULT_OFFICE(new Identifier("soupapi:textures/capes/default/office.png")),
        DEFAULT_YEARN(new Identifier("soupapi:textures/capes/default/yearn.png")),
        DEFAULT_COMMON(new Identifier("soupapi:textures/capes/default/common.png")),

        // Microsoft(XBox) capes
        XBOX_XBOX(new Identifier("soupapi:textures/capes/microsoft/xbox.png")),
        XBOX_1ST_BIRTHDAY(new Identifier("soupapi:textures/capes/microsoft/xbox_1st_birthday.png")),
        XBOX_UNUSED_STARWARS_1(new Identifier("soupapi:textures/capes/microsoft/unused_starwars_1.png")),
        XBOX_UNUSED_STARWARS_2(new Identifier("soupapi:textures/capes/microsoft/unused_starwars_2.png")),
        XBOX_ADVENTURE_TIME_UNUSED(new Identifier("soupapi:textures/capes/microsoft/adventuretime_unused.png")),

        // Minecraft dungeons capes
        MCD_AMETHYST(new Identifier("soupapi:textures/capes/mcd/amethyst.png")),
        MCD_CLOUDY_CLIMB(new Identifier("soupapi:textures/capes/mcd/cloudy_climb.png")),
        MCD_COW(new Identifier("soupapi:textures/capes/mcd/cow.png")),
        MCD_DOWNPOUR(new Identifier("soupapi:textures/capes/mcd/downpour.png")),
        MCD_FAUNA_FAIRE(new Identifier("soupapi:textures/capes/mcd/fauna_faire.png")),
        MCD_GIFT_WARP(new Identifier("soupapi:textures/capes/mcd/gift_warp.png")),
        MCD_GLOW(new Identifier("soupapi:textures/capes/mcd/glow.png")),
        MCD_HAMMER(new Identifier("soupapi:textures/capes/mcd/hammer.png")),
        MCD_HERO(new Identifier("soupapi:textures/capes/mcd/hero.png")),
        MCD_ICOLOGER(new Identifier("soupapi:textures/capes/mcd/icologer.png")),
        MCD_LUMINOUS_NIGHT(new Identifier("soupapi:textures/capes/mcd/luminous_night.png")),
        MCD_PHANTOM(new Identifier("soupapi:textures/capes/mcd/phantom.png")),
        MCD_SINISTER(new Identifier("soupapi:textures/capes/mcd/sinister.png")),
        MCD_TURTLE(new Identifier("soupapi:textures/capes/mcd/turtle.png")),

        // Custom Capes
        CUSTOM_1UP(new Identifier("soupapi:textures/capes/custom/1up.png")),
        CUSTOM_ADIDAS(new Identifier("soupapi:textures/capes/custom/adidas.png")),
        CUSTOM_ALPHA(new Identifier("soupapi:textures/capes/custom/alpha.png")),
        CUSTOM_AZURE(new Identifier("soupapi:textures/capes/custom/azure.png")),
        CUSTOM_BLUE_FLAME(new Identifier("soupapi:textures/capes/custom/blue_flame.png")),
        CUSTOM_BROWN_FEATHER(new Identifier("soupapi:textures/capes/custom/brown_feather.png")),
        CUSTOM_BUGGED(new Identifier("soupapi:textures/capes/custom/bugged.png")),
        CUSTOM_CB(new Identifier("soupapi:textures/capes/custom/cb.png")),
        CUSTOM_CHEST(new Identifier("soupapi:textures/capes/custom/chest.png")),
        CUSTOM_CHRISTMAS_LUNAR(new Identifier("soupapi:textures/capes/custom/christmas_lunar.png")),
        CUSTOM_DARK_GOLD(new Identifier("soupapi:textures/capes/custom/dark_gold.png")),
        CUSTOM_DISCORD(new Identifier("soupapi:textures/capes/custom/discord.png")),
        CUSTOM_FABRIC(new Identifier("soupapi:textures/capes/custom/fabric.png")),
        CUSTOM_GALAXY(new Identifier("soupapi:textures/capes/custom/galaxy.png")),
        CUSTOM_GAMEBOY(new Identifier("soupapi:textures/capes/custom/gameboy.png")),
        CUSTOM_GENDERFLUID(new Identifier("soupapi:textures/capes/custom/genderfluid.png")),
        CUSTOM_GRAY(new Identifier("soupapi:textures/capes/custom/gray.png")),
        CUSTOM_HALLOWEEN(new Identifier("soupapi:textures/capes/custom/halloween.png")),
        CUSTOM_ICE(new Identifier("soupapi:textures/capes/custom/ice.png")),
        CUSTOM_JAVA(new Identifier("soupapi:textures/capes/custom/java.png")),
        CUSTOM_JUKE_BOX(new Identifier("soupapi:textures/capes/custom/juke_box.png")),
        CUSTOM_KIRBY(new Identifier("soupapi:textures/capes/custom/kirby.png")),
        CUSTOM_MATRIX(new Identifier("soupapi:textures/capes/custom/matrix.png")),
        CUSTOM_NASA(new Identifier("soupapi:textures/capes/custom/nasa.png")),
        CUSTOM_NETHERITE(new Identifier("soupapi:textures/capes/custom/netherite.png")),
        CUSTOM_OMEGA_1(new Identifier("soupapi:textures/capes/custom/omega_1.png")),
        CUSTOM_OMEGA_2(new Identifier("soupapi:textures/capes/custom/omega_2.png")),
        CUSTOM_OMEGA_3(new Identifier("soupapi:textures/capes/custom/omega_3.png")),
        CUSTOM_ORANGE(new Identifier("soupapi:textures/capes/custom/orange.png")),
        CUSTOM_PURPLE(new Identifier("soupapi:textures/capes/custom/purple.png")),
        CUSTOM_RAIN(new Identifier("soupapi:textures/capes/custom/rain.png")),
        CUSTOM_ROSE(new Identifier("soupapi:textures/capes/custom/rose.png")),
        CUSTOM_SMOOTH_BLUE(new Identifier("soupapi:textures/capes/custom/smooth_blue.png")),
        CUSTOM_SNOWFLAKE_LUNAR(new Identifier("soupapi:textures/capes/custom/snowflake_lunar.png")),
        CUSTOM_SPEEDSILVER(new Identifier("soupapi:textures/capes/custom/speedsilver.png")),
        CUSTOM_VETERAN(new Identifier("soupapi:textures/capes/custom/veteran.png")),
        CUSTOM_WINTER(new Identifier("soupapi:textures/capes/custom/winter.png")),
        CUSTOM_YOUTUBE(new Identifier("soupapi:textures/capes/custom/youtube.png")),

        // Optifine
        OPTIFINE_STANDARD(new Identifier("soupapi:textures/capes/optifine/standard.png")),
        OPTIFINE_BLACK(new Identifier("soupapi:textures/capes/optifine/black.png")),
        OPTIFINE_BLUE(new Identifier("soupapi:textures/capes/optifine/blue.png")),
        OPTIFINE_CYAN(new Identifier("soupapi:textures/capes/optifine/cyan.png")),
        OPTIFINE_GRAY(new Identifier("soupapi:textures/capes/optifine/gray.png")),
        OPTIFINE_GREEN(new Identifier("soupapi:textures/capes/optifine/green.png")),
        OPTIFINE_PURPLE(new Identifier("soupapi:textures/capes/optifine/purple.png")),
        OPTIFINE_RED(new Identifier("soupapi:textures/capes/optifine/red.png")),
        OPTIFINE_YELLOW(new Identifier("soupapi:textures/capes/optifine/yellow.png")),
        OPTIFINE_WHITE(new Identifier("soupapi:textures/capes/optifine/white.png")),

        // Staff Capes by winvi
        STAFF_TEAM_PADEJ(new Identifier("soupapi:textures/capes/team/padej_.png")),
        STAFF_TEAM_WINVI(new Identifier("soupapi:textures/capes/team/winvi.png")),
        STAFF_TEAM_ICYCROW(new Identifier("soupapi:textures/capes/team/icycrow.png")),
        STAFF_TEAM_NOVADVORGA(new Identifier("soupapi:textures/capes/team/novadvorga.png")),
        STAFF_TEAM_IBRRPATAPIM_(new Identifier("soupapi:textures/capes/team/ibrrpatapim_.png"));

        private final Identifier texturePath;

        CapeTextures(Identifier texturePath) {
            this.texturePath = texturePath;
        }

        public Identifier getTexturePath() {
            return texturePath;
        }
    }
}
