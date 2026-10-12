package com.space_m.rocket;

import dev.architectury.event.events.common.LifecycleEvent;

/**
 * R5-A Einstieg (gemeinsam): Teile-Tabelle nach der Registrierung an die Items binden (Original
 * {@code MissilePart.registerAllPartsServer} in {@code MainRegistry.PostLoad}; Satelliten aus spaeteren Runden werden bei
 * Bedarf nachgezogen).
 */
public final class SpaceRocket {

    private SpaceRocket() {}

    public static void init() {
        LifecycleEvent.SETUP.register(RocketPart::resolve);
        LifecycleEvent.SERVER_STARTING.register(server -> RocketPart.resolve());
    }
}
