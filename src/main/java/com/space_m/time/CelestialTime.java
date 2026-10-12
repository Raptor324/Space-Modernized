package com.space_m.time;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.space_m.SpaceMod;
import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialBodyWorldSavedData;
import com.space_m.celestial.SolarSystem;
import com.space_m.dim.SpaceDimensions;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Zeit je Himmelskoerper, 1:1 {@code WorldProviderCelestial} des Originals:
 * <ul>
 *   <li>Ortszeit ({@code getWorldTime/setWorldTime}): jede Planeten-/Monddimension hat eine eigene, in
 *       {@link CelestialBodyWorldSavedData} gespeicherte Zeit; Kerbin (Overworld) und Orbit nutzen die Vanilla-Zeit.</li>
 *   <li>Tageslaenge ({@code getDayLength}): {@code rotationalPeriod / (1 - 1/orbitalPeriod(Planet))}, Sonnenwinkel
 *       wie {@code calculateCelestialAngle} (auch Kerbin, da das Original die Overworld durch {@code WorldProviderEarth}
 *       ersetzt).</li>
 *   <li>Mondphase ({@code getMoonPhase}): aus dem Winkel zum ersten Mond, ohne Mond Halbmond (2).</li>
 * </ul>
 * Die Gesamtzeit ({@code getGameTime}, Bahnen der Himmelskoerper) bleibt wie im Original global (Overworld).
 * Mixins: {@code LevelTimeMixin}, {@code ServerLevelTimeMixin}, {@code DimensionTypeTimeMixin}.
 */
// Original-Klasse: WorldProviderEarth (Kerbin-Tageslauf; Himmel in client.sky.SkyRegistration)
public final class CelestialTime {
    private CelestialTime() {}

    private static final Map<ServerLevel, CelestialBodyWorldSavedData> DATA = new IdentityHashMap<>();
    private static final Map<ResourceLocation, Optional<CelestialBody>> BODY_BY_EFFECTS = new ConcurrentHashMap<>();

    @Nullable private static MinecraftServer server;
    /** Client-Welt auf dem Render-Thread (nur Client gesetzt, siehe {@link CelestialTimeClient}). */
    @Nullable static Supplier<Level> clientLevel;
    private static final Map<String, long[]> MOON_PHASE_CACHE = new ConcurrentHashMap<>();

