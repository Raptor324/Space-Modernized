package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.space_m.dim.duna.DunaDustStorm;

import net.minecraft.world.level.Level;

/**
 * R3-2: Original {@code WorldProviderDuna.isDaytime()} - waehrend eines Staubsturms ab Staerke 0.2 ist es auf der
 * Duna nie Tag (Schlafen erlaubt, Untote brennen nicht). Server: Sturmzustand, Client: synchronisierte Staerke.
 */
@Mixin(Level.class)
public abstract class DunaDayMixin {

    @Inject(method = "isDay", at = @At("HEAD"), cancellable = true)
    private void space_m$dunaDustStorm(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (level.dimension() == com.space_m.dim.SpaceDimensions.DUNA && DunaDustStorm.suppressesDaytime(level)) {
            cir.setReturnValue(false);
        }
    }
}
