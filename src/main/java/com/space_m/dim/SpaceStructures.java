package com.space_m.dim;

import java.util.function.Predicate;

import org.slf4j.Logger;

import com.hbm_m.api.space.SpaceResources;
import com.hbm_m.world.gen.LegacyBiome;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.dim.laythe.WorldGeneratorLaythe;

import dev.architectury.event.events.common.LifecycleEvent;

/**
 * Systemweite Teile von {@code com.hbm.dim.WorldGeneratorCelestial} und {@code TileEntityFurnaceBrick.burnSpeed}:
 * <ul>
 * <li>{@code meteor_dungeon} fuer alle Space-Dimensionen ({@code PlanetGen.getSpaceDimensions()} = alle Koerper ausser
 * Orbit, auch Thatmo), angemeldet vor den Koerper-Strukturen (Original: Generator {@code WorldGeneratorCelestial} wird
 * vor den Koerper-Generatoren gebaut - gleiche Reihenfolge der Gewichtsliste). Bedingung wie im Original
 * {@code biome.rootHeight >= 0}, fuer Space-Biome mit der Original-Grundhoehe ({@link BiomeGenBaseCelestial}).</li>
 * <li>Ziegelofen-Geschwindigkeit 2 fuer das Planetengestein (Wunsch W-R3a-2/W-R3c-1, HBM-API
 * {@link SpaceResources#registerBrickFurnaceSpeed}).</li>
 * </ul>
 */
public final class SpaceStructures {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original-IDs Mun, Duna, Ike, Eve, Dres, Moho, Minmus, Laythe, Tekto, Thatmo (Reihenfolge PlanetGen.init). */
    public static final int[] SPACE_DIMENSIONS = { 413015, 413016, 413017, 413018, 413019, 413020, 413021, 413022, 413024, 413025 };

    private SpaceStructures() {}

    /** Aus {@link SpaceWorldgen#init()} vor den Koerpern (SETUP-Reihenfolge = Anmeldereihenfolge). */
    public static void init() {
        LifecycleEvent.SETUP.register(() -> {
            registerMeteorDungeons();
            registerBrickFurnace();
        });
    }

    private static void registerMeteorDungeons() {
        try {
            SpawnCondition meteor = NBTStructure.getStructure("meteor_dungeon");
            if (meteor == null) {
                LOGGER.warn("[space_m] HBM meteor_dungeon not found, no meteor dungeons on the celestial bodies");
                return;
            }
            Predicate<LegacyBiome> old = meteor.canSpawn;
            meteor.canSpawn = biome -> {
                BiomeGenBaseCelestial c = WorldGeneratorLaythe.celestial(biome);
                if (c != null) return c.rootHeight >= 0;
                return old == null || old.test(biome);
            };
            NBTStructure.registerStructure(meteor, SPACE_DIMENSIONS);
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] meteor_dungeon registration failed", e);
        }
    }

    private static void registerBrickFurnace() {
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.duna.DunaBlocks.DUNA_COBBLE, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.dres.DresBlocks.DRES_ROCK, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.ike.IkeBlocks.IKE_REGOLITH, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.eve.EveBlocks.EVE_ROCK, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.moho.MohoBlocks.MOHO_REGOLITH, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.moon.MunBlocks.MOON_ROCK, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.minmus.MinmusBlocks.MINMUS_REGOLITH, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.duna.DunaBlocks.DUNA_SANDS, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.laythe.LaytheBlocks.LAYTHE_SILT, 2);
        SpaceResources.registerBrickFurnaceSpeed(com.space_m.dim.eve.EveBlocks.EVE_SILT, 2);
        SpaceResources.registerBrickFurnaceSpeed(() -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", "moon_turf")), 2);
    }
}
