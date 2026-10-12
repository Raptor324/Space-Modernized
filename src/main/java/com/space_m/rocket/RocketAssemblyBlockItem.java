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

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.rocket.client.ItemRenderRocketAssembly.instance();
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
                return com.space_m.rocket.client.ItemRenderRocketAssembly.instance();
            }
        });
    }
    *///?}
}
