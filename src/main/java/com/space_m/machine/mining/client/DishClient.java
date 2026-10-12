package com.space_m.machine.mining.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.machine.chem.client.SpaceMachineItemRenderer;
import com.space_m.machine.mining.DishMachines;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Client-Anmeldung Dish-Controller (Renderer, Item-Darstellung). Aus {@code SpaceClient.setup}. */
public final class DishClient {
    private DishClient() {}

    /** Aus {@code SpaceClient.init} (derzeit nichts). */
    public static void init() { }

    public static void setup() {
        BlockEntityRendererRegistry.register(DishMachines.DISH_CONTROL_BE.get(), RenderDishControl::new);

        // RenderDishControl.getRenderer(): Inventar T(0,-0.5,0) S(6); T(0.5,0,0), ganzes Modell
        SpaceMachineItemRenderer.register(DishMachines.DISH_CONTROL.get(), new SpaceMachineItemRenderer.Entry() {
            @Override public void inventory(PoseStack ps) { ps.translate(0, -0.5, 0); ps.scale(6F, 6F, 6F); }
            @Override public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ps.translate(0.5, 0, 0);
                RenderDishControl.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(RenderDishControl.TEX)), light);
            }
        });
    }
}
