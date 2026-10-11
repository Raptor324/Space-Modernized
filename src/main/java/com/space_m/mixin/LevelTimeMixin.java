package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.space_m.time.CelestialTime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Original {@code WorldProviderCelestial.getWorldTime}: Planeten-/Monddimensionen haben eine eigene Ortszeit
 * ({@code CelestialBodyData.time}); Vanilla liest in allen Nicht-Overworld-Dimensionen die Overworld-Zeit
 * ({@code DerivedLevelData}). Nur Serverseite - der Client erhaelt die Ortszeit ueber das Vanilla-Zeitpaket
 * ({@code ClientboundSetTimePacket} mit {@code level.getDayTime()}) und fuehrt sie selbst weiter.
 * {@code LevelAccessor.dayTime()} (Sonnenwinkel/Mondphase) liest die Leveldaten direkt; das gleicht
 * {@code CelestialTime.celestialAngle} ueber den Dimensionstyp aus.
 */
@Mixin(Level.class)
public abstract class LevelTimeMixin {

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void space_m$localDayTime(CallbackInfoReturnable<Long> cir) {
        if ((Object) this instanceof ServerLevel level && CelestialTime.hasLocalTime(level)) {
            Long local = CelestialTime.getLocalTimeOrNull(level);
            if (local != null) cir.setReturnValue(local);
        }
    }
}
