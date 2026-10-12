package com.space_m.entity.mob;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.entity.IRadiationImmune;
import com.space_m.atmosphere.ISuffocationImmune;
import com.space_m.entity.mob.ai.EntityMoonWalkHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityMoonCow} (Mun, Thatmo): Kuh mit Raumhelm, strahlungs- und erstickungsimmun, geht im Moonwalk
 * ({@link EntityMoonWalkHelper}), Geraeusche gedaempft (Lautstaerke 0.1, "muffled by helmet"). Drops: 0-2 Leder und
 * 1-3 Kaese (je + rand(1 + looting)). Werte der Vanilla-Kuh (10 Leben, Tempo 0.2), Groesse 0.9x1.3 (1.7.10).
 */
public class EntityMoonCow extends Cow implements IRadiationImmune, ISuffocationImmune {

    public EntityMoonCow(EntityType<? extends EntityMoonCow> entityType, Level world) {
        super(entityType, world);
        this.moveControl = new EntityMoonWalkHelper(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Cow.createAttributes();
    }

    @Nullable
    @Override
    public EntityMoonCow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return SpaceMobs.MOON_COW.get().create(level);
    }

    @Override
    protected float getSoundVolume() {
        return 0.1F; // muffled by helmet
    }

    //? if < 1.21.1 {
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);
    //?} else {
    /*@Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);
    *///?}
        int looting = MobDrops.looting(this, source);
        int j = this.random.nextInt(3) + this.random.nextInt(1 + looting);
        int k;

        for (k = 0; k < j; ++k) {
            this.spawnAtLocation(Items.LEATHER, 0);
        }

        j = this.random.nextInt(3) + 1 + this.random.nextInt(1 + looting);

        for (k = 0; k < j; ++k) {
            this.spawnAtLocation(com.hbm_m.item.ModItems.CHEESE.get(), 0);
        }
    }
}
