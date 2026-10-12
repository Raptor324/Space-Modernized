package com.space_m.rocket.transport;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Clientseitige Partikel der Transporter-Rampe. Das Original ruft {@code ExplosionLarge.spawnShock} im Client-Zweig
 * auf (dort ein Server-Paket); HBMs {@code ExplosionLarge.spawnShock} sendet nur vom Server. Gleiche Daten
 * ({@code smoke}/{@code shock}, y+0,5) hier direkt ueber {@code ParticleEffectClient.effectNTNow} - Muster
 * {@code ParticleUtil.spawnGasFlame} (Client-Klasse wird nur im Client-Zweig geladen).
 */
final class TransportClientEffects {
    private TransportClientEffects() {}

    static void shock(Level level, double x, double y, double z, int count, double strength) {
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
