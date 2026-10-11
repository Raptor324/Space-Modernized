package com.space_m.machine.atmo.client;

import com.space_m.blockentity.SpaceBlockEntities;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;

/**
 * Client-Anmeldung der Atmo-Maschinen (Original {@code ClientProxy.registerTileEntitySpecialRenderer}): BER fuer
 * Kompressor und Emitter. Der Turm ist ein statisches OBJ-Blockmodell (Blockstate {@code machine_atmo_tower},
 * Transformation wie {@code RenderAtmoTower}: Ecke, 180 Grad, Ausrichtung des Deuterium-Turms).
 * Original-Klasse: RenderAtmoTower
 */
public final class AtmoClient {
    private AtmoClient() {}

    public static void setup() {
        BlockEntityRendererRegistry.register(SpaceBlockEntities.ATMOSPHERIC_VENT.get(), AtmosphericCompressorRenderer::new);
        BlockEntityRendererRegistry.register(SpaceBlockEntities.ATMO_EMITTER.get(), AtmosphericEmitterRenderer::new);
    }
}
