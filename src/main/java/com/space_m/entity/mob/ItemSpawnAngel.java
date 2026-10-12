package com.space_m.entity.mob;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.special.ItemChopper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code spawn_angel} ({@code ItemChopper}, NTM Space): setzt den Radioaktiven Engel 25 Bloecke ueber dem
 * angeklickten Block bzw. der Fluessigkeit ab (Benutzung/Namensschild/Verbrauch wie HBMs {@link ItemChopper}).
 */
public class ItemSpawnAngel extends ItemChopper {

    public ItemSpawnAngel(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public Entity spawnCreature(Level world, double x, double y, double z) {
        EntityBFAngel entity = SpaceMobs.BF_ANGEL.get().create(world);
        if (entity == null) return null;
        y += 25;

        entity.moveTo(x, y, z, Mth.wrapDegrees(world.random.nextFloat() * 360.0F), 0.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yBodyRot = entity.getYRot();
        if (world instanceof ServerLevel sl)
            //? if < 1.21.1 {
            entity.finalizeSpawn(sl, sl.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWN_EGG, null, null);
            //?} else {
            /*entity.finalizeSpawn(sl, sl.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            *///?}
        world.addFreshEntity(entity);
        return entity;
    }
}
