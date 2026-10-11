package com.space_m.client.sky;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code com.hbm.dim.CloudProviderCelestial}: bis zu drei Wolkenschichten je nach Luftdruck
 * (0,5 / 2,5 / 5 atm), getoent nach der Atmosphaeren-Zusammensetzung, schnell (flach) oder schoen (Quader, 2 Durchgaenge).
 * Kamera = {@code camX/camY/camZ} des Vanilla-Aufrufs. Seed: der 1.7.10-Client kennt den Welt-Seed nicht (0);
 * Dimensionsnummer = {@code CelestialBody.legacyDimensionId}.
 * Original-Klasse: CloudProviderCelestial
 */
public class CelestialCloudRenderer {

    private static final ResourceLocation CLOUDS_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/clouds.png");

    private static final float CLOUD_BASE_ALPHA = 0.8F;
    private static final float CLOUD_HEIGHT_SHIFT = 0.33F;
    private static final float CLOUD_SCROLL_SPEED = 0.03F;

    private static final float FAST_CLOUD_TILE_SIZE = 32.0F;
    private static final int FAST_CLOUD_RADIUS = 256 / (int) FAST_CLOUD_TILE_SIZE;

    private static final float FANCY_CLOUD_SCALE = 12.0F;
    private static final float FANCY_CLOUD_DEPTH = 4.0F;
    private static final int FANCY_CLOUD_TILE_SIZE = 8;
    private static final int FANCY_CLOUD_RADIUS = 4;
    private static final double LAYER_POSITION_RANGE = 16384.0D;

    private static final float[] LAYER_HEIGHT_OFFSETS = {0.0F, 12.0F, 24.0F};
    private static final float[] LAYER_ALPHA_MULTIPLIERS = {1.0F, 0.84F, 0.76F};
    private static final double[] LAYER_SCROLL_MULTIPLIERS = {1.0D, 0.84D, 0.7D};

    /** Original {@code render(partialTicks, world, mc)}; Matrix/Zustand ({@link SkyGL}) vom Aufrufer begonnen. */
    public void render(float partialTicks, ClientLevel world, Minecraft mc, CelestialSkyProvider provider, float vanillaCloudHeight, double camX, double camY, double camZ) {
        int layerCount = provider.getCloudLayerCount(world);
        if (layerCount <= 0) {
            return;
        }

        float baseCloudHeight = provider.getCloudHeight(world, vanillaCloudHeight);
        if (baseCloudHeight < -9000.0F) {
            return;
        }

        CBT_Atmosphere atmosphere = CelestialBody.getTrait(world, CBT_Atmosphere.class);
        Vec3 cloudColor = getTintedCloudColor(atmosphere, world.getCloudColor(partialTicks));
        if (cloudColor == null) {
            cloudColor = new Vec3(1.0D, 1.0D, 1.0D);
        }

        for (int layerIndex = 0; layerIndex < layerCount && layerIndex < LAYER_HEIGHT_OFFSETS.length; layerIndex++) {
            float layerHeight = baseCloudHeight + LAYER_HEIGHT_OFFSETS[layerIndex];
            float layerAlpha = CLOUD_BASE_ALPHA * LAYER_ALPHA_MULTIPLIERS[layerIndex];

            if (mc.options.getCloudsType() == CloudStatus.FANCY) {
                renderFancyLayer(partialTicks, world, mc, cloudColor, layerHeight, layerIndex, layerAlpha, camX, camY, camZ);
            } else {
                renderFlatLayer(partialTicks, world, mc, cloudColor, layerHeight, layerIndex, layerAlpha, camX, camY, camZ);
            }
        }
    }

