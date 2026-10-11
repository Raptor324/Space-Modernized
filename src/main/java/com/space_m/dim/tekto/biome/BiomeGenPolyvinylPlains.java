package com.space_m.dim.tekto.biome;

/** 1:1 {@code com.hbm.dim.tekto.biome.BiomeGenPolyvinylPlains} ("Polyvinyl Plains", {@code tektoPolyvinylBiome} 92, Gummigras ueber Tekto-Schlick). */
public class BiomeGenPolyvinylPlains extends BiomeGenBaseTekto {

    public static final int ID = 92;

    public BiomeGenPolyvinylPlains(int id) {
        super(id, "tekto_polyvinyl_plains");
        this.setBiomeName("Polyvinyl Plains");

        this.setHeight(new Height(0.125F, 0.05F));
        this.setTopBlock(() -> com.space_m.dim.tekto.TektoBlocks.RUBBER_GRASS.get().defaultBlockState());
        this.setFillerBlock(() -> com.space_m.dim.tekto.TektoBlocks.RUBBER_SILT.get().defaultBlockState());
    }
}
