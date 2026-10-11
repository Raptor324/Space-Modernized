package com.space_m.machine.atmo.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.atmo.AtmosphericEmitterBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderAtmosphericEmitter} (Modell {@code outtake.obj}): Blockmitte, Metadaten 2/4/3/5 ->
 * 90/180/270/0 Grad, dann (-0,5 | 0 | 0,5); {@code Base} fest, {@code Fan} um die Hochachse ({@code -fan}).
 * Original-Klasse: RenderAtmosphericEmitter
 */
public class AtmosphericEmitterRenderer implements HbmBerBounds<AtmosphericEmitterBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/block/machines/outtake.obj"));
    public static final ResourceLocation TEX = SpaceMod.id("textures/block/machine/outtake.png");

    public AtmosphericEmitterRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(AtmosphericEmitterBlockEntity emitter, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        switch (ObjBerHelper.meta(emitter)) {
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        ps.translate(-0.5D, 0D, 0.5D);

        // GL_CULL_FACE aus
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Base", ps, vc, light);

        float rot = emitter.prevFan + (emitter.fan - emitter.prevFan) * f;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-rot));
        MODEL.renderPart("Fan", ps, vc, light);
        ps.popPose();

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(AtmosphericEmitterBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
