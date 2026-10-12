package com.space_m.machine.dyson;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.crates.TungstenCrateBlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code IDysonConverter}: Ziel des Dyson-Empfaengerstrahls.
 *
 * <p>Zusatz (Port): die Tungsten-Kiste ist ein HBM-Block ({@code TileEntityCrateTungsten implements IDysonConverter} im
 * Original) - sie wird ueber {@link #of(BlockEntity)} angebunden, ohne HBM zu aendern ({@code addEnergy(x, y, z,
 * energy / 5000, NORTH)} = {@code TungstenCrateBlockEntity.addLaserEnergy}).
 * Original-Klasse: IDysonConverter
 */
public interface IDysonConverter {

    /** Attempt to provide power at a given position on the converter, return false if power can't be received at this location. */
    boolean provideEnergy(int x, int y, int z, long energy);

    /** Tell Dyson receivers our energy conversion limit. */
    long maximumEnergy();

    /**
     * Wandler eines Blockentities: eigene Wandler direkt, HBM-Mehrblockteile ueber ihren Kern (Original
     * {@code BlockDummyable.findCore}), die HBM-Tungsten-Kiste ueber einen Adapter; sonst null.
     */
    @Nullable
    static IDysonConverter of(@Nullable BlockEntity te) {
        if (te instanceof IDysonConverter c) return c;
        if (te instanceof TungstenCrateBlockEntity crate) {
            return new IDysonConverter() {
                @Override
                public boolean provideEnergy(int x, int y, int z, long energy) {
                    Level level = crate.getLevel();
                    if (level != null) crate.addLaserEnergy(level, new BlockPos(x, y, z), energy / 5000, Direction.NORTH);
                    return true;
                }

                @Override
                public long maximumEnergy() {
                    return Long.MAX_VALUE;
                }
            };
        }
        return null;
    }

    /** Original {@code findCore}: Kern-Blockentity eines Mehrblockteils, sonst das Blockentity selbst. */
    @Nullable
    static BlockEntity core(Level level, @Nullable BlockEntity te) {
        if (te instanceof IMultiblockPart part && part.getControllerPos() != null) {
            BlockEntity core = level.getBlockEntity(part.getControllerPos());
            if (core != null) return core;
        }
        return te;
    }
}
