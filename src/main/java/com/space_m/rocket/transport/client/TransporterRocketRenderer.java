package com.space_m.rocket.transport.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.MinerRocketRenderer;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.rocket.transport.TransporterRocketBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 NTM Space {@code RenderTransporterRocket}: Blockmitte, 90 Grad + Ausrichtung (2: 0, 4: 90, 3: 180, 5: 270),
 * {@code transporter_pad} Teil {@code base}; Tankarm {@code pipe} kippt um (0 | 0,75 | -0,75) bis -30 Grad, solange
 * die Rakete steht; die Rakete ({@code minerRocket}, ohne Rueckseitenausblendung) steigt beim Start mit
 * {@code launchTicks} auf bzw. sinkt bei der Landung ab.
 * Original-Klasse: RenderTransporterRocket
 */
public class TransporterRocketRenderer implements HbmBerBounds<TransporterRocketBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/machines/transporter_pad.obj"));
    public static final ResourceLocation TEX = SpaceMod.id("textures/models/machines/transporter_pad.png");

    public TransporterRocketRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TransporterRocketBlockEntity pad, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();

        ps.translate(0.5D, 0.0D, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));

        switch (ObjBerHelper.meta(pad)) {
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(0F));
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90F));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180F));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270F));
            default -> { }
        }

        var vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("base", ps, vc, light);

        ps.pushPose();
        {
            float rot = Mth.clamp(getPipeEngage(pad, interp), 0, 1);

            ps.translate(0.0F, 0.75F, -0.75F);
            ps.mulPose(Axis.XP.rotationDegrees(rot * -30.0F));
            ps.translate(0.0F, -0.75F, 0.75F);

            MODEL.renderPart("pipe", ps, vc, light);
        }
        ps.popPose();

        if (pad.launchTicks < 100) {
            ps.pushPose();
            {
                ps.translate(0.0F, 0.75F + Mth.clamp(pad.launchTicks + (pad.hasRocket ? -interp : interp), 0, 200), 0.0F);

                // GL_CULL_FACE aus
                MinerRocketRenderer.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(MinerRocketRenderer.TEXTURE)), light);
            }
            ps.popPose();
        }

        ps.popPose();
    }

    private float getPipeEngage(TransporterRocketBlockEntity pad, float interp) {
        if (pad.launchTicks >= 0) {
            return !pad.hasRocket ? (pad.launchTicks + interp) * 0.25F : 1.0F;
        } else {
            return 1 - (-pad.launchTicks - 1 + interp) * 0.25F;
        }
    }

    @Override public boolean shouldRenderOffScreen(TransporterRocketBlockEntity be) { return true; }

    /** Original {@code getMaxRenderDistanceSquared = 65536} (256 Bloecke). */
    @Override public int getViewDistance() { return 256; }
}
