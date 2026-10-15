package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.SoupModule;
import me.Padej_.soupapi.screen.CapeSelectScreen;
import me.Padej_.soupapi.settings.impl.BooleanSetting;
import me.Padej_.soupapi.settings.impl.ButtonSetting;
import me.Padej_.soupapi.settings.impl.EnumSetting;
import me.Padej_.soupapi.settings.impl.StringSetting;
import net.minecraft.util.ResourceLocation;

public class Capes extends SoupModule {
    public static BooleanSetting enabled = new BooleanSetting("Enabled", "desc", false);
    @SuppressWarnings("unused")
    public static ButtonSetting capeSelectButton = new ButtonSetting("Select Cape", "desc", () -> mc.displayGuiScreen(new CapeSelectScreen()));
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
        APRIL_AWESOME_CAEP(new ResourceLocation("soupapi:textures/capes/april/awesome_caep.png")),
        APRIL_BLONK_CAEP(new ResourceLocation("soupapi:textures/capes/april/blonk_caep.png")),
        APRIL_NO_CIRCLE_CAEP(new ResourceLocation("soupapi:textures/capes/april/no_circle_caep.png")),
        APRIL_NYAN_CAEP(new ResourceLocation("soupapi:textures/capes/april/nyan_caep.png")),
        APRIL_SQUID_CAEP(new ResourceLocation("soupapi:textures/capes/april/squid_caep.png")),
        APRIL_VETERINARIAN_CAEP(new ResourceLocation("soupapi:textures/capes/april/veterinarian_caep.png")),

