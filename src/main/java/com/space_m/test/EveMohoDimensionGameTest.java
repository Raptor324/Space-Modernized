package com.space_m.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.space_m.SpaceMod;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialChunkGenerator;
import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.CelestialSeed;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.SpaceDimensions;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.eve.ChunkProviderEve;
import com.space_m.dim.eve.EveBlocks;
import com.space_m.dim.eve.WorldProviderEve;
import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.moho.ChunkProviderMoho;
import com.space_m.dim.moho.MohoBlocks;
import com.space_m.dim.moho.WorldProviderMoho;
import com.space_m.dim.moho.biome.BiomeGenBaseMoho;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R3-3: Eve und Moho laden aus dem Datapack, generieren mit den 1:1 portierten ChunkProvidern/GenLayer-Ketten
 * (alle Biome kommen vor, Gestein/Oberflaeche/Meere stimmen), sind threadunabhaengig deterministisch, und die
 * Bloecke droppen wie im Original. Dazu je eine Draufsicht-Karte ({@code run/space_m_eve_map.png},
 * {@code run/space_m_moho_map.png}).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EveMohoDimensionGameTest {

    private EveMohoDimensionGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkRegistries(GameTestHelper helper, String body, BiomeGenBaseCelestial... biomes) {
        var access = helper.getLevel().getServer().registryAccess();
        var type = access.registryOrThrow(Registries.DIMENSION_TYPE).get(SpaceDimensions.type(body));
        check(type != null, "dimension_type space_m:" + body + " missing");
        check(type.minY() == 0 && type.height() == 256, "dimension_type " + body + " height 0-255");
        check(SpaceMod.id(body).equals(type.effectsLocation()), "dimension_type " + body + " effects space_m:" + body);
        for (BiomeGenBaseCelestial b : biomes) {
            check(access.registryOrThrow(Registries.BIOME).containsKey(b.key), "biome " + b.key.location() + " missing");
        }
        check(CelestialGenerators.get(body) != null && CelestialGenerators.get(body).seaLevel() == 63, "body generator " + body + " (seaLevel 63)");
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);
        check(placed.containsKey(SpaceMod.id(body + "/decorator")) && placed.containsKey(SpaceMod.id(body + "/surface")), body + " placed features missing");
    }

    private static void checkLevel(GameTestHelper helper, ResourceKey<Level> key) {
        ServerLevel level = helper.getLevel().getServer().getLevel(key);
        if (level == null) return; // NeoForge-GameTestServer laedt keine Datapack-Dimensionen (siehe DimensionGameTest)
        check(level.getChunkSource().getGenerator() instanceof CelestialChunkGenerator, key.location() + " uses space_m:celestial");
        check(level.getMinBuildHeight() == 0 && level.getHeight() == 256, key.location() + " height 0-255");
        check(level.getChunkSource().getGenerator().getSeaLevel() == 63, key.location() + " seaLevel 63");
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim_eve_moho", timeoutTicks = 100)
    public static void eveMohoDimensionsLoad(GameTestHelper helper) {
        checkRegistries(helper, "eve", BiomeGenBaseEve.evePlains, BiomeGenBaseEve.eveOcean, BiomeGenBaseEve.eveMountains,
                BiomeGenBaseEve.eveSeismicPlains, BiomeGenBaseEve.eveRiver);
        checkRegistries(helper, "moho", BiomeGenBaseMoho.mohoCrag, BiomeGenBaseMoho.mohoBasalt, BiomeGenBaseMoho.mohoLavaSea,
                BiomeGenBaseMoho.mohoPlateau);
        checkLevel(helper, SpaceDimensions.EVE);
        checkLevel(helper, SpaceDimensions.MOHO);
        check(BuiltInRegistries.FLUID.containsKey(SpaceMod.id("mercury_fluid")), "mercury_fluid not registered");
        helper.succeed();
    }

    /** Alle Biome der GenLayer-Ketten kommen in einem 4096x4096-Feld vor (Detailschicht, wie die gespeicherten Biome). */
    @GameTest(template = "empty3x3x3", batch = "space_dim_eve_moho", timeoutTicks = 400)
    public static void eveMohoBiomeDistribution(GameTestHelper helper) {
        long seed = CelestialSeed.get();
        Set<BiomeGenBaseCelestial> eve = new HashSet<>();
        Set<BiomeGenBaseCelestial> moho = new HashSet<>();
        WorldChunkManagerCelestial e = new WorldChunkManagerCelestial(WorldProviderEve.createBiomeGenerators(seed), seed, BiomeGenBaseEve.evePlains);
        WorldChunkManagerCelestial m = new WorldChunkManagerCelestial(WorldProviderMoho.createBiomeGenerators(seed), seed, BiomeGenBaseMoho.mohoCrag);
        for (int x = -2048; x < 2048; x += 64) {
            for (int z = -2048; z < 2048; z += 64) {
                eve.add(e.getBiomeGenForCoords(x, z));
                moho.add(m.getBiomeGenForCoords(x, z));
            }
        }
        check(eve.containsAll(Set.of(BiomeGenBaseEve.evePlains, BiomeGenBaseEve.eveOcean, BiomeGenBaseEve.eveMountains,
                BiomeGenBaseEve.eveSeismicPlains, BiomeGenBaseEve.eveRiver)), "Eve biomes " + eve);
        check(moho.containsAll(Set.of(BiomeGenBaseMoho.mohoCrag, BiomeGenBaseMoho.mohoBasalt, BiomeGenBaseMoho.mohoLavaSea,
                BiomeGenBaseMoho.mohoPlateau)), "Moho biomes " + moho);
        helper.succeed();
    }

    /** Gelaende: Grundgestein unten, Koerpergestein darueber, Oberflaeche aus den Original-Bloecken, Meere. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_eve_moho", timeoutTicks = 400)
    public static void eveMohoTerrain(GameTestHelper helper) {
        Block eveRock = EveBlocks.EVE_ROCK.get();
        Block mercury = EveBlocks.MERCURY_BLOCK.get();
        Block mohoStone = MohoBlocks.MOHO_STONE.get();
        int[] e = scan("eve", eveRock, mercury);
        int[] m = scan("moho", mohoStone, Blocks.LAVA);
        check(e[0] == 64 * 256 && e[1] > 64 * 2000, "Eve buffer: bedrock " + e[0] + ", rock " + e[1]);
        check(e[2] > 0, "Eve: no mercury seas in 8x8 chunks around 0,0 and 64,64? got " + e[2]);
        check(m[0] == 64 * 256 && m[1] > 64 * 2000, "Moho buffer: bedrock " + m[0] + ", rock " + m[1]);
        helper.succeed();
    }

    /** {bedrock-Spalten, Gestein y1-127, Meeresbloecke} ueber 64 Chunks. */
    private static int[] scan(String body, Block rock, Block sea) {
        int bedrock = 0, stone = 0, seaBlocks = 0;
        for (int cx = 0; cx < 8; cx++) {
            for (int cz = 0; cz < 8; cz++) {
                BlockState[] blocks = CelestialGenerators.buffer(body, cx * 8 - 32, cz * 8 - 32).blocks;
                for (int c = 0; c < 256; c++) {
                    if (blocks[c * 256] != null && blocks[c * 256].is(Blocks.BEDROCK)) bedrock++;
                    for (int y = 1; y < 128; y++) {
                        BlockState s = blocks[c * 256 + y];
                        if (s == null) continue;
                        if (s.is(rock)) stone++;
                        else if (s.is(sea)) seaBlocks++;
                    }
                }
            }
        }
        return new int[] {bedrock, stone, seaBlocks};
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim_eve_moho", timeoutTicks = 600)
    public static void eveTerrainMap(GameTestHelper helper) {
        map("eve", EveBlocks.EVE_ROCK.get(), EveBlocks.EVE_SILT.get(), EveBlocks.MERCURY_BLOCK.get(), 0x6A4C8D, 0xB48CD2, 0x8C8C9C);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim_eve_moho", timeoutTicks = 600)
    public static void mohoTerrainMap(GameTestHelper helper) {
        map("moho", MohoBlocks.MOHO_STONE.get(), MohoBlocks.MOHO_REGOLITH.get(), Blocks.LAVA, 0x7C6658, 0xA08068, 0xFF6010);
        helper.succeed();
    }

    /**
     * Sichtpruefung: Draufsicht 512x512 Bloecke um 0,0 als {@code space_m_<body>_map.png} - Farbe je Oberflaechenblock
     * (Gestein, Oberflaeche, Meer, Basalt dunkel), Helligkeit = Hoehe.
     */
    private static void map(String body, Block rock, Block surface, Block sea, int rockRgb, int surfRgb, int seaRgb) {
        int size = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size * 16, size * 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Block basalt = com.hbm_m.block.ModBlocks.BASALT.get();
        int min = 255, max = 0;
        for (int cx = 0; cx < size; cx++) {
            for (int cz = 0; cz < size; cz++) {
                BlockState[] blocks = CelestialGenerators.buffer(body, cx - size / 2, cz - size / 2).blocks;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int y = 255;
                        BlockState s = null;
                        while (y > 0) {
                            s = blocks[(x * 16 + z) * 256 + y];
                            if (s != null && !s.isAir()) break;
                            y--;
                        }
                        min = Math.min(min, y);
                        max = Math.max(max, y);
                        int base;
                        if (s == null) base = 0;
                        else if (s.is(sea)) base = seaRgb;
                        else if (s.is(surface)) base = surfRgb;
                        else if (s.is(rock)) base = rockRgb;
                        else if (s.is(basalt)) base = 0x303038;
                        else if (s.is(Blocks.LAVA)) base = 0xFF6010;
                        else base = 0xC0C0C0;
                        float f = Math.max(0.35F, Math.min(1.3F, 0.4F + (y - 40) / 80F));
                        int r = Math.min(255, (int) (((base >> 16) & 255) * f));
                        int g = Math.min(255, (int) (((base >> 8) & 255) * f));
                        int b = Math.min(255, (int) ((base & 255) * f));
                        img.setRGB(cx * 16 + x, cz * 16 + z, r << 16 | g << 8 | b);
                    }
                }
            }
        }
        try {
            java.io.File out = new java.io.File("space_m_" + body + "_map.png");
            javax.imageio.ImageIO.write(img, "png", out);
            com.mojang.logging.LogUtils.getLogger().info("[space_m] {} terrain map {} (surface y {}..{})", body, out.getAbsolutePath(), min, max);
        } catch (java.io.IOException e) {
            throw new GameTestAssertException("map not written: " + e);
        }
        check(max - min > 8 && max < 250 && min > 0, body + " surface heights plausible, got " + min + ".." + max);
    }

    /** Beute (Datapack-Loot): Gestein selbst, Erzvarianten wie das Original. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_eve_moho", timeoutTicks = 20)
    public static void eveMohoBlockDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        java.util.function.BiConsumer<Block, String> drops = (block, expected) -> {
            var list = Block.getDrops(block.defaultBlockState(), level, pos, null);
            check(!list.isEmpty(), BuiltInRegistries.BLOCK.getKey(block) + " drops nothing");
            String got = BuiltInRegistries.ITEM.getKey(list.get(0).getItem()).toString();
            check(got.equals(expected), BuiltInRegistries.BLOCK.getKey(block) + " drops " + got + ", expected " + expected);
        };
        drops.accept(EveBlocks.EVE_ROCK.get(), "space_m:eve_rock");
        drops.accept(EveBlocks.EVE_SILT.get(), "space_m:eve_silt");
        drops.accept(EveBlocks.ORE_GAS.get(), "space_m:ore_gas_empty_eve");
        drops.accept(EveBlocks.ORE_NIOBIUM.get(), "space_m:ore_niobium_eve");
        drops.accept(EveBlocks.GEYSIR_ELECTRIC.get(), "space_m:geysir_electric");
        drops.accept(MohoBlocks.MOHO_STONE.get(), "space_m:moho_stone");
        drops.accept(MohoBlocks.ORE_GLOWSTONE.get(), "minecraft:glowstone_dust");
        drops.accept(MohoBlocks.ORE_URANIUM.get(), "hbm_m:uranium_raw");
        helper.succeed();
    }

    /** Zwei unabhaengige Generator-Instanzen (= zwei Threads) liefern denselben Chunk (auch Risse/Basaltsprenkel). */
    @GameTest(template = "empty3x3x3", batch = "space_dim_eve_moho", timeoutTicks = 300)
    public static void eveMohoGeneratorDeterministic(GameTestHelper helper) {
        long seed = CelestialSeed.get();
        for (int[] c : new int[][] {{3, -7}, {-40, 12}, {100, 100}}) {
            ChunkProviderCelestial a = new ChunkProviderEve(new WorldChunkManagerCelestial(WorldProviderEve.createBiomeGenerators(seed), seed, BiomeGenBaseEve.evePlains), seed);
            ChunkProviderCelestial b = new ChunkProviderEve(new WorldChunkManagerCelestial(WorldProviderEve.createBiomeGenerators(seed), seed, BiomeGenBaseEve.evePlains), seed);
            b.provideChunk(c[0] + 5, c[1] - 2);
            check(Arrays.equals(a.provideChunk(c[0], c[1]).blocks, b.provideChunk(c[0], c[1]).blocks), "Eve chunk " + c[0] + "," + c[1] + " differs between instances");
            check(Arrays.equals(a.provideChunk(c[0], c[1]).blocks, CelestialGenerators.buffer("eve", c[0], c[1]).blocks), "Eve cached buffer differs");

            ChunkProviderCelestial m1 = new ChunkProviderMoho(new WorldChunkManagerCelestial(WorldProviderMoho.createBiomeGenerators(seed), seed, BiomeGenBaseMoho.mohoCrag), seed);
            ChunkProviderCelestial m2 = new ChunkProviderMoho(new WorldChunkManagerCelestial(WorldProviderMoho.createBiomeGenerators(seed), seed, BiomeGenBaseMoho.mohoCrag), seed);
            m2.provideChunk(c[0] - 3, c[1] + 9);
            check(Arrays.equals(m1.provideChunk(c[0], c[1]).blocks, m2.provideChunk(c[0], c[1]).blocks), "Moho chunk " + c[0] + "," + c[1] + " differs between instances");
        }
        helper.succeed();
    }
}
