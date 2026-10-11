package com.space_m.client.sky;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;
import com.space_m.celestial.trait.CBT_Weather;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code com.hbm.render.util.AtmosphereRenderUtil}: Farben, Dichte und Stil der Atmosphaere eines
 * Koerpers fuer die Planetenscheiben am Himmel und im Planetarium, Eve-Blitze, Nuke-Uniforms, 2D-Leuchtrand.
 * Dazu {@code WorldProviderCelestial.getAtmosphereFluidColor/getColorFromHex} (dort statisch).
 * Original-Klasse: AtmosphereRenderUtil
 */
public final class AtmosphereRenderUtil {

    private static final float OPAQUE_ATMOSPHERE_PRESSURE = 5.0F;
    private static final float CLOUDY_ATMOSPHERE_PRESSURE = 0.5F;
    private static final float DENSE_HAZE_PRESSURE = 3.0F;
    private static final float CLOUD_STORM_SMOOTHING_RATE = 6.0F;
    private static final String EVE_BODY_NAME = "eve";
    private static final float EVE_STRIKE_WINDOW = 8.0F;
    private static final float EVE_STRIKE_FADE_TIME = 5.0F;
    private static final float EVE_STRIKE_DELAY_RANGE = 2.4F;
    private static final float EVE_STRIKE_GATE_THRESHOLD = 0.82F;
    private static final double EVE_HASH_MULTIPLIER = 43758.5453123D;

    /** Original-Schluessel {@code body.dimensionId}; hier der Name (eindeutig, auch fuer Koerper ohne Dimension). */
    private static final Map<String, CloudStormFadeState> CLOUD_STORM_FADE_STATES = new HashMap<>();

    public static final int ATMOSPHERE_STYLE_CLEAR = 0;
    public static final int ATMOSPHERE_STYLE_CLOUDS = 1;
    public static final int ATMOSPHERE_STYLE_HAZE = 2;
    public static final int ATMOSPHERE_STYLE_GAS_BANDS = 3;
    public static final int LIGHTNING_MODE_STANDARD = 0;
    public static final int LIGHTNING_MODE_EVE = 1;
    private static final float DEFAULT_NUKE_SHOCK_CENTER = 0.5F;

    private static final class CloudStormFadeState {

        private float darkness;
        private long lastSampleTime;

        private CloudStormFadeState(float darkness, long lastSampleTime) {
            this.darkness = darkness;
            this.lastSampleTime = lastSampleTime;
        }
    }

    private AtmosphereRenderUtil() { }

    // ── WorldProviderCelestial (statisch) ──

    /** Original {@code WorldProviderCelestial.getAtmosphereFluidColor(FluidType)}. */
    public static Vec3 getAtmosphereFluidColor(ResourceLocation fluid) {
        if (fluid == null) {
            return new Vec3(1.0D, 1.0D, 1.0D);
        }

        if (fluid.equals(CelestialFluids.EVEAIR)) {
            return new Vec3(53F / 255F, 32F / 255F, 74F / 255F);
        }

        // Slightly redder "red sand" tint for Duna-like atmospheres.
        if (fluid.equals(CelestialFluids.DUNAAIR)) {
            return new Vec3(198F / 255F, 96F / 255F, 64F / 255F);
        }

        // Neutral/desaturated CO2 tint.
        if (fluid.equals(SkyFluids.CARBONDIOXIDE)) {
            return new Vec3(188F / 255F, 192F / 255F, 198F / 255F);
        }

        if (fluid.equals(CelestialFluids.EARTHAIR) || fluid.equals(CelestialFluids.OXYGEN) || fluid.equals(CelestialFluids.NITROGEN)) {
            return new Vec3(0.7529412F, 0.84705883F, 1.0F);
        }

        return getColorFromHex(CelestialFluids.color(fluid));
    }

    /** Original {@code WorldProviderCelestial.getColorFromHex}. */
    public static Vec3 getColorFromHex(int hexColor) {
        float red = ((hexColor >> 16) & 0xFF) / 255.0F;
        float green = ((hexColor >> 8) & 0xFF) / 255.0F;
        float blue = (hexColor & 0xFF) / 255.0F;
        return new Vec3(red, green, blue);
    }

    // ── AtmosphereRenderUtil ──

