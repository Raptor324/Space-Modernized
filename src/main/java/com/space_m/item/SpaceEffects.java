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

    // E12 (Nutzerentscheidung): HbmPotion nitan / flashbang / slippery des Space-Forks (Farben/Symbole 1:1)
    public static final RegistrySupplier<MobEffect> NITAN = EFFECTS.register("nitan", NitanEffect::new);
    public static final RegistrySupplier<MobEffect> FLASHBANG = EFFECTS.register("flashbang", FlashbangEffect::new);
    public static final RegistrySupplier<MobEffect> SLIPPERY = EFFECTS.register("slippery", SlipperyEffect::new);

    /** Original {@code ModDamageSource.nitan} ("death.attack.nitan"). */
    public static final ResourceKey<DamageType> NITAN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, SpaceMod.id("nitan"));

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

    static DamageSource nitan(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(NITAN_DAMAGE));
    }

    /** 1:1 {@code HbmPotion.nitan}: jeden Tick; die dritte Dosis Ambrosia ({@code nitanCount == 3}) toetet. */
    public static class NitanEffect extends HbmEffect {
        public NitanEffect() { super(MobEffectCategory.BENEFICIAL, 8388736, 3, 1); }

        @Override
        protected boolean isReady(int duration, int amplifier) { return true; }

        @Override
        protected void tick(@NotNull LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) return;
            if (entity instanceof Player player && com.space_m.fork.ForkItems.getNitanCount(player) == 3) {
                entity.hurt(nitan(entity.level()), 1000);
            }
        }
    }

    /**
     * 1:1 {@code HbmPotion.flashbang}: Zombies/Skelette brennen 20 s, Langsamkeit (Dauer 5, Stufe 10). Im Original
     * liefert {@code isReady} fuer diesen Effekt false - die Wirkung laeuft also nie (und er wird nirgends verabreicht).
     */
    public static class FlashbangEffect extends HbmEffect {
        public FlashbangEffect() { super(MobEffectCategory.BENEFICIAL, 0xD0D0D0, 15, 1); }

        @Override
        protected boolean isReady(int duration, int amplifier) { return false; }

        @Override
        protected void tick(@NotNull LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) return;
            if (entity instanceof net.minecraft.world.entity.monster.Zombie || entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton) {
                com.hbm_m.platform.PlatformHooks.setSecondsOnFire(entity, 20);
            }
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 5, 10));
        }
    }

    /** 1:1 {@code HbmPotion.slippery} ("Butterfingers"): {@code motionY += 0.026} je Tick (im Original nie verabreicht). */
    public static class SlipperyEffect extends HbmEffect {
        public SlipperyEffect() { super(MobEffectCategory.BENEFICIAL, 0xD0D0D0, 15, 0); }

        @Override
        protected boolean isReady(int duration, int amplifier) { return true; }

        @Override
        protected void tick(@NotNull LivingEntity entity, int amplifier) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.026D, 0));
        }
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
