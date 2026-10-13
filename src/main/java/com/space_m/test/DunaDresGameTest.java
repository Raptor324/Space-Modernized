package com.space_m.test;

import java.util.HashMap;
import java.util.Map;

import com.space_m.SpaceMod;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialChunkGenerator;
import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.CelestialSeed;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.SpaceDimensions;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.dres.ChunkProviderDres;
import com.space_m.dim.dres.DresBlocks;
import com.space_m.dim.dres.WorldProviderDres;
import com.space_m.dim.dres.biome.BiomeGenBaseDres;
import com.space_m.dim.duna.ChunkProviderDuna;
import com.space_m.dim.duna.DunaBlocks;
import com.space_m.dim.duna.DunaDustStorm;
import com.space_m.dim.duna.WorldProviderDuna;
import com.space_m.dim.duna.biome.BiomeGenBaseDuna;
import com.space_m.dim.legacy.LegacyBiomes;

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
 * R3-2 Duna + Dres: Datapack-Dimensionen/Biome/Features, Gelaende aus den portierten ChunkProvidern (Oberflaeche,
 * Grundgestein, Gesteinskoerper), Biomverteilung der GenLayer-Ketten (inkl. Flussrinnen der Duna in der groben
 * Schicht), Beute, Staubsturm-Zustandsmaschine, Strukturanmeldung bei HBM, Draufsichtkarten
 * ({@code run/space_m_duna_map.png}, {@code run/space_m_dres_map.png}).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DunaDresGameTest {

    private DunaDresGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 100)
    public static void dunaDresRegistries(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        for (String body : new String[] {"duna", "dres"}) {
            var type = access.registryOrThrow(Registries.DIMENSION_TYPE).get(SpaceDimensions.type(body));
            check(type != null, "dimension_type space_m:" + body + " missing");
            check(type.minY() == 0 && type.height() == 256, body + " height 0-255");
            check(SpaceMod.id(body).equals(type.effectsLocation()), body + " effects space_m:" + body);
            check(CelestialGenerators.get(body) != null && CelestialGenerators.get(body).seaLevel() == 63, "body generator " + body + " (seaLevel 63)");
        }
        var biomes = access.registryOrThrow(Registries.BIOME);
        for (BiomeGenBaseCelestial b : new BiomeGenBaseCelestial[] {BiomeGenBaseDuna.dunaPlains, BiomeGenBaseDuna.dunaLowlands, BiomeGenBaseDuna.dunaPolar,
                BiomeGenBaseDuna.dunaHills, BiomeGenBaseDuna.dunaPolarHills, BiomeGenBaseDres.dresPlains, BiomeGenBaseDres.dresCanyon}) {
            check(biomes.containsKey(b.key), "biome " + b.key.location() + " missing");
        }
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);
        for (String f : new String[] {"duna/ore_iron_duna", "duna/basalt_gem", "duna/cave_sulfur", "duna/lantern", "duna/calcium", "dres/ore_niobium", "dres/ore_coltan", "dres/layer_hematite"}) {
            check(placed.containsKey(SpaceMod.id(f)), "placed feature space_m:" + f + " missing");
        }
        check(BuiltInRegistries.BIOME_SOURCE.containsKey(SpaceMod.id("martian")), "biome source space_m:martian (The Martian) not registered");
        check(access.registryOrThrow(Registries.WORLD_PRESET).containsKey(SpaceMod.id("martian")), "world preset space_m:martian missing");
        check(com.hbm_m.world.gen.nbt.NBTStructure.getStructure("duna_comms") != null, "duna_comms not registered at HBM NBTStructure");
        check(com.hbm_m.world.gen.nbt.NBTStructure.getStructure("dres_rbmk") != null, "dres_rbmk not registered at HBM NBTStructure");
        check(com.hbm_m.api.space.SpaceResources.isExtraPumpGround(DunaBlocks.DUNA_SANDS.get()), "duna_sands is no pump ground");
        ServerLevel duna = helper.getLevel().getServer().getLevel(SpaceDimensions.DUNA);
        if (duna != null) check(duna.getChunkSource().getGenerator() instanceof CelestialChunkGenerator, "duna uses space_m:celestial");
        helper.succeed();
    }

    /** Biomverteilung: alle 5 Duna- und beide Dres-Biome kommen vor; die Duna-Flussmischung liefert ID 7 nur grob. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 200)
    public static void dunaDresBiomeLayers(GameTestHelper helper) {
        long seed = CelestialSeed.get();
        WorldChunkManagerCelestial duna = new WorldChunkManagerCelestial(WorldProviderDuna.createBiomeGenerators(seed), seed, BiomeGenBaseDuna.dunaPlains);
        Map<BiomeGenBaseCelestial, Integer> detail = new HashMap<>();
        BiomeGenBaseCelestial[] buf = duna.loadBlockGeneratorData(null, -2048, -2048, 4096, 1);
        for (BiomeGenBaseCelestial b : buf) detail.merge(b, 1, Integer::sum);
        BiomeGenBaseCelestial[] buf2 = duna.loadBlockGeneratorData(null, 0, -2048, 1, 4096);
        for (BiomeGenBaseCelestial b : buf2) detail.merge(b, 1, Integer::sum);
        check(detail.keySet().containsAll(java.util.List.of(BiomeGenBaseDuna.dunaPlains, BiomeGenBaseDuna.dunaLowlands, BiomeGenBaseDuna.dunaPolar,
                BiomeGenBaseDuna.dunaHills, BiomeGenBaseDuna.dunaPolarHills)) && !detail.containsKey(LegacyBiomes.RIVER), "Duna detail biomes " + detail);
        int river = 0;
        BiomeGenBaseCelestial[] coarse = duna.getBiomesForGeneration(null, -1024, -1024, 512, 512);
        for (BiomeGenBaseCelestial b : coarse) if (b == LegacyBiomes.RIVER) river++;
        check(river > 0, "Duna coarse layer has no river channels (GenLayerRiverMix)");

        WorldChunkManagerCelestial dres = new WorldChunkManagerCelestial(WorldProviderDres.createBiomeGenerators(seed), seed, BiomeGenBaseDres.dresPlains);
        Map<BiomeGenBaseCelestial, Integer> d2 = new HashMap<>();
        for (BiomeGenBaseCelestial b : dres.loadBlockGeneratorData(null, -2048, 0, 4096, 1)) d2.merge(b, 1, Integer::sum);
        check(d2.size() == 2, "Dres biomes " + d2);
        com.mojang.logging.LogUtils.getLogger().info("[space_m] Duna biomes {}, coarse river cells {}, Dres biomes {}", detail, river, d2);
        helper.succeed();
    }

    private static int surface(BlockState[] blocks, int x, int z) {
        for (int y = 255; y > 0; y--) {
            BlockState s = blocks[(x * 16 + z) * 256 + y];
            if (s != null && !s.isAir()) return y;
        }
        return 0;
    }

    /** Gelaende-Puffer: Grundgestein unten, Gesteinskoerper, Oberflaechenbloecke wie im Original. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 400)
    public static void dunaDresTerrain(GameTestHelper helper) {
        Block dunaRock = DunaBlocks.DUNA_ROCK.get();
        Block dresRock = DresBlocks.DRES_ROCK.get();
        int dunaTop = 0, dunaBody = 0, dresTop = 0, dresBody = 0, bedrock = 0, columns = 0;
        java.util.Set<Block> dunaSurface = new java.util.HashSet<>();
        for (int cx = -6; cx < 6; cx += 3) {
            for (int cz = -6; cz < 6; cz += 3) {
                BlockState[] d = CelestialGenerators.buffer("duna", cx * 7, cz * 7).blocks;
                BlockState[] r = CelestialGenerators.buffer("dres", cx * 7, cz * 7).blocks;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        columns++;
                        if (d[(x * 16 + z) * 256] != null && d[(x * 16 + z) * 256].is(Blocks.BEDROCK)) bedrock++;
                        BlockState s = d[(x * 16 + z) * 256 + surface(d, x, z)];
                        dunaSurface.add(s.getBlock());
                        if (s.is(DunaBlocks.DUNA_SANDS.get()) || s.is(Blocks.SNOW_BLOCK) || s.is(DunaBlocks.DRY_ICE.get()) || s.is(dunaRock)
                                || s.is(DunaBlocks.FERRIC_CLAY.get()) || s.is(Blocks.RED_TERRACOTTA) || s.is(DunaBlocks.DUNA_COBBLE.get())
                                || s.is(com.hbm_m.block.ModBlocks.BASALT.get()) || s.is(Blocks.GRAVEL)) dunaTop++;
                        BlockState t = r[(x * 16 + z) * 256 + surface(r, x, z)];
                        if (t.getBlock() instanceof com.hbm_m.block.generic.BlockSellafieldSlaked || t.is(dresRock)) dresTop++;
                        for (int y = 5; y < 50; y++) {
                            BlockState a = d[(x * 16 + z) * 256 + y];
                            if (a != null && (a.is(dunaRock) || a.is(DunaBlocks.DUNA_COBBLE.get()))) dunaBody++;
                            BlockState b = r[(x * 16 + z) * 256 + y];
                            if (b != null && b.is(dresRock)) dresBody++;
                        }
                    }
                }
            }
        }
        check(bedrock == columns, "Duna bedrock floor " + bedrock + "/" + columns);
        check(dunaTop > columns * 9 / 10, "Duna surface blocks " + dunaTop + "/" + columns + " " + dunaSurface);
        check(dresTop > columns * 9 / 10, "Dres surface sellafield_slaked " + dresTop + "/" + columns);
        check(dunaBody > columns * 20 && dresBody > columns * 20, "rock bodies duna " + dunaBody + ", dres " + dresBody);
        helper.succeed();
    }

    /** Dekorierte Welt (nur wenn der GameTest-Server Datapack-Dimensionen laedt, Forge 1.20.1): Koerper-Erze. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 400)
    public static void dunaDresDecorated(GameTestHelper helper) {
        for (var key : java.util.List.of(SpaceDimensions.DUNA, SpaceDimensions.DRES)) {
            ServerLevel level = helper.getLevel().getServer().getLevel(key);
            if (level == null) continue;
            String suffix = "_" + key.location().getPath();
            int ores = 0;
            for (int c = 0; c < 4; c++) {
                ChunkAccess chunk = level.getChunk(c, 0);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y < 70; y++) {
                    if (BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(new BlockPos(x, y, z)).getBlock()).getPath().endsWith(suffix)) ores++;
                }
            }
            check(ores > 0, key.location() + ": no body ore variants decorated");
        }
        helper.succeed();
    }

    /** Staubsturm: Neue-Welt-Werte, Wechsel Ruhe/Sturm mit Original-Spannen, Schlafen beendet, isDaytime. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 100)
    public static void dunaDustStorm(GameTestHelper helper) {
        ServerLevel duna = helper.getLevel().getServer().getLevel(SpaceDimensions.DUNA);
        if (duna == null) {
            check(DunaDustStorm.intensity(helper.getLevel()) == 0, "dust storm outside Duna");
            helper.succeed();
            return;
        }
        DunaDustStorm.setForTest(duna, 0, 1);
        DunaDustStorm.INSTANCE.updateWeather(duna);
        int t = DunaDustStorm.timerForTest(duna);
        check(DunaDustStorm.intensity(duna) == 0 && t >= 11999 && t < 180000, "calm phase after storm, timer " + t);
        DunaDustStorm.setForTest(duna, 0, 0);
        DunaDustStorm.INSTANCE.updateWeather(duna);
        float i = DunaDustStorm.intensity(duna);
        t = DunaDustStorm.timerForTest(duna);
        check(i >= 0.25F && i <= 1.0F && t >= 11999 && t < 24000, "storm " + i + " timer " + t);
        check(i < 0.2F || !duna.isDay(), "isDaytime false during storm >= 0.2");
        DunaDustStorm.INSTANCE.resetRainAndThunder(duna);
        check(DunaDustStorm.intensity(duna) == 0, "sleep clears the storm");
        helper.succeed();
    }

    /** Beute: duna_rock -> duna_cobble, Erze wie die Oberwelt-Gegenstuecke. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 20)
    public static void dunaDresBlockDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        java.util.function.BiConsumer<Block, String> drops = (block, expected) -> {
            var list = Block.getDrops(block.defaultBlockState(), level, pos, null);
            check(!list.isEmpty(), BuiltInRegistries.BLOCK.getKey(block) + " drops nothing");
            String got = BuiltInRegistries.ITEM.getKey(list.get(0).getItem()).toString();
            check(got.equals(expected), BuiltInRegistries.BLOCK.getKey(block) + " drops " + got + ", expected " + expected);
        };
        drops.accept(DunaBlocks.DUNA_ROCK.get(), "space_m:duna_cobble");
        drops.accept(DunaBlocks.DUNA_SANDS.get(), "space_m:duna_sands");
        drops.accept(DunaBlocks.DRY_ICE.get(), "space_m:dry_ice");
        drops.accept(DunaBlocks.ORE_OIL.get(), "hbm_m:oil_tar_crude");
        drops.accept(DresBlocks.DRES_ROCK.get(), "space_m:dres_rock");
        drops.accept(DresBlocks.ORE_NIOBIUM.get(), "space_m:ore_niobium_dres");
        drops.accept(DresBlocks.ORE_URANIUM.get(), "hbm_m:uranium_raw");
        helper.succeed();
    }

    /** Hoehen sind unabhaengig von der Generator-Instanz (Math.random() im Original betrifft nur Unterboden/Eis). */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 200)
    public static void dunaDresDeterministic(GameTestHelper helper) {
        long seed = CelestialSeed.get();
        for (int[] c : new int[][] {{3, -7}, {-40, 12}}) {
            ChunkProviderCelestial a = new ChunkProviderDuna(new WorldChunkManagerCelestial(WorldProviderDuna.createBiomeGenerators(seed), seed, BiomeGenBaseDuna.dunaPlains), seed);
            ChunkProviderCelestial b = new ChunkProviderDuna(new WorldChunkManagerCelestial(WorldProviderDuna.createBiomeGenerators(seed), seed, BiomeGenBaseDuna.dunaPlains), seed);
            b.provideChunk(c[0] + 5, c[1] - 2);
            BlockState[] x = a.provideChunk(c[0], c[1]).blocks, y = b.provideChunk(c[0], c[1]).blocks;
            ChunkProviderCelestial r1 = new ChunkProviderDres(new WorldChunkManagerCelestial(WorldProviderDres.createBiomeGenerators(seed), seed, BiomeGenBaseDres.dresPlains), seed);
            ChunkProviderCelestial r2 = new ChunkProviderDres(new WorldChunkManagerCelestial(WorldProviderDres.createBiomeGenerators(seed), seed, BiomeGenBaseDres.dresPlains), seed);
            r2.provideChunk(c[0] - 3, c[1]);
            BlockState[] p = r1.provideChunk(c[0], c[1]).blocks, q = r2.provideChunk(c[0], c[1]).blocks;
            // Math.random() (Original) waehlt im Unterboden Sand/Gestein; die Hoehlen graben nur Gestein -> einzelne
            // Spalten an Hoehlenoeffnungen duerfen abweichen, das Gelaende selbst nicht.
            int dd = 0, rd = 0;
            for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
                if (surface(x, i, j) != surface(y, i, j)) dd++;
                if (surface(p, i, j) != surface(q, i, j)) rd++;
            }
            check(dd <= 25 && rd <= 25, "height differs at chunk " + c[0] + "," + c[1] + ": duna " + dd + ", dres " + rd + " of 256 columns");
        }
        helper.succeed();
    }

    /** Sichtpruefung: Draufsicht 512x512 je Koerper (Hoehe als Helligkeit, Blockfarbe), Biomgrenzen der Duna. */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 600)
    public static void dunaDresTerrainMap(GameTestHelper helper) {
        for (String body : new String[] {"duna", "dres"}) {
            int size = 32;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size * 16, size * 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
            int min = 255, max = 0;
            for (int cx = 0; cx < size; cx++) {
                for (int cz = 0; cz < size; cz++) {
                    BlockState[] blocks = CelestialGenerators.buffer(body, cx - size / 2, cz - size / 2).blocks;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            int y = surface(blocks, x, z);
                            BlockState s = blocks[(x * 16 + z) * 256 + y];
                            min = Math.min(min, y);
                            max = Math.max(max, y);
                            int col = s == null ? 0x000000 : s.getMapColor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
                            float shade = Math.max(0.3F, Math.min(1.3F, 0.4F + (y - 30) / 80F));
                            int rr = Math.min(255, (int) (((col >> 16) & 255) * shade));
                            int gg = Math.min(255, (int) (((col >> 8) & 255) * shade));
                            int bb = Math.min(255, (int) ((col & 255) * shade));
                            img.setRGB(cx * 16 + x, cz * 16 + z, rr << 16 | gg << 8 | bb);
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
        helper.succeed();
    }

    /**
     * W-B1: {@code duna_comms} (413016) und {@code dres_rbmk} (413019) ueber {@code hbm_m:ntm}/{@code hbm_m:ntm_grid}
     * mit Dimensionsfeld - im 128x128-Chunk-Raster muss je Koerper mindestens eine Struktur mit ihren Bloecken stehen.
     */
    @GameTest(template = "empty3x3x3", batch = "space_dim_r3b", timeoutTicks = 2400)
    public static void dunaDresStructuresGenerate(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        String[][] cases = {{"duna", "hbm_m:pole_satellite_receiver", "hbm_m:deco_titanium"}, {"dres", "hbm_m:tile_lab", "hbm_m:tile_lab_cracked"}};
        for (String[] cs : cases) {
            var structKey = net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, SpaceMod.id(cs[0] + "_structures"));
            var setKey = net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET, SpaceMod.id(cs[0] + "_structures"));
            var structures = access.registryOrThrow(Registries.STRUCTURE);
            check(structures.containsKey(structKey), "structure space_m:" + cs[0] + "_structures missing");
            check(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(setKey), "structure_set space_m:" + cs[0] + "_structures missing");
            ServerLevel level = helper.getLevel().getServer().getLevel(SpaceDimensions.level(cs[0]));
            // R9-C: ohne "Strukturen erzeugen" (NeoForge-GameTestServer) keine Strukturstarts
            if (level == null || !level.getServer().getWorldData().worldGenOptions().generateStructures()) continue;
            var set = access.registryOrThrow(Registries.STRUCTURE_SET).getHolderOrThrow(setKey);
            var state = level.getChunkSource().getGeneratorState();
            var structure = structures.get(structKey);
            Block a = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.tryParse(cs[1]));
            Block b = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.tryParse(cs[2]));
            int cells = 0, starts = 0, found = 0;
            net.minecraft.world.level.levelgen.structure.BoundingBox box = null;
            outer:
            for (int x = -64; x < 64; x++) {
                for (int z = -64; z < 64; z++) {
                    if (!state.hasStructureChunkInRange(set, x, z, 0)) continue;
                    cells++;
                    ChunkAccess c = level.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS, true);
                    var start = c.getStartForStructure(structure);
                    if (start == null || !start.isValid()) continue;
                    starts++;
                    var bb = start.getBoundingBox();
                    int count = 0;
                    for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
                        for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
                            ChunkAccess ch = level.getChunk(cx, cz);
                            for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) for (int y = Math.max(1, bb.minY()); y <= Math.min(255, bb.maxY()); y++) {
                                BlockState st = ch.getBlockState(new BlockPos((cx << 4) + i, y, (cz << 4) + j));
                                if (st.is(a) || st.is(b)) count++;
                            }
                        }
                    }
                    if (count > 3) {
                        found = count;
                        box = bb;
                        break outer;
                    }
                    if (starts > 12) break outer;
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("[space_m] {} structures: {} grid cells, {} starts, found {} marker blocks in {}", cs[0], cells, starts, found, box);
            check(found > 3, cs[0] + ": no " + (cs[0].equals("duna") ? "duna_comms" : "dres_rbmk") + " built (" + cells + " cells, " + starts + " starts)");
        }
        helper.succeed();
    }
}
