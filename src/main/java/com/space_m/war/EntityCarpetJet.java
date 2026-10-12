package com.space_m.war;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.logic.EntityPlaneBase;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * NTM Space {@code EntityBomber.statFacCarpetJet} (Thatmo-Schlachtfeld): B-2-artiger Teppichbomber (Typ 9,
 * Aussehen 10) mit Xenon-Abgasstrahlen und Schleife {@code entity.jetloopbomb}. Fliegt 1,5-fach schneller als der
 * normale Bomber, Lebensdauer 150 Ticks, wirft zwischen Tick 10 und 70 alle 3 Ticks ein {@link EntityCarpetBomblet}
 * (Typ 9 = VNT-Explosion 4 mit Kreuzschaden 100) mit Streuung (wie Typ 0).
 *
 * <p>Abweichung (technisch): HBM-Modernized hat den Master-{@code EntityBomber} ohne Typ 9/Aussehen 10; das Addon
 * kann dessen paketinterne Felder nicht setzen und bringt den Carpet-Jet daher als eigene Entity auf HBMs
 * {@link EntityPlaneBase} (gleiche Flugphysik, Lebenspunkte, Absturz, Chunk-Laden).
 */
public class EntityCarpetJet extends EntityPlaneBase {

    int bombStart = 75;
    int bombStop = 125;
    int bombRate = 3;
    int type = 9;

    public EntityCarpetJet(EntityType<? extends EntityCarpetJet> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    /** Original {@code EntityBomber.statFacCarpetJet(world, x, y, z)}. */
    public static EntityCarpetJet statFacCarpetJet(Level world, double x, double y, double z) {
        EntityCarpetJet bomber = new EntityCarpetJet(WarEntities.CARPET_JET.get(), world);

        bomber.timer = 150;
        bomber.bombStart = 10;
        bomber.bombStop = 70;
        bomber.bombRate = 3;
        bomber.type = 9;

        bomber.fac(world, x, y, z);
        //bomber.addVelocity(0.6, 0, 0.6);

        return bomber;
    }

    /** Original {@code EntityBomber.getDataWatcher().getWatchableObjectByte(16)} = 10. */
    public int getPlaneType() {
        return 10;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;

        if (level().isClientSide) {
            WarEntities.clientTick.accept(this);
        }

        if (!level().isClientSide && this.health > 0 && this.tickCount > bombStart && this.tickCount < bombStop && this.tickCount % bombRate == 0) {
            Level world = level();
            world.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, HbmSoundsNT.get("hbm:entity.bombWhistle"), SoundSource.NEUTRAL, 10.0F, 0.9F + random.nextFloat() * 0.2F);
            EntityCarpetBomblet zeta = new EntityCarpetBomblet(WarEntities.CARPET_BOMBLET.get(), world);
            zeta.setPos(getX() + random.nextDouble() - 0.5, getY() - random.nextDouble(), getZ() + random.nextDouble() - 0.5);
            zeta.setBombType(type);
            Vec3 m = getDeltaMovement();
            // type == 0 || type == 9: Streuung
            zeta.setDeltaMovement(m.x + random.nextGaussian() * 0.15, 0, m.z + random.nextGaussian() * 0.15);
            zeta.updateRotation();
            world.addFreshEntity(zeta);
        }
    }

    /** Original {@code EntityBomber.fac}, Typ 9: Tempo x1,5. */
    public void fac(Level world, double x, double y, double z) {

        Vec3 vector = new Vec3(world.random.nextDouble() - 0.5, 0, world.random.nextDouble() - 0.5).normalize();
        double mult = com.hbm_m.config.ModClothConfig.get().enableBomberShortMode ? 1 : 2;
        vector = new Vec3(vector.x * mult, 0, vector.z * mult);

        this.moveTo(x - vector.x * 100, y + 50, z - vector.z * 100, 0.0F, 0.0F);
        this.loadNeighboringChunks((int) (x / 16), (int) (z / 16));

        this.setDeltaMovement(vector.x, 0.0D, vector.z);

        if (type == 9) {
            this.setDeltaMovement(vector.x * 1.5, 0.0D, vector.z * 1.5);
        }

        this.rotation();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        bombStart = nbt.getInt("bombStart");
        bombStop = nbt.getInt("bombStop");
        bombRate = Math.max(1, nbt.getInt("bombRate"));
        type = nbt.getInt("type");
        timer = nbt.contains("timer") ? nbt.getInt("timer") : 150;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("bombStart", bombStart);
        nbt.putInt("bombStop", bombStop);
        nbt.putInt("bombRate", bombRate);
        nbt.putInt("type", type);
        nbt.putInt("timer", timer);
    }
}
