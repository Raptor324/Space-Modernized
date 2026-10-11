package com.space_m.dim.ike;

import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;

/** Client: Alkylbromid durchscheinend wie {@code BlockFluidBase.getRenderBlockPass() == 1}. */
public final class IkeClient {

    private IkeClient() {}

    public static void setup() {
        RenderTypeRegistry.register(RenderType.translucent(), IkeBlocks.BROMINE_FLUID.get(), IkeBlocks.BROMINE_FLUID_FLOWING.get());
    }
}
