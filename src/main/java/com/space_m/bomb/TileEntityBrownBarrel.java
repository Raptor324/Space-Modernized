package com.space_m.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code TileEntityBrownBarrel} ({@code tileentity_brown_barrel}): leere Blockentitaet mit
 * {@code getMaxRenderDistanceSquared = 65536}. Im Original registriert, {@code BrownBarrel} ist aber kein
 * {@code ITileEntityProvider} - der Typ existiert, wird vom Block nie erzeugt.
 * Original-Klasse: TileEntityBrownBarrel
 */
public class TileEntityBrownBarrel extends BlockEntity {

    public TileEntityBrownBarrel(BlockPos pos, BlockState state) {
        super(SpaceBombs.BROWN_BARREL_BE.get(), pos, state);
    }
}
