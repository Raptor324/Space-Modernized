package com.space_m.client.sky;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;

/**
 * Ersatz fuer {@code net.minecraft.client.renderer.Tessellator} (1.7.10) im Himmel: sammelt Vertices wie
 * {@code startDrawingQuads/addVertexWithUV/setColorRGBA_F/draw} und zeichnet sie mit
 * <ul>
 *   <li>Vanilla {@code position_tex_color} (Textur an) bzw. {@code position_color} (Textur aus), Vertices auf der CPU mit
 *       {@link SkyGL#pose()} transformiert, oder</li>
 *   <li>dem aktiven {@link SkyShader} (Modellkoordinaten, Matrix als Uniform).</li>
 * </ul>
 * Vertices ohne eigene Farbe bekommen {@link SkyGL#color} (wie {@code glColor} bei Tessellator ohne Farbe).
 * {@code GL_POINTS} gibt es in {@link VertexFormat.Mode} nicht - Punkte zeichnet {@link #drawPoints} roh.
 */
public final class SkyTess {
    private SkyTess() {}

    /** Original {@code startDrawing(GL_POINTS)}: eigener Pfad. */
    public static final int POINTS = 0;
    public static final int QUADS = GL11.GL_QUADS;
    public static final int TRIANGLE_FAN = GL11.GL_TRIANGLE_FAN;
    public static final int TRIANGLES = GL11.GL_TRIANGLES;
    /** Original {@code startDrawing(3)} = GL_LINE_STRIP. */
    public static final int LINE_STRIP = GL11.GL_LINE_STRIP;

    private static int mode = QUADS;
    private static float[] data = new float[9 * 1024];
    private static int count;
    private static boolean hasColor;
    private static float cr = 1, cg = 1, cb = 1, ca = 1;
    private static double tx, ty, tz;

    public static void startDrawingQuads() { startDrawing(QUADS); }

    public static void startDrawing(int glMode) {
        mode = glMode;
        count = 0;
        hasColor = false;
        tx = ty = tz = 0;
    }

    public static void setColorRGBA_F(float r, float g, float b, float a) {
        hasColor = true;
        cr = r; cg = g; cb = b; ca = a;
    }

    /** {@code setColorRGBA_I(rgb, alpha)}. */
    public static void setColorRGBA_I(int rgb, int a) {
        setColorRGBA_F((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, a / 255F);
    }

    /** {@code setTranslation}. */
    public static void setTranslation(double x, double y, double z) { tx = x; ty = y; tz = z; }

    public static void addVertex(double x, double y, double z) { addVertexWithUV(x, y, z, 0, 0); }

    public static void addVertexWithUV(double x, double y, double z, double u, double v) {
        int need = (count + 1) * 9;
        if (need > data.length) data = java.util.Arrays.copyOf(data, data.length * 2);
        int o = count * 9;
        data[o] = (float) (x + tx); data[o + 1] = (float) (y + ty); data[o + 2] = (float) (z + tz);
        data[o + 3] = (float) u; data[o + 4] = (float) v;
        if (hasColor) {
            data[o + 5] = cr; data[o + 6] = cg; data[o + 7] = cb; data[o + 8] = ca;
        } else {
            data[o + 5] = SkyGL.color[0]; data[o + 6] = SkyGL.color[1]; data[o + 7] = SkyGL.color[2]; data[o + 8] = SkyGL.color[3];
        }
        count++;
    }

    /** {@code tessellator.draw()}. */
    public static void draw() {
        if (count == 0) return;
        if (mode == POINTS) { drawPoints(); return; }

        VertexFormat.Mode vm = switch (mode) {
            case GL11.GL_TRIANGLE_FAN -> VertexFormat.Mode.TRIANGLE_FAN;
            case GL11.GL_TRIANGLES -> VertexFormat.Mode.TRIANGLES;
            case GL11.GL_LINE_STRIP -> VertexFormat.Mode.DEBUG_LINE_STRIP;
            default -> VertexFormat.Mode.QUADS;
        };
        SkyShader raw = SkyGL.activeShader;
        Matrix4f m = raw != null ? new Matrix4f() : SkyGL.pose();
        if (raw != null) raw.applyMatrices();
        else RenderSystem.setShader(SkyGL.texture2D ? GameRenderer::getPositionTexColorShader : GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        // Vanilla-Shader binden Attribute nach ihrem eigenen Format (position_color: Position, Color)
        boolean withUv = raw != null || SkyGL.texture2D;
        VertexFormat fmt = withUv ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR;
        Vector4f p = new Vector4f();
        //? if < 1.21.1 {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(vm, fmt);
        for (int i = 0; i < count; i++) {
            int o = i * 9;
            m.transform(p.set(data[o], data[o + 1], data[o + 2], 1F));
            if (withUv) bb.vertex(p.x, p.y, p.z).uv(data[o + 3], data[o + 4]).color(data[o + 5], data[o + 6], data[o + 7], data[o + 8]).endVertex();
            else bb.vertex(p.x, p.y, p.z).color(data[o + 5], data[o + 6], data[o + 7], data[o + 8]).endVertex();
        }
        BufferBuilder.RenderedBuffer rb = bb.end();
        if (raw != null) BufferUploader.draw(rb);
        else BufferUploader.drawWithShader(rb);
        //?} else {
        /*BufferBuilder bb = Tesselator.getInstance().begin(vm, fmt);
        for (int i = 0; i < count; i++) {
            int o = i * 9;
            m.transform(p.set(data[o], data[o + 1], data[o + 2], 1F));
            if (withUv) bb.addVertex(p.x, p.y, p.z).setUv(data[o + 3], data[o + 4]).setColor(data[o + 5], data[o + 6], data[o + 7], data[o + 8]);
            else bb.addVertex(p.x, p.y, p.z).setColor(data[o + 5], data[o + 6], data[o + 7], data[o + 8]);
        }
        com.mojang.blaze3d.vertex.MeshData md = bb.build();
        if (md != null) {
            if (raw != null) BufferUploader.draw(md);
            else BufferUploader.drawWithShader(md);
        }
        *///?}
        count = 0;
    }

    // ── GL_POINTS (Rohpfad: eigener VAO/VBO, glDrawArrays) ──

    private static int pointVao = -1, pointVbo = -1;

    private static void drawPoints() {
        SkyShader shader = SkyGL.activeShader;
        boolean own = shader == null;
        if (own) {
            shader = SkyShaders.POINT_TEX;
            if (!shader.use()) { count = 0; return; }
        }
        shader.applyMatrices();

        if (pointVao < 0) {
            pointVao = GL30.glGenVertexArrays();
            pointVbo = GL15.glGenBuffers();
        }
        java.nio.FloatBuffer fb = MemoryUtil.memAllocFloat(count * 9);
        try {
            fb.put(data, 0, count * 9).flip();
            GL30.glBindVertexArray(pointVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pointVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STREAM_DRAW);
            int stride = 9 * 4;
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 3L * 4);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, stride, 5L * 4);
            GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
        } finally {
            MemoryUtil.memFree(fb);
        }
        // Vanilla merkt sich den zuletzt gebundenen Sofort-Puffer
        BufferUploader.invalidate();
        VertexBuffer.unbind();
        if (own) shader.stop();
        count = 0;
    }
}
