package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.LivingEntity;

/**
 * Lesezugriff auf {@code LivingEntity.jumping} (Original 1.7.10 {@code EntityLivingBase.isJumping}, oeffentlich):
 * Schwerelosigkeit im Orbit steigt mit gedrueckter Sprungtaste ({@code ModEventHandler.onLivingUpdate}).
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor("jumping")
    boolean space_m$isJumping();
}
