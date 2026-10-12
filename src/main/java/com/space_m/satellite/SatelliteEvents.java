package com.space_m.satellite;

import com.space_m.rocket.RocketPart;
import com.space_m.rocket.RocketPayloadHooks;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * R6-C Einstieg (gemeinsam): Satelliten-Registry nach HBM, Raketen-Nutzlast ({@link RocketPayloadHooks}), Client-Sync
 * je Tick, und das {@code ItemSatellite}-Verhalten der HBM-{@code sat_*}-Nutzlasten (Rechtsklick, Herstellen) per Event.
 */
public final class SatelliteEvents {

    private SatelliteEvents() {}

    public static void init() {
        RocketPayloadHooks.set(new PayloadHooks());
        com.hbm_m.api.space.SpaceApi.setSatelliteHooks(new SpaceSatelliteHooks()); // W-R6C-2..4

        LifecycleEvent.SETUP.register(SpaceSatelliteRegistry::ensureRegistered);
        LifecycleEvent.SERVER_STARTING.register(server -> SpaceSatelliteRegistry.ensureRegistered());
        LifecycleEvent.SERVER_STOPPED.register(server -> SatelliteSyncPacket.reset());

        // Original PermaSyncHandler (SATELLITES) je Spieler und Tick
        TickEvent.SERVER_POST.register(SatelliteSyncPacket::tick);

        // ItemSatellite.onItemRightClick fuer die HBM-Nutzlasten (sat_mapper ...); die Space-Items erledigen das selbst
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!isHbmPayloadSatellite(stack.getItem())) return CompoundEventResult.pass();
            InteractionResultHolder<ItemStack> result = SatelliteItemLogic.use(player.level(), player, stack);
            if (result == null) return CompoundEventResult.pass();
            return CompoundEventResult.interruptTrue(result.getObject());
        });

        // ItemSatellite.onCreated -> ensureItemData (HBM-Nutzlasten)
        PlayerEvent.CRAFT_ITEM.register((player, constructed, inventory) -> {
            if (isHbmPayloadSatellite(constructed.getItem())) SatelliteItemData.ensureItemData(constructed);
        });
    }

    /** Die Gegenstaende, die im Original {@code ItemSatellite} sind: Satelliten-Nutzlasten der Teile-Tabelle. */
    public static boolean isPayloadSatellite(Item item) {
        if (!SpaceSatelliteRegistry.isSatelliteItem(item)) return false;
        RocketPart part = RocketPart.getPart(item);
        return part != null && part.warhead == RocketPart.WarheadType.SATELLITE;
    }

    /** HBM-Nutzlasten (nicht {@link ItemSpaceSatellite}). */
    public static boolean isHbmPayloadSatellite(Item item) {
        return !(item instanceof ItemSpaceSatellite) && isPayloadSatellite(item);
    }

    /** {@link RocketPayloadHooks} 1:1 {@code XSatelliteRegistry}/{@code SatelliteBase}. */
    static final class PayloadHooks implements RocketPayloadHooks {
        @Override
        public boolean isSatelliteItem(Item item) {
            return SpaceSatelliteRegistry.isSatelliteItem(item);
        }

        @Override
        public void copyItemData(ItemStack from, ItemStack to) {
            SatelliteItemData.copyItemData(from, to);
        }

        @Override
        public void ensureItemData(ItemStack stack) {
            SatelliteItemData.ensureItemData(stack);
        }

        @Override
        public void orbit(ServerLevel targetWorld, ItemStack stack, int freq, double x, double y, double z) {
            SpaceSatellites.orbit(targetWorld, stack, freq, x, y, z);
        }
    }
}
