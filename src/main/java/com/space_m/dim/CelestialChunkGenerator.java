package com.space_m.dim;

import java.util.List;
import java.util.concurrent.CompletableFuture;
//? if < 1.21.1 {
import java.util.concurrent.Executor;
//?}

import com.mojang.serialization.Codec;
//? if >= 1.21.1 {
/*import com.mojang.serialization.MapCodec;
*///?}
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.ChunkProviderCelestial.BlockMetaBuffer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.NoiseColumn;

/**
 * 1.20-Generator der Himmelskoerper ({@code "type": "space_m:celestial"}) - Huelle um die 1:1 portierten
 * {@link ChunkProviderCelestial}-Unterklassen (Original: {@code IChunkProvider} des {@code WorldProvider<Koerper>}).
 *
 * <ul>
 *   <li>{@code fillFromNoise}: kompletter Original-Ablauf im Puffer (Rauschen, Oberflaeche je Biom, MapGens wie
 *       Hoehlen/Krater/Blasen in Original-Reihenfolge), dann 1:1 in den Chunk kopiert (y 0-255 = Original-y).</li>
 *   <li>{@code buildSurface}/{@code applyCarvers}: leer - Oberflaeche und MapGens laufen im Original im
 *       ChunkProvider vor der Dekoration (nicht im Biom); sie sind deshalb Teil des Puffers.</li>
 *   <li>{@code populate} -> Vanilla-Dekoration mit den Features der Biom-JSONs ({@code BiomeDecoratorCelestial},
 *       Erze aus {@code WorldGeneratorCelestial}/{@code WorldGenerator<Koerper>} als PlacedFeatures).</li>
 *   <li>{@code performWorldGenSpawning} -> {@link #spawnOriginalMobs} (Spawns des Biom-JSON).</li>
 * </ul>
 * JSON: {@code {"type": "space_m:celestial", "body": "mun", "biome_source": {...}}}.
 */
public class CelestialChunkGenerator extends ChunkGenerator {

    //? if < 1.21.1 {
    public static final Codec<CelestialChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    //?} else {
    /*public static final MapCodec<CelestialChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    *///?}
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            Codec.STRING.fieldOf("body").forGetter(g -> g.body)
    ).apply(instance, instance.stable(CelestialChunkGenerator::new)));

    /** Generierte Hoehe (Original-Welt 0..255). */
    public static final int GEN_DEPTH = 256;

    private final String body;

    public CelestialChunkGenerator(BiomeSource biomeSource, String body) {
        super(biomeSource);
        this.body = body;
    }

    public String body() {
        return body;
    }

    //? if < 1.21.1 {
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
    //?} else {
    /*@Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
    *///?}

    // ── Gelaende ───────────────────────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(fill(chunk));
    }
    //?} else {
    /*@Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(fill(chunk));
    }
    *///?}

    /** Kopiert den Original-Puffer in den Chunk (Abschnitte gesperrt wie im Vanilla-Generator). */
    private ChunkAccess fill(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        BlockMetaBuffer buffer = CelestialGenerators.buffer(body, pos.x, pos.z);
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        int minY = Math.max(0, chunk.getMinBuildHeight());
        int maxY = Math.min(GEN_DEPTH, chunk.getMaxBuildHeight());
        if (minY >= maxY) return chunk;

        int firstSection = chunk.getSectionIndex(minY);
        int lastSection = chunk.getSectionIndex(maxY - 1);
        LevelChunkSection[] sections = chunk.getSections();
        for (int s = firstSection; s <= lastSection; s++) sections[s].acquire();

        try {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int column = (x * 16 + z) * 256;
                    for (int y = minY; y < maxY; y++) {
                        BlockState state = buffer.blocks[column + y];
                        if (state == null || state.isAir()) continue;

                        LevelChunkSection section = sections[chunk.getSectionIndex(y)];
                        section.setBlockState(x, y & 15, z, state, false);
                        oceanFloor.update(x, y, z, state);
                        worldSurface.update(x, y, z, state);

                        if (!state.getFluidState().isEmpty()) {
                            chunk.markPosForPostprocessing(mpos.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                        }
                    }
                }
            }
        } finally {
            for (int s = firstSection; s <= lastSection; s++) sections[s].release();
        }

        return chunk;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // Original: genTerrainBlocks laeuft im ChunkProvider (Teil des Puffers)
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Original: MapGen-Hoehlen/Krater laufen im ChunkProvider (Teil des Puffers)
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Original: SpawnerAnimals.performWorldGenSpawning(world, biome, k + 8, l + 8, 16, 16, rand) im populate
        ChunkPos chunkpos = region.getCenter();
        Holder<Biome> holder = region.getBiome(chunkpos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(region.getSeed(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(region, holder, chunkpos, random);
    }

    // ── Abfragen ───────────────────────────────────────────────────────────────────────────────

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        CelestialGenerators.BodyGen gen = CelestialGenerators.get(body);
        return gen != null ? gen.seaLevel() : 63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        BlockMetaBuffer buffer = CelestialGenerators.buffer(body, x >> 4, z >> 4);
        int column = ((x & 15) * 16 + (z & 15)) * 256;
        for (int y = GEN_DEPTH - 1; y >= 0; y--) {
            BlockState state = buffer.blocks[column + y];
            if (state != null && type.isOpaque().test(state)) return y + 1;
        }
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockMetaBuffer buffer = CelestialGenerators.buffer(body, x >> 4, z >> 4);
        int column = ((x & 15) * 16 + (z & 15)) * 256;
        int minY = level.getMinBuildHeight();
        BlockState[] states = new BlockState[level.getHeight()];
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < states.length; i++) {
            int y = minY + i;
            BlockState s = y >= 0 && y < GEN_DEPTH ? buffer.blocks[column + y] : null;
            states[i] = s == null ? air : s;
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        BlockMetaBuffer buffer = CelestialGenerators.buffer(body, pos.getX() >> 4, pos.getZ() >> 4);
        BiomeGenBaseCelestial biome = buffer.biomes == null ? null : buffer.biomes[(pos.getZ() & 15) * 16 + (pos.getX() & 15)];
        info.add("Celestial body: " + body + (biome != null ? " / " + biome.biomeName + " (" + biome.biomeID + ")" : ""));
    }
}
