package com.space_m.machine.misc;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code BlockFurnaceSpace} ({@code furnace} / {@code lit_furnace}): Ersatz des Vanilla-Ofens, der vor dem
 * Anzuenden atmet ({@link TileEntityFurnaceSpace}). Jeder von einem Spieler gesetzte Vanilla-Ofen wird sofort ersetzt
 * ({@code ModEventHandler.onBlockPlaced}, "sneaky sneaky space furnace", s. {@link MiscEvents}). Wie im Original wechselt
 * der Ofen beim An-/Ausgehen zwischen den Bloecken {@code furnace} und {@code lit_furnace}
 * ({@link #updateFurnaceBlockState}, Inventar bleibt erhalten); Drop und Pick-Block = Vanilla-Ofen
 * ({@code BlockFurnace.getItemDropped/getItem}). Nicht abdichtend ({@code isSealed = false}, nicht im Tag
 * {@code space_m:sealable}).
 * Original-Klasse: BlockFurnaceSpace
 */
public class BlockFurnaceSpace extends FurnaceBlock {

    /** Original {@code BlockFurnace.field_149934_M} (keepInventory beim Blockwechsel). */
    private static boolean keepInventory = false;

    private final boolean lit;

    public BlockFurnaceSpace(boolean lit, Properties properties) {
        super(properties);
        this.lit = lit;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(LIT, lit));
    }

    /** Original {@code updateFurnaceBlockState}: Blockwechsel furnace <-> lit_furnace mit gleichem Metadatum und Blockentity. */
    public static void updateFurnaceBlockState(boolean lit, Level world, BlockPos pos) {
        BlockState old = world.getBlockState(pos);
        BlockEntity tileentity = world.getBlockEntity(pos);
        if (tileentity == null) return;

        //? if < 1.21.1 {
        CompoundTag data = tileentity.saveWithoutMetadata();
        //?} else {
        /*CompoundTag data = tileentity.saveWithoutMetadata(world.registryAccess());
        *///?}

        keepInventory = true;
        Block target = lit ? MiscBlocks.LIT_FURNACE.get() : MiscBlocks.FURNACE.get();
        world.setBlock(pos, target.defaultBlockState().setValue(FACING, old.getValue(FACING)).setValue(LIT, lit), 3);
        keepInventory = false;

        BlockEntity fresh = world.getBlockEntity(pos);
        if (fresh != null) {
            //? if < 1.21.1 {
            fresh.load(data);
            //?} else {
            /*fresh.loadWithComponents(data, world.registryAccess());
            *///?}
            fresh.setChanged();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (keepInventory) {
            if (state.hasBlockEntity() && !state.is(newState.getBlock())) level.removeBlockEntity(pos);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFurnaceSpace(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, MiscBlockEntities.FURNACE_SPACE.get(), (l, p, s, be) -> {
            AbstractFurnaceBlockEntity.serverTick(l, p, s, be);
            BlockState now = l.getBlockState(p);
            if (now.getBlock() instanceof BlockFurnaceSpace furnace && now.hasProperty(LIT) && now.getValue(LIT) != furnace.lit) {
                updateFurnaceBlockState(now.getValue(LIT), l, p);
            }
        });
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof TileEntityFurnaceSpace furnace) {
            player.openMenu(furnace);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    /** Original {@code getItem}: Vanilla-Ofen. */
    @Override
    //? if < 1.21.1 {
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    //?} else {
    /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
    *///?}
        return new ItemStack(Items.FURNACE);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<FurnaceBlock> CODEC = simpleCodec(p -> new BlockFurnaceSpace(false, p));
    @Override public com.mojang.serialization.MapCodec<FurnaceBlock> codec() { return CODEC; }
    *///?}

    /** Lichtstufe des {@code lit_furnace} (Original {@code setLightLevel(0.875F)} = 13). */
    public boolean isLitBlock() {
        return lit;
    }
}
