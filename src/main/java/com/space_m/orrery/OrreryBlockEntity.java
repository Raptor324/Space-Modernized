package com.space_m.orrery;

import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code BlockOrrery.TileEntityOrrery}: keine Daten, nur der grosse Render-Rahmen
 * (x-49..x+50, y-19..y+20, z-49..z+50) und 256 Bloecke Sichtweite ({@code getMaxRenderDistanceSquared = 65536}).
 * Mapping-Name {@code tileentity_orrery} -> ID {@code orrery}.
 * Original-Klasse: TileEntityOrrery
 */
public class OrreryBlockEntity extends BlockEntity {

    private AABB bb = null;

    public OrreryBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.ORRERY.get(), pos, state);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) {
            BlockPos p = getBlockPos();
            bb = new AABB(
                p.getX() - 49,
                p.getY() - 19,
                p.getZ() - 49,
                p.getX() + 50,
                p.getY() + 20,
                p.getZ() + 50
            );
        }

        return bb;
    }
}
