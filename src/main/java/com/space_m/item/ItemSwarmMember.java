package com.space_m.item;

/**
 * {@code swarm_member} (Dyson-Schwarmmitglied): einfacher Gegenstand, im Inventar/in der Hand als 3D-Modell
 * ({@code dyson_swarm_satellite.obj}, Original {@code ItemRenderLibrary}), Item-Modell {@code builtin/entity}.
 */
public class ItemSwarmMember extends net.minecraft.world.item.Item {

    public ItemSwarmMember(Properties properties) {
        super(properties);
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
