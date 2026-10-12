package com.space_m.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NTM {@code BlockFF} als {@code ff} (Textur {@code code}, kein Creative-Tab): Kraftfeld-Prototyp mit
 * {@link TileEntityFF}. Im Original steht {@code ff} in {@code ModBlocks}, wird aber nie per
 * {@code GameRegistry.registerBlock} angemeldet - kein Item, nur per Befehl setzbar.
 * Original-Klasse: BlockFF
 */
public class BlockFF extends BaseEntityBlock {

    public BlockFF(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFF(pos, state);
    }

    /** Original {@code getRenderType() = -1} (nur TESR); der Block selbst zeigt die Textur {@code code}. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBombs.FORCEFIELD_BE.get(), (lvl, pos, st, be) -> be.tick());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockFF> CODEC = simpleCodec(BlockFF::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
