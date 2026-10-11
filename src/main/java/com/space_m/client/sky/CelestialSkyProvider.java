package com.space_m.client.sky;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import com.hbm_m.handler.ImpactWorldHandler;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystem.AstroMetric;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;
import com.space_m.celestial.trait.CBT_Destroyed;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.celestial.trait.CBT_Water;
import com.space_m.celestial.trait.CBT_Weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Client-Teil von NTM Space {@code WorldProviderCelestial} (Himmels-, Nebel-, Sonnen-, Stern-, Wolken- und Wetterfarben,
 * Finsternis, Lichtkarte, Meteore) fuer eine Dimension. 1.7.10 fragt den WorldProvider, hier fragt die
 * {@link CelestialEffects} dieses Objekt. {@code super.xxx} des Originals = Vanilla-Wert der Client-Welt.
 *
 * <p>Die Unterklassen am Ende sind die Himmels-Anteile der Koerper-Provider ({@code WorldProviderEve/Tekto/Thatmo/
 * Ike/Laythe/Duna/Orbit}); Weltgenerierung, Bloecke usw. dieser Provider gehoeren nicht hierher.
 * Himmel-Teil von WorldProviderCelestial/WorldProviderEarth (Oberwelt = dieser Basisprovider).
 */
public class CelestialSkyProvider {

    /** Koerper dieser Dimension (Original {@code body}, gesetzt in {@code generateLightBrightnessTable}). */
    public CelestialBody body(ClientLevel level) {
        return CelestialBody.getBody(level);
    }

    public List<AstroMetric> metrics = new ArrayList<>();

    protected double eclipseAmount;

    /** {@code getSunBrightness(1.0F)} einmal je Lichtkarten-Aktualisierung (Zelle 0/0), fuer alle 256 Zellen. */
    public float lightmapSunBrightness = 1.0F;

    /** Original {@code WorldProviderCelestial.meteors} (nur Client). */
    public static final ArrayList<Meteor> meteors = new ArrayList<>();

    private long lastUpdateFrame = Long.MIN_VALUE;
    private float lastUpdatePartial = Float.NaN;

    // ── updateWeather (Client-Anteil) ──

    /** Original {@code updateWeather()} auf dem Client (einmal je Client-Tick fuer die Welt des Spielers). */
    public void clientTick(ClientLevel level) {
        ListIterator<Meteor> iterator = meteors.listIterator();
        while (iterator.hasNext()) {
            Meteor meteor = iterator.next();
            Meteor fragment = meteor.update(level.random);
            if (meteor.isDead) iterator.remove();
            if (fragment != null) iterator.add(fragment);
        }
    }

    // ── updateSky ──

    /**
     * Original {@code updateSky(partialTicks)} (am Anfang jedes Bildes aus {@code getSkyColor}): Bahnmetriken und
     * Finsternis zwischenspeichern. Hier einmal je Bild/Teiltick, beim ersten Aufrufer (Nebelfarbe oder Himmel).
     */
    public void ensureSky(ClientLevel level, float partialTicks) {
        long frame = level.getGameTime();
        if (frame == lastUpdateFrame && partialTicks == lastUpdatePartial) return;
        lastUpdateFrame = frame;
        lastUpdatePartial = partialTicks;
        updateSky(level, partialTicks);
    }

