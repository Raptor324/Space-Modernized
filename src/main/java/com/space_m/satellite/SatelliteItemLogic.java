package com.space_m.satellite;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ISatChip;
import com.space_m.celestial.CelestialBody;
import com.space_m.rocket.RocketPart;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Verhalten von NTM Space {@code ItemSatellite} fuer alle Satelliten-Gegenstaende: Tooltip (Frequenz, Besitzer, Bahn,
 * Farbe mit Shift; Beschreibung; Handstart-Hinweis), Schleichen+Rechtsklick = Einstellungs-GUI, Rechtsklick in der
 * Orbit-Dimension = Satellit von Hand aussetzen (nur Satelliten bis 16 000 kg).
 *
 * <p>Fuer die Space-neuen Gegenstaende ({@link ItemSpaceSatellite}) direkt; fuer die HBM-{@code sat_*} (Raptor-/HBM-Code
 * unveraendert) ueber Rechtsklick-/Tooltip-/Herstell-Events ({@link SatelliteEvents}, Client {@code SatelliteClient}).
 * Original-Klasse: ItemSatellite
 */
public final class SatelliteItemLogic {

    private SatelliteItemLogic() {}

    /** Original {@code if(mass <= 16_000) canLaunchByHand = true}. */
    public static final int HAND_LAUNCH_MASS = 16_000;

    // Client-Haken (setzt SatelliteClient; Server: nie aufgerufen)
    public static Runnable openSettings = () -> { };
    public static BooleanSupplier shiftDown = () -> false;
    public static Supplier<Level> clientLevel = () -> null;

    /** Masse der Nutzlast aus der Teile-Tabelle (R5-A, Original {@code ItemSatellite(mass)}). */
    public static boolean canLaunchByHand(Item item) {
        RocketPart part = RocketPart.getPart(item);
        return part != null && part.warhead == RocketPart.WarheadType.SATELLITE && part.mass <= HAND_LAUNCH_MASS;
    }

    /**
     * Original {@code onItemRightClick}. @return null = nicht zustaendig (Vanilla/HBM weiter), sonst Ergebnis.
     */
    @Nullable
    public static InteractionResultHolder<ItemStack> use(Level world, Player player, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            if (world.isClientSide) openSettings.run();
            return InteractionResultHolder.success(stack);
        }

        if (!canLaunchByHand(stack.getItem())) return null;
        if (!CelestialBody.inOrbit(world)) return null;

        if (world instanceof ServerLevel server) {
            CelestialBody body = CelestialBody.getTarget(world, (int) player.getX(), (int) player.getZ()).body;
            ServerLevel targetWorld = body != null && body.dimension != null ? server.getServer().getLevel(body.dimension) : server.getServer().overworld();
            if (targetWorld == null) return InteractionResultHolder.pass(stack);

            SpaceSatellites.orbit(targetWorld, stack, ISatChip.getFreqS(stack), player.getX(), player.getY(), player.getZ());

            player.sendSystemMessage(Component.literal("Satellite launched successfully!"));
        }

        stack.shrink(1);

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    /**
     * Original {@code addInformation}. {@code withDescription}: die Beschreibungszeile ({@code item.sat.desc.<typ>}) -
     * bei den HBM-Chips zeigt HBM schon seine eigene ({@code satchip.*}), dort entfaellt sie.
     */
    public static void appendTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, boolean withDescription) {
        if (shiftDown.getAsBoolean()) {
            SatelliteOrbit o = SatelliteItemData.read(itemstack);
            list.add(formatTooltipEntry("item.sat.desc.frequency", Integer.toString(ISatChip.getFreqS(itemstack))));
            list.add(formatTooltipEntry("item.sat.desc.owner", o.owner));
            list.add(formatTooltipEntry("item.sat.desc.speed", formatOrbitSpeed(o.altitude) + "km/s"));
            list.add(formatTooltipEntry("item.sat.desc.altitude", formatValue(o.altitude) + "km"));
            list.add(formatTooltipEntry("item.sat.desc.inclination", formatValue(o.inclination) + "°"));
            list.add(formatTooltipEntry("item.sat.desc.phase", formatPhaseOffset(o.phaseOffset) + "°"));
            list.add(formatTooltipEntry("item.sat.desc.color", getHexColor(o)));
        } else {
            list.add(Component.literal("").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }

        if (withDescription) {
            String desc = descriptionKey(itemstack.getItem());
            if ("item.sat.desc.gerald".equals(desc)) {
                list.add(Component.translatable("item.sat.desc.gerald.single_use"));
                list.add(Component.translatable("item.sat.desc.gerald.orbital_module"));
                list.add(Component.translatable("item.sat.desc.gerald.melter"));
            } else if (desc != null) {
                list.add(Component.translatable(desc));
            }
        }

        if (canLaunchByHand(itemstack.getItem())) {
            list.add(Component.translatable("item.sat.desc.launch_by_hand").withStyle(ChatFormatting.GOLD));

            Level world = level != null ? level : clientLevel.get();
            if (world != null && CelestialBody.inOrbit(world))
                list.add(Component.translatable("item.sat.desc.deploy_orbit").withStyle(ChatFormatting.BOLD));
        }
    }

    /** Original: {@code if(this == ModItems.sat_xxx) list.add(item.sat.desc.xxx)}. */
    @Nullable
    private static String descriptionKey(Item item) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        return switch (id) {
            case "hbm_m:sat_foeq" -> "item.sat.desc.foeq";
            case "hbm_m:sat_gerald" -> "item.sat.desc.gerald";
            case "hbm_m:sat_laser" -> "item.sat.desc.laser";
            case "hbm_m:sat_mapper" -> "item.sat.desc.mapper";
            case "hbm_m:sat_miner" -> "item.sat.desc.miner";
            case "hbm_m:sat_lunar_miner" -> "item.sat.desc.lunar_miner";
            case "hbm_m:sat_radar" -> "item.sat.desc.radar";
            case "hbm_m:sat_resonator" -> "item.sat.desc.resonator";
            case "hbm_m:sat_scanner" -> "item.sat.desc.scanner";
            case "space_m:sat_war" -> "item.sat.desc.war";
            case "space_m:sat_dyson_relay" -> "item.sat.desc.dyson_relay";
            default -> null;
        };
    }

    public static String formatValue(float value) {
        if (value == (int) value) return Integer.toString((int) value);
        return Float.toString(value);
    }

    public static String formatOrbitSpeed(float altitude) {
        float orbitSpeed = SatelliteOrbit.getOrbitSpeedKmPerSecond(altitude);
        return formatValue(Math.round(orbitSpeed * 10.0F) / 10.0F);
    }

    public static String formatPhaseOffset(float phase) {
        float phaseOffset = SatelliteOrbit.normalizePhaseOffset(phase);
        return formatValue(Math.round(phaseOffset * 10.0F) / 10.0F);
    }

    private static String getHexColor(SatelliteOrbit o) {
        int r = Mth.clamp(Math.round(o.colorR * 255F), 0, 255);
        int g = Mth.clamp(Math.round(o.colorG * 255F), 0, 255);
        int b = Mth.clamp(Math.round(o.colorB * 255F), 0, 255);
        return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
    }

    /** {@code YELLOW + key + ": " + GOLD + value}. */
    private static MutableComponent formatTooltipEntry(String key, String value) {
        String safeValue = value != null ? value : "";
        return Component.literal("").append(Component.translatable(key).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(": ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(safeValue).withStyle(ChatFormatting.GOLD));
    }
}
