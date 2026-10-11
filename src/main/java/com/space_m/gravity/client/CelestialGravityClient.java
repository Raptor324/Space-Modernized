package com.space_m.gravity.client;

import com.space_m.celestial.CelestialBody;
import com.space_m.gravity.CelestialGravity;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

/**
 * Clientseite von {@link CelestialGravity}: schwebende Items im Orbit auch in der lokalen Vorhersage (Original nur
 * Server - dort sprang das Item zwischen lokalem Fallen und Server-Korrektur). Lebewesen laufen ueber den
 * gemeinsamen Lebewesen-Tick.
 */
public final class CelestialGravityClient {

    private CelestialGravityClient() {}

    public static void init() {
        ClientTickEvent.CLIENT_LEVEL_PRE.register(CelestialGravityClient::levelTick);
    }

    private static void levelTick(ClientLevel level) {
        if (!CelestialBody.inOrbit(level)) return;
        for (Entity e : level.entitiesForRendering()) CelestialGravity.floatItem(e);
    }
}
