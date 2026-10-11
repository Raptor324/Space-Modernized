package com.space_m.dim.laythe.biome;

/** 1:1 {@code com.hbm.dim.laythe.biome.BiomeGenLaytheIslands} ("Laythe Islands", {@code laytheBiome} 97). */
public class BiomeGenLaytheIslands extends BiomeGenBaseLaythe {

    /** {@code SpaceConfig.laytheBiome}. */
    public static final int ID = 97;

    public BiomeGenLaytheIslands(int id) {
        super(id, "laythe_islands");
        this.setBiomeName("Laythe Islands");
        this.waterColorMultiplier = 0x5b209a;

        this.setHeight(new Height(0.256F, 0.05F));
        this.setTemperatureRainfall(0.2F, 0.2F);
    }
}
