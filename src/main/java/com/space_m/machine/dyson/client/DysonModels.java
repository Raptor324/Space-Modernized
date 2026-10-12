package com.space_m.machine.dyson.client;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Modelle/Texturen des Dyson-Systems (Original {@code ResourceManager}: {@code dyson_receiver}, {@code dyson_anatmogenesis},
 * {@code dyson_he_converter}, {@code dyson_tu_converter}, {@code dyson_spinlaunch} + {@code *_tex}) und der
 * Empfaengerstrahl ({@code BeamPronter.prontBeamwithDepth} mit Alpha - HBMs Port kennt kein Alpha).
 */
public final class DysonModels {

    private DysonModels() {}

    public static final SimpleObjModel RECEIVER = new SimpleObjModel(SpaceMod.id("models/machines/dyson_receiver.obj"));
    public static final SimpleObjModel ANATMOGENESIS = new SimpleObjModel(SpaceMod.id("models/machines/dyson_anatmogenesis.obj"));
    public static final SimpleObjModel HE_CONVERTER = new SimpleObjModel(SpaceMod.id("models/machines/dyson_he_converter.obj"));
    public static final SimpleObjModel TU_CONVERTER = new SimpleObjModel(SpaceMod.id("models/machines/dyson_tu_converter.obj"));
    public static final SimpleObjModel SPINLAUNCH = new SimpleObjModel(SpaceMod.id("models/machines/dyson_spinlaunch.obj"));

    public static final ResourceLocation RECEIVER_TEX = SpaceMod.id("textures/block/machine/dyson_receiver.png");
    public static final ResourceLocation ANATMOGENESIS_TEX = SpaceMod.id("textures/block/machine/dyson_anatmogenesis.png");
    public static final ResourceLocation HE_CONVERTER_TEX = SpaceMod.id("textures/block/machine/dyson_he_converter.png");
    public static final ResourceLocation TU_CONVERTER_TEX = SpaceMod.id("textures/block/machine/dyson_tu_converter.png");
    public static final ResourceLocation SPINLAUNCH_TEX = SpaceMod.id("textures/block/machine/dyson_spinlaunch.png");

    /** Dyson-Maschinen: {@code case 2: 0; case 4: 90; case 3: 180; case 5: 270}. */
    public static void rotate(PoseStack ps, int meta) {
        switch (meta) {
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
    }

    /**
     * 1:1 {@code BeamPronter.prontBeamwithDepth(skeleton, wave, SOLID, outer, inner, start, segments, size, layers,
     * thickness, alpha)}: gemischt (SRC_ALPHA, ONE), ohne Culling/Licht, mit Tiefe.
     */
    public static void prontBeamSolid(PoseStack ps, MultiBufferSource buffers, Vec3 skeleton, boolean spiral,
                                      int outerColor, int innerColor, int start, int segments, float size, int layers,
                                      float thickness, float alpha) {
        ps.pushPose();

        float sYaw = (float) (Math.atan2(skeleton.x, skeleton.z) * 180F / Math.PI);
        float sqrt = Mth.sqrt((float) (skeleton.x * skeleton.x + skeleton.z * skeleton.z));
        float sPitch = (float) (Math.atan2(skeleton.y, sqrt) * 180F / Math.PI);

        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(sYaw));
        ps.mulPose(Axis.XP.rotationDegrees(sPitch - 90));

        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.BEAM_SOLID_DEPTH);
        int a = Mth.clamp((int) (alpha * 255F), 0, 255);

        java.util.Random rand = new java.util.Random(start);
        double length = skeleton.length();
        double segLength = length / segments;
        double lastX = 0, lastY = 0, lastZ = 0;

        for (int i = 0; i <= segments; i++) {
            double[] spinner = { size, 0, 0 };

            if (spiral) {
                spinner = rotY(spinner, (float) Math.PI * (float) start / 180F);
                spinner = rotY(spinner, (float) Math.PI * 45F / 180F * i);
            } else {
                spinner = rotY(spinner, (float) Math.PI * 2 * rand.nextFloat());
                spinner = rotY(spinner, (float) Math.PI * 2 * rand.nextFloat());
            }

            double pX = spinner[0];
            double pY = segLength * i + spinner[1];
            double pZ = spinner[2];

            if (i > 0) {
                float radius = thickness / layers;

                for (int j = 1; j <= layers; j++) {
                    float inter = (float) (j - 1) / (float) (layers - 1);

                    int r1 = (outerColor & 0xFF0000) >> 16, g1 = (outerColor & 0x00FF00) >> 8, b1 = outerColor & 0x0000FF;
                    int r2 = (innerColor & 0xFF0000) >> 16, g2 = (innerColor & 0x00FF00) >> 8, b2 = innerColor & 0x0000FF;
                    int r = (int) (r1 + (r2 - r1) * inter), g = (int) (g1 + (g2 - g1) * inter), b = (int) (b1 + (b2 - b1) * inter);

                    double rj = radius * j;
                    quad(vc, m, r, g, b, a, lastX + rj, lastY, lastZ + rj, lastX + rj, lastY, lastZ - rj, pX + rj, pY, pZ - rj, pX + rj, pY, pZ + rj);
                    quad(vc, m, r, g, b, a, lastX - rj, lastY, lastZ + rj, lastX - rj, lastY, lastZ - rj, pX - rj, pY, pZ - rj, pX - rj, pY, pZ + rj);
                    quad(vc, m, r, g, b, a, lastX + rj, lastY, lastZ + rj, lastX - rj, lastY, lastZ + rj, pX - rj, pY, pZ + rj, pX + rj, pY, pZ + rj);
                    quad(vc, m, r, g, b, a, lastX + rj, lastY, lastZ - rj, lastX - rj, lastY, lastZ - rj, pX - rj, pY, pZ - rj, pX + rj, pY, pZ - rj);
                }
            }

            lastX = pX;
            lastY = pY;
            lastZ = pZ;
        }

        ps.popPose();
    }

    /** 1.7 {@code Vec3.rotateAroundY}. */
    private static double[] rotY(double[] v, float ang) {
        float c = Mth.cos(ang), s = Mth.sin(ang);
        return new double[] { v[0] * c + v[2] * s, v[1], v[2] * c - v[0] * s };
    }

    private static void quad(VertexConsumer vc, Matrix4f m, int r, int g, int b, int a,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4) {
        //? if < 1.21.1 {
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, a).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, a).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(r, g, b, a).endVertex();
        vc.vertex(m, (float) x4, (float) y4, (float) z4).color(r, g, b, a).endVertex();
        //?} else {
        /*vc.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        vc.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        vc.addVertex(m, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
        vc.addVertex(m, (float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
        *///?}
    }
}
