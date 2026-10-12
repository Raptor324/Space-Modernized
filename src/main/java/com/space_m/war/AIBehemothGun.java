package com.space_m.war;

import java.util.EnumSet;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.EntityArtilleryShell;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.factory.XFactory762mm;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code com.hbm.entity.mob.ai.EntityAIBehemothGun}: Ziel 2-50 Bloecke; bis {@code switchAttackDistance}
 * alle {@code delay} Ticks eine Gatling-Kugel (7,62 mm AP, Rueckstoss 0, 0,5 Grundschaden, Streuung 0,01) aus 10
 * Bloecken Hoehe, darueber Artillerie (Granate Typ 9 aus 14 Bloecken Hoehe, Nachladen {@code reloadDelay} Feuerzyklen).
 * Der Koerper dreht sich mit dem Kopf. Keine Mutex-Flags (Original Standard 0).
 */
// Original-Klasse: EntityAIBehemothGun
public class AIBehemothGun extends Goal {

    private final PathfinderMob owner;
    private LivingEntity target;
    private final int delay;
    private int timer;
    private final int switchAttackDistance;
    private boolean artilleryMode;
    private int reloadTimer;
    private final int reloadDelay;

    public AIBehemothGun(PathfinderMob owner, boolean checkSight, boolean nearbyOnly, int delay, int switchAttackDistance, int reloadDelay) {
        this.owner = owner;
        this.delay = delay;
        this.timer = delay;
        this.switchAttackDistance = switchAttackDistance;
        this.artilleryMode = false;
        this.reloadTimer = reloadDelay;
        this.reloadDelay = reloadDelay;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity entity = this.owner.getTarget();

        if (entity == null) {
            return false;
        } else {
            this.target = entity;
            double dist = new Vec3(target.getX() - owner.getX(), target.getY() - owner.getY(), target.getZ() - owner.getZ()).length();
            artilleryMode = dist > switchAttackDistance;
            return dist > 2 && dist < 50;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || !this.owner.getNavigation().isDone();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        timer--;

        if (timer <= 0 && target != null) {
            if (artilleryMode) {
                fireArtilleryShell();
            } else {
                fireGatlingBarrage();
            }
            timer = delay;
        }
        this.owner.setYRot(this.owner.getYHeadRot());
    }

    private void fireGatlingBarrage() {
        double xOffset = Math.cos(Math.toRadians(owner.getYRot())) * 1.5;
        double zOffset = Math.sin(Math.toRadians(owner.getYRot())) * 1.5;
        double yOffset = 10;

        double targetX = target.getX() - (owner.getX() + xOffset);
        double targetY = target.getY() + target.getEyeHeight() - (owner.getY() + yOffset);
        double targetZ = target.getZ() - (owner.getZ() + zOffset);

        double distance = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        targetX /= distance;
        targetY /= distance;
        targetZ /= distance;

        BulletConfig config = XFactory762mm.r762_ap.setKnockback(0);
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(owner.level(), owner, config, 0.5F * config.damageMult, 0.01F,
                owner.getX() + xOffset, owner.getY() + yOffset, owner.getZ() + zOffset, targetX, targetY, targetZ);
        bullet.setPos(owner.getX() + xOffset, owner.getY() + yOffset, owner.getZ() + zOffset);

        bullet.setDeltaMovement(targetX * 0.5D, targetY * 0.5D, targetZ * 0.5D);

        owner.level().addFreshEntity(bullet);
        owner.playSound(HbmSoundsNT.get("hbm:weapon.calShoot"), 15.0F, 1.0F);
    }

    private void fireArtilleryShell() {
        if (reloadTimer <= 0) {
            EntityArtilleryShell grenade = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), owner.level());
            grenade.setType(9);
            grenade.setPos(owner.getX(), owner.getY() + 14, owner.getZ());
            Vec3 vec = new Vec3(target.getX() - owner.getX(), 0, target.getZ() - owner.getZ());
            double mx = vec.x * 0.05D;
            double my = 0.5D + owner.getRandom().nextDouble() * 0.5D;
            double mz = vec.z * 0.05D;
            grenade.setThrowableHeading(mx, my * 6, mz, 5F, 0);
            grenade.setTarget(target.getX(), target.getZ(), target.getY());
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), HbmSoundsNT.get("hbm:turret.jeremy_fire"), SoundSource.HOSTILE, 25.0F, 1.0F);
            owner.level().addFreshEntity(grenade);
            reloadTimer = reloadDelay;
        } else {
            reloadTimer--;
        }
    }
}
