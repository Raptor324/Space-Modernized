package com.space_m.orbit.client;

import java.util.ArrayList;
import java.util.List;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.OrbitHooks;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.client.sky.SkyHooks;
import com.space_m.config.SpaceConfig;
import com.space_m.network.SpacePacketHandler;
import com.space_m.orbit.OrbitBlocks;
import com.space_m.orbit.OrbitalStation;
import com.space_m.orbit.StationSyncPacket;
import com.space_m.sound.SpaceSounds;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Client-Teil von R6-A: Stations-Sync, Port-Renderer, Station am Himmel ({@link SkyHooks}), Stationsbrummen
 * (Original {@code ModEventHandlerClient}: im Orbit mit kuenstlicher Schwerkraft {@code getLoopedSound(
 * "hbm:misc.stationhum", player, AUDIO_SHIP_HUM_VOLUME, 5.0F, 1.0F, 10)}).
 */
public final class OrbitClient {

    private OrbitClient() {}

    public static void init() {
        SpacePacketHandler.registerS2C(SpacePacketHandler.ORBITAL_STATION, StationSyncPacket::decode, (pkt, ctx) -> ctx.queue(pkt::apply));

        SkyHooks.set(new StationSkyHooks(SkyHooks.get()));

        ClientTickEvent.CLIENT_POST.register(OrbitClient::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            OrbitalStation.setClientStation(null);
            stopHum();
        });
    }

    /** Aus {@code SpaceClient.setup()}. */
    public static void setup() {
        BlockEntityRendererRegistry.register(OrbitBlocks.ORBITAL_STATION_BE.get(), RenderOrbitalStation::new);
    }

    // ── Station am Himmel (Dekorator, s. SPACE_R6_iface B) ────────────────────────────────────

    static final class StationSkyHooks implements SkyHooks {
        private final SkyHooks prev;

        StationSkyHooks(SkyHooks prev) {
            this.prev = prev;
        }

        @Override public CelestialBody stationOrbiting() { return OrbitalStation.clientStation().orbiting; }
        @Override public CelestialBody stationTarget() { return OrbitalStation.clientStation().target; }

        @Override
        public StationState stationState() {
            return StationState.values()[OrbitalStation.clientStation().state.ordinal()];
        }

        @Override public double stationTransferProgress(float partialTicks) { return OrbitalStation.clientStation().getTransferProgress(partialTicks); }
        @Override public double stationUnscaledProgress(float partialTicks) { return OrbitalStation.clientStation().getUnscaledProgress(partialTicks); }

        /** Original {@code OrbitalStation.orbitingStations} (aus dem Trait-Sync). */
        @Override
        public List<StationPos> orbitingStations() {
            List<StationPos> list = new ArrayList<>();
            for (int[] s : SolarSystemWorldSavedData.clientStations) list.add(new StationPos(s[0], s[1]));
            return list;
        }

        @Override public void renderSatellites(float partialTicks, ClientLevel level, float solarAngle) { prev.renderSatellites(partialTicks, level, solarAngle); }
        @Override public List<ShockStatus> clientShocks(CelestialBody body) { return prev.clientShocks(body); }
    }

    // ── Client-Tick ────────────────────────────────────────────────────────────────────────────

    private static HumSound shipHum;

    private static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stopHum();
            return;
        }

        boolean inOrbit = CelestialBody.inOrbit(mc.level);

        // Original: der Kern-TE laesst die Client-Station mitlaufen (stateTimer++), Server ueberschreibt jeden Tick
        if (inOrbit) OrbitalStation.clientStation().update(mc.level);

        if (inOrbit && OrbitHooks.get().hasGravity(player)) {
            if (shipHum == null || shipHum.isStopped() || !mc.getSoundManager().isActive(shipHum)) {
                shipHum = new HumSound(player);
                mc.getSoundManager().play(shipHum);
            }

            shipHum.updateVolume(SpaceConfig.get().audioShipHumVolume);
            shipHum.keepAlive();
        } else {
            stopHum();
        }
    }

    private static void stopHum() {
        if (shipHum != null) {
            shipHum.stopSound();
            shipHum = null;
        }
    }

    /** Original {@code AudioWrapper} mit Spieler-Bindung, 10 Ticks Keep-Alive. */
    static final class HumSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        private int keepAlive;

        HumSound(LocalPlayer player) {
            super(SpaceSounds.MISC_STATIONHUM.get(), SoundSource.AMBIENT, player.getRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = SpaceConfig.get().audioShipHumVolume;
            this.pitch = 1.0F;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.keepAlive = 10;
        }

        void keepAlive() {
            keepAlive = 10;
        }

        void updateVolume(float volume) {
            this.volume = volume;
        }

        void stopSound() {
            stop();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || --keepAlive < 0) {
                stop();
                return;
            }
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }
    }
}
