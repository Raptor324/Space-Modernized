package com.space_m.client.sky;

import java.util.List;
import java.util.Random;

import com.hbm_m.extprop.HbmLivingProps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystem.AstroMetric;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Destroyed;
import com.space_m.celestial.trait.CBT_Dyson;
import com.space_m.celestial.trait.CBT_Impact;
import com.space_m.celestial.trait.CBT_Lights;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.celestial.trait.CelestialBodyTrait.CBT_COMPROMISED;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von NTM Space {@code com.hbm.dim.SkyProviderCelestial}: Himmel aller Himmelskoerper (auch Kerbin =
 * Oberwelt). Sonne mit Korona und Dyson-Schwarm (bzw. Schwarzes Loch), Sterne mit Achsneigung und siderischem Winkel,
 * alle anderen Koerper mit Groesse/Position/Phase aus der Bahnmechanik ({@code SolarSystem.calculateMetricsFromBody}),
 * Ringe, Atmosphaeren-/Wolken-/Blitz-/Lichter-Shader, Einschlaege, Krieg, Meteore, Finsternis, Abendrot, Planetenboden
 * aus grosser Hoehe.
 *
 * <p>Technische Uebersetzung: GL11-Zustandsmaschine -> {@link SkyGL}, Tessellator -> {@link SkyTess},
 * {@code Shader} -> {@link SkyShader}, {@code glCallList(glSkyList/2)} -> {@link SkyDomes}. Anaglyph-3D gibt es seit 1.13
 * nicht mehr (entfaellt). Satelliten/Stationen/Nuke-Schocks kommen ueber {@link SkyHooks} (R6/R8).
 * Original-Klasse: SkyProviderCelestial
 */
public class CelestialSkyRenderer {

    private static final ResourceLocation planetTexture = SpaceMod.id("textures/misc/space/planet.png");
    private static final ResourceLocation flareTexture = SpaceMod.id("textures/misc/space/sunspike.png");
    private static final ResourceLocation nightTexture = SpaceMod.id("textures/misc/space/night.png");
    private static final ResourceLocation digammaStar = SpaceMod.id("textures/misc/space/star_digamma.png");
    private static final ResourceLocation lodeStar = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/misc/star_lode.png");
    private static final ResourceLocation stationTexture = SpaceMod.id("textures/misc/space/station.png");

    protected static final ResourceLocation impactTexture = SpaceMod.id("textures/misc/space/impact.png");
    protected static final ResourceLocation shockwaveTexture = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/particle/shockwave.png");
    protected static final ResourceLocation shockFlareTexture = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/particle/flare.png");

    private static final ResourceLocation ringTexture = SpaceMod.id("textures/misc/space/rings.png");
    private static final ResourceLocation destroyedBody = SpaceMod.id("textures/misc/space/destroyed.png");

    protected static final ResourceLocation thatmoShield = SpaceMod.id("textures/particle/cens.png");

    private static final ResourceLocation noise = SpaceMod.id("textures/shaders/ichannel1.png");

    protected static final ResourceLocation particleBase = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/particle/particle_base.png");

    private static final ResourceLocation[] citylights = new ResourceLocation[] {
        SpaceMod.id("textures/misc/space/citylights_0.png"),
        SpaceMod.id("textures/misc/space/citylights_1.png"),
        SpaceMod.id("textures/misc/space/citylights_2.png"),
        SpaceMod.id("textures/misc/space/citylights_3.png"),
    };

    private static final ResourceLocation defaultMask = SpaceMod.id("textures/misc/space/default_mask.png");

    private static float currentFov = 70;

    /** Kamera der laufenden Zeichnung (Original {@code mc.renderViewEntity}, {@code RenderManager.playerView*}). */
    protected Camera camera;

    /**
     * Original {@code render(partialTicks, world, mc)}. {@code setupFog}: Vanilla-Himmelsnebel ({@code FOG_SKY}) setzen.
     * Matrix/Zustand ({@link SkyGL}) ist vom Aufrufer mit der Kamerarotation begonnen.
     */
    public void render(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider celestialProvider, Camera camera, Runnable setupFog) {
        this.camera = camera;
        celestialProvider.ensureSky(world, partialTicks);

        float fogIntensity = CelestialFog.lastFogDensity * 30;
        currentFov = mc.options.fov().get().floatValue();

        CelestialBody body = CelestialBody.getBody(world);
        CelestialBody sun = body.getStar();
        CBT_Atmosphere atmosphere = body.getTrait(CBT_Atmosphere.class);

        boolean hasAtmosphere = atmosphere != null;

        float pressure = hasAtmosphere ? (float) atmosphere.getPressure() : 0.0F;
        float visibility = hasAtmosphere ? Mth.clamp(2.0F - pressure, 0.1F, 1.0F) : 1.0F;

        SkyGL.disableTexture();
        Vec3 pos = camera.getPosition();
        Vec3 skyColor = celestialProvider.getSkyColor(world, pos, partialTicks);

        float skyR = (float) skyColor.x;
        float skyG = (float) skyColor.y;
        float skyB = (float) skyColor.z;

        // Diminish sky colour when leaving the atmosphere
        if (pos.y > 300) {
            double curvature = Mth.clamp((800.0F - (float) pos.y) / 500.0F, 0.0F, 1.0F);
            skyR *= curvature;
            skyG *= curvature;
            skyB *= curvature;
        }

        float planetR = skyR;
        float planetG = skyG;
        float planetB = skyB;

        FogRenderer.levelFogColor();
        if (fogIntensity > 0.01F) {
            float[] fogColor = RenderSystem.getShaderFogColor();
            planetR = (float) SkyMath.clampedLerp(skyR, fogColor[0], fogIntensity);
            planetG = (float) SkyMath.clampedLerp(skyG, fogColor[1], fogIntensity);
            planetB = (float) SkyMath.clampedLerp(skyB, fogColor[2], fogIntensity);
        }

        Vec3 planetTint = new Vec3(planetR, planetG, planetB);

        SkyGL.depthMask(false);
        SkyGL.color(skyR, skyG, skyB);

        // Set maximum sky fog distance to 12 chunks, works nicely with Celeritas/Distant Horizons
        // and makes for a more consistent sky in vanilla too
        setupFog.run();
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(Math.min(12.0F, mc.options.getEffectiveRenderDistance()) * 16.0F);
        SkyDomes.drawSky();
        setupFog.run();

        SkyGL.enableTexture();

        SkyGL.enableBlend();

        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);

        float starBrightness = celestialProvider.getStarBrightness(world, partialTicks) * visibility;
        float solarAngle = world.getTimeOfDay(partialTicks);
        float siderealAngle = (float) SolarSystem.calculateSiderealAngle(world, partialTicks, body);

        // Handle any special per-body sunset rendering
        renderSunset(partialTicks, world, mc, celestialProvider, solarAngle, pressure, body.surfaceTexture);

        renderStars(partialTicks, world, mc, starBrightness, solarAngle + siderealAngle, body.axialTilt);

        SkyGL.push();
        {

            SkyGL.rotate(body.axialTilt, 1.0F, 0.0F, 0.0F);
            SkyGL.rotate(-90.0F, 0.0F, 1.0F, 0.0F);

            SkyGL.rotate(solarAngle * 360.0F, 1.0F, 0.0F, 0.0F);

            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);

            // Scale sun size for rendering (texture is 4 times larger than actual, for glow)
            double sunSize = SolarSystem.calculateSunSize(body) * SolarSystem.SUN_RENDER_SCALE;
            double coronaSize = sunSize * (3 - Mth.clamp(pressure, 0.0F, 1.0F));

            renderSun(partialTicks, world, mc, celestialProvider, sun, sunSize, coronaSize, visibility, pressure);

            float blendAmount = hasAtmosphere ? Mth.clamp(1 - CelestialSkyProvider.getSunBrightnessFactor(world, partialTicks), 0.25F, 1F) : 1F;

            renderCelestials(partialTicks, world, mc, celestialProvider.metrics, solarAngle, null, planetTint, visibility, blendAmount, null, SolarSystem.MAX_APPARENT_SIZE_SURFACE);

            SkyGL.enableBlend();

