package com.space_m.machine.dyson.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.machine.dyson.TileEntityDysonConverterAnatmogenesis;
import com.space_m.machine.dyson.TileEntityDysonConverterHE;
import com.space_m.machine.dyson.TileEntityDysonConverterTU;
import com.space_m.machine.misc.client.ItemRenderMachines;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Die drei Dyson-Wandler-Renderer (Original {@code RenderDysonConverterHE}, {@code RenderDysonConverterTU},
 * {@code RenderDysonConverterAnatmogenesis}), je 1:1 mit Item-Darstellung ({@code getRenderer()}).
 * Original-Klasse: RenderDysonConverterHE
 * Original-Klasse: RenderDysonConverterTU
 * Original-Klasse: RenderDysonConverterAnatmogenesis
 */
public final class RenderDysonConverters {

    private RenderDysonConverters() {}

    /**
     * {@code RenderDysonConverterHE}: Gehaeuse {@code HE_Converter}, Spulen {@code Coil1}/{@code Coil3} und gegenlaeufig
     * {@code Coil2} um Z auf y 1.5 (Weltzeit beim Wandeln). Item: T(0,-1,0) S(3); S(0.55).
     */
    public static class HE implements HbmBerBounds<TileEntityDysonConverterHE> {

        public HE(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(TileEntityDysonConverterHE converter, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            {
                ps.translate(0.5D, 0, 0.5D);
                DysonModels.rotate(ps, ObjBerHelper.meta(converter));

                VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(DysonModels.HE_CONVERTER_TEX));
                DysonModels.HE_CONVERTER.renderPart("HE_Converter", ps, vc, light);

                float t = converter.isConverting && converter.getLevel() != null ? converter.getLevel().getGameTime() + f : 0;

                ps.translate(0.0F, 1.5F, 0.0F);

                ps.pushPose();
                {
                    ps.mulPose(Axis.ZP.rotationDegrees(t));
                    ps.translate(0.0F, -1.5F, 0.0F);
                    DysonModels.HE_CONVERTER.renderPart("Coil1", ps, vc, light);
                    DysonModels.HE_CONVERTER.renderPart("Coil3", ps, vc, light);
                }
                ps.popPose();

                ps.pushPose();
                {
                    ps.mulPose(Axis.ZN.rotationDegrees(t));
                    ps.translate(0.0F, -1.5F, 0.0F);
                    DysonModels.HE_CONVERTER.renderPart("Coil2", ps, vc, light);
                }
                ps.popPose();
            }
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(TileEntityDysonConverterHE be) { return true; }
        @Override public int getViewDistance() { return 256; }

        public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
            @Override
            public void renderInventory(PoseStack ps) {
                ps.translate(0, -1, 0);
                ps.scale(3.0F, 3.0F, 3.0F);
            }

            @Override
            public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
                ps.scale(0.55F, 0.55F, 0.55F);
                DysonModels.HE_CONVERTER.renderAll(ps, buf.getBuffer(RenderType.entityCutout(DysonModels.HE_CONVERTER_TEX)), light);
            }
        };
    }

    /** {@code RenderDysonConverterTU}: ganzes Modell. Item: T(3,1,0) S(3); S(0.55). */
    public static class TU implements HbmBerBounds<TileEntityDysonConverterTU> {

        public TU(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(TileEntityDysonConverterTU converter, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            ps.translate(0.5D, 0, 0.5D);
            DysonModels.rotate(ps, ObjBerHelper.meta(converter));
            DysonModels.TU_CONVERTER.renderAll(ps, buf.getBuffer(RenderType.entityCutout(DysonModels.TU_CONVERTER_TEX)), light);
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(TileEntityDysonConverterTU be) { return true; }
        @Override public int getViewDistance() { return 256; }

        public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
            @Override
            public void renderInventory(PoseStack ps) {
                ps.translate(3, 1, 0);
                ps.scale(3.0F, 3.0F, 3.0F);
            }

            @Override
            public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
                ps.scale(0.55F, 0.55F, 0.55F);
                DysonModels.TU_CONVERTER.renderAll(ps, buf.getBuffer(RenderType.entityCutout(DysonModels.TU_CONVERTER_TEX)), light);
            }
        };
    }

    /**
     * {@code RenderDysonConverterAnatmogenesis}: Gehaeuse {@code Anatmogenesis}, Spulen {@code Coils} beim Wandeln
     * zitternd ({@code sin(t*0.1)*0.025 + rand*0.02}). Item: T(0,-2,0) S(2.5); S(0.55).
     */
    public static class Anatmogenesis implements HbmBerBounds<TileEntityDysonConverterAnatmogenesis> {

        public Anatmogenesis(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(TileEntityDysonConverterAnatmogenesis converter, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            ps.pushPose();
            {
                ps.translate(0.5D, 0, 0.5D);
                DysonModels.rotate(ps, ObjBerHelper.meta(converter));

                VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(DysonModels.ANATMOGENESIS_TEX));
                DysonModels.ANATMOGENESIS.renderPart("Anatmogenesis", ps, vc, light);

                if (converter.isConverting && converter.getLevel() != null) {
                    float t = converter.getLevel().getGameTime() + f;
                    ps.translate(0, Math.sin(t * 0.1) * 0.025 + converter.getLevel().random.nextFloat() * 0.02, 0);
                }

                DysonModels.ANATMOGENESIS.renderPart("Coils", ps, vc, light);
            }
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(TileEntityDysonConverterAnatmogenesis be) { return true; }
        @Override public int getViewDistance() { return 256; }

        public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
            @Override
            public void renderInventory(PoseStack ps) {
                ps.translate(0, -2, 0);
                ps.scale(2.5F, 2.5F, 2.5F);
            }

            @Override
            public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
                ps.scale(0.55F, 0.55F, 0.55F);
                DysonModels.ANATMOGENESIS.renderAll(ps, buf.getBuffer(RenderType.entityCutout(DysonModels.ANATMOGENESIS_TEX)), light);
            }
        };
    }
}