        // Default Capes
        DEFAULT_15TH_ANNIVERSARY(new ResourceLocation("soupapi:textures/capes/default/15th_anniversary.png")),
        DEFAULT_BACON(new ResourceLocation("soupapi:textures/capes/default/bacon.png")),
        DEFAULT_BIRTHDAY(new ResourceLocation("soupapi:textures/capes/default/birthday.png")),
        DEFAULT_CHERRY_BLOSSOM(new ResourceLocation("soupapi:textures/capes/default/cherry_blossom.png")),
        DEFAULT_COBALT(new ResourceLocation("soupapi:textures/capes/default/cobalt.png")),
        DEFAULT_DANNY_BSTYLE(new ResourceLocation("soupapi:textures/capes/default/danny_bstyle.png")),
        DEFAULT_FOLLOWERS(new ResourceLocation("soupapi:textures/capes/default/followers.png")),
        DEFAULT_MCC_15TH_YEARS(new ResourceLocation("soupapi:textures/capes/default/mcc_15th_years.png")),
        DEFAULT_MIGRATOR(new ResourceLocation("soupapi:textures/capes/default/migrator.png")),
        DEFAULT_MILLIONTH_CUSTOMER(new ResourceLocation("soupapi:textures/capes/default/millionth_customer.png")),
        DEFAULT_MINECON_2011(new ResourceLocation("soupapi:textures/capes/default/minecon_2011.png")),
        DEFAULT_MINECON_2012(new ResourceLocation("soupapi:textures/capes/default/minecon_2012.png")),
        DEFAULT_MINECON_2013(new ResourceLocation("soupapi:textures/capes/default/minecon_2013.png")),
        DEFAULT_MINECON_2014(new ResourceLocation("soupapi:textures/capes/default/minecon_2014.png")),
        DEFAULT_MINECON_2015(new ResourceLocation("soupapi:textures/capes/default/minecon_2015.png")),
        DEFAULT_MINECON_2016(new ResourceLocation("soupapi:textures/capes/default/minecon_2016.png")),
        DEFAULT_MINECON_2017(new ResourceLocation("soupapi:textures/capes/default/minecon_2017.png")),
        DEFAULT_MINECON_2018(new ResourceLocation("soupapi:textures/capes/default/minecon_2018.png")),
        DEFAULT_MINECON_2019(new ResourceLocation("soupapi:textures/capes/default/minecon_2019.png")),
        DEFAULT_MINECRAFT_EXPERIENCE(new ResourceLocation("soupapi:textures/capes/default/minecraft_experience.png")),
        DEFAULT_MOJANG(new ResourceLocation("soupapi:textures/capes/default/mojang.png")),
        DEFAULT_MOJANG_OLD(new ResourceLocation("soupapi:textures/capes/default/mojang_old.png")),
        DEFAULT_MOJANG_STUDIOS(new ResourceLocation("soupapi:textures/capes/default/mojang_studios.png")),
        DEFAULT_MOJIRA_MODERATOR(new ResourceLocation("soupapi:textures/capes/default/mojira_moderator.png")),
        DEFAULT_NEW_YEARS_2010(new ResourceLocation("soupapi:textures/capes/default/new_years_2010.png")),
        DEFAULT_NEW_YEARS_2011(new ResourceLocation("soupapi:textures/capes/default/new_years_2011.png")),
        DEFAULT_PANCAKE(new ResourceLocation("soupapi:textures/capes/default/pancake.png")),
        DEFAULT_PRISMARINE(new ResourceLocation("soupapi:textures/capes/default/prismarine.png")),
        DEFAULT_PURPLE_HEART(new ResourceLocation("soupapi:textures/capes/default/purple_heart.png")),
        DEFAULT_REALMS_NEW(new ResourceLocation("soupapi:textures/capes/default/realms_new.png")),
        DEFAULT_REALMS_OLD(new ResourceLocation("soupapi:textures/capes/default/realms_old.png")),
        DEFAULT_SCROLLS(new ResourceLocation("soupapi:textures/capes/default/scrolls.png")),
        DEFAULT_SNOWMAN(new ResourceLocation("soupapi:textures/capes/default/snowman.png")),
        DEFAULT_SPADE(new ResourceLocation("soupapi:textures/capes/default/spade.png")),
        DEFAULT_TEST(new ResourceLocation("soupapi:textures/capes/default/test.png")),
        DEFAULT_TRANSLATOR(new ResourceLocation("soupapi:textures/capes/default/translator.png")),
        DEFAULT_TURTLE(new ResourceLocation("soupapi:textures/capes/default/turtle.png")),
        DEFAULT_UNUSED_1(new ResourceLocation("soupapi:textures/capes/default/unused_1.png")),
        DEFAULT_UNUSED_2(new ResourceLocation("soupapi:textures/capes/default/unused_2.png")),
        DEFAULT_UNUSED_3(new ResourceLocation("soupapi:textures/capes/default/unused_3.png")),
        DEFAULT_VALENTINE(new ResourceLocation("soupapi:textures/capes/default/valentine.png")),
        DEFAULT_VANILLA(new ResourceLocation("soupapi:textures/capes/default/vanilla.png")),
        DEFAULT_MENACE(new ResourceLocation("soupapi:textures/capes/default/menace.png")),
        DEFAULT_HOME(new ResourceLocation("soupapi:textures/capes/default/home.png")),
        DEFAULT_OFFICE(new ResourceLocation("soupapi:textures/capes/default/office.png")),
        DEFAULT_YEARN(new ResourceLocation("soupapi:textures/capes/default/yearn.png")),
        DEFAULT_COMMON(new ResourceLocation("soupapi:textures/capes/default/common.png")),

        // Microsoft(XBox) capes
        XBOX_XBOX(new ResourceLocation("soupapi:textures/capes/microsoft/xbox.png")),
        XBOX_1ST_BIRTHDAY(new ResourceLocation("soupapi:textures/capes/microsoft/xbox_1st_birthday.png")),
        XBOX_UNUSED_STARWARS_1(new ResourceLocation("soupapi:textures/capes/microsoft/unused_starwars_1.png")),
        XBOX_UNUSED_STARWARS_2(new ResourceLocation("soupapi:textures/capes/microsoft/unused_starwars_2.png")),
        XBOX_ADVENTURE_TIME_UNUSED(new ResourceLocation("soupapi:textures/capes/microsoft/adventuretime_unused.png")),