    private static float smoothCloudStormDarkness(CelestialBody body, float targetDarkness) {
        if (body == null) {
            return targetDarkness;
        }

        long now = Util.getMillis();
        CloudStormFadeState state = CLOUD_STORM_FADE_STATES.get(body.name);
        if (state == null) {
            CLOUD_STORM_FADE_STATES.put(body.name, new CloudStormFadeState(targetDarkness, now));
            return targetDarkness;
        }

        float deltaSeconds = Mth.clamp((now - state.lastSampleTime) / 1000.0F, 0.0F, 0.5F);
        float blend = Mth.clamp(deltaSeconds * CLOUD_STORM_SMOOTHING_RATE, 0.0F, 1.0F);
        state.darkness += (targetDarkness - state.darkness) * blend;
        state.lastSampleTime = now;

        return state.darkness;
    }

    public static float getAtmosphereGlowAlpha(CelestialBody body) {
        if (body == null) {
            return 0.0F;
        }

        if (body.gas != null) {
            return 0.35F;
        }

        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere != null) {
            float pressure = Mth.clamp((float) atmosphere.getPressure(), 0.0F, 3.0F);
            if (pressure <= 0.02F) {
                return 0.0F;
            }
            return Mth.clamp(0.08F + pressure * 0.16F, 0.08F, 0.5F);
        }

