package com.space_m.entity.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityScutterfish} (alle Laythe-Biome, Schwarm 4): Fisch (Wandern 1.5, Blick 6), droppt
 * {@code scuttertail} ({@code getDropItem} -> Vanilla-{@code dropFewItems}: rand(3), mit Pluenderung + rand(looting+1)).
 */
public class EntityScutterfish extends EntityFish {

    public EntityScutterfish(EntityType<? extends EntityScutterfish> entityType, Level world) {
        super(entityType, world, 1.5, 6.0F);
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
        com.hbm_m.entity.mob.CreeperDrops.dropDefault(this, com.space_m.item.SpaceMaterialItems.SCUTTERTAIL.get(), MobDrops.looting(this, source));
    }
}
