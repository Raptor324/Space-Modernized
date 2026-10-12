package com.space_m.machine.misc.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.misc.TileEntityRadiator;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderRadiator}: Kern-Mitte (auch in der Hoehe), Ausrichtung (2: 90, 4: 180, 3: 270, 5: 0),
 * T(-0.5, 0, 0), Rot Z -90, ganzes Modell. Item: Inventar T(0,-5,0) S(1.2); S(0.75), ganzes Modell.
 * Original-Klasse: RenderRadiator
 */
public class RenderRadiator implements HbmBerBounds<TileEntityRadiator> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/machines/radiator.obj"));
    public static final ResourceLocation TEXTURE = SpaceMod.id("textures/block/machine/radiator.png");

    public RenderRadiator(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityRadiator te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, 0.5D, 0.5D);

            switch (ObjBerHelper.meta(te)) {
                case 2 -> ps.mulPose(Axis.YP.rotationDegrees(90));
                case 4 -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case 3 -> ps.mulPose(Axis.YP.rotationDegrees(270));
                default -> { }
            }

            ps.translate(-0.5F, 0, 0);

            ps.mulPose(Axis.ZP.rotationDegrees(-90));

            MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEXTURE)), light);
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityRadiator be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536

    /** Original {@code getRenderer()}. */
    public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
        @Override
        public void renderInventory(PoseStack ps) {
            ps.translate(0, -5, 0);
            ps.scale(1.2F, 1.2F, 1.2F);
        }

        @Override
        public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
            ps.scale(0.75F, 0.75F, 0.75F);
            MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEXTURE)), light);
        }
    };
}
