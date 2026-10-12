package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.orbit.propulsion.TileEntityXenonThruster;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1:1 NTM Space {@code RenderXenonThruster}: Kern-Mitte, 1 tiefer, -90 Grad, Ausrichtung (2: 90, 4: 180, 3: 270, 5: 0),
 * 0,5 zurueck; Gehaeuse {@code Thruster}; Abgasstrahl {@code Exhaust} (Textur {@code xenon_trail}) vom Heck aus um
 * {@code (1,2 - r^2 * 0,2) * thrustAmount} gestreckt, Alpha {@code thrustAmount}, ohne Licht/Tiefenschreiben.
 * Original-Klasse: RenderXenonThruster
 */
public class RenderXenonThruster implements HbmBerBounds<TileEntityXenonThruster> {

    public RenderXenonThruster(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityXenonThruster rocket, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, -1.0D, 0.5D);

            ps.mulPose(Axis.YP.rotationDegrees(-90));

            switch (ObjBerHelper.meta(rocket)) {
                case 2 -> ps.mulPose(Axis.YP.rotationDegrees(90));
                case 4 -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case 3 -> ps.mulPose(Axis.YP.rotationDegrees(270));
                default -> { }
            }

            ps.translate(0, 0, -0.5);

            float trailStretch = rocket.getLevel() != null ? rocket.getLevel().random.nextFloat() : 0F;
            trailStretch = 1.2F - (trailStretch * trailStretch * 0.2F);
            trailStretch *= rocket.thrustAmount;

            PropulsionModels.XENON_THRUSTER.renderPart("Thruster", ps, buf.getBuffer(RenderType.entityCutout(PropulsionModels.XENON_THRUSTER_TEX)), light);

            if (trailStretch > 0) {
                ps.translate(0, 0, -1F);
                ps.scale(1, 1, trailStretch);
                ps.translate(0, 0, 1F);

                PropulsionModels.XENON_THRUSTER.renderPartEntity("Exhaust", ps,
                        buf.getBuffer(RenderType.entityTranslucentEmissive(PropulsionModels.XENON_EXHAUST_TEX)),
                        0xF000F0, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, rocket.thrustAmount);
            }
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityXenonThruster be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536
}
