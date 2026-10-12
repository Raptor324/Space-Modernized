package com.space_m.machine.mining;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.menu.MultiblockMenuReach;
import com.hbm_m.platform.ModItemStackHandler;
import com.space_m.celestial.CelestialBody;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.SpaceRocketItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineDriveProcessor}: verarbeitet volle Laufwerke ({@code hard_drive_full}) in 100
 * Ticks (200 HE/Tick, Speicher 2 000 HE, arbeitet nur ab 75 % Ladung), wenn die Prozessorstufe (Slot 2:
 * {@code processt1..3}) mindestens die Verarbeitungsstufe des Ziels vom aktuellen Koerper aus erreicht; klont ein volles
 * Laufwerk auf ein leeres ({@code hard_drive}, Slot 1). Slots: 0 Laufwerk, 1 Klonziel, 2 Prozessor, 3 Batterie;
 * Stapelgrenze 1. Steuerung aus dem GUI ({@code NBTControlPacket}: {@code process}/{@code clone}), Status als Text mit
 * Farbcodes.
 * Original-Klasse: TileEntityMachineDriveProcessor
 */
public class TileEntityMachineDriveProcessor extends BaseMachineBlockEntity implements IControlReceiver {

    public long maxPower = 2_000;

    public boolean isProcessing;
    public int progress;
    public int maxProgress = 100; // 5 seconds

    public String status = "";
    public boolean hasDrive = false;

    private int lastTier;

    public TileEntityMachineDriveProcessor(BlockPos pos, BlockState state) {
        super(MiningMachines.DRIVE_PROCESSOR_BE.get(), pos, state, 4, 2_000L, 2_000L);
    }

    /** Original {@code getInventoryStackLimit() = 1}. */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineDriveProcessor be) {
        if (!level.isClientSide) be.serverTick();
    }

    void serverTick() {
        chargeFromBatterySlot(3); // Library.chargeTEFromItems(slots, 3, power, maxPower)
        // Strom: Original trySubscribe an allen sechs Nachbarn -> HBM-Netz (Kern + Anschlusszelle)

        ItemStack drive = inventory.getStackInSlot(0);

        if (getEnergyStored() < maxPower * 0.75) {
            isProcessing = false;
            status = ChatFormatting.RED + "No power ";
        } else if (drive.isEmpty() || !drive.is(SpaceRocketItems.FULL_DRIVE.get())) {
            isProcessing = false;
            status = "";
        } else if (getProcessingTier() < ItemVOTVdrive.getProcessingTier(drive, CelestialBody.getBody(level))) {
            isProcessing = false;
            status = ChatFormatting.RED + "Low tier ";
        }

        if (lastTier != getProcessingTier()) {
            status = "";
        }

        if (isProcessing) {
            setEnergyStored(getEnergyStored() - 200);

            status = ChatFormatting.GREEN + "" + ChatFormatting.ITALIC + "Processing  ";
            progress++;

            if (progress >= maxProgress) {
                progress = 0;
                isProcessing = false;
                ItemVOTVdrive.setProcessed(drive, true);
                inventory.setStackInSlot(0, drive); // Aenderung melden
                status = ChatFormatting.GREEN + "Done! ";
            }
        } else {
            progress = 0;
        }

        lastTier = getProcessingTier();
        ItemStack now = inventory.getStackInSlot(0);
        hasDrive = !now.isEmpty() && now.is(SpaceRocketItems.FULL_DRIVE.get());

        setChanged();
        sendUpdateToClient(); // networkPackNT(15)
    }

    @Override
    public boolean hasPermission(Player player) {
        // Original isUseableByPlayer: Abstand^2 <= 128
        return MultiblockMenuReach.stillValidCore(this, player, 128.0D);
    }

    /** Original: {@code ModItems.circuit} mit {@code PROCESST1/2/3} -> Stufe 1/2/3, sonst 0. */
    int getProcessingTier() {
        ItemStack stack = inventory.getStackInSlot(2);
        if (stack.isEmpty()) return 0;
        if (stack.is(SpaceMaterialItems.PROCESST1.get())) return 1;
        if (stack.is(SpaceMaterialItems.PROCESST2.get())) return 2;
        if (stack.is(SpaceMaterialItems.PROCESST3.get())) return 3;
        return 0;
    }

    private void processDrive(boolean process) {
        if (!process) {
            isProcessing = false;
            return;
        }

        ItemStack drive = inventory.getStackInSlot(0);
        if (getEnergyStored() < maxPower * 0.75) return;
        if (drive.isEmpty() || !drive.is(SpaceRocketItems.FULL_DRIVE.get())) return;
        if (ItemVOTVdrive.getProcessed(drive)) return;

        // Check that our installed upgrade is a high enough tier
        if (getProcessingTier() >= ItemVOTVdrive.getProcessingTier(drive, CelestialBody.getBody(level))) {
            isProcessing = true;
        }
    }

    private void cloneDrive() {
        ItemStack drive = inventory.getStackInSlot(0);
        if (getEnergyStored() < maxPower * 0.75) return;
        if (drive.isEmpty() || !drive.is(SpaceRocketItems.FULL_DRIVE.get())) return;
        ItemStack target = inventory.getStackInSlot(1);
        if (target.isEmpty() || !target.is(SpaceMaterialItems.HARD_DRIVE.get())) {
            status = ChatFormatting.RED + "No target ";
            return;
        }

        ItemVOTVdrive.markCopied(drive);
        inventory.setStackInSlot(0, drive);
        inventory.setStackInSlot(1, drive.copy());

        status = ChatFormatting.GREEN + "Drive cloned ";
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("process")) {
            processDrive(data.getBoolean("process"));
        }

        if (data.contains("clone")) {
            cloneDrive();
        }
    }

    // ── NBT (Original writeToNBT; hasDrive nur im Sync wie serialize) ─────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);

        nbt.putLong("power", getEnergyStored());
        nbt.putBoolean("isProcessing", isProcessing);
        nbt.putInt("progress", progress);
        nbt.putString("status", status);
        nbt.putInt("lastTier", lastTier);

        nbt.putBoolean("hasDrive", hasDrive);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);

        if (nbt.contains("power")) setEnergyStored(nbt.getLong("power"));
        isProcessing = nbt.getBoolean("isProcessing");
        progress = nbt.getInt("progress");
        status = nbt.getString("status");
        lastTier = nbt.getInt("lastTier");

        if (level != null && level.isClientSide) hasDrive = nbt.getBoolean("hasDrive");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    /** Original: einfache {@code Slot}s, keine Pruefung. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerDriveProcessor(id, inv, this);
    }

    /** Original {@code getRenderBoundingBox}: x-1..x+2, y..y+1, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }
}
