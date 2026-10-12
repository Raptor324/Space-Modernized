package com.space_m.rocket.menu;

import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.MachineRocketAssemblyBlockEntity;
import com.space_m.rocket.RocketPart;
import com.space_m.rocket.RocketPart.PartType;
import com.space_m.rocket.SpaceRocketItems;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Slots der VAB, 1:1 NTM Space {@code SlotRocket} (+ innere Klassen {@code SlotRocketPart}, {@code SlotCapsule},
 * {@code SlotDrive}). Stapelgrenze 8 (Original {@code getInventoryStackLimit}).
 * Original-Klasse: SlotRocket
 */
public class SlotRocket extends Slot {

    public SlotRocket(Container inventory, int id, int x, int y) {
        super(inventory, id, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof ItemCustomRocket;
    }

    @Override
    public int getMaxStackSize() {
        return 8;
    }

    public static class SlotRocketPart extends SlotLayer {

        private final PartType type;

        public SlotRocketPart(Container inventory, int id, int x, int y, int layer, PartType type) {
            super(inventory, id, x, y, layer);
            this.type = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!super.mayPlace(stack)) return false;
            if (stack == null || stack.isEmpty()) return false;
            RocketPart part = RocketPart.getPart(stack);
            if (part == null) return false;

            return part.type == type;
        }

        @Override
        public int getMaxStackSize() {
            return 8;
        }
    }

    public static class SlotCapsule extends Slot {

        public SlotCapsule(Container inventory, int id, int x, int y) {
            super(inventory, id, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            RocketPart item = RocketPart.getPart(stack);
            if (item == null) return false;

            if (item.type != PartType.WARHEAD) return false;
            if (stack.getItem() == SpaceRocketItems.RP_POD_20.get()) return false;

            return item.isCapsuleOrSatellite();
        }

        @Override
        public int getMaxStackSize() {
            return 8;
        }
    }

    public static class SlotDrive extends SlotLayer {

        private final MachineRocketAssemblyBlockEntity inventory;

        public SlotDrive(Container container, MachineRocketAssemblyBlockEntity inventory, int id, int x, int y, int layer) {
            super(container, id, x, y, layer);
            this.inventory = inventory;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isVisible()) return false;
            if (stack == null || stack.isEmpty()) return false;
            return stack.getItem() instanceof ItemVOTVdrive;
        }

        @Override
        public void setLayer(int layer) {
            super.setLayer(layer);
            if (inventory != null) inventory.setCurrentStage(layer);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
