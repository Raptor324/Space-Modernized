package com.space_m.entity.mob.ai;

import java.util.EnumSet;
import java.util.Random;

import com.space_m.entity.mob.MobSounds;
import com.space_m.sound.SpaceSounds;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * 1:1 {@code EntityAIStepTowardsTarget} (Tankbot, War Behemoth): alle {@code stepDuration + stepCooldown} Ticks ein
 * Piepen und ein Schritt von {@code stepDistance} Bloecken Richtung Ziel (ab {@code minDistance}), ohne Ziel ein
 * Schritt zu einem Zufallsblock im Umkreis von 5. Laeuft immer (keine Sperrbits).
 */
public class EntityAIStepTowardsTarget extends Goal {

    private final PathfinderMob entity;
    private final double stepDistance;
    private final double speed;
    private final int stepDuration;
    private final int stepCooldown;
    private final double minDistance;
    private int stepTimer;
    private final Random random = new Random();

    public EntityAIStepTowardsTarget(PathfinderMob entity, double stepDistance, double speed, int stepDuration, int stepCooldown, double minDistance) {
        this.entity = entity;
        this.stepDistance = stepDistance;
        this.speed = speed;
        this.stepDuration = stepDuration;
        this.stepCooldown = stepCooldown;
        this.minDistance = minDistance;
        this.stepTimer = 0;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        this.stepTimer = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.stepTimer > 0) {
            this.stepTimer--;
        } else {
            LivingEntity target = this.entity.getTarget();
            if (target != null && target.isAlive()) {
                double dx = target.getX() - this.entity.getX();
                double dz = target.getZ() - this.entity.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                MobSounds.atEntity(this.entity, SpaceSounds.ENTITY_BEEP.get(), 1.0F, 1.0F);

                if (distance > minDistance) {
                    dx = (dx / distance) * stepDistance;
                    dz = (dz / distance) * stepDistance;

                    this.entity.getNavigation().moveTo(this.entity.getX() + dx, this.entity.getY(), this.entity.getZ() + dz, this.speed);
                }
            } else {
                // No target found, move to a random nearby block
                int randomX = Mth.floor(this.entity.getX()) + random.nextInt(10) - 5;
                int randomZ = Mth.floor(this.entity.getZ()) + random.nextInt(10) - 5;
                int randomY = Mth.floor(this.entity.getY());
                this.entity.getNavigation().moveTo(randomX, randomY, randomZ, this.speed);
            }
            this.stepTimer = stepDuration + stepCooldown; // Reset step timer with cooldown
        }
    }
}
