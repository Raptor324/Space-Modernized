package com.space_m.satellite;

import com.hbm_m.satellite.Satellite;

/**
 * 1:1 NTM Space {@code SatelliteDysonRelay}: ein Satellit ohne eigene Logik; der Dyson-Empfaenger (R7) empfaengt nachts,
 * wenn auf seiner Frequenz ein Relais um den Koerper kreist ({@code sat instanceof SatelliteDysonRelay || sun > 0}).
 * Original-Klasse: SatelliteDysonRelay
 */
public class SatelliteDysonRelay extends Satellite {

    @Override
    public String getType() {
        return "DYSON_RELAY";
    }
}
