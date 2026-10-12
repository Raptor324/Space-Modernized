package com.space_m.entity.mob.ai;

import java.util.EnumSet;

import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.item.weapon.sedna.factory.XFactory12ga;
import com.hbm_m.item.weapon.sedna.factory.XFactoryRocket;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.SpentCasing.CasingType;
import com.hbm_m.particle.helper.IParticleCreator;
import com.space_m.entity.mob.MobSounds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityAITankshell} (Tankbot): Ziel 2-50 Bloecke entfernt; ueber {@code AttackDistance} wird alle
 * {@code delay} Ticks gefeuert (nach {@code reloadDelay} Nachladetakten): ueber 30 Bloecke eine QD-Rakete
 * ({@code rocket_qd[1]}, unter 30 Leben {@code rocket_qd[3]}), sonst 8 Explosiv-Schrotkugeln {@code g12_explosive};
 * dazu eine Huelse ("TankGa"), Jeremy-Feuer-/Nachladegeraeusch. Richtet den Koerper nach dem Kopf aus.
 *
 * <p>Die Geschosse werden wie im Original mit dem "Waffen"-Konstruktor erzeugt (Positionswerte als Versatz) und
 * danach auf die Position gesetzt und mit eigener Bewegung versehen.
 */
public class EntityAITankshell extends Goal {

    private final PathfinderMob owner;
    private LivingEntity target;
    private final int delay;
    private int timer;
    private final int AttackDistance;
    private boolean artilleryMode;
    private int reloadTimer;
    private final int reloadDelay;

    public EntityAITankshell(PathfinderMob owner, boolean checkSight, boolean nearbyOnly, int delay, int switchAttackDistance, int reloadDelay) {
        this.owner = owner;
        this.delay = delay;
        this.timer = delay;
        this.AttackDistance = switchAttackDistance;
        this.artilleryMode = true;
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
            artilleryMode = dist > AttackDistance;
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
        if (timer <= 0) {
            if (artilleryMode) {
                fireArtilleryShell();
            }
            timer = delay;
        }
        this.owner.setYRot(this.owner.getYHeadRot());
    }

    /** {@code new CasingEjector().setMotion(0, 0.6, -1).setAngleRange(0.1F, 0.1F)}. */
    private static final double EJ_MX = 0, EJ_MY = 0.6, EJ_MZ = -1;
    private static final float EJ_FACTOR = 0.1F;

    protected Vec3 getCasingSpawnPos() {
        return new Vec3(owner.getX(), owner.getY(), owner.getZ());
    }

    /**
     * Original: "casing"-Paket mit {@code pitch 0}, {@code yaw = rotationYawHead} (Grad, vom Auswerfer als Bogenmass
     * genommen) -> {@code CasingEjector.spawnCasing}. Hier serverseitig berechnet und als HBM-"casingNT" geschickt.
     */
    protected void spawnCasing() {
        SpentCasing cachedCasingConfig = new SpentCasing(CasingType.SHOTGUN).setColor(0xE5DD00, SpentCasing.COLOR_CASE_12GA).setScale(10F).register("TankGa").setupSmoke(0.02F, 0.5D, 60, 20).setMaxAge(60);

        if (!(owner.level() instanceof ServerLevel server)) return;
        Vec3 spawn = this.getCasingSpawnPos();
        var rand = owner.getRandom();
        float pitch = 0F;
        float yaw = owner.getYHeadRot();
        // rotateVector(motion, pitch + gauss * pitchFactor, yaw + gauss * pitchFactor)
        float pitchA = pitch + (float) rand.nextGaussian() * EJ_FACTOR;
        float yawA = yaw + (float) rand.nextGaussian() * EJ_FACTOR;
        double cp = Math.cos(pitchA), sp = Math.sin(pitchA);
        double y1 = EJ_MY * cp - EJ_MZ * sp;
        double z1 = EJ_MY * sp + EJ_MZ * cp;
        double x1 = EJ_MX;
        double cy = Math.cos(-yawA), sy = Math.sin(-yawA);
        double x2 = x1 * cy + z1 * sy;
        double z2 = -x1 * sy + z1 * cy;

        CompoundTag data = new CompoundTag();
        data.putString("type", "casingNT");
        data.putDouble("mX", x2);
        data.putDouble("mY", y1);
        data.putDouble("mZ", z2);
        data.putFloat("yaw", (float) Math.toDegrees(yaw));
        data.putFloat("pitch", (float) Math.toDegrees(pitch));
        data.putFloat("mPitch", (float) (rand.nextGaussian() * 5F));
        data.putFloat("mYaw", (float) (rand.nextGaussian() * 10F));
        data.putString("name", cachedCasingConfig.getName());
        data.putBoolean("smoking", false);
        data.putInt("smokeLife", 0);
        data.putDouble("smokeLift", 0);
        data.putInt("nodeLife", 0);
        IParticleCreator.sendPacket(server, spawn.x, spawn.y, spawn.z, 50, data);
    }

    private void fireArtilleryShell() {
        if (reloadTimer <= 0) {
            double xOffset = Math.cos(Math.toRadians(owner.getYHeadRot())) * 0;
            double zOffset = Math.sin(Math.toRadians(owner.getYHeadRot())) * 1.5;
            double yOffset = 10;

            double targetX = target.getX() - (owner.getX() + xOffset);
            double targetY = target.getY() + target.getEyeHeight() - (owner.getY() + yOffset);
            double targetZ = target.getZ() - (owner.getZ() + zOffset);

            double distance = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
            targetX /= distance;
            targetY /= distance;
            targetZ /= distance;
            spawnCasing();

            if (distance > 30) {
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(owner, XFactoryRocket.rocket_qd[owner.getHealth() < 30 ? 3 : 1], 4, 0.01F, owner.getX(), owner.getY() + 3, owner.getZ());
                bullet.setPos(owner.getX(), owner.getY() + 3, owner.getZ());
                bullet.setDeltaMovement(targetX * 0.5D, targetY * 0.1D, targetZ * 0.5D);
                owner.level().addFreshEntity(bullet);
            } else {
                for (int i = 0; i < 8; i++) {
                    EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(owner, XFactory12ga.g12_explosive, 4, 0.01F, owner.getX(), owner.getY() + 3, owner.getZ());
                    bullet.setPos(owner.getX(), owner.getY() + 3, owner.getZ());
                    bullet.setDeltaMovement(targetX * (0.2 + (0.5 * Math.random())), targetY * 0.1, targetZ * (0.2 + (0.5 * Math.random())));
                    owner.level().addFreshEntity(bullet);
                }
            }

            MobSounds.at(owner.level(), owner.getX(), owner.getY(), owner.getZ(), MobSounds.hbm("hbm:turret.jeremy_fire"), 25.0F, 1.0F);
            reloadTimer = reloadDelay;
        } else {
            reloadTimer--;
        }

        if (reloadTimer == 20) {
            MobSounds.at(owner.level(), owner.getX(), owner.getY(), owner.getZ(), MobSounds.hbm("hbm:turret.jeremy_reload"), 3.0F, 1.0F);
        }
    }
}
