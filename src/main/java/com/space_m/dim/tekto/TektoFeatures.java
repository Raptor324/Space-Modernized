package com.space_m.dim.tekto;

import com.space_m.dim.SpaceWorldgen;

import dev.architectury.registry.registries.RegistrySupplier;

/** Weltgen-Features von Tekto; registriert im gemeinsamen {@link SpaceWorldgen#FEATURES} (vor dessen {@code register()}). */
public final class TektoFeatures {

    private TektoFeatures() {}

    /** {@code rubberPlantsPerChunk} -> {@link WorldGenRubberPlant}. */
    public static final RegistrySupplier<WorldGenRubberPlant> RUBBER_PLANTS = SpaceWorldgen.FEATURES.register("rubber_plants", WorldGenRubberPlant::new);
    /** {@link WorldGeneratorTekto#generate} (Kobalt, Geysire, Baeume). */
    public static final RegistrySupplier<WorldGeneratorTekto> TEKTO_DECORATOR = SpaceWorldgen.FEATURES.register("tekto_decorator", WorldGeneratorTekto::new);

    public static void init() {
    }
}
