package com.space_m.dim.dres;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.world.gen.nbt.BlockSelector;
import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.JigsawPool;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code com.hbm.dim.dres.WorldGeneratorDres}.
 *
 * <p><b>Erze</b> ({@code generateDres}): Datapack-Features im Biom {@code space_m:dres}/{@code dres_basins}
 * ({@code placed_feature/dres/ore_cobalt_dres|ore_iron_dres|ore_niobium|ore_coltan|ore_lanthanium|ore_shale}), Werte 1:1:
 * Kobalt {@code cobaltSpawn}x4 y3-24, Eisen {@code copperSpawn}(!)x9 y4-30, Niob 12x8 y1-33, Coltan
 * {@code coltanRate}x4 y15-54, Lanthan 1x6 y4-67, Schiefer 1x12 y8-39 - jeweils im Dres-Gestein.
 *
 * <p><b>Dres-RBMK</b> ({@code SpawnCondition "dres_rbmk"}): Jigsaw-Pools start/default/outside/reactor/inback/outback
 * 1:1 (Fallbacks, Gewichte 5:1 Sternen-RBMK/Brueter), Laborfliesen mit 20 % Rissen ({@code LabTiles(0.2F)}),
 * Start auf y 40, bis 128 Teile, Reichweite 64, nur in den Dres-Ebenen; Leergewicht 16. Teile aus
 * {@code assets/space_m/structures/dresbmk/} ({@code scratchpad/sR3b_struct.py}). Wie die Mun-Basis ueber die
 * NBTStructure-Schnittstelle von HBM unter der Original-ID 413019 angemeldet - erscheint erst mit Wunsch W-B1.
 *
 * <p>{@code BlockOre.addValidBody(ore_shale/ore_lanthanium/ore_niobium/ore_coltan, DRES)} folgt mit dem
 * BlockOre-Tooltip (R8). {@code getBedrockAcid} (Salpetersaeure 500 mB) gehoert zu den Bedrock-Erzen (R8).
 */
public final class WorldGeneratorDres {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.dresDimension}. */
    public static final int DRES_DIMENSION_ID = 413019;

    private WorldGeneratorDres() {}

    public static void init() {
        LifecycleEvent.SETUP.register(WorldGeneratorDres::registerStructures);
    }

    static NBTStructure load(String file) {
        String path = "/assets/" + SpaceMod.MOD_ID + "/structures/" + file;
        try (InputStream in = WorldGeneratorDres.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path);
            return new NBTStructure(file, in);
        } catch (IOException e) {
            throw new IllegalStateException("[space_m] Dres structure could not be loaded: " + path, e);
        }
    }

    private static JigsawPiece piece(String name, String file, Map<Block, BlockSelector> tiles) {
        JigsawPiece p = new JigsawPiece(name, load("dresbmk/" + file));
        p.blockTable = tiles;
        return p;
    }

    private static void registerStructures() {
        try {
            Map<Block, BlockSelector> tiles = new HashMap<>();
            tiles.put(ModBlocks.TILE_LAB.get(), new LabTiles(0.2F));

            JigsawPiece core = piece("dres_core", "core.nbt", tiles);
            JigsawPiece t = piece("dres_t", "t.nbt", tiles);
            JigsawPiece airlock = piece("dres_airlock", "airlock.nbt", tiles);
            JigsawPiece dome = piece("dres_dome", "dome.nbt", tiles);
            JigsawPiece pool = piece("dres_pool", "pool.nbt", tiles);
            JigsawPiece balcony = piece("dres_balcony", "balcony.nbt", tiles);
            JigsawPiece pad = piece("dres_pad", "pad.nbt", tiles);
            JigsawPiece starbmk = piece("dres_hall_starbmk", "hall/starbmk.nbt", tiles);
            JigsawPiece breeder = piece("dres_hall_breeder", "hall/breeder.nbt", tiles);
            JigsawPiece incap = piece("dres_incap", "incap.nbt", tiles);
            JigsawPiece outcap = piece("dres_outcap", "outcap.nbt", tiles);

            NBTStructure.registerStructure(DRES_DIMENSION_ID, new SpawnCondition("dres_rbmk") {{
                spawnWeight = 8;
                minHeight = 40;
                maxHeight = 40;
                sizeLimit = 128;
                rangeLimit = 64;
                canSpawn = biome -> "space_m:dres".equals(biome.name);
                startPool = "start";
                pools = new HashMap<String, JigsawPool>() {{
                    put("start", new JigsawPool() {{
                        add(core, 1);
                    }});
                    put("default", new JigsawPool() {{
                        add(t, 1);
                        add(airlock, 1);
                        add(dome, 1);
                        add(pool, 1);
                        fallback = "inback";
                    }});
                    put("outside", new JigsawPool() {{
                        add(balcony, 1);
                        add(pad, 1);
                        fallback = "outback";
                    }});
                    put("reactor", new JigsawPool() {{
                        add(starbmk, 5);
                        add(breeder, 1);
                    }});
                    put("inback", new JigsawPool() {{
                        add(incap, 1);
                    }});
                    put("outback", new JigsawPool() {{
                        add(outcap, 1);
                    }});
                }};
            }});

            NBTStructure.registerNullWeight(DRES_DIMENSION_ID, 16);
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Dres RBMK structure registration failed", e);
        }
    }
}
