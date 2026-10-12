package com.space_m.machine.dyson.client;

import com.hbm_m.particle.helper.ParticleCreators;
import com.space_m.machine.dyson.DysonBlockEntities;
import com.space_m.machine.dyson.DysonBlocks;
import com.space_m.machine.misc.client.ItemRenderMachines;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;

/**
 * Client-Anmeldung des Dyson-Systems (Original {@code ClientProxy}: {@code RenderDysonReceiver},
 * {@code RenderDysonConverterAnatmogenesis}, {@code RenderDysonConverterHE}, {@code RenderDysonConverterTU},
 * {@code RenderDysonLauncher}; {@code effectNT}-Typen "spinlaunch" und "flare"; Item-Renderer).
 */
public final class DysonClient {

    private DysonClient() {}

    /** Aus {@code SpaceClient.setup} (Client-Setup). */
    public static void setup() {
        BlockEntityRendererRegistry.register(DysonBlockEntities.RECEIVER.get(), RenderDysonReceiver::new);
        BlockEntityRendererRegistry.register(DysonBlockEntities.CONVERTER_ANATMOGENESIS.get(), RenderDysonConverters.Anatmogenesis::new);
        BlockEntityRendererRegistry.register(DysonBlockEntities.CONVERTER_HE.get(), RenderDysonConverters.HE::new);
        BlockEntityRendererRegistry.register(DysonBlockEntities.CONVERTER_TU.get(), RenderDysonConverters.TU::new);
        BlockEntityRendererRegistry.register(DysonBlockEntities.LAUNCHER.get(), RenderDysonLauncher::new);

        ItemRenderMachines.register(DysonBlocks.DYSON_LAUNCHER, RenderDysonLauncher.ITEM);
        ItemRenderMachines.register(DysonBlocks.DYSON_RECEIVER, RenderDysonReceiver.ITEM);
        ItemRenderMachines.register(DysonBlocks.DYSON_CONVERTER_HE, RenderDysonConverters.HE.ITEM);
        ItemRenderMachines.register(DysonBlocks.DYSON_CONVERTER_TU, RenderDysonConverters.TU.ITEM);
        ItemRenderMachines.register(DysonBlocks.DYSON_CONVERTER_ANATMOGENESIS, RenderDysonConverters.Anatmogenesis.ITEM);

        // ClientProxy.effectNT: "spinlaunch" (Dyson-Startanlage), "flare" (ParticleUtil.spawnFlare vom Server)
        ParticleCreators.particleCreators().putIfAbsent("spinlaunch", DysonClientEffects::spinlaunch);
        ParticleCreators.particleCreators().putIfAbsent("flare", (level, player, rand, x, y, z, data) ->
                DysonClientEffects.spawnFlare(x, y, z, data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"), data.getFloat("scale")));
    }
}
