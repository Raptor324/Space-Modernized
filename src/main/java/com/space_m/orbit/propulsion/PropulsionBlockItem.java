package com.space_m.orbit.propulsion;

import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.world.level.block.Block;

/**
 * Block-Item der Triebwerke und des Stationscomputers: Mehrblock-Platzierung (HBM) und Inventar-Darstellung wie die
 * Original-{@code IItemRendererProvider} ({@code RenderHTR3/HTRF4/HTRNeo/XenonThruster/OrbitalComputer.getRenderer()}).
 */
public class PropulsionBlockItem extends MultiblockBlockItem {

    public PropulsionBlockItem(Block block, Properties props) {
        super(block, props);
    }
}
