package com.space_m.machine.chem;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IPersistentNBT;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.machine.chem.recipe.ChemRecipes;
import com.space_m.machine.chem.recipe.CryoRecipe;

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
 * 1:1 NTM Space {@code TileEntityMachineCryoDistill}: 1 000 000 HE, Eingang 64 000 mB (Start {@code EARTHAIR}),
 * vier Ausgaenge je 24 000 mB ({@code NITROGEN}, {@code OXYGEN}, {@code KRYPTON}, {@code CARBONDIOXIDE}). Jeden Tick
 * {@code distill()}: 20 000 HE und 100 mB Eingang je Zyklus nach {@link CryoRecipe}. Plaetze: 0 Batterie,
 * 1/2, 3/4, 5/6, 8/9 Kanister der Ausgaenge 1-4, 7 Fluid-Identifikator, 10 frei (Original 11 Plaetze).
 * Original-Klasse: TileEntityMachineCryoDistill
 */
public class TileEntityMachineCryoDistill extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IPersistentNBT {

    public static final long maxPower = 1_000_000;
    public static final int SLOTS = 11;

    public FluidTank[] tanks;

    public TileEntityMachineCryoDistill(BlockPos pos, BlockState state) {
        super(ChemMachines.CRYO_DISTILL_BE.get(), pos, state, SLOTS, maxPower, maxPower);

        this.tanks = new FluidTank[5];
        this.tanks[0] = new FluidTank(ChemUtil.fluid("EARTHAIR"), 64_000);
        this.tanks[1] = new FluidTank(ChemUtil.fluid("NITROGEN"), 24_000);
        this.tanks[2] = new FluidTank(ChemUtil.fluid("OXYGEN"), 24_000);
        this.tanks[3] = new FluidTank(ChemUtil.fluid("KRYPTON"), 24_000);
        this.tanks[4] = new FluidTank(ChemUtil.fluid("CARBONDIOXIDE"), 24_000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.cryoDistillator");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineCryoDistill be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(0);

        ItemStack[] slots = be.slotArray();
        boolean changed = be.tanks[0].setType(7, slots);

        ChemUtil.DirPos[] con = be.getConPos();

        // Subscribe input tank (Strom ueber die Anschlusszellen)
        be.trySubscribe(be.tanks[0].getTankType(), level, con[0].pos(), con[0].dir());

        be.distill();

        changed |= be.tanks[1].unloadTank(1, 2, slots);
        changed |= be.tanks[2].unloadTank(3, 4, slots);
        changed |= be.tanks[3].unloadTank(5, 6, slots);
        changed |= be.tanks[4].unloadTank(8, 9, slots);
        if (changed) be.writeSlots(slots);

        for (int i = 1; i < 5; i++) {
            for (int o = 1; o < 5; o++) {
                if (be.tanks[i].getFill() > 0) {
                    be.tryProvide(be.tanks[i], level, con[o].pos(), con[o].dir());
                }
            }
        }

        be.setChanged();
        be.sendUpdateToClient(); // networkPackNT(15)
    }

    ItemStack[] slotArray() {
        ItemStack[] slots = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) slots[i] = inventory.getStackInSlot(i);
        return slots;
    }

    void writeSlots(ItemStack[] slots) {
        for (int i = 0; i < SLOTS; i++) inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);
    }

    /** Original {@code distill()}. */
    void distill() {
        CryoRecipe out = CryoRecipe.getOutput(level, tanks[0].getTankType());
        if (out == null) {
            for (int i = 1; i < 5; i++) tanks[i].setTankType(ChemUtil.none());
            return;
        }

        for (int i = 1; i < 5; i++) tanks[i].setTankType(ChemRecipes.type(out.getOutput(i - 1)));

        if (getEnergyStored() < 20_000) return;
        if (tanks[0].getFill() < 100) return;

        for (int i = 1; i < 5; i++) {
            if (tanks[i].getFill() + ChemRecipes.fill(out.getOutput(i - 1)) > tanks[i].getMaxFill()) return;
        }

        tanks[0].setFill(tanks[0].getFill() - 100);
        for (int i = 1; i < 5; i++) tanks[i].setFill(tanks[i].getFill() + ChemRecipes.fill(out.getOutput(i - 1)));

        setEnergyStored(getEnergyStored() - 20_000);
    }

    /** Original {@code getConPos()}: 0 Eingang, 1-4 Ausgaenge, 5 Strom ({@code rot = dir.getRotation(UP)}). */
    public ChemUtil.DirPos[] getConPos() {
        Direction dir = ChemUtil.dir(getBlockState());
        Direction rot = ChemUtil.rot(dir);
        BlockPos p = worldPosition;

        return new ChemUtil.DirPos[] {
                // Input
                ChemUtil.at(p, dir, -1, -3, -2, rot.getOpposite()),
                // Outputs
                ChemUtil.at(p, dir, 4, -2, -2, dir),
                ChemUtil.at(p, dir, 4, -1, -2, dir),
                ChemUtil.at(p, dir, 4, 1, -2, dir),
                ChemUtil.at(p, dir, 4, 2, -2, dir),
                // Power
                ChemUtil.at(p, dir, -2, -3, -2, rot.getOpposite()),
        };
    }

    // ── Fluide ───────────────────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1], tanks[2], tanks[3], tanks[4] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean canConnect(Fluid type, Direction dir) {
        return dir != null && dir != Direction.DOWN;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT (Original "input", "o1".."o4") ───────────────────────────────────────────────────

    private static final String[] KEYS = { "input", "o1", "o2", "o3", "o4" };

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        for (int i = 0; i < 5; i++) tanks[i].writeToNBT(nbt, KEYS[i]);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        for (int i = 0; i < 5; i++) tanks[i].readFromNBT(nbt, KEYS[i]);
    }

    /** Original {@code writeNBT}: nur wenn ein Tank etwas enthaelt. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = true;
        for (FluidTank t : tanks) if (t.getFill() != 0) empty = false;
        if (empty) return;
        for (int i = 0; i < 5; i++) tanks[i].writeToNBT(nbt, KEYS[i]);
    }

    /** Original {@code getRenderBoundingBox}: x-4..x+6, y-2..y+5, z-4..z+4. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 4, worldPosition.getY() - 2, worldPosition.getZ() - 4,
                worldPosition.getX() + 6, worldPosition.getY() + 5, worldPosition.getZ() + 4);
    }

    // ── Inventar / GUI ───────────────────────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return isEnergyProviderItem(stack);
        if (slot == 7) return stack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier;
        return slot == 1 || slot == 3 || slot == 5 || slot == 8;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerMachineCryoDistill(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }
}
