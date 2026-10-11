package com.space_m.dim.tekto.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.blocks.BlockVinylVine} ({@code vinyl_vines} "Vinyl Vines"): Vanilla-Ranke mit Textur
 * {@code vine}, ohne Biomfaerbung (Farbe weiss), haelt zusaetzlich, wenn darunter kein undurchsichtiger Block oder
 * Kunststoffblaetter liegen ({@code canBlockStay}).
 */
public class BlockVinylVine extends VineBlock {

    public BlockVinylVine(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return !below.isSolidRender(level, pos.below()) || below.getBlock() instanceof LeavesBlock || super.canSurvive(state, level, pos);
    }
}
