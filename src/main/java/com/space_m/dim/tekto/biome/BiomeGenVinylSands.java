package com.space_m.dim.tekto.biome;

/** 1:1 {@code com.hbm.dim.tekto.biome.BiomeGenVinylSands} ("Vinyl Desert", {@code tektoVinylIslandBiome} 88, Vinylsand). */
public class BiomeGenVinylSands extends BiomeGenBaseTekto {

    public static final int ID = 88;

    public BiomeGenVinylSands(int id) {
        super(id, "tekto_vinyl_sands");
        this.setBiomeName("Vinyl Desert");

        this.setHeight(new Height(0.425F, 0.3F));
    }
}
