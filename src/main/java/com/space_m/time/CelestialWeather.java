package com.space_m.time;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.celestial.trait.CBT_Weather;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * Wetter je Himmelskoerper (Serverseite), 1:1 {@code WorldProviderCelestial.updateWeather}/{@code resetRainAndThunder}
 * und {@code WorldProviderOrbit.updateWeather} des Originals. Vanilla teilt das Wetter aller Dimensionen mit der
 * Overworld ({@code DerivedLevelData}); hier kommt es aus dem {@link CBT_Weather}-Trait des Koerpers.
 *
 * <p>Gilt fuer Kerbin (Overworld - das Original ersetzt deren Provider durch {@code WorldProviderEarth}), die 10
 * Planeten-/Monddimensionen und den Orbit (nie Wetter). Nether/End/fremde Dimensionen bleiben Vanilla.
 * Der Client erhaelt Regen-/Gewitterstaerke ueber die Vanilla-Pakete ({@code ClientboundGameEventPacket}, nur an
 * Spieler der Dimension).
 */
public final class CelestialWeather {
    private CelestialWeather() {}

    /** Original: {@code provider instanceof WorldProviderCelestial} (Overworld + Planeten/Monde). */
    @Nullable
    public static CelestialBody celestialBody(Level level) {
        if (level.dimension() != Level.OVERWORLD && !SpaceDimensions.BODIES.contains(level.dimension())) return null;
        return CelestialBody.getBody(level);
    }

    /**
     * Ersetzt {@code ServerLevel.advanceWeatherCycle} fuer Himmelskoerper-Dimensionen.
     * @return true = behandelt (Vanilla-Wetter wird uebersprungen)
     */
    public static boolean updateWeather(ServerLevel level) {
        boolean orbit = SpaceDimensions.isOrbit(level.dimension());
        CelestialBody body = orbit ? null : celestialBody(level);
        if (!orbit && body == null) return false;

        boolean wasRaining = level.isRaining();
        float oldRain = level.getRainLevel(1.0F);
        float oldThunder = level.getThunderLevel(1.0F);
        float rain = 0.0F;
        float thunder = 0.0F;

        if (!orbit && CBT_Weather.supportsWeather(body)) { // hasWeatherCycle()
            rain = oldRain;
            thunder = oldThunder;
            CBT_Weather weather = CBT_Weather.ensureTrait(body);
            if (weather != null && weather.updateForTick(level.getServer().getTickCount(), level.getRandom(), body)) {
                SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
                if (data != null) data.setDirty();
            }
            weather = body.getTrait(CBT_Weather.class);
            if (weather != null) {
                rain = weather.rainStrength;
                thunder = weather.thunderStrength;
            }
        }

        // Server: prev = aktuell (Level.setRainLevel setzt beide); der Client interpoliert aus den Paketen.
        level.setRainLevel(rain);
        level.setThunderLevel(thunder);

        if (!orbit) {
            CelestialLevelExtension ext = CelestialLevelExtension.get(level.dimension());
            if (ext != null) ext.updateWeather(level);
        }

        // Vanilla-Synchronisation (ServerLevel.advanceWeatherCycle, nur Spieler dieser Dimension)
        var players = level.getServer().getPlayerList();
        if (oldRain != level.getRainLevel(1.0F)) {
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0F)), level.dimension());
        }
        if (oldThunder != level.getThunderLevel(1.0F)) {
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0F)), level.dimension());
        }
        if (wasRaining != level.isRaining()) {
            players.broadcastAll(new ClientboundGameEventPacket(wasRaining ? ClientboundGameEventPacket.STOP_RAINING : ClientboundGameEventPacket.START_RAINING, 0.0F), level.dimension());
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0F)), level.dimension());
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0F)), level.dimension());
        }
        return true;
    }

    /**
     * Original {@code resetRainAndThunder} (alle Spieler haben geschlafen): Wetter klar fuer 12000..180000 Ticks,
     * Ortszeit auf "local morning". Orbit: nichts (eigener Provider ohne Ueberschreibung).
     */
    public static void resetRainAndThunder(ServerLevel level) {
        CelestialBody body = celestialBody(level);
        if (body == null) return;

        CBT_Weather weather = CBT_Weather.ensureTrait(body);
        if (weather != null) {
            weather.forceClear(level.getRandom(), level.getRandom().nextInt(168000) + 12000);
            SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
            if (data != null) data.setDirty();
        }

        level.setRainLevel(0.0F);
        level.setThunderLevel(0.0F);

        CelestialLevelExtension ext = CelestialLevelExtension.get(level.dimension());
        if (ext != null) ext.resetRainAndThunder(level);

        if (level.dimension() == Level.OVERWORLD) return;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;

        // 1:1 Original: i < dayLength, daher i - i % dayLength == 0 -> Ortszeit 0 (Sonnenaufgang-Seite des Tages)
        long dayLength = (long) CelestialTime.getDayLength(body);
        long i = CelestialTime.getLocalTime(level) % dayLength;
        CelestialTime.setLocalTime(level, i - i % dayLength);
    }
}
