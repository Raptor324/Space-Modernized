package com.space_m.entity.mob;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Original-Soundnamen ({@code "hbm:entity.chopperDamage"} ...) -> HBM-Events ({@link HbmSoundsNT#get}); Space-eigene
 * ({@code entity.bfaShoot}, {@code entity.bfatalk}, {@code entity.beep}) aus {@code SpaceSounds}.
 * {@code worldObj.playSoundAtEntity}/{@code entity.playSound} -> {@link #atEntity}, {@code playSoundEffect} -> {@link #at}.
 */
public final class MobSounds {

    private MobSounds() {}

    @Nullable
    public static SoundEvent hbm(String key) {
        return HbmSoundsNT.get(key);
    }

    /** {@code entity.playSound(name, vol, pitch)} / {@code playSoundAtEntity}: an der Entity, fuer alle hoerbar. */
    public static void atEntity(Entity e, @Nullable SoundEvent sound, float volume, float pitch) {
        if (sound == null || e.level().isClientSide) return;
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), sound, e.getSoundSource(), volume, pitch);
    }

    /** {@code worldObj.playSoundEffect(x, y, z, name, vol, pitch)}. */
    public static void at(Level level, double x, double y, double z, @Nullable SoundEvent sound, float volume, float pitch) {
        if (sound == null || level.isClientSide) return;
        level.playSound(null, x, y, z, sound, SoundSource.HOSTILE, volume, pitch);
    }
}
