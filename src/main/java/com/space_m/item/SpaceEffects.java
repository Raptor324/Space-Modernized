package com.space_m.item;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.effect.HbmEffect;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.sound.HbmSoundsNT;
import com.space_m.SpaceMod;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * R4-B: neue Trankeffekte von NTM Space ({@code HbmPotion.run}, "GOD WATCHES YOU", Farbe 1118481, Symbol 14/0,
 * schlecht). {@code isReady}: Restdauer &lt;= 10 -> Ziel stirbt ({@code ModDamageSource.run}, absolut + durch Ruestung),
 * Antimaterie-Explosion 12, {@code weapon.mukeExplosion}; Nicht-Spieler werden entfernt.
 */
public final class SpaceEffects {

    private SpaceEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.MOB_EFFECT);

    public static final RegistrySupplier<MobEffect> RUN = EFFECTS.register("run", RunEffect::new);

    /** Original {@code ModDamageSource.run} ("death.attack.run"). */
    public static final ResourceKey<DamageType> RUN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, SpaceMod.id("run"));

    public static void init() {
        EFFECTS.register();
    }

    //? if < 1.21.1 {
    public static MobEffect holder(RegistrySupplier<MobEffect> e) { return e.get(); }
    //?} else {
    /*public static net.minecraft.core.Holder<MobEffect> holder(RegistrySupplier<MobEffect> e) {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e.get());
    }
    *///?}

    static DamageSource run(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RUN_DAMAGE));
    }

    /** 1:1 {@code HbmPotion.run}. */
    public static class RunEffect extends HbmEffect {

        public RunEffect() {
            super(MobEffectCategory.HARMFUL, 1118481, 14, 0);
        }

        @Override
        protected boolean isReady(int duration, int amplifier) {
            return duration <= 10;
        }

        @Override
        protected void tick(@NotNull LivingEntity entity, int amplifier) {
            Level level = entity.level();
            entity.hurt(run(level), 1000);
            entity.setHealth(0.0F);

            new ExplosionVNT(level, entity.getX(), entity.getY(), entity.getZ(), 12).makeAmat().explode();
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HbmSoundsNT.get("hbm:weapon.mukeExplosion"),
                    SoundSource.PLAYERS, 100.0F, 1.0F);

            if (!(entity instanceof Player)) entity.discard();
        }
    }
}
