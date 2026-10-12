package com.space_m.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntitySifterEel} (Laythe): Fisch (Wandern 1.8, Blick 8) in sechs Varianten, ohne Drops
 * ({@code EntityLiving.getDropItem} = nichts). Variante zufaellig je Erzeugung (nur Textur, wie im Original).
 */
public class EntitySifterEel extends EntityFish implements IEntityEnumMulti {

    public enum SifterEel {
        PLAIN,
        FAST,
        EXOTIC,
        PHASED,
        ELEMENTAL,
        PERFECT,
    }

    public SifterEel type;

    public EntitySifterEel(EntityType<? extends EntitySifterEel> entityType, Level world) {
        super(entityType, world, 1.8, 8.0F);
        type = SifterEel.values()[world.random.nextInt(SifterEel.values().length)];
    }

    @Override
    public Enum<?> getEnum() {
        return type;
    }
}
