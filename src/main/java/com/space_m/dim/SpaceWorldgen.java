package com.space_m.dim;

//? if < 1.21.1 {
import com.mojang.serialization.Codec;
//?} else {
/*import com.mojang.serialization.MapCodec;
*///?}
import com.space_m.SpaceMod;
import com.space_m.dim.feature.CelestialOreFeature;
import com.space_m.dim.feature.DepthDepositFeature;
import com.space_m.dim.feature.KeyholeFeature;
import com.space_m.dim.moon.WorldProviderMoon;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Dimensions-Framework des Addons (R1-B): Registrierung von Generator, Biomquelle und Weltgen-Features sowie
 * Anmeldung der Koerper-Generatoren. Die Dimensionen selbst sind Datapack-Eintraege:
 * <pre>
 *   data/space_m/dimension_type/&lt;koerper&gt;.json   Hoehe 0-255 (Original), Licht, effects = space_m:&lt;koerper&gt;
 *   data/space_m/dimension/&lt;koerper&gt;.json        generator space_m:celestial + biome_source space_m:celestial
 *   data/space_m/worldgen/biome/&lt;biom&gt;.json       Temperatur/Niederschlag/Spawns + Features (Erze, Dekorator)
 *   data/space_m/worldgen/configured_feature|placed_feature/&lt;koerper&gt;/...   Erze/Lagerstaetten 1:1
 * </pre>
 * Original-Gegenstuecke: {@code WorldProviderCelestial}/{@code WorldProvider<Koerper>} (Dimension + Fabriken,
 * {@link CelestialGenerators}), {@code ChunkProviderCelestial} ({@link CelestialChunkGenerator}),
 * {@code WorldChunkManagerCelestial} ({@link CelestialBiomeSource}), {@code BiomeDecoratorCelestial} und
 * {@code WorldGeneratorCelestial}/{@code WorldGenerator<Koerper>} (Features).
 */
public final class SpaceWorldgen {

    private SpaceWorldgen() {}

    //? if < 1.21.1 {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.CHUNK_GENERATOR);
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(SpaceMod.MOD_ID, Registries.BIOME_SOURCE);
    //?} else {
    /*public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.CHUNK_GENERATOR);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(SpaceMod.MOD_ID, Registries.BIOME_SOURCE);
    *///?}
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(SpaceMod.MOD_ID, Registries.FEATURE);

    static {
        CHUNK_GENERATORS.register("celestial", () -> CelestialChunkGenerator.CODEC);
        BIOME_SOURCES.register("celestial", () -> CelestialBiomeSource.CODEC);
    }

    /** Original {@code WorldGeneratorCelestial.generateNTMOres/generateVanillaOres} + {@code WorldGenerator<Koerper>}-Erze. */
    public static final RegistrySupplier<CelestialOreFeature> CELESTIAL_ORE = FEATURES.register("celestial_ore", CelestialOreFeature::new);
    /** Original {@code DepthDeposit.generateCondition} (Tiefenerze). */
    public static final RegistrySupplier<DepthDepositFeature> DEPTH_DEPOSIT = FEATURES.register("depth_deposit", DepthDepositFeature::new);
    /** Original Schluesselloch-Stein in {@code generateNTMOres}. */
    public static final RegistrySupplier<KeyholeFeature> KEYHOLE = FEATURES.register("keyhole", KeyholeFeature::new);
    /** Original {@code BiomeDecoratorCelestial.genDecorations} (Lava-Quellen, Seen). */
    public static final RegistrySupplier<BiomeDecoratorCelestial> CELESTIAL_DECORATOR = FEATURES.register("celestial_decorator", BiomeDecoratorCelestial::new);
    /** R3-1: Original {@code OreLayer3D} (Erzlagen je Koerper/global, {@code MainRegistry}). */
    public static final RegistrySupplier<com.space_m.dim.feature.OreLayer3DFeature> ORE_LAYER_3D = FEATURES.register("ore_layer_3d", com.space_m.dim.feature.OreLayer3DFeature::new);
    /** R3-1: Original {@code OreCave} (Schwefel-/Asbesthoehlen je Koerper, {@code MainRegistry}). */
    public static final RegistrySupplier<com.space_m.dim.feature.OreCaveFeature> ORE_CAVE = FEATURES.register("ore_cave", com.space_m.dim.feature.OreCaveFeature::new);

    /** Einmal aus {@code SpaceRegistry.init()} (vor {@code SpaceBlocks.init()}: meldet auch die Koerper-Bloecke an). */
    public static void init() {
        // Koerper (Bloecke + Generator-Fabriken). Neue Koerper (R3) hier je eine Zeile.
        WorldProviderMoon.init();
        com.space_m.dim.eve.WorldProviderEve.init(); // R3-3
        com.space_m.dim.moho.WorldProviderMoho.init(); // R3-3
        com.space_m.dim.minmus.WorldProviderMinmus.init(); // R3-1
        com.space_m.dim.ike.WorldProviderIke.init(); // R3-1
        com.space_m.dim.duna.WorldProviderDuna.init(); // R3-2
        com.space_m.dim.dres.WorldProviderDres.init(); // R3-2
        com.space_m.dim.laythe.WorldProviderLaythe.init(); // R3-4
        com.space_m.dim.tekto.WorldProviderTekto.init(); // R3-4
        com.space_m.dim.thatmo.WorldProviderThatmo.init(); // R3-4

        CHUNK_GENERATORS.register();
        BIOME_SOURCES.register();
        FEATURES.register();
        CelestialSeed.init();
    }
}
