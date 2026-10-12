package com.space_m.machine.chem.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.machine.chem.TileEntityMachineCryoDistill;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderCryoDistill}: T(0.5, -2, 0.5), 180 Grad, dann Ausrichtung (2: 0, 4: 90, 3: 180, 5: 270).
 * Item: Inventar T(0,-3,0) S(3.5), sonst S(0.5).
 * Original-Klasse: RenderCryoDistill
 */
public class RenderCryoDistill implements HbmBerBounds<TileEntityMachineCryoDistill> {

    public RenderCryoDistill(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineCryoDistill tile, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, -2.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ChemModels.rotate0(ps, ObjBerHelper.meta(tile));
        ChemModels.CRYO_DISTILL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChemModels.CRYODISTILL_TEX)), light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineCryoDistill be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
