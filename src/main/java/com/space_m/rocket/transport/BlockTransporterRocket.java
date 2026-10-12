package com.space_m.rocket.transport;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.item.SpaceItems;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code BlockTransporterRocket} ({@code transporter_rocket}): {@code getDimensions {1, 0, 1, 1, 1, 2}},
 * Offset 1; {@code fillSpace}: Sockel {0,0,1,1,1,2}, Saeule {1,0,1,1,0,0}, Tankstand {1,0,1,1,-2,2}; Anschluesse
 * ({@code makeExtra}) an den vier Ecken um den Kern und an den beiden Treibstoffzellen (dir +-1, rot -2).
 * Rechtsklick oeffnet die GUI, ausser mit dem Transporter-Linker in der Hand (dann handelt das Item).
 * Beim Setzen wird der Chunk erzwungen (BE, erster Tick), beim Abbau freigegeben und die Verknuepfung geloest.
 * Original-Klasse: BlockTransporterRocket
 */
public class BlockTransporterRocket extends DummyableMachineBlock {

    public BlockTransporterRocket(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(0, 0, 1, 1, 1, 2)
                .box(1, 0, 1, 1, 0, 0)
                .box(1, 0, 1, 1, -2, 2)
                .extra(1, 0, 1)
                .extra(1, 0, -1)
                .extra(-1, 0, 1)
                .extra(-1, 0, -1)
                .extra(1, 0, -2)
                .extra(-1, 0, -2)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransporterRocketBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.TRANSPORTER_ROCKET.get(), TransporterBlockEntityBase::tick);
    }

    /** Original {@code getRenderType() = -1} (TESR). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(InteractionHand.MAIN_HAND));
    }
    *///?}

    /** Original {@code onBlockActivated}: mit Linker -> false (Item handelt), sonst {@code standardOpenBehavior}. */
    private InteractionResult onActivated(Level level, BlockPos pos, Player player, ItemStack held) {
        boolean linker = !held.isEmpty() && held.getItem() == SpaceItems.TRANSPORTER_LINKER.get();
        if (linker) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof TransporterRocketBlockEntity be && player instanceof ServerPlayer sp) {
            MenuRegistry.openExtendedMenu(sp, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    /** Original {@code breakBlock} (Kern, meta >= 12): Chunk freigeben, Verknuepfung loesen. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            TransporterChunkLoader.unforceChunk(server, pos);

            if (level.getBlockEntity(pos) instanceof TransporterBlockEntityBase te) {
                te.unlinkTransporter();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockTransporterRocket> CODEC = simpleCodec(BlockTransporterRocket::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
