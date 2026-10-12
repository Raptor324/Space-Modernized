package com.space_m.machine.chem.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.machine.chem.TileEntityMachineAlkylation;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderAlkylation}: Blockmitte, Ausrichtung (2: 0, 4: 90, 3: 180, 5: 270), ganzes Modell.
 * Item: Inventar T(0,-2,0) S(5), sonst S(0.5).
 * Original-Klasse: RenderAlkylation
 */
public class RenderAlkylation implements HbmBerBounds<TileEntityMachineAlkylation> {

    public RenderAlkylation(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineAlkylation tile, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ChemModels.rotate0(ps, ObjBerHelper.meta(tile));
        ChemModels.ALKYL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChemModels.ALKYL_TEX)), light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineAlkylation be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
