package com.space_m.rocket.transport;

import org.jetbrains.annotations.Nullable;

import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code CombatDropPod} ({@code combat_drop}): Block der gelandeten Kampfkapsel, nur TESR
 * ({@code getRenderType = -1}, nicht undurchsichtig). Drop: 16 Stahlbarren (Loot-Tabelle).
 * Original-Klasse: CombatDropPod
 */
public class CombatDropPodBlock extends BaseEntityBlock {

    public CombatDropPodBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CombatDropPodBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.COMBAT_DROP_POD.get(), CombatDropPodBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<CombatDropPodBlock> CODEC = simpleCodec(CombatDropPodBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
