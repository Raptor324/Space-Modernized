package com.space_m.celestial;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Anbindung der Orbit-Dimension an die Raumstationen (Original {@code OrbitalStation}, R6). Die Himmelskoerper-
 * Logik fragt nur ueber diese Schnittstelle; R6 setzt {@link #set(OrbitHooks)}. Standard = keine Stationen:
 * man umkreist Kerbin, ohne Transfer, Stationsschwerkraft 1; kuenstliche Schwerkraft wie Original an die
 * Atmosphaere gekoppelt ({@code AtmosphereEffects.hasGravity}, R2-A).
 */
public interface OrbitHooks {

    /** Original {@code HbmLivingProps.hasGravity} (Entity in Atmosphaere einer Station = kuenstliche Schwerkraft). */
    default boolean hasGravity(LivingEntity entity) { return com.space_m.atmosphere.AtmosphereEffects.hasGravity(entity); }

    /** Original {@code station.gravityMultiplier} der Station an x/z. */
    default float gravityMultiplier(Level level, int x, int z) { return 1.0F; }

    /** Original {@code station.orbiting}; null = Standard (Kerbin). */
    @Nullable
    default CelestialBody orbiting(Level level, int x, int z) { return null; }

    /** Original {@code station.hasStation} (fuer {@code CelestialBody.getTarget}). */
    default boolean hasStation(Level level, int x, int z) { return true; }

    /** Original {@code WorldProviderOrbit.getSunPower(x, z)} (Transfer-Lerp, Finsternis der Station). */
    default float sunPower(Level level, int x, int z) {
        CelestialBody orbiting = orbiting(level, x, z);
        return (orbiting != null ? orbiting : CelestialBody.getBody("kerbin")).getSunPower();
    }

    /** Stationen ueber dem Koerper des Spielers fuer das Sync-Paket ({@code {dX, dZ}}). */
    default List<int[]> syncedStations(ServerPlayer player) { return Collections.emptyList(); }

    // ── Registrierung ──

    final class Holder {
        private static OrbitHooks hooks = new OrbitHooks() { };

        private Holder() {}
    }

    static OrbitHooks get() {
        return Holder.hooks;
    }

    static void set(OrbitHooks hooks) {
        Holder.hooks = hooks;
    }
}
