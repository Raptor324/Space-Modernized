package com.space_m.entity.mob;

import com.hbm_m.itempool.ItemPool;
import com.hbm_m.itempool.ItemPoolsComponent;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityScrapFish} (Sagan-See auf Laythe): Fisch (Wandern 0.8, Blick 4) in sieben Varianten; droppt
 * {@code rand(3 + looting) + 1} Teile aus dem Pool {@code POOL_MACHINE_PARTS}.
 *
 * <p>Die Variante wird wie im Original bei jeder Erzeugung zufaellig gewaehlt (Server und Client getrennt, nicht
 * gespeichert) - sie bestimmt nur die Textur.
 */
public class EntityScrapFish extends EntityFish implements IEntityEnumMulti {

    public enum ScrapFish {
        STEEL,
        ALUMINIUM,
        ISOTOPE,
        CADMIUM,
        TECH,
        BLOOD,
        HORROR,
    }

    public ScrapFish type;

    public EntityScrapFish(EntityType<? extends EntityScrapFish> entityType, Level world) {
        super(entityType, world, 0.8, 4.0F);
        type = ScrapFish.values()[world.random.nextInt(ScrapFish.values().length)];
    }

    @Override
    public Enum<?> getEnum() {
        return type;
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
        var pool = ItemPool.getPool(ItemPoolsComponent.POOL_MACHINE_PARTS);
        int j = random.nextInt(3 + looting) + 1;

        for (int k = 0; k < j; ++k) {
            this.spawnAtLocation(ItemPool.getStack(pool, random).copy(), 0.0F);
        }
    }
}
