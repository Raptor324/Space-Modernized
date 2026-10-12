package com.space_m.machine.chem.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.machine.chem.TileEntityMachineVacuumCircuit;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderVacuumCircuit}: Blockmitte, Ausrichtung (2: 90, 4: 180, 3: 270, 5: 0), ganzes Modell;
 * das Rezeptergebnis ({@code display}) liegt bei (0.625 | 1.25 | 0.5), Y 90, X -90, S(1.5) als Rahmen-Gegenstand.
 * Item: Inventar T(0,-1,0) S(3.5).
 * Original-Klasse: RenderVacuumCircuit
 */
public class RenderVacuumCircuit implements HbmBerBounds<TileEntityMachineVacuumCircuit> {

    public RenderVacuumCircuit(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineVacuumCircuit solderer, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ChemModels.rotate90(ps, ObjBerHelper.meta(solderer));

        ChemModels.VAC_CIR_STATION.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChemModels.VAC_CIR_STATION_TEX)), light);

        if (!solderer.display.isEmpty()) {
            ps.pushPose();
            ps.translate(0.625D, 1.25D, 0.5D);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            ps.scale(1.5F, 1.5F, 1.5F);
            ObjBerHelper.renderDecoItemInFrame(ps, buf, solderer.display, light, solderer.getLevel());
            ps.popPose();
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineVacuumCircuit be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
