package com.space_m.dim.moon;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.slf4j.Logger;

import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.JigsawPool;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;

import dev.architectury.event.events.common.LifecycleEvent;

/**
 * 1:1 {@code com.hbm.dim.moon.WorldGeneratorMoon}.
 *
 * <p><b>Erze</b> ({@code generateMoon}): als Datapack-Features im Biom {@code space_m:mun}
 * ({@code placed_feature/mun/ore_lithium|ore_aluminium_moon|ore_fluorite_moon|ore_quartz|ore_shale}), Werte 1:1:
 * Lithium {@code lithiumSpawn}x6 y4-11, Aluminium {@code aluminiumSpawn}x6 y5-44, Fluorit {@code fluoriteSpawn}x4
 * y5-49, Quarz 10x13 y5-68, Schiefer 1x12 y8-39 - jeweils im Mondgestein.
 *
 * <p><b>Mun-Basis</b> ({@code NBTStructure.registerStructure(moonDimension, SpawnCondition "mun_base")}): Jigsaw-Pools
 * start/default/connect 1:1, Teile aus {@code assets/space_m/structures/munbase/*.nbt} (umgesetzt mit
 * {@code scratchpad/sR1b_struct.py}). Angemeldet wird ueber die NBTStructure-Schnittstelle von HBM-Modernized unter
 * der Original-Dimensions-ID 413015 (das HBM-API ist int-basiert); erzeugt wird sie ueber
 * {@code data/space_m/worldgen/structure|structure_set/mun_structures.json} ({@code hbm_m:ntm}/{@code hbm_m:ntm_grid}
 * mit {@code "dimension": 413015}, W-B1). Angemeldet wird nach der Block-Registrierung ({@code SETUP}), weil
 * {@code NBTStructure(name, stream)} die Palette sofort aufloest.
 *
 * <p>{@code BlockOre.addValidBody} (Tooltip "Can be found on") folgt mit dem BlockOre-Tooltip (R8).
 */
public final class WorldGeneratorMoon {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.moonDimension}. */
    public static final int MOON_DIMENSION_ID = 413015;

    private WorldGeneratorMoon() {}

    public static void init() {
        LifecycleEvent.SETUP.register(WorldGeneratorMoon::registerStructures);
    }

    private static NBTStructure load(String file) {
        String path = "/assets/" + SpaceMod.MOD_ID + "/structures/munbase/" + file;
        try (InputStream in = WorldGeneratorMoon.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path);
            return new NBTStructure("munbase/" + file, in);
        } catch (IOException e) {
            throw new IllegalStateException("[space_m] Mun structure could not be loaded: " + path, e);
        }
    }

    private static void registerStructures() {
        // WorldGeneratorCelestial: meteor_dungeon fuer alle Space-Dimensionen (im Original vor WorldGeneratorMoon
        // angemeldet). HBM meldet dieselbe SpawnCondition fuer die Oberwelt an; hier dieselbe Instanz fuer die Mun.
        try {
            SpawnCondition meteor = NBTStructure.getStructure("meteor_dungeon");
            if (meteor != null) NBTStructure.registerStructure(MOON_DIMENSION_ID, meteor);
            else LOGGER.warn("[space_m] HBM meteor_dungeon not found, Mun gets no meteor dungeons");
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] meteor_dungeon registration for Mun failed", e);
        }

        try {
            JigsawPiece munBase = new JigsawPiece("mun_base", load("base.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece munFlag = new JigsawPiece("mun_flag", load("flag.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece munPanels = new JigsawPiece("mun_panels", load("panels.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece munStardar = new JigsawPiece("mun_stardar", load("stardar.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece munTanks = new JigsawPiece("mun_tanks", load("tanks.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece c1 = new JigsawPiece("mun_connector_1", load("connector-1.nbt"));
            JigsawPiece c2 = new JigsawPiece("mun_connector_2", load("connector-2.nbt"));
            JigsawPiece c3 = new JigsawPiece("mun_connector_3", load("connector-3.nbt"));
            JigsawPiece cs = new JigsawPiece("mun_connector_s", load("connector-s.nbt"));
            JigsawPiece cl = new JigsawPiece("mun_connector_l", load("connector-l.nbt"));
            JigsawPiece ct = new JigsawPiece("mun_connector_t", load("connector-t.nbt"));

            NBTStructure.registerStructure(MOON_DIMENSION_ID, new SpawnCondition("mun_base") {{
                spawnWeight = 6;
                sizeLimit = 32;
                startPool = "start";
                pools = new HashMap<String, JigsawPool>() {{
                    put("start", new JigsawPool() {{
                        add(munBase, 1);
                    }});
                    put("default", new JigsawPool() {{
                        add(munBase, 1);
                        add(munFlag, 2);
                        add(munPanels, 6);
                        add(munStardar, 1);
                        add(munTanks, 6);
                    }});
                    put("connect", new JigsawPool() {{
                        add(c1, 1);
                        add(c2, 1);
                        add(c3, 1);
                        add(cs, 1);
                        add(cl, 1);
                        add(ct, 1);
                    }});
                }};
            }});

            NBTStructure.registerNullWeight(MOON_DIMENSION_ID, 18);
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Mun base structure registration failed", e);
        }
    }
}
