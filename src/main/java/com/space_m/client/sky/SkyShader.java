package com.space_m.client.sky;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.main.SpaceRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * 1:1-Port von {@code com.hbm.render.shader.Shader} (NTM Space): ein eigenes GL-Programm aus Vertex- und
 * Fragment-Shader, Uniforms per Name mit Cache. Unterschiede, technisch noetig (Core-Profil ab 1.17):
 * <ul>
 *   <li>Quellen liegen unter {@code space_m:shaders/sky/*.vsh|fsh} (GLSL 150, mechanisch umgewandelt,
 *       siehe {@code sR1c_shaders.py}); Attribute {@code Position/UV0/Color} auf 0/1/2 gebunden
 *       (= {@code DefaultVertexFormat.POSITION_TEX_COLOR}).</li>
 *   <li>Die feste Matrix {@code gl_ModelViewProjectionMatrix} wird als {@code ProjMat}/{@code ModelViewMat} gesetzt
 *       ({@link #applyMatrices()}).</li>
 *   <li>Wird erst beim ersten {@link #use()} gebaut (GL-Kontext + Ressourcen noetig) statt im statischen Initialisierer.</li>
 *   <li>{@link #stop()} setzt den von Vanilla gemerkten Programmzustand zurueck ({@code ShaderInstance.clear()}).</li>
 * </ul>
 * Original-Klasse: Shader
 */
public final class SkyShader {

    private final ResourceLocation vertex;
    private final ResourceLocation fragment;

    private boolean tried = false;
    private boolean hasLoaded = false;

    private int shaderProgram;
    private int vertexShader;
    private int fragmentShader;

    private final HashMap<String, Integer> uniforms = new HashMap<>();

    public SkyShader(String fragment) {
        this("default", fragment);
    }

    public SkyShader(String vertex, String fragment) {
        this.vertex = SpaceMod.id("shaders/sky/" + vertex + ".vsh");
        this.fragment = SpaceMod.id("shaders/sky/" + fragment + ".fsh");
    }

    /** Original-Konstruktor (lazy). */
    private void load() {
        tried = true;
        try {
            vertexShader = loadShader(vertex, GL20.GL_VERTEX_SHADER);
            fragmentShader = loadShader(fragment, GL20.GL_FRAGMENT_SHADER);

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vertexShader);
            GL20.glAttachShader(shaderProgram, fragmentShader);
            GL20.glBindAttribLocation(shaderProgram, 0, "Position");
            GL20.glBindAttribLocation(shaderProgram, 1, "UV0");
            GL20.glBindAttribLocation(shaderProgram, 2, "Color");
            GL20.glBindAttribLocation(shaderProgram, 3, "UV2");
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("Failed to link shader program: " + GL20.glGetProgramInfoLog(shaderProgram, 1024));
            }

            hasLoaded = true;
        } catch (RuntimeException ex) {
            SpaceRegistry.LOGGER.error("Shaders failed to load, falling back to default pipeline ({})", fragment, ex);
            hasLoaded = false;
        }
    }

    private int loadShader(ResourceLocation location, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, loadResource(location));
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Failed to compile shader " + location + ": " + GL20.glGetShaderInfoLog(shader, 1024));
        }

        return shader;
    }

    private static String loadResource(ResourceLocation location) {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(location);
        if (res.isEmpty()) throw new RuntimeException("Missing shader resource " + location);
        try (InputStream in = res.get().open();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Error reading shader file " + location, e);
        }
    }

    public boolean isLoaded() {
        if (!tried) load();
        return hasLoaded;
    }

    /** {@code use()}: true, wenn das Programm aktiv ist (sonst wird die zugehoerige Zeichnung uebersprungen). */
    public boolean use() {
        if (!tried) load();
        if (!hasLoaded) return false;
        RenderSystem.assertOnRenderThread();
        GL20.glUseProgram(shaderProgram);
        SkyGL.activeShader = this;
        applyMatrices();
        return true;
    }

    /** {@code stop()}: Programm abwaehlen; Vanillas Programm-Cache zuruecksetzen, damit der naechste apply() bindet. */
    public void stop() {
        if (!hasLoaded) return;
        if (SkyGL.activeShader == this) SkyGL.activeShader = null;
        GameRenderer.getPositionShader().clear();
    }

    /** Uebergibt {@code gl_ModelViewProjectionMatrix} (aktueller {@link SkyGL}-Stapel, Projektion von RenderSystem). */
    void applyMatrices() {
        if (!hasLoaded) return;
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(SkyGL.pose());
        setMatrix("ModelViewMat", mv);
        setMatrix("ProjMat", RenderSystem.getProjectionMatrix());
    }

    private void setMatrix(String name, Matrix4f m) {
        int loc = getUniformLocation(name);
        if (loc < 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL20.glUniformMatrix4fv(loc, false, m.get(stack.mallocFloat(16)));
        }
    }

    public int getUniformLocation(String name) {
        if (!hasLoaded) return 0;
        return uniforms.computeIfAbsent(name, n -> GL20.glGetUniformLocation(shaderProgram, n));
    }

    public void setUniform1f(String name, float value) {
        if (!hasLoaded) return;
        GL20.glUniform1f(getUniformLocation(name), value);
    }

    public void setUniform1f(int location, float value) {
        if (!hasLoaded) return;
        GL20.glUniform1f(location, value);
    }

    public void setUniform1i(String name, int value) {
        if (!hasLoaded) return;
        GL20.glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform1i(int location, int value) {
        if (!hasLoaded) return;
        GL20.glUniform1i(location, value);
    }

    public void setUniform2f(String name, float a, float b) {
        if (!hasLoaded) return;
        GL20.glUniform2f(getUniformLocation(name), a, b);
    }

    /** {@code cleanup()} (Ressourcen-Neuladen). */
    public void cleanup() {
        if (!hasLoaded) { tried = false; return; }
        GL20.glDetachShader(shaderProgram, vertexShader);
        GL20.glDetachShader(shaderProgram, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        GL20.glDeleteProgram(shaderProgram);
        uniforms.clear();
        hasLoaded = false;
        tried = false;
    }
}
