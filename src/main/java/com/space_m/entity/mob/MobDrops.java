package com.space_m.entity.mob;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Pluenderungsstufe fuer die Code-Drops ({@code dropFewItems(hitByPlayer, looting)} der 1.7.10). 1.20.1 reicht sie an
 * {@code dropCustomDeathLoot} weiter; 1.21.1 nicht mehr - dort aus der Hauptwaffe des Verursachers gelesen
 * (wie {@code EnchantmentHelper.getMobLooting} der 1.7.10).
 */
public final class MobDrops {

    private MobDrops() {}

    public static int looting(LivingEntity victim, DamageSource source) {
        //? if < 1.21.1 {
        return source.getEntity() instanceof LivingEntity attacker
                ? net.minecraft.world.item.enchantment.EnchantmentHelper.getMobLooting(attacker) : 0;
        //?} else {
        /*if (!(source.getEntity() instanceof LivingEntity attacker)) return 0;
        var reg = victim.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                reg.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), attacker);
        *///?}
    }
}
