package com.space_m.deco;

import com.hbm_m.block.generic.BlockNoSpawn;

/**
 * 1:1 {@code com.hbm.blocks.BlockAstroturf} ({@code astroturf}, "Astroturf"): {@code BlockNoSpawn} - keine
 * Kreaturen-Spawns, roter Tooltip {@code tile.nospawn}. Oben {@code astroturf_top}, Seiten {@code astroturf_side},
 * unten Vanilla-Erde (Blockmodell {@code space_m:block/astroturf}). Keine Biomfaerbung (Original ohne colorMultiplier).
 */
// Original-Klasse: BlockAstroturf
public class BlockAstroturf extends BlockNoSpawn {

    public BlockAstroturf(Properties properties) {
        super(properties);
    }
}
