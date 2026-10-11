package com.space_m.client.sky;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;

import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Daten anderer Runden, die der Himmel braucht, als Client-Schnittstelle mit Standardwerten (= "noch nichts davon"):
 * <ul>
 *   <li>R6 Orbit/Station: Client-Station ({@code OrbitalStation.clientStation}: Zustand, Ziel, Transfer-Fortschritt),
 *       Stationen am Himmel ({@code OrbitalStation.orbitingStations}), Satelliten ({@code SatelliteSavedData.getClientSats},
 *       {@code SatelliteBase.render/renderDefault}), Satelliten-Vorschau in der Hand.</li>
 *   <li>R8 Krieg/Nuke: {@code CelestialNukeShockHandler.getClientShocks(body)}.</li>
 * </ul>
 * Die zustaendige Runde setzt {@link #set(SkyHooks)} im Client-Setup. Nur Client.
 */
public interface SkyHooks {

    /** Original {@code OrbitalStation.StationState}. */
    enum StationState { ORBIT, LEAVING, TRANSFER, ARRIVING }

    /** Original {@code CelestialNukeShockHandler.MAX_ACTIVE_SHOCKS}. */
    int MAX_ACTIVE_SHOCKS = 4;

    /** Original {@code CelestialNukeShockHandler.ShockStatus}. */
    record ShockStatus(long time, float centerX, float centerY, float strength) {}

    /** Stationsposition fuer {@code renderStation} (Original {@code station.dX/dZ}). */
    record StationPos(int dX, int dZ) {}

    // ── R6: Client-Station ──

    /** {@code clientStation.orbiting}; null = Kerbin. */
    @Nullable default CelestialBody stationOrbiting() { return null; }

    /** {@code clientStation.target}; null = wie {@link #stationOrbiting()}. */
    @Nullable default CelestialBody stationTarget() { return null; }

    default StationState stationState() { return StationState.ORBIT; }

    /** {@code clientStation.getTransferProgress(partialTicks)}. */
    default double stationTransferProgress(float partialTicks) { return 0; }

    /** {@code clientStation.getUnscaledProgress(partialTicks)}. */
    default double stationUnscaledProgress(float partialTicks) { return 0; }

    /** {@code OrbitalStation.orbitingStations} (Stationen ueber dem aktuellen Koerper). */
    default List<StationPos> orbitingStations() { return Collections.emptyList(); }

    // ── R6: Satelliten ──

    /**
     * Zeichnet Bobmazon-Standardsatellit (nur Kerbin), alle Client-Satelliten und die Vorschau des gehaltenen
     * Satelliten (Original-Block in {@code SkyProviderCelestial.render} bei {@code visibility > 0.2}).
     * Matrix/Zustand: {@link SkyGL} (bereits um die Sonne rotiert).
     */
    default void renderSatellites(float partialTicks, ClientLevel level, float solarAngle) { }

    // ── R8: Nuklear-Erschuetterungen ──

    /** {@code CelestialNukeShockHandler.getClientShocks(body)}. */
    default List<ShockStatus> clientShocks(CelestialBody body) { return Collections.emptyList(); }

    // ── Registrierung ──

    final class Holder {
        private static SkyHooks hooks = new SkyHooks() { };

        private Holder() {}
    }

    static SkyHooks get() {
        return Holder.hooks;
    }

    static void set(SkyHooks hooks) {
        Holder.hooks = hooks;
    }
}
