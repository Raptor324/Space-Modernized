package com.space_m.machine.atmo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original {@code getConPos()} der drei 2x2-Maschinen (Tower, Kompressor, Emitter): alle acht Felder, die waagrecht an
 * den 2x2-Sockel grenzen, jeweils mit der Richtung vom Sockel weg ({@code DirPos}). Statt die Original-Formeln je
 * Ausrichtung nachzurechnen, werden sie aus der (1:1 uebersetzten) Struktur abgeleitet - fuer einen 2x2-Sockel ist das
 * exakt dieselbe Menge (Muster HBM {@code MachineDeuteriumTowerBlockEntity}).
 */
public final class AtmoConnections {
    private AtmoConnections() {}

    public record DirPos(BlockPos pos, Direction dir) {}

    /** Felder der untersten Ebene (Kern + Nebenzellen auf Kernhoehe). */
    public static Set<BlockPos> base(BlockPos core, BlockState state) {
        Set<BlockPos> set = new HashSet<>();
        set.add(core);
        if (state.getBlock() instanceof DummyableMachineBlock block) {
            MultiblockStructureHelper helper = block.getStructureHelper();
            Direction facing = state.getValue(DummyableMachineBlock.FACING);
            for (BlockPos local : helper.getPartOffsets()) {
                BlockPos world = helper.getRotatedPos(core, local, facing);
                if (world.getY() == core.getY()) set.add(world);
            }
        }
        return set;
    }

    /** Original {@code getConPos()}: waagrechte Nachbarn des Sockels, die nicht selbst zum Sockel gehoeren. */
    public static List<DirPos> conPos(BlockPos core, BlockState state) {
        Set<BlockPos> base = base(core, state);
        List<DirPos> list = new ArrayList<>(8);
        for (BlockPos b : base) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos at = b.relative(d);
                if (base.contains(at)) continue;
                list.add(new DirPos(at, d));
            }
        }
        return list;
    }
}
