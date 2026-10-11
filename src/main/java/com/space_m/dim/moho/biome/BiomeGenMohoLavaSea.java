package com.space_m.dim.moho.biome;

import com.space_m.dim.moho.MohoBlocks;

/**
 * 1:1 {@code com.hbm.dim.moho.biome.BiomeGenMohoLavaSea} ("Moho Lava Sea", Biom {@code space_m:moho_lava_sea}): Hoehe (-0.8, 0.01), Lavameer bis y 63 mit Gesteinsplatten ({@code MapGenVNoise}); Oberflaeche und
 * Fuellung {@code moho_regolith} ("thiccer regolith").
 */
public class BiomeGenMohoLavaSea extends BiomeGenBaseMoho {

    public BiomeGenMohoLavaSea(int id) {
        super(id, "moho_lava_sea");
        this.setBiomeName("Moho Lava Sea");

        this.setHeight(new Height(-0.8F, 0.01F));

        this.setTopBlock(() -> MohoBlocks.MOHO_REGOLITH.get().defaultBlockState());
        this.setFillerBlock(() -> MohoBlocks.MOHO_REGOLITH.get().defaultBlockState()); // thiccer regolith due to uhhhhhh...................
    }
}
