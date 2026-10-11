package com.space_m.dim.moho;

import com.space_m.block.SpaceBlocks;
import com.space_m.dim.PlanetOres;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;

/**
 * Bloecke des Moho (Original {@code ModBlocks}, NTM Space): {@code moho_stone} (Planetengestein), {@code moho_regolith}
 * (Oberflaeche) und die Moho-Varianten (Metadatum {@code Body.MOHO}) aller Erze aus {@code WorldGeneratorCelestial} und
 * {@code WorldGeneratorMoho}. {@code basalt}, {@code ore_basalt_*} (Original {@code ore_basalt} Meta 0-4),
 * {@code volcano_core}, {@code geysir_nether} gibt es schon in HBM-Modernized.
 */
public final class MohoBlocks {

    private MohoBlocks() {}

    public static final String BODY = "moho";

    /** {@code moho_stone = new BlockGeneric(Material.rock).setStepSound(soundTypeStone).setHardness(1.5F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> MOHO_STONE = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("moho_stone",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));
    /** {@code moho_regolith = new BlockGeneric(Material.rock).setStepSound(soundTypeStone).setHardness(1.5F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> MOHO_REGOLITH = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("moho_regolith",
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

    // ── WorldGeneratorMoho ──
    /** {@code ore_mineral = new BlockOre(Material.rock).setHardness(5.0F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> ORE_MINERAL = PlanetOres.ore("ore_mineral", BODY, 5.0F, 10.0F);
    /** {@code ore_glowstone = new BlockOre(Material.rock).setLightLevel(4F/15F).setHardness(5.0F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> ORE_GLOWSTONE = PlanetOres.ore("ore_glowstone", BODY,
            p -> new Block(p.lightLevel(s -> 4)), 5.0F, 10.0F);
    /** {@code ore_fire = new BlockOre(Material.rock).setHardness(0.4F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> ORE_FIRE = PlanetOres.ore("ore_fire", BODY, 0.4F, 10.0F);
    /** {@code ore_australium = new BlockOre(Material.rock).setHardness(5.0F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> ORE_AUSTRALIUM = PlanetOres.ore("ore_australium", BODY, 5.0F, 10.0F);
    /** {@code ore_shale = new BlockOre(Material.rock).setHardness(1.5F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> ORE_SHALE = PlanetOres.ore("ore_shale", BODY, 1.5F, 10.0F);

    /** Erzwingt das Laden der Klasse (Registrierung vor {@code SpaceBlocks.init()}). */
    public static void init() {
    }
}
