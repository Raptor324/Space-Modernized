package com.space_m.dim.tekto.biome;

/** 1:1 {@code com.hbm.dim.tekto.biome.BiomeGenForest} ("Tekto Forest", {@code tektoForestBiome} 89, Gummigras ueber Tekto-Schlick). */
public class BiomeGenForest extends BiomeGenBaseTekto {

    public static final int ID = 89;

    public BiomeGenForest(int id) {
        super(id, "tekto_forest");
        this.setBiomeName("Tekto Forest");

        this.setHeight(new Height(0.425F, 0.3F));
        this.setTopBlock(() -> com.space_m.dim.tekto.TektoBlocks.RUBBER_GRASS.get().defaultBlockState());
        this.setFillerBlock(() -> com.space_m.dim.tekto.TektoBlocks.RUBBER_SILT.get().defaultBlockState());
    }
}
