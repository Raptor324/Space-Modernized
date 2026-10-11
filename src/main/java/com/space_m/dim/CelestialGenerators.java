package com.space_m.dim;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.space_m.dim.ChunkProviderCelestial.BlockMetaBuffer;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Anmeldung der Generatoren je Himmelskoerper - Gegenstueck zu den Original-{@code WorldProvider<Koerper>}
 * ({@code registerWorldChunkManager}, {@code createChunkGenerator}, {@code getStone}).
 *
 * <p>Das Datapack nennt nur den Koerper ({@code "generator": {"type": "space_m:celestial", "body": "mun"}});
 * hier haengen daran die Java-Fabriken. Neue Koerper (R3) melden sich in ihrem {@code WorldProvider<Koerper>.init()}
 * an ({@link #register}).
 *
 * <p>Threads: ChunkProvider, MapGens und GenLayer haben Zustand. Jeder Generator-Thread bekommt pro Koerper eine
 * eigene Instanz ({@link #state}); weil alles nur vom Weltseed und den Chunk-Koordinaten abhaengt, liefern alle
 * Instanzen dieselben Chunks. Fertige Puffer werden kurz zwischengespeichert ({@link #buffer}), damit
 * Struktur-Hoehenabfragen und die eigentliche Generierung den Chunk nicht doppelt rechnen.
 */
public final class CelestialGenerators {

    private CelestialGenerators() {}

    /**
     * Generator-Definition eines Koerpers.
     *
     * @param chunkManager  Original {@code registerWorldChunkManager} (Seed -> Biomverteilung)
     * @param chunkProvider Original {@code createChunkGenerator}
     * @param stone         Original {@code WorldProviderCelestial.getStone} (Erz-Ersatzblock)
     * @param seaLevel      {@code seaLevel} des ChunkProviders (ohne den Provider zu bauen)
     */
    public record BodyGen(String body,
                          LongFunction<WorldChunkManagerCelestial> chunkManager,
                          BiFunction<CelestialWorld, Long, ChunkProviderCelestial> chunkProvider,
                          Supplier<BlockState> stone,
                          int seaLevel) {
    }

    private static final Map<String, BodyGen> BODIES = new ConcurrentHashMap<>();

    /** Je Thread und Koerper: WorldChunkManager + ChunkProvider fuer den aktuellen Seed. */
    private static final ThreadLocal<Map<String, State>> STATES = ThreadLocal.withInitial(HashMap::new);

    private static final Map<String, Map<Long, BlockMetaBuffer>> BUFFERS = new ConcurrentHashMap<>();
    private static final int BUFFER_CACHE = 256;

    private static volatile int generation;

    public static void register(BodyGen gen) {
        BODIES.put(gen.body(), gen);
    }

    @Nullable
    public static BodyGen get(String body) {
        return BODIES.get(body);
    }

    public static BodyGen require(String body) {
        BodyGen g = BODIES.get(body);
        if (g == null) throw new IllegalStateException("No celestial generator registered for body '" + body + "' (known: " + BODIES.keySet() + ")");
        return g;
    }

    /** Neuer Weltseed: alle Thread-Instanzen und Puffer verwerfen. */
    static void invalidate() {
        generation++;
        BUFFERS.clear();
    }

    /** Zustand dieses Threads fuer den Koerper (nach Seed-Wechsel neu gebaut). */
    public static State state(String body) {
        long seed = CelestialSeed.get();
        Map<String, State> map = STATES.get();
        State s = map.get(body);
        if (s == null || s.seed != seed || s.generation != generation) {
            BodyGen gen = require(body);
            WorldChunkManagerCelestial wcm = gen.chunkManager().apply(seed);
            ChunkProviderCelestial provider = gen.chunkProvider().apply(wcm, seed);
            s = new State(seed, generation, wcm, provider);
            map.put(body, s);
        }
        return s;
    }

    /** Fertiger Chunk-Puffer (aus dem Zwischenspeicher oder neu generiert). */
    public static BlockMetaBuffer buffer(String body, int chunkX, int chunkZ) {
        long key = ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
        Map<Long, BlockMetaBuffer> cache = BUFFERS.computeIfAbsent(body, b -> new LinkedHashMap<>(BUFFER_CACHE, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, BlockMetaBuffer> eldest) {
                return size() > BUFFER_CACHE;
            }
        });
        synchronized (cache) {
            BlockMetaBuffer b = cache.get(key);
            if (b != null) return b;
        }
        BlockMetaBuffer b = state(body).provider.provideChunk(chunkX, chunkZ);
        synchronized (cache) {
            cache.put(key, b);
        }
        return b;
    }

    public static final class State {
        public final long seed;
        final int generation;
        public final WorldChunkManagerCelestial chunkManager;
        public final ChunkProviderCelestial provider;

        State(long seed, int generation, WorldChunkManagerCelestial chunkManager, ChunkProviderCelestial provider) {
            this.seed = seed;
            this.generation = generation;
            this.chunkManager = chunkManager;
            this.provider = provider;
        }
    }
}
