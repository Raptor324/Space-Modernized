package com.space_m.config;

/**
 * Konfiguration des Addons - 1:1 die Optionen, die NTM Space gegenueber dem Basis-NTM (5771) einfuehrt:
 * {@code SpaceConfig} (neu) sowie die Space-Ergaenzungen in {@code WorldConfig}, {@code GeneralConfig},
 * {@code MobConfig}, {@code RadiationConfig} und {@code ClientConfig}. Schluessel, Kommentare und Standardwerte
 * stammen aus {@code loadFromConfig} des Originals (bei Abweichung Feld/Ladewert gilt der Ladewert, z.B.
 * {@code nickelSpawn} 12, {@code mineralSpawn} 4, {@code bedrockCadmiumSpawn} 400).
 *
 * <p>Speicherung wie HBM-Modernized: JSON ohne Fremdbibliothek ({@link SpaceConfigStore}),
 * {@code config/space_m/common.json} und {@code config/space_m/client.json}, global (nicht pro Welt).
 *
 * <p><b>Bewusst entfallen (technisch noetig):</b>
 * <ul>
 *   <li>11 Dimensions-IDs ({@code 17.01_moonDimension} ... {@code 17.10_thatmoDimension}, 413015-413025):
 *       Dimensionen sind ab 1.16 Datapack-{@code ResourceKey}s ({@code space_m:mun} ...), es gibt keine
 *       numerischen IDs mehr.</li>
 *   <li>31 Biom-IDs ({@code 16.02_moonBiome} ... {@code 16.32_thatmoBiome}): Biome sind Registry-Eintraege mit
 *       {@code ResourceLocation}; die 255er-ID-Grenze und Kollisionen (EndlessIDs-Offset) existieren nicht mehr.
 *       {@code crashOnBiomeConflict} bleibt als Schalter erhalten, kann aber nie ausloesen.</li>
 *   <li>{@code PotionConfig} 8.11-8.14 (Trank-IDs): Effekte sind Registry-Eintraege.</li>
 * </ul>
 * Zugriff: {@code SpaceConfig.get().feld}.
 */
public class SpaceConfig {

    // Original-Kategorien (CommonConfig, Space-Fassung)
    public static final String GENERAL = "01_general";
    public static final String ORES = "02_ores";
    public static final String MOBS = "12_mobs";
    public static final String RADIATION = "13_radiation";
    public static final String HAZARD = "14_hazard";
    public static final String DIMS = "17_dims";
    public static final String LBSM = "LESS BULLSHIT MODE";
    public static final String CLIENT = "client";

    // ════════════════════════ SpaceConfig (neu in NTM Space) ════════════════════════

    @ConfigEntry(category = DIMS, key = "17.00_allowNetherPortals", origin = "SpaceConfig",
            comment = "Should Nether portals function on other celestial bodies?")
    public boolean allowNetherPortals = false;

    @ConfigEntry(category = GENERAL, key = "1.90_maxProbeDistance", origin = "SpaceConfig",
            comment = "How far from the center of the dimension can probes generate landing coordinates")
    public int maxProbeDistance = 32_000;

    @ConfigEntry(category = GENERAL, key = "1.93_maxStationDistance", origin = "SpaceConfig",
            comment = "How far from the center of the dimension can orbital stations be generated",
            min = 1024, max = Integer.MAX_VALUE)
    public int maxStationDistance = 32_000;

    @ConfigEntry(category = GENERAL, key = "1.91_enableVolcanoGen", origin = "SpaceConfig",
            comment = "Should volcanoes be active when spawning, disabling will prevent natural volcanoes from spewing lava and growing")
    public boolean enableVolcanoGen = true;

    @ConfigEntry(category = GENERAL, key = "1.92_crashOnBiomeConflict", origin = "SpaceConfig",
            comment = "To avoid biome ID collisions, the game will crash if one occurs, and give instructions on how to fix. Only disable this if you know what you're doing!")
    public boolean crashOnBiomeConflict = true;

    @ConfigEntry(category = GENERAL, key = "1.93_showOreLocations", origin = "SpaceConfig",
            comment = "Should ores indicate which planets they can be found on.")
    public boolean showOreLocations = true;

    @ConfigEntry(category = GENERAL, key = "1.94_combatPodDespawn", origin = "SpaceConfig",
            comment = "wether combat pods should despawn after a certian amount of time.")
    public boolean combatPodDespawn = false;

    // ════════════════════════ GeneralConfig (Space-Ergaenzungen) ════════════════════════

    @ConfigEntry(category = GENERAL, key = "1.42_threadedAtmospheres", origin = "GeneralConfig",
            comment = "If enabled, will run atmosphere blobbing in a separate thread for performance")
    public boolean enableThreadedAtmospheres = true;

