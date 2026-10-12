package com.space_m.dim.eve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.dim.PlanetOres;
import com.space_m.fluid.SpaceFluids;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;

/**
 * Bloecke der Eve (Original {@code ModBlocks}, NTM Space): Planetengestein {@code eve_rock} (Textur
 * {@code eve_stone_2}), Oberflaeche {@code eve_silt} (fallend wie Sand), das Quecksilber-Meer {@code mercury_block}
 * ({@code mercury_fluid} + {@code bucket_mercury}), der Entladungsgeysir {@code geysir_electric}
 * ({@link BlockVolcanoV2}) und die Eve-Varianten (Metadatum {@code Body.EVE}) aller Erze, die
 * {@code WorldGeneratorCelestial}, {@code WorldGeneratorEve} und {@code ChunkProviderEve} auf der Eve erzeugen.
 *
 * <p>{@code basalt}, {@code basalt_smooth}, {@code ore_depth_nether_neodymium} ({@code depth_nether_neodymium}),
 * {@code stone_depth} und {@code sulfuric_acid_block} gibt es schon in HBM-Modernized.
 */
public final class EveBlocks {

    private EveBlocks() {}

    public static final String BODY = "eve";

    /** Bloecke mit Schaufel ({@code Material.sand}). */
    private static final List<RegistrySupplier<? extends Block>> SHOVEL = new ArrayList<>();

    /**
     * {@code eve_rock = new BlockGeneric(Material.rock).setStepSound(soundTypeStone).setHardness(1.5F)
     * .setResistance(10.0F).setBlockTextureName("eve_stone_2")}.
     */
    public static final RegistrySupplier<Block> EVE_ROCK = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("eve_rock",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    /**
     * {@code eve_silt = new BlockFalling(Material.sand).setStepSound(soundTypeSand).setHardness(0.5F)} - faellt wie
     * Sand, kein Werkzeug noetig (Schaufel schneller).
     */
    public static final RegistrySupplier<Block> EVE_SILT = shovel(SpaceBlocks.registerWithItem("eve_silt",
            () -> new Silt(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE)
                    .strength(0.5F).sound(SoundType.SAND)), SpaceCreativeTabs.Tab.BLOCKS));

    // ── Quecksilber (Original mercury_fluid / mercury_block / bucket_mercury) ──

    private static final RegistrySupplier<?>[] MERCURY_REFS = new RegistrySupplier[2];

    /**
     * {@code mercury_fluid = new GenericFluid("mercury_fluid").setDensity(1840).setViscosity(1000).setTemperature(273)};
     * {@code BlockFluidClassic} mit Standard-{@code quantaPerBlock} 8 (= Vanilla {@code dropOff} 1), Tickrate =
     * Viskositaet / 200 = 5 (wie HBM-Modernized {@code WorldFluids.SULFURIC_ACID}, gleiche Werte).
     */
    @SuppressWarnings("unchecked")
    private static final SimpleArchitecturyFluidAttributes MERCURY_ATTRIBUTES = SimpleArchitecturyFluidAttributes
            .ofSupplier(() -> (Supplier<? extends Fluid>) MERCURY_REFS[1], () -> (Supplier<? extends Fluid>) MERCURY_REFS[0])
            .sourceTexture(com.space_m.SpaceMod.id("block/mercury_still"))
            .flowingTexture(com.space_m.SpaceMod.id("block/mercury_flowing"))
            .density(1840)
            .viscosity(1000)
            .luminosity(0)
            .temperature(273)
            .dropOff(1)
            .slopeFindDistance(4)
            .tickDelay(5)
            .explosionResistance(500F)
            .convertToSource(false)
            .blockSupplier(() -> EveBlocks.MERCURY_BLOCK)
            .bucketItemSupplier(() -> EveBlocks.BUCKET_MERCURY)
            .fillSound(SoundEvents.BUCKET_FILL)
            .emptySound(SoundEvents.BUCKET_EMPTY);

    public static final RegistrySupplier<Fluid> MERCURY_FLUID = SpaceFluids.FLUIDS.register("mercury_fluid",
            () -> new com.hbm_m.inventory.fluid.HbmFlowingFluid.Source(MERCURY_ATTRIBUTES));
    public static final RegistrySupplier<Fluid> MERCURY_FLUID_FLOWING = SpaceFluids.FLUIDS.register("mercury_fluid_flowing",
            () -> new com.hbm_m.inventory.fluid.HbmFlowingFluid.Flowing(MERCURY_ATTRIBUTES));

    static {
        MERCURY_REFS[0] = MERCURY_FLUID;
        MERCURY_REFS[1] = MERCURY_FLUID_FLOWING;
    }

