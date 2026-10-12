package com.space_m.entity.mob;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.item.weapon.sedna.factory.XFactoryEnergy;
import com.hbm_m.item.weapon.sedna.factory.XFactoryFlamer;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;
import com.hbm_m.util.Vec3NT;
import com.space_m.atmosphere.ISuffocationImmune;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Invasion;
import com.space_m.sound.SpaceSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBFAngel} ("Radioactive Angel", Boss): fliegender Engel mit 10 000 Leben, 500 Erfahrung, feuerfest,
 * strahlungs-/erstickungsimmun, verschwindet nicht; nach jedem Treffer 5 Ticks unverwundbar.
 * <ul>
 *   <li>Zielsuche alle 50 Ticks im Umkreis 100/50/100: naechster Spieler (nicht kreativ/unsichtbar) als Ziel, sichtbare
 *       Lebewesen bis 100 Bloecke als Nebenziele (ohne Spieler ein zufaelliges Nebenziel).</li>
 *   <li>Ticks 0-199 von 300: Tesla-Ueberladungsstrahl ({@code energy_tesla_overcharge}, 8 Schaden, Hitscan 250) nach
 *       70 Ticks Aufladen ({@code bfatalk} bei 30), Zielpunkt bis 10 Ticks vorher nachgefuehrt.</li>
 *   <li>Ticks 200-299: Balefire-Flammen ({@code flame_balefire}) - jede Sekunde auf ein Nebenziel, sonst alle 4 Ticks
 *       aufs Ziel ({@code richard_fire}).</li>
 *   <li>Alle 200 Ticks drei Einschlaege um das Ziel (+-6): 60 Ticks rote Markierung + Zielsuchton, dann Strahl von
 *       40 Bloecken senkrecht herab (6 Schaden).</li>
 *   <li>Bewegung: Wegpunkt im Kreis 15-24 Bloecke um das Ziel, alle 85 Ticks neu, in den letzten 10 Ticks Sprint.</li>
 *   <li>Tod ({@code deathTime} ab -30): Absinken, bei 19 Mini-Nuke-Effekt, {@code core_angel}, Chat
 *       "Stars are starting to flicker..." an Spieler im Umkreis 200 und {@code CBT_Invasion(0, 122, false)} auf dem
 *       Himmelskoerper, falls noch keine Invasion laeuft.</li>
 * </ul>
 * Der senkrechte Todesstrahl ({@code beamTimer}) wird im Original nie gesetzt - 1:1 uebernommen (ohne Wirkung).
 * Bossleiste: Original {@code BossStatus} im Renderer -> {@link ServerBossEvent} (lila wie die 1.7.10-Leiste).
 */
public class EntityBFAngel extends FlyingMob implements Enemy, IRadiationImmune, ISuffocationImmune {

    private static final EntityDataAccessor<Byte> BEAM = SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> WX = SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WY = SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WZ = SynchedEntityData.defineId(EntityBFAngel.class, EntityDataSerializers.INT);

    public int courseChangeCooldown;
    public int scanCooldown;

    public int hurtCooldown;
    public int beamTimer;
    private Entity target;
    private final List<Entity> secondaries = new ArrayList<>();

    private double lockedX;
    private double lockedY;
    private double lockedZ;

    private int aoeCooldown = 0;
    private int aoeStage = 0;

    private final Vec3[] strikepositions = new Vec3[3];
    private final int[] strikeTimers = new int[3];
    private final boolean[] strikeArmed = new boolean[3];

