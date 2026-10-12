package com.space_m.orbit;

import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;

import net.minecraft.world.level.Level;

/**
 * Server-/gemeinsamer Teil von NTM Space {@code WorldProviderOrbit} (der Himmel - {@code updateSky}, Sternen-/
 * Sonnenhelligkeit, Himmelswinkel - liegt in {@code CelestialSkyProvider.Orbit}/{@code OrbitSkyRenderer}, R1-C).
 * Die Dimension selbst ist ein Datapack ({@code data/space_m/dimension/orbit.json}): flacher Generator ohne Schichten
 * = {@code ChunkProviderOrbit} (leere Chunks, Biom "Space" = {@code BiomeGenOrbit}), kein Wetter/Blitz/Schnee
 * ({@code updateWeather}/{@code canDoLightning}/{@code canDoRainSnowIce}, {@code CelestialWeather}), keine Wolken.
 * Original-Klasse: WorldProviderOrbit
 * Original-Klasse: ChunkProviderOrbit
 * Original-Klasse: BiomeGenOrbit
 */
public final class OrbitWorld {

    private OrbitWorld() {}

    // Orbit at an altitude that provides an hour-long realtime orbit (game time is fast so we go slow)
    // We want a consistent orbital period to prevent orbiting too slow or fast (both for player comfort and feel)
    private static final float ORBITAL_PERIOD = 7200;

    public static float getOrbitalAltitude(CelestialBody body) {
        return getAltitudeForPeriod(body.massKg, ORBITAL_PERIOD);
    }

    // r = ∛[(G x Me x T2) / (4π2)]
    private static float getAltitudeForPeriod(float massKg, float period) {
        return (float) Math.cbrt((AstronomyUtil.GRAVITATIONAL_CONSTANT * massKg * (period * period)) / (4 * Math.PI * Math.PI));
    }

    /** Original {@code getSunPower(x, z)}: Transfer -> Lerp der Sonnenleistung, sonst mal (1 - Finsternis). */
    public static float getSunPower(Level level, int x, int z) {
        OrbitalStation station = OrbitalStation.getStationFromPosition(level, x, z);

        double progress = station.getTransferProgress(0);
        float sunPower = station.orbiting.getSunPower();
        if (progress > 0) {
            return (float) OrbitalStation.lerp(progress, sunPower, station.target.getSunPower());
        }

        double eclipseAmount = station.getEclipseAmount(level);

        return sunPower * (1 - (float) eclipseAmount);
    }
}
