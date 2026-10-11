package com.space_m.machine.atmo.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.atmo.AtmosphericCompressorBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderAtmosphericCompressor}: Ecke als Ursprung, 180 Grad, Ausrichtung wie der Deuterium-Turm,
 * Gehaeuse {@code Body_Cylinder}; Luefter {@code Fan_Cylinder.001} dreht um die Achse (-0,19 | 0,19).
 * Original-Klasse: RenderAtmosphericCompressor
 */
public class AtmosphericCompressorRenderer implements HbmBerBounds<AtmosphericCompressorBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/block/machines/atmo_vent.obj"));
    public static final ResourceLocation TEX = SpaceMod.id("textures/block/machine/atmo_vent.png");

    public AtmosphericCompressorRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(AtmosphericCompressorBlockEntity atmo, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180F));

        switch (ObjBerHelper.meta(atmo)) {
            case 2 -> ps.translate(0F, 0F, -1F);
            case 3 -> { ps.mulPose(Axis.YP.rotationDegrees(180F)); ps.translate(1F, 0F, 0F); }
            case 4 -> { ps.mulPose(Axis.YP.rotationDegrees(90F)); ps.translate(1F, 0F, -1F); }
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270F));
            default -> { }
        }

        // GL_CULL_FACE aus
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Body_Cylinder", ps, vc, light);

        float rot = atmo.prevRot + (atmo.rot - atmo.prevRot) * f;
        ps.translate(-0.19D, 0D, 0.19D);
        ps.mulPose(Axis.YN.rotationDegrees(rot));
        ps.translate(0.19D, 0D, -0.19D);
        MODEL.renderPart("Fan_Cylinder.001", ps, vc, light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(AtmosphericCompressorBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
