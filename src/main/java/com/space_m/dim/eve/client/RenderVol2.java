package com.space_m.dim.eve.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.client.sky.SkyBeamPronter;
import com.space_m.client.sky.SkyBeamPronter.EnumBeamType;
import com.space_m.client.sky.SkyBeamPronter.EnumWaveType;
import com.space_m.client.sky.SkyGL;
import com.space_m.dim.eve.BlockVolcanoV2.TileEntityLightningVolcano;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.render.tileentity.RenderVol2}: Entladungsstrahl des Entladungsgeysirs ({@code geysir_electric}),
 * drei Strahlen (Spirale + 2x zufaellig, NTM-Space-{@code BeamPronter} mit Alpha) entlang der Achse, solange
 * {@code chargetime == 0}; Laenge {@code 100 + flashd}, Alpha {@code 1 - min(1, flashd / 110)}.
 * Der Block hat immer Metadatum 0 (Fall 0: um +X gekippt -> Strahl nach oben). Sichtweite 256.
 */
public class RenderVol2 implements BlockEntityRenderer<TileEntityLightningVolcano> {

    public RenderVol2(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TileEntityLightningVolcano emitter, float f, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (emitter.getLevel() == null) return;
        poseStack.pushPose();
        try {
            SkyGL.begin(poseStack.last().pose());

            SkyGL.push();
            SkyGL.translate(0.5, 0, 0.5);
            SkyGL.enableCull();

            SkyGL.rotate(90, 0F, 1F, 0F);

            // getBlockMetadata() == 0
            SkyGL.translate(0.0D, 0.5D, -0.5D);
            SkyGL.rotate(90, 1F, 0F, 0F);

            SkyGL.translate(0, 0.5, 0.5);
            float scale = (emitter.flashd) * 1F;
            float alphad = 1.0F - Math.min(1.0F, scale / 110);
            SkyGL.enableBlend();
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);

            SkyGL.color(1, 1, 1, alphad);
            if (emitter.chargetime == 0) {
                long time = emitter.getLevel().getGameTime();
                SkyBeamPronter.prontBeam(new Vec3(0, 0, -100 - scale), EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0x101020, 0x101020, 0, 8, 0F, 6, (float) 3 * alphad, alphad * 2F);
                SkyBeamPronter.prontBeam(new Vec3(0, 0, -100 - scale), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 2) % 1000, 26, (float) 3, 2, 0.0625F * (float) 2, 0.5F * alphad);
                SkyBeamPronter.prontBeam(new Vec3(0, 0, -100 - scale), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 4) % 1000, 26, (float) 3, 2, 0.0625F * (float) 2, 0.5F * alphad);
            }

            SkyGL.disableBlend();
            SkyGL.pop();
        } finally {
            SkyGL.reset();
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityLightningVolcano tile) {
        return true;
    }

    /** {@code getMaxRenderDistanceSquared = 65536}. */
    @Override
    public int getViewDistance() {
        return 256;
    }

    //? if neoforge {
    /*@Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(TileEntityLightningVolcano tile) {
        return tile.getRenderBoundingBox();
    }
    *///?}
}