    @ConfigEntry(category = GENERAL, key = "1.43_serverSafety", origin = "GeneralConfig",
            comment = "Allows for automated entity culling to function properly.")
    public boolean enableSacrilege = false;

    @ConfigEntry(category = GENERAL, key = "1.44_hardcoreDarkness", origin = "GeneralConfig",
            comment = "If enabled, sets night-time minimum fog to zero, to complement hardcore darkness mods")
    public boolean enableHardcoreDarkness = false;

    @ConfigEntry(category = GENERAL, key = "1.45_enableUnavoidableGravity", origin = "GeneralConfig",
            comment = "Requires extra large machines to have a proper foundation, for machines that have gravity by default, rather than by config (only applies to the Big-Ass Tank currently).")
    public boolean enableUnavoidableGravity = true;

    @ConfigEntry(category = LBSM, key = "LBSM_NeuCon", origin = "GeneralConfig",
            comment = "When enabled, Player Decontaminators can decontaminate radioactive items stemmed from neutron rads.")
    public boolean enableLBSMNeutronDecon = true;

    // ════════════════════════ MobConfig / RadiationConfig (Space-Ergaenzungen) ════════════════════════

    @ConfigEntry(category = MOBS, key = "12.D03_enableMunCows", origin = "MobConfig",
            comment = "Whether Mun Cows should spawn on the Mun")
    public boolean enableMunCows = true;

    @ConfigEntry(category = RADIATION, key = "CELESTIAL_00_radMultiplier", origin = "RadiationConfig",
            comment = "Radiation multiplier on celestial bodies, 1 = 1 rad/s on the Mun")
    public double celestialRadMultiplier = 0.25D;

    @ConfigEntry(category = HAZARD, key = "HAZ_01_disableNeutrons", origin = "RadiationConfig",
            comment = "When turned off, all neutron radiation hazards are disabled")
    public boolean disableNeutron = true;

    // ════════════════════════ ClientConfig (Space-Ergaenzung, hbmClient.json) ════════════════════════

    @ConfigEntry(category = CLIENT, key = "AUDIO_SHIP_HUM_VOLUME", origin = "ClientConfig", client = true,
            comment = "")  // Original ohne Kommentar; Lautstaerke von misc.stationhum
    public float audioShipHumVolume = 0.1F;

    // ════════════════════════ WorldConfig (Space-Ergaenzungen, Kategorie 02_ores) ════════════════════════

    /** Original {@code WorldConfig.ironSpawn} (Duna/Ike-Eisenadern); im Original nicht konfigurierbar. */
    public int ironSpawn = 9;

