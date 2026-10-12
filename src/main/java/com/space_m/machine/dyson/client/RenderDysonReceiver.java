package com.space_m.machine.dyson.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.machine.dyson.TileEntityDysonReceiver;
import com.space_m.machine.misc.client.ItemRenderMachines;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code RenderDysonReceiver}: Gehaeuse {@code DysonReceiver}, Spulen {@code Coil1}/{@code Coil3} und
 * gegenlaeufig {@code Coil2} um die Z-Achse auf y 1.5 (Winkel = Weltzeit beim Empfang, sonst 0); beim Empfang zwei
 * Strahlen 0xff8800 (Spirale, und Zufall mit {@code length/2} Segmenten, Start = Weltzeit % 1000) mit Alpha 0.5 bis
 * {@code beamLength}, volle Helligkeit. Item: Inventar T(0,-4,0) S(1.5); S(0.55), ganzes Modell.
 * Original-Klasse: RenderDysonReceiver
 */
public class RenderDysonReceiver implements HbmBerBounds<TileEntityDysonReceiver> {

    public RenderDysonReceiver(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityDysonReceiver receiver, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, 0, 0.5D);

            DysonModels.rotate(ps, ObjBerHelper.meta(receiver));

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(DysonModels.RECEIVER_TEX));
            DysonModels.RECEIVER.renderPart("DysonReceiver", ps, vc, light);

            long time = receiver.getLevel() != null ? receiver.getLevel().getGameTime() : 0;
            float t = receiver.isReceiving ? time + f : 0;

            ps.translate(0.0F, 1.5F, 0.0F);

            ps.pushPose();
            {
                ps.mulPose(Axis.ZP.rotationDegrees(t));
                ps.translate(0.0F, -1.5F, 0.0F);
                DysonModels.RECEIVER.renderPart("Coil1", ps, vc, light);
                DysonModels.RECEIVER.renderPart("Coil3", ps, vc, light);
            }
            ps.popPose();

            ps.pushPose();
            {
                ps.mulPose(Axis.ZN.rotationDegrees(t));
                ps.translate(0.0F, -1.5F, 0.0F);
                DysonModels.RECEIVER.renderPart("Coil2", ps, vc, light);
            }
            ps.popPose();

            int length = receiver.beamLength;
            int color = 0xff8800;

            if (receiver.isReceiving) {
                DysonModels.prontBeamSolid(ps, buf, new Vec3(0, 0, length), true, color, color, 0, 1, 0F, 2, 0.4F, 0.5F);
                DysonModels.prontBeamSolid(ps, buf, new Vec3(0, 0, length), false, color, color, (int) (time % 1000), (length / 2), 0.0625F, 2, 0.4F, 0.5F);
            }
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityDysonReceiver be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536

    /** Original {@code getRenderer()}. */
    public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
        @Override
        public void renderInventory(PoseStack ps) {
            ps.translate(0, -4, 0);
            ps.scale(1.5F, 1.5F, 1.5F);
        }

        @Override
        public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
            ps.scale(0.55F, 0.55F, 0.55F);
            DysonModels.RECEIVER.renderAll(ps, buf.getBuffer(RenderType.entityCutout(DysonModels.RECEIVER_TEX)), light);
        }
    };
}
