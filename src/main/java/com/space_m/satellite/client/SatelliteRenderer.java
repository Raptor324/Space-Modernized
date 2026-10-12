package com.space_m.satellite.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.client.sky.SkyBeamPronter;
import com.space_m.client.sky.SkyGL;
import com.space_m.client.sky.SkyObjModel;
import com.space_m.client.sky.SkyTess;
import com.space_m.satellite.ClientSatellite;
import com.space_m.satellite.SatelliteOrbit;
import com.space_m.satellite.SatelliteRailgun;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Satelliten am Himmel, 1:1 NTM Space {@code SatelliteBase.render/renderDefault/renderOrbitLine} und
 * {@code SatelliteRailgun.render}, ueber den GL-Ersatz des Himmels ({@link SkyGL}/{@link SkyTess}).
 * Original-Klassen: SatelliteBase (Client), SatelliteRailgun.render
 */
public final class SatelliteRenderer {

    private SatelliteRenderer() {}

    private static final ResourceLocation SATELLITE_TEXTURE = SpaceMod.id("textures/misc/space/satellite.png");
    // SatelliteRailgun: RefStrings.MODID + ":textures/particle/shockwave.png" (in HBM-Modernized) und misc/space/flare.png
    private static final ResourceLocation SHOCKWAVE = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/particle/shockwave.png");
    private static final ResourceLocation FLASH = SpaceMod.id("textures/misc/space/flare.png");

    /** Original {@code satellite.render(partialTicks, world, mc, solarAngle, id)}. */
    public static void render(ClientSatellite sat, float partialTicks, ClientLevel world, float solarAngle) {
        if (sat.satellite instanceof SatelliteRailgun railgun) {
            renderRailgun(railgun, partialTicks, world, solarAngle, sat.freq);
            return;
        }
        SatelliteOrbit o = sat.orbit;
        renderDefault(partialTicks, solarAngle, sat.freq, o.colorR, o.colorG, o.colorB, o.inclination, o.altitude, o.phaseOffset, o.isBlinking, o.blinkPeriod);
    }

    public static void renderDefault(float partialTicks, float solarAngle, long seed, float r, float g, float b, float inclination, float altitude, boolean isBlinking, float blinkPeriod) {
        renderDefault(partialTicks, solarAngle, seed, r, g, b, inclination, altitude, SatelliteOrbit.DEFAULT_PHASE_OFFSET, isBlinking, blinkPeriod);
    }

    public static void renderDefault(float partialTicks, float solarAngle, long seed, float r, float g, float b, float inclination, float altitude, float phaseOffset, boolean isBlinking, float blinkPeriod) {
        double ticks = (double) System.currentTimeMillis() / 50.0D;
        float orbitAngle = SatelliteOrbit.applyPhaseOffsetToOrbitAngle(phaseOffset, altitude, (ticks / 600.0D) * -360.0D, 360.0F);
        float renderAltitude = Math.max(1.0F, altitude);

        SkyGL.push();
        {

            SkyGL.rotate(solarAngle * -360.0F, 1.0F, 0.0F, 0.0F);
            SkyGL.rotate(inclination, 0.0F, 0.0F, 1.0F);
            SkyGL.rotate(orbitAngle, 1.0F, 0.0F, 0.0F);

            SkyGL.color(r, g, b, SatelliteOrbit.getBlinkAlpha(isBlinking, blinkPeriod));

            SkyGL.bind(SATELLITE_TEXTURE);

            float size = 0.5F;

            SkyTess.startDrawingQuads();
            SkyTess.addVertexWithUV(-size, renderAltitude, -size, 0.0D, 0.0D);
            SkyTess.addVertexWithUV(size, renderAltitude, -size, 0.0D, 1.0D);
            SkyTess.addVertexWithUV(size, renderAltitude, size, 1.0D, 1.0D);
            SkyTess.addVertexWithUV(-size, renderAltitude, size, 1.0D, 0.0D);
            SkyTess.draw();

        }
        SkyGL.pop();
    }

