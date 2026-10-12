package com.space_m.dim.duna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.space_m.block.SpaceBlocks;
import com.space_m.dim.PlanetOres;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Bloecke der Duna (Original {@code ModBlocks}, neu in NTM Space): {@code duna_sands}, {@code duna_cobble},
 * {@code duna_rock}, {@code dry_ice}, {@code ferric_clay}, dazu {@code stone_resource} Metadatum {@code CALCIUM}
 * (ebenfalls neu in NTM Space, Kalkstein-Adern aller Koerper und die weissen Baender der Duna-Huegel) und die
 * Duna-Varianten (Metadatum {@code Body.DUNA}) aller Erze aus {@code WorldGeneratorCelestial},
 * {@code WorldGeneratorDuna} und {@code ChunkProviderDuna} (Oel-Blasen).
 *
 * <p>Werte 1:1 {@code ModBlocks} (Haerte, Widerstand x0.6 wie {@link PlanetOres#rock}, Klang, Material ->
 * Kartenfarbe/Werkzeug). {@code duna_rock}/{@code duna_sands} werden auch von Eve und Tekto benutzt
 * ({@code BiomeGenEveOcean/River}, {@code BiomeGenTetrachloricRiver}).
 */
public final class DunaBlocks {

    private DunaBlocks() {}

    public static final String BODY = "duna";

    private static final List<RegistrySupplier<? extends Block>> SHOVEL = new ArrayList<>();

    /** {@code new BlockFalling(Material.sand).setHardness(0.5F)}, Sand-Klang ("Dunaian Sand"). */
    public static final RegistrySupplier<Block> DUNA_SANDS = shovel(SpaceBlocks.registerWithItem("duna_sands", () -> fallingSand(
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.SAND)), SpaceCreativeTabs.Tab.BLOCKS));

    /** {@code new BlockGeneric(Material.rock).setHardness(2.0F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> DUNA_COBBLE = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("duna_cobble",
            () -> new Block(PlanetOres.rock(2.0F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    // Original-Klasse: BlockGenericDrop (Beute ueber Loot-Tabelle duna_rock)
    /** {@code new BlockGenericDrop(Material.rock, duna_cobble).setHardness(1.5F).setResistance(10.0F)} - Beute {@code duna_cobble}. */
    public static final RegistrySupplier<Block> DUNA_ROCK = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("duna_rock",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    /** {@code new BlockGeneric(Material.ice).setHardness(0.5F)}, Stein-Klang, Textur {@code ice_dry}; Material ice braucht kein Werkzeug. */
    public static final RegistrySupplier<Block> DRY_ICE = SpaceBlocks.registerWithItem("dry_ice",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.5F).sound(SoundType.STONE)), SpaceCreativeTabs.Tab.BLOCKS);

    /** {@code new BlockGeneric(Material.clay).setHardness(0.6F)}, Kies-Klang (Material clay -> Schaufel). */
    public static final RegistrySupplier<Block> FERRIC_CLAY = shovel(SpaceBlocks.registerWithItem("ferric_clay",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(0.6F).sound(SoundType.GRAVEL)), SpaceCreativeTabs.Tab.BLOCKS));

    /**
     * {@code stone_resource} Metadatum {@code EnumStoneType.CALCIUM} ({@code BlockResourceStone}, Material.rock,
     * 5.0/10.0, Textur {@code stone_resource.calcium}; droppt sich selbst). HBM-Modernized hat nur die
     * 1.7.10-Typen ({@code stone_resource_limestone} ...), daher hier unter demselben Namensschema.
     */
    public static final RegistrySupplier<Block> STONE_RESOURCE_CALCIUM = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("stone_resource_calcium",
            () -> new Block(PlanetOres.rock(5.0F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    // ── WorldGeneratorCelestial.generateVanillaOres (Gestein duna_rock != stone) ──
    public static final RegistrySupplier<Block> ORE_IRON = PlanetOres.ore("ore_iron", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_GOLD = PlanetOres.ore("ore_gold", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_REDSTONE = PlanetOres.ore("ore_redstone", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_DIAMOND = PlanetOres.ore("ore_diamond", BODY, 3.0F, 5.0F);

    // ── WorldGeneratorCelestial.generateNTMOres (+ WorldGeneratorDuna: Eisen/Zink nutzen dieselben Bloecke) ──
    // Original-Klasse: BlockOreOutgas (= HBM com.hbm_m.block.gas.OutgasBlock, Koerper-Varianten ueber PlanetOres)
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

    // ── ChunkProviderDuna: Oel-Blasen (BlockOreFluid OIL, leer -> ore_oil_empty), BlockOre.addValidBody(ore_oil, DUNA) ──
    public static final RegistrySupplier<Block> ORE_OIL = PlanetOres.ore("ore_oil", BODY,
            p -> new com.space_m.resources.BlockOreFluid(p, () -> DunaBlocks.ORE_OIL_EMPTY.get(), com.hbm_m.api.space.SpaceResources.ReserveType.OIL, com.space_m.celestial.SolarSystem.Body.DUNA), 5.0F, 10.0F) /* R8-B: BlockOreFluid */;
    public static final RegistrySupplier<Block> ORE_OIL_EMPTY = PlanetOres.ore("ore_oil_empty", BODY, 5.0F, 10.0F);

    private static <T extends Block> RegistrySupplier<T> shovel(RegistrySupplier<T> block) {
        SHOVEL.add(block);
        return block;
    }

    /** Bloecke mit Material sand/clay ({@code minecraft:mineable/shovel}, Datagen {@code SpaceTagProviders}). */
    public static List<RegistrySupplier<? extends Block>> shovelBlocks() {
        return Collections.unmodifiableList(SHOVEL);
    }

    /** 1.7.10 {@code BlockFalling}: faellt, Staubpartikel in der Durchschnittsfarbe der Textur (#94412a). */
    private static Block fallingSand(BlockBehaviour.Properties p) {
        p = p.pushReaction(PushReaction.NORMAL);
        //? if < 1.21.1 {
        return new net.minecraft.world.level.block.SandBlock(0x94412A, p);
        //?} else {
        /*return new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xFF94412A), p);
        *///?}
    }

    /** Erzwingt das Laden der Klasse (Registrierung vor {@code SpaceBlocks.init()}). */
    public static void init() {
    }
}
