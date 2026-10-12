package com.space_m.rocket;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;

import net.minecraft.world.level.Level;

/**
 * Stationsdaten fuer Orbit-Laufwerke (Original {@code OrbitalStation.getStation(x, z)}: {@code name}, {@code orbiting},
 * {@code hasStation}, {@code recallPod}). R6 setzt die echte Implementierung mit {@link #set}; Standard = keine Station
 * (umkreist den Koerper des Abfragenden, kein Name, kein Drop-Pod).
 */
public interface RocketStationHooks {

    /** Original {@code station.name} ("" = unbenannt). */
    default String stationName(int x, int z) { return ""; }

    /** Original {@code station.hasStation}. */
    default boolean hasStation(int x, int z) { return false; }

    /** Original {@code station.orbiting} (bei {@code !hasStation} setzt das Original {@code CelestialBody.getBody(world)}). */
    @Nullable
    default CelestialBody orbiting(Level level, int x, int z) { return null; }

    /** Original {@code station.recallPod(target)}. */
    default boolean recallPod(Level level, int x, int z, ItemVOTVdrive.Destination target) { return false; }

    final class Holder {
        private static RocketStationHooks hooks = new RocketStationHooks() { };

        private Holder() {}
    }

    static RocketStationHooks get() { return Holder.hooks; }

    static void set(RocketStationHooks hooks) { Holder.hooks = hooks; }
}
