package com.space_m.machine.chem.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.machine.chem.TileEntityHydroponic;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderHydroponic}: Blockmitte, Ausrichtung (2: 90, 4: 180, 3: 270, 5: 0), {@code Base}; ab
 * 200 HE die {@code Lights} voll hell, additiv ({@code GL_SRC_ALPHA, GL_ONE}), ohne Tiefenschreiben und ohne
 * Rueckseitenausblendung (= {@code RenderType.eyes}). Item: Inventar T(0,-1.75,0) S(3), ganzes Modell.
 * Original-Klasse: RenderHydroponic
 */
public class RenderHydroponic implements HbmBerBounds<TileEntityHydroponic> {

    public RenderHydroponic(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityHydroponic hydro, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ChemModels.rotate90(ps, ObjBerHelper.meta(hydro));

        ChemModels.HYDROPONIC.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutout(ChemModels.HYDROPONIC_TEX)), light);

        if (hydro.getEnergyStored() >= 200) {
            ChemModels.HYDROPONIC.renderPart("Lights", ps, buf.getBuffer(RenderType.eyes(ChemModels.HYDROPONIC_TEX)), 0xF000F0);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityHydroponic be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
