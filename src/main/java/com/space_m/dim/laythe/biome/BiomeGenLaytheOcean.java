package com.space_m.dim.laythe.biome;

/**
 * 1:1 {@code com.hbm.dim.laythe.biome.BiomeGenLaytheOcean} ("Sagan Sea", {@code laytheOceanBiome} 96).
 * Wasserkreaturen ScrapFish 2/1/4, SifterEel 1/1/4, DepthSquid 1/1/4 folgen mit den Mobs (R8).
 */
public class BiomeGenLaytheOcean extends BiomeGenBaseLaythe {

    /** {@code SpaceConfig.laytheOceanBiome}. */
    public static final int ID = 96;

    public BiomeGenLaytheOcean(int id) {
        super(id, "laythe_ocean");
        this.setBiomeName("Sagan Sea");

        this.setHeight(new Height(-1.8F, 0.24F));
        this.setTemperatureRainfall(0.2F, 0.2F);
    }
}
