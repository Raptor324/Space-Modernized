package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.orbit.propulsion.TileEntityMachineHTRF4;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderHTRF4}: Kern-Mitte, 2 tiefer, Ausrichtung wie HTR3, ganzes Modell mit Textur {@code lpw2}.
 * Original-Klasse: RenderHTRF4
 */
public class RenderHTRF4 implements HbmBerBounds<TileEntityMachineHTRF4> {

    public RenderHTRF4(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineHTRF4 tile, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, -2.0D, 0.5D);

        PropulsionModels.rotateThruster(ps, ObjBerHelper.meta(tile));

        PropulsionModels.HTRF4.renderAll(ps, buf.getBuffer(RenderType.entityCutout(PropulsionModels.LPW2_TEX)), light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineHTRF4 be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
