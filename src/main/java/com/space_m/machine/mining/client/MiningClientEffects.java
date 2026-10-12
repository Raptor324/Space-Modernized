package com.space_m.machine.mining.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Clientseitige Partikel des Gas-Docks. Das Original ruft {@code ExplosionLarge.spawnShock} im Client-Zweig auf; HBMs
 * {@code ExplosionLarge.spawnShock} sendet nur vom Server. Gleiche Daten ({@code smoke}/{@code shock}, y+0,5) direkt ueber
 * {@code ParticleEffectClient.effectNTNow} (Kopie von {@code TransportClientEffects}, dort paketintern). Wird nur aus dem
 * Client-Zweig des Ticks geladen.
 */
public final class MiningClientEffects {
    private MiningClientEffects() {}

    public static void shock(Level level, double x, double y, double z, int count, double strength) {
        if (!level.isClientSide) return;
        CompoundTag data = new CompoundTag();
        data.putString("type", "smoke");
        data.putString("mode", "shock");
        data.putInt("count", count);
        data.putDouble("strength", strength);
        data.putDouble("posX", x);
        data.putDouble("posY", y + 0.5);
        data.putDouble("posZ", z);
        com.hbm_m.particle.helper.ParticleEffectClient.effectNTNow(data);
    }
}
