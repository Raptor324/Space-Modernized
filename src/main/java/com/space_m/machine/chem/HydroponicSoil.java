package com.space_m.machine.chem;

import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Original {@code MachineHydroponic.canSustainPlant}: Pflanzen stehen auf dem Kern oder einer seiner waagrecht
 * benachbarten Zellen ("nur die Bloecke direkt neben dem Kern tragen Pflanzen"). Im Port sind die Nachbarzellen HBMs
 * Phantomzellen, die kein {@code canSustainPlant} haben - deshalb fragen die Mixins {@code BushBlockHydroponicMixin}/
 * {@code CropBlockHydroponicMixin} und {@code SpaceCrops.BlockSpaceCrop} hier nach.
 */
public final class HydroponicSoil {

    private HydroponicSoil() {}

    /** true, wenn {@code soil} Kern einer Hydroponik ist oder eine Zelle derselben direkt neben dem Kern. */
    public static boolean sustains(BlockGetter level, BlockPos soil) {
        if (level == null) return false;
        if (level.getBlockState(soil).getBlock() instanceof MachineHydroponic) return true;

        BlockEntity be = level.getBlockEntity(soil);
        if (!(be instanceof IMultiblockPart part)) return false;
        BlockPos core = part.getControllerPos();
        if (core == null || !(level.getBlockState(core).getBlock() instanceof MachineHydroponic)) return false;

        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (soil.relative(d).equals(core)) return true;
        }
        return false;
    }
}
