package com.space_m.dim.minmus;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.hbm_m.world.gen.nbt.BlockSelector;
import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.JigsawPool;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code com.hbm.dim.minmus.WorldGeneratorMinmus}.
 *
 * <p><b>Erze</b> ({@code generateMinmus}): Datapack-Features im Biom ({@code placed_feature/minmus/ore_nickel_minmus|
 * ore_titanium_minmus|malachite|ore_copper_minmus}), Werte 1:1: Nickel {@code nickelSpawn}x8 y1-43, Titan
 * {@code titaniumSpawn}x12 y4-30, Malachit ({@code stone_resource} MALACHITE) 1x16 y6-45, Kupfer
 * {@code copperSpawn * 3}x12 y8-63 - jeweils im Minmus-Gestein.
 *
 * <p><b>Minmus-Basis</b> ({@code NBTStructure.registerStructure(minmusDimension, SpawnCondition "minmus_base")}):
 * dieselben Teile wie die Mun-Basis ({@code StructureManager.mun_*}, hier {@code assets/space_m/structures/munbase}),
 * aber Fahne {@code flag-uk} und im Basisteil Beton ({@code concrete_colored}) -> {@code concrete_colored_ext}
 * SAND ({@code DesertConcrete}). Angemeldet unter der Original-Dimensions-ID 413021; die HBM-Strukturerzeugung
 * fragt derzeit nur Dimension 0 ab (Wunsch W-B1 in {@code SPACE_API_wishes.md}).
 *
 * <p>{@code BlockOre.addValidBody(ore_brine, MINMUS)} (Tooltip "Can be found on") folgt mit dem BlockOre-Tooltip (R8).
 */
public final class WorldGeneratorMinmus {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.minmusDimension}. */
    public static final int MINMUS_DIMENSION_ID = 413021;

    private WorldGeneratorMinmus() {}

    public static void init() {
        LifecycleEvent.SETUP.register(WorldGeneratorMinmus::registerStructures);
    }

    private static NBTStructure load(String file) {
        String path = "/assets/" + SpaceMod.MOD_ID + "/structures/munbase/" + file;
        try (InputStream in = WorldGeneratorMinmus.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path);
            return new NBTStructure("munbase/" + file, in);
        } catch (IOException e) {
            throw new IllegalStateException("[space_m] Minmus structure could not be loaded: " + path, e);
        }
    }

    /** {@code DesertConcrete}: {@code concrete_colored_ext} Metadatum 6 = {@code EnumConcreteType.SAND}. */
    private static class DesertConcrete extends BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            this.selected = com.hbm_m.block.ModBlocks.CONCRETE_COLORED_EXT_SAND.get().defaultBlockState();
        }

    }

    private static void registerStructures() {
        try {
            Map<Block, BlockSelector> concrete = new HashMap<>();
            // concrete_colored (im Basisteil nur Metadatum 0 = weiss) -> im Port hbm_m:concrete_white
            concrete.put(com.hbm_m.block.ModBlocks.CONCRETE_WHITE.get(), new DesertConcrete());

            JigsawPiece minmusBase = new JigsawPiece("minmus_base", load("base.nbt")) {{ alignToTerrain = true; heightOffset = -1; blockTable = concrete; }};
            JigsawPiece minmusFlag = new JigsawPiece("minmus_flag", load("flag-uk.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece minmusPanels = new JigsawPiece("minmus_panels", load("panels.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece minmusStardar = new JigsawPiece("minmus_stardar", load("stardar.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece minmusTanks = new JigsawPiece("minmus_tanks", load("tanks.nbt")) {{ alignToTerrain = true; heightOffset = -1; }};
            JigsawPiece c1 = new JigsawPiece("minmus_connector_1", load("connector-1.nbt"));
            JigsawPiece c2 = new JigsawPiece("minmus_connector_2", load("connector-2.nbt"));
            JigsawPiece c3 = new JigsawPiece("minmus_connector_3", load("connector-3.nbt"));
            JigsawPiece cs = new JigsawPiece("minmus_connector_s", load("connector-s.nbt"));
            JigsawPiece cl = new JigsawPiece("minmus_connector_l", load("connector-l.nbt"));
            JigsawPiece ct = new JigsawPiece("minmus_connector_t", load("connector-t.nbt"));

            NBTStructure.registerStructure(MINMUS_DIMENSION_ID, new SpawnCondition("minmus_base") {{
                spawnWeight = 6;
                sizeLimit = 32;
                startPool = "start";
                pools = new HashMap<String, JigsawPool>() {{
                    put("start", new JigsawPool() {{
                        add(minmusBase, 1);
                    }});
                    put("default", new JigsawPool() {{
                        add(minmusBase, 1);
                        add(minmusFlag, 2);
                        add(minmusPanels, 6);
                        add(minmusStardar, 1);
                        add(minmusTanks, 6);
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

            NBTStructure.registerNullWeight(MINMUS_DIMENSION_ID, 18);
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Minmus base structure registration failed", e);
        }
    }
}
