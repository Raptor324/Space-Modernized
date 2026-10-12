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

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.machine.misc.client.ItemRenderMachines.instance();
            }
        });
    }
    //?} elif neoforge {
    /*@SuppressWarnings("removal")
    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.machine.misc.client.ItemRenderMachines.instance();
            }
        });
    }
    *///?}
}
