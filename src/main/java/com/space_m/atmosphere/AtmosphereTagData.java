package com.space_m.atmosphere;

import java.util.ArrayList;
import java.util.List;

/**
 * Inhalte der Atmosphaeren-Tags (Datagen {@code SpaceTagProviders}, Ersatz der Original-Interfaces/-Felder an
 * HBM-Klassen). Quelle jeweils NTM Space:
 * <ul>
 *   <li>{@code space_m:unsealable}: {@code isSealed = false} an MachineBrickFurnace ({@code furnace_brick}),
 *       MachineDiFurnace ({@code blast_furnace}), MachineDiesel ({@code dieselgen}), MachineFan ({@code fan}).</li>
 *   <li>{@code space_m:sealed_armor}: {@code ArmorFSB.setSealed(true)} in {@code ModItemsArmor} (12 Saetze).</li>
 *   <li>{@code space_m:suffocation_immune}: {@code ISuffocationImmune} an CyberCrab (+Tesla/Taint), Dummy, FBI,
 *       HunterChopper, MaskMan, ParasiteMaggot, PlasticBag, RADBeast, UFO, UFOBase (FBI-Drohne, SiegeCraft),
 *       Glyphid (+8 Unterarten). Space-Mobs (BFAngel, MoonCow, Tankbot, WarBehemoth, SiegeUFO) traegt R8 ein.</li>
 *   <li>{@code space_m:corrosion_protection}: {@code insert_cmb.withCorrosionProtection()} (in HBM noch nicht portiert
 *       -> optional).</li>
 * </ul>
 */
public final class AtmosphereTagData {
	private AtmosphereTagData() {}

	public static final List<String> UNSEALABLE = List.of("hbm_m:furnace_brick", "hbm_m:blast_furnace", "hbm_m:dieselgen", "hbm_m:fan");

	public static final List<String> SEALED_ARMOR = sets("t51", "steamsuit", "ajr", "ajro", "rpa", "ncrpa", "envsuit", "hev",
			"fau", "dns", "taurun", "trenchmaster");

	public static final List<String> SUFFOCATION_IMMUNE = List.of("hbm_m:entity_cyber_crab", "hbm_m:entity_tesla_crab",
			"hbm_m:entity_taint_crab", "hbm_m:entity_ntm_test_dummy", "hbm_m:entity_ntm_fbi", "hbm_m:entity_ntm_fbi_drone",
			"hbm_m:entity_mob_hunter_chopper", "hbm_m:maskman", "hbm_m:parasite_maggot", "hbm_m:entity_plastic_bag",
			"hbm_m:rad_beast", "hbm_m:ufo", "hbm_m:entity_ntm_siege_craft", "hbm_m:glyphid", "hbm_m:glyphid_brawler",
			"hbm_m:glyphid_behemoth", "hbm_m:glyphid_brenda", "hbm_m:glyphid_bombardier", "hbm_m:glyphid_blaster",
			"hbm_m:glyphid_scout", "hbm_m:glyphid_nuclear", "hbm_m:glyphid_digger");

	// R9-A: Original ModItems insert_yharonite (9999, 0.01, 1, 1, 1).withCorrosionProtection() - in HBM portiert
	public static final List<String> CORROSION_PROTECTION_OPTIONAL = List.of("hbm_m:insert_cmb", "space_m:insert_cmb", "hbm_m:insert_yharonite");

	private static List<String> sets(String... names) {
		List<String> ids = new ArrayList<>();
		for(String name : names) {
			for(String part : new String[] { "helmet", "plate", "legs", "boots" }) ids.add("hbm_m:" + name + "_" + part);
		}
		return List.copyOf(ids);
	}
}
