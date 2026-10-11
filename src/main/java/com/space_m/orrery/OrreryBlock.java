package com.space_m.orrery;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Planetarium (Orrery), 1:1 NTM Space {@code com.hbm.blocks.generic.BlockOrrery}: unsichtbarer Block
 * ({@code getRenderType() = -1}), die bewegte Darstellung des Sonnensystems zeichnet der BER
 * ({@code com.space_m.client.orrery.OrreryRenderer}). Werte aus {@code ModBlocks}: Material eisen, Haerte 10,
 * Widerstand 20, Tab Maschinen, Partikel-Textur {@code block_steel_machine}.
 * Original-Klasse: BlockOrrery
 */
public class OrreryBlock extends BaseEntityBlock {

    public OrreryBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrreryBlockEntity(pos, state);
    }

    /** Original {@code getRenderType() = -1}. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<OrreryBlock> CODEC = simpleCodec(OrreryBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
