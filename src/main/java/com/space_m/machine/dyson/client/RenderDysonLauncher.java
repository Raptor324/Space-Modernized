package com.space_m.machine.dyson.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.machine.dyson.TileEntityDysonLauncher;
import com.space_m.machine.misc.client.ItemRenderMachines;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderDysonLauncher}: Kern-Mitte, Ausrichtung (2: 0, 4: 90, 3: 180, 5: 270), {@code Launch};
 * Rotor {@code The_Thing_That_Rotates} um die um 45 Grad geneigte Achse auf y 8.5 gedreht ({@code lastRotation ->
 * rotation}), Nutzlast {@code Payload} im Rotor (wenn geladen und nicht im Auslauf) bzw. im Auslauf entlang der
 * Schleuderbahn ({@code payloadTicks * 10}). Item: Inventar T(0,-2,0) S(3); S(0.25), ganzes Modell.
 * Original-Klasse: RenderDysonLauncher
 */
public class RenderDysonLauncher implements HbmBerBounds<TileEntityDysonLauncher> {

    public RenderDysonLauncher(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityDysonLauncher launcher, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, 0, 0.5D);

            DysonModels.rotate(ps, ObjBerHelper.meta(launcher));

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(DysonModels.SPINLAUNCH_TEX));
            DysonModels.SPINLAUNCH.renderPart("Launch", ps, vc, light);

            float t = launcher.lastRotation + (launcher.rotation - launcher.lastRotation) * f;

            ps.pushPose();
            {
                ps.translate(0, 8.5F, 0);
                ps.mulPose(Axis.XP.rotationDegrees(45.0F));
                ps.mulPose(Axis.YP.rotationDegrees(t));
                ps.mulPose(Axis.XP.rotationDegrees(-45.0F));
                ps.translate(0, -8.5F, 0);

                DysonModels.SPINLAUNCH.renderPart("The_Thing_That_Rotates", ps, vc, light);

                if (launcher.satCount > 0 && !launcher.isSpinningDown) {
                    DysonModels.SPINLAUNCH.renderPart("Payload", ps, vc, light);
                }
            }
            ps.popPose();

            if (launcher.isSpinningDown) {
                float p = launcher.payloadTicks + f;

                ps.translate(1.0F, 8.5F, 0);
                ps.mulPose(Axis.XP.rotationDegrees(45.0F));
                ps.mulPose(Axis.YP.rotationDegrees(90.0F));
                ps.translate(p * 10.0F, 0, 0);
                ps.mulPose(Axis.XP.rotationDegrees(-45.0F));
                ps.translate(0, -8.5F, 0);

                DysonModels.SPINLAUNCH.renderPart("Payload", ps, vc, light);
            }
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityDysonLauncher be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536

    /** Original {@code getRenderer()}. */
    public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
        @Override
        public void renderInventory(PoseStack ps) {
            ps.translate(0, -2, 0);
            ps.scale(3.0F, 3.0F, 3.0F);
        }

        @Override
        public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
            ps.scale(0.25F, 0.25F, 0.25F);
            DysonModels.SPINLAUNCH.renderAll(ps, buf.getBuffer(RenderType.entityCutout(DysonModels.SPINLAUNCH_TEX)), light);
        }
    };
}
