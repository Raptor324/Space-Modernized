package com.space_m.client.sky;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Kleiner Ersatz fuer den Zustandsautomaten der festen GL-Pipeline (1.7.10), damit die Himmelsklassen von NTM Space
 * Zeile fuer Zeile portiert werden koennen: {@code glColor}, {@code GL_TEXTURE_2D}, Matrixstapel ({@code glPushMatrix},
 * {@code glRotatef}, ...), gebundene Textur und Blendfunktion.
 *
 * <p>Der Matrixstapel ist ein {@link PoseStack}; Vertices werden fuer die Vanilla-Shader auf der CPU transformiert
 * ({@link SkyTess}), fuer die eigenen Rohshader ({@link SkyShader}) als {@code ModelViewMat}-Uniform uebergeben.
 * Nur Render-Thread, nur Client.
 */
public final class SkyGL {
    private SkyGL() {}

    /** Matrixstapel der aktuellen Himmels-/BER-Zeichnung. */
    public static PoseStack ps = new PoseStack();

    /** {@code glColor4f}: Farbe fuer Vertices ohne eigene Farbe. */
    public static final float[] color = {1F, 1F, 1F, 1F};
    /** {@code glEnable/glDisable(GL_TEXTURE_2D)}. */
    public static boolean texture2D = true;
    /** Zuletzt gebundene Textur ({@code bindTexture}). */
    public static ResourceLocation boundTexture;

    /** Aktiver Rohshader ({@link SkyShader#use()}), sonst null = Vanilla-Shader. */
    static SkyShader activeShader;

    // ── Lebenszyklus ──

    /** Beginnt eine Zeichnung mit gegebenem Ausgangsmatrix (Himmel: Kamera-Rotation, BER: Posen-Stapel). */
    public static void begin(Matrix4f base) {
        ps = new PoseStack();
        ps.last().pose().set(base);
        color[0] = color[1] = color[2] = color[3] = 1F;
        texture2D = true;
        boundTexture = null;
        activeShader = null;
    }

    // ── Matrix ──

    public static void push() { ps.pushPose(); }

    public static void pop() { ps.popPose(); }

    public static void rotate(double deg, double x, double y, double z) {
        if (x == 1 && y == 0 && z == 0) { ps.mulPose(Axis.XP.rotationDegrees((float) deg)); return; }
        if (x == 0 && y == 1 && z == 0) { ps.mulPose(Axis.YP.rotationDegrees((float) deg)); return; }
        if (x == 0 && y == 0 && z == 1) { ps.mulPose(Axis.ZP.rotationDegrees((float) deg)); return; }
        // glRotate mit beliebiger Achse (wird von OpenGL normiert)
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len < 1.0E-8) return;
        ps.mulPose(new org.joml.Quaternionf().rotationAxis((float) Math.toRadians(deg),
                (float) (x / len), (float) (y / len), (float) (z / len)));
    }

    public static void translate(double x, double y, double z) { ps.translate(x, y, z); }

    public static void scale(double x, double y, double z) { ps.scale((float) x, (float) y, (float) z); }

    public static Matrix4f pose() { return ps.last().pose(); }

    // ── Farbe / Textur ──

    public static void color(float r, float g, float b) { color(r, g, b, 1F); }

    public static void color(double r, double g, double b, double a) { color((float) r, (float) g, (float) b, (float) a); }

    public static void color(float r, float g, float b, float a) {
        color[0] = r; color[1] = g; color[2] = b; color[3] = a;
    }

    public static void enableTexture() { texture2D = true; }

    public static void disableTexture() { texture2D = false; }

    /** {@code mc.renderEngine.bindTexture(rl)} auf Einheit 0 (Vanilla-Shader und Rohshader). */
    public static void bind(ResourceLocation rl) {
        boundTexture = rl;
        RenderSystem.setShaderTexture(0, rl);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(Minecraft.getInstance().getTextureManager().getTexture(rl).getId());
    }

    /** {@code GL13.glActiveTexture(GL_TEXTURE1); bindTexture(rl); glActiveTexture(GL_TEXTURE0)} (fuer Rohshader). */
    public static void bindUnit(int unit, ResourceLocation rl) {
        int id = Minecraft.getInstance().getTextureManager().getTexture(rl).getId();
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
        RenderSystem.bindTexture(id);
        RenderSystem.setShaderTexture(unit, rl);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
    }

    // ── Blend / Tiefe / Cull ──

    public static void enableBlend() { RenderSystem.enableBlend(); }

    public static void disableBlend() { RenderSystem.disableBlend(); }

    /** {@code GL11.glBlendFunc(src, dst)}. */
    public static void blendFunc(int src, int dst) { RenderSystem.blendFunc(src, dst); }

    /** {@code OpenGlHelper.glBlendFunc(src, dst, srcA, dstA)}. */
    public static void blendFunc(int src, int dst, int srcA, int dstA) { RenderSystem.blendFuncSeparate(src, dst, srcA, dstA); }

    public static void depthMask(boolean b) { RenderSystem.depthMask(b); }

    public static void enableCull() { RenderSystem.enableCull(); }

    public static void disableCull() { RenderSystem.disableCull(); }

    public static void enableDepthTest() { RenderSystem.enableDepthTest(); }

    public static void disableDepthTest() { RenderSystem.disableDepthTest(); }

    /** {@code GL11.glClear(GL_DEPTH_BUFFER_BIT)}. */
    public static void clearDepth() {
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    /** {@code GL11.glColorMask}. */
    public static void colorMask(boolean r, boolean g, boolean b, boolean a) { RenderSystem.colorMask(r, g, b, a); }

    /** Standard nach dem Himmel: Blend aus, Standard-Blendfunktion, Farbe weiss, Tiefe schreibbar. */
    public static void reset() {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    // ── GL-Konstanten der Originalaufrufe ──
    public static final int GL_SRC_ALPHA = GL11.GL_SRC_ALPHA;
    public static final int GL_ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;
    public static final int GL_ONE = GL11.GL_ONE;
    public static final int GL_ZERO = GL11.GL_ZERO;
    /** {@code GL11.GL_ONE_MINUS_CONSTANT_ALPHA} (0x8004). */
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
}
