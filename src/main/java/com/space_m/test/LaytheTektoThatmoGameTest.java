package com.space_m.test;

import java.util.HashMap;
import java.util.Map;

import com.space_m.SpaceMod;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialChunkGenerator;
import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.ChunkProviderCelestial.BlockMetaBuffer;
import com.space_m.dim.SpaceDimensions;
import com.space_m.dim.laythe.LaytheBlocks;
import com.space_m.dim.laythe.biome.BiomeGenBaseLaythe;
import com.space_m.dim.tekto.TektoBlocks;
import com.space_m.dim.tekto.biome.BiomeGenBaseTekto;
import com.space_m.dim.thatmo.BiomeGenThatmo;

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
import net.minecraft.world.level.chunk.ChunkAccess;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R3-4: Laythe, Tekto, Thatmo - Dimensionen/Biome/Features im Datapack, Generatoren 1:1 (Gelaende aus dem Puffer,
 * GenLayer-Biome), Dekoration in der echten Welt (nur wenn der GameTestServer Datapack-Dimensionen laedt, s.
 * {@code DimensionGameTest}), Beute, Draufsicht-Karten {@code run/space_m_<koerper>_map.png}.
 *
 * <p>Laythe-Polar, Tekto und Thatmo wuerfeln Oberflaechenbloecke mit {@code Math.random()} (wie im Original) - ihre
 * Chunks sind daher nicht seed-deterministisch; geprueft werden Bloecke, Hoehen und Biome.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaytheTektoThatmoGameTest {

    private LaytheTektoThatmoGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkRegistries(GameTestHelper helper, String body, BiomeGenBaseCelestial[] biomes, String... features) {
        var access = helper.getLevel().getServer().registryAccess();
        var type = access.registryOrThrow(Registries.DIMENSION_TYPE).get(SpaceDimensions.type(body));
        check(type != null, "dimension_type space_m:" + body + " missing");
        check(type.minY() == 0 && type.height() == 256, "dimension_type " + body + " height 0-255");
        check(SpaceMod.id(body).equals(type.effectsLocation()), "dimension_type " + body + " effects space_m:" + body);
        for (BiomeGenBaseCelestial b : biomes) {
            check(access.registryOrThrow(Registries.BIOME).containsKey(b.key), "biome " + b.key.location() + " missing");
        }
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);
        for (String f : features) check(placed.containsKey(SpaceMod.id(f)), "placed feature space_m:" + f + " missing");
        check(CelestialGenerators.get(body) != null, "body generator " + body);
    }

    /** Biomhaeufigkeit der GenLayer-Kette ueber 2048x2048 Bloecke (Schritt 16). */
    private static Map<BiomeGenBaseCelestial, Integer> biomeCounts(String body) {
        Map<BiomeGenBaseCelestial, Integer> counts = new HashMap<>();
        var wcm = CelestialGenerators.state(body).chunkManager;
        for (int x = -1024; x < 1024; x += 16) {
            for (int z = -1024; z < 1024; z += 16) {
                counts.merge(wcm.getBiomeGenForCoords(x, z), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static int count(BlockState[] blocks, Block b, int yMin, int yMax) {
        int n = 0;
        for (int c = 0; c < 256; c++) for (int y = yMin; y < yMax; y++) {
            BlockState s = blocks[c * 256 + y];
            if (s != null && s.is(b)) n++;
        }
        return n;
    }

    private static int bedrock(BlockState[] blocks) {
        int n = 0;
        for (int c = 0; c < 256; c++) if (blocks[c * 256] != null && blocks[c * 256].is(Blocks.BEDROCK)) n++;
        return n;
    }

    // ───────────────────────── Laythe ─────────────────────────

    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 100)
    public static void laytheRegistries(GameTestHelper helper) {
        checkRegistries(helper, "laythe", new BiomeGenBaseCelestial[] {BiomeGenBaseLaythe.laytheIsland, BiomeGenBaseLaythe.laytheOcean,
                BiomeGenBaseLaythe.laythePolar, BiomeGenBaseLaythe.laytheCoast},
                "laythe/water_plants", "laythe/water_coral", "laythe/vanilla_biome_ores", "laythe/laythe_asbestos", "laythe/ore_uranium", "laythe/depth_borax");
        check(CelestialGenerators.get("laythe").seaLevel() == 63, "Laythe sea level 63");
        helper.succeed();
    }

    /** GenLayer: Inseln, Sagan-See, Riff, Pole; Ozean ueberwiegt (Wasserwelt). */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 400)
    public static void laytheBiomes(GameTestHelper helper) {
        Map<BiomeGenBaseCelestial, Integer> c = biomeCounts("laythe");
        for (BiomeGenBaseCelestial b : new BiomeGenBaseCelestial[] {BiomeGenBaseLaythe.laytheIsland, BiomeGenBaseLaythe.laytheOcean, BiomeGenBaseLaythe.laytheCoast}) {
            check(c.getOrDefault(b, 0) > 0, "Laythe biome " + b.biomeName + " never generated: " + c);
        }
        check(c.getOrDefault(BiomeGenBaseLaythe.laytheOcean, 0) > c.getOrDefault(BiomeGenBaseLaythe.laytheIsland, 0), "Laythe: ocean should dominate: " + c);
        helper.succeed();
    }

    /** Puffer: Grundgestein, Wasser bis y 62, Laythe-Schlick am Meeresgrund. */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 400)
    public static void laytheTerrain(GameTestHelper helper) {
        int water = 0, silt = 0, bed = 0;
        for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) {
            BlockState[] b = CelestialGenerators.buffer("laythe", cx * 7, cz * 7).blocks;
            water += count(b, Blocks.WATER, 0, 64);
            silt += count(b, LaytheBlocks.LAYTHE_SILT.get(), 0, 256);
            bed += bedrock(b);
            check(count(b, Blocks.WATER, 64, 256) == 0, "Laythe: no sea water above y 63");
        }
        check(bed == 25 * 256, "Laythe bedrock floor, got " + bed);
        check(water > 25 * 256 * 5, "Laythe ocean water, got " + water);
        check(silt > 1000, "Laythe silt, got " + silt);
        helper.succeed();
    }

    // ───────────────────────── Tekto ─────────────────────────

    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 100)
    public static void tektoRegistries(GameTestHelper helper) {
        checkRegistries(helper, "tekto", new BiomeGenBaseCelestial[] {BiomeGenBaseTekto.polyvinylPlains, BiomeGenBaseTekto.halogenHills,
                BiomeGenBaseTekto.tetrachloricRiver, BiomeGenBaseTekto.forest, BiomeGenBaseTekto.vinylsands},
                "tekto/rubber_plants", "tekto/tekto_decorator", "tekto/vanilla_ore_iron", "tekto/ore_cobalt");
        check(BuiltInRegistries.FLUID.containsKey(SpaceMod.id("ccl_fluid")), "ccl_fluid not registered");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 400)
    public static void tektoBiomes(GameTestHelper helper) {
        Map<BiomeGenBaseCelestial, Integer> c = biomeCounts("tekto");
        for (BiomeGenBaseCelestial b : new BiomeGenBaseCelestial[] {BiomeGenBaseTekto.polyvinylPlains, BiomeGenBaseTekto.halogenHills,
                BiomeGenBaseTekto.tetrachloricRiver, BiomeGenBaseTekto.forest, BiomeGenBaseTekto.vinylsands}) {
            check(c.getOrDefault(b, 0) > 0, "Tekto biome " + b.biomeName + " never generated: " + c);
        }
        helper.succeed();
    }

    /** Puffer: Basalt-Koerper, Tetrachlorkohlenstoff-Meer, Vinylsand/Gummigras an der Oberflaeche. */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 400)
    public static void tektoTerrain(GameTestHelper helper) {
        int basalt = 0, ccl = 0, top = 0, bed = 0;
        Block b1 = com.hbm_m.block.ModBlocks.BASALT.get();
        for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) {
            BlockState[] b = CelestialGenerators.buffer("tekto", cx * 9, cz * 9).blocks;
            basalt += count(b, b1, 1, 128);
            ccl += count(b, TektoBlocks.CCL_BLOCK.get(), 0, 64);
            top += count(b, TektoBlocks.VINYL_SAND.get(), 0, 256) + count(b, TektoBlocks.RUBBER_GRASS.get(), 0, 256) + count(b, TektoBlocks.RUBBER_SILT.get(), 0, 256);
            bed += bedrock(b);
        }
        check(bed == 25 * 256, "Tekto bedrock floor, got " + bed);
        check(basalt > 25 * 4000, "Tekto basalt body, got " + basalt);
        check(top > 500, "Tekto surface (vinyl sand / rubber grass / silt), got " + top);
        com.mojang.logging.LogUtils.getLogger().info("[space_m] Tekto buffer: basalt {}, ccl {}, surface {}", basalt, ccl, top);
        helper.succeed();
    }

    // ───────────────────────── Thatmo ─────────────────────────

    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 100)
    public static void thatmoRegistries(GameTestHelper helper) {
        checkRegistries(helper, "thatmo", new BiomeGenBaseCelestial[] {BiomeGenThatmo.biome}, "thatmo/vanilla_mushrooms", "thatmo/ore_uranium", "thatmo/vanilla_ore_iron");
        check(CelestialGenerators.get("thatmo").seaLevel() == 64, "Thatmo sea level 64");
        helper.succeed();
    }

    /** Puffer: Schnee-Oberflaeche ueber Basalt, Sellafit, Grundgestein. */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 400)
    public static void thatmoTerrain(GameTestHelper helper) {
        int snow = 0, basalt = 0, bed = 0;
        for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) {
            BlockState[] b = CelestialGenerators.buffer("thatmo", cx * 5, cz * 5).blocks;
            snow += count(b, Blocks.SNOW_BLOCK, 0, 256);
            basalt += count(b, com.hbm_m.block.ModBlocks.BASALT.get(), 1, 128);
            bed += bedrock(b);
        }
        check(bed == 25 * 256, "Thatmo bedrock floor, got " + bed);
        check(snow > 25 * 256, "Thatmo snow surface, got " + snow);
        check(basalt > 25 * 4000, "Thatmo basalt body, got " + basalt);
        helper.succeed();
    }

    // ───────────────────────── Welt (Dekoration) ─────────────────────────

    /** Echte Dimensionen (nur wenn geladen): Generator, Hoehe, dekorierte Erze/Pflanzen. */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 600)
    public static void bodiesGenerateInWorld(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String body : new String[] {"laythe", "tekto", "thatmo"}) {
            ServerLevel level = server.getLevel(SpaceDimensions.level(body));
            if (level == null) {
                com.mojang.logging.LogUtils.getLogger().info("[space_m] GameTestServer without datapack dimensions - {} checked via buffer only", body);
                continue;
            }
            check(level.getChunkSource().getGenerator() instanceof CelestialChunkGenerator, body + " uses space_m:celestial");
            check(level.getMinBuildHeight() == 0 && level.getHeight() == 256, body + " height 0-255");
            int variants = 0, decor = 0;
            for (int ccx = 0; ccx < 3; ccx++) for (int ccz = 0; ccz < 3; ccz++) {
                ChunkAccess chunk = level.getChunk(ccx, ccz);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y < 128; y++) {
                    BlockState s = chunk.getBlockState(new BlockPos(ccx * 16 + x, y, ccz * 16 + z));
                    String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
                    if (path.endsWith("_" + body) || path.endsWith("_kerbin") || s.is(com.hbm_m.block.ModBlocks.URANIUM_ORE.get())) variants++;
                    if (path.startsWith("laythe_seagrass") || path.startsWith("laythe_glowgrass") || path.startsWith("laythe_kelp")
                            || path.startsWith("plant_tall_laythe") || path.startsWith("rubber_plant_") || path.startsWith("rubber_tall_")
                            || path.endsWith("_log")) decor++;
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("[space_m] {} world: ore variants {}, flora {}", body, variants, decor);
            // Thatmo: Erze ersetzen nur geloeschtes Sellafit (getStone), das dort nur als duenne Lage unter der Oberflaeche
            // liegt - wie im Original entstehen praktisch keine Erze.
            if (!body.equals("thatmo")) check(variants > 0, body + ": no body ore variants decorated");
            if (body.equals("laythe")) check(decor > 0, "laythe: no water plants decorated");
        }
        helper.succeed();
    }

    // ───────────────────────── Beute ─────────────────────────

    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 20)
    public static void bodyBlockDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        java.util.function.BiConsumer<Block, String> drops = (block, expected) -> {
            var list = Block.getDrops(block.defaultBlockState(), level, pos, null);
            check(!list.isEmpty(), BuiltInRegistries.BLOCK.getKey(block) + " drops nothing");
            String got = BuiltInRegistries.ITEM.getKey(list.get(0).getItem()).toString();
            check(got.equals(expected), BuiltInRegistries.BLOCK.getKey(block) + " drops " + got + ", expected " + expected);
        };
        drops.accept(LaytheBlocks.LAYTHE_SILT.get(), "space_m:laythe_silt");
        drops.accept(LaytheBlocks.ORES.get("ore_uranium").get(), "hbm_m:uranium_raw");
        drops.accept(LaytheBlocks.ORE_OIL.get(), "space_m:ore_oil_empty_laythe");
        drops.accept(LaytheBlocks.LAYTHE_CORAL_BLOCK.get("fire").get(), "space_m:laythe_coral_block_fire");
        drops.accept(TektoBlocks.RUBBER_GRASS.get(), "space_m:rubber_grass");
        drops.accept(TektoBlocks.PVC_LOG.get(), "space_m:pvc_log");
        drops.accept(TektoBlocks.ORE_TEKTO.get(), "space_m:ore_tekto_empty_tekto");
        drops.accept(TektoBlocks.ORES.get("ore_iron").get(), "minecraft:raw_iron");
        drops.accept(com.space_m.dim.thatmo.ThatmoBlocks.ORE_NICKEL.get(), "space_m:ore_nickel_kerbin");
        helper.succeed();
    }

    // ───────────────────────── Karten ─────────────────────────

    /** Sichtpruefung: Draufsicht 512x512 Bloecke um 0,0 je Koerper als {@code space_m_<koerper>_map.png}. */
    @GameTest(template = "empty3x3x3", batch = "space_dim", timeoutTicks = 1200)
    public static void bodyTerrainMaps(GameTestHelper helper) {
        Map<Block, Integer> colors = new HashMap<>();
        colors.put(Blocks.WATER, 0x3030C0);
        colors.put(LaytheBlocks.LAYTHE_SILT.get(), 0x9070A0);
        colors.put(Blocks.SNOW_BLOCK, 0xF0F0F0);
        colors.put(Blocks.PACKED_ICE, 0xA0C0FF);
        colors.put(Blocks.STONE, 0x808080);
        colors.put(TektoBlocks.CCL_BLOCK.get(), 0x60A0A0);
        colors.put(TektoBlocks.VINYL_SAND.get(), 0xD0C080);
        colors.put(TektoBlocks.RUBBER_GRASS.get(), 0x40A040);
        colors.put(TektoBlocks.RUBBER_SILT.get(), 0x708050);
        colors.put(TektoBlocks.GEYSIR_CHLORIC.get(), 0xFF0000);
        colors.put(com.hbm_m.block.ModBlocks.BASALT.get(), 0x303030);
        colors.put(com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get(), 0x406040);
        for (String body : new String[] {"laythe", "tekto", "thatmo"}) {
            int size = 32;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size * 16, size * 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
            int min = 255, max = 0;
            for (int cx = 0; cx < size; cx++) for (int cz = 0; cz < size; cz++) {
                BlockMetaBuffer buf = CelestialGenerators.buffer(body, cx - size / 2, cz - size / 2);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    int y = 255;
                    BlockState s = null;
                    while (y > 0) {
                        s = buf.blocks[(x * 16 + z) * 256 + y];
                        if (s != null && !s.isAir()) break;
                        y--;
                    }
                    min = Math.min(min, y);
                    max = Math.max(max, y);
                    int base = s == null ? 0x000000 : colors.getOrDefault(s.getBlock(), 0xA0A0A0);
                    float shade = Math.max(0.35F, Math.min(1.3F, 0.5F + (y - 40) / 80F));
                    int r = Math.min(255, (int) (((base >> 16) & 255) * shade));
                    int g = Math.min(255, (int) (((base >> 8) & 255) * shade));
                    int bl = Math.min(255, (int) ((base & 255) * shade));
                    img.setRGB(cx * 16 + x, cz * 16 + z, r << 16 | g << 8 | bl);
                }
            }
            try {
                java.io.File out = new java.io.File("space_m_" + body + "_map.png");
                javax.imageio.ImageIO.write(img, "png", out);
                com.mojang.logging.LogUtils.getLogger().info("[space_m] {} terrain map {} (surface y {}..{})", body, out.getAbsolutePath(), min, max);
            } catch (java.io.IOException e) {
                throw new GameTestAssertException("map not written: " + e);
            }
            check(max - min > 4 && max < 250 && min > 0, body + " surface heights plausible, got " + min + ".." + max);
        }
        helper.succeed();
    }
}
