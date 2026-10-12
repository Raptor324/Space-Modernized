package com.space_m.machine.chem.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.machine.chem.TileEntityMachineMilkReformer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderMilkReformer}: Blockmitte, ohne Rueckseitenausblendung, 180 Grad, dann Ausrichtung
 * (2: 90, 4: 180, 3: 270, 5: 0), ganzes Modell {@code milker}. Item: Inventar T(0,-3,0) S(3.5), sonst S(0.5).
 * Original-Klasse: RenderMilkReformer
 */
public class RenderMilkReformer implements HbmBerBounds<TileEntityMachineMilkReformer> {

    public RenderMilkReformer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineMilkReformer tile, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ChemModels.rotate90(ps, ObjBerHelper.meta(tile));
        ChemModels.MILK_REFORMER.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(ChemModels.MILK_REFORMER_TEX)), light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineMilkReformer be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
