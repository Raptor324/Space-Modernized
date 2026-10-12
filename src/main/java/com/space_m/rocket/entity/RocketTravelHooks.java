package com.space_m.rocket.entity;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Anbindung der fliegenden Rakete an Orbit-Dimension und Raumstationen (Original-Aufrufe aus
 * {@code EntityRideableRocket}: {@code SolarSystemWorldSavedData.addStation}, {@code OrbitalStation.setState(TRANSFER)},
 * {@code OrbitalStation.getStationFromPosition(..).update/getUnscaledProgress}, {@code data.removeStation},
 * {@code OrbitalStation.spawn}, {@code OrbitalStation.addStation}, {@code OrbitalStation.getPort},
 * {@code CompatExternal.getCoreFromPos -> TileEntityOrbitalStation/Launcher.dockRocket}). Die Stationen entstehen in R6;
 * R6 setzt die echte Implementierung mit {@link #set}.
 *
 * <p><b>Standard (ohne R6, technisch noetige Abweichung):</b> {@link #beginTransfer} liefert {@code null} - der
 * Himmelskoerper-Transfer laeuft dann <i>in der Rakete selbst</i> (gleiche Transferzeit
 * {@link #calculateTransferTime}, gleiche Fortschritts-/Stufentrennungs-/Geraeuschlogik), ohne Umweg ueber die
 * Orbit-Dimension; danach wird wie im Original direkt ueber dem Ziel gelandet. Ohne Stationen gibt es keine Docking-Ports.
 */
public interface RocketTravelHooks {

    /** Original {@code OrbitalStation.STATION_SIZE}. */
    int STATION_SIZE = 1024;

    /** Original {@code OrbitalStation.calculateTransferTime(distance, size, thrust)}. */
    static int calculateTransferTime(double distance, int size, float thrust) {
        return (int) (Math.log(1 + (distance * size / thrust * 100)) * 150);
    }

    /**
     * Original {@code beginCelestialTransfer}: {@code data.addStation(from)} + {@code station.setState(TRANSFER, time)},
     * {@code orbiting = from}, {@code target = to}.
     *
     * @return Rasterkoordinaten {@code {dX, dZ}} der Transfer-Station in der Orbit-Dimension oder {@code null}, wenn es
     *         keine Orbit-Dimension/Stationen gibt (dann Transfer in der Rakete, s. Klassenkommentar)
     */
    @Nullable
    default int[] beginTransfer(ServerLevel level, CelestialBody from, CelestialBody to, int transferTime) { return null; }

    /** Original {@code station.update(world)} der Station an x/z (nur Stationsmodus). */
    default void tickTransfer(Level level, int x, int z) {}

    /** Original {@code station.getUnscaledProgress(0)} (0..1, nur Stationsmodus). */
    default double transferProgress(Level level, int x, int z) { return 0; }

    /** Original {@code station.getUnscaledProgress(0) > 0.99 || station.state == ARRIVING} (nur Stationsmodus). */
    default boolean transferArrived(Level level, int x, int z) { return transferProgress(level, x, z) > 0.99; }

    /** Original {@code station.target} (HUD "Travelling to"). */
    @Nullable
    default CelestialBody transferTarget(Level level, int x, int z) { return null; }

    /** Original {@code SolarSystemWorldSavedData.get(world).removeStation(station)}. */
    default void endTransfer(Level level, int x, int z) {}

    /** Original {@code OrbitalStation.spawn(targetWorld, x, z)} - Stationskern anlegen, falls noch keiner existiert. */
    default void spawnStation(ServerLevel orbit, int x, int z) {}

    /** Original {@code OrbitalStation.addStation(x, z, body)} (Nutzlast {@code rp_station_core_20}). */
    default void addStation(int x, int z, CelestialBody orbiting) {}

    /** Original {@code OrbitalStation.getPort(x, z)}; null = (noch) kein Port geladen. */
    @Nullable
    default RocketPort getPort(Level level, int x, int z) { return null; }

    /**
     * Original {@code onUpdate}/{@code sizeSet}: steht die Rakete auf einem Stationsport bzw. Stationsstarter
     * ({@code CompatExternal.getCoreFromPos} an der Raketenspitze), dort andocken.
     */
    default void dockAt(Level level, BlockPos top, EntityRideableRocket rocket) {}

    /** Original {@code TileEntityOrbitalStation} (Docking-Port). */
    interface RocketPort {
        BlockPos pos();
        void despawnRocket();
        void reservePort();
        void dockRocket(EntityRideableRocket rocket);
    }

    // ── Registrierung ──

    final class Holder {
        private static RocketTravelHooks hooks = new RocketTravelHooks() { };

        private Holder() {}
    }

    static RocketTravelHooks get() {
        return Holder.hooks;
    }

    static void set(RocketTravelHooks hooks) {
        Holder.hooks = hooks;
    }
}
