package com.space_m.test;

import java.util.Arrays;

import com.space_m.SpaceMod;
import com.space_m.dim.CelestialChunkGenerator;
import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.CelestialSeed;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.SpaceDimensions;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.moon.BiomeGenMoon;
import com.space_m.dim.moon.ChunkProviderMoon;
import com.space_m.dim.moon.MunBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
//? if < 1.21.1 {
import net.minecraft.world.level.chunk.ChunkStatus;
//?} else {
/*import net.minecraft.world.level.chunk.status.ChunkStatus;
*///?}
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R1-B Dimensions-Framework: die Mun-Dimension laedt aus dem Datapack, generiert mit dem portierten
 * {@code ChunkProviderMoon} (Hoehe 0-255, Grundgestein unten, Mondgestein/-boden, Erze) und ist unabhaengig vom
 * Generator-Thread deterministisch.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DimensionGameTest {

    private DimensionGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /**
     * Die Mun-Dimension oder {@code null}: Der NeoForge-1.21.1-GameTestServer baut seine Welt nur aus dem
     * Flachland-Preset ({@code WorldDimensions.bake} mit leerer Dimensionsliste) und laedt keine Datapack-Dimensionen
     * (Forge 1.20.1 schon; normale Welten/Server auf beiden Loadern ebenfalls). Dann pruefen die Tests Registry und
     * Generator direkt.
     */
    private static ServerLevel mun(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(SpaceDimensions.MUN);
        if (level == null) {
            com.mojang.logging.LogUtils.getLogger().info("[space_m] GameTestServer without datapack dimensions - checking registries/generator only");
        }
        return level;
    }

    private static void checkRegistries(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var type = access.registryOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE).get(SpaceDimensions.type("mun"));
        check(type != null, "dimension_type space_m:mun missing");
        check(type.minY() == 0 && type.height() == 256 && type.logicalHeight() == 256, "dimension_type mun height 0-255");
        check(SpaceMod.id("mun").equals(type.effectsLocation()), "dimension_type mun effects space_m:mun");
        check(access.registryOrThrow(net.minecraft.core.registries.Registries.BIOME).containsKey(BiomeGenMoon.biome.key), "biome space_m:mun missing");
        check(net.minecraft.core.registries.BuiltInRegistries.CHUNK_GENERATOR.containsKey(SpaceMod.id("celestial")), "chunk generator space_m:celestial not registered");
        check(net.minecraft.core.registries.BuiltInRegistries.BIOME_SOURCE.containsKey(SpaceMod.id("celestial")), "biome source space_m:celestial not registered");
        var placed = access.registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        check(placed.containsKey(SpaceMod.id("mun/ore_lithium")) && placed.containsKey(SpaceMod.id("mun/depth_borax")), "Mun placed features missing");
    }

    /** Dimension, Generator und Hoehe wie im Original (y 0-255). */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 100)
    public static void munDimensionLoads(GameTestHelper helper) {
        checkRegistries(helper);
        ServerLevel level = mun(helper);
        if (level == null) {
            check(CelestialGenerators.get("mun") != null && CelestialGenerators.get("mun").seaLevel() == 64, "body generator mun (seaLevel 64)");
            helper.succeed();
            return;
        }
        check(level.getChunkSource().getGenerator() instanceof CelestialChunkGenerator, "mun uses space_m:celestial, got " + level.getChunkSource().getGenerator());
        check(level.getMinBuildHeight() == 0 && level.getHeight() == 256, "mun height 0-255, got " + level.getMinBuildHeight() + "+" + level.getHeight());
        int sea = level.getChunkSource().getGenerator().getSeaLevel();
        check(sea == 64, "ChunkProviderMoon seaLevel 64, got " + sea);
        helper.succeed();
    }

    /** Gelaende: Grundgestein unten, Mondgestein darueber, Oberflaeche Mondboden/Basalt, Erze dekoriert. */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 400)
    public static void munTerrainGenerates(GameTestHelper helper) {
        ServerLevel level = mun(helper);
        Block moonRock = MunBlocks.MOON_ROCK.get();
        Block turf = com.hbm_m.block.ModBlocks.MOON_TURF.get();
        Block basalt = com.hbm_m.block.ModBlocks.BASALT.get();
        if (level == null) {
            // ohne Welt: Puffer des Generators (Gelaende ohne Dekoration)
            BlockState[] blocks = CelestialGenerators.buffer("mun", 0, 0).blocks;
            int rock = 0, top = 0, bedrock = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (blocks[(x * 16 + z) * 256] != null && blocks[(x * 16 + z) * 256].is(Blocks.BEDROCK)) bedrock++;
                    for (int y = 255; y > 0; y--) {
                        BlockState s = blocks[(x * 16 + z) * 256 + y];
                        if (s == null || s.isAir()) continue;
                        if (s.is(turf) || s.is(basalt) || s.is(moonRock)) top++;
                        break;
                    }
                    for (int y = 1; y < 128; y++) if (blocks[(x * 16 + z) * 256 + y] != null && blocks[(x * 16 + z) * 256 + y].is(moonRock)) rock++;
                }
            }
            check(bedrock == 256 && rock > 4000 && top > 128, "Mun buffer terrain: bedrock " + bedrock + ", rock " + rock + ", top " + top);
            helper.succeed();
            return;
        }
        ChunkAccess chunk = level.getChunk(0, 0);

        int rock = 0, top = 0, bedrock = 0, ores = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (chunk.getBlockState(new BlockPos(x, 0, z)).is(Blocks.BEDROCK)) bedrock++;
                int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                BlockState surface = chunk.getBlockState(new BlockPos(x, h, z));
                if (surface.is(turf) || surface.is(basalt) || surface.is(moonRock)) top++;
                for (int y = 1; y < 128; y++) {
                    BlockState s = chunk.getBlockState(new BlockPos(x, y, z));
                    if (s.is(moonRock)) rock++;
                    else if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().endsWith("_mun")) ores++;
                }
            }
        }
        check(bedrock == 256, "bedrock floor at y 0 (256 columns), got " + bedrock);
        check(rock > 4000, "moon_rock body, got " + rock);
        check(top > 128, "surface moon_turf/basalt/moon_rock in most columns, got " + top);
        check(ores > 0, "Mun ore variants decorated in chunk 0,0, got " + ores);
        helper.succeed();
    }

    /**
     * Sichtpruefung: Draufsicht 512x512 Bloecke um 0,0 als {@code run/space_m_mun_map.png} (Hoehe als Helligkeit,
     * Basalt-Mare dunkel, Mondboden hell) - zum Vergleich mit dem Original-Gelaende.
     */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 400)
    public static void munTerrainMap(GameTestHelper helper) {
        int size = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size * 16, size * 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Block moonRock = MunBlocks.MOON_ROCK.get();
        Block basalt = com.hbm_m.block.ModBlocks.BASALT.get();
        int min = 255, max = 0;
        for (int cx = 0; cx < size; cx++) {
            for (int cz = 0; cz < size; cz++) {
                BlockState[] blocks = CelestialGenerators.buffer("mun", cx - size / 2, cz - size / 2).blocks;
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
                        int shade = Math.max(0, Math.min(255, (y - 30) * 3));
                        int rgb;
                        if (s != null && s.is(basalt)) rgb = (shade / 3) << 16 | (shade / 3) << 8 | (shade / 2);
                        else if (s != null && s.is(moonRock)) rgb = (shade * 3 / 4) << 16 | (shade * 3 / 4) << 8 | (shade * 3 / 4);
                        else if (s != null && s.is(Blocks.BEDROCK)) rgb = 0x400000;
                        else rgb = shade << 16 | shade << 8 | shade;
                        img.setRGB(cx * 16 + x, cz * 16 + z, rgb);
                    }
                }
            }
        }
        try {
            java.io.File out = new java.io.File("space_m_mun_map.png");
            javax.imageio.ImageIO.write(img, "png", out);
            com.mojang.logging.LogUtils.getLogger().info("[space_m] Mun terrain map {} (surface y {}..{})", out.getAbsolutePath(), min, max);
        } catch (java.io.IOException e) {
            throw new GameTestAssertException("map not written: " + e);
        }
        check(max - min > 8 && max < 200 && min > 0, "Mun surface heights plausible, got " + min + ".." + max);
        helper.succeed();
    }

    /** Beute der Mun-Bloecke (Datapack-Loot, auf NeoForge nach Pfad-Umbenennung loot_table). */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 20)
    public static void munBlockDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        java.util.function.BiConsumer<Block, String> drops = (block, expected) -> {
            var list = Block.getDrops(block.defaultBlockState(), level, pos, null);
            check(!list.isEmpty(), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block) + " drops nothing");
            String got = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(list.get(0).getItem()).toString();
            check(got.equals(expected), net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block) + " drops " + got + ", expected " + expected);
        };
        drops.accept(MunBlocks.MOON_ROCK.get(), "space_m:moon_rock");
        drops.accept(MunBlocks.ORE_URANIUM.get(), "hbm_m:uranium_raw");
        drops.accept(MunBlocks.ORE_IRON.get(), "minecraft:raw_iron");
        drops.accept(MunBlocks.ORE_NICKEL.get(), "space_m:ore_nickel_mun");
        drops.accept(MunBlocks.ORE_BRINE.get(), "space_m:ore_brine_empty_mun");
        helper.succeed();
    }

    /**
     * NBT-Strukturen der Mun (W-B1): {@code space_m:mun_structures} ({@code hbm_m:ntm}, Dimension 413015) erzeugt im
     * Gitter Mun-Basen (Oberflaeche, Beton) - geprueft im 128x128-Chunk-Feld um 0,0, dann komplett generiert.
     */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 2400)
    public static void munStructuresGenerate(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var structKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, SpaceMod.id("mun_structures"));
        var setKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE_SET, SpaceMod.id("mun_structures"));
        var structures = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        check(structures.containsKey(structKey), "structure space_m:mun_structures missing");
        check(access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).containsKey(setKey), "structure_set space_m:mun_structures missing");
        ServerLevel level = mun(helper);
        if (level == null) {
            helper.succeed();
            return;
        }
        var set = access.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).getHolderOrThrow(setKey);
        var state = level.getChunkSource().getGeneratorState();
        var structure = structures.get(structKey);
        int cells = 0, starts = 0, bases = 0;
        net.minecraft.world.level.levelgen.structure.BoundingBox baseBox = null;
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                if (!state.hasStructureChunkInRange(set, x, z, 0)) continue;
                cells++;
                ChunkAccess c = level.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS, true);
                var start = c.getStartForStructure(structure);
                if (start == null || !start.isValid()) continue;
                starts++;
                if (start.getBoundingBox().minY() > 40) { // Mun-Basis an der Oberflaeche, Meteor-Verlies bei y 32
                    bases++;
                    if (baseBox == null) baseBox = start.getBoundingBox();
                }
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("[space_m] Mun structures: {} grid cells, {} starts, {} surface bases, first {}", cells, starts, bases, baseBox);
        check(bases > 0, "no Mun base in 128x128 chunks (" + cells + " cells, " + starts + " starts)");
        int concrete = 0;
        Block smooth = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", "concrete_smooth"));
        for (int cx = baseBox.minX() >> 4; cx <= baseBox.maxX() >> 4; cx++) {
            for (int cz = baseBox.minZ() >> 4; cz <= baseBox.maxZ() >> 4; cz++) {
                ChunkAccess c = level.getChunk(cx, cz);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = baseBox.minY(); y <= baseBox.maxY(); y++)
                    if (c.getBlockState(new BlockPos((cx << 4) + x, y, (cz << 4) + z)).is(smooth)) concrete++;
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("[space_m] Mun base at {}: {} concrete_smooth blocks", baseBox.getCenter(), concrete);
        check(concrete > 20, "Mun base built (concrete_smooth > 20), got " + concrete);
        helper.succeed();
    }

    /** Zwei unabhaengige Generator-Instanzen (= zwei Threads) liefern denselben Chunk (MapGenGreg-Seeding usw.). */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 200)
    public static void munGeneratorDeterministic(GameTestHelper helper) {
        long seed = CelestialSeed.get();
        for (int[] c : new int[][] {{3, -7}, {-40, 12}}) {
            ChunkProviderCelestial a = new ChunkProviderMoon(WorldChunkManagerCelestial.fixed(BiomeGenMoon.biome, seed), seed);
            ChunkProviderCelestial b = new ChunkProviderMoon(WorldChunkManagerCelestial.fixed(BiomeGenMoon.biome, seed), seed);
            b.provideChunk(c[0] + 5, c[1] - 2); // anderer Zustand vorher
            check(Arrays.equals(a.provideChunk(c[0], c[1]).blocks, b.provideChunk(c[0], c[1]).blocks), "chunk " + c[0] + "," + c[1] + " differs between instances");
            check(Arrays.equals(a.provideChunk(c[0], c[1]).blocks, CelestialGenerators.buffer("mun", c[0], c[1]).blocks), "cached buffer differs");
        }
        helper.succeed();
    }
}
