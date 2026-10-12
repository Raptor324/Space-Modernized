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

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.orbit.client.ItemRenderOrbitalStation.instance();
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
                return com.space_m.orbit.client.ItemRenderOrbitalStation.instance();
            }
        });
    }
    *///?}
}
