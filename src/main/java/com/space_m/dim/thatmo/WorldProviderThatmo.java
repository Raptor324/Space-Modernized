package com.space_m.dim.thatmo;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.SpaceWorldgen;
import com.space_m.dim.WorldChunkManagerCelestial;

import dev.architectury.registry.registries.RegistrySupplier;

/**
 * 1:1 {@code com.hbm.dim.thatmo.WorldProviderThatmo} (Dimension "Thatmo", {@code space_m:thatmo}, Original-ID 413025):
 * <ul>
 *   <li>{@code registerWorldChunkManager} = {@code WorldChunkManagerHell(new BiomeGenThatmo(thatmoBiome))} -> ein Biom.</li>
 *   <li>{@code createChunkGenerator} = {@link ChunkProviderThatmo}; {@code getStone} = {@code sellafield_slaked}.</li>
 *   <li>Schlachtfeld ({@code updateWeather} mit {@code CBT_BATTLEFIELD}): Client-Teil (Blitze, Schild, Meteore,
 *       Himmel-/Nebelfarbe, Sonnenhelligkeit) liegt bei R1-C ({@code ThatmoSkyRenderer.State}); der Server-Teil
 *       (alle 8 s ein Duesen-Teppichbomber {@code EntityBomber.statFacCarpetJet} 40 Bloecke um jeden Spieler)
 *       braucht den Bombertyp 9 aus NTM Space und folgt mit dem Krieg (R8).</li>
 * </ul>
 * Thatmo ist im Original nicht anfliegbar (Koerper ohne Dimension); die Dimension existiert aber und ist per
 * Befehl erreichbar - so auch hier ({@code canLand = false}, R1-A).
 */
public final class WorldProviderThatmo {

    private WorldProviderThatmo() {}

    /** BiomeDecorator (Vanilla) - Pilze. */
    public static final RegistrySupplier<VanillaMushrooms> VANILLA_MUSHROOMS = SpaceWorldgen.FEATURES.register("vanilla_mushrooms", VanillaMushrooms::new);

    public static void init() {
        ThatmoBlocks.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(ThatmoBlocks.BODY,
                seed -> WorldChunkManagerCelestial.fixed(BiomeGenThatmo.biome, seed),
                ChunkProviderThatmo::new,
                () -> com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(),
                64));
        // WorldGeneratorThatmo wird im Original nie angemeldet (fehlt in PlanetGen) - hier ebenso nicht.
    }
}
