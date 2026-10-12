package com.space_m.rocket.transport;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.explosion.ExplosionLarge;
import com.space_m.block.SpaceBlocks;
import com.space_m.entity.SpaceEntities;
import com.space_m.sound.SpaceSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code EntityCombatDropPod} ({@code entity_combat_pod}): faellt mit einem Block pro Teilschritt (4
 * Teilschritte je Tick, Bewegung fest {@code 0 | -1 | 0}). Sobald der Block ueber der Kapsel (Original-Pruefung
 * {@code ((int)(x-0,5), (int)(y+1), (int)(z-0,5))}) nicht Luft ist: Rauch ({@code ExplosionLarge.spawnParticles}, 50),
 * {@code block.hatchImpact} (10 / 0,5-1,0), {@code combat_drop} an {@code ((int)x, (int)y+1, (int)z)} setzen und die
 * Nutzlast ({@link #setPayload}) uebergeben. Farbe ueber synchronisierte Daten (Original DataWatcher 20).
 *
 * <p>Nutzung (Original {@code CBT_Invasion.spawnCattle}, R8): {@code new EntityCombatDropPod(level)}, Position,
 * {@code setPayload(entityNbtMitId, amount, color)}, {@code addFreshEntity}.
 * Abweichung: unterhalb von {@code minBuildHeight - 64} wird die Kapsel entfernt (das Original ruft
 * {@code onEntityUpdate} nicht auf und fiele endlos).
 * Original-Klasse: EntityCombatDropPod
 */
public class EntityCombatDropPod extends Entity {

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(EntityCombatDropPod.class, EntityDataSerializers.INT);

    @Nullable private CompoundTag entityType;
    private int amount;
    private int color;

    public EntityCombatDropPod(EntityType<? extends EntityCombatDropPod> type, Level world) {
        super(type, world);
        this.noCulling = true; // ignoreFrustumCheck
    }

    public EntityCombatDropPod(Level world) {
        this(SpaceEntities.COMBAT_POD.get(), world);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public void setEntity(Entity entity, int amount) {
        this.entityType = new CompoundTag();
        entity.saveWithoutId(this.entityType);
        this.amount = amount;
    }

    public void setColor(int color) {
        this.color = color;
        this.entityData.set(COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, 0);
    }
    *///?}

    public void setPayload(CompoundTag entityData, int amount, int color) {
        this.entityType = entityData;
        this.amount = amount;
        this.setColor(color);
    }

    @Override
    public void tick() {
        double motionX = 0, motionY = -1, motionZ = 0;
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.xo = this.xOld = this.getX();
        this.yo = this.yOld = this.getY();
        this.zo = this.zOld = this.getZ();

        Level world = level();

        for (int i = 0; i < 4; i++) {
            double posX = getX(), posY = getY(), posZ = getZ();

            BlockPos check = new BlockPos((int) (posX - 0.5), (int) (posY + 1), (int) (posZ - 0.5));
            if (!world.isClientSide && !world.getBlockState(check).isAir()) {
                ExplosionLarge.spawnParticles(world, posX, posY + 1, posZ, 50);

                world.playSound(null, posX, posY, posZ, SpaceSounds.BLOCK_HATCHIMPACT.get(), SoundSource.BLOCKS,
                        10.0F, 0.5F + this.random.nextFloat() * 0.5F);

                BlockPos at = new BlockPos((int) posX, (int) posY + 1, (int) posZ);

                world.setBlockAndUpdate(at, SpaceBlocks.COMBAT_DROP.get().defaultBlockState());

                BlockEntity te = world.getBlockEntity(at);

                if (te instanceof CombatDropPodBlockEntity capsule) {
                    if (this.entityType != null) {
                        capsule.setPayload(this.entityType, this.amount, this.color);
                        capsule.sync();
                    } else {
                        return;
                    }
                }
                this.discard();
                break;
            }

            this.setPos(posX + motionX, posY + motionY, posZ + motionZ);
        }

        if (!world.isClientSide && getY() < world.getMinBuildHeight() - 64) discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.entityType = nbt.getCompound("entityType");
        this.color = nbt.getInt("color");
        this.amount = nbt.getInt("amount");
        this.setColor(nbt.getInt("color"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("color", color);
        nbt.putInt("amount", amount);
        if (entityType != null) nbt.put("entityType", entityType);
    }
}
