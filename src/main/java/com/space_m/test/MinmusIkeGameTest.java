package com.space_m.test;

import java.util.Arrays;

import com.space_m.SpaceMod;
import com.space_m.dim.CelestialChunkGenerator;
import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.CelestialSeed;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.SpaceDimensions;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.ike.BiomeGenIke;
import com.space_m.dim.ike.ChunkProviderIke;
import com.space_m.dim.ike.IkeBlocks;
import com.space_m.dim.minmus.BiomeGenBaseMinmus;
import com.space_m.dim.minmus.ChunkProviderMinmus;
import com.space_m.dim.minmus.MinmusBlocks;
import com.space_m.dim.minmus.WorldProviderMinmus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
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
 * R3-1 Minmus + Ike: Dimensionen aus dem Datapack, portierte ChunkProvider ({@code ChunkProviderMinmus} mit
 * GenLayer-Kette Becken/Huegel, {@code ChunkProviderIke} mit Felsnadeln/Sellafield-Kratern), Determinismus,
 * Beute und Draufsicht-Karten {@code run/space_m_minmus_map.png} / {@code run/space_m_ike_map.png}.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MinmusIkeGameTest {

    private MinmusIkeGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkBody(GameTestHelper helper, String body, String... biomes) {
        var access = helper.getLevel().getServer().registryAccess();
        var type = access.registryOrThrow(Registries.DIMENSION_TYPE).get(SpaceDimensions.type(body));
        check(type != null, "dimension_type space_m:" + body + " missing");
        check(type.minY() == 0 && type.height() == 256, "dimension_type " + body + " height 0-255");
        check(SpaceMod.id(body).equals(type.effectsLocation()), "dimension_type " + body + " effects space_m:" + body);
        for (String b : biomes) check(access.registryOrThrow(Registries.BIOME).containsKey(SpaceMod.id(b)), "biome space_m:" + b + " missing");
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);
        for (String f : new String[] {"ore_uranium", "cave_sulfur", "layer_conglomerate", "keyhole", "calcium"})
            check(placed.containsKey(SpaceMod.id(body + "/" + f)), "placed feature " + body + "/" + f + " missing");
        check(CelestialGenerators.get(body) != null && CelestialGenerators.get(body).seaLevel() == 63, "body generator " + body);
        ServerLevel level = helper.getLevel().getServer().getLevel(SpaceDimensions.level(body));
        if (level != null) {
            check(level.getChunkSource().getGenerator() instanceof CelestialChunkGenerator, body + " uses space_m:celestial");
            check(level.getMinBuildHeight() == 0 && level.getHeight() == 256, body + " height 0-255");
        }
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim_r3a", timeoutTicks = 100)
    public static void minmusIkeRegistries(GameTestHelper helper) {
        checkBody(helper, "minmus", "minmus_hills", "minmus_basins");
        checkBody(helper, "ike", "ike");
        var placed = helper.getLevel().getServer().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        check(placed.containsKey(SpaceMod.id("minmus/ore_copper_minmus")) && placed.containsKey(SpaceMod.id("minmus/layer_smooth")), "Minmus features");
        check(placed.containsKey(SpaceMod.id("ike/decorator")) && placed.containsKey(SpaceMod.id("ike/pedestal")) && placed.containsKey(SpaceMod.id("ike/ore_mineral")), "Ike features");
        check(BuiltInRegistries.FEATURE.containsKey(SpaceMod.id("ore_layer_3d")) && BuiltInRegistries.FEATURE.containsKey(SpaceMod.id("ore_cave")), "ore_layer_3d/ore_cave");
        check(BuiltInRegistries.FLUID.containsKey(SpaceMod.id("bromine_fluid")) && BuiltInRegistries.BLOCK.containsKey(SpaceMod.id("bromine_block")), "bromine fluid");
        helper.succeed();
    }

    private static final java.util.Map<String, Integer> TOPS = new java.util.TreeMap<>();

    private static int[] scan(String body, Block stone, Block top) {
        TOPS.clear();
        int bedrock = 0, rock = 0, surf = 0;
        for (int cx = 0; cx < 2; cx++) for (int cz = 0; cz < 2; cz++) {
            BlockState[] blocks = CelestialGenerators.buffer(body, cx, cz).blocks;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int col = (x * 16 + z) * 256;
                if (blocks[col] != null && blocks[col].is(Blocks.BEDROCK)) bedrock++;
                for (int y = 1; y < 128; y++) if (blocks[col + y] != null && blocks[col + y].is(stone)) rock++;
                for (int y = 255; y > 0; y--) {
                    BlockState s = blocks[col + y];
                    if (s == null || s.isAir()) continue;
                    // Oberflaeche: Regolith/Gestein, auf Ike auch Sellafield-Krater (seedabhaengig, r bis 64)
                    if (s.is(top) || s.is(stone) || s.is(com.hbm_m.block.ModBlocks.SELLAFIELD_0.get()) || s.is(com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get())
                            || s.is(MinmusBlocks.MINMUS_SMOOTH.get())) surf++;
                    TOPS.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath() + "@" + (y / 16 * 16), 1, Integer::sum);
                    break;
                }
            }
        }
        return new int[] {bedrock, rock, surf};
    }

    /** Gelaende-Puffer: Grundgestein unten, Gestein darunter, Regolith/Minzeis an der Oberflaeche. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3a", timeoutTicks = 400)
    public static void minmusIkeTerrain(GameTestHelper helper) {
        int[] m = scan("minmus", MinmusBlocks.MINMUS_STONE.get(), MinmusBlocks.MINMUS_REGOLITH.get());
        int[] k = scan("ike", IkeBlocks.IKE_STONE.get(), IkeBlocks.IKE_REGOLITH.get());
        com.mojang.logging.LogUtils.getLogger().info("[space_m] Ike surface blocks {}", TOPS);
        check(m[0] == 1024 && m[1] > 8000 && m[2] > 512, "Minmus buffer: bedrock " + m[0] + ", stone " + m[1] + ", surface " + m[2]);
        check(k[0] == 1024 && k[1] > 8000 && k[2] > 512, "Ike buffer: bedrock " + k[0] + ", stone " + k[1] + ", surface " + k[2] + " " + TOPS);
        helper.succeed();
    }

    /** GenLayer-Kette: beide Minmus-Biome kommen vor (Becken und Huegel). */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3a", timeoutTicks = 200)
    public static void minmusBiomes(GameTestHelper helper) {
        long seed = CelestialSeed.get();
        WorldChunkManagerCelestial wcm = new WorldChunkManagerCelestial(WorldProviderMinmus.createBiomeGenerators(seed), seed, BiomeGenBaseMinmus.minmusPlains);
        int plains = 0, basins = 0;
        var arr = wcm.loadBlockGeneratorData(null, -1024, -1024, 2048, 2048);
        for (var b : arr) {
            if (b == BiomeGenBaseMinmus.minmusPlains) plains++;
            else if (b == BiomeGenBaseMinmus.minmusCanyon) basins++;
        }
        check(plains > 0 && basins > 0, "Minmus biomes hills " + plains + " / basins " + basins);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim_r3a", timeoutTicks = 200)
    public static void minmusIkeDeterministic(GameTestHelper helper) {
        long seed = CelestialSeed.get();
        for (int[] c : new int[][] {{3, -7}, {-40, 12}}) {
            ChunkProviderCelestial a = new ChunkProviderMinmus(new WorldChunkManagerCelestial(WorldProviderMinmus.createBiomeGenerators(seed), seed, BiomeGenBaseMinmus.minmusPlains), seed);
            ChunkProviderCelestial b = new ChunkProviderMinmus(new WorldChunkManagerCelestial(WorldProviderMinmus.createBiomeGenerators(seed), seed, BiomeGenBaseMinmus.minmusPlains), seed);
            b.provideChunk(c[0] + 5, c[1] - 2);
            check(Arrays.equals(a.provideChunk(c[0], c[1]).blocks, b.provideChunk(c[0], c[1]).blocks), "Minmus chunk " + c[0] + "," + c[1] + " differs");
            check(Arrays.equals(a.provideChunk(c[0], c[1]).blocks, CelestialGenerators.buffer("minmus", c[0], c[1]).blocks), "Minmus cached buffer differs");

            ChunkProviderCelestial i = new ChunkProviderIke(WorldChunkManagerCelestial.fixed(BiomeGenIke.biome, seed), seed);
            ChunkProviderCelestial j = new ChunkProviderIke(WorldChunkManagerCelestial.fixed(BiomeGenIke.biome, seed), seed);
            j.provideChunk(c[0] - 3, c[1] + 9);
            check(Arrays.equals(i.provideChunk(c[0], c[1]).blocks, j.provideChunk(c[0], c[1]).blocks), "Ike chunk " + c[0] + "," + c[1] + " differs");
            check(Arrays.equals(i.provideChunk(c[0], c[1]).blocks, CelestialGenerators.buffer("ike", c[0], c[1]).blocks), "Ike cached buffer differs");
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim_r3a", timeoutTicks = 20)
    public static void minmusIkeDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        java.util.function.BiConsumer<Block, String> drops = (block, expected) -> {
            var list = Block.getDrops(block.defaultBlockState(), level, pos, null);
            check(!list.isEmpty(), BuiltInRegistries.BLOCK.getKey(block) + " drops nothing");
            String got = BuiltInRegistries.ITEM.getKey(list.get(0).getItem()).toString();
            check(got.equals(expected), BuiltInRegistries.BLOCK.getKey(block) + " drops " + got + ", expected " + expected);
        };
        drops.accept(MinmusBlocks.MINMUS_STONE.get(), "space_m:minmus_stone");
        drops.accept(MinmusBlocks.MINMUS_SMOOTH.get(), "space_m:minmus_smooth");
        drops.accept(MinmusBlocks.STONE_RESOURCE_CONGLOMERATE.get(), "space_m:stone_resource_conglomerate");
        drops.accept(MinmusBlocks.ORE_URANIUM.get(), "hbm_m:uranium_raw");
        drops.accept(MinmusBlocks.ORE_BRINE.get(), "space_m:ore_brine_empty_minmus");
        drops.accept(IkeBlocks.IKE_REGOLITH.get(), "space_m:ike_regolith");
        drops.accept(IkeBlocks.ORE_MINERAL.get(), "space_m:ore_mineral_ike");
        drops.accept(IkeBlocks.ORE_IRON.get(), "minecraft:raw_iron");
        helper.succeed();
    }

    private static void map(String body, Block[] colored, int[] colors) {
        int size = 32;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size * 16, size * 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
        int min = 255, max = 0;
        for (int cx = 0; cx < size; cx++) for (int cz = 0; cz < size; cz++) {
            BlockState[] blocks = CelestialGenerators.buffer(body, cx - size / 2, cz - size / 2).blocks;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
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
                int rgb = shade << 16 | shade << 8 | shade;
                for (int i = 0; i < colored.length; i++) {
                    if (s != null && s.is(colored[i])) {
                        int c = colors[i];
                        rgb = (((c >> 16) & 255) * shade / 255) << 16 | (((c >> 8) & 255) * shade / 255) << 8 | ((c & 255) * shade / 255);
                        break;
                    }
                }
                img.setRGB(cx * 16 + x, cz * 16 + z, rgb);
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

    /** Sichtpruefung: Draufsicht 512x512 Bloecke um 0,0 (Hoehe = Helligkeit; Minzeis tuerkis, Sellafit gruen). */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3a", timeoutTicks = 600)
    public static void minmusIkeMaps(GameTestHelper helper) {
        map("minmus", new Block[] {MinmusBlocks.MINMUS_SMOOTH.get(), MinmusBlocks.MINMUS_STONE.get()}, new int[] {0x80FFE0, 0xC0D8D0});
        map("ike", new Block[] {com.hbm_m.block.ModBlocks.SELLAFIELD_0.get(), com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get(), IkeBlocks.IKE_STONE.get()},
                new int[] {0x40FF40, 0x208020, 0xA0A0B0});
        helper.succeed();
    }
}
