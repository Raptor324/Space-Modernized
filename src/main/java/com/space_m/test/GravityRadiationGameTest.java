package com.space_m.test;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.extprop.HbmLivingProps;
import com.space_m.SpaceMod;
import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.dim.SpaceDimensions;
import com.space_m.gravity.CelestialGravity;
import com.space_m.platform.GravityHooks;
import com.space_m.time.CelestialTime;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Schwerkraft und Sonnenstrahlung je Himmelskoerper (NTM Space {@code ModEventHandler.onLivingUpdate/onEntityFall},
 * {@code EntityEffectHandler.handleRadiationFX}).
 *
 * <p>Der NeoForge-1.21.1-GameTestServer laedt keine Datapack-Dimensionen (s. {@code DimensionGameTest}); dann laufen
 * die Pruefungen in der Oberwelt mit den Werten des Mun-Koerpers (gleiche Rechnung, nur ohne Mun-Gelaende).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GravityRadiationGameTest {

    private GravityRadiationGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static CelestialBody body(String name) {
        CelestialBody b = CelestialBody.getBodyOrNull(name);
        check(b != null, "Koerper fehlt: " + name);
        return b;
    }

    /** Freie Luftsaeule hoch ueber dem Gelaende (Chunk geladen). */
    private static BlockPos airPos(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getMaxBuildHeight() - 40, z);
    }

    /** Laesst ein Schwein {@code ticks} Ticks fallen (Schwerkraft-Tick + Vanilla-Tick), Rueckgabe: Fallstrecke. */
    private static double fall(Pig pig, float gravity, int ticks) {
        double y0 = pig.getY();
        for (int i = 0; i < ticks; i++) {
            CelestialGravity.tick(pig, gravity);
            pig.tick();
        }
        return y0 - pig.getY();
    }

    private static Pig pig(ServerLevel level, BlockPos pos) {
        Pig pig = EntityType.PIG.create(level);
        check(pig != null, "Schwein");
        pig.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0, 0);
        pig.setNoAi(true);
        pig.tickCount = 100; // Original: erst nach 20 Ticks
        return pig;
    }

    /** Mun-Schwerkraft (Attribut g/20, Fallkurve, Fallschaden) gegen Kerbin; Wassermobs und Huehner wie Original. */
    @GameTest(template = "empty3x3x3", batch = "space_gravity", timeoutTicks = 40)
    public static void gravityMunVsKerbin(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel munLevel = overworld.getServer().getLevel(SpaceDimensions.MUN);
        boolean realMun = munLevel != null;
        ServerLevel mun = realMun ? munLevel : overworld;

        float gMun = CelestialEnvironment.gravityOf(body("mun"));
        float gKerbin = CelestialEnvironment.gravityOf(body("kerbin"));
        float gDuna = CelestialEnvironment.gravityOf(body("duna"));
        check(gKerbin >= 1.5F && gKerbin <= 1.7F, "Kerbin ~1.6, ist " + gKerbin);
        check(gMun > 0 && gMun < 0.5F, "Mun leicht, ist " + gMun);
        if (realMun) {
            Pig probe = pig(mun, airPos(mun, 8, 8));
            check(Math.abs(CelestialBody.getGravity(probe) - gMun) < 1e-5, "getGravity auf der Mun");
            check(SpaceApi.gravity(probe) == CelestialBody.getGravity(probe), "SpaceApi.gravity = CelestialBody.getGravity");
        }

        // Attribut: Mun = 0.08 * max(g, 0.2) / 1.6, Kerbin unveraendert 0.08
        Pig munPig = pig(mun, airPos(mun, 8, 8));
        CelestialGravity.tick(munPig, gMun);
        double expected = GravityHooks.BASE * Math.max(gMun, 0.2F) / AstronomyUtil.STANDARD_GRAVITY;
        check(Math.abs(GravityHooks.get(munPig) - expected) < 1e-9, "Mun-Schwerkraftattribut " + GravityHooks.get(munPig) + " statt " + expected);

        Pig kerbinPig = pig(overworld, airPos(overworld, helper.absolutePos(BlockPos.ZERO).getX(), helper.absolutePos(BlockPos.ZERO).getZ()));
        CelestialGravity.tick(kerbinPig, gKerbin);
        check(Math.abs(GravityHooks.get(kerbinPig) - GravityHooks.BASE) < 1e-9, "Kerbin-Schwerkraft 0.08, ist " + GravityHooks.get(kerbinPig));

        // Fallkurve: Strecke linear in der Schwerkraft (Vanilla-Daempfung gleich) -> Verhaeltnis = Faktor
        double dMun = fall(munPig, gMun, 12);
        double dKerbin = fall(kerbinPig, gKerbin, 12);
        double ratio = dMun / dKerbin;
        double factor = Math.max(gMun, 0.2F) / AstronomyUtil.STANDARD_GRAVITY;
        check(dKerbin > 1.0D, "Kerbin-Schwein faellt, " + dKerbin);
        check(Math.abs(ratio - factor) < 0.02D, "Fallstrecke Mun/Kerbin " + ratio + " statt " + factor);

        // Ersten 20 Ticks, Wasser: unveraendert; Huhn nie; Wassermob Standardschwere
        Pig young = pig(mun, airPos(mun, 8, 8));
        young.tickCount = 5;
        CelestialGravity.tick(young, gMun);
        check(GravityHooks.get(young) == GravityHooks.BASE, "erste 20 Ticks normal");
        Chicken chicken = EntityType.CHICKEN.create(mun);
        check(chicken != null, "Huhn");
        chicken.tickCount = 100;
        CelestialGravity.tick(chicken, gMun);
        check(GravityHooks.get(chicken) == GravityHooks.BASE, "Huehner ohne Koerperschwerkraft");
        Squid squid = EntityType.SQUID.create(mun);
        check(squid != null && CelestialBody.getGravity(squid) == AstronomyUtil.STANDARD_GRAVITY, "Wassermob Standardschwere");

        // Schwerelos (Orbit): Attribut 0
        Pig floating = pig(mun, airPos(mun, 8, 8));
        CelestialGravity.tick(floating, 0.0F);
        check(GravityHooks.get(floating) == 0.0D, "schwerelos 0");

        // Fallschaden (onEntityFall): g < 0.3 -> 0, g < 1.5 -> * g/1.6, Kerbin unveraendert
        check(CelestialGravity.fallDistance(kerbinPig, 10F) == 10F, "Kerbin-Fallhoehe unveraendert");
        if (realMun) {
            float munFall = CelestialGravity.fallDistance(munPig, 10F);
            float exp = gMun < 0.3F ? 0F : 10F * gMun / AstronomyUtil.STANDARD_GRAVITY;
            check(Math.abs(munFall - exp) < 1e-5, "Mun-Fallhoehe " + munFall + " statt " + exp);
            ServerLevel duna = overworld.getServer().getLevel(SpaceDimensions.DUNA);
            if (duna != null) {
                Pig dunaPig = pig(duna, airPos(duna, 8, 8));
                float dunaFall = CelestialGravity.fallDistance(dunaPig, 10F);
                float expD = gDuna < 0.3F ? 0F : gDuna < 1.5F ? 10F * gDuna / AstronomyUtil.STANDARD_GRAVITY : 10F;
                check(Math.abs(dunaFall - expD) < 1e-5, "Duna-Fallhoehe " + dunaFall + " statt " + expD);
            }
        }

        // Zurueck auf Kerbin: Modifikator wird entfernt
        CelestialGravity.tick(munPig, gKerbin);
        check(GravityHooks.get(munPig) == GravityHooks.BASE, "Modifikator entfernt");
        helper.succeed();
    }

    /** Sonnenstrahlung auf der Mun-Oberflaeche (Tag, freier Himmel) vs. unter einer Decke / im Gestein; Kerbin 0. */
    @GameTest(template = "empty3x3x3", batch = "space_gravity", timeoutTicks = 200)
    public static void radiationMunSurfaceVsCeiling(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel munLevel = overworld.getServer().getLevel(SpaceDimensions.MUN);
        boolean realMun = munLevel != null;
        ServerLevel level = realMun ? munLevel : overworld;

        // Werte: Mun 0.25 * Sonnenkraft Kerbins (1 AU) = 0.25 RAD/s; Kerbin mit 1 atm negativ -> 0
        float munRad = CelestialEnvironment.celestialRadiation(body("mun"), false);
        check(Math.abs(munRad - 0.25F * body("kerbin").getSunPower()) < 1e-4, "Mun-Sonnenstrahlung " + munRad);
        check(munRad > 0.2F && munRad < 0.3F, "Mun ~0.25 RAD/s, ist " + munRad);
        check(CelestialEnvironment.celestialRadiation(body("kerbin"), false) < 0, "Kerbin-Atmosphaere schirmt ab");
        check(CelestialEnvironment.celestialRadiation(body("kerbin"), true) > 0.2F, "Kerbin-Orbit ohne Atmosphaere");
        check(CelestialEnvironment.celestialRadiation(body("eve"), false) < 0, "Eve dichte Atmosphaere");
        check(CelestialEnvironment.celestialRadiation(body("moho"), false) > munRad, "Moho naeher an Kerbol");

        // Tag erzwingen (Ortszeit Mun bzw. Oberwelt), Himmelshelligkeit neu rechnen
        long oldTime = realMun ? CelestialTime.getLocalTime(level) : level.getDayTime();
        boolean day = false;
        double dayLength = realMun ? CelestialTime.getDayLength(body("mun")) : 24000D;
        for (int k = 0; k < 64 && !day; k++) {
            long t = (long) (dayLength * k / 64D);
            if (realMun) CelestialTime.setLocalTime(level, t); else level.setDayTime(t);
            level.updateSkyBrightness();
            day = level.getSkyDarken() == 0 && !level.isRaining();
        }
        check(day, "kein Tag gefunden");

        int x = realMun ? 40 : helper.absolutePos(BlockPos.ZERO).getX() + 40;
        int z = realMun ? 40 : helper.absolutePos(BlockPos.ZERO).getZ() + 40;
        level.getChunk(x >> 4, z >> 4);
        level.setChunkForced(x >> 4, z >> 4, true);
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos surface = new BlockPos(x, h, z);
        BlockPos rock = new BlockPos(x, Math.max(level.getMinBuildHeight() + 2, h - 12), z);

        if (realMun) {
            check(CelestialEnvironment.skyExposed(level, surface), "Mun-Oberflaeche unter freiem Himmel");
            float surf = SpaceApi.ambientRadiation(level, surface);
            check(Math.abs(surf - munRad) < 1e-5, "Mun-Oberflaeche " + surf + " statt " + munRad);
            check(SpaceApi.ambientRadiation(level, rock) == 0F, "im Gestein keine Sonnenstrahlung");

            // HBM-Kette: EntityEffectHandler kontaminiert mit max(Chunk, Sonne)/20 je Tick
            Pig pig = pig(level, surface);
            pig.tickCount = 1;
            float before = HbmLivingProps.getRadiation(pig) + HbmLivingProps.getRadEnv(pig);
            for (int i = 0; i < 10; i++) {
                pig.tickCount++;
                com.hbm_m.handler.EntityEffectHandler.onUpdate(pig);
            }
            check(HbmLivingProps.getRadiation(pig) + HbmLivingProps.getRadEnv(pig) > before, "Schwein auf der Mun bestrahlt");
        } else {
            check(SpaceApi.ambientRadiation(level, surface) == 0F, "Oberwelt (Kerbin): Atmosphaere schirmt ab");
            check(CelestialEnvironment.skyExposed(level, surface), "Oberwelt-Testort unter freiem Himmel");
        }

        // Decke 5x5 drei Bloecke ueber dem Kopf: Himmelslicht < 14 -> keine Strahlung (1 Block reicht im Original nicht)
        BlockPos roof = surface.above(3);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            level.setBlock(roof.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
        }
        final long restore = oldTime;
        helper.succeedWhen(() -> {
            check(!CelestialEnvironment.skyExposed(level, surface), "Decke schirmt Himmelslicht ab");
            check(SpaceApi.ambientRadiation(level, surface) == 0F, "unter der Decke keine Sonnenstrahlung");
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(roof.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 3);
            }
            level.setChunkForced(x >> 4, z >> 4, false);
            if (realMun) CelestialTime.setLocalTime(level, restore); else level.setDayTime(restore);
        });
    }

    @SuppressWarnings("unused")
    private static boolean isMun(Level level) {
        return level.dimension() == SpaceDimensions.MUN;
    }
}
