package com.space_m.time;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Planetenspezifische Erweiterung der Zeit-/Wetterlogik einer Dimension - Ersatz fuer die Ueberschreibungen in den
 * {@code WorldProvider<Planet>}-Unterklassen des Originals (z.B. {@code WorldProviderDuna}: Staubsturm in
 * {@code updateWeather}, {@code writeToNBT/readFromNBT}, {@code resetRainAndThunder}). Die Planeten-Agenten (R3)
 * melden ihre Erweiterung mit {@link #register} an; alle Methoden sind optional.
 *
 * <p>Alle Methoden laufen nur auf dem Server; Synchronisation zum Client uebernimmt die jeweilige Erweiterung.
 * Persistente Felder (Original {@code writeToNBT/readFromNBT} des Providers) liegen in
 * {@link com.space_m.celestial.CelestialBodyWorldSavedData#providerData()} der Dimension.
 */
public interface CelestialLevelExtension {

    /** Nach dem Wetter-Tick der Dimension (Original: {@code super.updateWeather()} in der Unterklasse). */
    default void updateWeather(ServerLevel level) {}

    /** Alle Spieler haben geschlafen (Original {@code resetRainAndThunder}, nach der Basislogik). */
    default void resetRainAndThunder(ServerLevel level) {}

    Map<ResourceKey<Level>, CelestialLevelExtension> EXTENSIONS = new HashMap<>();

    static void register(ResourceKey<Level> dimension, CelestialLevelExtension extension) {
        EXTENSIONS.put(dimension, extension);
    }

    @Nullable
    static CelestialLevelExtension get(ResourceKey<Level> dimension) {
        return EXTENSIONS.get(dimension);
    }
}
