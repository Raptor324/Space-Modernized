package com.space_m.dim.dres;

import com.space_m.block.SpaceBlocks;
import com.space_m.dim.PlanetOres;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;

/**
 * Bloecke des Dres (Original {@code ModBlocks}): {@code dres_rock} (neu in NTM Space, Textur {@code dresbase}) und die
 * Dres-Varianten (Metadatum {@code Body.DRES}) aller Erze aus {@code WorldGeneratorCelestial} und
 * {@code WorldGeneratorDres}. Die Oberflaeche {@code sellafield_slaked} ist schon in HBM-Modernized.
 *
 * <p>{@code ore_niobium} ist neu in NTM Space, {@code ore_lanthanium}/{@code ore_coltan} haben in 1.7.10 keine
 * Oberwelt-Variante in HBM-Modernized ({@code coltan_ore} = Kerbin): Werte 1:1 {@code ModBlocks}
 * (Niob 5/10, Lanthan/Coltan 15/10).
 */
public final class DresBlocks {

    private DresBlocks() {}

    public static final String BODY = "dres";

    /** {@code new BlockGeneric(Material.rock).setHardness(1.5F).setResistance(10.0F)}, Textur {@code dresbase}. */
    public static final RegistrySupplier<Block> DRES_ROCK = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("dres_rock",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    // ── WorldGeneratorCelestial.generateVanillaOres ──
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
    public static final RegistrySupplier<Block> CLUSTER_IRON = PlanetOres.ore("cluster_iron", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_TITANIUM = PlanetOres.ore("cluster_titanium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_ALUMINIUM = PlanetOres.ore("cluster_aluminium", BODY, 5.0F, 15.0F);
    public static final RegistrySupplier<Block> CLUSTER_COPPER = PlanetOres.ore("cluster_copper", BODY, 5.0F, 15.0F);

    // ── WorldGeneratorDres ──
    public static final RegistrySupplier<Block> ORE_NIOBIUM = PlanetOres.ore("ore_niobium", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_COLTAN = PlanetOres.ore("ore_coltan", BODY, 15.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_LANTHANIUM = PlanetOres.ore("ore_lanthanium", BODY, 15.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_SHALE = PlanetOres.ore("ore_shale", BODY, 1.5F, 10.0F);

    /** Erzwingt das Laden der Klasse (Registrierung vor {@code SpaceBlocks.init()}). */
    public static void init() {
    }
}
