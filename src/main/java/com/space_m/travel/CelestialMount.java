package com.space_m.travel;

import net.minecraft.server.level.ServerPlayer;

/**
 * Fahrzeug, das nach einem {@link CelestialTeleporter}-Wechsel mit seinem Spieler neu verbunden werden muss.
 * Original: {@code if(newEntity instanceof EntityRideableRocket) ((EntityRideableRocket) newEntity).setThrower(playerMP)}
 * ("Ensure rocket stickiness") - die Rakete (R5) implementiert dieses Interface.
 */
public interface CelestialMount {
    void onCelestialTeleport(ServerPlayer rider);
}
