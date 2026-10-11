package com.space_m.client.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 NTM Space {@code com.hbm.dim.laythe.SkyProviderLaytheSunset}: dreifaches Abendrot auf Laythe mit additiver
 * Mischung ({@code GL_ONE_MINUS_CONSTANT_ALPHA}, Konstante 0 -> 1, "the magic sauce").
 * Original-Klasse: SkyProviderLaytheSunset
 */
public class LaytheSkyRenderer extends CelestialSkyRenderer {

    @Override
    protected void renderSunset(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider provider, float solarAngle, float pressure, ResourceLocation surfaceTexture) {
        float[] sunsetColor = provider.calcSunriseSunsetColors(world, world.getTimeOfDay(partialTicks), partialTicks);

        if (sunsetColor != null) {
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_CONSTANT_ALPHA, SkyGL.GL_ONE, SkyGL.GL_ZERO); // The magic sauce

            float[] anaglyphColor = sunsetColor;
            byte segments = 16;

            SkyGL.disableTexture();

            SkyGL.push();
            {

                SkyGL.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                SkyGL.rotate(Mth.sin(world.getSunAngle(partialTicks)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
                SkyGL.rotate(90.0F, 0.0F, 0.0F, 1.0F);

                SkyTess.startDrawing(SkyTess.TRIANGLE_FAN);
                SkyTess.setColorRGBA_F(anaglyphColor[0], anaglyphColor[1], anaglyphColor[2], sunsetColor[3]);
                SkyTess.addVertex(0.0D, 150.0D, 0.0D);
                SkyTess.setColorRGBA_F(sunsetColor[0], sunsetColor[1], sunsetColor[2], 0.0F);

                for (int j = 0; j <= segments; ++j) {
                    float angle = (float) j * (float) Math.PI * 2.0F / (float) segments;
                    float sinAngle = Mth.sin(angle);
                    float cosAngle = Mth.cos(angle);
                    SkyTess.addVertex((double) (sinAngle * 160.0F), (double) (cosAngle * 160.0F), (double) (-cosAngle * 90.0F * sunsetColor[3]));
                }

                SkyTess.draw();

            }
            SkyGL.pop();

            SkyGL.push();
            {

                SkyGL.rotate(135.0F, 1.0F, 0.0F, 0.0F);
                SkyGL.translate(0, -60, 0);

                SkyTess.startDrawing(SkyTess.TRIANGLE_FAN);
                SkyTess.setColorRGBA_F(anaglyphColor[0], anaglyphColor[1], anaglyphColor[2], sunsetColor[3]);
                SkyTess.addVertex(0.0D, 100.0D, 0.0D);
                SkyTess.setColorRGBA_F(sunsetColor[0], sunsetColor[1], sunsetColor[2], 0.0F);

                for (int j = 0; j <= segments; ++j) {
                    float angle = (float) j * (float) Math.PI * 2.0F / (float) segments;
                    float sinAngle = Mth.sin(angle);
                    float cosAngle = Mth.cos(angle);

                    SkyTess.addVertex((double) (sinAngle * 100.0F), (double) (cosAngle * 100.0F), (double) (-cosAngle * 90.0F));
                }

                SkyTess.draw();

            }
            SkyGL.pop();

            SkyGL.push();
            {

                SkyGL.rotate(135.0F, 1.0F, 0.0F, 0.0F);
                SkyGL.translate(0, -30, 0);

                SkyTess.startDrawing(SkyTess.TRIANGLE_FAN);
                SkyTess.setColorRGBA_F(anaglyphColor[0], anaglyphColor[1], anaglyphColor[2], sunsetColor[3]);
                SkyTess.addVertex(0.0D, 80.0D, 0.0D);
                SkyTess.setColorRGBA_F(sunsetColor[0], sunsetColor[1] * 0.2F, sunsetColor[2], 0.0F);

                for (int j = 0; j <= segments; ++j) {
                    float angle = (float) j * (float) Math.PI * 2.0F / (float) segments;
                    float sinAngle = Mth.sin(angle);
                    float cosAngle = Mth.cos(angle);

                    SkyTess.addVertex((double) (sinAngle * 100.0F), (double) (cosAngle * 100.0F), (double) (-cosAngle * 90.0F));
                }

                SkyTess.draw();

            }
            SkyGL.pop();

            SkyGL.enableTexture();
        }
    }

}
