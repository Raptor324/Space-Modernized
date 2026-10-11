package com.space_m.dim.minmus;

/**
 * 1:1 {@code com.hbm.dim.minmus.biome.BiomeGenMinmusBasin} ("Minmus Basins", Biom {@code space_m:minmus_basins}):
 * Hoehe (-1.0, 0.02), Oberflaeche {@code minmus_smooth} (die glatten Minz-"Meere"), Fuellung Regolith.
 * Oberflaeche nach 1.7.10 {@code genBiomeTerrain} (ersetzt nur {@code Blocks.stone} - auf Minmus wirkungslos,
 * genau wie im Original).
 */
public class BiomeGenMinmusBasin extends BiomeGenBaseMinmus {

    public BiomeGenMinmusBasin(int id) {
        super(id, "minmus_basins");
        this.setBiomeName("Minmus Basins");

        this.setHeight(new Height(-1F, 0.02F));
        this.setTopBlock(() -> MinmusBlocks.MINMUS_SMOOTH.get().defaultBlockState());
        this.setFillerBlock(() -> MinmusBlocks.MINMUS_REGOLITH.get().defaultBlockState());
    }

}
