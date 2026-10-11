package com.space_m.client.sky;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
//? if < 1.21.1 {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import com.space_m.main.SpaceRegistry;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

/**
 * Himmel einer Space-Dimension (und von Kerbin = Oberwelt) als {@link DimensionSpecialEffects}: entspricht den
 * 1.7.10-Hooks {@code WorldProvider.getSkyRenderer/getCloudRenderer/getWeatherRenderer} ({@code renderSky},
 * {@code renderClouds}, {@code renderSnowAndRain}) und den Farb-/Licht-Hooks des Providers
 * ({@code getFogColor} -> {@link #getBrightnessDependentFogColor}, {@code calcSunriseSunsetColors} ->
 * {@link #getSunriseColor}, {@code updateLightmap}/Sonnenhelligkeit -> {@link #adjustLightmapColors}).
 *
 * <p>1.20.1 (Forge {@code IForgeDimensionSpecialEffects}) bekommt einen {@code PoseStack}, 1.21.1 (NeoForge
 * {@code IDimensionSpecialEffectsExtension}) eine Modelview-Matrix - beides wird zur Ausgangsmatrix von {@link SkyGL}.
 */
public class CelestialEffects extends DimensionSpecialEffects {

    public final CelestialSkyProvider provider;
    private final CelestialSkyRenderer sky;
    private final CelestialCloudRenderer clouds = new CelestialCloudRenderer();
    private final CelestialWeatherRenderer weather = new CelestialWeatherRenderer();

    /** Vanilla-Effekte fuer fremde Dimensionen mit demselben Effekt-Schluessel (nur {@code minecraft:overworld}). */
    private final DimensionSpecialEffects fallback;

    public CelestialEffects(float cloudLevel, CelestialSkyProvider provider, CelestialSkyRenderer sky, DimensionSpecialEffects fallback) {
        super(cloudLevel, true, SkyType.NORMAL, false, false);
        this.provider = provider;
        this.sky = sky;
        this.fallback = fallback;
    }

    // ── Farben ──

    /** Original {@code getFogColor(solarAngle, y)}: Biom-Nebelfarbe wird ersetzt, {@code brightness} = Sonnenfaktor. */
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!SkyDispatch.active(level)) return fallback != null ? fallback.getBrightnessDependentFogColor(fogColor, brightness) : fogColor;
        return provider.getFogColor(level, brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    /** Original {@code calcSunriseSunsetColors} (wirkt auf die Nebelfarbe wie in 1.7.10). */
    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!SkyDispatch.active(level)) return fallback != null ? fallback.getSunriseColor(timeOfDay, partialTicks) : super.getSunriseColor(timeOfDay, partialTicks);
        return provider.calcSunriseSunsetColors(level, timeOfDay, partialTicks);
    }

    // ── Himmel ──

    //? if < 1.21.1 {
    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        Matrix4f view = poseStack.last().pose();
    //?} else {
    /*@Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        Matrix4f view = modelViewMatrix;
    *///?}
        if (!SkyDispatch.active(level)) return false;
        setupFog.run();
        if (isFoggy || SkyDispatch.skyBlocked(camera)) return true;
        SkyDispatch.pushIdentityModelView();
        try {
            SkyGL.begin(view);
            sky.render(partialTick, level, Minecraft.getInstance(), provider, camera, setupFog);
        } catch (RuntimeException e) {
            SkyDispatch.logOnce("sky", e);
        } finally {
            SkyGL.reset();
            SkyDispatch.popModelView();
            setupFog.run();
        }
        return true;
    }

    // ── Wolken ──

    //? if < 1.21.1 {
    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        Matrix4f view = poseStack.last().pose();
    //?} else {
    /*@Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack, double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        Matrix4f view = new Matrix4f(poseStack.last().pose()).mul(modelViewMatrix);
    *///?}
        if (!SkyDispatch.active(level)) return false;
        SkyDispatch.pushIdentityModelView();
        try {
            SkyGL.begin(view);
            clouds.render(partialTick, level, Minecraft.getInstance(), provider, getCloudHeight(), camX, camY, camZ);
        } catch (RuntimeException e) {
            SkyDispatch.logOnce("clouds", e);
        } finally {
            SkyGL.reset();
            SkyDispatch.popModelView();
        }
        return true;
    }

    // ── Wetter ──

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        if (!SkyDispatch.active(level)) return false;
        // Koerper ohne Wetterzyklus: das Original zeichnet nichts (canDoRainSnowIce = false)
        boolean useShaderTransparency = Minecraft.useShaderTransparency();
        try {
            SkyGL.begin(new Matrix4f());
            RenderSystem.depthMask(useShaderTransparency);
            weather.render(partialTick, level, Minecraft.getInstance(), provider, lightTexture, camX, camY, camZ);
        } catch (RuntimeException e) {
            SkyDispatch.logOnce("weather", e);
        } finally {
            SkyGL.reset();
            RenderSystem.depthMask(useShaderTransparency);
        }
        return true;
    }

    // ── Lichtkarte ──

    /**
     * Original: Sonnenhelligkeit des Providers ({@code getSunBrightness}: Finsternis, Dichte, Krieg, Blitze) statt
     * Vanilla-{@code skyDarken}, danach {@code updateLightmap} des Koerpers (Ike rot, Laythe gruen). Die Vanilla-Formel
     * dieser Zelle wird mit der Provider-Helligkeit neu gerechnet (nur ohne Bossnebel-Abdunkelung).
     */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, Vector3f colors) {
        if (!SkyDispatch.active(level)) return;
        try {
            SkyDispatch.adjustLightmap(this, level, partialTicks, skyDarken, blockLightRedFlicker, pixelX, pixelY, colors);
        } catch (RuntimeException e) {
            SkyDispatch.logOnce("lightmap", e);
        }
    }

    @Override
    public String toString() {
        return "CelestialEffects[" + provider.getClass().getSimpleName() + "/" + sky.getClass().getSimpleName() + "]";
    }

    static {
        SpaceRegistry.LOGGER.debug("[space_m] CelestialEffects loaded");
    }
}
