package com.space_m.bomb;

import java.util.List;

import com.hbm_m.entity.logic.EntityExplosionChunkloading;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * NTM Space {@code EntityBalefire} im Antimaterie-Modus ({@code bf.antimatter()}), gespawnt von
 * {@link TileEntityAntimatter#explode()}:
 * <ul>
 *   <li>Spirale {@link ExplosionBalefireAntimatter}, Geschwindigkeit steigt je Tick um 1.</li>
 *   <li>Die ersten 10 Ticks: {@code radiate(40000 * range, range * 2)} - Dosis durch die Summe der Explosionswiderstaende
 *       auf der Sichtlinie und das Abstandsquadrat; ab 500 RAD sterben Nicht-Kreative sofort (mit Schockwelle).</li>
 *   <li>Je Tick mit 1/5 {@code random.explode}, solange die Spirale laeuft Donner + {@code dealDamage(range * 2)}.</li>
 *   <li>{@link #startFacAnti}: Original {@code EntityNukeTorex.startFacAnti} (Typ 2 = Regenbogen-Pilz, Skala
 *       {@code clamp(scale * 0.01, 0.5, 5)}, Reichweite 1000) ueber den HBM-Partikelkanal.</li>
 * </ul>
 * Original-Klasse: EntityBalefire
 */
public class EntityBalefireAntimatter extends EntityExplosionChunkloading {

    /** Partikel-Schluessel des Client-Erzeugers ({@code com.space_m.bomb.client.BombClient}). */
    public static final String TOREX_ANTI = "space_m:nuke_anti";

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionBalefireAntimatter exp;
    public int speed = 1;
    public boolean did = false;

    public EntityBalefireAntimatter(EntityType<? extends EntityBalefireAntimatter> type, Level level) {
        super(type, level);
    }

    @Override
    protected int getChunkLoadRadius() {
        if (this.destructionRange <= 0) return super.getChunkLoadRadius();
        return Math.min(12, Math.max(super.getChunkLoadRadius(), (this.destructionRange + 15) >> 4) + 1);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() { }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;

        updateChunkTicket();

        if (!this.did) {
            exp = new ExplosionBalefireAntimatter((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
            this.did = true;
        }

        speed += 1; // increase speed to keep up with expansion

        boolean flag = false;
        for (int i = 0; i < this.speed; i++) {
            flag = exp.update();

            if (flag) {
                clearChunkTicket();
                this.discard();
                break;
            }
        }

        if (this.tickCount < 10) {
            radiate(40000 * this.destructionRange, this.destructionRange * 2);
        }

        if (random.nextInt(5) == 0)
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 10000.0F, 0.8F + random.nextFloat() * 0.2F);

        if (!flag) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000.0F, 0.8F + random.nextFloat() * 0.2F);
            ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.destructionRange * 2);
        }

        age++;
    }

    private void radiate(float rads, double range) {
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class,
                new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range, range, range));

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (LivingEntity e : entities) {
            Vec3 vec = new Vec3(e.getX() - getX(), (e.getY() + e.getEyeHeight()) - getY(), e.getZ() - getZ());
            double len = vec.length();
            vec = vec.normalize();

            float res = 0;

            for (int i = 1; i < len; i++) {
                int ix = (int) Math.floor(getX() + vec.x * i);
                int iy = (int) Math.floor(getY() + vec.y * i);
                int iz = (int) Math.floor(getZ() + vec.z * i);

                res += level().getBlockState(pos.set(ix, iy, iz)).getBlock().getExplosionResistance();
            }

            if (res < 1)
                res = 1;

            float eRads = rads;
            eRads /= res;
            eRads /= (float) (len * len);

            ContaminationUtil.contaminate(e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);
            if (eRads >= 500) {
                if (!(e instanceof Player p && p.getAbilities().instabuild)) {
                    e.setHealth(0);
                    ExplosionLarge.spawnShock(level(), e.getX(), e.getY(), e.getZ(), 10, 10);
                    // Original e.setDead(): Spieler sterben ueber setHealth(0) regulaer
                    if (!(e instanceof Player)) e.discard();
                }
            }
        }

        age++;
    }

    /** Original {@code EntityNukeTorex.startFacAnti(world, x, y, z, scale)}. */
    public static void startFacAnti(Level level, double x, double y, double z, float scale) {
        if (!(level instanceof ServerLevel server)) return;
        CompoundTag tag = new CompoundTag();
        tag.putString("type", TOREX_ANTI);
        tag.putFloat("scale", scale);
        IParticleCreator.sendPacket(server, x, y, z, 1000, tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        age = nbt.getInt("age");
        destructionRange = nbt.getInt("destructionRange");
        speed = nbt.getInt("speed");
        did = nbt.getBoolean("did");

        exp = new ExplosionBalefireAntimatter((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
        exp.readFromNbt(nbt, "exp_");

        this.did = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("age", age);
        nbt.putInt("destructionRange", destructionRange);
        nbt.putInt("speed", speed);
        nbt.putBoolean("did", did);
        nbt.putBoolean("antimatter", true);

        if (exp != null)
            exp.saveToNbt(nbt, "exp_");
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
        super.remove(reason);
    }
}
