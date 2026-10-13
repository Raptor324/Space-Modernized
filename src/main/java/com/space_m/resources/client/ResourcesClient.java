package com.space_m.resources.client;

import java.util.List;

import com.space_m.config.SpaceConfig;
import com.space_m.resources.OreLocations;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Client-Teil der Planeten-Ressourcen: Erz-Tooltip 1:1 NTM Space {@code BlockOre.addInformation} (Space-Erze und
 * Koerper-Varianten) bzw. {@code ModEventHandlerClient} Abschnitt {@code /// ORES ///} (Vanilla-Erze ueber
 * {@code BlockOre.vanillaMap}, Kohle nur Kerbin). Schalter {@code SpaceConfig.showOreLocations}.
 */
public final class ResourcesClient {

    private ResourcesClient() {}

    private static boolean initialized;

    public static void init() {
        if (initialized) return;
        initialized = true;

        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register((stack, lines, flag) -> addOreTooltip(stack, lines));
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> addOreTooltip(stack, lines));
        *///?}

        // runData hat keinen Minecraft-Client - Architectury wuerde dort an getItemColors() mit NPE scheitern
        if (net.minecraft.client.Minecraft.getInstance() == null) return;

        // Bedrock-Erzstufen: Ebene 0 mit der Grade-Toenung (Original getColorFromItemStack, pass 0), Overlays ungetoent
        java.util.List<dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.item.Item>> ores = com.space_m.resources.CelestialBedrockOreItems.all();
        @SuppressWarnings("unchecked")
        java.util.function.Supplier<net.minecraft.world.item.Item>[] arr = ores.toArray(new java.util.function.Supplier[0]);
        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> {
            if (tintIndex != 0 || !(stack.getItem() instanceof com.space_m.resources.ItemCelestialBedrockOre ore)) return 0xFFFFFFFF;
            return 0xFF000000 | ore.getGrade().tint;
        }, arr);
    }

    public static void addOreTooltip(ItemStack stack, List<Component> list) {
        if (!SpaceConfig.get().showOreLocations) return;
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return;
        OreLocations.addTooltip(stack.getItem(), list);
    }
}
