package com.space_m.dim.moho.biome;

import com.space_m.dim.moho.MohoBlocks;

/**
 * 1:1 {@code com.hbm.dim.moho.biome.BiomeGenMohoCrag} ("Moho Crag", Biom {@code space_m:moho_crag}): Hoehe (0.275, 0.666); Oberflaeche und
 * Fuellung {@code moho_regolith} ("thiccer regolith").
 */
public class BiomeGenMohoCrag extends BiomeGenBaseMoho {

    public BiomeGenMohoCrag(int id) {
        super(id, "moho_crag");
        this.setBiomeName("Moho Crag");

        this.setHeight(new Height(0.275F, 0.666F));

        this.setTopBlock(() -> MohoBlocks.MOHO_REGOLITH.get().defaultBlockState());
        this.setFillerBlock(() -> MohoBlocks.MOHO_REGOLITH.get().defaultBlockState()); // thiccer regolith due to uhhhhhh...................
    }
}
