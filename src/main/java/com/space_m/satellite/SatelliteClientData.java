package com.space_m.satellite;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.space_m.celestial.CelestialBody;

/**
 * Client-Satelliten (Original {@code SatelliteSavedData.clientSats/clientSatsByDimension} mit
 * {@code getClientSats()} / {@code getClientSats(dimensionId)}), gefuellt vom {@link SatelliteSyncPacket}.
 * Schluessel = Koerpername statt Dimensionsnummer. Nur Daten, keine Client-Klassen (auch von gemeinsamem Code lesbar).
 */
public final class SatelliteClientData {

    private SatelliteClientData() {}

    private static Map<Integer, ClientSatellite> clientSats = new HashMap<>();
    private static Map<String, Map<Integer, ClientSatellite>> clientSatsByBody = new HashMap<>();

    public static void setClientSats(Map<Integer, ClientSatellite> sats) {
        clientSats = sats != null ? sats : new HashMap<>();
    }

    /** Satelliten des Koerpers unter dem Spieler (im Orbit: des umkreisten). */
    public static Map<Integer, ClientSatellite> getClientSats() {
        return clientSats;
    }

    public static void setClientSatsByBody(Map<String, Map<Integer, ClientSatellite>> satsByBody) {
        clientSatsByBody = satsByBody != null ? satsByBody : new HashMap<>();
    }

    public static Map<Integer, ClientSatellite> getClientSats(CelestialBody body) {
        if (body == null) return Collections.emptyMap();
        Map<Integer, ClientSatellite> sats = clientSatsByBody.get(body.name);
        return sats != null ? sats : Collections.emptyMap();
    }

    /** Beim Verlassen der Welt. */
    public static void clear() {
        clientSats = new HashMap<>();
        clientSatsByBody = new HashMap<>();
    }
}