        return 0.0F;
    }

    public static float getAtmosphereSurfaceAlpha(CelestialBody body) {
        return getAtmosphereDensity(body);
    }

    public static float getAtmospherePressure(CelestialBody body) {
        if (body == null) {
            return 0.0F;
        }

        if (body.gas != null) {
            return OPAQUE_ATMOSPHERE_PRESSURE;
        }

        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere == null) {
            return 0.0F;
        }

        return Math.max(0.0F, (float) atmosphere.getPressure());
    }

    public static float getAtmosphereDensity(CelestialBody body) {
        return Mth.clamp(getAtmospherePressure(body) / OPAQUE_ATMOSPHERE_PRESSURE, 0.0F, 1.0F);
    }

    public static int getAtmosphereStyle(CelestialBody body) {
        if (body == null) {
            return ATMOSPHERE_STYLE_CLEAR;
        }

        if (body.gas != null) {
            return ATMOSPHERE_STYLE_GAS_BANDS;
        }

        float pressure = getAtmospherePressure(body);
        if (pressure > DENSE_HAZE_PRESSURE) {
            return ATMOSPHERE_STYLE_HAZE;
        }
        if (pressure >= CLOUDY_ATMOSPHERE_PRESSURE) {
            return ATMOSPHERE_STYLE_CLOUDS;
        }

        return ATMOSPHERE_STYLE_CLEAR;
    }

    public static Vec3 getBodyAtmosphereColor(CelestialBody body) {
        if (body == null) {
            return new Vec3(1.0D, 1.0D, 1.0D);
        }

        if (body.gas != null) {
            return getAtmosphereFluidColor(body.gas);
        }

        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        if (atmosphere != null && !atmosphere.fluids.isEmpty()) {
            double totalPressure = 0.0D;
            double r = 0.0D;
            double g = 0.0D;
            double b = 0.0D;

            for (FluidEntry entry : atmosphere.fluids) {
                if (entry == null || entry.fluid == null || entry.pressure <= 0.0D) {
                    continue;
                }

                Vec3 fluidColor = getAtmosphereFluidColor(entry.fluid);
                r += fluidColor.x * entry.pressure;
                g += fluidColor.y * entry.pressure;
                b += fluidColor.z * entry.pressure;
                totalPressure += entry.pressure;
            }

            if (totalPressure > 0.0D) {
                return new Vec3(
                    Mth.clamp(r / totalPressure, 0.0D, 1.0D),
                    Mth.clamp(g / totalPressure, 0.0D, 1.0D),
                    Mth.clamp(b / totalPressure, 0.0D, 1.0D)
                );
            }
        }

        return new Vec3(1.0D, 1.0D, 1.0D);
    }

    public static Vec3 getBodyCloudColor(CelestialBody body) {
        Vec3 baseClouds = new Vec3(1.0D, 1.0D, 1.0D);

        if (body == null) {
            return baseClouds;
        }

        if (body.gas != null) {
            Vec3 gasColor = getBodyAtmosphereColor(body);
            double gasPeak = Math.max(gasColor.x, Math.max(gasColor.y, gasColor.z));
            if (gasPeak <= 0.0D) {
                return new Vec3(1.0D, 1.0D, 1.0D);
            }

            return new Vec3(
                Mth.clamp(gasColor.x / gasPeak, 0.0D, 1.0D),
                Mth.clamp(gasColor.y / gasPeak, 0.0D, 1.0D),
                Mth.clamp(gasColor.z / gasPeak, 0.0D, 1.0D)
            );
        }

        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        return CelestialCloudRenderer.getTintedCloudColor(atmosphere, baseClouds);
    }

    public static float getBodyCloudTintStrength(CelestialBody body) {
        if (body == null) {
            return 0.0F;
        }

        if (body.gas != null) {
            return 1.0F;
        }

        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);
        return CelestialCloudRenderer.getCloudTintStrength(atmosphere);
    }

    public static boolean bodyHasWeatherCycle(CelestialBody body) {
        return CBT_Weather.supportsWeather(body);
    }

    public static float getBodyCloudStormDarkness(CelestialBody body, float partialTicks) {
        CBT_Weather weather = body != null ? body.getTrait(CBT_Weather.class) : null;
        float targetDarkness = 0.0F;

        if (weather != null && bodyHasWeatherCycle(body)) {
            float rainStrength = weather.getRainStrength(partialTicks);
            float thunderStrength = weather.getThunderStrength(partialTicks);
            float rainStateDarkness = weather.raining ? 0.22F : 0.0F;
            float thunderStateDarkness = weather.thundering ? 0.48F : 0.0F;
            float animatedDarkness = rainStrength * 0.35F + thunderStrength * 0.22F;
            targetDarkness = Mth.clamp(Math.max(rainStateDarkness + thunderStateDarkness, animatedDarkness), 0.0F, 0.9F);
        }

        return smoothCloudStormDarkness(body, targetDarkness);
    }

    public static float getBodyCloudLightningStrength(CelestialBody body, float partialTicks) {
        CBT_Weather weather = body != null ? body.getTrait(CBT_Weather.class) : null;
        if (weather == null || !bodyHasWeatherCycle(body) || !weather.canSpawnLightning) {
            return 0.0F;
        }

        float lightningActivity = CBT_Weather.getLightningActivityFactor(body);
        if (isEveLightningBody(body)) {
            return Mth.clamp(lightningActivity, 0.0F, 1.0F);
        }

        return Mth.clamp(weather.getThunderStrength(partialTicks) * lightningActivity, 0.0F, 1.0F);
    }

    public static int getBodyLightningMode(CelestialBody body) {
        return isEveLightningBody(body) ? LIGHTNING_MODE_EVE : LIGHTNING_MODE_STANDARD;
    }

    public static float getBodyEveFlashStrength(CelestialBody body, float atmosphereTime) {
        if (!isEveLightningBody(body)) {
            return 0.0F;
        }

        return getEveFlashStrength(atmosphereTime);
    }

    private static boolean isEveLightningBody(CelestialBody body) {
        return body != null && EVE_BODY_NAME.equals(body.name);
    }

    private static float getEveFlashStrength(float atmosphereTime) {
        float eventIndex = Mth.floor(atmosphereTime / EVE_STRIKE_WINDOW);
        float eventTime = atmosphereTime - eventIndex * EVE_STRIKE_WINDOW;
        float strikeDelay = eveHash(eventIndex, 41.9F) * EVE_STRIKE_DELAY_RANGE;
        if (eventTime < strikeDelay) {
            return 0.0F;
        }

        if (eveHash(eventIndex, 313.7F) < EVE_STRIKE_GATE_THRESHOLD) {
            return 0.0F;
        }

        return Mth.clamp(getEveStrikeFade(Math.max(eventTime - strikeDelay, 0.0F)), 0.0F, 1.0F);
    }

    private static float getEveStrikeFade(float eventTime) {
        float strikeIntro = smoothstep(0.0F, 0.14F, eventTime);
        float strikeFade = 1.0F - smoothstep(0.16F, EVE_STRIKE_FADE_TIME, eventTime);
        return strikeIntro * strikeFade;
    }

    private static float eveHash(float x, float y) {
        double value = Math.sin(x * 127.1D + y * 311.7D) * EVE_HASH_MULTIPLIER;
        return frac(value);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0F : 1.0F;
        }

        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float frac(double value) {
        return (float) (value - Math.floor(value));
    }

    public static void applyNukeShockUniforms(SkyShader shader, List<SkyHooks.ShockStatus> shocks, double currentTime) {
        int shockCount = Math.min(shocks != null ? shocks.size() : 0, SkyHooks.MAX_ACTIVE_SHOCKS);
        shader.setUniform1i("nukeShockCount", shockCount);

        for (int i = 0; i < SkyHooks.MAX_ACTIVE_SHOCKS; i++) {
            float shockTime = -1.0F;
            float shockCenterX = DEFAULT_NUKE_SHOCK_CENTER;
            float shockCenterY = DEFAULT_NUKE_SHOCK_CENTER;
            float shockStrength = 0.0F;

            if (i < shockCount) {
                SkyHooks.ShockStatus shock = shocks.get(i);
                shockTime = (float) (currentTime - shock.time());
                shockCenterX = shock.centerX();
                shockCenterY = shock.centerY();
                shockStrength = shock.strength();
            }

            shader.setUniform1f("nukeShockTime[" + i + "]", shockTime);
            shader.setUniform1f("nukeShockCenterX[" + i + "]", shockCenterX);
            shader.setUniform1f("nukeShockCenterY[" + i + "]", shockCenterY);
            shader.setUniform1f("nukeShockStrength[" + i + "]", shockStrength);
        }
    }

    /** Leuchtrand in 2D (GUI-Kartenansicht), Zeichnung ueber {@link SkyTess} mit dem aktuellen {@link SkyGL}-Stapel. */
    public static void renderAtmosphereGlow2D(CelestialBody body, double centerX, double centerY, double size, float visibility) {
        float glowAlpha = getAtmosphereGlowAlpha(body) * visibility;
        if (glowAlpha <= 0.001F) {
            return;
        }

        Vec3 atmo = getBodyAtmosphereColor(body);
        float r = Mth.clamp((float) atmo.x * 1.15F, 0.0F, 1.0F);
        float g = Mth.clamp((float) atmo.y * 1.15F, 0.0F, 1.0F);
        float b = Mth.clamp((float) atmo.z * 1.15F, 0.0F, 1.0F);

        double radius = size * 0.5D;
        float leadingGlow = glowAlpha;
        float trailingGlow = glowAlpha;
        double innerSize = radius * 0.98D;
        double middleSize = radius * 1.075D;
        double outerSize = radius * 1.15D * (1.0D + glowAlpha * 0.25D);

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.disableTexture();
        SkyGL.disableCull();

        SkyTess.startDrawingQuads();

        // Top band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(centerX - outerSize, centerY - outerSize, 0.0D);
        SkyTess.addVertex(centerX + outerSize, centerY - outerSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
        SkyTess.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
        SkyTess.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);

        SkyTess.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
        SkyTess.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow);
        SkyTess.addVertex(centerX + innerSize, centerY - innerSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow);
        SkyTess.addVertex(centerX - innerSize, centerY - innerSize, 0.0D);

        // Left band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(centerX + outerSize, centerY - outerSize, 0.0D);
        SkyTess.addVertex(centerX + outerSize, centerY + outerSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
        SkyTess.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
        SkyTess.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);

        SkyTess.addVertex(centerX + middleSize, centerY - middleSize, 0.0D);
        SkyTess.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow);
        SkyTess.addVertex(centerX + innerSize, centerY + innerSize, 0.0D);
        SkyTess.addVertex(centerX + innerSize, centerY - innerSize, 0.0D);

        // Bottom band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(centerX + outerSize, centerY + outerSize, 0.0D);
        SkyTess.addVertex(centerX - outerSize, centerY + outerSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
        SkyTess.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2.0F);
        SkyTess.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);

        SkyTess.addVertex(centerX + middleSize, centerY + middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
        SkyTess.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow);
        SkyTess.addVertex(centerX - innerSize, centerY + innerSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow);
        SkyTess.addVertex(centerX + innerSize, centerY + innerSize, 0.0D);

        // Right band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(centerX - outerSize, centerY + outerSize, 0.0D);
        SkyTess.addVertex(centerX - outerSize, centerY - outerSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2.0F);
        SkyTess.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
        SkyTess.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);

        SkyTess.addVertex(centerX - middleSize, centerY + middleSize, 0.0D);
        SkyTess.addVertex(centerX - middleSize, centerY - middleSize, 0.0D);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow);
        SkyTess.addVertex(centerX - innerSize, centerY - innerSize, 0.0D);
        SkyTess.addVertex(centerX - innerSize, centerY + innerSize, 0.0D);

        SkyTess.draw();

        SkyGL.enableCull();
        SkyGL.enableTexture();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);
    }
}
