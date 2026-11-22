package com.space_m.rocket;

import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.world.level.block.Block;

/**
 * Block-Item der VAB: Mehrblock-Platzierung (HBM) und Inventar-Darstellung wie Original {@code RenderRocketAssembly}
 * als {@code IItemRendererProvider} (Teil "Base" des OBJ).
 */
public class RocketAssemblyBlockItem extends MultiblockBlockItem {

    public RocketAssemblyBlockItem(Block block, Properties props) {
        super(block, props);
    }
}
