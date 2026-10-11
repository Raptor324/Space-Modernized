package com.space_m.client.sky;

import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * NTM-Space-Fassung von {@code com.hbm.render.util.BeamPronter} mit dem neuen Alpha-Parameter (Space hat
 * {@code prontBeam(..., float alpha)} ergaenzt; HBM-Modernized hat die Master-Fassung ohne Alpha). Zeichnet ueber
 * {@link SkyGL}/{@link SkyTess} im Himmel von Thatmo. Wellenformen/Farben/Schichten 1:1.
 */
public final class SkyBeamPronter {
    private SkyBeamPronter() {}

    public static Random rand = new Random();

    public enum EnumWaveType {
        RANDOM, SPIRAL
    }

    public enum EnumBeamType {
        SOLID, LINE
    }

    private static boolean depthMask = false;

    public static void prontBeam(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness) {
        prontBeam(skeleton, wave, beam, outerColor, innerColor, start, segments, size, layers, thickness, 1F);
    }

    public static void prontBeam(Vec3 skeleton, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness, float alpha) {

        SkyGL.push();
        SkyGL.depthMask(depthMask);

        float sYaw = (float) (Math.atan2(skeleton.x, skeleton.z) * 180F / Math.PI);
        float sqrt = Mth.sqrt((float) (skeleton.x * skeleton.x + skeleton.z * skeleton.z));
        float sPitch = (float) (Math.atan2(skeleton.y, (double) sqrt) * 180F / Math.PI);

        SkyGL.rotate(180, 0, 1F, 0);
        SkyGL.rotate(sYaw, 0, 1F, 0);
        SkyGL.rotate(sPitch - 90, 1F, 0, 0);

        SkyGL.push();
        SkyGL.disableTexture();

        if (beam == EnumBeamType.SOLID) {
            SkyGL.disableCull();

            SkyGL.enableBlend();
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);
        }

        double length = skeleton.length();
        rand.setSeed(start);
        double segLength = length / segments;
        double lastX = 0;
        double lastY = 0;
        double lastZ = 0;

        for (int i = 0; i <= segments; i++) {

            double sx = size, sy = 0, sz = 0;

            if (wave == EnumWaveType.SPIRAL) {
                double[] v = rotY(sx, sy, sz, (float) Math.PI * (float) start / 180F);
                v = rotY(v[0], v[1], v[2], (float) Math.PI * 45F / 180F * i);
                sx = v[0]; sy = v[1]; sz = v[2];
            } else if (wave == EnumWaveType.RANDOM) {
                double[] v = rotY(sx, sy, sz, (float) Math.PI * 2 * rand.nextFloat());
                v = rotY(v[0], v[1], v[2], (float) Math.PI * 2 * rand.nextFloat());
                sx = v[0]; sy = v[1]; sz = v[2];
            }

            double pX = 0 * segLength * i + sx;
            double pY = 1 * segLength * i + sy;
            double pZ = 0 * segLength * i + sz;

            if (beam == EnumBeamType.LINE && i > 0) {

                SkyTess.startDrawing(SkyTess.LINE_STRIP);
                setColorOpaque(outerColor);
                SkyTess.addVertex(pX, pY, pZ);
                SkyTess.addVertex(lastX, lastY, lastZ);
                SkyTess.draw();
            }

            if (beam == EnumBeamType.SOLID && i > 0) {

                float radius = thickness / layers;

                for (int j = 1; j <= layers; j++) {

                    float inter = (float) (j - 1) / (float) (layers - 1);

                    int r1 = ((outerColor & 0xFF0000) >> 16);
                    int g1 = ((outerColor & 0x00FF00) >> 8);
                    int b1 = ((outerColor & 0x0000FF) >> 0);

                    int r2 = ((innerColor & 0xFF0000) >> 16);
                    int g2 = ((innerColor & 0x00FF00) >> 8);
                    int b2 = ((innerColor & 0x0000FF) >> 0);

                    int r = ((int) (r1 + (r2 - r1) * inter)) << 16;
                    int g = ((int) (g1 + (g2 - g1) * inter)) << 8;
                    int b = ((int) (b1 + (b2 - b1) * inter)) << 0;

                    int color = r | g | b;

                    SkyTess.startDrawingQuads();
                    setColorWithAlpha(color, alpha);
                    SkyTess.addVertex(lastX + (radius * j), lastY, lastZ + (radius * j));
                    SkyTess.addVertex(lastX + (radius * j), lastY, lastZ - (radius * j));
                    SkyTess.addVertex(pX + (radius * j), pY, pZ - (radius * j));
                    SkyTess.addVertex(pX + (radius * j), pY, pZ + (radius * j));
                    SkyTess.draw();
                    SkyTess.startDrawingQuads();
                    setColorWithAlpha(color, alpha);
                    SkyTess.addVertex(lastX - (radius * j), lastY, lastZ + (radius * j));
                    SkyTess.addVertex(lastX - (radius * j), lastY, lastZ - (radius * j));
                    SkyTess.addVertex(pX - (radius * j), pY, pZ - (radius * j));
                    SkyTess.addVertex(pX - (radius * j), pY, pZ + (radius * j));
                    SkyTess.draw();
                    SkyTess.startDrawingQuads();
                    setColorWithAlpha(color, alpha);
                    SkyTess.addVertex(lastX + (radius * j), lastY, lastZ + (radius * j));
                    SkyTess.addVertex(lastX - (radius * j), lastY, lastZ + (radius * j));
                    SkyTess.addVertex(pX - (radius * j), pY, pZ + (radius * j));
                    SkyTess.addVertex(pX + (radius * j), pY, pZ + (radius * j));
                    SkyTess.draw();
                    SkyTess.startDrawingQuads();
                    setColorWithAlpha(color, alpha);
                    SkyTess.addVertex(lastX + (radius * j), lastY, lastZ - (radius * j));
                    SkyTess.addVertex(lastX - (radius * j), lastY, lastZ - (radius * j));
                    SkyTess.addVertex(pX - (radius * j), pY, pZ - (radius * j));
                    SkyTess.addVertex(pX + (radius * j), pY, pZ - (radius * j));
                    SkyTess.draw();


                }
            }

            lastX = pX;
            lastY = pY;
            lastZ = pZ;
        }

        if (beam == EnumBeamType.LINE) {

            SkyTess.startDrawing(SkyTess.LINE_STRIP);
            setColorOpaque(innerColor);
            SkyTess.addVertex(0, 0, 0);
            SkyTess.addVertex(0, skeleton.length(), 0);
            SkyTess.draw();
        }

        if (beam == EnumBeamType.SOLID) {
            SkyGL.disableBlend();
            SkyGL.enableTexture();

        }

        SkyGL.enableTexture();

        SkyGL.pop();
        SkyGL.depthMask(true);

        SkyGL.pop();
    }

    /** 1.7.10 {@code Vec3.rotateAroundY}. */
    private static double[] rotY(double x, double y, double z, float angle) {
        float c = Mth.cos(angle);
        float s = Mth.sin(angle);
        return new double[] {x * c + z * s, y, z * c - x * s};
    }

    private static void setColorOpaque(int color) {
        SkyTess.setColorRGBA_I(color, 255);
    }

    private static void setColorWithAlpha(int color, float alpha) {
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        SkyGL.color(red, green, blue, alpha);
    }
}
