package com.space_m.client.sky;

import java.util.Random;

import org.lwjgl.opengl.GL13;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code com.hbm.dim.WeatherProviderCelestial}: Regen/Schnee in der Farbe des Koerper-Fluids
 * ({@code getWeatherColor/getSnowColor}), nur auf Koerpern mit Wetterzyklus. Die feste Pipeline
 * ({@code GL_COMBINE}: Farbe aus dem Vertex, Alpha aus der Textur; Lichtkarte per {@code setBrightness}) bildet der
 * Rohshader {@code weather} nach. Biom-Abfrage: 1.7.10 {@code canSpawnLightningBolt/getEnableSnow} ->
 * {@code Biome.hasPrecipitation}, Temperatur >= 0,15 -> {@code getPrecipitationAt == RAIN}.
 * Original-Klasse: WeatherProviderCelestial
 */
public class CelestialWeatherRenderer {

    private static final ResourceLocation RAIN_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/rain.png");
    private static final ResourceLocation SNOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/snow.png");

    private final Random random = new Random();
    private float[] rainXCoords;
    private float[] rainYCoords;
    private final SkyRawMesh mesh = new SkyRawMesh();

    /** Original {@code render(partialTicks, world, mc)}; Kamera = {@code camX/Y/Z}, Matrix = RenderSystem-Modelview. */
    public void render(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider provider, LightTexture lightTexture, double camX, double camY, double camZ) {
        float intensity = world.getRainLevel(partialTicks);

        if (intensity <= 0.0F) {
            return;
        }

        if (!provider.hasWeatherCycle(world)) {
            return;
        }

        SkyShader shader = SkyShaders.WEATHER;
        if (!shader.isLoaded()) return;

        lightTexture.turnOnLightLayer();
        initRainCoords();

        int timer = mc.player != null ? mc.player.tickCount : (int) (world.getGameTime() & Integer.MAX_VALUE);
        int playerX = Mth.floor(camX);
        int playerY = Mth.floor(camY);
        int playerZ = Mth.floor(camZ);
        double interpX = camX;
        double interpY = camY;
        double interpZ = camZ;
        int playerHeight = Mth.floor(interpY);
        int renderLayerCount = Minecraft.useFancyGraphics() ? 10 : 5;
        Vec3 rainColor = provider.getWeatherColor(world);
        Vec3 snowColor = provider.getSnowColor(world);

        SkyGL.disableCull();
        SkyGL.enableBlend();
        SkyGL.enableDepthTest();
        SkyGL.blendFunc(770, 771, 1, 0);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        int layer = -1;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int layerZ = playerZ - renderLayerCount; layerZ <= playerZ + renderLayerCount; ++layerZ) {
            for (int layerX = playerX - renderLayerCount; layerX <= playerX + renderLayerCount; ++layerX) {
                int rainCoord = (layerZ - playerZ + 16) * 32 + layerX - playerX + 16;
                float rainCoordX = this.rainXCoords[rainCoord] * 0.5F;
                float rainCoordY = this.rainYCoords[rainCoord] * 0.5F;
                mpos.set(layerX, playerY, layerZ);
                Biome biome = world.getBiome(mpos).value();

                if (!biome.hasPrecipitation()) {
                    continue;
                }

                int precipitationHeight = world.getHeight(Heightmap.Types.MOTION_BLOCKING, layerX, layerZ);
                int minHeight = playerY - renderLayerCount;
                int maxHeight = playerY + renderLayerCount;

                if (minHeight < precipitationHeight) minHeight = precipitationHeight;
                if (maxHeight < precipitationHeight) maxHeight = precipitationHeight;

                int layerY = precipitationHeight;
                if (precipitationHeight < playerHeight) layerY = playerHeight;

                if (minHeight == maxHeight) {
                    continue;
                }

                this.random.setSeed(layerX * layerX * 3121 + layerX * 45238971 ^ layerZ * layerZ * 418711 + layerZ * 13761);
                mpos.set(layerX, minHeight, layerZ);
                boolean renderRain = biome.getPrecipitationAt(mpos) == Biome.Precipitation.RAIN;

                mpos.set(layerX, layerY, layerZ);
                int light = LevelRenderer.getLightColor(world, mpos);

                if (renderRain) {
                    if (layer != 0) {
                        if (layer >= 0) {
                            flush(shader, layer);
                        }

                        layer = 0;
                        mesh.begin();
                    }

                    int rainSeed = layerX * layerX * 3121 + layerX * 45238971 + layerZ * layerZ * 418711 + layerZ * 13761;
                    float rainOffset = ((timer + (rainSeed & 31)) + partialTicks) / 32.0F * (3.0F + this.random.nextFloat());
                    double distX = layerX + 0.5D - camX;
                    double distZ = layerZ + 0.5D - camZ;
                    float intensityMod = (float) Math.sqrt(distX * distX + distZ * distZ) / renderLayerCount;

                    float lu = light & 0xFFFF;
                    float lv = light >> 16 & 0xFFFF;
                    float r = (float) rainColor.x;
                    float g = (float) rainColor.y;
                    float b = (float) rainColor.z;
                    float a = ((1.0F - intensityMod * intensityMod) * 0.5F + 0.5F) * intensity;
                    mesh.vertex(layerX - rainCoordX + 0.5D - interpX, minHeight - interpY, layerZ - rainCoordY + 0.5D - interpZ, 0.0F, minHeight / 4.0F + rainOffset, r, g, b, a, lu, lv);
                    mesh.vertex(layerX + rainCoordX + 0.5D - interpX, minHeight - interpY, layerZ + rainCoordY + 0.5D - interpZ, 1.0F, minHeight / 4.0F + rainOffset, r, g, b, a, lu, lv);
                    mesh.vertex(layerX + rainCoordX + 0.5D - interpX, maxHeight - interpY, layerZ + rainCoordY + 0.5D - interpZ, 1.0F, maxHeight / 4.0F + rainOffset, r, g, b, a, lu, lv);
                    mesh.vertex(layerX - rainCoordX + 0.5D - interpX, maxHeight - interpY, layerZ - rainCoordY + 0.5D - interpZ, 0.0F, maxHeight / 4.0F + rainOffset, r, g, b, a, lu, lv);
                } else {
                    if (layer != 1) {
                        if (layer >= 0) {
                            flush(shader, layer);
                        }

                        layer = 1;
                        mesh.begin();
                    }

                    float swayLoop = ((timer & 511) + partialTicks) / 512.0F;
                    float fallVariation = this.random.nextFloat() + timer * 0.01F * (float) this.random.nextGaussian();
                    float swayVariation = this.random.nextFloat() + timer * (float) this.random.nextGaussian() * 0.001F;
                    double distX = layerX + 0.5D - camX;
                    double distZ = layerZ + 0.5D - camZ;
                    float intensityMod = (float) Math.sqrt(distX * distX + distZ * distZ) / renderLayerCount;

                    int snowLight = (light * 3 + 15728880) / 4;
                    float lu = snowLight & 0xFFFF;
                    float lv = snowLight >> 16 & 0xFFFF;
                    float r = (float) snowColor.x;
                    float g = (float) snowColor.y;
                    float b = (float) snowColor.z;
                    float a = ((1.0F - intensityMod * intensityMod) * 0.3F + 0.5F) * intensity;
                    mesh.vertex(layerX - rainCoordX + 0.5D - interpX, minHeight - interpY, layerZ - rainCoordY + 0.5D - interpZ, 0.0F + fallVariation, minHeight / 4.0F + swayLoop + swayVariation, r, g, b, a, lu, lv);
                    mesh.vertex(layerX + rainCoordX + 0.5D - interpX, minHeight - interpY, layerZ + rainCoordY + 0.5D - interpZ, 1.0F + fallVariation, minHeight / 4.0F + swayLoop + swayVariation, r, g, b, a, lu, lv);
                    mesh.vertex(layerX + rainCoordX + 0.5D - interpX, maxHeight - interpY, layerZ + rainCoordY + 0.5D - interpZ, 1.0F + fallVariation, maxHeight / 4.0F + swayLoop + swayVariation, r, g, b, a, lu, lv);
                    mesh.vertex(layerX - rainCoordX + 0.5D - interpX, maxHeight - interpY, layerZ - rainCoordY + 0.5D - interpZ, 0.0F + fallVariation, maxHeight / 4.0F + swayLoop + swayVariation, r, g, b, a, lu, lv);
                }
            }
        }

