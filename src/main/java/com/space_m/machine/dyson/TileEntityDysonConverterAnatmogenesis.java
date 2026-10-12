package com.space_m.machine.dyson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.space_m.celestial.CelestialBody;
import com.space_m.machine.atmo.AtmoFluids;
import com.space_m.machine.misc.R7MachineBlock;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityDysonConverterAnatmogenesis} ("Erschaffung einer Atmosphaere aus dem Nichts", die
 * Survival-Version des Atmosphaeren-Editors): nimmt den Empfaengerstrahl an {@code dir*5, y+1} an und setzt
 * {@code energy / 28 800 000} mB des gewaehlten Gases (Standard Sauerstoff) frei bzw. faengt es ein
 * ({@code FT_Gaseous.release/capture}); 100 THE/s ergeben 0,1 atm in 8 Stunden. {@code gasProduced} zaehlt je Tick und
 * wird nach dem Sync geleert. Client: Gasfahne ("tower", Farbe des Gases) ueber dem Kern, solange gewandelt wird.
 * Original-Klasse: TileEntityDysonConverterAnatmogenesis
 */
public class TileEntityDysonConverterAnatmogenesis extends BaseMachineBlockEntity implements IDysonConverter {

    // what the FUCK is anatmogenesis you ask?
    // I made it the fuck up

    // from:
    // * an-        = without
    // * atmo-      = vapour, air (from atmosphere)
    // * genesis    = creation

    // similar to abiogenesis (life from non-life/nothing)
    // anatmogenesis is the creation of an atmosphere from nothing

    // this is effectively the survival version of the creative atmosphere editor,
    // turning absolutely ridiculous amounts of energy into any gas you please,
    // or remove a gas entirely, if you so desire.

    public Fluid fluid = ModFluids.OXYGEN.getSource();
    public boolean isEmitting = true;

    public long gasProduced;

    public boolean isConverting;

    /** Stand von {@code gasProduced} beim letzten Sync (das Paket wird erst nach dem Leeren gebaut). */
    private long syncedGasProduced;

    // 100THE/s will produce 0.1atm in 8 hours
    private static final long HE_TO_MB = 28_800_000;

    public TileEntityDysonConverterAnatmogenesis(BlockPos pos, BlockState state) {
        super(DysonBlockEntities.CONVERTER_ANATMOGENESIS.get(), pos, state, 0, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDysonConverterAnatmogenesis be) {
        if (!level.isClientSide) {
            be.isConverting = be.gasProduced > 0;
            be.syncedGasProduced = be.gasProduced;

            be.setChanged();
            be.sendUpdateToClient(); // networkPackNT(250)
            be.gasProduced = 0;
        } else if (be.isConverting) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.machine.dyson.client.DysonClientEffects.anatmogenesisTick(be));
        }
    }

    @Override
    public boolean provideEnergy(int x, int y, int z, long energy) {
        Direction dir = R7MachineBlock.dir(getBlockState());
        int rx = worldPosition.getX() + dir.getStepX() * 5;
        int ry = worldPosition.getY() + 1;
        int rz = worldPosition.getZ() + dir.getStepZ() * 5;

        if (x != rx || y != ry || z != rz) return false;

        long volume = energy / HE_TO_MB;
        gasProduced += volume;

        if (level != null) {
            if (isEmitting) {
                CelestialBody.releaseGas(level, AtmoFluids.id(fluid), volume);
            } else {
                CelestialBody.captureGas(level, AtmoFluids.id(fluid), volume);
            }
        }

        return true;
    }

    @Override
    public long maximumEnergy() {
        return Long.MAX_VALUE;
    }

    // ==================== NBT / Sync ====================

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putString("fluid", AtmoFluids.id(fluid).toString());
        nbt.putBoolean("emit", isEmitting);
        // networkPackNT
        nbt.putBoolean("s_converting", isConverting);
        nbt.putLong("s_gasProduced", syncedGasProduced);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        if (nbt.contains("fluid")) {
            ResourceLocation id = ResourceLocation.tryParse(nbt.getString("fluid"));
            fluid = AtmoFluids.fluid(id);
        }
        isEmitting = nbt.getBoolean("emit");
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag nbt) {
        super.applyClientUpdate(nbt);
        isConverting = nbt.getBoolean("s_converting");
        gasProduced = nbt.getLong("s_gasProduced");
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }

    /** Original {@code getName() = "container.dysonConverterAnatmogenesis"}. */
    @Override protected Component getDefaultName() { return Component.translatable("container.dysonConverterAnatmogenesis"); }
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
