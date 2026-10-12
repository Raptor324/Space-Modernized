package com.space_m.client.orrery;

import java.util.List;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystem.OrreryMetric;
import com.space_m.client.sky.SkyGL;
import com.space_m.client.sky.SkyTess;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code com.hbm.render.util.OrreryPronter}: das Sonnensystem als leuchtende Wuerfel mit Bahnlinien
 * ({@code SolarSystem.calculatePositionsOrrery}, nichtlinear gestaucht). Zeichnung ueber {@link SkyGL}/{@link SkyTess}
 * (Vollhell wie {@code setLightmapTextureCoords(240, 240)}, additiv {@code GL_SRC_ALPHA, GL_ONE}).
 * Original-Klasse: OrreryPronter
 */
// Original-Klasse: RenderUtil (renderBlock)
public final class OrreryPronter {
    private OrreryPronter() {}

    private static long lastTick;
    private static float lastPartial;

    private static List<OrreryMetric> metrics;

    public static void render(Minecraft mc, Level world, float partialTicks) {
        CelestialBody sun = CelestialBody.getStar(world);

        // Update metrics
        if (metrics == null || lastTick != world.getGameTime() || lastPartial != partialTicks) {
            metrics = SolarSystem.calculatePositionsOrrery(world, partialTicks);
            lastTick = world.getGameTime();
            lastPartial = partialTicks;
        }

        // Setup glow
        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);

        SkyGL.push();
        {

            // Draw sun
            SkyGL.push();
            {

                SkyGL.bind(sun.texture);
                renderBlock(0.375, 0.625);

            }
            SkyGL.pop();

            // Scale so sun is 1x1x1
            double scale = 1 / Math.min(sun.radiusKm, SolarSystem.ORRERY_MAX_RADIUS);
            SkyGL.scale(scale, scale, scale);


            // Draw bodies
            for (OrreryMetric metric : metrics) {
                SkyGL.bind(metric.body.texture);

                SkyGL.push();
                {

                    double bodyScale = metric.body.radiusKm;
                    if (bodyScale < 2_000) bodyScale = (bodyScale / 63) * (bodyScale / 63) + 1_000;

                    SkyGL.translate(metric.position.x, metric.position.z, metric.position.y);
                    SkyGL.scale(bodyScale, bodyScale, bodyScale);

                    renderBlock(0, 1);

                }
                SkyGL.pop();

                SkyGL.disableTexture();
                SkyGL.color(metric.body.color[0], metric.body.color[1], metric.body.color[2]);

                SkyTess.startDrawing(SkyTess.LINE_STRIP);

                for (int i = 1; i < metric.orbitalPath.length; i++) {
                    Vec3 from = metric.orbitalPath[i - 1];
                    Vec3 to = metric.orbitalPath[i];

                    SkyTess.addVertex(from.x, from.z, from.y);
                    SkyTess.addVertex(to.x, to.z, to.y);
                }

                Vec3 first = metric.orbitalPath[0];
                Vec3 last = metric.orbitalPath[metric.orbitalPath.length - 1];

                SkyTess.addVertex(last.x, last.z, last.y);
                SkyTess.addVertex(first.x, first.z, first.y);

                SkyTess.draw();

                SkyGL.enableTexture();
                SkyGL.color(1, 1, 1);
            }

        }
        SkyGL.pop();


        // Reset state
        SkyGL.disableBlend();
    }

    /** {@code com.hbm.util.RenderUtil.renderBlock(tessellator, uvMin, uvMax)} mit {@code disableColor} (= glColor). */
    static void renderBlock(double uvMin, double uvMax) {
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(-0.5, +0.5, -0.5, uvMax, uvMax);
        SkyTess.addVertexWithUV(+0.5, +0.5, -0.5, uvMin, uvMax);
        SkyTess.addVertexWithUV(+0.5, -0.5, -0.5, uvMin, uvMin);
        SkyTess.addVertexWithUV(-0.5, -0.5, -0.5, uvMax, uvMin);

        SkyTess.addVertexWithUV(-0.5, +0.5, +0.5, uvMax, uvMax);
        SkyTess.addVertexWithUV(-0.5, +0.5, -0.5, uvMin, uvMax);
        SkyTess.addVertexWithUV(-0.5, -0.5, -0.5, uvMin, uvMin);
        SkyTess.addVertexWithUV(-0.5, -0.5, +0.5, uvMax, uvMin);

        SkyTess.addVertexWithUV(+0.5, +0.5, +0.5, uvMax, uvMax);
        SkyTess.addVertexWithUV(-0.5, +0.5, +0.5, uvMin, uvMax);
        SkyTess.addVertexWithUV(-0.5, -0.5, +0.5, uvMin, uvMin);
        SkyTess.addVertexWithUV(+0.5, -0.5, +0.5, uvMax, uvMin);

        SkyTess.addVertexWithUV(+0.5, +0.5, -0.5, uvMax, uvMax);
        SkyTess.addVertexWithUV(+0.5, +0.5, +0.5, uvMin, uvMax);
        SkyTess.addVertexWithUV(+0.5, -0.5, +0.5, uvMin, uvMin);
        SkyTess.addVertexWithUV(+0.5, -0.5, -0.5, uvMax, uvMin);

        SkyTess.addVertexWithUV(-0.5, -0.5, -0.5, uvMax, uvMax);
        SkyTess.addVertexWithUV(+0.5, -0.5, -0.5, uvMin, uvMax);
        SkyTess.addVertexWithUV(+0.5, -0.5, +0.5, uvMin, uvMin);
        SkyTess.addVertexWithUV(-0.5, -0.5, +0.5, uvMax, uvMin);

        SkyTess.addVertexWithUV(+0.5, +0.5, -0.5, uvMax, uvMax);
        SkyTess.addVertexWithUV(-0.5, +0.5, -0.5, uvMin, uvMax);
        SkyTess.addVertexWithUV(-0.5, +0.5, +0.5, uvMin, uvMin);
        SkyTess.addVertexWithUV(+0.5, +0.5, +0.5, uvMax, uvMin);
        SkyTess.draw();
    }
}
