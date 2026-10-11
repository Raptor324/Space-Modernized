package com.space_m.dim.laythe;

import com.space_m.dim.SpaceWorldgen;

import dev.architectury.registry.registries.RegistrySupplier;

/**
 * Weltgen-Features von Laythe (Dekorator-Teile von {@code BiomeDecoratorCelestial}); registriert im gemeinsamen
 * {@link SpaceWorldgen#FEATURES} (vor dessen {@code register()}).
 */
public final class LaytheFeatures {

    private LaytheFeatures() {}

    /** {@code waterPlantsPerChunk} -> {@link WorldGenWaterPlant}. */
    public static final RegistrySupplier<WorldGenWaterPlant> WATER_PLANTS = SpaceWorldgen.FEATURES.register("water_plants", WorldGenWaterPlant::new);
    /** {@code coralPerChunk} -> {@link WorldGenWaterCoral}. */
    public static final RegistrySupplier<WorldGenWaterCoral> WATER_CORAL = SpaceWorldgen.FEATURES.register("water_coral", WorldGenWaterCoral::new);
    /** {@code BiomeDecorator.generateOres()} (Vanilla-Erze in Stein). */
    public static final RegistrySupplier<VanillaBiomeOres> VANILLA_BIOME_ORES = SpaceWorldgen.FEATURES.register("vanilla_biome_ores", VanillaBiomeOres::new);

    public static void init() {
    }
}
