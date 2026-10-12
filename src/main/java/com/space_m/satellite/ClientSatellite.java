package com.space_m.satellite;

import com.hbm_m.satellite.Satellite;

/**
 * Ein Satellit auf dem Client (Original: {@code SatelliteBase}-Instanz in {@code SatelliteSavedData.clientSats}):
 * die HBM-/Addon-Satelliteninstanz (Typ, Zusatzdaten wie {@code interp} der Railkanone) plus ihre Bahndaten.
 */
public final class ClientSatellite {

    public final int freq;
    public final Satellite satellite;
    public final SatelliteOrbit orbit;

    public ClientSatellite(int freq, Satellite satellite, SatelliteOrbit orbit) {
        this.freq = freq;
        this.satellite = satellite;
        this.orbit = orbit;
    }
}
