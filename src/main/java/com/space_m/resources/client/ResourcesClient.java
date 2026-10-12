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
    }

    public static void addOreTooltip(ItemStack stack, List<Component> list) {
        if (!SpaceConfig.get().showOreLocations) return;
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return;
        OreLocations.addTooltip(stack.getItem(), list);
    }
}
