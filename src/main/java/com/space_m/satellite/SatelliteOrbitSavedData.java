package com.space_m.satellite;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;
import com.space_m.platform.SavedDataHooks;
import com.space_m.platform.SpaceSavedData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Altbestand: Bahndaten ({@link SatelliteOrbit}) je Frequenz aus der Zeit vor W-R6C-1. Seitdem stehen sie wie im Original
 * im Satelliten selbst ({@code Satellite.addonData}, {@link SpaceSatellites#ORBIT_KEY}); diese SavedData wird nur noch
 * gelesen (Uebernahme beim ersten Zugriff), neue Satelliten schreiben hier nichts mehr.
 * Fehlt ein Eintrag (Satellit ueber einen HBM-Weg gestartet, z.B. Soyuz), gilt wie im Original {@code createFromId}
 * die registrierte Farbe der Klasse mit Standardbahn.
 */
public class SatelliteOrbitSavedData extends SpaceSavedData {

    public static final String NAME = "space_m_satellite_orbits";

    private final Map<Integer, SatelliteOrbit> orbits = new HashMap<>();

    public static SatelliteOrbitSavedData get(ServerLevel level) {
        return SavedDataHooks.get(level, NAME, SatelliteOrbitSavedData::new, SatelliteOrbitSavedData::load);
    }

    private static SatelliteOrbitSavedData load(CompoundTag nbt) {
        SatelliteOrbitSavedData data = new SatelliteOrbitSavedData();
        int count = nbt.getInt("satCount");
        for (int i = 0; i < count; i++) {
            SatelliteOrbit o = new SatelliteOrbit();
            o.readFromNBT(nbt.getCompound("sat_data_" + i), SpaceSatelliteRegistry.FALLBACK_COLOR);
            data.orbits.put(nbt.getInt("sat_freq_" + i), o);
        }
        return data;
    }

    @Override
    protected CompoundTag write(CompoundTag nbt) {
        nbt.putInt("satCount", orbits.size());
        int i = 0;
        for (Map.Entry<Integer, SatelliteOrbit> e : orbits.entrySet()) {
            CompoundTag data = new CompoundTag();
            e.getValue().writeToNBT(data);
            nbt.put("sat_data_" + i, data);
            nbt.putInt("sat_freq_" + i, e.getKey());
            i++;
        }
        return nbt;
    }

    /** Bahndaten des Satelliten auf {@code freq}; legt bei Bedarf die Standardbahn (registrierte Farbe) an. */
    public SatelliteOrbit getOrCreate(int freq, Satellite sat) {
        SatelliteOrbit o = orbits.get(freq);
        if (o == null) {
            o = SatelliteOrbit.withColor(SpaceSatelliteRegistry.getRegisteredColor(sat == null ? null : sat.getClass()));
            orbits.put(freq, o);
            setDirty();
        }
        return o;
    }

    public void put(int freq, SatelliteOrbit orbit) {
        orbits.put(freq, orbit);
        setDirty();
    }

    /** Eintraege ohne HBM-Satelliten entfernen (z.B. nach {@code /satellite descend}). */
    public void prune(SatelliteManager manager) {
        if (orbits.keySet().removeIf(freq -> !manager.isFreqTaken(freq))) setDirty();
    }

    public Map<Integer, SatelliteOrbit> all() {
        return orbits;
    }
}
