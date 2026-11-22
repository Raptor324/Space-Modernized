package com.space_m.orbit;

import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.world.level.block.Block;

/**
 * Block-Item der Stationsports: Mehrblock-Platzierung (HBM) und Inventar-Darstellung wie Original
 * {@code RenderOrbitalStation} als {@code IItemRendererProvider} (ganzes {@code docking_port.obj}).
 */
public class OrbitalStationBlockItem extends MultiblockBlockItem {

    public OrbitalStationBlockItem(Block block, Properties props) {
        super(block, props);
    }
}
