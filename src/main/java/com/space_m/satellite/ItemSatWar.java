package com.space_m.satellite;

/**
 * {@code sat_war} (Railkanone): wie {@link ItemSpaceSatellite}, im Inventar das Railkanonen-Modell (Original
 * {@code ItemRenderLibrary}-Eintrag, Item-Modell {@code builtin/entity}). Original-Klasse: ItemSatellite (sat_war)
 */
public class ItemSatWar extends ItemSpaceSatellite {

    public ItemSatWar(Properties properties) {
        super(properties);
    }

    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.satellite.client.ItemRenderSatWar.instance();
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
                return com.space_m.satellite.client.ItemRenderSatWar.instance();
            }
        });
    }
    *///?}
}