            if (visibility > 0.2F) {
                // JEFF BOZOS WOULD LIKE TO KNOW YOUR LOCATION
                // ... to send you a pakedge :)))
                // Satellites (Bobmazon on Kerbin, client satellites, held preview): R6 via SkyHooks
                SkyHooks.get().renderSatellites(partialTicks, world, solarAngle);

                // Stations, too
                for (SkyHooks.StationPos station : SkyHooks.get().orbitingStations()) {
                    renderStation(partialTicks, world, mc, station, solarAngle);
                }
            }

        }
        SkyGL.pop();

        render3DModel(partialTicks, world, mc);

        // TODO: fix EVERYTHING
        // k lmao

        CBT_War war = body.getTrait(CBT_War.class);
        if (war != null) {
            for (int i = 0; i < war.getProjectiles().size(); i++) {
                CBT_War.Projectile projectile = war.getProjectiles().get(i);
                float thing = projectile.getFlashtime() + partialTicks;

                if (projectile.getTravel() <= 0) {
                    float alpd = 1.0F - Math.min(1.0F, thing / 100);

                    SkyGL.push();
                    {

                        render3DModel(partialTicks, world, mc);

                        SkyGL.translate(projectile.getTranslateX() + 70, projectile.getTranslateY(), projectile.getTranslateZ() + 50);
                        SkyGL.scale(thing, thing, thing);
                        SkyGL.rotate(90.0, -10.0, -1.0, 50.0);
                        SkyGL.rotate(20.0, -0.0, -1.0, 1.0);

                        SkyGL.color(1, 1, 1, alpd);

                        SkyGL.bind(shockwaveTexture);
                        SkyObjModel.PLANE.renderAll();

                    }
                    SkyGL.pop();

                    SkyGL.push();
                    {

                        SkyGL.translate(projectile.getTranslateX() + 70, projectile.getTranslateY(), projectile.getTranslateZ() + 50);
                        SkyGL.scale(thing * 0.4f, thing * 0.4f, thing * 0.4f);
                        SkyGL.rotate(90.0, -10.0, -1.0, 50.0);
                        SkyGL.rotate(20.0, -0.0, -1.0, 1.0);
                        SkyGL.color(1, 1, 1, alpd);

                        SkyGL.bind(thatmoShield);
                        SkyObjModel.PLANE.renderAll();

                    }
                    SkyGL.pop();
                }
            }
        }

        float rainStrength = world.getRainLevel(partialTicks);

        for (CelestialSkyProvider.Meteor meteor : CelestialSkyProvider.meteors) {
            SkyGL.push();

            // optimised 3 sqrt per meteor to just 1
            Vec3 offset = new Vec3(meteor.posX - pos.x, meteor.posY - pos.y, meteor.posZ - pos.z);
            double offsetLength = offset.length();
            double distance = Math.min(mc.options.getEffectiveRenderDistance() * 16, offsetLength);
            Vec3 offsetNormal = offsetLength >= 1.0E-4D ? new Vec3(offset.x / offsetLength, offset.y / offsetLength, offset.z / offsetLength) : offset;
            Vec3 renderOffset = new Vec3(offsetNormal.x * distance, offsetNormal.y * distance, offsetNormal.z * distance);

            SkyGL.translate(renderOffset.x, renderOffset.y, renderOffset.z);

            double descent = 2017d - meteor.posY;
            double quadratic = (-(descent * descent) + (1517 * descent)) / 41;

            float scalar = (float) (quadratic / offsetLength);
            SkyGL.scale(scalar, scalar, scalar);

            if (meteor.type == CelestialSkyProvider.MeteorType.SMOKE) {
                SkyGL.color(1, 0, 0, 1);
                SkyGL.bind(particleBase);
                renderSmoke(meteor.age);
            } else {
                SkyGL.color(1, 1, 1, 1);
                SkyGL.bind(shockFlareTexture);
                renderGlow(1, 1, 1, rainStrength);
            }

            SkyGL.pop();
        }

        if (body.hasRings) {
            SkyGL.push();
            {

                SkyGL.rotate(body.axialTilt - body.ringTilt, 1.0F, 0.0F, 0.0F);
                SkyGL.translate(0, -100, 0);
                SkyGL.rotate(-90.0F, 0.0F, 1.0F, 0.0F);

                renderRings(partialTicks, world, mc, body.ringTilt, body.ringColor, 200, visibility);

            }
            SkyGL.pop();
        }

        renderSpecialEffects(partialTicks, world, mc);

        CBT_COMPROMISED compromised = body.getTrait(CBT_COMPROMISED.class);
        if (compromised != null) {
            SkyGL.push();
            {

                float time = ((float) world.getDayTime() + partialTicks) * 0.2F;

                SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);
                SkyGL.disableCull();

                SkyGL.scale(194.5, 70.5, 94.5);
                SkyGL.rotate(90, 0, 0, 1);

                SkyGL.bind(noise);
                if (SkyShaders.FLESH.use()) {
                    SkyObjModel.SPHERE_V2.renderAll();

                    // Fix orbital plane
                    SkyGL.rotate(-90.0F, 0, 1, 0);

                    SkyShaders.FLESH.setUniform1f("iTime", time * 0.05F);
                    SkyShaders.FLESH.setUniform1i("iChannel1", 0);

                    SkyShaders.FLESH.stop();
                }

                SkyGL.enableCull();
                SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);

            }
            SkyGL.pop();
        }

        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);
        SkyGL.disableBlend();

        SkyGL.disableTexture();
        SkyGL.color(0.0F, 0.0F, 0.0F);

        double heightAboveHorizon = pos.y - celestialProvider.getHorizon(world);

        if (heightAboveHorizon < 0.0D) {
            SkyGL.push();
            {

                SkyGL.translate(0.0F, 12.0F, 0.0F);
                SkyDomes.drawSky2();

            }
            SkyGL.pop();

            float f8 = 1.0F;
            float f9 = -((float) (heightAboveHorizon + 65.0D));
            float opposite = -f8;

            SkyTess.startDrawingQuads();
            SkyTess.setColorRGBA_I(0, 255);
            SkyTess.addVertex(-f8, f9, f8);
            SkyTess.addVertex(f8, f9, f8);
            SkyTess.addVertex(f8, opposite, f8);
            SkyTess.addVertex(-f8, opposite, f8);
            SkyTess.addVertex(-f8, opposite, -f8);
            SkyTess.addVertex(f8, opposite, -f8);
            SkyTess.addVertex(f8, f9, -f8);
            SkyTess.addVertex(-f8, f9, -f8);
            SkyTess.addVertex(f8, opposite, -f8);
            SkyTess.addVertex(f8, opposite, f8);
            SkyTess.addVertex(f8, f9, f8);
            SkyTess.addVertex(f8, f9, -f8);
            SkyTess.addVertex(-f8, f9, -f8);
            SkyTess.addVertex(-f8, f9, f8);
            SkyTess.addVertex(-f8, opposite, f8);
            SkyTess.addVertex(-f8, opposite, -f8);
            SkyTess.addVertex(-f8, opposite, -f8);
            SkyTess.addVertex(-f8, opposite, f8);
            SkyTess.addVertex(f8, opposite, f8);
            SkyTess.addVertex(f8, opposite, -f8);
            SkyTess.draw();
        }

        if (celestialProvider.isSkyColored()) {
            SkyGL.color(skyR * 0.2F + 0.04F, skyG * 0.2F + 0.04F, skyB * 0.6F + 0.1F);
        } else {
            SkyGL.color(skyR, skyG, skyB);
        }

        SkyGL.push();
        {

            SkyGL.translate(0.0F, -((float) (heightAboveHorizon - 16.0D)), 0.0F);
            SkyDomes.drawSky2();

        }
        SkyGL.pop();

        double sc = 1 / (pos.y / 1000);
        double uvOffset = (pos.x / 1024) % 1;
        SkyGL.push();
        {

            SkyGL.enableTexture();
            SkyGL.enableBlend();

            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);

            float sunBrightness = celestialProvider.getSunBrightness(world, partialTicks);

            float alpha = ((float) pos.y - 200.0F) / 300.0F;
            if (alpha > 0.0F) {
                SkyGL.color(sunBrightness, sunBrightness, sunBrightness, alpha);
                SkyGL.bind(body.texture);
                SkyGL.rotate(180, 1, 0, 0);

                SkyTess.startDrawingQuads();
                SkyTess.addVertexWithUV(-115 * sc, 100.0D, -115 * sc, 0.0D + uvOffset, 0.0D);
                SkyTess.addVertexWithUV(115 * sc, 100.0D, -115 * sc, 1.0D + uvOffset, 0.0D);
                SkyTess.addVertexWithUV(115 * sc, 100.0D, 115 * sc, 1.0D + uvOffset, 1.0D);
                SkyTess.addVertexWithUV(-115 * sc, 100.0D, 115 * sc, 0.0D + uvOffset, 1.0D);
                SkyTess.draw();
            }

            SkyGL.disableBlend();

            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);

        }

        SkyGL.pop();
        SkyGL.enableTexture();
        SkyGL.depthMask(true);
    }

    protected void renderSunset(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider provider, float solarAngle, float pressure, ResourceLocation surfaceTexture) {
        float[] sunsetColor = calcSunriseSunsetColors(partialTicks, world, mc, provider, solarAngle, pressure);

        if (sunsetColor != null) {
            float[] anaglyphColor = sunsetColor;
            float sunsetDirection = Mth.sin(world.getSunAngle(partialTicks)) < 0.0F ? 180.0F : 0.0F;

            SkyGL.disableTexture();

            SkyGL.push();
            {

                SkyGL.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                SkyGL.rotate(sunsetDirection, 0.0F, 0.0F, 1.0F);
                SkyGL.rotate(90.0F, 0.0F, 0.0F, 1.0F);

                SkyTess.startDrawing(SkyTess.TRIANGLE_FAN);
                SkyTess.setColorRGBA_F(anaglyphColor[0], anaglyphColor[1], anaglyphColor[2], sunsetColor[3]);
                SkyTess.addVertex(0.0, 100.0, 0.0);
                SkyTess.setColorRGBA_F(sunsetColor[0], sunsetColor[1], sunsetColor[2], 0.0F);
                byte segments = 16;

                for (int j = 0; j <= segments; ++j) {
                    float angle = (float) j * 3.1415927F * 2.0F / (float) segments;
                    float sinAngle = Mth.sin(angle);
                    float cosAngle = Mth.cos(angle);
                    SkyTess.addVertex(sinAngle * 120.0F, cosAngle * 120.0F, -cosAngle * 40.0F * sunsetColor[3]);
                }

                SkyTess.draw();

            }
            SkyGL.pop();
            SkyGL.enableTexture();

            // charged dust
            if (pressure < 0.05F && surfaceTexture != null) {
                Random rand = new Random(0);

                SkyGL.push();
                {

                    double time = ((double) world.getDayTime() + partialTicks) * 0.002;

                    SkyGL.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    SkyGL.rotate(sunsetDirection, 0.0F, 0.0F, 1.0F);
                    SkyGL.rotate(90.0F, 0.0F, 0.0F, 1.0F);

                    SkyGL.bind(surfaceTexture);
                    SkyGL.color(0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F, rand.nextFloat() * sunsetColor[3] * 4.0F);

                    SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_CONSTANT_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);

                    SkyTess.startDrawing(SkyTess.POINTS);
                    for (int i = 0; i < 1024; i++) {
                        SkyTess.addVertex(rand.nextGaussian() * 50, 100, -((Math.abs(rand.nextGaussian() * 20) + time) % Math.abs(rand.nextGaussian()) * 20));
                    }
                    SkyTess.draw();

                }
                SkyGL.pop();
            }
        }
    }

    // We don't want certain sunrise/sunset effects to change the fog colour, so we do them here
    protected float[] calcSunriseSunsetColors(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider provider, float solarAngle, float pressure) {
        if (pressure < 0.05F) {
            float cutoff = 0.4F;
            float angle = Mth.cos(solarAngle * (float) Math.PI * 2.0F) - 0.0F;

            if (angle < -cutoff || angle > cutoff) return null;

            float colorIntensity = angle / cutoff * 0.5F + 0.5F;
            float alpha = 1.0F - (1.0F - Mth.sin(colorIntensity * (float) Math.PI)) * 0.99F;
            alpha *= alpha;
            return new float[] {0.9F, 1.0F, 1.0F, alpha * 0.2F};
        }

        return provider.calcSunriseSunsetColors(world, world.getTimeOfDay(partialTicks), partialTicks);
    }

    protected void renderStars(float partialTicks, ClientLevel world, Minecraft mc, float starBrightness, float siderealAngle, float axialTilt) {
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);

        if (SkyDispatch.renderLodeStar()) {
            float lodeSize = 1F + world.random.nextFloat() * 0.5F;
            double lodeDist = 100D;

            SkyGL.push();
            {

                SkyGL.rotate(-75.0F, 1.0F, 0.0F, 0.0F);
                SkyGL.rotate(10.0F, 0.0F, 1.0F, 0.0F);
                SkyGL.color(1F, 1F, 1F, 1.0F);
                SkyGL.bind(lodeStar); // genu-ine bona-fide ass whooping

                SkyTess.startDrawingQuads();
                SkyTess.addVertexWithUV(-lodeSize, lodeDist, -lodeSize, 0.0D, 0.0D);
                SkyTess.addVertexWithUV(lodeSize, lodeDist, -lodeSize, 0.0D, 1.0D);
                SkyTess.addVertexWithUV(lodeSize, lodeDist, lodeSize, 1.0D, 1.0D);
                SkyTess.addVertexWithUV(-lodeSize, lodeDist, lodeSize, 1.0D, 0.0D);
                SkyTess.draw();

            }
            SkyGL.pop();
        }

        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);

        SkyGL.push();
        {

            SkyGL.rotate(axialTilt, 1.0F, 0.0F, 0.0F);

            SkyGL.rotate(-90.0F, 0.0F, 1.0F, 0.0F);

            SkyGL.rotate(siderealAngle * 360.0F, 1.0F, 0.0F, 0.0F);
            SkyGL.rotate(-90.0F, 1.0F, 0.0F, 0.0F);

            if (starBrightness > 0.0F) {
                SkyGL.push();
                {

                    SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);

                    SkyGL.bind(nightTexture);
                    float starBrightnessAlpha = starBrightness * 0.6f;
                    SkyGL.color(1.0F, 1.0F, 1.0F, starBrightnessAlpha);

                    SkyGL.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    SkyGL.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                    renderSkyboxSide(4);

                    SkyGL.push();
                    SkyGL.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    renderSkyboxSide(1);
                    SkyGL.pop();

                    SkyGL.push();
                    SkyGL.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                    renderSkyboxSide(0);
                    SkyGL.pop();

                    SkyGL.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                    renderSkyboxSide(5);

                    SkyGL.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                    renderSkyboxSide(2);

                    SkyGL.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                    renderSkyboxSide(3);

                }
                SkyGL.pop();
            }

            // demeter
            SkyGL.push();
            {

                SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);

                float brightness = Math.max(0.5F, starBrightness * 1.6F);
                SkyGL.color(brightness, brightness, brightness, brightness);

                SkyGL.bind(digammaStar);

                float digamma = Minecraft.getInstance().player != null ? HbmLivingProps.getDigamma(Minecraft.getInstance().player) : 0F;
                float digmaSize = (1 + digamma * 0.25F);
                float digmaDist = 100F - digamma * 2.5F;

                SkyGL.rotate(140.0F, 1.0F, 0.0F, 0.0F);
                SkyGL.rotate(-40.0F, 0.0F, 0.0F, 1.0F);

                SkyTess.startDrawingQuads();
                SkyTess.addVertexWithUV(-digmaSize, digmaDist, -digmaSize, 0.0D, 0.0D);
                SkyTess.addVertexWithUV(digmaSize, digmaDist, -digmaSize, 0.0D, 1.0D);
                SkyTess.addVertexWithUV(digmaSize, digmaDist, digmaSize, 1.0D, 1.0D);
                SkyTess.addVertexWithUV(-digmaSize, digmaDist, digmaSize, 1.0D, 0.0D);
                SkyTess.draw();

                SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);

            }
            SkyGL.pop();

        }
        SkyGL.pop();
    }

    protected void renderSun(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider provider, CelestialBody sun, double sunSize, double coronaSize, float visibility, float pressure) {
        CBT_Dyson dyson = sun.getTrait(CBT_Dyson.class);
        int swarmCount = dyson != null ? dyson.size() : 0;

        SkyShader sunShader = SkyShaders.forBody(sun.shaderLocation);
        if (sunShader != null && sun.hasTrait(CBT_Destroyed.class)) {
            // BLACK HOLE SUN
            // WON'T YOU COME
            // AND WASH AWAY THE RAIN

            double shaderSize = sunSize * sun.shaderScale;

            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);

            SkyGL.bind(noise);
            if (sunShader.use()) {
                float time = ((float) world.getDayTime() + partialTicks) / 20.0F;

                SkyGL.push();

                // Fix orbital plane
                SkyGL.rotate(-90.0F, 0, 1, 0);
                sunShader.setUniform1f("iTime", time);
                sunShader.setUniform1i("iChannel1", 0);

                SkyTess.startDrawingQuads();
                SkyTess.addVertexWithUV(-shaderSize, 100.0D, -shaderSize, 0.0D, 0.0D);
                SkyTess.addVertexWithUV(shaderSize, 100.0D, -shaderSize, 1.0D, 0.0D);
                SkyTess.addVertexWithUV(shaderSize, 100.0D, shaderSize, 1.0D, 1.0D);
                SkyTess.addVertexWithUV(-shaderSize, 100.0D, shaderSize, 0.0D, 1.0D);
                SkyTess.draw();

                sunShader.stop();

                SkyGL.pop();
            }

            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        } else {
            // Some blanking to conceal the stars
            SkyGL.disableTexture();
            SkyGL.color(0.0F, 0.0F, 0.0F, 1.0F);

            SkyTess.startDrawingQuads();
            SkyTess.addVertex(-sunSize, 99.9D, -sunSize);
            SkyTess.addVertex(sunSize, 99.9D, -sunSize);
            SkyTess.addVertex(sunSize, 99.9D, sunSize);
            SkyTess.addVertex(-sunSize, 99.9D, sunSize);
            SkyTess.draw();

            // Draw the sun to the depth buffer to block swarm members that are behind
            // (Original: Farbe 0/0/0/0; der Kern-Shader verwirft Alpha 0 -> nur Tiefe ueber colorMask schreiben)
            SkyGL.depthMask(true);
            SkyGL.colorMask(false, false, false, false);
            SkyGL.color(0.0F, 0.0F, 0.0F, 1.0F);

            SkyTess.startDrawingQuads();
            SkyTess.addVertexWithUV(-sunSize * 0.25D, 100.1D, -sunSize * 0.25D, 0.0D, 0.0D);
            SkyTess.addVertexWithUV(sunSize * 0.25D, 100.1D, -sunSize * 0.25D, 1.0D, 0.0D);
            SkyTess.addVertexWithUV(sunSize * 0.25D, 100.1D, sunSize * 0.25D, 1.0D, 1.0D);
            SkyTess.addVertexWithUV(-sunSize * 0.25D, 100.1D, sunSize * 0.25D, 0.0D, 1.0D);
            SkyTess.draw();

            SkyGL.colorMask(true, true, true, true);
            SkyGL.depthMask(false);

            SkyGL.enableTexture();
            SkyGL.color(1.0F, 1.0F, 1.0F, visibility);

            SkyGL.bind(SolarSystem.kerbol.texture);

            float[] sunColor = provider.getSunColor(world);

            SkyGL.color(sunColor[0], sunColor[1], sunColor[2], visibility);
            SkyTess.startDrawingQuads();
            SkyTess.addVertexWithUV(-sunSize, 100.0D, -sunSize, 0.0D, 0.0D);
            SkyTess.addVertexWithUV(sunSize, 100.0D, -sunSize, 1.0D, 0.0D);
            SkyTess.addVertexWithUV(sunSize, 100.0D, sunSize, 1.0D, 1.0D);
            SkyTess.addVertexWithUV(-sunSize, 100.0D, sunSize, 0.0D, 1.0D);
            SkyTess.draw();

            // Draw a big ol' spiky flare! Less so when there is an atmosphere
            SkyGL.color(sunColor[0], sunColor[1], sunColor[2], 1 - Mth.clamp(pressure, 0.0F, 1.0F) * 0.75F);
            SkyGL.bind(flareTexture);

            SkyTess.startDrawingQuads();
            SkyTess.addVertexWithUV(-coronaSize, 99.9D, -coronaSize, 0.0D, 0.0D);
            SkyTess.addVertexWithUV(coronaSize, 99.9D, -coronaSize, 1.0D, 0.0D);
            SkyTess.addVertexWithUV(coronaSize, 99.9D, coronaSize, 1.0D, 1.0D);
            SkyTess.addVertexWithUV(-coronaSize, 99.9D, coronaSize, 0.0D, 1.0D);
            SkyTess.draw();

            // Draw the swarm members with depth occlusion
            // We do this last so we can render transparency against the sun
            renderSwarm(partialTicks, world, mc, sunSize * 0.5, swarmCount);

            // Clear and disable the depth buffer once again, buffer has to be writable to clear it
            SkyGL.depthMask(true);
            SkyGL.clearDepth();
            SkyGL.depthMask(false);
        }
    }

    private void renderSwarm(float partialTicks, ClientLevel world, Minecraft mc, double swarmRadius, int swarmCount) {
        // bloodseeking, parasitic, ecstatically tracing decay
        // thriving in the glow that death emits, the warm perfume it radiates

        //perfume makes my eyes water -j

        if (swarmCount <= 0) return;
        SkyShader swarmShader = SkyShaders.SWARM;
        if (!swarmShader.use()) return;

        // swarm members render as pixels, which can vary based on screen resolution
        // because of this, we make the pixels more transparent based on their apparent size, which varies by a fair few factors
        // this isn't a foolproof solution, analyzing the projection matrices would be best, but it works for now.
        float swarmScreenSize = (float) ((mc.getWindow().getHeight() / currentFov) * swarmRadius * 0.002);
        float time = ((float) world.getDayTime() + partialTicks) / 800.0F;

        swarmShader.setUniform1f("iTime", time);

        int offsetLocation = swarmShader.getUniformLocation("iOffset");

        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);
        SkyGL.color(0.0F, 0.0F, 0.0F, Mth.clamp(swarmScreenSize, 0, 1));

        SkyGL.push();
        {

            SkyGL.translate(0.0F, 100.0F, 0.0F);
            SkyGL.scale(swarmRadius, swarmRadius, swarmRadius);

            SkyGL.push();
            {

                SkyGL.rotate(80.0F, 1, 0, 0);

                SkyTess.startDrawing(SkyTess.POINTS);
                for (int i = 0; i < swarmCount; i += 3) {
                    swarmShader.setUniform1f(offsetLocation, i);

                    float t = i + time;
                    double x = Math.cos(t);
                    double z = Math.sin(t);

                    SkyTess.addVertex(x, 0, z);
                }
                SkyTess.draw();

            }
            SkyGL.pop();

            SkyGL.push();
            {

                SkyGL.rotate(60.0F, 0, 1, 0);
                SkyGL.rotate(80.0F, 1, 0, 0);

                SkyTess.startDrawing(SkyTess.POINTS);
                for (int i = 1; i < swarmCount; i += 3) {
                    swarmShader.setUniform1f(offsetLocation, i);

                    float t = i + time;
                    double x = Math.cos(t);
                    double z = Math.sin(t);

                    SkyTess.addVertex(x, 0, z);
                }
                SkyTess.draw();

            }
            SkyGL.pop();

            SkyGL.push();
            {

                SkyGL.rotate(-60.0F, 0, 1, 0);
                SkyGL.rotate(80.0F, 1, 0, 0);

                SkyTess.startDrawing(SkyTess.POINTS);
                for (int i = 2; i < swarmCount; i += 3) {
                    swarmShader.setUniform1f(offsetLocation, i);

                    float t = i + time;
                    double x = Math.cos(t);
                    double z = Math.sin(t);

                    SkyTess.addVertex(x, 0, z);
                }
                SkyTess.draw();

            }
            SkyGL.pop();

        }
        SkyGL.pop();

        swarmShader.stop();

        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);
    }

    protected void renderCelestials(float partialTicks, ClientLevel world, Minecraft mc, List<AstroMetric> metrics, float solarAngle, CelestialBody tidalLockedBody, Vec3 planetTint, float visibility, float blendAmount, CelestialBody orbiting, float maxSize) {
        float blendDarken = 0.1F;

        double transitionMinSize = 0.01D;
        double transitionMaxSize = 0.5D;

        for (AstroMetric metric : metrics) {

            // Ignore self
            if (metric.distance == 0)
                continue;

            boolean orbitingThis = metric.body == orbiting;

            double uvOffset = orbitingThis ? 1 - ((((double) world.getDayTime() + partialTicks) / 1024) % 1) : 0;
            double atmospherePatternOffset = orbitingThis ? -(((double) world.getDayTime() + partialTicks) / 1024.0D) : 0.0D;
            float axialTilt = orbitingThis ? 0 : metric.body.axialTilt;

            SkyGL.push();
            {

                double size = Mth.clamp(metric.apparentSize, 0, maxSize);
                boolean renderPoint = size < transitionMaxSize;
                boolean renderBody = size > transitionMinSize;

                SkyGL.rotate(metric.angle, 1.0, 0.0, 0.0);
                SkyGL.rotate(metric.inclination, 0.0, 0.0, 1.0);
                SkyGL.rotate(axialTilt + 90.0F, 0.0F, 1.0F, 0.0F);

                if (renderBody) {
                    // Draw the back half of the ring (obscured by body)
                    if (metric.body.hasRings) {
                        SkyGL.push();
                        {

                            SkyGL.color(metric.body.ringColor[0], metric.body.ringColor[1], metric.body.ringColor[2], visibility);
                            SkyGL.bind(ringTexture);

                            SkyGL.disableCull();

                            double ringSize = size * metric.body.ringSize;

                            SkyGL.translate(0.0F, 100.0F, 0.0F);
                            SkyGL.rotate(-metric.angle, 0, 0, 1);
                            SkyGL.rotate(90.0F - metric.body.ringTilt, 1, 0, 0);
                            SkyGL.rotate(metric.angle, 0, 1, 0);

                            SkyTess.startDrawingQuads();
                            SkyTess.addVertexWithUV(-ringSize, 0, -ringSize, 0.0D, 0.0D);
                            SkyTess.addVertexWithUV(ringSize, 0, -ringSize, 1.0D, 0.0D);
                            SkyTess.addVertexWithUV(ringSize, 0, 0, 1.0D, 0.5D);
                            SkyTess.addVertexWithUV(-ringSize, 0, 0, 0.0D, 0.5D);
                            SkyTess.draw();

                            SkyGL.enableCull();

                        }
                        SkyGL.pop();
                    }

                    CBT_Destroyed d = metric.body.getTrait(CBT_Destroyed.class);

                    if (d != null) {
                        // Stop calling things "interp", that's a verb not a noun
                        //its cause of "interpolate" which is my favorite word apparently :(
                        double destroyedProgressClientInterpolation = d.destProgress + size * 0.5;

                        float alpha = (float) (1.0F - Math.min(1.0F, destroyedProgressClientInterpolation / 100));
                        Random random = new Random(12);

                        int numQuads = 30;
                        for (int i = 0; i < numQuads; i++) {
                            double radius = (random.nextDouble() * size) * d.destProgress;

                            double randomTheta = random.nextDouble() * Math.PI * 2;
                            double randomPhi = random.nextDouble() * Math.PI;

                            double randomX = radius * Math.sin(randomPhi) * Math.cos(randomTheta) * 0.7;
                            double randomY = radius * Math.sin(randomPhi) * Math.sin(randomTheta);
                            double randomZ = radius * Math.cos(randomPhi) * 0.7;

                            float randomRotation = random.nextFloat() * 360.0F;


                            double uMin = random.nextDouble();
                            double vMin = random.nextDouble();
                            double uMax = Math.min(uMin + (random.nextDouble() * 0.2), 1.0);
                            double vMax = Math.min(vMin + (random.nextDouble() * 0.2), 1.0);

                            SkyGL.push();
                            {

                                SkyGL.translate(randomX * -0.05, randomY * 0.00, randomZ * -0.05);

                                SkyGL.rotate(randomRotation * d.destProgress * 0.05F, 0.0F, 1.0F, 0.0F);

                                SkyGL.bind(metric.body.texture);
                                SkyGL.color(1, 1, 1, 1);

                                SkyTess.startDrawingQuads();
                                double qsize = size * random.nextDouble() * 0.1;
                                SkyTess.addVertexWithUV(-qsize, 100.0D, -qsize, uMin, vMin);
                                SkyTess.addVertexWithUV(qsize, 100.0D, -qsize, uMax, vMin);
                                SkyTess.addVertexWithUV(qsize, 100.0D, qsize, uMax, vMax);
                                SkyTess.addVertexWithUV(-qsize, 100.0D, qsize, uMin, vMax);

                                SkyTess.draw();
                            }

                            SkyGL.pop();
                            SkyGL.push();
                            {

                                SkyGL.translate(randomX * 0.04, randomY * 0.00, randomZ * 0.04);

                                SkyGL.rotate(randomRotation * d.destProgress * 0.05F, 0.0F, 1.0F, 0.0F);
                                SkyGL.bind(destroyedBody);
                                SkyGL.color(1, 1, 1, 1);
                                SkyTess.startDrawingQuads();
                                double qsize = size * random.nextDouble() * 0.07;
                                SkyTess.addVertexWithUV(-qsize, 100.0D, -qsize, uMin, vMin);
                                SkyTess.addVertexWithUV(qsize, 100.0D, -qsize, uMax, vMin);
                                SkyTess.addVertexWithUV(qsize, 100.0D, qsize, uMax, vMax);
                                SkyTess.addVertexWithUV(-qsize, 100.0D, qsize, uMin, vMax);

                                SkyTess.draw();

                            }
                            SkyGL.pop();

                        }


                        SkyGL.color(1.0F, 1.0F, 1.0F, alpha);
                        SkyGL.bind(shockwaveTexture);
                        double interpe = (d.destProgress * 0.5) * size * 0.1;
                        SkyTess.startDrawingQuads();
                        SkyTess.addVertexWithUV(-interpe, 100.0D, -interpe, 0.0D + uvOffset, 0.0D);
                        SkyTess.addVertexWithUV(interpe, 100.0D, -interpe, 1.0D + uvOffset, 0.0D);
                        SkyTess.addVertexWithUV(interpe, 100.0D, interpe, 1.0D + uvOffset, 1.0D);
                        SkyTess.addVertexWithUV(-interpe, 100.0D, interpe, 0.0D + uvOffset, 1.0D);
                        SkyTess.draw();


                        SkyGL.color(1.0F, 1.0F, 1.0F, alpha * 2);
                        SkyGL.bind(shockFlareTexture);

                        destroyedProgressClientInterpolation = size * 3;
                        SkyTess.startDrawingQuads();
                        SkyTess.addVertexWithUV(-destroyedProgressClientInterpolation, 100.0D, -destroyedProgressClientInterpolation, 0.0D + uvOffset, 0.0D);
                        SkyTess.addVertexWithUV(destroyedProgressClientInterpolation, 100.0D, -destroyedProgressClientInterpolation, 1.0D + uvOffset, 0.0D);
                        SkyTess.addVertexWithUV(destroyedProgressClientInterpolation, 100.0D, destroyedProgressClientInterpolation, 1.0D + uvOffset, 1.0D);
                        SkyTess.addVertexWithUV(-destroyedProgressClientInterpolation, 100.0D, destroyedProgressClientInterpolation, 0.0D + uvOffset, 1.0D);
                        SkyTess.draw();

                    } else {

                        renderAtmosphereGlow(metric.body, size, 1.0F, metric.phase);

                        SkyGL.disableBlend();
                        SkyGL.color(1.0F, 1.0F, 1.0F, visibility);
                        SkyGL.bind(metric.body.texture);

                        SkyTess.startDrawingQuads();
                        SkyTess.addVertexWithUV(-size, 100.0D, -size, 0.0D + uvOffset, 0.0D);
                        SkyTess.addVertexWithUV(size, 100.0D, -size, 1.0D + uvOffset, 0.0D);
                        SkyTess.addVertexWithUV(size, 100.0D, size, 1.0D + uvOffset, 1.0D);
                        SkyTess.addVertexWithUV(-size, 100.0D, size, 0.0D + uvOffset, 1.0D);
                        SkyTess.draw();


                        CBT_Impact impact = metric.body.getTrait(CBT_Impact.class);
                        CBT_Lights light = metric.body.getTrait(CBT_Lights.class);
                        List<SkyHooks.ShockStatus> nukeShocks = SkyHooks.get().clientShocks(metric.body);

                        double impactTime = impact != null ? (world.getGameTime() - impact.time) + partialTicks : 0;
                        float impactAnimationTime = impact != null ? (float) impactTime : -1.0F;
                        int lightIntensity = light != null && impactTime < 40 ? Mth.clamp(light.getIntensity(), 0, citylights.length - 1) : 0;

                        int blackoutInterval = 8;
                        int maxBlackouts = 5;

                        int activeBlackouts = Math.min((int) (impactTime / blackoutInterval), maxBlackouts);

                        Vec3 atmosphereColor = AtmosphereRenderUtil.getBodyAtmosphereColor(metric.body);
                        Vec3 cloudColor = AtmosphereRenderUtil.getBodyCloudColor(metric.body);
                        float cloudTintStrength = AtmosphereRenderUtil.getBodyCloudTintStrength(metric.body);
                        float cloudStormDarkness = AtmosphereRenderUtil.getBodyCloudStormDarkness(metric.body, partialTicks);
                        float cloudLightningStrength = AtmosphereRenderUtil.getBodyCloudLightningStrength(metric.body, partialTicks);
                        float atmosphereOverlayAlpha = AtmosphereRenderUtil.getAtmosphereSurfaceAlpha(metric.body);
                        float atmosphereDensity = AtmosphereRenderUtil.getAtmosphereDensity(metric.body);
                        int atmosphereStyle = AtmosphereRenderUtil.getAtmosphereStyle(metric.body);

                        float atmosphereTime = ((float) world.getGameTime() + partialTicks) / 20.0F;
                        double currentShockTime = world.getGameTime() + partialTicks;
                        renderAtmosphereSurface(atmosphereColor, cloudColor, cloudTintStrength, cloudStormDarkness, atmosphereOverlayAlpha, uvOffset, atmospherePatternOffset, size, atmosphereTime, atmosphereStyle, impactAnimationTime, nukeShocks, currentShockTime);
                        renderCrescentShadow((float) -metric.phase, uvOffset, size);
                        renderAtmosphereEmissive(metric.body, (float) -metric.phase, uvOffset, size, lightIntensity, activeBlackouts, atmosphereDensity, atmospherePatternOffset, atmosphereTime, atmosphereStyle, impactAnimationTime, nukeShocks, currentShockTime);
                        renderNightLights(metric.body, (float) -metric.phase, uvOffset, size, lightIntensity, activeBlackouts, atmosphereDensity, atmospherePatternOffset, atmosphereTime, atmosphereStyle, impactAnimationTime, nukeShocks, currentShockTime);
                        renderLightningOverlay(metric.body, (float) -metric.phase, cloudTintStrength, cloudLightningStrength, atmosphereOverlayAlpha, uvOffset, atmospherePatternOffset, size, atmosphereTime, atmosphereStyle, impactAnimationTime, nukeShocks, currentShockTime);

                        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);

                        if (impact != null) {
                            double lavaAlpha = Math.min(impactTime * 0.1, 1.0);

                            double impactSize = (impactTime * 0.1) * size * 0.035;
                            double impactAlpha = 1.0 - Math.min(1.0, impactTime * 0.0015);
                            double flareSize = size * 1.5;
                            double flareAlpha = 1.0 - Math.min(1.0, impactTime * 0.002);

                            if (lavaAlpha > 0) {
                                SkyGL.color(1.0, 1.0, 1.0, lavaAlpha);
                                SkyGL.bind(impactTexture);

                                SkyTess.startDrawingQuads();
                                SkyTess.addVertexWithUV(-size, 100.0D, -size, 0.0D + uvOffset, 0.0D);
                                SkyTess.addVertexWithUV(size, 100.0D, -size, 1.0D + uvOffset, 0.0D);
                                SkyTess.addVertexWithUV(size, 100.0D, size, 1.0D + uvOffset, 1.0D);
                                SkyTess.addVertexWithUV(-size, 100.0D, size, 0.0D + uvOffset, 1.0D);
                                SkyTess.draw();
                            }

                            SkyGL.push();
                            {

                                SkyGL.translate(-size * 0.5, 0, size * 0.4);

                                // impact shockwave, increases in size and fades out
                                if (impactAlpha > 0) {
                                    SkyGL.color(1.0, 1.0, 1.0F, impactAlpha);
                                    SkyGL.bind(shockwaveTexture);

                                    SkyTess.startDrawingQuads();
                                    SkyTess.addVertexWithUV(-impactSize, 100.0D, -impactSize, 0.0D, 0.0D);
                                    SkyTess.addVertexWithUV(impactSize, 100.0D, -impactSize, 1.0D, 0.0D);
                                    SkyTess.addVertexWithUV(impactSize, 100.0D, impactSize, 1.0D, 1.0D);
                                    SkyTess.addVertexWithUV(-impactSize, 100.0D, impactSize, 0.0D, 1.0D);
                                    SkyTess.draw();
                                }

                                // impact flare, remains static in size and fades out
                                if (flareAlpha > 0) {
                                    SkyGL.color(1.0F, 1.0F, 1.0F, flareAlpha);
                                    SkyGL.bind(shockFlareTexture);

                                    SkyTess.startDrawingQuads();
                                    SkyTess.addVertexWithUV(-flareSize, 100.0D, -flareSize, 0.0D, 0.0D);
                                    SkyTess.addVertexWithUV(flareSize, 100.0D, -flareSize, 1.0D, 0.0D);
                                    SkyTess.addVertexWithUV(flareSize, 100.0D, flareSize, 1.0D, 1.0D);
                                    SkyTess.addVertexWithUV(-flareSize, 100.0D, flareSize, 0.0D, 1.0D);
                                    SkyTess.draw();
                                }

                            }
                            SkyGL.pop();
                        }


                        SkyGL.disableTexture();

                        // Draw another layer on top to blend with the atmosphere
                        SkyGL.color(planetTint.x - blendDarken, planetTint.y - blendDarken, planetTint.z - blendDarken, (1 - blendAmount * visibility));

                        SkyTess.startDrawingQuads();
                        SkyTess.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
                        SkyTess.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
                        SkyTess.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
                        SkyTess.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
                        SkyTess.draw();

                        SkyGL.enableTexture();
                    }


                    // Draw the front half of the ring (unobscured)
                    if (metric.body.hasRings) {
                        SkyGL.push();
                        SkyGL.color(metric.body.ringColor[0], metric.body.ringColor[1], metric.body.ringColor[2], visibility);
                        SkyGL.bind(ringTexture);

                        double ringSize = size * metric.body.ringSize;

                        SkyGL.disableCull();

                        SkyGL.translate(0.0F, 100.0F, 0.0F);
                        SkyGL.rotate(-metric.angle, 0, 0, 1);
                        SkyGL.rotate(90.0F - metric.body.ringTilt, 1, 0, 0);
                        SkyGL.rotate(metric.angle, 0, 1, 0);

                        SkyTess.startDrawingQuads();
                        SkyTess.addVertexWithUV(-ringSize, 0, 0, 0.0D, 0.5D);
                        SkyTess.addVertexWithUV(ringSize, 0, 0, 1.0D, 0.5D);
                        SkyTess.addVertexWithUV(ringSize, 0, ringSize, 1.0D, 1.0D);
                        SkyTess.addVertexWithUV(-ringSize, 0, ringSize, 0.0D, 1.0D);
                        SkyTess.draw();

                        SkyGL.enableCull();
                        SkyGL.pop();
                    }

                    List<SkyHooks.ShockStatus> flashShocks = SkyHooks.get().clientShocks(metric.body);
                    double flashShockTime = world.getGameTime() + partialTicks;
                    renderNukeImpactOverlays(size, (float) -metric.phase, flashShocks, flashShockTime);
                }

                if (renderPoint) {
                    float alpha = Mth.clamp((float) size * 100.0F, 0.0F, 1.0F);
                    alpha *= 1 - SkyMath.remap01_clamp((float) size, (float) transitionMinSize, (float) transitionMaxSize);
                    SkyGL.color(metric.body.color[0], metric.body.color[1], metric.body.color[2], alpha * visibility);
                    SkyGL.bind(planetTexture);

                    SkyTess.startDrawingQuads();
                    SkyTess.addVertexWithUV(-1.0D, 100.0D, -1.0D, 0.0D, 0.0D);
                    SkyTess.addVertexWithUV(1.0D, 100.0D, -1.0D, 1.0D, 0.0D);
                    SkyTess.addVertexWithUV(1.0D, 100.0D, 1.0D, 1.0D, 1.0D);
                    SkyTess.addVertexWithUV(-1.0D, 100.0D, 1.0D, 0.0D, 1.0D);
                    SkyTess.draw();
                }

            }
            SkyGL.pop();
        }
    }

    private void drawPlanetShaderQuad(double size) {
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(-size, 100.0D, -size, 0.0D, 0.0D);
        SkyTess.addVertexWithUV(size, 100.0D, -size, 1.0D, 0.0D);
        SkyTess.addVertexWithUV(size, 100.0D, size, 1.0D, 1.0D);
        SkyTess.addVertexWithUV(-size, 100.0D, size, 0.0D, 1.0D);
        SkyTess.draw();
    }

    private void renderAtmosphereSurface(Vec3 atmosphereColor, Vec3 cloudColor, float cloudTintStrength, float cloudStormDarkness, float atmosphereAlpha, double uvOffset, double patternOffset, double size, float atmosphereTime, int atmosphereStyle, float impactTime, List<SkyHooks.ShockStatus> nukeShocks, double currentShockTime) {
        if (atmosphereAlpha <= 0.001F) {
            return;
        }

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        SkyShader atmosphereShader = SkyShaders.ATMOSPHERE;
        if (!atmosphereShader.use()) return;
        atmosphereShader.setUniform1f("offset", (float) uvOffset);
        atmosphereShader.setUniform1f("patternOffset", (float) patternOffset);
        atmosphereShader.setUniform1i("bodyTex", 0);
        atmosphereShader.setUniform1i("useBodyAlphaMask", 0);
        atmosphereShader.setUniform1f("atmosphereColorR", (float) atmosphereColor.x);
        atmosphereShader.setUniform1f("atmosphereColorG", (float) atmosphereColor.y);
        atmosphereShader.setUniform1f("atmosphereColorB", (float) atmosphereColor.z);
        atmosphereShader.setUniform1f("cloudColorR", (float) cloudColor.x);
        atmosphereShader.setUniform1f("cloudColorG", (float) cloudColor.y);
        atmosphereShader.setUniform1f("cloudColorB", (float) cloudColor.z);
        atmosphereShader.setUniform1f("cloudTintStrength", cloudTintStrength);
        atmosphereShader.setUniform1f("cloudStormDarkness", cloudStormDarkness);
        atmosphereShader.setUniform1f("atmosphereAlpha", atmosphereAlpha);
        atmosphereShader.setUniform1f("atmosphereTime", atmosphereTime);
        atmosphereShader.setUniform1i("atmosphereStyle", atmosphereStyle);
        atmosphereShader.setUniform1f("impactTime", impactTime);
        AtmosphereRenderUtil.applyNukeShockUniforms(atmosphereShader, nukeShocks, currentShockTime);
        drawPlanetShaderQuad(size);
        atmosphereShader.stop();
    }

    private void renderLightningOverlay(CelestialBody body, float phase, float cloudTintStrength, float cloudLightningStrength, float atmosphereAlpha, double uvOffset, double patternOffset, double size, float atmosphereTime, int atmosphereStyle, float impactTime, List<SkyHooks.ShockStatus> nukeShocks, double currentShockTime) {
        if (atmosphereAlpha <= 0.001F || cloudLightningStrength <= 0.001F || (atmosphereStyle != AtmosphereRenderUtil.ATMOSPHERE_STYLE_CLOUDS && atmosphereStyle != AtmosphereRenderUtil.ATMOSPHERE_STYLE_HAZE)) {
            return;
        }

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        SkyShader lightningShader = SkyShaders.LIGHTNING;
        SkyGL.bind(body.texture);
        SkyGL.bindUnit(1, body.cityMask != null ? body.cityMask : defaultMask);
        if (!lightningShader.use()) return;
        lightningShader.setUniform1f("phase", phase);
        lightningShader.setUniform1f("offset", (float) uvOffset);
        lightningShader.setUniform1f("patternOffset", (float) patternOffset);
        lightningShader.setUniform1i("bodyTex", 0);
        lightningShader.setUniform1i("cityMask", 1);
        lightningShader.setUniform1i("useBodyAlphaMask", 0);
        lightningShader.setUniform1f("cloudTintStrength", cloudTintStrength);
        lightningShader.setUniform1f("cloudLightningStrength", cloudLightningStrength);
        lightningShader.setUniform1f("atmosphereAlpha", atmosphereAlpha);
        lightningShader.setUniform1f("atmosphereTime", atmosphereTime);
        lightningShader.setUniform1f("eveFlashStrength", AtmosphereRenderUtil.getBodyEveFlashStrength(body, atmosphereTime));
        lightningShader.setUniform1i("atmosphereStyle", atmosphereStyle);
        lightningShader.setUniform1i("lightningMode", AtmosphereRenderUtil.getBodyLightningMode(body));
        lightningShader.setUniform1f("impactTime", impactTime);
        AtmosphereRenderUtil.applyNukeShockUniforms(lightningShader, nukeShocks, currentShockTime);

        drawPlanetShaderQuad(size);
        lightningShader.stop();
    }

    private void renderNukeImpactOverlays(double size, float phase, List<SkyHooks.ShockStatus> nukeShocks, double currentShockTime) {
        if (nukeShocks.isEmpty()) {
            return;
        }

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        SkyShader nukeShader = SkyShaders.NUKE;
        if (!nukeShader.use()) return;
        nukeShader.setUniform1f("phase", phase);
        AtmosphereRenderUtil.applyNukeShockUniforms(nukeShader, nukeShocks, currentShockTime);
        drawPlanetShaderQuad(size);
        nukeShader.stop();
    }

    private void renderAtmosphereEmissive(CelestialBody body, float phase, double uvOffset, double size, int lightIntensity, int activeBlackouts, float atmosphereDensity, double patternOffset, float atmosphereTime, int atmosphereStyle, float impactTime, List<SkyHooks.ShockStatus> nukeShocks, double currentShockTime) {
        if (lightIntensity <= 0 || atmosphereDensity <= 0.001F || (atmosphereStyle != AtmosphereRenderUtil.ATMOSPHERE_STYLE_CLOUDS && atmosphereStyle != AtmosphereRenderUtil.ATMOSPHERE_STYLE_HAZE)) {
            return;
        }

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        SkyShader atmosphereEmissiveShader = SkyShaders.ATMOSPHERE_EMISSIVE;
        SkyGL.bind(citylights[lightIntensity]);
        SkyGL.bindUnit(1, body.cityMask != null ? body.cityMask : defaultMask);
        if (!atmosphereEmissiveShader.use()) return;
        atmosphereEmissiveShader.setUniform1f("phase", phase);
        atmosphereEmissiveShader.setUniform1f("offset", (float) uvOffset);
        atmosphereEmissiveShader.setUniform1f("atmosphereDensity", atmosphereDensity);
        atmosphereEmissiveShader.setUniform1f("patternOffset", (float) patternOffset);
        atmosphereEmissiveShader.setUniform1f("atmosphereTime", atmosphereTime);
        atmosphereEmissiveShader.setUniform1i("atmosphereStyle", atmosphereStyle);
        atmosphereEmissiveShader.setUniform1f("impactTime", impactTime);
        AtmosphereRenderUtil.applyNukeShockUniforms(atmosphereEmissiveShader, nukeShocks, currentShockTime);
        atmosphereEmissiveShader.setUniform1i("bodyTex", 0);
        atmosphereEmissiveShader.setUniform1i("lights", 0);
        atmosphereEmissiveShader.setUniform1i("cityMask", 1);
        atmosphereEmissiveShader.setUniform1i("blackouts", activeBlackouts);
        atmosphereEmissiveShader.setUniform1i("useBodyAlphaMask", 0);

        drawPlanetShaderQuad(size);
        atmosphereEmissiveShader.stop();
    }

    private void renderCrescentShadow(float phase, double uvOffset, double size) {
        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        SkyShader crescentShader = SkyShaders.CRESCENT;
        if (!crescentShader.use()) return;
        crescentShader.setUniform1f("phase", phase);
        crescentShader.setUniform1f("offset", (float) uvOffset);
        crescentShader.setUniform1i("bodyTex", 0);
        crescentShader.setUniform1i("useBodyAlphaMask", 0);
        drawPlanetShaderQuad(size);
        crescentShader.stop();
    }

    private void renderNightLights(CelestialBody body, float phase, double uvOffset, double size, int lightIntensity, int activeBlackouts, float atmosphereDensity, double patternOffset, float atmosphereTime, int atmosphereStyle, float impactTime, List<SkyHooks.ShockStatus> nukeShocks, double currentShockTime) {
        if (lightIntensity <= 0) {
            return;
        }

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        SkyShader nightLightsShader = SkyShaders.NIGHT_LIGHTS;
        SkyGL.bind(citylights[lightIntensity]);
        SkyGL.bindUnit(1, body.cityMask != null ? body.cityMask : defaultMask);
        if (!nightLightsShader.use()) return;
        nightLightsShader.setUniform1f("phase", phase);
        nightLightsShader.setUniform1f("offset", (float) uvOffset);
        nightLightsShader.setUniform1f("atmosphereDensity", atmosphereDensity);
        nightLightsShader.setUniform1f("patternOffset", (float) patternOffset);
        nightLightsShader.setUniform1f("atmosphereTime", atmosphereTime);
        nightLightsShader.setUniform1i("atmosphereStyle", atmosphereStyle);
        nightLightsShader.setUniform1f("impactTime", impactTime);
        AtmosphereRenderUtil.applyNukeShockUniforms(nightLightsShader, nukeShocks, currentShockTime);
        nightLightsShader.setUniform1i("bodyTex", 0);
        nightLightsShader.setUniform1i("lights", 0);
        nightLightsShader.setUniform1i("cityMask", 1);
        nightLightsShader.setUniform1i("blackouts", activeBlackouts);
        nightLightsShader.setUniform1i("useBodyAlphaMask", 0);

        drawPlanetShaderQuad(size);
        nightLightsShader.stop();
    }

    private void renderAtmosphereGlow(CelestialBody body, double size, float visibility, double phase) {
        float glowAlpha = AtmosphereRenderUtil.getAtmosphereGlowAlpha(body) * visibility;
        if (glowAlpha <= 0.001F) {
            return;
        }

        // until I can figure out how to make a four way quad lerp not look like shit I'm disabling this
        float leadingGlow = glowAlpha;// * MathHelper.clamp_float((float)Math.abs(0.5 - phase) * 2, 0, 1);
        float trailingGlow = glowAlpha;// * MathHelper.clamp_float((float)Math.abs(-0.5 - phase) * 2, 0, 1);

        Vec3 atmo = AtmosphereRenderUtil.getBodyAtmosphereColor(body);
        float r = Mth.clamp((float) atmo.x * 1.15F, 0.0F, 1.0F);
        float g = Mth.clamp((float) atmo.y * 1.15F, 0.0F, 1.0F);
        float b = Mth.clamp((float) atmo.z * 1.15F, 0.0F, 1.0F);

        // non-linear gradient stepping
        double innerSize = size * 0.98D;
        double middleSize = size * 1.075D;
        double outerSize = size * 1.15D * (1.0D + glowAlpha * 0.25D);

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);
        SkyGL.disableTexture();
        SkyGL.disableCull();

        SkyTess.startDrawingQuads();

        // Top band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(-outerSize, 100.0D, -outerSize);
        SkyTess.addVertex(outerSize, 100.0D, -outerSize);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2);
        SkyTess.addVertex(middleSize, 100.0D, -middleSize);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2);
        SkyTess.addVertex(-middleSize, 100.0D, -middleSize);

        SkyTess.addVertex(-middleSize, 100.0D, -middleSize);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2);
        SkyTess.addVertex(middleSize, 100.0D, -middleSize);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow);
        SkyTess.addVertex(innerSize, 100.0D, -innerSize);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow);
        SkyTess.addVertex(-innerSize, 100.0D, -innerSize);

        // Left band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(outerSize, 100.0D, -outerSize);
        SkyTess.addVertex(outerSize, 100.0D, outerSize);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2);
        SkyTess.addVertex(middleSize, 100.0D, middleSize);
        SkyTess.addVertex(middleSize, 100.0D, -middleSize);

        SkyTess.addVertex(middleSize, 100.0D, -middleSize);
        SkyTess.addVertex(middleSize, 100.0D, middleSize);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow);
        SkyTess.addVertex(innerSize, 100.0D, innerSize);
        SkyTess.addVertex(innerSize, 100.0D, -innerSize);

        // Bottom band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(outerSize, 100.0D, outerSize);
        SkyTess.addVertex(-outerSize, 100.0D, outerSize);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2);
        SkyTess.addVertex(-middleSize, 100.0D, middleSize);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow / 2);
        SkyTess.addVertex(middleSize, 100.0D, middleSize);

        SkyTess.addVertex(middleSize, 100.0D, middleSize);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2);
        SkyTess.addVertex(-middleSize, 100.0D, middleSize);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow);
        SkyTess.addVertex(-innerSize, 100.0D, innerSize);
        SkyTess.setColorRGBA_F(r, g, b, leadingGlow);
        SkyTess.addVertex(innerSize, 100.0D, innerSize);

        // Right band
        SkyTess.setColorRGBA_F(r, g, b, 0.0F);
        SkyTess.addVertex(-outerSize, 100.0D, outerSize);
        SkyTess.addVertex(-outerSize, 100.0D, -outerSize);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow / 2);
        SkyTess.addVertex(-middleSize, 100.0D, -middleSize);
        SkyTess.addVertex(-middleSize, 100.0D, middleSize);

        SkyTess.addVertex(-middleSize, 100.0D, middleSize);
        SkyTess.addVertex(-middleSize, 100.0D, -middleSize);
        SkyTess.setColorRGBA_F(r, g, b, trailingGlow);
        SkyTess.addVertex(-innerSize, 100.0D, -innerSize);
        SkyTess.addVertex(-innerSize, 100.0D, innerSize);

        SkyTess.draw();

        SkyGL.enableCull();
        SkyGL.enableTexture();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO);
    }

    protected void renderRings(float partialTicks, ClientLevel world, Minecraft mc, float ringTilt, float[] ringColor, float ringSize, float visibility) {
        SkyGL.color(ringColor[0], ringColor[1], ringColor[2], visibility);
        SkyGL.bind(ringTexture);

        double offset = -20.0D;

        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(offset, -ringSize, -ringSize, 0.0D, 0.0D);
        SkyTess.addVertexWithUV(offset, ringSize, -ringSize, 1.0D, 0.0D);
        SkyTess.addVertexWithUV(offset, ringSize, ringSize, 1.0D, 1.0D);
        SkyTess.addVertexWithUV(offset, -ringSize, ringSize, 0.0D, 1.0D);
        SkyTess.draw();
    }

    // is just drawing a big cube with UVs prepared to draw a gradient
    private void renderSkyboxSide(int side) {
        double u = side % 3 / 3.0D;
        double v = side / 3 / 2.0D;
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(-100.0D, -100.0D, -100.0D, u, v);
        SkyTess.addVertexWithUV(-100.0D, -100.0D, 100.0D, u, v + 0.5D);
        SkyTess.addVertexWithUV(100.0D, -100.0D, 100.0D, u + 0.3333333333333333D, v + 0.5D);
        SkyTess.addVertexWithUV(100.0D, -100.0D, -100.0D, u + 0.3333333333333333D, v);
        SkyTess.draw();
    }

    protected void renderSpecialEffects(float partialTicks, ClientLevel world, Minecraft mc) {

    }

    protected void render3DModel(float partialTicks, ClientLevel world, Minecraft mc) {

    }

    protected void renderStation(float partialTicks, ClientLevel world, Minecraft mc, SkyHooks.StationPos station, float solarAngle) {
        long seed = station.dX() * 1024L + station.dZ();

        double ticks = (double) (System.currentTimeMillis() % (1600 * 50)) / 50;

        SkyGL.push();
        {

            SkyGL.rotate(solarAngle * -360.0F, 1.0F, 0.0F, 0.0F);
            SkyGL.rotate(-40.0F + (float) (seed % 800) * 0.1F - 5.0F, 1.0F, 0.0F, 0.0F);
            SkyGL.rotate((float) (seed % 50) * 0.1F - 20.0F, 0.0F, 1.0F, 0.0F);
            SkyGL.rotate((float) (seed % 80) * 0.1F - 2.5F, 0.0F, 0.0F, 1.0F);
            SkyGL.rotate((ticks / 1600.0D) * -360.0D, 1.0F, 0.0F, 0.0F);

            SkyGL.color(0.8F, 1, 1, 1);

            SkyGL.bind(stationTexture);

            float size = 0.8F;

            SkyTess.startDrawingQuads();
            SkyTess.addVertexWithUV(-size, 100.0, -size, 0.0D, 0.0D);
            SkyTess.addVertexWithUV(size, 100.0, -size, 0.0D, 1.0D);
            SkyTess.addVertexWithUV(size, 100.0, size, 1.0D, 1.0D);
            SkyTess.addVertexWithUV(-size, 100.0, size, 1.0D, 0.0D);
            SkyTess.draw();

        }
        SkyGL.pop();
    }

    /** {@code GL11.glRotatef(180 - RenderManager.playerViewY, 0,1,0); glRotatef(-playerViewX, 1,0,0)}: Billboard. */
    protected void billboard() {
        SkyGL.rotate(180.0F - camera.getYRot(), 0.0F, 1.0F, 0.0F);
        SkyGL.rotate(-camera.getXRot(), 1.0F, 0.0F, 0.0F);
    }

    public void renderSmoke(long age) {
        SkyGL.push();
        SkyGL.enableBlend();

        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;
        float dark = 1f - Math.min(((float) (age) / (100f * 0.35F)), 1f);

        billboard();
        SkyGL.color(0.6F * dark + 0.0F, 0.6F * dark + 0.0F, dark + 0.0F, 1F);

        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(0.0F - f5, 0.0F - f6, 0.0D, 1, 0);
        SkyTess.addVertexWithUV(f4 - f5, 0.0F - f6, 0.0D, 0, 0);
        SkyTess.addVertexWithUV(f4 - f5, f4 - f6, 0.0D, 0, 1);
        SkyTess.addVertexWithUV(0.0F - f5, f4 - f6, 0.0D, 1, 1);
        SkyTess.draw();

        SkyGL.disableBlend();
        SkyGL.pop();
    }

    public void renderGlow(double x, double y, double z, float rainStrength) {
        SkyGL.push();
        SkyGL.enableBlend();

        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;
        double near = 0.51d * (Math.min(40000f, Math.max(0d, y - 35000d)) / 40000d);
        double entry = near * (1d - rainStrength) + (1d - (Math.min(200d, Math.max(0d, x - 2017d)) / 200f));

        billboard();
        SkyGL.color(entry, entry, entry, entry);

        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(0.0F - f5, 0.0F - f6, 0.0D, 1, 0);
        SkyTess.addVertexWithUV(f4 - f5, 0.0F - f6, 0.0D, 0, 0);
        SkyTess.addVertexWithUV(f4 - f5, f4 - f6, 0.0D, 0, 1);
        SkyTess.addVertexWithUV(0.0F - f5, f4 - f6, 0.0D, 1, 1);
        SkyTess.draw();

        SkyGL.disableBlend();
        SkyGL.pop();
    }
}
