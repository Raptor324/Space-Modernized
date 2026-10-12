package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.hbm_m.util.BobMathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.orbit.propulsion.TileEntityMachineHTR3;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderHTR3}: Kern-Mitte, 3 tiefer, Ausrichtung (3: 270, 5: 0, 2: 90, 4: 180); Zeit
 * {@code t = lastTime + (time - lastTime) * interp} treibt Schwanken ({@code sway}), Kardan der Duese ({@code h/v}),
 * Kolben ({@code sps}), Abdeckung und Aufhaengung. Textur {@code lpw2}; {@code ExhaustVacuum} wird nie gezeichnet.
 * Original-Klasse: RenderHTR3
 */
public class RenderHTR3 implements HbmBerBounds<TileEntityMachineHTR3> {

    public RenderHTR3(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineHTR3 rocket, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, -3.0D, 0.5D);

            PropulsionModels.rotateThruster(ps, ObjBerHelper.meta(rocket));

            double t = rocket.lastTime + (rocket.time - rocket.lastTime) * interp;

            double swayTimer = (t / 3D) % (Math.PI * 4);
            double sway = (Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5;

            double bellTimer = (t / 5D) % (Math.PI * 4);
            double h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
            double v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;

            double pistonTimer = (t / 5D) % (Math.PI * 2);
            double piston = BobMathUtil.sps(pistonTimer);
            double rotorTimer = (t / 5D) % (Math.PI * 16);
            double rotor = (BobMathUtil.sps(rotorTimer) + rotorTimer / 2D - 1) / 25.1327412287D;
            double turbine = (t % 100) / 100D;

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(PropulsionModels.LPW2_TEX));
            part("Center", ps, vc, light);

            part("PipeL1", ps, vc, light);
            part("PipeL2", ps, vc, light);
            part("PipeR1", ps, vc, light);
            part("PipeR2", ps, vc, light);

            renderMainAssembly(ps, vc, light, sway, h, v, piston, rotor, turbine);

            double coverTimer = (t / 5D) % (Math.PI * 4);
            double cover = (Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5;

            ps.pushPose();
            ps.translate(0, 0, -cover * 0.125);
            part("CoverTop", ps, vc, light);
            part("CoverBottom", ps, vc, light);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, 0, 3.5);
            ps.scale(1, 1, (float) ((3 + cover * 0.125) / 3));
            ps.translate(0, 0, -3.5);
            part("SuspensionBottom", ps, vc, light);
            part("SuspensionTop", ps, vc, light);
            ps.popPose();
        }
        ps.popPose();
    }

    private static void part(String name, PoseStack ps, VertexConsumer vc, int light) {
        PropulsionModels.HTR3.renderPart(name, ps, vc, light);
    }

    public static void renderMainAssembly(PoseStack ps, VertexConsumer vc, int light, double sway, double h, double v, double piston, double rotor, double turbine) {
        ps.pushPose();
        ps.translate(0, 0, -sway * 0.125);

        ps.pushPose();
        ps.translate(0, 0, piston * 0.375D + 0.375D);
        part("Piston", ps, vc, light);
        ps.popPose();

        renderBell(ps, vc, light, h, v);
        ps.popPose();

        renderShroud(ps, vc, light, h, v);
    }

    public static void renderBell(PoseStack ps, VertexConsumer vc, int light, double h, double v) {
        ps.pushPose();
        ps.translate(0, 3.5, -1);
        double magnitude = 2D;
        ps.mulPose(Axis.YP.rotationDegrees((float) (v * magnitude)));
        ps.mulPose(Axis.XP.rotationDegrees((float) (h * magnitude)));
        ps.translate(0, -3.5, 1);
        part("Engine", ps, vc, light);
        ps.popPose();
    }

    public static void renderShroud(PoseStack ps, VertexConsumer vc, int light, double h, double v) {
        double magnitude = 0.125D;

        ps.pushPose();
        ps.translate(0, -h * magnitude, 0);
        part("ShroudH", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(v * magnitude, 0, 0);
        part("ShroudV", ps, vc, light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineHTR3 be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
