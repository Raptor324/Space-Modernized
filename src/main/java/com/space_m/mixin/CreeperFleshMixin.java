package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.space_m.entity.mob.EntityCreeperFlesh;

import net.minecraft.world.entity.monster.Creeper;

/**
 * R8-A: {@code EntityCreeperFlesh} ueberschreibt im Original {@code func_146077_cc} (Explosion); in 1.20/1.21 ist
 * {@code Creeper.explodeCreeper()} privat -> hier abfangen (wie HBMs {@code CreeperMixin} fuer seine Creeper).
 */
@Mixin(Creeper.class)
public abstract class CreeperFleshMixin {

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void space_m$fleshExplode(CallbackInfo ci) {
        if ((Object) this instanceof EntityCreeperFlesh flesh) {
            flesh.fleshExplode();
            ci.cancel();
        }
    }
}
