package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.space_m.time.CelestialTime;
import com.space_m.time.CelestialWeather;

import net.minecraft.server.level.ServerLevel;

/**
 * Zeit und Wetter je Himmelskoerper auf der Serverseite (Original {@code WorldProviderCelestial}):
 * <ul>
 *   <li>{@code tickTime}: Ortszeit +1 je Tick bei {@code doDaylightCycle} (Forge 1.7.10 {@code provider.setWorldTime(getWorldTime()+1)});
 *       Nicht-Overworld-Level ticken in Vanilla keine Zeit ({@code tickTime=false}).</li>
 *   <li>{@code setDayTime}: {@code /time set|add} und Schlaf schreiben die Ortszeit ({@code provider.setWorldTime}).</li>
 *   <li>{@code advanceWeatherCycle}: Wetter aus {@code CBT_Weather} des Koerpers ({@code updateWeather}).</li>
 *   <li>{@code wakeUpAllPlayers}: {@code resetRainAndThunder} (Wetter klar, Ortszeit auf Morgen).</li>
 * </ul>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelTimeMixin {

    @Inject(method = "tickTime", at = @At("HEAD"))
    private void space_m$tickLocalTime(CallbackInfo ci) {
        CelestialTime.tickLocalTime((ServerLevel) (Object) this);
    }

    @Inject(method = "setDayTime", at = @At("HEAD"), cancellable = true)
    private void space_m$setLocalTime(long time, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (CelestialTime.hasLocalTime(level)) {
            CelestialTime.setLocalTime(level, time);
            ci.cancel();
        }
    }

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void space_m$celestialWeather(CallbackInfo ci) {
        if (CelestialWeather.updateWeather((ServerLevel) (Object) this)) ci.cancel();
    }

    @Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
    private void space_m$resetRainAndThunder(CallbackInfo ci) {
        CelestialWeather.resetRainAndThunder((ServerLevel) (Object) this);
    }
}
