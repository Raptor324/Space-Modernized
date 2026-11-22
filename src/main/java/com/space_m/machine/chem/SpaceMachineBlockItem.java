package com.space_m.machine.chem;

import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.world.level.block.Block;

/**
 * Block-Item der R7-B-Maschinen: Mehrblock-Platzierung (HBM) und Inventar-Darstellung wie das Original
 * ({@code IItemRendererProvider.getRenderer()} = {@code ItemRenderBase}), gezeichnet von
 * {@code com.space_m.machine.chem.client.SpaceMachineItemRenderer} (Eintraege je Block in den Client-Klassen).
 */
public class SpaceMachineBlockItem extends MultiblockBlockItem {

    public SpaceMachineBlockItem(Block block, Properties props) {
        super(block, props);
    }
}