    private void renderFlatLayer(float partialTicks, ClientLevel world, Minecraft mc, Vec3 cloudColor, float cloudHeight, int layerIndex, float alpha, double camX, double camY, double camZ) {
        SkyGL.disableCull();
        SkyGL.bind(CLOUDS_TEXTURE);
        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        float cameraY = (float) camY;
        float layerY = cloudHeight - cameraY + CLOUD_HEIGHT_SHIFT;

        float cloudRed = (float) cloudColor.x;
        float cloudGreen = (float) cloudColor.y;
        float cloudBlue = (float) cloudColor.z;

        double cloudTime = getCloudTime(world, mc, partialTicks);
        double layerOffsetX = getLayerPositionOffset(world, layerIndex, 0x4F1BBCDCBFA54001L);
        double layerOffsetZ = getLayerPositionOffset(world, layerIndex, 0x2C1B3C6D5E7F8103L);
        double motionX = camX
            + cloudTime * CLOUD_SCROLL_SPEED * LAYER_SCROLL_MULTIPLIERS[layerIndex]
            + layerOffsetX;
        double motionZ = camZ
            + layerOffsetZ;

        int wrapX = Mth.floor(motionX / 2048.0D);
        int wrapZ = Mth.floor(motionZ / 2048.0D);
        motionX -= wrapX * 2048.0D;
        motionZ -= wrapZ * 2048.0D;

        float uvOffsetX = (float) (motionX * 4.8828125E-4F);
        float uvOffsetZ = (float) (motionZ * 4.8828125E-4F);

        SkyTess.startDrawingQuads();
        SkyTess.setColorRGBA_F(cloudRed, cloudGreen, cloudBlue, alpha);

        for (int tileX = -FAST_CLOUD_RADIUS; tileX < FAST_CLOUD_RADIUS; tileX++) {
            for (int tileZ = -FAST_CLOUD_RADIUS; tileZ < FAST_CLOUD_RADIUS; tileZ++) {
                float minX = tileX * FAST_CLOUD_TILE_SIZE;
                float minZ = tileZ * FAST_CLOUD_TILE_SIZE;
                float maxX = minX + FAST_CLOUD_TILE_SIZE;
                float maxZ = minZ + FAST_CLOUD_TILE_SIZE;

                SkyTess.addVertexWithUV(minX, layerY, maxZ, minX * 4.8828125E-4F + uvOffsetX, maxZ * 4.8828125E-4F + uvOffsetZ);
                SkyTess.addVertexWithUV(maxX, layerY, maxZ, maxX * 4.8828125E-4F + uvOffsetX, maxZ * 4.8828125E-4F + uvOffsetZ);
                SkyTess.addVertexWithUV(maxX, layerY, minZ, maxX * 4.8828125E-4F + uvOffsetX, minZ * 4.8828125E-4F + uvOffsetZ);
                SkyTess.addVertexWithUV(minX, layerY, minZ, minX * 4.8828125E-4F + uvOffsetX, minZ * 4.8828125E-4F + uvOffsetZ);
            }
        }

        SkyTess.draw();
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);
        SkyGL.disableBlend();
        SkyGL.enableCull();
    }

    private void renderFancyLayer(float partialTicks, ClientLevel world, Minecraft mc, Vec3 cloudColor, float cloudHeight, int layerIndex, float alpha, double camX, double camY, double camZ) {
        SkyGL.push();
        SkyGL.disableCull();
        SkyGL.bind(CLOUDS_TEXTURE);
        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        double cloudTime = getCloudTime(world, mc, partialTicks);
        double layerOffsetX = getLayerPositionOffset(world, layerIndex, 0x4F1BBCDCBFA54001L);
        double layerOffsetZ = getLayerPositionOffset(world, layerIndex, 0x2C1B3C6D5E7F8103L);
        double motionX = (camX
            + cloudTime * CLOUD_SCROLL_SPEED * LAYER_SCROLL_MULTIPLIERS[layerIndex]
            + layerOffsetX) / FANCY_CLOUD_SCALE;
        double motionZ = (camZ
            + layerOffsetZ) / FANCY_CLOUD_SCALE + 0.33000001311302185D;
        float cameraY = (float) camY;
        float relativeCloudY = cloudHeight - cameraY + CLOUD_HEIGHT_SHIFT;

        int wrapX = Mth.floor(motionX / 2048.0D);
        int wrapZ = Mth.floor(motionZ / 2048.0D);
        motionX -= wrapX * 2048.0D;
        motionZ -= wrapZ * 2048.0D;

        float cloudRed = (float) cloudColor.x;
        float cloudGreen = (float) cloudColor.y;
        float cloudBlue = (float) cloudColor.z;

        float cloudUBase = Mth.floor(motionX) * 0.00390625F;
        float cloudVBase = Mth.floor(motionZ) * 0.00390625F;
        float cloudXOffset = (float) (motionX - Mth.floor(motionX));
        float cloudZOffset = (float) (motionZ - Mth.floor(motionZ));

        SkyGL.scale(FANCY_CLOUD_SCALE, 1.0F, FANCY_CLOUD_SCALE);

        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) {
                SkyGL.colorMask(false, false, false, false);
            } else {
                SkyGL.colorMask(true, true, true, true);
            }

            for (int tileX = -FANCY_CLOUD_RADIUS + 1; tileX <= FANCY_CLOUD_RADIUS; tileX++) {
                for (int tileZ = -FANCY_CLOUD_RADIUS + 1; tileZ <= FANCY_CLOUD_RADIUS; tileZ++) {
                    SkyTess.startDrawingQuads();

                    float minCloudX = tileX * FANCY_CLOUD_TILE_SIZE;
                    float minCloudZ = tileZ * FANCY_CLOUD_TILE_SIZE;
                    float cloudX = minCloudX - cloudXOffset;
                    float cloudZ = minCloudZ - cloudZOffset;

                    if (relativeCloudY > -FANCY_CLOUD_DEPTH - 1.0F) {
                        SkyTess.setColorRGBA_F(cloudRed * 0.7F, cloudGreen * 0.7F, cloudBlue * 0.7F, alpha);
                        addHorizontalFace(cloudX, relativeCloudY, cloudZ, minCloudX, minCloudZ, cloudUBase, cloudVBase, false);
                    }

                    if (relativeCloudY <= FANCY_CLOUD_DEPTH + 1.0F) {
                        SkyTess.setColorRGBA_F(cloudRed, cloudGreen, cloudBlue, alpha);
                        addHorizontalFace(cloudX, relativeCloudY + FANCY_CLOUD_DEPTH - 9.765625E-4F, cloudZ, minCloudX, minCloudZ, cloudUBase, cloudVBase, true);
                    }

                    SkyTess.setColorRGBA_F(cloudRed * 0.9F, cloudGreen * 0.9F, cloudBlue * 0.9F, alpha);
                    if (tileX > -1) {
                        addVerticalXFaces(cloudX, relativeCloudY, cloudZ, minCloudX, minCloudZ, cloudUBase, cloudVBase, 0.0F);
                    }
                    if (tileX <= 1) {
                        addVerticalXFaces(cloudX, relativeCloudY, cloudZ, minCloudX, minCloudZ, cloudUBase, cloudVBase, 1.0F - 9.765625E-4F);
                    }

                    SkyTess.setColorRGBA_F(cloudRed * 0.8F, cloudGreen * 0.8F, cloudBlue * 0.8F, alpha);
                    if (tileZ > -1) {
                        addVerticalZFaces(cloudX, relativeCloudY, cloudZ, minCloudX, minCloudZ, cloudUBase, cloudVBase, 0.0F);
                    }
                    if (tileZ <= 1) {
                        addVerticalZFaces(cloudX, relativeCloudY, cloudZ, minCloudX, minCloudZ, cloudUBase, cloudVBase, 1.0F - 9.765625E-4F);
                    }

                    SkyTess.draw();
                }
            }
        }

        SkyGL.colorMask(true, true, true, true);
        SkyGL.color(1.0F, 1.0F, 1.0F, 1.0F);
        SkyGL.disableBlend();
        SkyGL.enableCull();
        SkyGL.pop();
    }

    private void addHorizontalFace(float cloudX, float cloudY, float cloudZ, float minCloudX, float minCloudZ, float uvBaseX, float uvBaseZ, boolean topFace) {
        float maxCloudCoord = FANCY_CLOUD_TILE_SIZE;
        float worldMinZ = topFace ? cloudZ : cloudZ + maxCloudCoord;
        float worldMaxZ = topFace ? cloudZ + maxCloudCoord : cloudZ;
        float uvMinZ = topFace ? minCloudZ : minCloudZ + maxCloudCoord;
        float uvMaxZ = topFace ? minCloudZ + maxCloudCoord : minCloudZ;

        SkyTess.addVertexWithUV(cloudX + 0.0F, cloudY, worldMinZ, (minCloudX + 0.0F) * 0.00390625F + uvBaseX, uvMinZ * 0.00390625F + uvBaseZ);
        SkyTess.addVertexWithUV(cloudX + maxCloudCoord, cloudY, worldMinZ, (minCloudX + maxCloudCoord) * 0.00390625F + uvBaseX, uvMinZ * 0.00390625F + uvBaseZ);
        SkyTess.addVertexWithUV(cloudX + maxCloudCoord, cloudY, worldMaxZ, (minCloudX + maxCloudCoord) * 0.00390625F + uvBaseX, uvMaxZ * 0.00390625F + uvBaseZ);
        SkyTess.addVertexWithUV(cloudX + 0.0F, cloudY, worldMaxZ, (minCloudX + 0.0F) * 0.00390625F + uvBaseX, uvMaxZ * 0.00390625F + uvBaseZ);
    }

    private void addVerticalXFaces(float cloudX, float cloudY, float cloudZ, float minCloudX, float minCloudZ, float uvBaseX, float uvBaseZ, float xOffset) {
        for (int face = 0; face < FANCY_CLOUD_TILE_SIZE; face++) {
            float faceX = cloudX + face + xOffset;
            float uvX = (minCloudX + face + 0.5F) * 0.00390625F + uvBaseX;

            SkyTess.addVertexWithUV(faceX, cloudY + 0.0F, cloudZ + FANCY_CLOUD_TILE_SIZE, uvX, (minCloudZ + FANCY_CLOUD_TILE_SIZE) * 0.00390625F + uvBaseZ);
            SkyTess.addVertexWithUV(faceX, cloudY + FANCY_CLOUD_DEPTH, cloudZ + FANCY_CLOUD_TILE_SIZE, uvX, (minCloudZ + FANCY_CLOUD_TILE_SIZE) * 0.00390625F + uvBaseZ);
            SkyTess.addVertexWithUV(faceX, cloudY + FANCY_CLOUD_DEPTH, cloudZ + 0.0F, uvX, (minCloudZ + 0.0F) * 0.00390625F + uvBaseZ);
            SkyTess.addVertexWithUV(faceX, cloudY + 0.0F, cloudZ + 0.0F, uvX, (minCloudZ + 0.0F) * 0.00390625F + uvBaseZ);
        }
    }

    private void addVerticalZFaces(float cloudX, float cloudY, float cloudZ, float minCloudX, float minCloudZ, float uvBaseX, float uvBaseZ, float zOffset) {
        for (int face = 0; face < FANCY_CLOUD_TILE_SIZE; face++) {
            float faceZ = cloudZ + face + zOffset;
            float uvZ = (minCloudZ + face + 0.5F) * 0.00390625F + uvBaseZ;

            SkyTess.addVertexWithUV(cloudX + 0.0F, cloudY + FANCY_CLOUD_DEPTH, faceZ, (minCloudX + 0.0F) * 0.00390625F + uvBaseX, uvZ);
            SkyTess.addVertexWithUV(cloudX + FANCY_CLOUD_TILE_SIZE, cloudY + FANCY_CLOUD_DEPTH, faceZ, (minCloudX + FANCY_CLOUD_TILE_SIZE) * 0.00390625F + uvBaseX, uvZ);
            SkyTess.addVertexWithUV(cloudX + FANCY_CLOUD_TILE_SIZE, cloudY + 0.0F, faceZ, (minCloudX + FANCY_CLOUD_TILE_SIZE) * 0.00390625F + uvBaseX, uvZ);
            SkyTess.addVertexWithUV(cloudX + 0.0F, cloudY + 0.0F, faceZ, (minCloudX + 0.0F) * 0.00390625F + uvBaseX, uvZ);
        }
    }

    private double getCloudTime(ClientLevel world, Minecraft mc, float partialTicks) {
        int tickCounter = mc.player != null ? mc.player.tickCount : (int) (world.getGameTime() & Integer.MAX_VALUE);
        return tickCounter + partialTicks;
    }

    private double getLayerPositionOffset(ClientLevel world, int layerIndex, long salt) {
        double normalized = getLayerRandom(world, layerIndex, salt);
        return (normalized - 0.5D) * LAYER_POSITION_RANGE;
    }

    private double getLayerRandom(ClientLevel world, int layerIndex, long salt) {
        // 1.7.10-Client: WorldInfo vom Server ohne Seed -> 0
        long seed = 0L;
        long dimension = CelestialBody.getBody(world).legacyDimensionId;
        long mixed = mixSeed(seed ^ (dimension * 0x9E3779B97F4A7C15L) ^ ((long) (layerIndex + 1) * 0xC2B2AE3D27D4EB4FL) ^ salt);
        return (double) (mixed & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    private long mixSeed(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static boolean isNeutralCloudFluid(ResourceLocation fluid) {
        return fluid.equals(CelestialFluids.EARTHAIR)
            || fluid.equals(CelestialFluids.OXYGEN)
            || fluid.equals(CelestialFluids.NITROGEN)
            || fluid.equals(CelestialFluids.DUNAAIR)
            || fluid.equals(SkyFluids.CARBONDIOXIDE);
    }

    private static double[] getCloudTintData(CBT_Atmosphere atmosphere) {
        if (atmosphere == null || atmosphere.fluids.isEmpty()) {
            return null;
        }

        double totalPressure = 0.0D;
        double tintPressure = 0.0D;
        double tintR = 0.0D;
        double tintG = 0.0D;
        double tintB = 0.0D;

        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            FluidEntry entry = atmosphere.fluids.get(i);
            if (entry == null || entry.fluid == null || entry.pressure <= 0.0D) {
                continue;
            }

            totalPressure += entry.pressure;

            if (isNeutralCloudFluid(entry.fluid)) {
                continue;
            }

            Vec3 fluidColor = AtmosphereRenderUtil.getAtmosphereFluidColor(entry.fluid);
            tintPressure += entry.pressure;
            tintR += fluidColor.x * entry.pressure;
            tintG += fluidColor.y * entry.pressure;
            tintB += fluidColor.z * entry.pressure;
        }

        if (totalPressure <= 0.0D || tintPressure <= 0.0D) {
            return null;
        }

        tintR /= tintPressure;
        tintG /= tintPressure;
        tintB /= tintPressure;

        double tintPeak = Math.max(tintR, Math.max(tintG, tintB));
        if (tintPeak <= 0.0D) {
            return null;
        }

        double tintStrength = Mth.clamp(tintPressure / totalPressure, 0.0D, 0.65D);
        return new double[] {
            tintStrength,
            Mth.clamp(tintR / tintPeak, 0.0D, 1.0D),
            Mth.clamp(tintG / tintPeak, 0.0D, 1.0D),
            Mth.clamp(tintB / tintPeak, 0.0D, 1.0D)
        };
    }

    public static float getCloudTintStrength(CBT_Atmosphere atmosphere) {
        double[] tintData = getCloudTintData(atmosphere);
        return tintData != null ? (float) tintData[0] : 0.0F;
    }

    public static Vec3 getTintedCloudColor(CBT_Atmosphere atmosphere, Vec3 clouds) {
        if (clouds == null) {
            clouds = new Vec3(1.0D, 1.0D, 1.0D);
        }

        double[] tintData = getCloudTintData(atmosphere);
        if (tintData == null) {
            return clouds;
        }

        double cloudLuma = clouds.x * 0.299D + clouds.y * 0.587D + clouds.z * 0.114D;
        double cloudBrightness = Mth.clamp(cloudLuma * 0.9D, 0.0D, 1.0D);
        double tintStrength = tintData[0];
        double cloudColorR = Mth.clamp(tintData[1] * cloudBrightness, 0.0D, 1.0D);
        double cloudColorG = Mth.clamp(tintData[2] * cloudBrightness, 0.0D, 1.0D);
        double cloudColorB = Mth.clamp(tintData[3] * cloudBrightness, 0.0D, 1.0D);

        return new Vec3(
            Mth.clamp(clouds.x + (cloudColorR - clouds.x) * tintStrength, 0.0D, 1.0D),
            Mth.clamp(clouds.y + (cloudColorG - clouds.y) * tintStrength, 0.0D, 1.0D),
            Mth.clamp(clouds.z + (cloudColorB - clouds.z) * tintStrength, 0.0D, 1.0D)
        );
    }
}
