package com.space_m.dim.duna;

import com.space_m.celestial.CelestialBodyWorldSavedData;
import com.space_m.dim.SpaceDimensions;
import com.space_m.network.SpacePacket;
import com.space_m.network.SpacePacketHandler;
import com.space_m.time.CelestialLevelExtension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Staubsturm der Duna, Serverseite 1:1 aus {@code WorldProviderDuna} ({@code updateWeather},
 * {@code resetRainAndThunder}, {@code writeToNBT/readFromNBT}, {@code serialize}):
 * <ul>
 *   <li>Ruhephase 12 000-180 000 Ticks ({@code rand(168000) + 12000}), dann Sturm der Staerke 0.25-1.0
 *       ({@code nextFloat() * 0.75 + 0.25}) fuer 12 000-24 000 Ticks.</li>
 *   <li>Neue Welt: Staerke 1, Zeitgeber 0 (Feldwerte des Providers) -> der erste Tick beendet den "Sturm".</li>
 *   <li>Schlafen ({@code resetRainAndThunder}): Sturm aus, neue Ruhephase.</li>
 *   <li>{@code isDaytime()}: ab Staerke 0.2 ist es nie Tag (Mixin {@code DunaDayMixin}, beide Seiten).</li>
 * </ul>
 * Gespeichert in {@link CelestialBodyWorldSavedData#providerData()} der Duna ({@code dustStormTimer},
 * {@code dustStormIntensity} wie im Original-NBT). Der Client bekommt die Staerke ({@code serialize}, Original ueber
 * {@code PermaSyncHandler} jeden Tick) bei jeder Aenderung und jede Sekunde; Glaettung, Nebel und Staubpartikel
 * macht der Himmel ({@code CelestialSkyProvider.Duna}, {@code DunaClient}).
 */
public final class DunaDustStorm implements CelestialLevelExtension {

    public static final DunaDustStorm INSTANCE = new DunaDustStorm();

    /** Original S2C-Teil von {@code WorldProviderDuna.serialize}: Sturmstaerke. */
    public static final class Packet implements SpacePacket {
        public final float intensity;

        public Packet(float intensity) {
            this.intensity = intensity;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeFloat(intensity);
        }

        public static Packet decode(FriendlyByteBuf buf) {
            return new Packet(buf.readFloat());
        }
    }

    /** Zustand der Duna-Dimension (Original: Felder des {@code WorldProviderDuna}). */
    private static final class State {
        int dustStormTimer = 0;
        float dustStormIntensity = 1;
    }

    private static volatile State state;
    private static volatile ServerLevel stateLevel;

    /** Vom Server synchronisierte Staerke (Client; {@code dustStormIntensity} nach {@code deserialize}). */
    public static volatile float clientIntensity = 0;

    private DunaDustStorm() {}

    public static void init() {
        CelestialLevelExtension.register(SpaceDimensions.DUNA, INSTANCE);
    }

    private static State state(ServerLevel level) {
        State s = state;
        if (s == null || stateLevel != level) {
            s = new State();
            CompoundTag nbt = CelestialBodyWorldSavedData.get(level).providerData();
            // Original: readFromNBT nur bei vorhandenen Daten, sonst Feldwerte (Staerke 1, Zeitgeber 0)
            if (nbt.contains("dustStormIntensity")) {
                s.dustStormTimer = nbt.getInt("dustStormTimer");
                s.dustStormIntensity = nbt.getFloat("dustStormIntensity");
            }
            state = s;
            stateLevel = level;
        }
        return s;
    }

    private static void save(ServerLevel level, State s, boolean dirty) {
        CelestialBodyWorldSavedData data = CelestialBodyWorldSavedData.get(level);
        data.providerData().putInt("dustStormTimer", s.dustStormTimer);
        data.providerData().putFloat("dustStormIntensity", s.dustStormIntensity);
        if (dirty) data.setDirty();
    }

    /** Aktuelle Sturmstaerke ({@code dustStormIntensity}) der Duna, 0 ausserhalb. Server: Zustand, Client: Sync. */
    public static float intensity(Level level) {
        if (level == null || !SpaceDimensions.DUNA.equals(level.dimension())) return 0;
        if (level instanceof ServerLevel server) return state(server).dustStormIntensity;
        return clientIntensity;
    }

    /** Original {@code WorldProviderDuna.isDaytime}: {@code dustStormIntensity >= 0.2F} -> kein Tag. */
    public static boolean suppressesDaytime(Level level) {
        return intensity(level) >= 0.2F;
    }

    @Override
    public void updateWeather(ServerLevel level) {
        State s = state(level);
        float before = s.dustStormIntensity;

        if (s.dustStormTimer <= 0) {
            if (s.dustStormIntensity >= 0.05F) {
                s.dustStormIntensity = 0;
                s.dustStormTimer = level.random.nextInt(168000) + 12000;
            } else {
                s.dustStormIntensity = level.random.nextFloat() * 0.75F + 0.25F;
                s.dustStormTimer = level.random.nextInt(12000) + 12000;
            }
        }

        s.dustStormTimer--;

        boolean changed = before != s.dustStormIntensity;
        long t = level.getGameTime();
        save(level, s, changed || t % 20 == 0);
        if (changed || t % 20 == 0) sync(level, s.dustStormIntensity);
    }

    @Override
    public void resetRainAndThunder(ServerLevel level) {
        State s = state(level);
        s.dustStormIntensity = 0;
        s.dustStormTimer = level.random.nextInt(168000) + 12000;
        save(level, s, true);
        sync(level, 0);
    }

    private static void sync(ServerLevel level, float intensity) {
        SpacePacketHandler.sendToDimension(level, SpacePacketHandler.DUNA_DUST_STORM, new Packet(intensity));
    }

    /** Neuer Spieler in der Duna: sofort aktuellen Stand schicken (Original: PermaSync jeden Tick). */
    public static void syncTo(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level && SpaceDimensions.DUNA.equals(level.dimension())) {
            SpacePacketHandler.sendToPlayer(player, SpacePacketHandler.DUNA_DUST_STORM, new Packet(state(level).dustStormIntensity));
        }
    }

    /** Nur fuer Tests: Zustand setzen. */
    public static void setForTest(ServerLevel level, int timer, float intensity) {
        State s = state(level);
        s.dustStormTimer = timer;
        s.dustStormIntensity = intensity;
        save(level, s, true);
    }

    /** Nur fuer Tests: Zeitgeber lesen. */
    public static int timerForTest(ServerLevel level) {
        return state(level).dustStormTimer;
    }
}
