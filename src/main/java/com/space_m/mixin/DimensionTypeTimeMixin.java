package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.space_m.time.CelestialTime;

import net.minecraft.world.level.dimension.DimensionType;

/**
 * Original {@code WorldProviderCelestial.calculateCelestialAngle}: Sonnenwinkel aus der Tageslaenge des
 * Himmelskoerpers ({@code rotationalPeriod / (1 - 1/orbitalPeriod)}) statt fester 24000 Ticks. Alle Vanilla-Pfade
 * (Himmel, Helligkeit, {@code skyDarken}, Mob-Spawns, Tageslichtsensor) laufen ueber
 * {@code LevelTimeAccess.getTimeOfDay -> DimensionType.timeOfDay}; der Koerper ergibt sich aus
 * {@code DimensionType.effectsLocation()} ({@code space_m:<koerper>}, Overworld = Kerbin). Beide Seiten.
 */
@Mixin(DimensionType.class)
public abstract class DimensionTypeTimeMixin {

    @Inject(method = "timeOfDay", at = @At("HEAD"), cancellable = true)
    private void space_m$celestialAngle(long dayTime, CallbackInfoReturnable<Float> cir) {
        float angle = CelestialTime.celestialAngle((DimensionType) (Object) this, dayTime);
        if (!Float.isNaN(angle)) cir.setReturnValue(angle);
    }

    @Inject(method = "moonPhase", at = @At("HEAD"), cancellable = true)
    private void space_m$moonPhase(long dayTime, CallbackInfoReturnable<Integer> cir) {
        int phase = CelestialTime.moonPhase((DimensionType) (Object) this);
        if (phase >= 0) cir.setReturnValue(phase);
    }
}
