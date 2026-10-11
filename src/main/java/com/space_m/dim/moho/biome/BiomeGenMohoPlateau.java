package com.space_m.dim.moho.biome;

import com.space_m.dim.moho.MohoBlocks;

/**
 * 1:1 {@code com.hbm.dim.moho.biome.BiomeGenMohoPlateau} ("Moho Plateau", Biom {@code space_m:moho_plateau}): Hoehe (0.255, 0.432), Stufenplateaus ({@code MapGenPlateau}); Oberflaeche und
 * Fuellung {@code moho_regolith} ("thiccer regolith").
 */
public class BiomeGenMohoPlateau extends BiomeGenBaseMoho {

    public BiomeGenMohoPlateau(int id) {
        super(id, "moho_plateau");
        this.setBiomeName("Moho Plateau");

        this.setHeight(new Height(0.255F, 0.432F));

        this.setTopBlock(() -> MohoBlocks.MOHO_REGOLITH.get().defaultBlockState());
        this.setFillerBlock(() -> MohoBlocks.MOHO_REGOLITH.get().defaultBlockState()); // thiccer regolith due to uhhhhhh...................
    }
}
