package com.space_m.travel;

import com.space_m.command.SpaceCommands;
import com.space_m.time.CelestialTime;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;

/**
 * Verdrahtung von Reise, Befehlen und Zeit/Wetter je Himmelskoerper (Agent R1-D), loaderneutral ueber Architectury.
 * Aufruf einmal aus {@code SpaceRegistry.init()}.
 */
public final class SpaceTravel {
    private SpaceTravel() {}

    public static void init() {
        // Original ModEventHandler.worldTick (Phase END): CelestialTeleporter.runQueuedTeleport()
        TickEvent.SERVER_LEVEL_POST.register(level -> CelestialTeleporter.runQueuedTeleport(level.getServer()));
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            CelestialTeleporter.clearQueue();
            CelestialTime.clearCaches();
        });

        SpaceCommands.init();
        CelestialTime.init();
    }
}
