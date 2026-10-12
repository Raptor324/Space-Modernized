package com.space_m.war;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.entity.mob.EntityUFOBase;
import com.hbm_m.entity.mob.siege.SiegeTier;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.item.weapon.sedna.factory.XFactoryEnergy;
import com.hbm_m.util.Vec3NT;
import com.space_m.sound.SpaceSounds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 1:1 NTM Space {@code com.hbm.entity.mob.siege.EntitySiegeUFO} ({@code entity_meme_ufo}, "Mini UFO"): kleines
 * Invasions-UFO (1,5 x 1) mit Stufe aus {@link SiegeTier} (Leben = Stufe * 0,25, Tempo, Panzerung, Feuerfest,
 * kein Fremdschaden), schiesst alle 20-44 Ticks einen Smaragd-Laser (Hitscan 250, 8 Schaden) auf die zuletzt gemerkte
 * Zielposition und zieht sich nach 2 Minuten senkrecht zurueck (verschwindet ueber y 250 bzw. 15 s spaeter).
 * Basisklasse = HBM-Port {@link EntityUFOBase} (Wegpunkt, Zielsuche, Kursplanung). Strahlungsimmun.
 * Erstickungsimmunitaet (Original {@code ISuffocationImmune} der Basisklasse) ueber {@code space_m:suffocation_immune}.
 */
public class EntitySiegeUFO extends EntityUFOBase implements IRadiationImmune {

    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(EntitySiegeUFO.class, EntityDataSerializers.INT);

    private int attackCooldown;
    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    private int lifetime = 0;
    private boolean isRetreating = false;
    private static final int MAX_LIFETIME = 60 * 2 * 20;

    public EntitySiegeUFO(EntityType<? extends EntitySiegeUFO> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes();
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIER, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TIER, 0);
    }
    *///?}

    public void setTier(SiegeTier tier) {
        this.entityData.set(TIER, tier.id);

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.speedMod);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 0.25);
        this.setHealth(this.getMaxHealth());
    }

    public SiegeTier getTier() {
        SiegeTier tier = SiegeTier.tiers[this.entityData.get(TIER)];
        return tier != null ? tier : SiegeTier.CLAY;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {

        if (this.isInvulnerableTo(source))
            return false;

        SiegeTier tier = this.getTier();

        if (tier.fireProof && source.is(DamageTypeTags.IS_FIRE)) {
            this.clearFire();
            return false;
        }

        // Original: EntityDamageSource, dessen Verursacher kein Spieler ist
        if (tier.noFriendlyFire && source.getEntity() != null && !(source.getEntity() instanceof Player))
            return false;

        damage -= tier.dt;

        if (damage < 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 5F, 1.0F + random.nextFloat() * 0.5F);
            return false;
        }

        damage *= (1F - tier.dr);

        return super.hurt(source, damage);
    }

    /** Original {@code updateEntityActionState} (nur Server). */
    @Override
    protected void customServerAiStep() {

        lifetime++;
        if (lifetime > MAX_LIFETIME) {
            isRetreating = true;
        }

        if (isRetreating) {
            this.target = null;
            this.setWaypoint((int) getX(), 255, (int) getZ());
            approachPosition(20);
            if (this.getY() > 250 || lifetime > MAX_LIFETIME + 300) {
                this.discard();
                return;
            }
        }

        super.customServerAiStep();
        if (this.isRemoved()) return;

        if (this.courseChangeCooldown > 0) {
            this.courseChangeCooldown--;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (!isRetreating && this.target != null) {

            if (this.lastTargetX == 0 && this.lastTargetY == 0 && this.lastTargetZ == 0) {
                this.lastTargetX = this.target.getX();
                this.lastTargetY = this.target.getY() + this.target.getEyeHeight();
                this.lastTargetZ = this.target.getZ();
            }

            if (random.nextInt(10) == 0) {
                this.lastTargetX = this.target.getX();
                this.lastTargetY = this.target.getY() + this.target.getEyeHeight();
                this.lastTargetZ = this.target.getZ();
            }

            if (this.attackCooldown == 0) {
                this.attackCooldown = 20 + random.nextInt(25);

                double spawnX = this.getX();
                double spawnY = this.getY();
                double spawnZ = this.getZ();

                EntityBulletBeamBase bullet = new EntityBulletBeamBase(this, XFactoryEnergy.energy_emerald_overcharge.setKnockback(0), 8F);

                bullet.setPos(spawnX, spawnY, spawnZ);

                Vec3NT delta = Vec3NT.createVectorHelper(lastTargetX - spawnX, lastTargetY - spawnY, lastTargetZ - spawnZ);

                bullet.setRotationsFromVector(delta);
                bullet.performHitscanExternal(250D);

                level().addFreshEntity(bullet);
                this.playSound(SpaceSounds.ENTITY_BFASHOOT.get(), 2.0F, 1.0F);
            }
        }

        if (this.courseChangeCooldown > 0) {
            double speed = this.target == null ? 0.5D : 1.0D + (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.0D);

            if (isRetreating) speed *= 2.0D;

            approachPosition(speed);
        }
    }

    @Nullable
    @Override
    //? if < 1.21.1 {
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
    //?} else {
    /*public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
    *///?}
        this.setTier(SiegeTier.tiers[random.nextInt(SiegeTier.getLength())]);
        //? if < 1.21.1 {
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
        //?} else {
        /*return super.finalizeSpawn(level, difficulty, reason, data);
        *///?}
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("siegeTier", this.getTier().id);
        nbt.putInt("lifetime", this.lifetime);
        nbt.putBoolean("isRetreating", this.isRetreating);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        SiegeTier tier = SiegeTier.tiers[nbt.getInt("siegeTier")];
        this.setTier(tier != null ? tier : SiegeTier.CLAY);
        this.lifetime = nbt.getInt("lifetime");
        this.isRetreating = nbt.getBoolean("isRetreating");
    }

    /** Original {@code dropFewItems}: Stufen-Beute, unabhaengig vom Spielerabschuss. */
    //? if < 1.21.1 {
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean byPlayer) {
    //?} else {
    /*@Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel spaceLevel, DamageSource source, boolean byPlayer) {
    *///?}
        for (Supplier<ItemStack> drop : this.getTier().dropItem) {
            this.spawnAtLocation(drop.get().copy(), 0F);
        }
    }
}
