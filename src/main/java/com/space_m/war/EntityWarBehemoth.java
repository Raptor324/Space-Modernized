package com.space_m.war;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code com.hbm.entity.mob.EntityWarBehemoth} ({@code entity_war_behemoth}): zweibeiniger Kriegsläufer
 * (1,75 x 6,35, 200 Leben, Schritthoehe 5, kein Wasser, kein Despawn) auf Thatmo. Greift Spieler und alle anderen
 * Lebewesen ausser Behemoths an, stapft in 50-Block-Schritten auf das Ziel zu ({@link AIStepTowardsTarget}) und
 * schiesst mit der Gatling (7,62 AP, nah) bzw. Artillerie (Typ 9, ab 35 Bloecken, Nachladen 30) ({@link AIBehemothGun}).
 * Feuer/Magie immun, Projektile 1/4, Explosionen 1/2, ueber 50 Schaden nur zu 1/4; ein Ei toetet ihn mit 10 %.
 * Zusaetzliche Schwerkraft -0,1 je Tick. Ziel-ID synchronisiert (Original DataWatcher 19, Kopfdrehung im Renderer).
 * Erstickungsimmun ({@code space_m:suffocation_immune}).
 *
 * <p>Version: 1.20.1 Schritthoehe ueber {@code setMaxUpStep}, 1.21.1 Attribut {@code STEP_HEIGHT}.
 */
public class EntityWarBehemoth extends Monster {

    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityWarBehemoth.class, EntityDataSerializers.INT);

    public double headTargetYaw;

    /** Client: Kopf-/Rumpfneigung zum Ziel (Original {@code ModelBehemoth.setLivingAnimations} auf {@code rotationPitch}). */
    public float renderPitch;

    public EntityWarBehemoth(EntityType<? extends EntityWarBehemoth> type, Level world) {
        super(type, world);
        //? if < 1.21.1 {
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, -1.0F);
        //?} else {
        /*this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
        *///?}
        // neo-pendant: createAttributes (Attributes.STEP_HEIGHT 5)
        //? if < 1.21.1 {
        this.setMaxUpStep(5.0F);
        //?}
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 100.0D)
                .add(Attributes.MAX_HEALTH, 200.0D)
                //? if >= 1.21.1 {
                /*.add(Attributes.STEP_HEIGHT, 5.0D)
                *///?}
                ;
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 0, true, true, e -> !(e instanceof EntityWarBehemoth)));
        //this.goalSelector.addGoal(3, new AIStepTowardsTarget(this, 4, 0.18, 20, 60, 0.6));
        this.goalSelector.addGoal(3, new AIStepTowardsTarget(this, 50, 0.3D, 50, 10, 0.6));
        //this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 0.2D, false));
        this.goalSelector.addGoal(4, new AIBehemothGun(this, true, true, 3, 35, 30));
        this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
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

    @Override
    public boolean hurt(DamageSource source, float amount) {

        if (source.getDirectEntity() instanceof ThrownEgg && this.random.nextInt(10) == 0) {
            this.xpReward = 0;
            this.setHealth(0);
            return true;
        }

        if (source.is(DamageTypeTags.IS_FIRE))
            amount = 0;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)) // isMagicDamage
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

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HbmSoundsNT.get("hbm:entity.cybercrab");
    }

    /** Original {@code jumpMovementFactor = 0}. */
    @Override
    protected float getFlyingSpeed() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.10D, 0)); // Increase the downward pull for heavier gravity
        }
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

    /** Original DataWatcher 19: Entity-ID des Ziels (0 = keins). */
    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }
}
