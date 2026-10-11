package com.space_m.dim;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.space_m.SpaceMod;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.NoiseGeneratorPerlin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.BiomeGenBaseCelestial} (+ die benutzten Teile von 1.7.10 {@code BiomeGenBase}).
 *
 * <p>Aufteilung in 1.20: Was ein Datapack-Biom ausdruecken kann (Temperatur, Niederschlag, Spawns,
 * {@code getSpawningChance}, Features) steht in {@code data/space_m/worldgen/biome/<name>.json}. Was die
 * Generierung im Code braucht - {@code rootHeight}/{@code heightVariation} (Rauschhoehe), {@code topBlock}/
 * {@code fillerBlock} (Hoehlen, Krater) und {@link #genTerrainBlocks} (Oberflaeche) - lebt hier als Java-Objekt,
 * verknuepft ueber den Biom-Schluessel. Die Original-Biom-ID ({@code SpaceConfig.*Biome}) bleibt als
 * {@link #biomeID} fuer die GenLayer-Logik erhalten (die Schichten rechnen mit int-IDs).
 *
 * <p>Die Spawn-Listen ({@code creatures}/{@code monsters}/... und {@code getSpawnableList}) sind die
 * {@code spawners} des Biom-JSON; {@code checkId}/{@code crashOnBiomeConflict} entfallen (keine ID-Kollisionen
 * mehr moeglich, siehe {@code SpaceConfig}).
 */
public abstract class BiomeGenBaseCelestial {

    private static final Map<ResourceKey<Biome>, BiomeGenBaseCelestial> BY_KEY = new ConcurrentHashMap<>();
    private static final Map<Integer, BiomeGenBaseCelestial> BY_ID = new ConcurrentHashMap<>();

    /** 1.7.10 {@code BiomeGenBase.temperatureNoise}. */
    protected static final NoiseGeneratorPerlin temperatureNoise = new NoiseGeneratorPerlin(new Random(1234L), 1);

    /** 1.7.10 {@code BiomeGenBase.height_Default}. */
    public static final Height height_Default = new Height(0.1F, 0.2F);

    public final int biomeID;
    public final ResourceKey<Biome> key;

    public String biomeName;
    public float rootHeight;
    public float heightVariation;
    public float temperature = 0.5F;
    public float rainfall = 0.5F;
    public boolean enableRain = true;

    private Supplier<BlockState> topSupplier = Blocks.GRASS_BLOCK::defaultBlockState;
    private Supplier<BlockState> fillerSupplier = Blocks.DIRT::defaultBlockState;
    private volatile BlockState topBlock;
    private volatile BlockState fillerBlock;

    /**
     * @param id   Original-Biom-ID (Standardwert aus {@code SpaceConfig}, nur fuer die GenLayer-Logik)
     * @param path Registry-Pfad des Datapack-Bioms {@code space_m:<path>}
     */
    protected BiomeGenBaseCelestial(int id, String path) {
        this.biomeID = id;
        this.key = ResourceKey.create(Registries.BIOME, SpaceMod.id(path));
        this.rootHeight = height_Default.rootHeight;
        this.heightVariation = height_Default.variation;

        BiomeGenBaseCelestial old = BY_ID.put(id, this);
        if (old != null && old != this) throw new IllegalStateException("Celestial biome id " + id + " used twice: " + old.key + " / " + key);
        BY_KEY.put(key, this);
    }

    @Nullable
    public static BiomeGenBaseCelestial byKey(@Nullable ResourceKey<Biome> key) {
        return key == null ? null : BY_KEY.get(key);
    }

    /** 1.7.10 {@code BiomeGenBase.getBiome(id)}. */
    @Nullable
    public static BiomeGenBaseCelestial getBiome(int id) {
        return BY_ID.get(id);
    }

    // ── Original-Setter ──

    protected BiomeGenBaseCelestial setBiomeName(String name) {
        this.biomeName = name;
        return this;
    }

    protected BiomeGenBaseCelestial setDisableRain() {
        this.enableRain = false;
        return this;
    }

    protected BiomeGenBaseCelestial setTemperatureRainfall(float temperature, float rainfall) {
        this.temperature = temperature;
        this.rainfall = rainfall;
        return this;
    }

    protected final BiomeGenBaseCelestial setHeight(Height height) {
        this.rootHeight = height.rootHeight;
        this.heightVariation = height.variation;
        return this;
    }

    /** {@code this.topBlock = ...} (Bloecke werden erst nach der Registrierung aufgeloest). */
    protected void setTopBlock(Supplier<BlockState> top) {
        this.topSupplier = top;
        this.topBlock = null;
    }

    /** {@code this.fillerBlock = ...}. */
    protected void setFillerBlock(Supplier<BlockState> filler) {
        this.fillerSupplier = filler;
        this.fillerBlock = null;
    }

    /** {@code biome.topBlock}. */
    public BlockState topBlock() {
        BlockState s = topBlock;
        if (s == null) topBlock = s = topSupplier.get();
        return s;
    }

    /** {@code biome.fillerBlock}. */
    public BlockState fillerBlock() {
        BlockState s = fillerBlock;
        if (s == null) fillerBlock = s = fillerSupplier.get();
        return s;
    }

    public float getTemperature() {
        return this.temperature;
    }

    /** 1.7.10 {@code BiomeGenBase.getFloatTemperature}. */
    public final float getFloatTemperature(int x, int y, int z) {
        if (y > 64) {
            float f = (float) temperatureNoise.func_151601_a((double) x * 1.0D / 8.0D, (double) z * 1.0D / 8.0D) * 4.0F;
            return this.getTemperature() - (f + (float) y - 64.0F) * 0.05F / 30.0F;
        } else {
            return this.getTemperature();
        }
    }

    /** {@code genTerrainBlocks}: Oberflaeche einer Spalte. Standard = 1.7.10 {@code genBiomeTerrain}. */
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        this.genBiomeTerrain(world, rand, blocks, x, z, noise);
    }

    /**
     * 1:1 1.7.10 {@code BiomeGenBase.genBiomeTerrain} (inkl. des Vanilla-x/z-Tauschs im Pufferindex).
     */
    public final void genBiomeTerrain(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState block = this.topBlock();
        BlockState block1 = this.fillerBlock();
        int k = -1;
        int l = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int i1 = x & 15;
        int j1 = z & 15;
        int k1 = blocks.length / 256;

        for (int l1 = 255; l1 >= 0; --l1) {
            int i2 = (j1 * 16 + i1) * k1 + l1;

            if (l1 <= 0 + rand.nextInt(5)) {
                blocks[i2] = Blocks.BEDROCK.defaultBlockState();
            } else {
                BlockState block2 = blocks[i2];

                if (block2 != null && !block2.isAir()) {
                    if (Legacy.is(block2, stone)) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = null;
                                block1 = stone;
                            } else if (l1 >= 59 && l1 <= 64) {
                                block = this.topBlock();
                                block1 = this.fillerBlock();
                            }

                            if (l1 < 63 && (block == null || block.isAir())) {
                                if (this.getFloatTemperature(x, l1, z) < 0.15F) {
                                    block = Blocks.ICE.defaultBlockState();
                                } else {
                                    block = Blocks.WATER.defaultBlockState();
                                }
                            }

                            k = l;

                            if (l1 >= 62) {
                                blocks[i2] = block;
                            } else if (l1 < 56 - l) {
                                block = null;
                                block1 = stone;
                                blocks[i2] = Blocks.GRAVEL.defaultBlockState();
                            } else {
                                blocks[i2] = block1;
                            }
                        } else if (k > 0) {
                            --k;
                            blocks[i2] = block1;

                            if (k == 0 && Legacy.is(block1, Blocks.SAND)) {
                                k = rand.nextInt(4) + Math.max(0, l1 - 63);
                                block1 = Blocks.SANDSTONE.defaultBlockState();
                            }
                        }
                    }
                } else {
                    k = -1;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "BiomeGenBaseCelestial[" + biomeID + ", " + key.location() + "]";
    }

    /** 1.7.10 {@code BiomeGenBase.Height}. */
    public static class Height {
        public final float rootHeight;
        public final float variation;

        public Height(float rootHeight, float variation) {
            this.rootHeight = rootHeight;
            this.variation = variation;
        }
    }
}
