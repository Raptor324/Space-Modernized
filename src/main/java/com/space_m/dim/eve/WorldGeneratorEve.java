package com.space_m.dim.eve;

import java.util.Random;

import org.slf4j.Logger;

import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.mojang.logging.LogUtils;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.SpaceWorldgen;
import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.eve.genlayer.WorldGenElectricVolcano;
import com.space_m.dim.eve.genlayer.WorldGenEveSpike;
import com.space_m.dim.legacy.LegacyWorld;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1:1 {@code com.hbm.dim.eve.WorldGeneratorEve}.
 *
 * <p><b>Erze</b> ({@code generateEve}): Datapack-Features in allen Eve-Biomen ({@code placed_feature/eve/ore_niobium|
 * ore_iodine|ore_schrabidium}), Werte 1:1: Niob 12x8 y1-33, Iod 8x4 y5-52, Schrabidium 1x4 y1-16 - im Eve-Gestein.
 *
 * <p><b>Oberflaeche</b> = dieses Feature ({@code space_m:eve_surface}, danach im Biom): an einer Zufallsstelle des
 * Chunks eine Eve-Spitze, wenn dort Seismische Ebene ist, und mit 1/100 (x 1/10 im Generator) ein Entladungsvulkan.
 * Biom wie im Original blockgenau aus der Detailschicht ({@code world.getBiomeGenForCoords}).
 *
 * <p><b>Strukturen:</b> {@code NBTStructure.registerNullWeight(eveDimension, 24)} unter der Original-ID 413018 (wirkt,
 * sobald HBM Strukturen je Dimension erzeugt, Wunsch W-B1). {@code BlockOre.addValidBody} (Tooltip) folgt mit R8.
 */
public class WorldGeneratorEve extends Feature<NoneFeatureConfiguration> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.eveDimension}. */
    public static final int EVE_DIMENSION_ID = 413018;

    public static final RegistrySupplier<WorldGeneratorEve> EVE_SURFACE = SpaceWorldgen.FEATURES.register("eve_surface", WorldGeneratorEve::new);

    private WorldGenElectricVolcano volcano;

    public WorldGeneratorEve() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static void init() {
        LifecycleEvent.SETUP.register(() -> {
            try {
                NBTStructure.registerNullWeight(EVE_DIMENSION_ID, 24);
            } catch (RuntimeException e) {
                LOGGER.error("[space_m] Eve structure registration failed", e);
            }
        });
    }

    private WorldGenElectricVolcano volcano() {
        WorldGenElectricVolcano v = volcano;
        if (v == null) volcano = v = new WorldGenElectricVolcano(30, 22, EveBlocks.EVE_SILT.get().defaultBlockState(), EveBlocks.EVE_ROCK.get().defaultBlockState());
        return v;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        Random rand = LegacyWorld.random(ctx.random());
        int i = ctx.origin().getX() & ~15;
        int j = ctx.origin().getZ() & ~15;

        int x = i + rand.nextInt(16) + 8;
        int z = j + rand.nextInt(16) + 8;
        int y = L.getHeightValue(world, x, z);

        BiomeGenBaseCelestial biome = CelestialGenerators.state(EveBlocks.BODY).chunkManager.getBiomeGenForCoords(x, z);
        if (biome == BiomeGenBaseEve.eveSeismicPlains) {
            new WorldGenEveSpike().generate(world, rand, x, y, z);
        }

        // TODO: these span multiple chunks, fix this cascade!
        if (rand.nextInt(100) == 0) {
            volcano().generate(world, rand, x, y, z);
        }
        return true;
    }
}
