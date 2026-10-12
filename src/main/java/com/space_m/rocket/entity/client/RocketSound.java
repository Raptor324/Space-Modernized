package com.space_m.rocket.entity.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 1:1 {@code AudioWrapperClient}/{@code AudioDynamic} aus {@code getLoopedSound(sound, x, y, z, volume, range, pitch,
 * keepAlive)}: keine Vanilla-Abschwaechung, Lautstaerke linear {@code (1 - Abstand/Reichweite) * Lautstaerke},
 * verstummt, wenn {@link #keepAlive()} laenger als {@code keepAlive} Ticks ausbleibt. Wie im Original wird die Tonhoehe
 * von {@code getLoopedSound} nicht uebernommen (immer 1).
 */
public class RocketSound extends AbstractTickableSoundInstance {

    private final float maxVolume;
    private final float range;
    private final int keepAlive;
    private int timeSinceKA;

    public RocketSound(SoundEvent event, double x, double y, double z, float volume, float range, int keepAlive, boolean repeat) {
        super(event, SoundSource.PLAYERS, RandomSource.create());
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxVolume = volume;
        this.volume = volume;
        this.range = range;
        this.keepAlive = keepAlive;
        this.looping = repeat;
        this.delay = 0;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.NONE;
        updateVolume();
    }

    public void updatePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void keepAlive() {
        this.timeSinceKA = 0;
    }

    public void stopSound() {
        this.stop();
    }

    private void updateVolume() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        float dist = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2) + Math.pow(z - player.getZ(), 2));
        this.volume = Math.max((dist / range) * -maxVolume + maxVolume, 0F);
    }

    @Override
    public void tick() {
        updateVolume();

        if (this.timeSinceKA > this.keepAlive) {
            this.stop();
        }
        this.timeSinceKA++;
    }

    /** Original: auch ausserhalb der Reichweite (Lautstaerke 0) weiterlaufen. */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    public boolean isPlaying() {
        return !isStopped() && Minecraft.getInstance().getSoundManager().isActive(this);
    }
}
