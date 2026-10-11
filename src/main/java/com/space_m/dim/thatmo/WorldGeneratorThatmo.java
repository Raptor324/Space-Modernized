package com.space_m.dim.thatmo;

import java.util.HashMap;

import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.JigsawPool;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.space_m.dim.laythe.WorldGeneratorLaythe;

/**
 * 1:1 {@code com.hbm.dim.thatmo.WorldGeneratorThatmo}. <b>Im Original toter Code:</b> {@code PlanetGen} meldet den
 * Generator nie an ({@code new WorldGeneratorThatmo()} fehlt), die Strukturen erscheinen also nie und
 * {@code generateThatmo} ist leer. Der Port behaelt das bei - {@link #registerStructures()} wird nicht aufgerufen
 * (Nutzerentscheidung, ob die Thatmo-Strukturen aktiviert werden sollen). Die Teile liegen umgesetzt unter
 * {@code assets/space_m/structures/} (thatmotestmo, thatmo2, trenches, intersection, road, curve, tshapeintersex,
 * block1, block2, pfmfactory).
 *
 * <p>Abweichung bei Aktivierung: {@code trenches.blockTable} ({@code brick_concrete_cracked -> ConcreteBricks}) ist
 * in HBM-Modernized paketprivat ({@code Component.ConcreteBricks}) - ohne Austausch.
 */
public final class WorldGeneratorThatmo {

	/** Original {@code SpaceConfig.thatmoDimension}. */
	public static final int THATMO_DIMENSION_ID = 413025;

	private WorldGeneratorThatmo() {}

	private static NBTStructure load(String f) {
		return WorldGeneratorLaythe.load(f);
	}

	public static void registerStructures() {
		NBTStructure thatmotestmo = load("thatmotestmo.nbt");
		NBTStructure thatmo2 = load("thatmo2.nbt");
		NBTStructure trenches = load("trenches.nbt");
		NBTStructure intersection = load("intersection.nbt");
		NBTStructure road = load("road.nbt");
		NBTStructure curve = load("curve.nbt");
		NBTStructure tshape = load("tshapeintersex.nbt");
		NBTStructure block1 = load("block1.nbt");
		NBTStructure block2 = load("block2.nbt");
		NBTStructure pfmfac = load("pfmfactory.nbt");

		NBTStructure.registerStructure(THATMO_DIMENSION_ID, new SpawnCondition("thatmotest") {{
			structure = new JigsawPiece("thatmotest", thatmotestmo, -1);
			canSpawn = biome -> heightVariation(biome) < 0.1F;
		}});
		NBTStructure.registerStructure(THATMO_DIMENSION_ID, new SpawnCondition("thatmo2") {{
			structure = new JigsawPiece("thatmotest2", thatmo2, 1);
			canSpawn = biome -> heightVariation(biome) < 0.1F;
		}});
		NBTStructure.registerStructure(THATMO_DIMENSION_ID, new SpawnCondition("trenches") {{
			JigsawPiece rupture = new JigsawPiece("trenches", trenches, -1);
			rupture.conformToTerrain = true;
			spawnWeight = 2;
			canSpawn = biome -> heightVariation(biome) < 0.1F;
			structure = rupture;
		}});

		NBTStructure.registerStructure(THATMO_DIMENSION_ID, new SpawnCondition("city") {{
			sizeLimit = 128;
			canSpawn = biome -> heightVariation(biome) < 0.1F;
			startPool = "default";
			pools = new HashMap<String, JigsawPool>() {{
				put("default", new JigsawPool() {{
					add(new JigsawPiece("intersection", intersection, 0){{ alignToTerrain = true; }}, 1);
					add(new JigsawPiece("road_1", road, 0){{ conformToTerrain = true; }}, 1);
					add(new JigsawPiece("curve_1", curve, 0){{ conformToTerrain = true; }}, 1);
					add(new JigsawPiece("tshape", tshape, 0){{ conformToTerrain = true; }}, 1);
					add(new JigsawPiece("block1", block1, 0){{ alignToTerrain = true; }}, 1);
					add(new JigsawPiece("block2", block2, 0){{ alignToTerrain = true; }}, 1);
					add(new JigsawPiece("pfmfac", pfmfac, 0){{ alignToTerrain = true; }}, 1);
				}});
				put("roadsonly", new JigsawPool() {{
					add(new JigsawPiece("road_2", road, 0){{ conformToTerrain = true; }}, 1);
					add(new JigsawPiece("curve_2", curve, 0){{ conformToTerrain = true; }}, 1);
				}});

			}};
		}});
	}

	private static float heightVariation(com.hbm_m.world.gen.LegacyBiome biome) {
		var b = WorldGeneratorLaythe.celestial(biome);
		return b != null ? b.heightVariation : biome.heightVariation;
	}
}
