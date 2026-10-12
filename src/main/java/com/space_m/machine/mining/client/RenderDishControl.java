package com.space_m.machine.mining.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.mining.TileEntityDishControl;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderDishControl}: Blockmitte, 90 Grad, Ausrichtung (2: 0, 4: 90, 3: 180, 5: 270), Teile
 * {@code dish_control} + {@code joystick}, {@code drive} nur wenn der verknuepfte StarDar ein Laufwerk hat.
 * Item: Inventar T(0,-0.5,0) S(6), sonst T(0.5,0,0), ganzes Modell.
 * Original-Klasse: RenderDishControl
 */
public class RenderDishControl implements HbmBerBounds<TileEntityDishControl> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/machines/dish_controller.obj"));
    public static final ResourceLocation TEX = SpaceMod.id("textures/models/machines/dish_controller.png");

    public RenderDishControl(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityDishControl control, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        switch (ObjBerHelper.meta(control)) {
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("dish_control", ps, vc, light);
        MODEL.renderPart("joystick", ps, vc, light);
        if (control.starDarHasDisk()) MODEL.renderPart("drive", ps, vc, light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityDishControl be) { return true; }
}
