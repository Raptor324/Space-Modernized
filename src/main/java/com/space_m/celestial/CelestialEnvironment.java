package com.space_m.celestial;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceEnvironment;
import com.space_m.SpaceMod;
import com.space_m.celestial.SolarSystem.AstroMetric;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Temperature;
import com.space_m.celestial.trait.CBT_Water;
import com.space_m.config.SpaceConfig;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Implementierung von {@link SpaceEnvironment} fuer HBM-Modernized: beantwortet alle Umweltfragen aus dem
 * Himmelskoerper-Modell ({@link CelestialBody}, Traits, {@link SolarSystem}). Registriert beim Start
 * ({@link CelestialRegistry#init()}) ueber {@code SpaceApi.setEnvironment}.
 *
 * <p>Quellen im Original:
 * <ul>
 *   <li>Schwerkraft: {@code CelestialBody.getGravity(EntityLivingBase)} / {@code getSurfaceGravity() * PLAYER_GRAVITY_MODIFIER}</li>
 *   <li>Druck/Gase: {@code ChunkAtmosphereHandler.getAtmosphere} (Koerper-Atmosphaere; abgedichtete Raeume =
 *       {@link LocalAtmosphere}, R2), Verbrennung {@code TileEntityMachineBase.breatheAir}</li>
 *   <li>Temperatur: {@code CBT_Temperature} (ohne Trait {@link SpaceEnvironment#STANDARD_TEMPERATURE})</li>
 *   <li>Sonne: {@code WorldProviderCelestial.getSunPower(provider, x, z)} inkl. Finsternis, Orbit ueber {@link OrbitHooks}</li>
 *   <li>Wassertabelle: {@code CBT_Water}; Orbit-Flag: {@code CelestialBody.inOrbit}</li>
 * </ul>
 * Koerperkennung: Kerbin (Overworld) = {@link SpaceEnvironment#EARTH} (damit HBM-eigene Erd-Ressourcen gelten),
 * sonst {@code space_m:<name>}.
 */
// Original-Klasse: WorldProviderCelestial (Umweltabfragen; Himmel/Zeit/Generator in client.sky, time, dim)
public class CelestialEnvironment implements SpaceEnvironment {

    /**
     * Ortsabhaengige Atmosphaere (Original {@code ChunkAtmosphereManager.proxy}). Standard: nur die Koerper-
     * Atmosphaere; R2 ersetzt sie durch die Blob-Variante (abgedichtete Raeume, Verbrauch).
     */
    public interface LocalAtmosphere {
        /** Original {@code getAtmosphere(world, x, y, z)}: veraenderbare Kopie, null = Vakuum. */
        @Nullable
        default CBT_Atmosphere getAtmosphere(Level level, BlockPos pos) {
            CBT_Atmosphere atmosphere = celestialAtmosphere(level);
            return atmosphere.fluids.isEmpty() ? null : atmosphere;
        }

        /**
         * Blob-Teil von {@code breatheAir}: true, wenn ein abgedichteter Raum am Ort Luft >= 0.19 / O2 >= 0.09 atm
         * hat (dann {@code amount} mB verbraucht). Standard: keine Raeume.
         */
        default boolean breatheFromBlob(Level level, BlockPos pos, int amount) { return false; }
    }

    private static LocalAtmosphere localAtmosphere = new LocalAtmosphere() { };
    private static Predicate<Level> skyOverride = level -> false;

    public static void setLocalAtmosphere(LocalAtmosphere provider) {
        localAtmosphere = provider;
    }

    public static LocalAtmosphere localAtmosphere() {
        return localAtmosphere;
    }

    /** Himmel-Teil (R1-C) meldet, fuer welche Dimensionen er den Himmel zeichnet ({@link #overridesSky}). */
    public static void setSkyOverride(Predicate<Level> predicate) {
        skyOverride = predicate;
    }

    /** Original {@code getCelestialAtmosphere}: Kopie der Koerper-Atmosphaere, im Orbit leer. */
    public static CBT_Atmosphere celestialAtmosphere(Level level) {
        if (CelestialBody.inOrbit(level)) return new CBT_Atmosphere();
        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        if (atmosphere == null) return new CBT_Atmosphere();
        return atmosphere.clone();
    }

    /** Kennung eines Koerpers fuer die HBM-API. */
    public static ResourceLocation bodyId(CelestialBody body) {
        if ("kerbin".equals(body.name)) return EARTH;
        return SpaceMod.id(body.name);
    }

    /** Umkehrung von {@link #bodyId}; unbekannt -> null. */
    @Nullable
    public static CelestialBody bodyFromId(ResourceLocation id) {
        if (EARTH.equals(id)) return CelestialBody.getBodyOrNull("kerbin");
        if (!SpaceMod.MOD_ID.equals(id.getNamespace())) return null;
        return CelestialBody.getBodyOrNull(id.getPath());
    }

    /** Oberflaechenschwerkraft eines Koerpers in NTM-Einheiten (Original {@code getSurfaceGravity() * PLAYER_GRAVITY_MODIFIER}). */
    public static float gravityOf(CelestialBody body) {
        return body.getSurfaceGravity() * AstronomyUtil.PLAYER_GRAVITY_MODIFIER;
    }

    // ───────────────────────────── Schwerkraft / Orbit ─────────────────────────────

    @Override
    public float gravity(Level level, BlockPos pos) {
        if (CelestialBody.inOrbit(level)) return 0.0F;
        return gravityOf(CelestialBody.getBody(level));
    }

    @Override
    public float gravity(LivingEntity entity) {
        return CelestialBody.getGravity(entity);
    }

    @Override
    public boolean inOrbit(Level level) {
        return CelestialBody.inOrbit(level);
    }

    // ───────────────────────────── Atmosphaere ─────────────────────────────

    @Override
    public double pressure(Level level, BlockPos pos) {
        CBT_Atmosphere atmosphere = localAtmosphere.getAtmosphere(level, pos);
        return atmosphere != null ? atmosphere.getPressure() : 0.0D;
    }

    @Override
    public double gasPressure(Level level, BlockPos pos, ResourceLocation fluid) {
        CBT_Atmosphere atmosphere = localAtmosphere.getAtmosphere(level, pos);
        return atmosphere != null ? atmosphere.getPressure(fluid) : 0.0D;
    }

    /** 1:1 {@code TileEntityMachineBase.breatheAir(world, x, y, z, amount)}. */
    @Override
    public boolean consumeOxygen(Level level, BlockPos pos, int amount) {
        CBT_Atmosphere atmosphere = CelestialBody.inOrbit(level) ? null : CelestialBody.getTrait(level, CBT_Atmosphere.class);
        if (atmosphere != null) {
            if (atmosphere.hasFluid(CelestialFluids.EARTHAIR, 0.19) || atmosphere.hasFluid(CelestialFluids.OXYGEN, 0.09)) {
                return true;
            }
        }

        return localAtmosphere.breatheFromBlob(level, pos, amount);
    }

    /** 1:1 {@code CelestialBody.emitGas(world, fluid, amount)} (Gasfackel u.ae.; nur Server). */
    @Override
    public void emitGas(Level level, BlockPos pos, ResourceLocation fluid, int mb) {
        if (level.isClientSide) return;
        CelestialBody.emitGas(level, fluid, mb);
    }

    @Override
    public int temperature(Level level, BlockPos pos) {
        CBT_Temperature temperature = CelestialBody.getTrait(level, CBT_Temperature.class);
        return temperature != null ? (int) temperature.degrees : STANDARD_TEMPERATURE;
    }

    // ───────────────────────────── Strahlung ─────────────────────────────

    /**
     * 1:1 {@code EntityEffectHandler.handleRadiationFX} (Space-Teil): auf Himmelskoerpern ({@code WorldProviderCelestial},
     * auch Kerbin = Overworld) und im Orbit strahlt die Sonne, wenn der Ort freien Himmel hat
     * ({@code Himmelslicht - skylightSubtracted >= 14}: nachts, unter einer Blockdecke oder in Hoehlen nicht).
     * Wert: {@link #celestialRadiation}. HBM nimmt {@code max(Chunk-/Nether-Strahlung, Wert)} und kontaminiert mit
     * {@code rad / 20} je Tick (nur Server); der Geigerzaehler zeigt das ueber {@code radBuf} an.
     */
    @Override
    public float ambientRadiation(Level level, BlockPos pos) {
        if (!isCelestialOrOrbit(level)) return 0.0F;
        if (!skyExposed(level, pos)) return 0.0F;
        return Math.max(0.0F, celestialRadiation(level, pos.getX(), pos.getZ()));
    }

    /** Original {@code getSavedLightValue(Sky) - skylightSubtracted >= 14}: freier Himmel bei Tag (Abschirmung). */
    public static boolean skyExposed(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken() >= 14;
    }

    /**
     * Unabgeschirmte Sonnenstrahlung in RAD/s (Original): {@code target.body.getSunPower()} (1 / Abstand des Planeten
     * in AU^2; im Orbit der umkreiste Koerper), auf der Oberflaeche minus {@code Atmosphaerendruck * 4}, mal
     * {@code celestialRadMultiplier} (0.25 = 0.25 RAD/s auf der Mun). Kann negativ sein (dichte Atmosphaere).
     */
    public static float celestialRadiation(Level level, int x, int z) {
        CelestialBody.Target target = CelestialBody.getTarget(level, x, z);
        return celestialRadiation(target.body, target.inOrbit);
    }

    /** {@link #celestialRadiation(Level, int, int)} fuer einen Koerper (Oberflaeche: dessen Atmosphaere; Orbit: ohne). */
    public static float celestialRadiation(CelestialBody body, boolean inOrbit) {
        CBT_Atmosphere atmosphere = !inOrbit ? body.getTrait(CBT_Atmosphere.class) : null;

        float targetRad = body.getSunPower();

        if (atmosphere != null) {
            targetRad -= (float) atmosphere.getPressure() * 4;
        }

        targetRad *= (float) SpaceConfig.get().celestialRadMultiplier;
        return targetRad;
    }

    /** Original {@code provider instanceof WorldProviderCelestial || WorldProviderOrbit}: Overworld (Kerbin) + Space-Dimensionen. */
    public static boolean isCelestialOrOrbit(Level level) {
        return level.dimension() == Level.OVERWORLD || SpaceDimensions.isSpaceDimension(level.dimension());
    }

    /** R9-B: Neutronenaktivierung des Koerpers (Original {@code EntityEffectHandler.handleRadiation}, vor der Chunk-Strahlung). */
    @Override
    public void onLivingTick(net.minecraft.world.entity.LivingEntity entity) {
        com.space_m.item.NeutronEffects.tick(entity);
    }

    /** R9-B: Original {@code TileEntityDecon.deconNeutron}. */
    @Override
    public void onDecontaminate(net.minecraft.world.entity.LivingEntity entity) {
        com.space_m.item.NeutronEffects.decontaminate(entity);
    }

    // ───────────────────────────── Sonne / Himmel / Koerper ─────────────────────────────

    /** 1:1 {@code WorldProviderCelestial.getSunPower(provider, x, z)}. */
    @Override
    public float solarIntensity(Level level, BlockPos pos) {
        if (CelestialBody.inOrbit(level)) return OrbitHooks.get().sunPower(level, pos.getX(), pos.getZ());
        CelestialBody body = CelestialBody.getBody(level);
        return body.getSunPower() * (1 - (float) getEclipseAmount(level));
    }

    @Override
    public ResourceLocation body(Level level) {
        return bodyId(CelestialBody.getBody(level));
    }

    @Override
    public ResourceLocation targetBody(Level level, BlockPos pos) {
        return bodyId(CelestialBody.getTargetBody(level, pos));
    }

    @Override
    public boolean overridesSky(Level level) {
        return skyOverride.test(level);
    }

    @Override
    @Nullable
    public ResourceLocation waterTableFluid(Level level) {
        if (CelestialBody.inOrbit(level)) return null;
        CBT_Water water = CelestialBody.getTrait(level, CBT_Water.class);
        return water != null ? water.fluid : null;
    }

    // ───────────────────────────── Finsternis (WorldProviderCelestial) ─────────────────────────────

    private record EclipseCache(long gameTime, double amount) {}

    private static final Map<Level, EclipseCache> ECLIPSE = new WeakHashMap<>();

    /**
     * Original {@code WorldProviderCelestial.getEclipseAmount()} (Wert je Tick zwischengespeichert): Bahnmetriken vom
     * Koerper aus bei Sonnenwinkel {@code getCelestialAngle(0)} ({@code Level.getTimeOfDay(0)}), Bedeckung durch
     * Koerper mit scheinbarer Groesse >= 1.
     */
    public static synchronized double getEclipseAmount(Level level) {
        long time = level.getGameTime();
        EclipseCache cached = ECLIPSE.get(level);
        if (cached != null && cached.gameTime == time) return cached.amount;

        CelestialBody body = CelestialBody.getBody(level);
        // First fetch the suns true size
        double sunSize = SolarSystem.calculateSunSize(body);
        float solarAngle = level.getTimeOfDay(0);

        // Get our orrery of bodies
        List<AstroMetric> metrics = SolarSystem.calculateMetricsFromBody(level, 0, body, solarAngle);

        double amount = getEclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_SURFACE);
        ECLIPSE.put(level, new EclipseCache(time, amount));
        return amount;
    }

    /** 1:1 {@code WorldProviderCelestial.getEclipseFactor}. */
    public static double getEclipseFactor(List<AstroMetric> metrics, double sunSize, double maxSize) {
        double factor = 0;
        double sunArc = getArc(sunSize);

        // Calculate eclipse
        for (AstroMetric metric : metrics) {
            if (metric.apparentSize < 1) continue;

            double planetArc = getArc(Mth.clamp(metric.apparentSize, 0, maxSize));
            double minPhase = 1 - (planetArc + sunArc);
            double maxPhase = 1 - (planetArc - sunArc);
            if (metric.phaseObscure < minPhase) continue;

            double thisFactor = 1 - (metric.phaseObscure - maxPhase) / (minPhase - maxPhase);

            factor = Math.min(Math.max(factor, thisFactor), 1.0);
        }

        return factor;
    }

    // due to rendering, the arc is not exactly 1deg = 1deg, this converts from apparentSize to 0-1
    private static double getArc(double apparentSize) {
        return apparentSize * 0.0017D + Math.sqrt(apparentSize * 0.00003D);
    }
}
