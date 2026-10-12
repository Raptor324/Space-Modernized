package com.space_m.machine.mining;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityDishControl}: merkt sich den StarDar-Kern ({@code linkPosition}, {@code isLinked});
 * beim ersten Server-Tick wird eine gespeicherte Verknuepfung erneut geprueft ({@code establishLink}). Verknuepfung ueber
 * den Reaktorfuehler ({@link #tryLink}: x/y/z aus dem Fuehler-NBT -> Kern des StarDar). Der Renderer zeigt das Laufwerk,
 * wenn der verknuepfte StarDar eines hat ({@link #starDarHasDisk}).
 * Original-Klasse: TileEntityDishControl
 */
public class TileEntityDishControl extends BaseMachineBlockEntity {

    public BlockPos linkPosition = BlockPos.ZERO;
    public boolean isLinked;
    private boolean foundLink = false;

    public TileEntityDishControl(BlockPos pos, BlockState state) {
        super(DishMachines.DISH_CONTROL_BE.get(), pos, state, 0, 0, 0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dishControl");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDishControl be) {
        if (level.isClientSide) return;

        if (!be.foundLink && be.isLinked) {
            be.isLinked = be.establishLink(be.linkPosition);
            be.foundLink = true;
            be.setChanged();
        }

        if (level.getGameTime() % 20 == 0) be.sendUpdateToClient(); // networkPackNT(15)
    }

    /** Original {@code getLinkedDishClientSafe()} + {@code starDarHasDisk()}: Laufwerksplatz 0 des StarDar belegt. */
    public boolean starDarHasDisk() {
        if (!isLinked || level == null) return false;
        BlockEntity te = level.getBlockEntity(linkPosition);
        return te instanceof BaseMachineBlockEntity m && m.getInventory().getSlots() > 0 && !m.getInventory().getStackInSlot(0).isEmpty();
    }

    /** Original {@code TryLink(stack)}. */
    public void tryLink(ItemStack stack) {
        isLinked = linkWithSensor(stack);
        setChanged();
        sendUpdateToClient();
    }

    private boolean linkWithSensor(ItemStack stack) {
        CompoundTag tag = com.hbm_m.platform.StackNbt.read(stack);
        if (tag != null) {
            // Get the StarDar coordinates from the sensor
            return establishLink(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
        }
        return false;
    }

    /** Original {@code establishLink}: Block muss {@code machine_stardar} sein, Kern ({@code findCore}) mit BE. */
    private boolean establishLink(BlockPos pos) {
        if (level == null) return false;
        BlockPos core = DishMachines.stardarCore(level, pos);
        if (core != null) {
            // Dish linked successfully
            linkPosition = core;
            return true;
        }
        return false;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("isLinked", isLinked);
        if (!linkPosition.equals(BlockPos.ZERO)) {
            nbt.putInt("linkPosX", linkPosition.getX());
            nbt.putInt("linkPosY", linkPosition.getY());
            nbt.putInt("linkPosZ", linkPosition.getZ());
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        isLinked = nbt.getBoolean("isLinked");
        linkPosition = new BlockPos(nbt.getInt("linkPosX"), nbt.getInt("linkPosY"), nbt.getInt("linkPosZ"));
    }

    /** Original {@code getRenderBoundingBox}: x-1..x+2, y..y+1, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
    @Override public Component getDisplayName() { return getDefaultName(); }

    /** Fuer GameTests: Kern hinter einer Phantomzelle. */
    static BlockPos coreOf(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof IMultiblockPart part && part.getControllerPos() != null ? part.getControllerPos() : pos;
    }
}
