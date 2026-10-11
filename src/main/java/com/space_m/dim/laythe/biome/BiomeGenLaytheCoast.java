package com.space_m.dim.laythe.biome;

/**
 * 1:1 {@code com.hbm.dim.laythe.biome.BiomeGenLaytheCoast} ("Laythe Reef", {@code laytheCoastBiome} 94):
 * {@code waterPlantsPerChunk 16}, {@code coralPerChunk 32}. Scuttlecrab 4/1/3 folgt mit den Mobs (R8).
 */
public class BiomeGenLaytheCoast extends BiomeGenBaseLaythe {

    /** {@code SpaceConfig.laytheCoastBiome}. */
    public static final int ID = 94;

    public BiomeGenLaytheCoast(int id) {
        super(id, "laythe_coast");
        this.setBiomeName("Laythe Reef");

        this.setHeight(new Height(-0.4F, 0.01F));
        this.setTemperatureRainfall(0.2F, 0.2F);
        this.waterPlantsPerChunk = 16;
        this.coralPerChunk = 32;
    }
}