    @ConfigEntry(category = ORES, key = "2.24_nickelSpawnrate", origin = "WorldConfig",
            comment = "Amount of nickel ore veins per chunk")
    public int nickelSpawn = 12;
    @ConfigEntry(category = ORES, key = "2.25_zincSpawnrate", origin = "WorldConfig",
            comment = "Amount of zinc ore veins per chunk")
    public int zincSpawn = 8;
    @ConfigEntry(category = ORES, key = "2.26_mineralSpawnrate", origin = "WorldConfig",
            comment = "Amount of mineral ore veins per chunk")
    public int mineralSpawn = 4;
    @ConfigEntry(category = ORES, key = "2.27S_oilSpawnRate", origin = "WorldConfig",
            comment = "Spawns an oil bubble every nTH chunk (on Duna)")
    public int dunaOilSpawn = 100;
    @ConfigEntry(category = ORES, key = "2.28S_oilSpawnRate", origin = "WorldConfig",
            comment = "Spawns a DS oil bubble every nTH chunk (on Laythe)")
    public int laytheOilSpawn = 100;
    @ConfigEntry(category = ORES, key = "2.29S_gasSpawnRate", origin = "WorldConfig",
            comment = "Spawns a natural gas bubble every nTH chunk (on Eve)")
    public int eveGasSpawn = 100;
    @ConfigEntry(category = ORES, key = "2.30S_brineSpawnRate", origin = "WorldConfig",
            comment = "Spawns a brine bubble every nTH chunk (on Mun)")
    public int munBrineSpawn = 100;
    @ConfigEntry(category = ORES, key = "2.31S_brineSpawnRate", origin = "WorldConfig",
            comment = "Spawns a brine bubble every nTH chunk (on Minmus)")
    public int minmusBrineSpawn = 100;
    @ConfigEntry(category = ORES, key = "2.32S_brineSpawnRate", origin = "WorldConfig",
            comment = "Spawns a brine bubble every nTH chunk (on Ike)")
    public int ikeBrineSpawn = 100;
    @ConfigEntry(category = ORES, key = "2.O00_bedrockOilPerDeposit", origin = "WorldConfig",
            comment = "Oil extracted per bedrock oil block suck")
    public int bedrockOilPerDeposit = 100;
    @ConfigEntry(category = ORES, key = "2.O01_bedrockGasPerDepositMin", origin = "WorldConfig",
            comment = "Minimum natural gas extracted per bedrock oil block suck")
    public int bedrockGasPerDepositMin = 10;
    @ConfigEntry(category = ORES, key = "2.O02_bedrockGasPerDepositMax", origin = "WorldConfig",
            comment = "Maximum natural gas extracted per bedrock oil block suck")
    public int bedrockGasPerDepositMax = 50;
    @ConfigEntry(category = ORES, key = "2.O03_earthOilPerDeposit", origin = "WorldConfig",
            comment = "Oil extracted per Earth oil block suck")
    public int earthOilPerDeposit = 500;
    @ConfigEntry(category = ORES, key = "2.O04_earthGasPerDepositMin", origin = "WorldConfig",
            comment = "Minimum natural gas extracted per Earth oil block suck")
    public int earthGasPerDepositMin = 100;
    @ConfigEntry(category = ORES, key = "2.O05_earthGasPerDepositMax", origin = "WorldConfig",
            comment = "Maximum natural gas extracted per Earth oil block suck")
    public int earthGasPerDepositMax = 500;
    @ConfigEntry(category = ORES, key = "2.O06_earthOilDrainChance", origin = "WorldConfig",
            comment = "Chance for an Earth oil block to become empty on suck")
    public double earthOilDrainChance = 0.05D;
    @ConfigEntry(category = ORES, key = "2.O07_dunaOilPerDeposit", origin = "WorldConfig",
            comment = "Oil extracted per Duna oil block suck")
    public int dunaOilPerDeposit = 200;
    @ConfigEntry(category = ORES, key = "2.O08_dunaGasPerDepositMin", origin = "WorldConfig",
            comment = "Minimum natural gas extracted per Duna oil block suck")
    public int dunaGasPerDepositMin = 100;
    @ConfigEntry(category = ORES, key = "2.O09_dunaGasPerDepositMax", origin = "WorldConfig",
            comment = "Maximum natural gas extracted per Duna oil block suck")
    public int dunaGasPerDepositMax = 500;
    @ConfigEntry(category = ORES, key = "2.O10_dunaOilDrainChance", origin = "WorldConfig",
            comment = "Chance for a Duna oil block to become empty on suck")
    public double dunaOilDrainChance = 0.1D;
    @ConfigEntry(category = ORES, key = "2.O11_laytheOilPerDeposit", origin = "WorldConfig",
            comment = "Desulfurized Oil extracted per Laythe oil block suck")
    public int laytheOilPerDeposit = 500;
    @ConfigEntry(category = ORES, key = "2.O12_laytheGasPerDepositMin", origin = "WorldConfig",
            comment = "Minimum natural gas extracted per Laythe oil block suck")
    public int laytheGasPerDepositMin = 100;
    @ConfigEntry(category = ORES, key = "2.O13_laytheGasPerDepositMax", origin = "WorldConfig",
            comment = "Maximum natural gas extracted per Laythe oil block suck")
    public int laytheGasPerDepositMax = 500;
    @ConfigEntry(category = ORES, key = "2.O14_laytheOilDrainChance", origin = "WorldConfig",
            comment = "Chance for a Laythe oil block to become empty on suck")
    public double laytheOilDrainChance = 0.05D;
    @ConfigEntry(category = ORES, key = "2.O15_eveGasPerDeposit", origin = "WorldConfig",
            comment = "Natural Gas extracted per Eve gas block suck")
    public int eveGasPerDeposit = 500;
    @ConfigEntry(category = ORES, key = "2.O16_evePetPerDepositMin", origin = "WorldConfig",
            comment = "Minimum petroleum gas extracted per Eve oil block suck")
    public int evePetPerDepositMin = 20;
    @ConfigEntry(category = ORES, key = "2.O17_evePetPerDepositMax", origin = "WorldConfig",
            comment = "Maximum petroleum gas extracted per Eve oil block suck")
    public int evePetPerDepositMax = 100;
    @ConfigEntry(category = ORES, key = "2.O18_eveGasDrainChance", origin = "WorldConfig",
            comment = "Chance for an Eve gas block to become empty on suck")
    public double eveGasDrainChance = 0.05D;
    @ConfigEntry(category = ORES, key = "2.O19_munBrinePerDeposit", origin = "WorldConfig",
            comment = "Brine extracted per Mun brine block suck")
    public int munBrinePerDeposit = 300;
    @ConfigEntry(category = ORES, key = "2.O20_munBrineDrainChance", origin = "WorldConfig",
            comment = "Chance for an Mun brine block to become empty on suck")
    public double munBrineDrainChance = 0.05D;
    @ConfigEntry(category = ORES, key = "2.O21_minmusBrinePerDeposit", origin = "WorldConfig",
            comment = "Brine extracted per Minmus brine block suck")
    public int minmusBrinePerDeposit = 300;
    @ConfigEntry(category = ORES, key = "2.O22_minmusBrineDrainChance", origin = "WorldConfig",
            comment = "Chance for an Minmus brine block to become empty on suck")
    public double minmusBrineDrainChance = 0.05D;
    @ConfigEntry(category = ORES, key = "2.O23_ikeBrinePerDeposit", origin = "WorldConfig",
            comment = "Brine extracted per Ike brine block suck")
    public int ikeBrinePerDeposit = 300;
    @ConfigEntry(category = ORES, key = "2.O24_ikeBrineDrainChance", origin = "WorldConfig",
            comment = "Chance for an Ike brine block to become empty on suck")
    public double ikeBrineDrainChance = 0.05D;
    @ConfigEntry(category = ORES, key = "2.33S_tektoOilSpawnRate", origin = "WorldConfig",
            comment = "Spawns a Tekto oil bubble every nTH chunk (on Tekto)")
    public int tektoOilSpawn = 100;
    @ConfigEntry(category = ORES, key = "2.O25_tektoOilPerDeposit", origin = "WorldConfig",
            comment = "Oil extracted per Tekto oil block suck")
    public int tektoOilPerDeposit = 500;
    @ConfigEntry(category = ORES, key = "2.O26_tektoGasPerDepositMin", origin = "WorldConfig",
            comment = "Minimum natural gas extracted per Tekto oil block suck")
    public int tektoGasPerDepositMin = 100;
    @ConfigEntry(category = ORES, key = "2.O27_tektoGasPerDepositMax", origin = "WorldConfig",
            comment = "Maximum natural gas extracted per Tekto oil block suck")
    public int tektoGasPerDepositMax = 500;
    @ConfigEntry(category = ORES, key = "2.O28_tektoOilDrainChance", origin = "WorldConfig",
            comment = "Chance for a Tekto oil block to become empty on suck")
    public double tektoOilDrainChance = 0.05D;
    @ConfigEntry(category = ORES, key = "2.34_tektoBedrockOilSpawnRate", origin = "WorldConfig",
            comment = "Spawns a Tekto bedrock oil bubble every nTH chunk (on Tekto)")
    public int tektoBedrockOilSpawn = 200;
    @ConfigEntry(category = ORES, key = "2.O35_tektoBedrockOilPerDeposit", origin = "WorldConfig",
            comment = "Oil extracted per bedrock oil block suck")
    public int tektoBedrockOilPerDeposit = 100;
    @ConfigEntry(category = ORES, key = "2.O36_tektoBedrockGasPerDepositMin", origin = "WorldConfig",
            comment = "Minimum natural gas extracted per bedrock oil block suck")
    public int tektoBedrockGasPerDepositMin = 10;
    @ConfigEntry(category = ORES, key = "2.O37_tektoBedrockGasPerDepositMax", origin = "WorldConfig",
            comment = "Maximum natural gas extracted per bedrock oil block suck")
    public int tektoBedrockGasPerDepositMax = 50;
    @ConfigEntry(category = ORES, key = "2.B08_bedrockBismuthSpawn", origin = "WorldConfig",
            comment = "Spawns a bedrock bismuth deposit every nTH chunk")
    public int bedrockBismuthSpawn = 400;
    @ConfigEntry(category = ORES, key = "2.B09_bedrockCadmiumSpawn", origin = "WorldConfig",
            comment = "Spawns a bedrock cadmium deposit every nTH chunk")
    public int bedrockCadmiumSpawn = 400;

    // ════════════════════════ Singleton + Laden/Speichern ════════════════════════

    private static volatile SpaceConfig INSTANCE = new SpaceConfig();

    /** Aktuelle Werte (nie null). */
    public static SpaceConfig get() {
        return INSTANCE;
    }

    /** Laedt common.json + client.json (fehlende Dateien werden mit Standardwerten angelegt). Einmal im Mod-Konstruktor. */
    public static void load() {
        SpaceConfig cfg = new SpaceConfig();
        SpaceConfigStore.load(cfg, false);
        SpaceConfigStore.load(cfg, true);
        INSTANCE = cfg;
    }

    public static void save() {
        SpaceConfigStore.save(INSTANCE, false);
        SpaceConfigStore.save(INSTANCE, true);
    }
}
