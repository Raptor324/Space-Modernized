package com.space_m.machine.atmo.client;

/**
 * Client-Anmeldung der Atmo-Maschinen (Original {@code ClientProxy.registerTileEntitySpecialRenderer}): BER fuer
 * Kompressor und Emitter. Der Turm ist ein statisches OBJ-Blockmodell (Blockstate {@code machine_atmo_tower},
 * Transformation wie {@code RenderAtmoTower}: Ecke, 180 Grad, Ausrichtung des Deuterium-Turms).
 * Original-Klasse: RenderAtmoTower
 */
public final class AtmoClient {
    private AtmoClient() {}

    public static void setup() {
        // BERs migrated to Nucleus declarative specs in SpaceMachineRenderers
    }
}
