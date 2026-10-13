package com.space_m.fork.trinket;

import javax.annotation.Nullable;

import com.hbm_m.item.TrinketBlockItem;
import com.hbm_m.sound.HbmSoundsNT;
import com.space_m.SpaceMod;
import com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Space {@code BlockBobble} / {@code BlockPlushie} fuer die Fork-Typen (Aufbau wie HBM {@code TrinketBlock}): 16
 * Drehstufen, Typ im Blockentity, Drop mit Typ nur ausserhalb des Kreativmodus. Wackelkopf oeffnet die Tafel,
 * Plueschtier quietscht mit seinem Klang (FAT TETO halbe Tonhoehe); Butter auf Teto macht FAT TETO.
 */
public class ForkTrinketBlock extends BaseEntityBlock {

    public enum Kind { BOBBLE, PLUSHIE }

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    /** Original setBlockBounds 5.5/16 .. 0.625 (wie HBM). */
    private static final VoxelShape BOBBLE_SHAPE = Block.box(5.5, 0, 5.5, 10.5, 10, 10.5);

    public final Kind kind;

    public ForkTrinketBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    public int typeCount() {
        return kind == Kind.BOBBLE ? ForkTrinkets.ForkBobbleType.values().length : ForkPlushieType.values().length;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(ROTATION, Mth.floor((ctx.getRotation() + 180.0F) * 16.0F / 360.0F + 0.5D) & 15);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof ForkTrinketBlockEntity te) {
            te.setType(TrinketBlockItem.getType(stack) % typeCount());
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind == Kind.BOBBLE ? BOBBLE_SHAPE : Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForkTrinketBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return kind == Kind.PLUSHIE ? createTickerHelper(type, ForkTrinkets.TRINKET_BE.get(), ForkTrinketBlockEntity::tick) : null;
    }

    private ItemStack stackFor(BlockGetter level, BlockPos pos) {
        int type = level.getBlockEntity(pos) instanceof ForkTrinketBlockEntity te ? te.type : 0;
        return TrinketBlockItem.make(asItem(), type);
    }

    @Override
    //? if < 1.21.1 {
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    //?} else {
    /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
    *///?}
        return stackFor(level, pos);
    }

    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropTrinket(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropTrinket(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    /** Original {@code onBlockHarvested}: Gegenstand mit Typ, ohne Bewegung, nicht im Kreativmodus. */
    private void dropTrinket(Level level, BlockPos pos, Player player) {
        if (!player.isCreative() && !level.isClientSide) {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stackFor(level, pos));
            item.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(item);
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult r = activate(level, pos, player, stack);
        return r == InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, ItemStack.EMPTY);
    }
    *///?}

    /** Original {@code onBlockActivated} (Wackelkopf: Tafel; Plueschtier: Butter -> FAT TETO, sonst Quietschen). */
    public InteractionResult activate(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!(level.getBlockEntity(pos) instanceof ForkTrinketBlockEntity te)) return InteractionResult.PASS;

        if (kind == Kind.PLUSHIE) {
            ForkPlushieType type = ForkTrinkets.safe(ForkPlushieType.class, te.type);

            if (type == ForkPlushieType.TETO && !held.isEmpty() && held.getItem() == com.space_m.item.SpaceMaterialItems.BUTTER.get()) {
                te.squishTimer = 11;
                held.shrink(1); // Original stackSize-- auch im Kreativmodus
                if (!level.isClientSide) {
                    te.setType(ForkPlushieType.FATO.ordinal());
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HbmSoundsNT.get("player.groan"), SoundSource.BLOCKS, 1F, 2F);
                } else {
                    te.type = ForkPlushieType.FATO.ordinal();
                }
                return InteractionResult.SUCCESS;
            }

            if (level.isClientSide) {
                te.squishTimer = 11;
            } else if (type.sound != null) {
                SoundEvent ev = BuiltInRegistries.SOUND_EVENT.get(SpaceMod.id(type.sound));
                if (ev != null) level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ev, SoundSource.BLOCKS,
                        0.25F, type == ForkPlushieType.FATO ? 0.5F : 1F);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.fork.trinket.client.ForkBobbleScreen.open(te.type));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<ForkTrinketBlock> CODEC = com.hbm_m.platform.BlockCodecs.unsupported(ForkTrinketBlock.class);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