    public int gatlingCharge;
    public boolean isChargingGatling;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.space_m.entity_ntm_bfa"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public EntityBFAngel(EntityType<? extends EntityBFAngel> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 500;
        this.noCulling = true;
        this.deathTime = -30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10000.0D);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {

        if (hurtCooldown > 0)
            return false;

        boolean hit = super.hurt(source, amount);

        if (hit)
            hurtCooldown = 5;

        return hit;
    }

    /** {@code canAttackClass}: alles ausser anderen Engeln (der Projektil-Test des Originals greift nie). */
    private boolean canAttack(Entity e) {
        return e.getClass() != this.getClass();
    }

    /** {@code updateEntityActionState} (nur Server). */
    @Override
    protected void customServerAiStep() {

        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }

        if (this.hurtCooldown > 0) {
            this.hurtCooldown--;
        }

        if (this.courseChangeCooldown > 0) {
            this.courseChangeCooldown--;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
        }

        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
        }

        if (this.scanCooldown <= 0) {
            List<Entity> entities = level().getEntities(this, this.getBoundingBox().inflate(100, 50, 100));
            this.secondaries.clear();
            this.target = null;

            for (Entity entity : entities) {

                if (!entity.isAlive() || !canAttack(entity))
                    continue;

                if (entity instanceof Player player) {

                    if (player.getAbilities().instabuild || player.isSpectator())
                        continue;

                    if (player.hasEffect(MobEffects.INVISIBILITY))
                        continue;

                    if (this.target == null) {
                        this.target = entity;
                    } else {
                        if (this.distanceToSqr(entity) < this.distanceToSqr(this.target)) {
                            this.target = entity;
                        }
                    }
                }

                if (entity instanceof LivingEntity && this.distanceToSqr(entity) < 100 * 100 && this.hasLineOfSight(entity) && entity != this.target) {
                    this.secondaries.add(entity);
                }
            }

            if (this.target == null && !this.secondaries.isEmpty())
                this.target = this.secondaries.get(random.nextInt(this.secondaries.size()));

            this.scanCooldown = 50;
        }

        if (this.target != null && this.courseChangeCooldown <= 0) {

            Vec3 vec = new Vec3(this.getX() - this.target.getX(), 0, this.getZ() - this.target.getZ());

            if (random.nextInt(3) > 0)
                vec = vec.yRot((float) Math.PI * 2 * random.nextFloat());

            double length = vec.length();
            double overshoot = 35;

            int wX = (int) Math.floor(this.target.getX() - vec.x / length * overshoot);
            int wZ = (int) Math.floor(this.target.getZ() - vec.z / length * overshoot);

            this.setWaypoint(wX, Math.max(this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, wX, wZ) + random.nextInt(15), (int) this.target.getY() - 5), wZ);

            this.courseChangeCooldown = 40 + random.nextInt(20);
        }

        ServerLevel server = (ServerLevel) level();

        if (beamTimer <= 0 && this.getBeam()) {
            this.setBeam(false);
        }

        if (beamTimer > 0) {
            this.beamTimer--;

            int iy = 0;

            if (iy < this.getY()) {
                List<Entity> entities = level().getEntities(this, new AABB(this.getX(), iy, this.getZ(), this.getX(), this.getY(), this.getZ()).inflate(5, 0, 5));

                for (Entity e : entities) {
                    if (this.canAttack(e)) {
                        e.hurt(ModDamageSources.create(e, this, ModDamageTypes.CMB), 1000F);
                        com.hbm_m.platform.PlatformHooks.setSecondsOnFire(e, 5);

                        if (e instanceof LivingEntity living)
                            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
                    }
                }

                CompoundTag data = new CompoundTag();
                data.putString("type", "ufo");
                Vec3 m = this.getDeltaMovement();
                IParticleCreator.sendPacket(server, getX(), iy + 0.5, getZ(), 150, data);
                IParticleCreator.sendPacket(server, getX() + m.x * 0.5, iy + 0.5, getZ() + m.z * 0.5, 150, data);
            }
        }

        if (this.tickCount % 300 < 200) {

            if (!this.secondaries.isEmpty()) {
                Entity e = this.secondaries.get(random.nextInt(this.secondaries.size()));

                if (!e.isAlive()) {
                    this.secondaries.remove(e);
                }
            }

            if (this.target != null) {

                if (!isChargingGatling) {
                    isChargingGatling = true;
                    gatlingCharge = 70;
                } else {

                    if (gatlingCharge > 10) {
                        lockedX = this.target.getX();
                        lockedY = this.target.getY();
                        lockedZ = this.target.getZ();
                    }

                    gatlingCharge--;

                    if (gatlingCharge == 30) {
                        MobSounds.atEntity(this, SpaceSounds.ENTITY_BFATALK.get(), 10.0F, 1.0F);
                    }

                    if (gatlingCharge <= 0) {
                        fireBeamAt(lockedX, lockedY, lockedZ);

                        isChargingGatling = false;
                        gatlingCharge = 0;
                    }
                }
            }

        } else {

            if (this.tickCount % 20 == 0) {

                if (!this.secondaries.isEmpty()) {
                    Entity e = this.secondaries.get(random.nextInt(this.secondaries.size()));

                    if (e.isAlive()) {
                        rocketAttack(e);
                    } else {
                        this.secondaries.remove(e);
                    }

                } else if (this.target != null) {
                    rocketAttack(this.target);
                }

            } else if (this.tickCount % 4 == 2) {

                if (this.target != null) {
                    rocketAttack(this.target);
                }
            }
        }

        if (this.target != null) {

            if (aoeCooldown <= 0 && aoeStage == 0) {
                aoeStage = 1;

                for (int i = 0; i < 3; i++) {

                    double offsetX = (random.nextDouble() - 0.5) * 12;
                    double offsetZ = (random.nextDouble() - 0.5) * 12;

                    strikepositions[i] = new Vec3(target.getX() + offsetX, target.getY(), target.getZ() + offsetZ);

                    strikeTimers[i] = -i * 4;
                    strikeArmed[i] = true;
                }

                aoeCooldown = 200;
            }

            for (int i = 0; i < 3; i++) {

                if (strikeArmed[i]) {

                    strikeTimers[i]++;

                    if (strikeTimers[i] < 0)
                        continue;

                    Vec3 pos = strikepositions[i];

                    if (strikeTimers[i] < 60 && strikeTimers[i] % 5 == 0) {
                        CompoundTag fx = new CompoundTag();
                        fx.putString("type", "vanillaburst");
                        fx.putString("mode", "reddust");
                        fx.putDouble("motion", 0.2D);
                        fx.putInt("count", 35);
                        MobSounds.at(level(), pos.x, pos.y, pos.z, MobSounds.hbm("hbm:weapon.stingerLockOn"), 1.0F, 1F);

                        IParticleCreator.sendPacket(server, pos.x, pos.y, pos.z, 100, fx);
                    }

                    if (strikeTimers[i] >= 60) {
                        fireDownwardBeamAt(pos.x, pos.y, pos.z);
                        strikeArmed[i] = false;
                    }
                }
            }

            boolean allDone = true;
            for (int i = 0; i < 3; i++) {
                if (strikeArmed[i]) {
                    allDone = false;
                    break;
                }
            }

            if (allDone) {
                aoeStage = 0;
            }

            if (aoeCooldown > 0) {
                aoeCooldown--;
            }
        }

        Vec3 motion = Vec3.ZERO;

        if (this.target != null) {

            if (this.courseChangeCooldown <= 0) {

                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 15 + random.nextInt(10);

                int targetX = (int) (this.target.getX() + Math.cos(angle) * radius);
                int targetZ = (int) (this.target.getZ() + Math.sin(angle) * radius);
                int targetY = (int) (this.target.getY() + 5 + random.nextInt(5));

                this.setWaypoint(targetX, targetY, targetZ);

                this.courseChangeCooldown = 85;
            }

            if (this.courseChangeCooldown <= 10) {

                double dx = this.getWaypointX() - this.getX();
                double dy = this.getWaypointY() - this.getY();
                double dz = this.getWaypointZ() - this.getZ();

                Vec3 dir = new Vec3(dx, dy, dz).normalize();

                double t = this.courseChangeCooldown / 15.0;
                double dashSpeed = 2.0D * t;

                motion = new Vec3(dir.x * dashSpeed, dir.y * dashSpeed, dir.z * dashSpeed);
            }

            this.courseChangeCooldown--;
        }

        this.setDeltaMovement(motion);

        if (this.target != null) {
            double dx = this.target.getX() - this.getX();
            double dy = this.target.getY() + this.target.getBbHeight() * 0.5 - (this.getY() + 1);
            double dz = this.target.getZ() - this.getZ();

            float targetYaw = (float) (Math.atan2(dz, dx) * 180D / Math.PI) + 90F;

            float delta = Mth.wrapDegrees(targetYaw - this.getYRot());
            this.setYRot(this.getYRot() + delta * 0.15F);

            double dist = Math.sqrt(dx * dx + dz * dz);
            float targetPitch = (float) (-(Math.atan2(dy, dist) * -180D / Math.PI));
            this.setXRot(this.getXRot() + (targetPitch - this.getXRot()) * 0.1F);
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /** {@code onDeathUpdate}. */
    @Override
    protected void tickDeath() {

        if (this.getBeam())
            this.setBeam(false);

        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05D, 0));

        if (this.deathTime == -10) {
            MobSounds.atEntity(this, MobSounds.hbm("hbm:entity.chopperDamage"), 10.0F, 1.0F);
        }

        if (this.deathTime == 19 && !level().isClientSide) {
            ServerLevel server = (ServerLevel) level();
            CompoundTag data = new CompoundTag();
            data.putString("type", "tinytot");
            IParticleCreator.sendPacket(server, getX(), getY() + 0.5, getZ(), 250, data);
            MobSounds.at(level(), getX(), getY(), getZ(), MobSounds.hbm("hbm:weapon.mukeExplosion"), 15.0F, 1.0F);

            this.spawnAtLocation(new ItemStack(com.space_m.item.SpaceMaterialItems.CORE_ANGEL.get(), 1), 1);

            List<Player> players = level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(200, 200, 200));

            for (Player player : players) {
                player.sendSystemMessage(Component.literal("Stars are starting to flicker...").withStyle(ChatFormatting.RED));
            }

            CelestialBody body = CelestialBody.getBody(level());

            CBT_Invasion invasion = body.getTrait(CBT_Invasion.class);

            if (invasion == null) {
                body.modifyTraits(new CBT_Invasion(0, 122, false));
            }
        }

        super.tickDeath();
    }

    private void fireBeamAt(double x, double y, double z) {

        double spawnX = this.getX() - 1;
        double spawnY = this.getY() + 4;
        double spawnZ = this.getZ();

        EntityBulletBeamBase bullet = new EntityBulletBeamBase(this, XFactoryEnergy.energy_tesla_overcharge.setKnockback(0), 8F);

        bullet.setPos(spawnX, spawnY, spawnZ);

        Vec3NT delta = Vec3NT.createVectorHelper(x - spawnX, y - spawnY, z - spawnZ);

        bullet.setRotationsFromVector(delta);

        bullet.performHitscanExternal(250D);

        this.level().addFreshEntity(bullet);
        MobSounds.atEntity(this, SpaceSounds.ENTITY_BFASHOOT.get(), 5.0F, 1.0F);
    }

    private void fireDownwardBeamAt(double x, double y, double z) {

        double spawnX = x;
        double spawnY = y + 40;
        double spawnZ = z;

        EntityBulletBeamBase bullet = new EntityBulletBeamBase(this, XFactoryEnergy.energy_tesla_overcharge.setKnockback(0), 6F);

        bullet.setPos(spawnX, spawnY, spawnZ);

        Vec3NT delta = Vec3NT.createVectorHelper(0, -1, 0);

        bullet.setRotationsFromVector(delta);

        bullet.performHitscanExternal(250D);

        this.level().addFreshEntity(bullet);

        MobSounds.atEntity(this, SpaceSounds.ENTITY_BFASHOOT.get(), 5.0F, 1.0F);
    }

    private void rocketAttack(Entity e) {

        Vec3 heading = new Vec3(e.getX() - this.getX(), e.getY() + e.getBbHeight() / 2.0 - this.getY() - 0.5D, e.getZ() - this.getZ());

        heading = heading.normalize();

        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this, XFactoryFlamer.flame_balefire.setKnockback(0), 0.5F, 1, 0, 0, 0);

        double speed = 1.0;
        bullet.setDeltaMovement(heading.x * speed, heading.y * speed, heading.z * speed);

        this.level().addFreshEntity(bullet);
        MobSounds.atEntity(this, MobSounds.hbm("hbm:turret.richard_fire"), 5.0F, 1.0F);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEAM, (byte) 0);
        this.entityData.define(WX, 0);
        this.entityData.define(WY, 0);
        this.entityData.define(WZ, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAM, (byte) 0);
        builder.define(WX, 0);
        builder.define(WY, 0);
        builder.define(WZ, 0);
    }
    *///?}

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    public void setBeam(boolean b) {
        this.entityData.set(BEAM, (byte) (b ? 1 : 0));
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM) == 1;
    }

    public void setWaypoint(int x, int y, int z) {
        this.entityData.set(WX, x);
        this.entityData.set(WY, y);
        this.entityData.set(WZ, z);
    }

    public int getWaypointX() {
        return this.entityData.get(WX);
    }

    public int getWaypointY() {
        return this.entityData.get(WY);
    }

    public int getWaypointZ() {
        return this.entityData.get(WZ);
    }

    /** {@code isInRangeToRenderDist}: {@code distance < 500000}. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@org.jetbrains.annotations.Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }
}
