package com.space_m.machine.dyson;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1:1 NTM Space {@code CustomDamageHandlerDyson}: Lebewesen im Empfaengerstrahl erhalten {@code energy / 1000}
 * Explosionsschaden ({@code DamageSource.setExplosionSource(explosion.compat)}).
 * Original-Klasse: CustomDamageHandlerDyson
 */
public class CustomDamageHandlerDyson implements ICustomDamageHandler {

    protected long energy;

    public CustomDamageHandlerDyson(long energy) {
        this.energy = energy;
    }

    @Override
    public void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled) {
        if (entity instanceof LivingEntity)
            entity.hurt(EntityProcessorStandard.setExplosionSource(explosion.level, explosion.compat), (float) energy / 1_000);
    }
}
