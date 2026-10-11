package com.space_m.celestial;

import com.hbm_m.api.space.SpaceApi;

/**
 * Start des Himmelskoerper-Modells (aus {@code SpaceRegistry.init()} im Mod-Konstruktor):
 * Sonnensystem aufbauen (Original {@code SolarSystem.init()} in {@code MainRegistry.preInit}), die
 * {@link CelestialEnvironment} bei HBM-Modernized anmelden und die Server-Ereignisse registrieren.
 * Der Client-Teil ({@code celestial.client.CelestialClient}) wird von {@code SpaceClient.init()} gestartet.
 */
public final class CelestialRegistry {
    private CelestialRegistry() {}

    private static final CelestialEnvironment ENVIRONMENT = new CelestialEnvironment();

    public static void init() {
        SolarSystem.init();
        SpaceApi.setEnvironment(ENVIRONMENT);
        CelestialEvents.register();
    }

    public static CelestialEnvironment environment() {
        return ENVIRONMENT;
    }
}
