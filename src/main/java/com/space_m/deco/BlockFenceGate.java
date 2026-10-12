package com.space_m.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockFenceGate} ({@code fence_gate} "Chainlink Fence Gate", Material.iron,
 * Haerte 5, Widerstand 1, Textur {@code fence_metal_lower/_upper}): zweihohe Maschendraht-Tuer auf Basis von
 * {@code BlockModDoor} - per Hand und Redstone zu oeffnen, Klang {@code hbm:block.fenceGate} (Tonhoehe 0.9-1.0).
 *
 * <p>Geometrie (Original {@code func_150011_b}, Metadaten 0..3 = Blickrichtung OST/SUED/WEST/NORD, Bit 16 =
 * Scharnier rechts): geschlossen eine 1 px dicke Gitterwand mittig im Block; offen schwenkt sie um die Blockmitte
 * nach aussen (Modell 1 px an der Kante, halb ausserhalb des Blocks) und hat keine Kollision.
 * Auswahl/Strahltest benutzen immer die geschlossene Wand ({@code interacting}-Schalter des Originals).
 * Gegenstand: {@link ItemFenceGate} ({@code ItemModDoor}), Beute nur aus der unteren Haelfte.
 */
// Original-Klasse: BlockFenceGate
public class BlockFenceGate extends DoorBlock {

    private static final VoxelShape X_PANEL = Block.box(7.5D, 0.0D, 0.0D, 8.5D, 16.0D, 16.0D);
    private static final VoxelShape Z_PANEL = Block.box(0.0D, 0.0D, 7.5D, 16.0D, 16.0D, 8.5D);

    public BlockFenceGate(Properties props) {
        //? if < 1.21.1 {
        super(props, BlockSetType.IRON);
        //?} else {
        /*super(BlockSetType.IRON, props);
        *///?}
    }

    private static VoxelShape closed(BlockState state) {
        Direction d = state.getValue(FACING);
        return d.getAxis() == Direction.Axis.X ? X_PANEL : Z_PANEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return closed(state);
    }

    /** Original {@code getCollisionBoundingBoxFromPool}: offen keine Kollision. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(OPEN) ? Shapes.empty() : closed(state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(spaceUse(state, world, pos, player));
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return spaceUse(state, world, pos, player);
    }
    private InteractionResult spaceUse(BlockState state, Level world, BlockPos pos, Player player) {
    *///?}
        state = state.cycle(OPEN);
        world.setBlock(pos, state, 10);
        playGateSound(world, pos);
        world.gameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public void setOpen(Entity entity, Level world, BlockState state, BlockPos pos, boolean open) {
        if (state.is(this) && state.getValue(OPEN) != open) {
            world.setBlock(pos, state.setValue(OPEN, open), 10);
            playGateSound(world, pos);
            world.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
    }

    /** Wie {@code DoorBlock.neighborChanged}, mit dem Klang des Originals ({@code func_150014_a}). */
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        boolean flag = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.relative(
                state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
        if (!this.defaultBlockState().is(block) && flag != state.getValue(POWERED)) {
            if (flag != state.getValue(OPEN)) {
                playGateSound(world, pos);
                world.gameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
            world.setBlock(pos, state.setValue(POWERED, flag).setValue(OPEN, flag), 2);
        }
    }

    private static void playGateSound(Level world, BlockPos pos) {
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SpaceDeco.BLOCK_FENCEGATE.get(), SoundSource.BLOCKS, 1.0F,
                world.random.nextFloat() * 0.1F + 0.9F);
    }
}
