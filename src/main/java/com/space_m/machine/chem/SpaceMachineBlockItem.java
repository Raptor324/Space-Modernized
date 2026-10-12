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

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.machine.chem.client.SpaceMachineItemRenderer.instance();
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
                return com.space_m.machine.chem.client.SpaceMachineItemRenderer.instance();
            }
        });
    }
    *///?}
}
