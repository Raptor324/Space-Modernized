package com.space_m.dim.moon;

import com.space_m.block.SpaceBlocks;
import com.space_m.dim.PlanetOres;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;

/**
 * Bloecke der Mun (Original {@code ModBlocks}): {@code moon_rock} (neu in NTM Space) und die Mun-Varianten
 * (Metadatum {@code Body.MUN}) aller Erze, die {@code WorldGeneratorCelestial}, {@code WorldGeneratorMoon} und
 * {@code ChunkProviderMoon} auf der Mun erzeugen. {@code moon_turf} (Oberflaeche) und {@code basalt}
 * (Mare/"Meer") sind schon in HBM-Modernized ({@code hbm_m:moon_turf}, {@code hbm_m:basalt}).
 *
 * <p>Werte (Haerte/Widerstand/Klasse) 1:1 aus {@code ModBlocks}; Texturen/Modelle/Beute/Lang unter
 * {@code assets|data/space_m} (Skript {@code scratchpad/sR1b_resources.py}).
 */
public final class MunBlocks {

    private MunBlocks() {}

    public static final String BODY = "mun";

    /** {@code moon_rock = new BlockGeneric(Material.rock).setHardness(1.5F).setResistance(10.0F)}, Stein-Klang. */
    public static final RegistrySupplier<Block> MOON_ROCK = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("moon_rock",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    // ── WorldGeneratorCelestial.generateVanillaOres (BlockOre mit Vanilla-Gegenstueck, 3.0/5.0) ──
    public static final RegistrySupplier<Block> ORE_IRON = PlanetOres.ore("ore_iron", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_GOLD = PlanetOres.ore("ore_gold", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_REDSTONE = PlanetOres.ore("ore_redstone", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_DIAMOND = PlanetOres.ore("ore_diamond", BODY, 3.0F, 5.0F);

    // ── WorldGeneratorCelestial.generateNTMOres ──
    /** {@code BlockOreOutgas(rock, true, 5, true)}: gibt Radon ab (HBM {@code OutgasBlock}). */
    public static final RegistrySupplier<Block> ORE_URANIUM = PlanetOres.ore("ore_uranium", BODY,
            p -> new com.hbm_m.block.gas.OutgasBlock(p, () -> com.hbm_m.block.ModBlocks.GAS_RADON.get(), true, true), 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_THORIUM = PlanetOres.ore("ore_thorium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_TITANIUM = PlanetOres.ore("ore_titanium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_SULFUR = PlanetOres.ore("ore_sulfur", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_ALUMINIUM = PlanetOres.ore("ore_aluminium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_COPPER = PlanetOres.ore("ore_copper", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_NICKEL = PlanetOres.ore("ore_nickel", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_ZINC = PlanetOres.ore("ore_zinc", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_FLUORITE = PlanetOres.ore("ore_fluorite", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_NITER = PlanetOres.ore("ore_niter", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_TUNGSTEN = PlanetOres.ore("ore_tungsten", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_LEAD = PlanetOres.ore("ore_lead", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_BERYLLIUM = PlanetOres.ore("ore_beryllium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> ORE_RARE = PlanetOres.ore("ore_rare", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_CINNEBAR = PlanetOres.ore("ore_cinnebar", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_COBALT = PlanetOres.ore("ore_cobalt", BODY, 5.0F, 10.0F);
    /** {@code BlockCluster(rock)} 5.0/15.0. */
    public static final RegistrySupplier<Block> CLUSTER_IRON = PlanetOres.ore("cluster_iron", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_TITANIUM = PlanetOres.ore("cluster_titanium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_ALUMINIUM = PlanetOres.ore("cluster_aluminium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_COPPER = PlanetOres.ore("cluster_copper", BODY, 5.0F, 15.0F);

    // ── WorldGeneratorMoon ──
    public static final RegistrySupplier<Block> ORE_LITHIUM = PlanetOres.ore("ore_lithium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_QUARTZ = PlanetOres.ore("ore_quartz", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_SHALE = PlanetOres.ore("ore_shale", BODY, 1.5F, 10.0F);

    // ── ChunkProviderMoon: Sole-Blasen (BlockOreFluid BRINE, leer -> ore_brine_empty) ──
    public static final RegistrySupplier<Block> ORE_BRINE = PlanetOres.ore("ore_brine", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_BRINE_EMPTY = PlanetOres.ore("ore_brine_empty", BODY, 5.0F, 10.0F);

    /** Erzwingt das Laden der Klasse (Registrierung vor {@code SpaceBlocks.init()}). */
    public static void init() {
    }
}
