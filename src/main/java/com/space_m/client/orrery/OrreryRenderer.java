package com.space_m.client.orrery;

import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.client.sky.SkyGL;
import com.space_m.orrery.OrreryBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code com.hbm.render.tileentity.RenderOrrery}: Planetarium um die Blockmitte, Sichtweite 256
 * ({@code getMaxRenderDistanceSquared = 65536}). Das Inventar-Bild (kleiner Sonnenwuerfel, UV 0.375..0.625) ist ein
 * Item-Modell ({@code models/item/orrery.json}).
 * Original-Klasse: RenderOrrery
 */
public class OrreryRenderer implements BlockEntityRenderer<OrreryBlockEntity> {

    public OrreryRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(OrreryBlockEntity tile, float interp, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        poseStack.pushPose();
        try {
            SkyGL.begin(poseStack.last().pose());
            SkyGL.enableCull();

            SkyGL.translate(0.5, 0.5, 0.5);
            OrreryPronter.render(Minecraft.getInstance(), tile.getLevel(), interp);
        } finally {
            SkyGL.reset();
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(OrreryBlockEntity tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    //? if neoforge {
    /*@Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(OrreryBlockEntity tile) {
        return tile.getRenderBoundingBox();
    }
    *///?}
}
