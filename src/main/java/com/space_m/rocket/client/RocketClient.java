package com.space_m.rocket.client;

import java.util.List;

import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.inventory.SpaceMenus;
import com.space_m.rocket.ItemRocketPart;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketPart;
import com.space_m.rocket.SpaceRocketItems;

import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * R5-A Client: Screen und BER der VAB, Item-Eigenschaft {@code space_m:body} der Ziel-Laufwerke, Space-Tooltipzeilen der
 * HBM-Raketenteile.
 */
public final class RocketClient {

    private RocketClient() {}

    /** Aus {@code SpaceClient.init()}. */
    public static void init() {
        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register((stack, lines, flag) -> addSpaceTooltip(stack, lines));
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> addSpaceTooltip(stack, lines));
        *///?}
    }

    /** Client-Welt fuer Tooltips (haelt {@code ClientLevel} aus gemeinsamen Klassen heraus). */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.level.Level clientLevel() {
        return net.minecraft.client.Minecraft.getInstance().level;
    }

    /** Aus {@code SpaceClient.setup()}. */
    public static void setup() {
        MenuRegistry.registerScreenFactory(SpaceMenus.ROCKET_ASSEMBLY.get(), GUIMachineRocketAssembly::new);
        BlockEntityRendererRegistry.register(SpaceBlockEntities.ROCKET_ASSEMBLY.get(), RenderRocketAssembly::new);
        // Original ItemVOTVdrive: Overlay-Symbol je Metadaten (Koerper)
        ItemPropertiesRegistry.register(SpaceRocketItems.FULL_DRIVE.get(), ResourceLocation.fromNamespaceAndPath("space_m", "body"),
                (stack, level, entity, seed) -> ItemVOTVdrive.getBody(stack).ordinal());
    }

    /**
     * Space-Fassung von {@code ItemCustomMissilePart.addInformation} fuer die HBM-Teile ({@code hbm_m:mp_*}): Raptors Tooltip
     * bleibt, angehaengt werden Schub, ISP und Masse (Triebwerke), Masse (Rumpfe, Sprengkoepfe).
     */
    private static void addSpaceTooltip(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty() || stack.getItem() instanceof ItemRocketPart) return;
        if (!"hbm_m".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) return;
        RocketPart part = RocketPart.getPart(stack);
        if (part == null) return;
        if (part.warhead == RocketPart.WarheadType.SATELLITE) return; // Satelliten: Tooltip gehoert R6

        int at = Math.max(1, lines.size());
        switch (part.type) {
            case THRUSTER -> {
                lines.add(at++, TooltipLine.line("item.missile.part.thrust", part.getThrust() + "N"));
                lines.add(at++, TooltipLine.line("item.missile.part.isp", part.getISP() + "s"));
                lines.add(at, TooltipLine.line("item.missile.part.mass", part.mass + "kg"));
            }
            case FUSELAGE, WARHEAD -> lines.add(at, TooltipLine.line("item.missile.part.mass", part.mass + "kg"));
            default -> { }
        }
    }

    private static final class TooltipLine {
        static Component line(String key, String value) {
            return Component.literal("").append(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.BOLD))
                    .append(Component.literal(": ").withStyle(net.minecraft.ChatFormatting.BOLD))
                    .append(Component.literal(value).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
