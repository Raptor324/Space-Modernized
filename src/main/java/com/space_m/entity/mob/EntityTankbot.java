package com.space_m.entity.mob;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.util.ParticleUtil;
import com.space_m.atmosphere.ISuffocationImmune;
import com.space_m.entity.mob.ai.EntityAIStepTowardsTarget;
import com.space_m.entity.mob.ai.EntityAITankshell;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityTankbot} (Thatmo): vierbeiniger Panzerroboter, 180 Leben, Folgeweite 60, Schaden 15, voller
 * Rueckstossschutz, feuerfest, verschwindet nicht. Greift Spieler und alle Mobs ausser Tankbots an
 * ({@link EntityAITankshell}, Schrittbewegung {@link EntityAIStepTowardsTarget}). Panzerung: Feuer/Magie 0, Projektile
 * x0.25, Explosionen x0.5, ueber 50 nur ein Viertel; Eier toeten ihn mit 1/10. Tod: ab {@code deathTime} -50 Funken,
 * bei -30 Hubschrauber-Schadensgeraeusch und Gasflammen, bei -5 Explosion 5 (Feuer, ohne Blockschaden).
 * Ziel-ID synchronisiert (Original DataWatcher 20) fuer den Kopf im Renderer.
 */
public class EntityTankbot extends Monster implements IRadiationImmune, ISuffocationImmune {

    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityTankbot.class, EntityDataSerializers.INT);

    public double headTargetYaw;
    public double currentHeadYaw;

    public EntityTankbot(EntityType<? extends EntityTankbot> entityType, Level world) {
        super(entityType, world);
        // getNavigator().setAvoidsWater(true)
        //? if < 1.21.1 {
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, -1.0F);
        //?} else {
        /*this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
        *///?}
        this.deathTime = -50;
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.2D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 0, true, true, e -> !(e instanceof EntityTankbot)));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(3, new EntityAIStepTowardsTarget(this, 50, 0.2D, 200, 20, 0.6));
        this.goalSelector.addGoal(4, new EntityAITankshell(this, true, true, 2, 10, 30));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 180.0D);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_ID, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_ID, 0);
    }
    *///?}

    /** {@code onDeathUpdate}: verzoegerter Tod mit Funken und Explosion. */
    @Override
    protected void tickDeath() {
        if (this.deathTime == -30) {
            MobSounds.atEntity(this, MobSounds.hbm("hbm:entity.chopperDamage"), 10.0F, 1.0F);
        }
        if (this.deathTime >= -30 && !level().isClientSide) {
            ParticleUtil.spawnGasFlame(level(), this.getX(), this.getY() + 1, this.getZ(), random.nextGaussian(), 0.4, random.nextGaussian());
        }
        if (this.deathTime == -5 && !level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(), 5F, true, Level.ExplosionInteraction.NONE);
            ExplosionCreator.composeEffectSmall((ServerLevel) level(), this.getX(), this.getY(), this.getZ());
            this.discard();
        }

        super.tickDeath();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void setTarget(@Nullable LivingEntity entity) {
        super.setTarget(entity);
        this.entityData.set(TARGET_ID, entity != null ? entity.getId() : 0);
    }

    /** Original DataWatcher 20: Ziel-ID fuer den Renderer. */
    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {

        if (source.getDirectEntity() instanceof ThrownEgg && random.nextInt(10) == 0) {
            this.xpReward = 0;
            this.setHealth(0);
            return true;
        }

        if (source.is(DamageTypeTags.IS_FIRE))
            amount = 0;
        if (isMagic(source))
            amount = 0;
        if (source.is(DamageTypeTags.IS_PROJECTILE))
            amount *= 0.25F;
        if (source.is(DamageTypeTags.IS_EXPLOSION))
            amount *= 0.5F;

        if (amount > 50) {
            amount = 50 + (amount - 50) * 0.25F;
        }

        return super.hurt(source, amount);
    }

    /** 1.7.10 {@code isMagicDamage}: Magie, indirekte Magie (Traenke) und Dornen. */
    static boolean isMagic(DamageSource source) {
        return source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.THORNS);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MobSounds.hbm("hbm:entity.cybercrab");
    }

    /** {@code onUpdate}: Koerper dreht sich mit hoechstens 1 Grad je Tick zum Kopf. */
    @Override
    public void tick() {
        super.tick();

        float targetYaw = this.getYHeadRot();
        float currentYaw = this.getYRot();
        float maxYawChange = 1f;

        float yawDifference = targetYaw - currentYaw;

        while (yawDifference < -180.0F) yawDifference += 360.0F;
        while (yawDifference >= 180.0F) yawDifference -= 360.0F;

        if (yawDifference > maxYawChange) yawDifference = maxYawChange;
        if (yawDifference < -maxYawChange) yawDifference = -maxYawChange;

        this.setYRot(this.getYRot() + yawDifference);
    }

    /** {@code renderDistanceWeight = 24}: {@code getAverageEdgeLength() * 64 * 24}. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 64.0D * 24.0D;
        return distance < d * d;
    }
}
