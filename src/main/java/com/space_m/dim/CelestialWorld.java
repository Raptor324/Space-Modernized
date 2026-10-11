package com.space_m.dim;

/**
 * Was die portierten 1.7.10-Generatoren vom {@code World}-Objekt brauchen: Seed und Biome
 * ({@code world.getSeed()}, {@code world.getBiomeGenForCoords}, {@code getWorldChunkManager().getBiomesForGeneration}).
 * Waehrend der Generierung gibt es in 1.20 noch keine Welt; die Implementierung ist der
 * {@link WorldChunkManagerCelestial} des Koerpers (je Thread eine Instanz).
 */
public interface CelestialWorld {

    /** {@code world.getSeed()}. */
    long getSeed();

    /** {@code world.getBiomeGenForCoords(x, z)} (Detailschicht, Blockaufloesung). */
    BiomeGenBaseCelestial getBiomeGenForCoords(int x, int z);

    /** {@code getWorldChunkManager().getBiomesForGeneration(...)} (grobe Schicht, 1/4 Aufloesung). */
    BiomeGenBaseCelestial[] getBiomesForGeneration(BiomeGenBaseCelestial[] biomes, int x, int z, int width, int length);

    /** {@code getWorldChunkManager().loadBlockGeneratorData(...)} (Detailschicht, Blockaufloesung). */
    BiomeGenBaseCelestial[] loadBlockGeneratorData(BiomeGenBaseCelestial[] biomes, int x, int z, int width, int length);
}
