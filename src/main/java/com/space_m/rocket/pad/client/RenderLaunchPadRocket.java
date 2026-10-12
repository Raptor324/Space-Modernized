package com.space_m.rocket.pad.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.rocket.client.RocketPronter;
import com.space_m.rocket.pad.TileEntityLaunchPadRocket;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderLaunchPadRocket}: Sockel ({@code Base}), ab Turmhoehe 8 und freiem Himmel der
 * Versorgungsturm ({@code Tower_Base}, 3er-Segmente, 1er-Segmente fuer den Rest, {@code Tower_Cap}) und die eingelegte
 * Rakete 3 Bloecke ueber dem Kern ({@code MissilePronter.prontRocket} -> {@link RocketPronter}).
 */
public class RenderLaunchPadRocket implements BlockEntityRenderer<TileEntityLaunchPadRocket> {

    public static final SimpleObjModel rocket_pad = new SimpleObjModel(SpaceMod.id("models/machines/rocket_pad.obj"));
    public static final ResourceLocation rocket_pad_tex = SpaceMod.id("textures/models/machines/rocket_pad.png");
    public static final ResourceLocation rocket_pad_support_tex = SpaceMod.id("textures/models/machines/rocket_pad_support.png");

    public RenderLaunchPadRocket(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityLaunchPadRocket pad, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, 0, 0.5D);

            switch (ObjBerHelper.meta(pad)) {
                case 2 -> ps.mulPose(Axis.YP.rotationDegrees(90));
                case 4 -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case 3 -> ps.mulPose(Axis.YP.rotationDegrees(270));
                case 5 -> ps.mulPose(Axis.YP.rotationDegrees(0));
                default -> { }
            }

            rocket_pad.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutout(rocket_pad_tex)), light);

            // GL_CULL_FACE aus
            if (pad.canSeeSky && pad.height >= 8) {
                var vc = buf.getBuffer(RenderType.entityCutoutNoCull(rocket_pad_support_tex));
                ps.pushPose();
                {
                    rocket_pad.renderPart("Tower_Base", ps, vc, light);

                    for (int oy = 8; oy < pad.height - 2; oy += 3) {
                        rocket_pad.renderPart("Tower_Segment", ps, vc, light);
                        ps.translate(0, 3, 0);
                    }

                    ps.translate(0, -3, 0);

                    for (int oy = 0; oy < (pad.height - 2) % 3; oy++) {
                        rocket_pad.renderPart("Tower_Segment_Small", ps, vc, light);
                        ps.translate(0, 1, 0);
                    }

                    ps.translate(0, -1, 0);
                    rocket_pad.renderPart("Tower_Cap", ps, vc, light);
                }
                ps.popPose();
            }

            if (pad.rocket != null) {
                ps.translate(0.0F, 3.0F, 0.0F);
                RocketPronter.prontRocket(pad.rocket, ps, buf, light);
            }
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityLaunchPadRocket be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536
}
