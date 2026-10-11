package com.space_m.dim.eve.client;

import com.space_m.dim.eve.EveBlocks;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;

/**
 * Client-Teil der Eve (Original {@code ClientProxy}: {@code bindTileEntitySpecialRenderer(TileEntityLightningVolcano,
 * new RenderVol2())}); Quecksilber wird durchscheinend gezeichnet.
 */
public final class EveClient {

    private EveClient() {}

    /** Aus {@code SpaceClient.setup} (Client-Setup, Registries sind gefuellt). */
    public static void setup() {
        BlockEntityRendererRegistry.register(EveBlocks.ELECTRIC_VOLCANO_BE.get(), RenderVol2::new);
        // BlockFluidBase.getRenderBlockPass() == 1 (durchscheinend), wie HBM WorldFluids
        RenderTypeRegistry.register(RenderType.translucent(), EveBlocks.MERCURY_FLUID.get(), EveBlocks.MERCURY_FLUID_FLOWING.get());
    }
}
