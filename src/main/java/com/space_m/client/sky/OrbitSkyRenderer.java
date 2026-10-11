package com.space_m.client.sky;

import java.util.List;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystem.AstroMetric;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code com.hbm.dim.orbit.SkyProviderOrbit}: Himmel der Orbit-Dimension (Station umkreist einen
 * Koerper; beim Transfer gleiten Sonnengroesse und Blickrichtung zum Ziel). Stationszustand ueber {@link SkyHooks} (R6).
 * Original-Klasse: SkyProviderOrbit
 */
public class OrbitSkyRenderer extends CelestialSkyRenderer {

    private static CelestialBody lastBody;

    @Override
    public void render(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider celestialProvider, Camera camera, Runnable setupFog) {
        this.camera = camera;
        CelestialSkyProvider.Orbit provider = (CelestialSkyProvider.Orbit) celestialProvider;
        provider.ensureSky(world, partialTicks);
        SkyHooks station = SkyHooks.get();
        double progress = station.stationTransferProgress(partialTicks);
        float orbitalTilt = 80;

        SkyGL.depthMask(false);
        SkyGL.enableTexture();

        SkyGL.enableBlend();

        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);

        float solarAngle = getCelestialAngle(world, provider.metrics, partialTicks, provider);
        float siderealAngle = (float) SolarSystem.calculateSiderealAngle(world, partialTicks, CelestialSkyProvider.Orbit.orbiting());
        float celestialPhase = (1 - (solarAngle + 0.5F) % 1) * 2 - 1;

        float starBrightness = provider.getStarBrightness(world, partialTicks);

        renderStars(partialTicks, world, mc, starBrightness, solarAngle + siderealAngle, orbitalTilt);

        SkyGL.push();
        {

            SkyGL.rotate(orbitalTilt, 1.0F, 0.0F, 0.0F);
            SkyGL.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            SkyGL.rotate(solarAngle * 360.0F, 1.0F, 0.0F, 0.0F);

            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);

            CelestialBody orbitingBody = CelestialSkyProvider.Orbit.orbiting();
            double sunSize = SolarSystem.calculateSunSize(orbitingBody) * SolarSystem.SUN_RENDER_SCALE;
            if (station.stationState() != SkyHooks.StationState.ORBIT) {
                double sunTargetSize = SolarSystem.calculateSunSize(CelestialSkyProvider.Orbit.target()) * SolarSystem.SUN_RENDER_SCALE;
                sunSize = SkyMath.lerp(progress, sunSize, sunTargetSize);
            }
            double coronaSize = sunSize * (3 - SkyMath.smoothstep(Math.abs(celestialPhase), 0.7, 0.8));

            renderSun(partialTicks, world, mc, provider, orbitingBody.getStar(), sunSize, coronaSize, 1, 0);

            CelestialBody orbiting = orbitingBody;
            if (station.stationState() != SkyHooks.StationState.ORBIT && progress > 0.5) orbiting = CelestialSkyProvider.Orbit.target();

            renderCelestials(partialTicks, world, mc, provider.metrics, solarAngle, null, new Vec3(0, 0, 0), 1, 1, orbiting, SolarSystem.MAX_APPARENT_SIZE_ORBIT);

        }
        SkyGL.pop();


        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);
        SkyGL.disableBlend();

        SkyGL.enableTexture();
        SkyGL.depthMask(true);
    }

    // All angles within are normalized to -180/180
    private float getCelestialAngle(ClientLevel world, List<AstroMetric> metrics, float partialTicks, CelestialSkyProvider.Orbit provider) {
        SkyHooks station = SkyHooks.get();
        float solarAngle = provider.getCelestialAngle();
        if (station.stationState() == SkyHooks.StationState.ORBIT) return solarAngle;

        solarAngle = solarAngle * 360.0F - 180.0F;

        if (station.stationState() != SkyHooks.StationState.ARRIVING) lastBody = CelestialSkyProvider.Orbit.orbiting();
        if (lastBody == null) lastBody = CelestialSkyProvider.Orbit.orbiting();

        double progress = station.stationUnscaledProgress(partialTicks);
        float travelAngle = -(float) SolarSystem.calculateSingleAngle(metrics, lastBody, CelestialSkyProvider.Orbit.target());
        travelAngle = Mth.wrapDegrees(travelAngle + 90.0F);

        if (station.stationState() == SkyHooks.StationState.TRANSFER) {
            return (travelAngle + 180.0F) / 360.0F;
        } else if (station.stationState() == SkyHooks.StationState.LEAVING) {
            return ((float) SkyMath.clerp(progress, solarAngle, travelAngle) + 180.0F) / 360.0F;
        } else {
            return ((float) SkyMath.clerp(progress, travelAngle, solarAngle) + 180.0F) / 360.0F;
        }
    }

}
