package com.space_m.machine.dyson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.particle.helper.IParticleCreator;
import com.space_m.machine.misc.R7MachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityDysonConverterHE}: nimmt den Empfaengerstrahl an der Zelle {@code dir*4, y+1} an
 * ({@code power = energy}) und gibt ihn als HE ueber die Anschlusszelle hinten ({@code -dir*4}, Kabel bei {@code -dir*5})
 * ab. Puffer = zuletzt empfangene Energie ({@code getMaxPower() = power}); erhaelt er 10 Doppelticks lang nichts, wird er
 * geleert (kein Endspiel-Akku). Partikel "tau"/"hadron" an vier Spulenpunkten, solange er wandelt.
 *
 * <p>Abweichung: {@code power} kann bis {@code Long.MAX_VALUE / 200} gross sein; die HBM-Basisklasse fuehrt Kapazitaet und
 * Fuellstand getrennt - die Kapazitaet folgt deshalb jedem Empfang ({@code setEnergyCapacity(power)}), wie im Original.
 * Original-Klasse: TileEntityDysonConverterHE
 */
public class TileEntityDysonConverterHE extends BaseMachineBlockEntity implements IDysonConverter {

    public boolean isConverting;
    private int cooldown;

    public TileEntityDysonConverterHE(BlockPos pos, BlockState state) {
        super(DysonBlockEntities.CONVERTER_HE.get(), pos, state, 0, 0L, 0L, Long.MAX_VALUE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDysonConverterHE be) {
        if (level.isClientSide) return;
        ServerLevel sl = (ServerLevel) level;

        // Original: dir = orientation.getOpposite(); tryProvide an (dir*5) - ueber die Anschlusszelle (getExtraEnergyPorts)
        be.ensureNetworkInitialized();

        Direction dir = R7MachineBlock.dir(state).getOpposite();
        Direction rot = R7MachineBlock.rot(dir);

        be.isConverting = be.getEnergyStored() > 0;

        if (be.isConverting && level.getGameTime() % 2 == 0) {
            CompoundTag dPart = new CompoundTag();
            dPart.putString("type", level.getGameTime() % 10 == 0 ? "tau" : "hadron");
            dPart.putByte("count", (byte) 1);
            double bx = pos.getX() + 0.5, bz = pos.getZ() + 0.5;
            IParticleCreator.sendPacket(sl, bx + dir.getStepX() * 4 + rot.getStepX(), pos.getY() + 2.25, bz + dir.getStepZ() * 4 + rot.getStepZ(), 25, dPart);
            IParticleCreator.sendPacket(sl, bx + dir.getStepX() * 4 - rot.getStepX(), pos.getY() + 2.25, bz + dir.getStepZ() * 4 - rot.getStepZ(), 25, dPart);
            IParticleCreator.sendPacket(sl, bx + dir.getStepX() * 3 + rot.getStepX(), pos.getY() + 2.75, bz + dir.getStepZ() * 3 + rot.getStepZ(), 25, dPart);
            IParticleCreator.sendPacket(sl, bx + dir.getStepX() * 3 - rot.getStepX(), pos.getY() + 2.75, bz + dir.getStepZ() * 3 - rot.getStepZ(), 25, dPart);

            be.cooldown++;
            if (be.cooldown > 10) {
                // To prevent this machine acting like an endgame battery, but still be able to transmit every drop of power
                // this machine will clear its buffers (almost) immediately after transmitting power
                be.setEnergyStored(0);
                be.cooldown = 0;
            }
        }

        be.setChanged();
        be.sendUpdateToClient(); // networkPackNT(250)
    }

    @Override
    public boolean provideEnergy(int x, int y, int z, long energy) {
        Direction dir = R7MachineBlock.dir(getBlockState());
        int rx = worldPosition.getX() + dir.getStepX() * 4;
        int ry = worldPosition.getY() + 1;
        int rz = worldPosition.getZ() + dir.getStepZ() * 4;

        if (x != rx || y != ry || z != rz) return false;

        setEnergyCapacity(Math.max(energy, 0L)); // getMaxPower() = power
        setEnergyStored(energy);
        cooldown = 0;

        return true;
    }

    @Override
    public long maximumEnergy() {
        return Long.MAX_VALUE;
    }

    /** Stromausgang: Anschlusszelle {@code makeExtra(-dir*4)} (Kabel daneben bei {@code -dir*5}). */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        Direction dir = R7MachineBlock.dir(getBlockState());
        return new BlockPos[] { worldPosition.offset(-dir.getStepX() * 4, 0, -dir.getStepZ() * 4) };
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    // ==================== Sync ====================

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("s_converting", isConverting);
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag nbt) {
        super.applyClientUpdate(nbt);
        isConverting = nbt.getBoolean("s_converting");
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }

    /** Original {@code getName() = "container.machineDysonConverterHE"}. */
    @Override protected Component getDefaultName() { return Component.translatable("container.machineDysonConverterHE"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox}: x-6..x+7, y..y+6, z-6..z+7. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 6, worldPosition.getY(), worldPosition.getZ() - 6,
                worldPosition.getX() + 7, worldPosition.getY() + 6, worldPosition.getZ() + 7);
    }
}