    /** Nur GameTests: weitere Dimension mit Ortszeit behandeln ("Mock" solange keine Planetendimension existiert). */
    private static final Set<ResourceKey<Level>> TEST_LEVELS = ConcurrentHashMap.newKeySet();

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(s -> server = s);
        EnvExecutor.runInEnv(Env.CLIENT, () -> CelestialTimeClient::init);
    }

    public static void clearCaches() {
        DATA.clear();
        BODY_BY_EFFECTS.clear();
        MOON_PHASE_CACHE.clear();
        TEST_LEVELS.clear();
        server = null;
    }

    // ── Ortszeit (Server) ───────────────────────────────────────────────────────────────────────

    /** Original: {@code provider instanceof WorldProviderCelestial && dimensionId != 0}. */
    public static boolean hasLocalTime(Level level) {
        ResourceKey<Level> key = level.dimension();
        return SpaceDimensions.BODIES.contains(key) || (!TEST_LEVELS.isEmpty() && TEST_LEVELS.contains(key));
    }

    public static void addTestLocalTimeLevel(ResourceKey<Level> key) {
        TEST_LEVELS.add(key);
    }

    public static void removeTestLocalTimeLevel(ResourceKey<Level> key) {
        TEST_LEVELS.remove(key);
    }

    @Nullable
    private static CelestialBodyWorldSavedData data(ServerLevel level) {
        CelestialBodyWorldSavedData d = DATA.get(level);
        if (d == null) {
            try {
                d = CelestialBodyWorldSavedData.get(level);
            } catch (RuntimeException e) {
                return null; // Weltspeicher noch nicht bereit (ServerLevel-Konstruktor)
            }
            if (d == null) return null;
            DATA.put(level, d);
        }
        return d;
    }

    @Nullable
    public static Long getLocalTimeOrNull(ServerLevel level) {
        CelestialBodyWorldSavedData d = data(level);
        return d != null ? d.getLocalTime() : null;
    }

    public static long getLocalTime(ServerLevel level) {
        Long t = getLocalTimeOrNull(level);
        return t != null ? t : 0L;
    }

    public static void setLocalTime(ServerLevel level, long time) {
        CelestialBodyWorldSavedData d = data(level);
        if (d != null) d.setLocalTime(time);
    }

    /** Forge 1.7.10 {@code WorldServer.tick}: {@code if(doDaylightCycle) provider.setWorldTime(provider.getWorldTime() + 1)}. */
    public static void tickLocalTime(ServerLevel level) {
        if (!hasLocalTime(level)) return;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) return;
        CelestialBodyWorldSavedData d = data(level);
        if (d != null) d.setLocalTime(d.getLocalTime() + 1L);
    }

    // ── Tageslaenge / Sonnenwinkel (beide Seiten) ──────────────────────────────────────────────

    /** Himmelskoerper zu einem Dimensionstyp ({@code effects}: {@code space_m:<koerper>}, Overworld = Kerbin). */
    @Nullable
    public static CelestialBody bodyFor(DimensionType type) {
        ResourceLocation effects = type.effectsLocation();
        Optional<CelestialBody> body = BODY_BY_EFFECTS.get(effects);
        if (body == null) {
            body = lookupBody(effects);
            // vor der Sonnensystem-Initialisierung nichts endgueltig zwischenspeichern
            if (body.isPresent() || !CelestialBody.getAllBodies().isEmpty()) BODY_BY_EFFECTS.put(effects, body);
        }
        return body.orElse(null);
    }

    private static Optional<CelestialBody> lookupBody(ResourceLocation effects) {
        if (effects.equals(BuiltinDimensionTypes.OVERWORLD_EFFECTS)) {
            return Optional.ofNullable(CelestialBody.getBodyOrNull("kerbin"));
        }
        if (effects.getNamespace().equals(SpaceMod.MOD_ID)) {
            return Optional.ofNullable(CelestialBody.getBodyOrNull(effects.getPath()));
        }
        return Optional.empty();
    }

    /** Original {@code WorldProviderCelestial.getDayLength}, in Ticks. */
    public static double getDayLength(CelestialBody body) {
        return body.getRotationalPeriod() / (1 - (1 / body.getPlanet().getOrbitalPeriod()));
    }

    /**
     * Original {@code calculateCelestialAngle} (SOLAR angle, not sidereal/celestial). {@code partialTicks} = 0:
     * Vanilla reicht {@code getTimeOfDay(partialTick)} nicht bis {@code DimensionType.timeOfDay} durch.
     *
     * @return Winkel 0..1 oder {@code NaN}, wenn der Typ keinem Himmelskoerper gehoert (Vanilla-Rechnung)
     */
    public static float celestialAngle(DimensionType type, long worldTime) {
        if (type.hasFixedTime()) return Float.NaN;
        CelestialBody body = bodyFor(type);
        if (body == null || body.parent == null) return Float.NaN;
        return celestialAngle(getDayLength(body), effectiveDayTime(body, worldTime), 0.0F);
    }

    /**
     * Serverseite: {@code LevelAccessor.dayTime()} liest {@code getLevelData().getDayTime()} - in abgeleiteten
     * Dimensionen die Overworld-Zeit. Jeder Planetentyp gehoert genau einer Dimension, daher wird hier deren Ortszeit
     * eingesetzt. Client: die {@code ClientLevelData} traegt bereits die Ortszeit (Zeitpaket).
     */
    private static long effectiveDayTime(CelestialBody body, long dayTime) {
        MinecraftServer s = server;
        if (s != null && body.dimension != null && SpaceDimensions.BODIES.contains(body.dimension) && s.isSameThread()) {
            ServerLevel level = s.getLevel(body.dimension);
            if (level != null) {
                Long local = getLocalTimeOrNull(level);
                if (local != null) return local;
            }
        }
        return dayTime;
    }

    public static float celestialAngle(double dayLength, long worldTime, float partialTicks) {
        double j = worldTime % dayLength;
        double f1 = (j + partialTicks) / dayLength - 0.25F;

        if (f1 < 0.0F) {
            ++f1;
        }

        if (f1 > 1.0F) {
            --f1;
        }

        double f2 = f1;
        f1 = 0.5F - Math.cos(f1 * Math.PI) / 2.0F;
        return (float) (f2 + (f1 - f2) / 3.0D);
    }

    /**
     * Original {@code WorldProviderCelestial.getMoonPhase}: Phase aus dem Winkel zum naechsten (ersten) Mond.
     * @return 0..7 oder -1 (kein Himmelskoerper -> Vanilla)
     */
    public static int moonPhase(DimensionType type) {
        if (type.hasFixedTime()) return -1;
        CelestialBody body = bodyFor(type);
        if (body == null) return -1;

        // if no moons, default to half-moon difficulty
        if (body.satellites.size() == 0) return 2;

        Level world = timeLevel();
        if (world == null) return -1;
        long totalTime = world.getGameTime();

        // je Koerper und Gesamtzeit-Tick zwischengespeichert (Bahnrechnung ist teuer, Vanilla fragt oft)
        long[] cached = MOON_PHASE_CACHE.get(body.name);
        if (cached != null && cached[0] == totalTime && cached[2] == AstronomyUtil.TIME_MULTIPLIER) return (int) cached[1];

        // Determine difficulty phase from closest moon
        int phase = Math.round(8 - ((float) SolarSystem.calculateSingleAngle(world, body, body.satellites.get(0)) / 45 + 4));
        if (phase >= 8) phase = 0;
        MOON_PHASE_CACHE.put(body.name, new long[] { totalTime, phase, AstronomyUtil.TIME_MULTIPLIER });
        return phase;
    }

    /** Welt fuer die Gesamtzeit ({@code world.getTotalWorldTime()}) der aufrufenden Seite; null wenn keine. */
    @Nullable
    private static Level timeLevel() {
        MinecraftServer s = server;
        if (s != null && s.isSameThread()) {
            return s.overworld();
        }
        Supplier<Level> client = clientLevel;
        if (client != null) {
            Level l = client.get();
            if (l != null) return l;
        }
        return s != null ? s.overworld() : null;
    }
}
