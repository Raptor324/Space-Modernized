package com.space_m.machine.chem;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IPersistentNBT;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.machine.chem.recipe.AlkylationRecipe;
import com.space_m.machine.chem.recipe.ChemRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineAlkylation}: 1 000 000 HE, Tanks Eingang 8 000 mB (Start
 * {@code CHLOROMETHANE}), Saeure 4 000 mB, Ausgaenge je 8 000 mB ({@code UNSATURATEDS}, {@code CHLORINE}). Alle 2 Ticks
 * {@code alkylate()}: Rezept nach Eingangsfluid, 4 000 HE und 100 mB Eingang je Zyklus; Anschluesse alle 10 Ticks
 * ({@code getConPos}: je drei Felder links/rechts, je zwei vorne/hinten). Inventar 11 Plaetze (ohne GUI, wie Original).
 * Tanks wandern beim Abbau mit (IPersistentNBT).
 * Original-Klasse: TileEntityMachineAlkylation
 */
public class TileEntityMachineAlkylation extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IPersistentNBT {

    public static final long maxPower = 1_000_000;

    public FluidTank[] tanks;

    public TileEntityMachineAlkylation(BlockPos pos, BlockState state) {
        super(ChemMachines.ALKYLATION_BE.get(), pos, state, 11, maxPower, maxPower);

        this.tanks = new FluidTank[4];
        this.tanks[0] = new FluidTank(ChemUtil.fluid("CHLOROMETHANE"), 8_000);
        this.tanks[1] = new FluidTank(ChemUtil.none(), 4_000);
        this.tanks[2] = new FluidTank(ChemUtil.fluid("UNSATURATEDS"), 8_000);
        this.tanks[3] = new FluidTank(ChemUtil.fluid("CHLORINE"), 8_000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.alkylation");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineAlkylation be) {
        if (level.isClientSide) return;

        if (level.getGameTime() % 10 == 0) be.updateConnections();

        if (level.getGameTime() % 2 == 0) be.alkylate();

        be.setChanged();
        be.sendUpdateToClient(); // networkPackNT(25)
    }

    /** Original {@code alkylate()}. */
    void alkylate() {
        AlkylationRecipe out = AlkylationRecipe.getOutput(level, tanks[0].getTankType());
        if (out == null) {
            tanks[2].setTankType(ChemUtil.none());
            tanks[3].setTankType(ChemUtil.none());
            return;
        }

        tanks[1].setTankType(ChemRecipes.type(out.getAcid()));
        tanks[2].setTankType(ChemRecipes.type(out.getOutput1()));
        tanks[3].setTankType(ChemRecipes.type(out.getOutput2()));

        int acid = ChemRecipes.fill(out.getAcid());
        int o1 = ChemRecipes.fill(out.getOutput1());
        int o2 = ChemRecipes.fill(out.getOutput2());

        if (getEnergyStored() < 4_000) return; // 40 kHE/s
        if (tanks[0].getFill() < 100) return;
        if (tanks[1].getFill() < acid) return;

        if (tanks[2].getFill() + o1 > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + o2 > tanks[3].getMaxFill()) return;

        tanks[0].setFill(tanks[0].getFill() - 100);
        tanks[1].setFill(tanks[1].getFill() - acid);
        tanks[2].setFill(tanks[2].getFill() + o1);
        tanks[3].setFill(tanks[3].getFill() + o2);

        setEnergyStored(getEnergyStored() - 4_000);
    }

    private void updateConnections() {
        for (ChemUtil.DirPos pos : getConPos()) {
            trySubscribe(tanks[0].getTankType(), level, pos.pos(), pos.dir());
            trySubscribe(tanks[1].getTankType(), level, pos.pos(), pos.dir());
            if (tanks[2].getFill() > 0) tryProvide(tanks[2], level, pos.pos(), pos.dir());
            if (tanks[3].getFill() > 0) tryProvide(tanks[3], level, pos.pos(), pos.dir());
        }
    }

    /** Original {@code getConPos()} ({@code rot = dir.getRotation(DOWN)}). */
    public ChemUtil.DirPos[] getConPos() {
        Direction dir = ChemUtil.dir(getBlockState());
        Direction rot = dir.getCounterClockWise();
        BlockPos p = worldPosition;
        int dx = dir.getStepX(), dz = dir.getStepZ(), rx = rot.getStepX(), rz = rot.getStepZ();

        return new ChemUtil.DirPos[] {
                new ChemUtil.DirPos(p.offset(rx * 2, 0, rz * 2), rot),
                new ChemUtil.DirPos(p.offset(rx * 2 + dx * 2, 0, rz * 2 + dz * 2), rot),
                new ChemUtil.DirPos(p.offset(rx * 2 - dx * 2, 0, rz * 2 - dz * 2), rot),

                new ChemUtil.DirPos(p.offset(rx + dx * 3, 0, rz + dz * 3), dir),
                new ChemUtil.DirPos(p.offset(-rx + dx * 3, 0, -rz + dz * 3), dir),

                new ChemUtil.DirPos(p.offset(-rx * 2, 0, -rz * 2), rot.getOpposite()),
                new ChemUtil.DirPos(p.offset(-rx * 2 + dx * 2, 0, -rz * 2 + dz * 2), rot.getOpposite()),
                new ChemUtil.DirPos(p.offset(-rx * 2 - dx * 2, 0, -rz * 2 - dz * 2), rot.getOpposite()),

                new ChemUtil.DirPos(p.offset(rx - dx * 3, 0, rz - dz * 3), dir.getOpposite()),
                new ChemUtil.DirPos(p.offset(-rx - dx * 3, 0, -rz - dz * 3), dir.getOpposite()),
        };
    }

    // ── Fluide ───────────────────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[2], tanks[3] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0], tanks[1] }; }

    /** Original {@code canConnect}: nicht von unten. */
    @Override
    public boolean canConnect(Fluid type, Direction dir) {
        return dir != null && dir != Direction.DOWN;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
    }

    /** Original {@code writeNBT}: nur wenn ein Tank etwas enthaelt. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        if (tanks[0].getFill() == 0 && tanks[1].getFill() == 0 && tanks[2].getFill() == 0 && tanks[3].getFill() == 0) return;
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
    }

    /** Original {@code getRenderBoundingBox}: x-3..x+3, y..y+3, z-3..z+3. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 3, worldPosition.getY() + 3, worldPosition.getZ() + 3);
    }

    // ── kein GUI ─────────────────────────────────────────────────────────────────────────────

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
    @Override public Component getDisplayName() { return getDefaultName(); }
}
