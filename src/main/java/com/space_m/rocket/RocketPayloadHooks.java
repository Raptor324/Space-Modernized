package com.space_m.rocket;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Satelliten als Nutzlast (Original {@code XSatelliteRegistry.isSatelliteItem}, {@code SatelliteBase.copyItemData/ensureItemData}).
 * R6-C setzt die Implementierung mit {@link #set}; Standard = Satelliten-Items ohne Zusatzdaten (nur Frequenz ueber
 * {@code ISatChip}).
 */
public interface RocketPayloadHooks {

    default boolean isSatelliteItem(Item item) {
        RocketPart part = RocketPart.getPart(item);
        return part != null && part.warhead == RocketPart.WarheadType.SATELLITE && item != SpaceRocketItems.RP_STATION_CORE_20.get();
    }

    default void copyItemData(ItemStack from, ItemStack to) { }

    default void ensureItemData(ItemStack stack) { }

    /**
     * Original {@code XSatelliteRegistry.orbit(targetWorld, stack, freq, x, y, z)} beim Aussetzen der Satelliten-Nutzlast
     * (R6-C: Zielkoerper je Satellitentyp, Bahndaten aus dem Stapel). Standard = HBM {@code SatelliteManager.orbit}.
     */
    default void orbit(net.minecraft.server.level.ServerLevel targetWorld, ItemStack stack, int freq, double x, double y, double z) {
        com.hbm_m.satellite.SatelliteManager.get(targetWorld).orbit(targetWorld, stack, freq, x, y, z);
    }

    final class Holder {
        private static RocketPayloadHooks hooks = new RocketPayloadHooks() { };

        private Holder() {}
    }

    static RocketPayloadHooks get() { return Holder.hooks; }

    static void set(RocketPayloadHooks hooks) { Holder.hooks = hooks; }
}
