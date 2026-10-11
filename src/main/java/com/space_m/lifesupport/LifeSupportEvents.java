package com.space_m.lifesupport;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.item.SpaceItems;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Gemeinsame Ereignisse der Lebenserhaltung (R2-B), loaderneutral ueber Architectury.
 * <ul>
 *   <li>Schwere Stiefel: Original {@code ItemModHeavyBoots.onArmorTick} (getragen) und {@code modUpdate} (als Mod im
 *       Stiefel) laufen im Original auf Client und Server (Spielerbewegung ist clientseitig) - hier Spieler-Tick beider
 *       Seiten.</li>
 *   <li>Original {@code FluidTank.noDualUnload.add(oxy_pinwheel)}.</li>
 * </ul>
 */
public final class LifeSupportEvents {

    private LifeSupportEvents() {}

    public static void init() {
        TickEvent.PLAYER_PRE.register(LifeSupportEvents::playerTick);
        LifecycleEvent.SETUP.register(() -> FluidTank.noDualUnload.add(SpaceItems.OXY_PINWHEEL.get()));
    }

    private static void playerTick(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) return;

        if (boots.getItem() instanceof ItemModHeavyBoots heavy) {
            heavy.onArmorTick(player.level(), player, boots);
            return;
        }

        if (ArmorModificationHelper.hasMods(boots)) {
            ItemStack mod = ArmorModificationHelper.pryMod(boots, ArmorModificationHelper.boots_only);
            if (mod != null && !mod.isEmpty() && mod.getItem() instanceof ItemModHeavyBoots heavy) {
                heavy.onArmorTick(player.level(), player, mod);
            }
        }
    }
}
