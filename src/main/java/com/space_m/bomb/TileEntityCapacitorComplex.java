package com.space_m.bomb;

import com.hbm_m.blockentity.machines.MachineCapacitorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kondensator-Blockentitaet fuer {@code capacitor_complex} (Original {@code TileEntityCapacitor(power)}): Logik der
 * HBM-{@link MachineCapacitorBlockEntity} (Kapazitaet aus dem Block, Laden/Entladen, Schienenkette, ROR-Werte), aber
 * mit eigenem Blockentitaets-Typ, weil der HBM-Typ nur die HBM-Kondensatorbloecke zulaesst.
 */
public class TileEntityCapacitorComplex extends MachineCapacitorBlockEntity {

    public TileEntityCapacitorComplex(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    /** Eigener Typ (Speicher-ID, Gueltigkeitspruefung, Ticker-Pruefung laufen ueber {@code getType()}). */
    @Override
    public BlockEntityType<?> getType() {
        return SpaceBombs.CAPACITOR_COMPLEX_BE.get();
    }
}
