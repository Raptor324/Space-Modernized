package com.space_m.command;

import dev.architectury.event.events.common.CommandRegistrationEvent;

/**
 * Befehle des Addons (Original {@code MainRegistry.serverStart}: {@code CommandStations}, {@code CommandTotalTime};
 * {@code ModEventHandler.onCommand}: planetares {@code /weather}). Brigadier ueber Architectury (beide Loader);
 * das Ereignis kommt nach den Vanilla-Befehlen, daher kann {@code /weather} ueberlagert werden.
 */
public final class SpaceCommands {
    private SpaceCommands() {}

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, ctx, selection) -> {
            CommandStations.register(dispatcher);
            CommandTotalTime.register(dispatcher);
            PlanetaryWeatherCommand.register(dispatcher);
        });
    }
}
