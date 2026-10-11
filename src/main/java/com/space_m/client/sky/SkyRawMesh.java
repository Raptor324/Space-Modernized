package com.space_m.client.sky;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;

/**
 * Roher Dreiecks-Puffer mit Lichtkarten-Koordinate (Position, UV0, Farbe, UV2) fuer {@link SkyShader}-Programme,
 * die die feste Pipeline mit Lichtkarte nachbilden (Wetter: {@code GL_COMBINE} + {@code setBrightness}).
 * Vierecke werden als zwei Dreiecke abgelegt.
 */
final class SkyRawMesh {
    private static final int STRIDE = 11;

    private float[] data = new float[STRIDE * 6 * 256];
    private int count;
    private final float[][] quad = new float[4][STRIDE];
    private int quadFill;

    private int vao = -1, vbo = -1;

    void begin() {
        count = 0;
        quadFill = 0;
    }

    /** Eine Viereck-Ecke (Reihenfolge wie Tessellator QUADS). */
    void vertex(double x, double y, double z, double u, double v, float r, float g, float b, float a, float lu, float lv) {
        float[] q = quad[quadFill++];
        q[0] = (float) x; q[1] = (float) y; q[2] = (float) z; q[3] = (float) u; q[4] = (float) v;
        q[5] = r; q[6] = g; q[7] = b; q[8] = a; q[9] = lu; q[10] = lv;
        if (quadFill == 4) {
            put(quad[0]); put(quad[1]); put(quad[2]);
            put(quad[0]); put(quad[2]); put(quad[3]);
            quadFill = 0;
        }
    }

    private void put(float[] v) {
        if ((count + 1) * STRIDE > data.length) data = Arrays.copyOf(data, data.length * 2);
        System.arraycopy(v, 0, data, count * STRIDE, STRIDE);
        count++;
    }

    boolean isEmpty() {
        return count == 0;
    }

    /** Zeichnet mit dem aktiven Rohshader (Attribute 0..3 = Position, UV0, Color, UV2). */
    void draw(SkyShader shader) {
        if (count == 0) return;
        shader.applyMatrices();
        if (vao < 0) {
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
        }
        FloatBuffer fb = MemoryUtil.memAllocFloat(count * STRIDE);
        try {
            fb.put(data, 0, count * STRIDE).flip();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fb, GL15.GL_STREAM_DRAW);
            int stride = STRIDE * 4;
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 3L * 4);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, stride, 5L * 4);
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 2, GL11.GL_FLOAT, false, stride, 9L * 4);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, count);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
        } finally {
            MemoryUtil.memFree(fb);
        }
        BufferUploader.invalidate();
        VertexBuffer.unbind();
        count = 0;
    }
}