    /**
     * {@code mercury_block = new GenericFluidBlock(mercury_fluid, Material.water, "mercury_still", "mercury_flowing")
     * .setDamage(ModDamageSource.lead, 1F).setResistance(500F)}: der Schaden wirkt im Original nur fuer
     * {@code sulfuric_acid_block} ({@code onEntityCollidedWithBlock} prueft {@code this == sulfuric_acid_block}),
     * Quecksilber verletzt also nicht - hier daher ohne Schadensquelle.
     */
    public static final RegistrySupplier<LiquidBlock> MERCURY_BLOCK = SpaceBlocks.register("mercury_block",
            () -> new com.hbm_m.block.fluid.GenericFluidBlock(() -> (FlowingFluid) MERCURY_FLUID.get(),
                    com.hbm_m.platform.BlockProps.copy(Blocks.WATER).mapColor(MapColor.METAL).strength(100F, 500F).noLootTable()));

    /** {@code bucket_mercury = new ItemModBucket(mercury_block).setContainerItem(Items.bucket)}, blockTab. */
    public static final RegistrySupplier<Item> BUCKET_MERCURY = SpaceItems.register("bucket_mercury",
            () -> new dev.architectury.core.item.ArchitecturyBucketItem(() -> (FlowingFluid) MERCURY_FLUID.get(),
                    new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)),
            SpaceCreativeTabs.Tab.BLOCKS);

    // ── Entladungsgeysir ──

    /**
     * {@code geysir_electric = new BlockVolcanoV2(Material.rock).setLightLevel(1.0F).setStepSound(soundTypeStone)
     * .setHardness(2.0F).setBlockTextureName("basalt")} (Widerstand = Haerte * 5 = 10 -> 1.20 wie 1.7.10 * 3 / 5 = 6),
     * ohne Creative-Tab (wie im Original).
     */
    public static final RegistrySupplier<BlockVolcanoV2> GEYSIR_ELECTRIC = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("geysir_electric",
            () -> new BlockVolcanoV2(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.STONE).lightLevel(s -> 15).noOcclusion()),
            null, b -> new BlockItem(b, new Item.Properties())));

    /** Original-Mapping {@code tileentity_electric_volcano}. */
    public static final RegistrySupplier<BlockEntityType<BlockVolcanoV2.TileEntityLightningVolcano>> ELECTRIC_VOLCANO_BE =
            SpaceBlockEntities.register("electric_volcano", BlockVolcanoV2.TileEntityLightningVolcano::new, GEYSIR_ELECTRIC);

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

    // ── WorldGeneratorEve ──
    /** {@code ore_niobium = new BlockOre(Material.rock).setHardness(5.0F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> ORE_NIOBIUM = PlanetOres.ore("ore_niobium", BODY, 5.0F, 10.0F);
    /** {@code ore_iodine = new BlockOre(Material.rock).setHardness(5.0F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> ORE_IODINE = PlanetOres.ore("ore_iodine", BODY, 5.0F, 10.0F);
    /** {@code ore_schrabidium = new BlockOre(Material.rock).setHardness(15.0F).setResistance(600.0F)} (Space: ohne Strahlungswerte). */
    public static final RegistrySupplier<Block> ORE_SCHRABIDIUM = PlanetOres.ore("ore_schrabidium", BODY, 15.0F, 600.0F);

    // ── ChunkProviderEve: Erdgas-Blasen (BlockOreFluid GAS, leer -> ore_gas_empty) ──
    /** {@code ore_gas = new BlockOreFluid(Material.rock, ore_gas_empty, ReserveType.GAS)} 5.0/10.0. */
    public static final RegistrySupplier<Block> ORE_GAS = PlanetOres.ore("ore_gas", BODY,
            p -> new com.space_m.resources.BlockOreFluid(p, () -> EveBlocks.ORE_GAS_EMPTY.get(), com.hbm_m.api.space.SpaceResources.ReserveType.GAS, com.space_m.celestial.SolarSystem.Body.EVE), 5.0F, 10.0F) /* R8-B: BlockOreFluid */;
    /** {@code ore_gas_empty = new BlockOre(Material.rock).setBlockTextureName("ore_oil_empty")} 5.0/10.0. */
    public static final RegistrySupplier<Block> ORE_GAS_EMPTY = PlanetOres.ore("ore_gas_empty", BODY, 5.0F, 10.0F);

    private static <T extends Block> RegistrySupplier<T> shovel(RegistrySupplier<T> block) {
        SHOVEL.add(block);
        return block;
    }

    /** Bloecke fuer {@code minecraft:mineable/shovel} (Datagen {@code SpaceTagProviders}). */
    public static List<RegistrySupplier<? extends Block>> shovelBlocks() {
        return Collections.unmodifiableList(SHOVEL);
    }

    /** 1.7.10 {@code BlockFalling} (eve_silt). */
    public static class Silt extends FallingBlock {
        public Silt(BlockBehaviour.Properties properties) {
            super(properties);
        }

        //? if >= 1.21.1 {
        /*public static final com.mojang.serialization.MapCodec<Silt> CODEC = simpleCodec(Silt::new);

        @Override
        protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec() {
            return CODEC;
        }
        *///?}
    }

    /** Erzwingt das Laden der Klasse (Registrierung vor {@code SpaceBlocks.init()}). */
    public static void init() {
    }
}
