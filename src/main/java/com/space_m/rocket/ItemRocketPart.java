package com.space_m.rocket;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.space_m.rocket.RocketPart.FuelType;
import com.space_m.rocket.RocketPart.PartSize;
import com.space_m.rocket.RocketPart.WarheadType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Die Space-neuen Raketenteile ({@code mp_thruster_20_methalox} ..., {@code rp_f_20_*}, {@code rp_l_20}, {@code rp_c_20},
 * {@code rp_sc_20}, {@code rp_pod_20}), 1:1 NTM Space {@code ItemCustomMissilePart} (Space-Fassung mit Masse/Schub/ISP).
 * Die Werte stehen in der Teile-Tabelle ({@link RocketPart}); hier nur noch die nicht raketenrelevanten Original-Attribute
 * (Verbrauch/Nutzlast der Triebwerke, Ungenauigkeit der Leitwerke) und der Tooltip.
 * Darstellung: OBJ-Modell des Teils als Icon (Original {@code ItemRenderMissilePart}, Item-Modell {@code builtin/entity}).
 * Original-Klasse: ItemCustomMissilePart (Space-Teile; Raptors HBM-Klasse bleibt unberuehrt)
 */
public class ItemRocketPart extends Item {

    /** Original {@code attributes[1]} (Triebwerk: Verbrauch) bzw. {@code attributes[0]} (Leitwerk: Ungenauigkeit). */
    private final float attrA;
    /** Original {@code attributes[2]} (Triebwerk: Nutzlast). */
    private final float attrB;

    public ItemRocketPart(Properties props, float attrA, float attrB) {
        super(props);
        this.attrA = attrA;
        this.attrB = attrB;
    }

    @Nullable
    public RocketPart part() {
        return RocketPart.getPart(this);
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
    *///?}
        if (this == SpaceRocketItems.RP_POD_20.get()) return;

        RocketPart part = part();
        if (part == null) return;

        try {
            switch (part.type) {
                case WARHEAD -> {
                    list.add(line("item.missile.part.size", getSize(part.bottom)));
                    list.add(line("item.missile.part.type", getWarhead(part.warhead)));
                    list.add(line("item.missile.part.mass", Component.literal(part.mass + "kg")));
                }
                case FUSELAGE -> {
                    list.add(line("item.missile.part.topSize", getSize(part.top)));
                    list.add(line("item.missile.part.bottomSize", getSize(part.bottom)));
                    list.add(line("item.missile.part.fuelType", getFuelName(part.fuel)));
                    list.add(line("item.missile.part.fuelAmount", Component.literal(part.getTankSize() + "mB")));
                    list.add(line("item.missile.part.mass", Component.literal(part.mass + "kg")));
                }
                case FINS -> {
                    list.add(line("item.missile.part.size", getSize(part.top)));
                    list.add(line("item.missile.part.inaccuracy", Component.literal(attrA * 100 + "%")));
                }
                case THRUSTER -> {
                    list.add(line("item.missile.part.size", getSize(part.top)));
                    list.add(line("item.missile.part.fuelType", getFuelName(part.fuel)));
                    list.add(line("item.missile.part.fuelConsumption", Component.literal(attrA + "l/tick")));
                    list.add(line("item.missile.part.maxPayload", Component.literal(attrB + "kg")));
                    list.add(line("item.missile.part.thrust", Component.literal(part.getThrust() + "N")));
                    list.add(line("item.missile.part.isp", Component.literal(part.getISP() + "s")));
                    list.add(line("item.missile.part.mass", Component.literal(part.mass + "kg")));
                }
                default -> { }
            }
        } catch (Exception ex) {
            list.add(Component.translatable("error.generic"));
        }
    }

    /** {@code EnumChatFormatting.BOLD + key + ": " + EnumChatFormatting.GRAY + value}. */
    static MutableComponent line(String key, Component value) {
        MutableComponent v = value.getStyle().getColor() == null ? value.copy().withStyle(ChatFormatting.GRAY) : value.copy();
        return Component.literal("").append(Component.translatable(key).withStyle(ChatFormatting.BOLD))
                .append(Component.literal(": ").withStyle(ChatFormatting.BOLD)).append(v);
    }

    /** Original {@code getSize}: 2.5/3.0 m fallen dort auf "none" (1:1). */
    static Component getSize(PartSize size) {
        return switch (size) {
            case ANY -> Component.translatable("item.missile.part.size.any");
            case SIZE_10 -> Component.literal("1.0m");
            case SIZE_15 -> Component.literal("1.5m");
            case SIZE_20 -> Component.literal("2.0m");
            default -> Component.translatable("item.missile.part.size.none");
        };
    }

    /** Original {@code getWarhead()} fuer die beiden Space-Nutzlasten (blinkend Gold/Rot). */
    static Component getWarhead(@Nullable WarheadType type) {
        boolean blink = System.currentTimeMillis() % 1000 < 500;
        if (type == WarheadType.APOLLO)
            return Component.translatable("item.warhead.desc.apollo").withStyle(blink ? ChatFormatting.GOLD : ChatFormatting.RED);
        if (type == WarheadType.SATELLITE)
            return Component.translatable("item.warhead.desc.satellite").withStyle(blink ? ChatFormatting.GOLD : ChatFormatting.RED);
        return Component.translatable("general.na").withStyle(ChatFormatting.BOLD);
    }

    /** Original {@code getFuelName()} (Farben 1:1; METHALOX/KEROLOX Space-neu). */
    static Component getFuelName(@Nullable FuelType fuel) {
        if (fuel == null) return Component.translatable("general.na").withStyle(ChatFormatting.BOLD);
        ChatFormatting color = switch (fuel) {
            case ANY -> ChatFormatting.GRAY;
            case KEROSENE, KEROLOX -> ChatFormatting.LIGHT_PURPLE;
            case METHALOX -> ChatFormatting.YELLOW;
            case SOLID -> ChatFormatting.GOLD;
            case HYDROGEN -> ChatFormatting.DARK_AQUA;
            case XENON -> ChatFormatting.DARK_PURPLE;
            case BALEFIRE -> ChatFormatting.GREEN;
            case HYDRAZINE -> ChatFormatting.AQUA;
        };
        return Component.translatable(fuel.unlocalizedName).withStyle(color);
    }

    // Original ItemRenderMissilePart: OBJ des Teils als Icon (Item-Modell builtin/entity)
    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.rocket.client.ItemRenderRocketPart.instance();
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
                return com.space_m.rocket.client.ItemRenderRocketPart.instance();
            }
        });
    }
    *///?}
}
