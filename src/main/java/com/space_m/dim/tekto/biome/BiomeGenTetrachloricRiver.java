package com.space_m.dim.tekto.biome;

/** 1:1 {@code com.hbm.dim.tekto.biome.BiomeGenTetrachloricRiver} ("Tetrachloride River", {@code tektoRiverBiome} 90, Vinylsand). */
public class BiomeGenTetrachloricRiver extends BiomeGenBaseTekto {

    public static final int ID = 90;

    public BiomeGenTetrachloricRiver(int id) {
        super(id, "tekto_river");
        this.setBiomeName("Tetrachloride River");

        this.setHeight(new Height(-0.7F, 0.0F));
        // muh freezing point
        // Original vergleicht mit duna_rock/duna_sands (kommt auf Tekto nicht vor -> Oberflaeche bleibt Basalt)
        this.rock = () -> lookup("space_m:duna_rock");
        this.alt = () -> lookup("space_m:duna_sands");
    }
}
