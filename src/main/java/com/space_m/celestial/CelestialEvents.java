package com.space_m.celestial;

import java.util.ArrayList;
import java.util.List;

import com.space_m.celestial.trait.CBT_Weather;
import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Serverseitige Ereignisse des Himmelskoerper-Modells (loaderneutral ueber Architectury), Original:
 * <ul>
 *   <li>{@code ModEventHandler.onServerTick} (START): {@code CBT_Weather.updateGlobalWeather()}, alle Traits
 *       {@code update(false, body)}, {@code CelestialBody.updateSwarms()}</li>
 *   <li>{@code ModEventHandler.onWorldTick} (END): einmal je Sekunde {@code CelestialBody.updateChemistry(world)}</li>
 *   <li>{@code PermaSyncPacket} je Spieler und Tick, Abschnitt CBT alle 5 Ticks -> {@link CelestialTraitsPacket}</li>
 * </ul>
 */
public final class CelestialEvents {
    private CelestialEvents() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;

        LifecycleEvent.SERVER_STARTING.register(CelestialBody::setServer);
        LifecycleEvent.SERVER_STOPPED.register(server -> CelestialBody.setServer(null));

        TickEvent.SERVER_PRE.register(CelestialEvents::onServerTickStart);
        TickEvent.SERVER_LEVEL_POST.register(CelestialEvents::onLevelTickEnd);
        TickEvent.SERVER_POST.register(CelestialEvents::onServerTickEnd);
    }

    private static void onServerTickStart(MinecraftServer server) {
        if (CelestialBody.server() != server) CelestialBody.setServer(server);

        CBT_Weather.updateGlobalWeather();
        for (CelestialBody body : CelestialBody.getAllBodies()) {
            List<CelestialBodyTrait> traits = new ArrayList<>(body.getTraits().values());
            for (CelestialBodyTrait trait : traits) {
                trait.update(false, body);
            }
        }

        // Dyson Swarms
        CelestialBody.updateSwarms();
    }

    private static void onLevelTickEnd(ServerLevel level) {
        // Once per second, run atmospheric chemistry
        if (level.getGameTime() % 20 == 0) {
            CelestialBody.updateChemistry(level);
        }
    }

    private static void onServerTickEnd(MinecraftServer server) {
        SolarSystemWorldSavedData data = null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // update a little less frequently to not blast the players with large packets
            if (player.level().getGameTime() % 5 != 1) continue;
            if (data == null) data = SolarSystemWorldSavedData.get(server);
            SpacePacketHandler.sendToPlayer(player, SpacePacketHandler.CELESTIAL_TRAITS, CelestialTraitsPacket.create(player, data));
        }
    }
}