    /** Original {@code renderOrbitLine} (GL_LINE_LOOP -> Linienzug mit geschlossenem Ende). */
    public static void renderOrbitLine(float solarAngle, float r, float g, float b, float inclination, float altitude, boolean isBlinking, float blinkPeriod) {
        float renderAltitude = Math.max(1.0F, altitude);
        float alpha = 0.35F * SatelliteOrbit.getBlinkAlpha(isBlinking, blinkPeriod);

        boolean tex = SkyGL.texture2D;
        SkyGL.disableTexture();
        SkyGL.enableBlend();
        RenderSystem.lineWidth(1.0F);
        SkyGL.color(r, g, b, alpha);

        SkyGL.push();
        {
            SkyGL.rotate(solarAngle * -360.0F, 1.0F, 0.0F, 0.0F);
            SkyGL.rotate(inclination, 0.0F, 0.0F, 1.0F);

            SkyTess.startDrawing(SkyTess.LINE_STRIP);
            for (int i = 0; i <= 72; i++) {
                double angle = Math.PI * 2.0D * (i % 72) / 72.0D;
                SkyTess.addVertex(0.0D, renderAltitude * Math.cos(angle), renderAltitude * Math.sin(angle));
            }
            SkyTess.draw();
        }
        SkyGL.pop();

        // glPopAttrib
        if (tex) SkyGL.enableTexture();
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Original {@code SatelliteRailgun.render}: Railkanone mit Muendungsblitz und Strahl, Lage aus der Frequenz. */
    public static void renderRailgun(SatelliteRailgun sat, float partialTicks, ClientLevel world, float solarAngle, long id) {
        SkyGL.push();
        {

            SkyGL.scale(5, 5, 5);

            double rounded = Math.round(id / 1000.0);
            double x = ((id % 10) - 5) * 2;
            double y = (((id / 10) % 10) - 5) * 2;

            double xPos = Math.min(Math.max(-rounded + 30 + x, -50), 50);
            double yPos = Math.min(Math.max(-rounded - 20 + y, -50), 50);

            SkyGL.translate(xPos, yPos, 20);
            float fuck = sat.interp;
            float alped = 1.0F - Math.min(1.0F, fuck / 100);

            SkyGL.push();
            {

                SkyGL.color(1, 1, 1, alped);

                SkyGL.translate(1, 5.5, 0);
                SkyGL.scale(fuck * 0.2, fuck * 0.2, fuck * 0.2);
                SkyGL.bind(FLASH);
                SkyObjModel.PLANE.renderAll();

                SkyGL.bind(SHOCKWAVE);
                SkyObjModel.PLANE.renderAll();

            }
            SkyGL.pop();

            SkyGL.push();
            {

                SkyGL.translate(1, 5.5, 0);
                SkyBeamPronter.prontBeam(new Vec3(0, fuck * 2, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, 0, 1, 0F, 6, (float) 1.6 * 1.2F * alped, alped * 0.2F);
                SkyBeamPronter.prontBeam(new Vec3(0, fuck * 2, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, 0, 1, 0F, 6, (float) 0.7 * 0.6F, alped * 0.6F);
                SkyBeamPronter.prontBeam(new Vec3(0, fuck * 2, 0), SkyBeamPronter.EnumWaveType.RANDOM, SkyBeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (world.getGameTime() / 5) % 1000, 35, 0.2F, 6, (float) 0.2 * 0.1F, alped);
                SkyGL.clearDepth();
                SkyGL.enableCull();

            }
            SkyGL.pop();


            SkyGL.enableDepthTest();
            SkyGL.disableBlend();
            SkyGL.rotate(-90, 0, 0, 1);

            SkyGL.color(1, 1, 1, 1);
            SkyGL.bind(SkyObjModel.SAT_RAIL_TEX);
            SkyObjModel.SAT_RAIL.renderAll();

            SkyGL.clearDepth();
            SkyGL.enableBlend();

        }
        SkyGL.pop();
    }
}