        if (layer >= 0) {
            flush(shader, layer);
        }

        SkyGL.enableCull();
        SkyGL.disableBlend();
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);
        lightTexture.turnOffLightLayer();
    }

    /** {@code tessellator.draw()} der aktuellen Schicht (0 = Regen, 1 = Schnee). */
    private void flush(SkyShader shader, int layer) {
        if (mesh.isEmpty()) return;
        SkyGL.bind(layer == 0 ? RAIN_TEXTURE : SNOW_TEXTURE);
        int lightmap = RenderSystem.getShaderTexture(2);
        RenderSystem.activeTexture(GL13.GL_TEXTURE2);
        RenderSystem.bindTexture(lightmap);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        if (!shader.use()) return;
        shader.setUniform1i("Sampler0", 0);
        shader.setUniform1i("Sampler2", 2);
        shader.setUniform1f("FogStart", RenderSystem.getShaderFogStart());
        shader.setUniform1f("FogEnd", RenderSystem.getShaderFogEnd());
        float[] fog = RenderSystem.getShaderFogColor();
        org.lwjgl.opengl.GL20.glUniform4f(shader.getUniformLocation("FogColor"), fog[0], fog[1], fog[2], fog[3]);
        mesh.draw(shader);
        shader.stop();
    }

    private void initRainCoords() {
        if (this.rainXCoords != null) {
            return;
        }

        this.rainXCoords = new float[1024];
        this.rainYCoords = new float[1024];

        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                float coordX = j - 16;
                float coordY = i - 16;
                float coordLength = Mth.sqrt(coordX * coordX + coordY * coordY);
                this.rainXCoords[i << 5 | j] = -coordY / coordLength;
                this.rainYCoords[i << 5 | j] = coordX / coordLength;
            }
        }
    }
}
