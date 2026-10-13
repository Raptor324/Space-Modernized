package com.space_m.fork.trinket.client;

import com.space_m.fork.trinket.ForkTrinkets;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;

/** E11 Client: Renderer der Fork-Figuren (Original {@code ClientProxy}: RenderBobble/RenderPlushie). */
public final class ForkTrinketClient {

    private ForkTrinketClient() {}

    /** Aus {@code SpaceClient.init()}. */
    public static void init() {
        // Bobmazon-Katalog auch auf dem Client (Liste wird dort angezeigt), idempotent
        dev.architectury.event.events.client.ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(p -> ForkTrinkets.addBobmazonOffers());
        ClientLifecycleEvent.CLIENT_SETUP.register(mc -> BlockEntityRendererRegistry.register(ForkTrinkets.TRINKET_BE.get(), ForkTrinketRenderer::new));
    }
}
