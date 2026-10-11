package com.space_m.dim.tekto.biome;

/** 1:1 {@code com.hbm.dim.tekto.biome.BiomeGenHalogenHills} ("Halogen Hills", {@code tektoHalogenHillBiome} 91, Tekto-Schlick). */
public class BiomeGenHalogenHills extends BiomeGenBaseTekto {

    public static final int ID = 91;

    public BiomeGenHalogenHills(int id) {
        super(id, "tekto_halogen_hills");
        this.setBiomeName("Halogen Hills");

        this.setHeight(new Height(0.525F, 0.4F));
        this.setTopBlock(() -> com.space_m.dim.tekto.TektoBlocks.RUBBER_SILT.get().defaultBlockState());
        this.setFillerBlock(() -> com.space_m.dim.tekto.TektoBlocks.RUBBER_SILT.get().defaultBlockState());
    }
}
