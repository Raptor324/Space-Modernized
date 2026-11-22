package com.space_m.machine.misc;

import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.world.level.block.Block;

/**
 * Block-Item der R7-A-Mehrblockmaschinen (Dyson-System, Solar, Stardar, Radiator): HBM-Mehrblock-Platzierung und die
 * Inventar-Darstellung der Original-{@code IItemRendererProvider.getRenderer()} ueber
 * {@link com.space_m.machine.misc.client.ItemRenderMachines} (je Block angemeldet in den Client-Klassen).
 */
public class MachineBlockItem extends MultiblockBlockItem {

    public MachineBlockItem(Block block, Properties props) {
        super(block, props);
    }
}