        // Minecraft dungeons capes
        MCD_AMETHYST(new ResourceLocation("soupapi:textures/capes/mcd/amethyst.png")),
        MCD_CLOUDY_CLIMB(new ResourceLocation("soupapi:textures/capes/mcd/cloudy_climb.png")),
        MCD_COW(new ResourceLocation("soupapi:textures/capes/mcd/cow.png")),
        MCD_DOWNPOUR(new ResourceLocation("soupapi:textures/capes/mcd/downpour.png")),
        MCD_FAUNA_FAIRE(new ResourceLocation("soupapi:textures/capes/mcd/fauna_faire.png")),
        MCD_GIFT_WARP(new ResourceLocation("soupapi:textures/capes/mcd/gift_warp.png")),
        MCD_GLOW(new ResourceLocation("soupapi:textures/capes/mcd/glow.png")),
        MCD_HAMMER(new ResourceLocation("soupapi:textures/capes/mcd/hammer.png")),
        MCD_HERO(new ResourceLocation("soupapi:textures/capes/mcd/hero.png")),
        MCD_ICOLOGER(new ResourceLocation("soupapi:textures/capes/mcd/icologer.png")),
        MCD_LUMINOUS_NIGHT(new ResourceLocation("soupapi:textures/capes/mcd/luminous_night.png")),
        MCD_PHANTOM(new ResourceLocation("soupapi:textures/capes/mcd/phantom.png")),
        MCD_SINISTER(new ResourceLocation("soupapi:textures/capes/mcd/sinister.png")),
        MCD_TURTLE(new ResourceLocation("soupapi:textures/capes/mcd/turtle.png")),

        // Custom Capes
        CUSTOM_1UP(new ResourceLocation("soupapi:textures/capes/custom/1up.png")),
        CUSTOM_ADIDAS(new ResourceLocation("soupapi:textures/capes/custom/adidas.png")),
        CUSTOM_ALPHA(new ResourceLocation("soupapi:textures/capes/custom/alpha.png")),
        CUSTOM_AZURE(new ResourceLocation("soupapi:textures/capes/custom/azure.png")),
        CUSTOM_BLUE_FLAME(new ResourceLocation("soupapi:textures/capes/custom/blue_flame.png")),
        CUSTOM_BROWN_FEATHER(new ResourceLocation("soupapi:textures/capes/custom/brown_feather.png")),
        CUSTOM_BUGGED(new ResourceLocation("soupapi:textures/capes/custom/bugged.png")),
        CUSTOM_CB(new ResourceLocation("soupapi:textures/capes/custom/cb.png")),
        CUSTOM_CHEST(new ResourceLocation("soupapi:textures/capes/custom/chest.png")),
        CUSTOM_CHRISTMAS_LUNAR(new ResourceLocation("soupapi:textures/capes/custom/christmas_lunar.png")),
        CUSTOM_DARK_GOLD(new ResourceLocation("soupapi:textures/capes/custom/dark_gold.png")),
        CUSTOM_DISCORD(new ResourceLocation("soupapi:textures/capes/custom/discord.png")),
        CUSTOM_FABRIC(new ResourceLocation("soupapi:textures/capes/custom/fabric.png")),
        CUSTOM_GALAXY(new ResourceLocation("soupapi:textures/capes/custom/galaxy.png")),
        CUSTOM_GAMEBOY(new ResourceLocation("soupapi:textures/capes/custom/gameboy.png")),
        CUSTOM_GENDERFLUID(new ResourceLocation("soupapi:textures/capes/custom/genderfluid.png")),
        CUSTOM_GRAY(new ResourceLocation("soupapi:textures/capes/custom/gray.png")),
        CUSTOM_HALLOWEEN(new ResourceLocation("soupapi:textures/capes/custom/halloween.png")),
        CUSTOM_ICE(new ResourceLocation("soupapi:textures/capes/custom/ice.png")),
        CUSTOM_JAVA(new ResourceLocation("soupapi:textures/capes/custom/java.png")),
        CUSTOM_JUKE_BOX(new ResourceLocation("soupapi:textures/capes/custom/juke_box.png")),
        CUSTOM_KIRBY(new ResourceLocation("soupapi:textures/capes/custom/kirby.png")),
        CUSTOM_MATRIX(new ResourceLocation("soupapi:textures/capes/custom/matrix.png")),
        CUSTOM_NASA(new ResourceLocation("soupapi:textures/capes/custom/nasa.png")),
        CUSTOM_NETHERITE(new ResourceLocation("soupapi:textures/capes/custom/netherite.png")),
        CUSTOM_OMEGA_1(new ResourceLocation("soupapi:textures/capes/custom/omega_1.png")),
        CUSTOM_OMEGA_2(new ResourceLocation("soupapi:textures/capes/custom/omega_2.png")),
        CUSTOM_OMEGA_3(new ResourceLocation("soupapi:textures/capes/custom/omega_3.png")),
        CUSTOM_ORANGE(new ResourceLocation("soupapi:textures/capes/custom/orange.png")),
        CUSTOM_PURPLE(new ResourceLocation("soupapi:textures/capes/custom/purple.png")),
        CUSTOM_RAIN(new ResourceLocation("soupapi:textures/capes/custom/rain.png")),
        CUSTOM_ROSE(new ResourceLocation("soupapi:textures/capes/custom/rose.png")),
        CUSTOM_SMOOTH_BLUE(new ResourceLocation("soupapi:textures/capes/custom/smooth_blue.png")),
        CUSTOM_SNOWFLAKE_LUNAR(new ResourceLocation("soupapi:textures/capes/custom/snowflake_lunar.png")),
        CUSTOM_SPEEDSILVER(new ResourceLocation("soupapi:textures/capes/custom/speedsilver.png")),
        CUSTOM_VETERAN(new ResourceLocation("soupapi:textures/capes/custom/veteran.png")),
        CUSTOM_WINTER(new ResourceLocation("soupapi:textures/capes/custom/winter.png")),
        CUSTOM_YOUTUBE(new ResourceLocation("soupapi:textures/capes/custom/youtube.png")),

