package com.space_m.client.sky;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;

/**
 * Die beiden Himmelsflaechen von 1.7.10 {@code RenderGlobal} ({@code glSkyList} in Hoehe 16, {@code glSkyList2} in
 * Hoehe -16; Raster aus 64er-Quadraten), die {@code SkyProviderCelestial} per {@code glCallList} zeichnet. Hier als
 * {@link VertexBuffer} mit Vanilla-{@code position}-Shader (wendet Nebel an wie {@code GL_FOG}).
 */
public final class SkyDomes {
    private SkyDomes() {}

    private static VertexBuffer sky;
    private static VertexBuffer sky2;

    private static VertexBuffer build(boolean upper) {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        byte b2 = 64;
        int i = 256 / b2 + 2;
        float f = upper ? 16.0F : -16.0F;
        //? if < 1.21.1 {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        //?} else {
        /*BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        *///?}
        for (int j = -b2 * i; j <= b2 * i; j += b2) {
            for (int k = -b2 * i; k <= b2 * i; k += b2) {
                if (upper) {
                    put(bb, j, f, k);
                    put(bb, j + b2, f, k);
                    put(bb, j + b2, f, k + b2);
                    put(bb, j, f, k + b2);
                } else {
                    put(bb, j + b2, f, k);
                    put(bb, j, f, k);
                    put(bb, j, f, k + b2);
                    put(bb, j + b2, f, k + b2);
                }
            }
        }
        vb.bind();
        //? if < 1.21.1 {
        vb.upload(bb.end());
        //?} else {
        /*vb.upload(bb.buildOrThrow());
        *///?}
        VertexBuffer.unbind();
        return vb;
    }

    private static void put(BufferBuilder bb, float x, float y, float z) {
        //? if < 1.21.1 {
        bb.vertex(x, y, z).endVertex();
        //?} else {
        /*bb.addVertex(x, y, z);
        *///?}
    }

    /** {@code glCallList(glSkyList)} mit Farbe {@link SkyGL#color} und aktuellem Nebel. */
    public static void drawSky() {
        if (sky == null) sky = build(true);
        draw(sky);
    }

    /** {@code glCallList(glSkyList2)}. */
    public static void drawSky2() {
        if (sky2 == null) sky2 = build(false);
        draw(sky2);
    }

    private static void draw(VertexBuffer vb) {
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(SkyGL.color[0], SkyGL.color[1], SkyGL.color[2], SkyGL.color[3]);
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(SkyGL.pose());
        vb.bind();
        vb.drawWithShader(mv, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
}
