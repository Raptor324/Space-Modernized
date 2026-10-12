package com.space_m.orbit.propulsion;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code BlockStationPropulsionCreative} ({@code propulsion_creative}, "Creative Station Propulsion"):
 * einfacher Block (Textur {@code block_steel}) mit {@link TileEntityStationPropulsionCreative}.
 * Original-Klasse: BlockStationPropulsionCreative
 */
public class BlockStationPropulsionCreative extends BaseEntityBlock {

    public BlockStationPropulsionCreative(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityStationPropulsionCreative(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PropulsionBlockEntities.PROPULSION_CREATIVE.get(), TileEntityStationPropulsionCreative::tick);
    }

    /** Original {@code BlockContainer}: normaler Wuerfel. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockStationPropulsionCreative> CODEC = simpleCodec(BlockStationPropulsionCreative::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
