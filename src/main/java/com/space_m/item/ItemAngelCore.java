package com.space_m.item;

/**
 * {@code core_angel} (Original {@code ItemBattery(16 000 000, 16 000 000, 800 000)}): HBM-Batterie, dargestellt als Rumpf des
 * Balefire-Engels (Original {@code ItemRenderAngelCore}), Item-Modell {@code builtin/entity}.
 */
public class ItemAngelCore extends com.hbm_m.item.fekal_electric.ModBatteryItem {

    public ItemAngelCore(Properties properties, long capacity, long maxReceive, long maxExtract) {
        super(properties, capacity, maxReceive, maxExtract);
    }

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.client.item.ItemRenderSpaceBase.instance();
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
                return com.space_m.client.item.ItemRenderSpaceBase.instance();
            }
        });
    }
    *///?}
}