        // Optifine
        OPTIFINE_STANDARD(new ResourceLocation("soupapi:textures/capes/optifine/standard.png")),
        OPTIFINE_BLACK(new ResourceLocation("soupapi:textures/capes/optifine/black.png")),
        OPTIFINE_BLUE(new ResourceLocation("soupapi:textures/capes/optifine/blue.png")),
        OPTIFINE_CYAN(new ResourceLocation("soupapi:textures/capes/optifine/cyan.png")),
        OPTIFINE_GRAY(new ResourceLocation("soupapi:textures/capes/optifine/gray.png")),
        OPTIFINE_GREEN(new ResourceLocation("soupapi:textures/capes/optifine/green.png")),
        OPTIFINE_PURPLE(new ResourceLocation("soupapi:textures/capes/optifine/purple.png")),
        OPTIFINE_RED(new ResourceLocation("soupapi:textures/capes/optifine/red.png")),
        OPTIFINE_YELLOW(new ResourceLocation("soupapi:textures/capes/optifine/yellow.png")),
        OPTIFINE_WHITE(new ResourceLocation("soupapi:textures/capes/optifine/white.png")),

        // Staff Capes by winvi
        STAFF_TEAM_PADEJ(new ResourceLocation("soupapi:textures/capes/team/padej_.png")),
        STAFF_TEAM_WINVI(new ResourceLocation("soupapi:textures/capes/team/winvi.png")),
        STAFF_TEAM_ICYCROW(new ResourceLocation("soupapi:textures/capes/team/icycrow.png")),
        STAFF_TEAM_NOVADVORGA(new ResourceLocation("soupapi:textures/capes/team/novadvorga.png")),
        STAFF_TEAM_IBRRPATAPIM_(new ResourceLocation("soupapi:textures/capes/team/ibrrpatapim_.png"));

        private final ResourceLocation texturePath;

        CapeTextures(ResourceLocation texturePath) {
            this.texturePath = texturePath;
        }

        public ResourceLocation getTexturePath() {
            return texturePath;
        }
    }
}
