package com.space_m.client.sky;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;

/**
 * Nebel des Space-Himmels:
 * <ul>
 *   <li>Farbe: 1.7.10 {@code EntityRenderer.updateFogColor} mischt die Nebelfarbe des Providers mit dessen
 *       Himmelsfarbe. In 1.20/1.21 nimmt {@code FogRenderer.setupColor} dafuer die Vanilla-Himmelsfarbe - deshalb
 *       rechnet {@link #computeFogColor} den Luft-Zweig von {@code setupColor} mit der Provider-Himmelsfarbe nach
 *       (Wasser/Lava/Pulverschnee bleiben Vanilla).</li>
 *   <li>Dichte: Original {@code ModEventHandlerRenderer.thickenFog} ({@code provider.fogDensity}, Modus {@code GL_EXP}).
 *       Den EXP-Nebel gibt es im Kernprofil nicht; er wird als linearer Nebel 0 .. 3/Dichte (95 % Deckung)
 *       nachgebildet. {@link #lastFogDensity} braucht der Himmel (Planetentoenung).</li>
 * </ul>
 */
public final class CelestialFog {
    private CelestialFog() {}

    /** Original {@code ModEventHandlerRenderer.lastFogDensity}. */
    public static float lastFogDensity;

    /** @return {r, g, b} oder null (Vanilla-Wert behalten). */
    public static float[] computeFogColor(Camera camera, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        CelestialSkyProvider provider = SkyDispatch.provider(level);
        if (provider == null) return null;
        FogType fogtype = camera.getFluidInCamera();
        if (fogtype != FogType.NONE) return null;

        provider.ensureSky(level, partialTicks);
        int renderDistance = mc.options.getEffectiveRenderDistance();

        float f4 = 0.25F + 0.75F * (float) renderDistance / 32.0F;
        f4 = 1.0F - (float) Math.pow(f4, 0.25D);
        Vec3 vec3 = provider.getSkyColor(level, camera.getPosition(), partialTicks);
        float f6 = (float) vec3.x;
        float f8 = (float) vec3.y;
        float f10 = (float) vec3.z;
        float f11 = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTicks) * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
        Vec3 vec32 = provider.getFogColor(level, f11);
        float fogRed = (float) vec32.x();
        float fogGreen = (float) vec32.y();
        float fogBlue = (float) vec32.z();
        if (renderDistance >= 4) {
            float f12 = Mth.sin(level.getSunAngle(partialTicks)) > 0.0F ? -1.0F : 1.0F;
            float f16 = camera.getLookVector().x() * f12;
            if (f16 < 0.0F) {
                f16 = 0.0F;
            }

            if (f16 > 0.0F) {
                float[] afloat = provider.calcSunriseSunsetColors(level, level.getTimeOfDay(partialTicks), partialTicks);
                if (afloat != null) {
                    f16 *= afloat[3];
                    fogRed = fogRed * (1.0F - f16) + afloat[0] * f16;
                    fogGreen = fogGreen * (1.0F - f16) + afloat[1] * f16;
                    fogBlue = fogBlue * (1.0F - f16) + afloat[2] * f16;
                }
            }
        }

        fogRed += (f6 - fogRed) * f4;
        fogGreen += (f8 - fogGreen) * f4;
        fogBlue += (f10 - fogBlue) * f4;
        float f13 = level.getRainLevel(partialTicks);
        if (f13 > 0.0F) {
            float f14 = 1.0F - f13 * 0.5F;
            float f17 = 1.0F - f13 * 0.4F;
            fogRed *= f14;
            fogGreen *= f14;
            fogBlue *= f17;
        }

        float f15 = level.getThunderLevel(partialTicks);
        if (f15 > 0.0F) {
            float f18 = 1.0F - f15 * 0.5F;
            fogRed *= f18;
            fogGreen *= f18;
            fogBlue *= f18;
        }

        float f5 = ((float) camera.getPosition().y - (float) level.getMinBuildHeight()) * level.getLevelData().getClearColorScale();
        if (f5 < 1.0F) {
            if (f5 < 0.0F) {
                f5 = 0.0F;
            }

            f5 *= f5;
            fogRed *= f5;
            fogGreen *= f5;
            fogBlue *= f5;
        }

        float darken = mc.gameRenderer.getDarkenWorldAmount(partialTicks);
        if (darken > 0.0F) {
            fogRed = fogRed * (1.0F - darken) + fogRed * 0.7F * darken;
            fogGreen = fogGreen * (1.0F - darken) + fogGreen * 0.6F * darken;
            fogBlue = fogBlue * (1.0F - darken) + fogBlue * 0.6F * darken;
        }

        float f7 = 0.0F;
        Entity entity = camera.getEntity();
        if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.NIGHT_VISION) && !living.hasEffect(MobEffects.DARKNESS)) {
            f7 = net.minecraft.client.renderer.GameRenderer.getNightVisionScale(living, partialTicks);
        }

        if (fogRed != 0.0F && fogGreen != 0.0F && fogBlue != 0.0F) {
            float f9 = Math.min(1.0F / fogRed, Math.min(1.0F / fogGreen, 1.0F / fogBlue));
            fogRed = fogRed * (1.0F - f7) + fogRed * f9 * f7;
            fogGreen = fogGreen * (1.0F - f7) + fogGreen * f9 * f7;
            fogBlue = fogBlue * (1.0F - f7) + fogBlue * f9 * f7;
        }

        return new float[] {fogRed, fogGreen, fogBlue};
    }

    /**
     * Original {@code thickenFog}: Dichte des Providers; > 0 -> {near, far} des linearen Ersatznebels, sonst null.
     */
    public static float[] fogDistances(Camera camera, float farPlane) {
        ClientLevel level = Minecraft.getInstance().level;
        CelestialSkyProvider provider = SkyDispatch.provider(level);
        if (provider == null) {
            lastFogDensity = 0;
            return null;
        }
        FogType type = camera.getFluidInCamera();
        if (type == FogType.LAVA || type == FogType.POWDER_SNOW) return null;

        lastFogDensity = provider.fogDensity(level, type == FogType.WATER);
        if (lastFogDensity <= 0) return null;

        float far = Math.min(farPlane, 3.0F / lastFogDensity);
        return new float[] {0.0F, far};
    }
}