    protected void updateSky(ClientLevel level, float partialTicks) {
        CelestialBody body = body(level);
        // First fetch the suns true size
        double sunSize = SolarSystem.calculateSunSize(body);
        float solarAngle = level.getTimeOfDay(partialTicks);

        // Get our orrery of bodies, this is cached for reuse in sky rendering
        metrics = SolarSystem.calculateMetricsFromBody(level, partialTicks, body, solarAngle);

        // Get our eclipse amount
        eclipseAmount = CelestialEnvironment.getEclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_SURFACE);
    }

    // ── Nebel ──

    /** Original {@code fogDensity(FogDensity)}; 0 = Vanilla-Nebel. {@code inWater}: Kamera in Wasser. */
    public float fogDensity(ClientLevel level, boolean inWater) {
        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        if (atmosphere == null) return 0;

        float pressure = (float) atmosphere.getPressure();

        if (pressure <= 2F) return 0;

        return pressure * pressure * 0.002F;
    }

    /** Original {@code getFogColor(solarAngle, y)}; {@code sun} = {@code clamp(cos(angle*2pi)*2+0.5)} (Vanilla-Helligkeit). */
    public Vec3 getFogColor(ClientLevel level, float sun) {
        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);

        // The cold hard vacuum of space
        if (atmosphere == null) return new Vec3(0, 0, 0);

        float sunR = sun;
        float sunG = sun;
        float sunB = sun;

        if (!SkyConfig.enableHardcoreDarkness()) {
            sunR *= 0.94F;
            sunG *= 0.94F;
            sunB *= 0.91F;
        }

        float totalPressure = (float) atmosphere.getPressure();
        double cx = 0, cy = 0, cz = 0;

        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            FluidEntry entry = atmosphere.fluids.get(i);
            double fx, fy, fz;

            if (CelestialFluids.EVEAIR.equals(entry.fluid)) {
                fx = 53F / 255F * sunR; fy = 32F / 255F * sunG; fz = 74F / 255F * sunB;
            } else if (CelestialFluids.DUNAAIR.equals(entry.fluid) || SkyFluids.CARBONDIOXIDE.equals(entry.fluid)) {
                fx = 212F / 255F * sunR; fy = 112F / 255F * sunG; fz = 78F / 255F * sunB;
            } else if (CelestialFluids.EARTHAIR.equals(entry.fluid) || CelestialFluids.OXYGEN.equals(entry.fluid) || CelestialFluids.NITROGEN.equals(entry.fluid)) {
                // Default to regular ol' overworld
                fx = 0.7529412F * sunR; fy = 0.84705883F * sunG; fz = 1.0F * sunB;
            } else {
                Vec3 c = AtmosphereRenderUtil.getColorFromHex(CelestialFluids.color(entry.fluid));
                fx = c.x * sunR * 1.4F; fy = c.y * sunG * 1.4F; fz = c.z * sunB * 1.4F;
            }

            float percentage = (float) entry.pressure / totalPressure;
            cx += fx * percentage;
            cy += fy * percentage;
            cz += fz * percentage;
        }

        // Add minimum fog colour, for night-time glow
        if (!SkyConfig.enableHardcoreDarkness()) {
            float nightDensity = Mth.clamp(totalPressure, 0.0F, 1.0F);
            cx += 0.06F * nightDensity;
            cy += 0.06F * nightDensity;
            cz += 0.09F * nightDensity;
        }

        // Fog intensity remains high to simulate a thin looking atmosphere on low pressure planets
        float pressureFactor = Mth.clamp(totalPressure * 10.0F, 0.0F, 1.0F);
        cx *= pressureFactor;
        cy *= pressureFactor;
        cz *= pressureFactor;

        Entity view = Minecraft.getInstance().getCameraEntity();
        if (view != null && view.getY() > 600) {
            double curvature = Mth.clamp((1000.0F - (float) view.getY()) / 400.0F, 0.0F, 1.0F);
            cx *= curvature;
            cy *= curvature;
            cz *= curvature;
        }

        if (eclipseAmount > 0) {
            cx *= 1 - eclipseAmount * 0.3;
            cy *= 1 - eclipseAmount * 0.3;
            cz *= 1 - eclipseAmount * 0.3;

            float[] sunsetFog = calcSunriseSunsetColors(level, 0.25F, 0);
            if (sunsetFog != null) {
                double sunsetAmount = Mth.clamp(eclipseAmount * 0.5 - (1 - sun), 0.0, 1.0);
                cx = cx * (1.0F - sunsetAmount) + sunsetFog[0] * sunsetAmount;
                cy = cy * (1.0F - sunsetAmount) + sunsetFog[1] * sunsetAmount;
                cz = cz * (1.0F - sunsetAmount) + sunsetFog[2] * sunsetAmount;
            }
        }

        float dust = ImpactWorldHandler.getDustForClient(level);
        float fire = ImpactWorldHandler.getFireForClient(level);

        cy *= 1 - (dust * 0.5F);
        cz *= 1 - dust;

        if (fire > 0) {
            cx *= Math.max((1 - (dust * 2)), 0);
            cy *= Math.max((1 - (dust * 2)), 0);
            cz *= Math.max((1 - (dust * 2)), 0);
        } else {
            cx *= 1 - dust;
            cy *= 1 - dust;
            cz *= 1 - dust;
        }

        return new Vec3(cx, cy, cz);
    }

    // ── Himmel ──

    /** Original {@code getSkyColor(camera, partialTicks)} (ohne das {@code updateSky}, s. {@link #ensureSky}). */
    public Vec3 getSkyColor(ClientLevel level, Vec3 cameraPos, float partialTicks) {
        ensureSky(level, partialTicks);
        CelestialBody body = body(level);

        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        double cx = 0, cy = 0, cz = 0;

        // The cold hard vacuum of space
        if (atmosphere == null) {
            return new Vec3(0, 0, 0);
        }

        float sun = getSunBrightnessFactor(level, 1.0F);
        float totalPressure = (float) atmosphere.getPressure();

        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            FluidEntry entry = atmosphere.fluids.get(i);
            double fx, fy, fz;

            if (CelestialFluids.EVEAIR.equals(entry.fluid)) {
                fx = 53F / 255F * sun; fy = 32F / 255F * sun; fz = 74F / 255F * sun;
            } else if (CelestialFluids.DUNAAIR.equals(entry.fluid) || SkyFluids.CARBONDIOXIDE.equals(entry.fluid)) {
                fx = 212F / 255F * sun; fy = 112F / 255F * sun; fz = 78F / 255F * sun;
            } else if (CelestialFluids.EARTHAIR.equals(entry.fluid) || CelestialFluids.OXYGEN.equals(entry.fluid) || CelestialFluids.NITROGEN.equals(entry.fluid)) {
                // Default to regular ol' overworld
                Vec3 v = level.getSkyColor(cameraPos, partialTicks);
                fx = v.x; fy = v.y; fz = v.z;
            } else {
                Vec3 c = AtmosphereRenderUtil.getColorFromHex(CelestialFluids.color(entry.fluid));
                fx = c.x * sun; fy = c.y * sun; fz = c.z * sun;
            }

            float percentage = (float) entry.pressure / totalPressure;
            cx += fx * percentage;
            cy += fy * percentage;
            cz += fz * percentage;
        }

        CBT_War wardat = body.getTrait(CBT_War.class);

        if (wardat != null) {
            for (int i = 0; i < wardat.getProjectiles().size(); i++) {
                CBT_War.Projectile projectile = wardat.getProjectiles().get(i);
                float flash = projectile.getFlashtime();
                if (projectile.getAnimtime() > 0) {
                    float invertedFlash = 100 - flash;

                    cx += invertedFlash * 0.5;
                    cy += invertedFlash * 0.5;
                    cz += invertedFlash * 0.5;
                }
            }
        }

        // Lower pressure sky renders thinner
        float pressureFactor = Mth.clamp(totalPressure, 0.0F, 1.0F);
        cx *= pressureFactor;
        cy *= pressureFactor;
        cz *= pressureFactor;

        if (eclipseAmount > 0) {
            cx *= 1 - eclipseAmount * 0.6;
            cy *= 1 - eclipseAmount * 0.6;
            cz *= 1 - eclipseAmount * 0.5;
        }

        float dust = ImpactWorldHandler.getDustForClient(level);
        float fire = ImpactWorldHandler.getFireForClient(level);

        if (dust > 0) {
            if (fire > 0) {
                cx *= 1.3;
                cy *= Math.max((1 - (dust * 1.4f)), 0);
                cz *= Math.max((1 - (dust * 4)), 0);
            } else {
                cy *= 1 - (dust * 0.5F);
                cz *= Math.max((1 - (dust * 4)), 0);
            }

            cx *= fire + (1 - dust);
            cy *= fire + (1 - dust);
            cz *= fire + (1 - dust);
        }

        return new Vec3(cx, cy, cz);
    }

    /** Original {@code calcSunriseSunsetColors(solarAngle, partialTicks)}. */
    public float[] calcSunriseSunsetColors(ClientLevel level, float solarAngle, float partialTicks) {
        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        if (atmosphere == null || atmosphere.getPressure() < 0.05F) return null;

        float[] colors = vanillaSunriseColors(solarAngle);
        if (colors == null) return null;

        // Mars IRL has inverted blue sunsets, which look cool as
        // So carbon dioxide rich atmospheres will do the same
        // for now, it's just a swizzle between red and blue
        if (atmosphere.hasFluid(CelestialFluids.DUNAAIR) || atmosphere.hasFluid(SkyFluids.CARBONDIOXIDE)) {
            float tmp = colors[0];
            colors[0] = colors[2];
            colors[2] = tmp;
        } else if (atmosphere.hasFluid(CelestialFluids.EVEAIR)) {
            float f2 = 0.4F;
            float f3 = Mth.cos((solarAngle) * (float) Math.PI * 2.0F) - 0.0F;
            float f4 = -0.0F;

            if (f3 >= f4 - f2 && f3 <= f4 + f2) {
                float f5 = (f3 - f4) / f2 * 0.5F + 0.5F;
                float f6 = 1.0F - (1.0F - Mth.sin(f5 * (float) Math.PI)) * 0.99F;
                f6 *= f6;
                colors[0] = f5 * 0.01F;
                colors[1] = f5 * f5 * 0.9F + 0.3F;
                colors[2] = f5 * f5;
                colors[3] = f6;
            }
        } else if (atmosphere.hasFluid(CelestialFluids.TEKTOAIR) || atmosphere.hasFluid(CelestialFluids.JOOLGAS) || atmosphere.hasFluid(SkyFluids.CHLORINE)) {
            float tmp = colors[1];
            colors[1] = colors[2];
            colors[2] = tmp;
        }

        float dustFactor = 1 - ImpactWorldHandler.getDustForClient(level);
        colors[0] *= dustFactor;
        colors[1] *= dustFactor;
        colors[2] *= dustFactor;
        colors[3] *= dustFactor;

        return colors;
    }

    /** {@code WorldProvider.calcSunriseSunsetColors} 1.7.10 (= Vanilla {@code DimensionSpecialEffects.getSunriseColor}). */
    public static float[] vanillaSunriseColors(float solarAngle) {
        float f2 = 0.4F;
        float f3 = Mth.cos(solarAngle * (float) Math.PI * 2.0F) - 0.0F;
        float f4 = -0.0F;

        if (f3 >= f4 - f2 && f3 <= f4 + f2) {
            float f5 = (f3 - f4) / f2 * 0.5F + 0.5F;
            float f6 = 1.0F - (1.0F - Mth.sin(f5 * (float) Math.PI)) * 0.99F;
            f6 *= f6;
            return new float[] {f5 * 0.3F + 0.7F, f5 * f5 * 0.7F + 0.2F, f5 * f5 * 0.0F + 0.2F, f6};
        }
        return null;
    }

    /** Original {@code getCloudLayerCount(atmosphere)}. */
    public static int getCloudLayerCount(CBT_Atmosphere atmosphere) {
        if (atmosphere == null || atmosphere.getPressure() < 0.5F) {
            return 0;
        }

        if (atmosphere.getPressure() >= 5.0F) {
            return 3;
        }

        if (atmosphere.getPressure() >= 2.5F) {
            return 2;
        }

        return 1;
    }

    public boolean hasWeatherCycle(ClientLevel level) {
        return CBT_Weather.supportsWeather(body(level));
    }

    /** Original {@code getWeatherColor()}. */
    public Vec3 getWeatherColor(ClientLevel level) {
        CBT_Water water = CelestialBody.getTrait(level, CBT_Water.class);
        if (water == null || water.fluid == null) {
            return new Vec3(1.0D, 1.0D, 1.0D);
        }

        Vec3 base = AtmosphereRenderUtil.getColorFromHex(CelestialFluids.color(water.fluid));
        double luminance = base.x * 0.299D + base.y * 0.587D + base.z * 0.114D;
        double saturation = 0.35D;

        double desaturatedR = luminance + (base.x - luminance) * saturation;
        double desaturatedG = luminance + (base.y - luminance) * saturation;
        double desaturatedB = luminance + (base.z - luminance) * saturation;

        return new Vec3(
            Mth.clamp(desaturatedR, 0.0D, 1.0D),
            Mth.clamp(desaturatedG, 0.0D, 1.0D),
            Mth.clamp(desaturatedB, 0.0D, 1.0D)
        );
    }

    /** Original {@code getSnowColor()}. */
    public Vec3 getSnowColor(ClientLevel level) {
        CBT_Water water = CelestialBody.getTrait(level, CBT_Water.class);
        if (water == null || water.fluid == null || CelestialFluids.WATER.equals(water.fluid)) {
            return new Vec3(1.0D, 1.0D, 1.0D);
        }

        return getWeatherColor(level);
    }

    // Stars do not show up during the day in a vacuum, common misconception:
    // The reason stars aren't visible during the day on Earth isn't because of the sky,
    // the sky is ALWAYS there. The reason they aren't visible is because the Sun is too bright!
    /** Original {@code getStarBrightness(par1)}. */
    public float getStarBrightness(ClientLevel level, float par1) {
        // Stars become visible during the day beyond the orbit of Duna
        // And are fully visible during the day beyond the orbit of Jool
        float distanceStart = 20_000_000;
        float distanceEnd = 80_000_000;

        float semiMajorAxisKm = CelestialBody.getPlanet(level).semiMajorAxisKm;
        float distanceFactor = Mth.clamp((semiMajorAxisKm - distanceStart) / (distanceEnd - distanceStart), 0F, 1F);

        float starBrightness = level.getStarBrightness(par1);

        float dust = ImpactWorldHandler.getDustForClient(level);

        return Mth.clamp(starBrightness, distanceFactor, 1F) * (1 - dust);
    }

    /** Original {@code getSunBrightness(par1)}. */
    public float getSunBrightness(ClientLevel level, float par1) {
        if (CelestialBody.getStar(level).hasTrait(CBT_Destroyed.class))
            return 0;

        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);

        float sunBrightness = level.getSkyDarken(par1);

        sunBrightness *= 1 - eclipseAmount * 0.6;

        float dust = ImpactWorldHandler.getDustForClient(level);
        sunBrightness *= (1 - dust);

        // brightness _inside_ of the atmosphere, from effects like lightning or war weapons
        float insideBrightness = 0;

        CBT_War wardat = body(level).getTrait(CBT_War.class);

        if (wardat != null) {
            for (int i = 0; i < wardat.getProjectiles().size(); i++) {
                CBT_War.Projectile projectile = wardat.getProjectiles().get(i);
                float flash = projectile.getFlashtime();
                if (projectile.getAnimtime() > 0) {
                    insideBrightness += 100 - flash;
                }
            }
        }

        if (atmosphere == null) {
            return sunBrightness + insideBrightness;
        }

        return sunBrightness * Mth.clamp(1.0F - ((float) atmosphere.getPressure() - 1.5F) * 0.2F, 0.25F, 1.0F) + insideBrightness;
    }

    /** Forge 1.7.10 {@code World.getSunBrightnessFactor(partialTicks)} (wie {@code getSkyDarken} ohne {@code *0.8+0.2}). */
    public static float getSunBrightnessFactor(Level level, float partialTicks) {
        float f1 = level.getTimeOfDay(partialTicks);
        float f2 = 1.0F - (Mth.cos(f1 * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
        f2 = Mth.clamp(f2, 0.0F, 1.0F);
        f2 = 1.0F - f2;
        f2 = (float) ((double) f2 * (1.0D - (double) (level.getRainLevel(partialTicks) * 5.0F) / 16.0D));
        f2 = (float) ((double) f2 * (1.0D - (double) (level.getThunderLevel(partialTicks) * 5.0F) / 16.0D));
        return f2;
    }

    /** Original {@code getSunColor()}. */
    public float[] getSunColor(ClientLevel level) {
        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);

        if (atmosphere == null) return new float[] {1.0F, 1.0F, 1.0F};

        float[] sunColor = {1.0F, 1.0F, 1.0F};

        // Adjust the sun colour based on atmospheric composition
        for (FluidEntry entry : atmosphere.fluids) {
            // Chlorines all redden the sun by absorbing blue and green
            if (CelestialFluids.TEKTOAIR.equals(entry.fluid)
                || SkyFluids.CHLORINE.equals(entry.fluid)
                || SkyFluids.CHLOROMETHANE.equals(entry.fluid)
                || SkyFluids.RADIOSOLVENT.equals(entry.fluid)
                || CelestialFluids.CCL.equals(entry.fluid)) {
                float absorption = Mth.clamp(1.0F - (float) entry.pressure * 0.5F, 0.0F, 1.0F);
                sunColor[1] *= absorption;
                sunColor[2] *= absorption;
            }
        }

        return sunColor;
    }

    /** Original {@code isSkyColored()} (WorldProviderSurface: true). */
    public boolean isSkyColored() {
        return true;
    }

    /** Original {@code getHorizon()}: Oberwelt Vanilla, sonst 63. */
    public double getHorizon(ClientLevel level) {
        if (level.dimension() == Level.OVERWORLD) return level.getLevelData().getHorizonHeight(level);
        return 63;
    }

    /** Original {@code getCloudHeight()}: ohne Wolkenschichten -99999, sonst Vanilla. */
    public float getCloudHeight(ClientLevel level, float vanillaHeight) {
        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);

        if (getCloudLayerCount(atmosphere) <= 0) return -99999;

        return vanillaHeight;
    }

    public int getCloudLayerCount(ClientLevel level) {
        return getCloudLayerCount(CelestialBody.getTrait(level, CBT_Atmosphere.class));
    }

    /**
     * Original {@code updateLightmap(int[] lightmap)}: ein Lichtkarten-Pixel (Farben 0..1) anpassen;
     * {@code sky} = Original {@code lightBrightnessTable[i / 16]}. true = veraendert.
     */
    public boolean updateLightmap(ClientLevel level, float sky, float[] rgb) {
        return false;
    }

    // ── Meteore ──

    /** Original {@code WorldProviderCelestial.Meteor}. */
    public static class Meteor {

        public double posX;
        public double posY;
        public double posZ;
        public double prevPosX;
        public double prevPosY;
        public double prevPosZ;
        public double motionX;
        public double motionY;
        public double motionZ;
        public boolean isDead = false;
        public long age;
        public MeteorType type;

        public Meteor(double posX, double posY, double posZ) {
            this(posX, posY, posZ, MeteorType.STANDARD, -31.2, -20.8, 20);
        }

        public Meteor(double posX, double posY, double posZ, MeteorType type, double motionX, double motionY, double motionZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.type = type;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
        }

        /** Original {@code Meteor.addMeteor()} (aufgerufen von {@code CBT_Invasion} ueber dessen Client-Hook). */
        public static void addMeteor() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            meteors.add(new Meteor(player.getX() + player.level().random.nextInt(16000) - 8000, 2017, player.getZ() + player.level().random.nextInt(16000) - 8000));
        }

        private Meteor update(net.minecraft.util.RandomSource rand) {
            if (this.posY <= 500 && this.type != MeteorType.SMOKE) {
                this.isDead = true;
            }

            if (this.type == MeteorType.SMOKE) {
                this.age++;
                if (this.age >= 60)
                    this.isDead = true;
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.posX += this.motionX;
            this.posY += this.motionY;
            this.posZ += this.motionZ;

            if (this.type != MeteorType.SMOKE && this.type != MeteorType.FRAGMENT) {
                return new Meteor((this.posX + rand.nextInt(16)) - 8, (this.posY + rand.nextInt(16)), (this.posZ + rand.nextInt(16)) - 8, MeteorType.SMOKE, 0, 0, 0);
            }

            return null;
        }
    }

    public enum MeteorType {
        STANDARD,
        FRAGMENT,
        SMOKE
    }

    // ════════════════════════════ Koerper-Provider (Himmels-Anteile) ════════════════════════════

    /** {@code WorldProviderEve}: Blitzlicht der Eve-Gewitter hellt Himmel und Sonne auf, Donnergrollen. */
    public static class Eve extends CelestialSkyProvider {
        private float lastFlashStrength;

        @Override
        public void clientTick(ClientLevel level) {
            super.clientTick(level);
            float flashStrength = getFlashStrength(level, 0.0F);
            if (lastFlashStrength <= 0.01F && flashStrength > 0.01F) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) SkySounds.playRumble(player);
            }
            lastFlashStrength = flashStrength;
        }

        @Override
        public Vec3 getSkyColor(ClientLevel level, Vec3 cameraPos, float partialTicks) {
            Vec3 ohshit = super.getSkyColor(level, cameraPos, partialTicks);
            float alpha = getFlashStrength(level, partialTicks);

            return new Vec3(ohshit.x + alpha, ohshit.y + alpha, ohshit.z + alpha);
        }

        @Override
        public float getSunBrightness(ClientLevel level, float par1) {
            float imsuper = super.getSunBrightness(level, par1);
            float alpha = getFlashStrength(level, par1);

            return imsuper + alpha * 0.7F;
        }

        private float getFlashStrength(ClientLevel level, float partialTicks) {
            return AtmosphereRenderUtil.getBodyEveFlashStrength(body(level), ((float) level.getGameTime() + partialTicks) / 20.0F);
        }
    }

    /** {@code WorldProviderTekto}: Sonne 0.7. */
    public static class Tekto extends CelestialSkyProvider {
        @Override
        public float getSunBrightness(ClientLevel level, float par1) {
            float imsuper = super.getSunBrightness(level, par1);

            return imsuper * 0.7F;
        }
    }

    /** {@code WorldProviderIke}: rote Naechte (Duna-Licht). */
    public static class Ike extends CelestialSkyProvider {
        @Override
        public boolean updateLightmap(ClientLevel level, float sky, float[] rgb) {
            float sun = lightmapSunBrightness - 0.1F;
            float duna = Math.max(sky - sun, 0);

            rgb[0] = Math.min(rgb[0] + duna * 20 / 255F, 1F);
            return true;
        }
    }

    /** {@code WorldProviderLaythe}: gruene Naechte (Jool-Licht), Wassernebel 0.03. */
    public static class Laythe extends CelestialSkyProvider {
        @Override
        public float fogDensity(ClientLevel level, boolean inWater) {
            if (inWater) {
                return 0.03F;
            }

            return super.fogDensity(level, inWater);
        }

        @Override
        public boolean updateLightmap(ClientLevel level, float sky, float[] rgb) {
            float sun = lightmapSunBrightness;
            float jool = Math.max(sky - sun, 0);

            rgb[1] = Math.min(rgb[1] + jool * 60 / 255F, 1F);
            return true;
        }
    }

    /**
     * {@code WorldProviderDuna}: Staubsturm-Nebel. Sturmzustand liegt im Original im Provider (Server-NBT, R3 Duna);
     * hier ueber {@link #dustStorm} (0 = kein Sturm), geglaettet wie {@code dustStormSmoothed}.
     */
    public static class Duna extends CelestialSkyProvider {
        /** Zielstaerke des Sturms (Original {@code dustStormIntensity}), von R3 gesetzt. */
        public static float dustStorm = 0;
        private float dustStormSmoothed = 0;

        @Override
        public void clientTick(ClientLevel level) {
            super.clientTick(level);
            dustStormSmoothed = (float) (dustStormSmoothed + (dustStorm - dustStormSmoothed) * 0.008);
            com.space_m.dim.duna.client.DunaClient.tickDust(level, dustStormSmoothed); // R3-2: Staubpartikel
        }

        /** R3-2: Original {@code WorldProviderDuna.getHorizon()} = 52. */
        @Override
        public double getHorizon(ClientLevel level) {
            return 52;
        }

        @Override
        public float fogDensity(ClientLevel level, boolean inWater) {
            if (dustStormSmoothed >= 0.25F)
                return dustStormSmoothed * dustStormSmoothed * 0.075F;

            return super.fogDensity(level, inWater);
        }

        public float getDustStormSmoothed() {
            return dustStormSmoothed;
        }
    }

    /** {@code WorldProviderThatmo}: Schlachtfeld-Blitze (Zustand in {@link ThatmoSkyRenderer.State}). */
    public static class Thatmo extends CelestialSkyProvider {
        @Override
        public void clientTick(ClientLevel level) {
            super.clientTick(level);
            ThatmoSkyRenderer.State.clientTick(level);
        }

        @Override
        public Vec3 getSkyColor(ClientLevel level, Vec3 cameraPos, float partialTicks) {
            Vec3 ohshit = super.getSkyColor(level, cameraPos, partialTicks);
            float add = ThatmoSkyRenderer.State.flashAlpha();
            return new Vec3(ohshit.x + add, ohshit.y + add, ohshit.z + add);
        }

        @Override
        public Vec3 getFogColor(ClientLevel level, float sun) {
            Vec3 ohshit = super.getFogColor(level, sun);
            float add = ThatmoSkyRenderer.State.flashAlpha();
            return new Vec3(ohshit.x + add, ohshit.y + add, ohshit.z + add);
        }

        @Override
        public float getSunBrightness(ClientLevel level, float par1) {
            float imsuper = super.getSunBrightness(level, par1);
            return imsuper * 0.1F + ThatmoSkyRenderer.State.flashAlpha();
        }
    }

    /** {@code WorldProviderOrbit} (Himmel): Metriken vom Stationsorbit, eigener Sonnenwinkel, kein Nebel/Wetter. */
    public static class Orbit extends CelestialSkyProvider {

        // Orbit at an altitude that provides an hour-long realtime orbit (game time is fast so we go slow)
        private static final float ORBITAL_PERIOD = 7200;

        private float celestialAngle;

        public static float getOrbitalAltitude(CelestialBody body) {
            return getAltitudeForPeriod(body.massKg, ORBITAL_PERIOD);
        }

        // r = cbrt[(G x Me x T2) / (4pi2)]
        private static float getAltitudeForPeriod(float massKg, float period) {
            return (float) Math.cbrt((com.space_m.celestial.AstronomyUtil.GRAVITATIONAL_CONSTANT * massKg * (period * period)) / (4 * Math.PI * Math.PI));
        }

        public static CelestialBody orbiting() {
            CelestialBody b = SkyHooks.get().stationOrbiting();
            return b != null ? b : CelestialBody.getBody("kerbin");
        }

        public static CelestialBody target() {
            CelestialBody b = SkyHooks.get().stationTarget();
            return b != null ? b : orbiting();
        }

        @Override
        protected void updateSky(ClientLevel level, float partialTicks) {
            SkyHooks station = SkyHooks.get();
            CelestialBody orbiting = orbiting();
            CelestialBody target = target();

            // First fetch the suns true size
            double sunSize = SolarSystem.calculateSunSize(orbiting);

            double progress = station.stationTransferProgress(partialTicks);

            // Get our orrery of bodies, this is cached for reuse in sky rendering
            if (station.stationState() == SkyHooks.StationState.ORBIT) {
                double altitude = getOrbitalAltitude(orbiting);
                metrics = SolarSystem.calculateMetricsFromSatellite(level, partialTicks, orbiting, altitude);
            } else {
                double fromAlt = getOrbitalAltitude(orbiting);
                double toAlt = getOrbitalAltitude(target);
                metrics = SolarSystem.calculateMetricsBetweenSatelliteOrbits(level, partialTicks, orbiting, target, fromAlt, toAlt, progress);

                double sunTargetSize = SolarSystem.calculateSunSize(target);
                sunSize = SkyMath.lerp(progress, sunSize, sunTargetSize);
            }

            // Get our sun angle
            float angle = (float) SolarSystem.calculateSingleAngle(level, partialTicks, metrics, orbiting, getOrbitalAltitude(orbiting));
            if (progress > 0) {
                angle = (float) SkyMath.clerp(progress, angle, (float) SolarSystem.calculateSingleAngle(level, partialTicks, metrics, target, getOrbitalAltitude(target)));
            }

            celestialAngle = 0.5F - (angle / 360.0F);

            // Get our eclipse amount
            eclipseAmount = CelestialEnvironment.getEclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);
        }

        /** Original {@code calculateCelestialAngle}: gemerkter Winkel. */
        public float getCelestialAngle() {
            return celestialAngle;
        }

        @Override
        public float fogDensity(ClientLevel level, boolean inWater) {
            return 0;
        }

        @Override
        public Vec3 getFogColor(ClientLevel level, float sun) {
            return new Vec3(0, 0, 0);
        }

        @Override
        public Vec3 getSkyColor(ClientLevel level, Vec3 cameraPos, float partialTicks) {
            ensureSky(level, partialTicks);
            return new Vec3(0, 0, 0);
        }

        @Override
        public float[] calcSunriseSunsetColors(ClientLevel level, float solarAngle, float partialTicks) {
            return null;
        }

        @Override
        public float getStarBrightness(ClientLevel level, float par1) {
            // Stars look cool in orbit, but obvs at Moho we don't want the big fuckoff sun to not extinguish
            // Stars become visible during the day part of orbit just before Earth
            // And are fully visible during the day beyond the orbit of Duna
            float distanceStart = 9_000_000;
            float distanceEnd = 30_000_000;

            double progress = SkyHooks.get().stationTransferProgress(par1);
            float semiMajorAxisKm = orbiting().getPlanet().semiMajorAxisKm;
            if (progress > 0) {
                semiMajorAxisKm = (float) SkyMath.lerp(progress, semiMajorAxisKm, target().getPlanet().semiMajorAxisKm);
            }

            float distanceFactor = Mth.clamp((semiMajorAxisKm - distanceStart) / (distanceEnd - distanceStart), 0F, 1F);

            float starBrightness = (float) eclipseAmount;

            return Mth.clamp(starBrightness, distanceFactor, 1F);
        }

        @Override
        public float getSunBrightness(ClientLevel level, float par1) {
            if (SolarSystem.kerbol.hasTrait(CBT_Destroyed.class))
                return 0;

            return 1.0F - (float) eclipseAmount;
        }

        @Override
        public float getCloudHeight(ClientLevel level, float vanillaHeight) {
            return -99999;
        }

        @Override
        public int getCloudLayerCount(ClientLevel level) {
            return 0;
        }

        @Override
        public boolean hasWeatherCycle(ClientLevel level) {
            return false;
        }
    }

    /** Fuer Zufallswerte, die das Original aus {@code world.rand} zieht (nicht synchron, nur Optik). */
    static final Random RAND = new Random();
}
