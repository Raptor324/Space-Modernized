package com.space_m.machine.chem;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;

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
 * 1:1 NTM Space {@code TileEntityMachineMilkReformer}: 1 000 000 HE, Milch 64 000 mB -> {@code EMILK} 32 000,
 * {@code CMILK} 32 000, {@code CREAM} 32 000. Jeden Tick {@code refine()}: 10 000 HE, 100 mB Milch -> 50 + 35 + 15 mB.
 * Plaetze: 0 Batterie, 1/2 Milch-Kanister, 3/4, 5/6, 7/8 Ausgabe-Kanister, 9-10 frei. Anschluesse ({@code getConPos})
 * je zwei Felder an allen vier Seiten.
 * Original-Klasse: TileEntityMachineMilkReformer
 */
public class TileEntityMachineMilkReformer extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final long maxPower = 1_000_000;
    public static final int SLOTS = 11;

    public FluidTank[] tanks;

    public TileEntityMachineMilkReformer(BlockPos pos, BlockState state) {
        super(ChemMachines.MILK_REFORMER_BE.get(), pos, state, SLOTS, maxPower, maxPower);

        this.tanks = new FluidTank[4];
        this.tanks[0] = new FluidTank(ChemUtil.fluid("MILK"), 64_000);
        this.tanks[1] = new FluidTank(ChemUtil.fluid("EMILK"), 32_000);
        this.tanks[2] = new FluidTank(ChemUtil.fluid("CMILK"), 32_000);
        this.tanks[3] = new FluidTank(ChemUtil.fluid("CREAM"), 32_000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.milkreformer");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineMilkReformer be) {
        if (level.isClientSide) return;

        be.updateConnections();
        be.chargeFromBatterySlot(0);

        ItemStack[] slots = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) slots[i] = be.inventory.getStackInSlot(i);
        boolean changed = be.tanks[0].loadTank(1, 2, slots);

        be.refine();

        changed |= be.tanks[1].unloadTank(3, 4, slots);
        changed |= be.tanks[2].unloadTank(5, 6, slots);
        changed |= be.tanks[3].unloadTank(7, 8, slots);
        if (changed) for (int i = 0; i < SLOTS; i++) be.inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);

        for (ChemUtil.DirPos con : getConPos(pos)) {
            for (int i = 1; i < 4; i++) {
                if (be.tanks[i].getFill() > 0) be.tryProvide(be.tanks[i], level, con.pos(), con.dir());
            }
        }

        be.setChanged();
        be.sendUpdateToClient(); // networkPackNT(150)
    }

    /** Original {@code refine()}. */
    void refine() {
        if (getEnergyStored() < 10_000) return;
        if (tanks[0].getFill() < 100) return;
        if (tanks[1].getFill() + 50 > tanks[1].getMaxFill()) return;
        if (tanks[2].getFill() + 35 > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + 15 > tanks[3].getMaxFill()) return;

        setEnergyStored(getEnergyStored() - 10_000);
        tanks[0].setFill(tanks[0].getFill() - 100);
        tanks[1].setFill(tanks[1].getFill() + 50);
        tanks[2].setFill(tanks[2].getFill() + 35);
        tanks[3].setFill(tanks[3].getFill() + 15);
    }

    private void updateConnections() {
        for (ChemUtil.DirPos pos : getConPos(worldPosition)) {
            trySubscribe(tanks[0].getTankType(), level, pos.pos(), pos.dir());
        }
    }

    /** Original {@code getConPos()} (feste Weltachsen). */
    public static ChemUtil.DirPos[] getConPos(BlockPos p) {
        return new ChemUtil.DirPos[] {
                new ChemUtil.DirPos(p.offset(2, 0, 1), Direction.EAST),
                new ChemUtil.DirPos(p.offset(2, 0, -1), Direction.EAST),
                new ChemUtil.DirPos(p.offset(-2, 0, 1), Direction.WEST),
                new ChemUtil.DirPos(p.offset(-2, 0, -1), Direction.WEST),
                new ChemUtil.DirPos(p.offset(1, 0, 2), Direction.SOUTH),
                new ChemUtil.DirPos(p.offset(-1, 0, 2), Direction.SOUTH),
                new ChemUtil.DirPos(p.offset(1, 0, -2), Direction.NORTH),
                new ChemUtil.DirPos(p.offset(-1, 0, -2), Direction.NORTH)
        };
    }

    // ── Fluide ───────────────────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1], tanks[2], tanks[3] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean canConnect(Fluid type, Direction dir) {
        return dir != null && dir != Direction.DOWN;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT (Original "input", "m1".."m3") ───────────────────────────────────────────────────

    private static final String[] KEYS = { "input", "m1", "m2", "m3" };

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        for (int i = 0; i < 4; i++) tanks[i].writeToNBT(nbt, KEYS[i]);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        for (int i = 0; i < 4; i++) tanks[i].readFromNBT(nbt, KEYS[i]);
    }

    /** Original {@code getRenderBoundingBox}: x-2..x+3, y..y+7, z-2..z+3. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 7, worldPosition.getZ() + 3);
    }

    // ── Inventar / GUI ───────────────────────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return isEnergyProviderItem(stack);
        return slot == 1 || slot == 3 || slot == 5 || slot == 7;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerMachineMilkReformer(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }
}
