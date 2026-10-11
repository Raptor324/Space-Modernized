package com.space_m.machine.atmo;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialFluids;

import net.minecraft.core.BlockPos;
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
 * 1:1 NTM Space {@code TileEntityAtmoTower} (erbt dort {@code TileEntityDeuteriumTower} ->
 * {@code TileEntityDeuteriumExtractor}): verfluessigt Luft ({@code EARTHAIR}, 50 000 mB) zu Stickstoff
 * ({@code NITROGEN}, 5 000 mB) im Verhaeltnis 50:1.
 *
 * <p>Je Tick, wenn {@code power >= maxPower/100} (10 000 HE), {@code power > 200}, mindestens 100 mB Luft und Platz im
 * Stickstofftank: {@code convert = min(nitrogen.max, air.fill) / 50}, begrenzt auf den freien Platz; Luft
 * {@code - convert*50}, Stickstoff {@code + convert}, Energie {@code - maxPower/100}. Puffer 1 000 000 HE.
 * Luft wird an den acht Sockelnachbarn abonniert, Stickstoff dorthin geliefert ({@code getConPos} des Deuterium-Turms).
 *
 * <p>Abweichung (technisch noetig): Stickstoff ist ein Space-Fluid (R4, {@code space_m:nitrogen}); solange es nicht
 * registriert ist, bleibt der Turm stehen statt ein typloses Fluid zu erzeugen.
 * Original-Klasse: TileEntityAtmoTower
 */
public class AtmoTowerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final long MAX_POWER = 1_000_000L;

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.AIR.getSource(), 50_000),
            new FluidTank(AtmoFluids.fluid(CelestialFluids.NITROGEN), 5_000)
    };

    public AtmoTowerBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.ATMOSPHERIC_TOWER.get(), pos, state, 0, MAX_POWER, MAX_POWER);
    }

    /** Original {@code hasPower}: {@code power >= getMaxPower() / 100}. */
    public boolean hasPower() {
        return getEnergyStored() >= MAX_POWER / 100;
    }

    /** Original {@code hasEnoughWater}: {@code tanks[0].getFill() >= 100}. */
    public boolean hasEnoughWater() {
        return tanks[0].getFill() >= 100;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmoTowerBlockEntity be) {
        if (level.isClientSide) return;
        be.ensureNetworkInitialized();

        FluidTank in = be.tanks[0];
        FluidTank out = be.tanks[1];

        if (be.hasPower() && be.getEnergyStored() > 200 && be.hasEnoughWater() && out.getMaxFill() > out.getFill()
                && !AtmoFluids.isNone(out.getTankType())) {
            int convert = Math.min(out.getMaxFill(), in.getFill()) / 50;
            convert = Math.min(convert, out.getMaxFill() - out.getFill());

            in.setFill(in.getFill() - convert * 50); // dividing first, then multiplying, will remove any rounding issues
            out.setFill(out.getFill() + convert);
            be.setEnergyStored(be.getEnergyStored() - MAX_POWER / 100);
        }

        // subscribeToAllAround(tanks[0]) / sendFluidToAll(tanks[1]) ueber getConPos
        if (level instanceof ServerLevel server) {
            for (AtmoConnections.DirPos dp : AtmoConnections.conPos(pos, state)) {
                be.trySubscribe(in.getTankType(), server, dp.pos(), dp.dir());
                be.tryProvide(out, server, dp.pos(), dp.dir());
            }
        }

        // networkPackNT(50)
        be.setChanged();
        be.sendUpdateToClient();
    }

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== NBT (Original-Schluessel "water"/"heavyWater") ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "heavyWater");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "heavyWater");
    }

    // ==================== sonstiges ====================

    /** Original {@code getName() = "container.deuterium"}. */
    @Override protected Component getDefaultName() { return Component.translatable("block.space_m.machine_atmo_tower"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code TileEntityDeuteriumTower.getRenderBoundingBox}: x-1..x+2, y..y+10, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 10, worldPosition.getZ() + 2);
    }
}
