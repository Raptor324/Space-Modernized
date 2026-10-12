package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.space_m.machine.chem.HydroponicSoil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * R7-B: Original {@code MachineHydroponic.canSustainPlant} - Pflanzen in der Hydroponik stehen auf deren Kern oder
 * den Phantomzellen daneben (HBM-Zellen ohne {@code canSustainPlant}); {@code BushBlock.canSurvive} bleibt dort true.
 */
@Mixin(BushBlock.class)
public abstract class BushBlockHydroponicMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void space_m$hydroponicSoil(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (HydroponicSoil.sustains(level, pos.below())) cir.setReturnValue(true);
    }
}
