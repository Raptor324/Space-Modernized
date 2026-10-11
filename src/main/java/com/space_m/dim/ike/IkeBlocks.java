package com.space_m.dim.ike;

import java.util.function.Supplier;

import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.dim.PlanetOres;
import com.space_m.fluid.SpaceFluids;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.core.item.ArchitecturyBucketItem;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

/**
 * Bloecke von Ike (Original {@code ModBlocks}): Gestein {@code ike_stone}, Regolith {@code ike_regolith}
 * ({@code BlockTint} mit Zusatztexturen - Zufallstextur je Position, im Port als Blockstate-Varianten), die
 * Ike-Varianten (Metadatum {@code Body.IKE}) aller Erze aus {@code WorldGeneratorCelestial}, {@code WorldGeneratorIke}
 * und {@code ChunkProviderIke}, sowie die Welt-Fluessigkeit Alkylbromid ({@code bromine_fluid} + {@code bromine_block},
 * Seen auf Ike) mit Eimer {@code bucket_bromine} und der Gegenstand {@code divine_shard} (Sockel im Ike-Artefakt).
 *
 * <p>Werte 1:1 aus {@code ModBlocks}/{@code ModItems}; Texturen/Modelle/Beute unter {@code assets|data/space_m}
 * (Skript {@code scratchpad/sR3a_resources.py}).
 */
public final class IkeBlocks {

    private IkeBlocks() {}

    public static final String BODY = "ike";

    /** {@code ike_stone = new BlockGeneric(Material.rock).setHardness(1.5F).setResistance(10.0F)}. */
    public static final RegistrySupplier<Block> IKE_STONE = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("ike_stone",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));
    /**
     * {@code ike_regolith = new BlockTint(Material.rock, "ike_regolith2" ... "ike_regolith5")} 1.5/10.0. Die
     * Positions-Streuung {@code (l >> 16 & 3) % 5} waehlt nur die ersten vier Texturen (ike_regolith, 2, 3, 4) -
     * im Blockstate als vier gleichgewichtete Zufallsvarianten. {@code colorMultiplier} (Metadatum-Abdunklung)
     * ist bei Metadatum 0 weiss und entfaellt.
     */
    public static final RegistrySupplier<Block> IKE_REGOLITH = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("ike_regolith",
            () -> new Block(PlanetOres.rock(1.5F, 10.0F)), SpaceCreativeTabs.Tab.BLOCKS));

    // ── WorldGeneratorCelestial.generateVanillaOres (3.0/5.0) ──
    public static final RegistrySupplier<Block> ORE_IRON = PlanetOres.ore("ore_iron", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_GOLD = PlanetOres.ore("ore_gold", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_REDSTONE = PlanetOres.ore("ore_redstone", BODY, 3.0F, 5.0F);
    public static final RegistrySupplier<Block> ORE_DIAMOND = PlanetOres.ore("ore_diamond", BODY, 3.0F, 5.0F);

    // ── WorldGeneratorCelestial.generateNTMOres (+ WorldGeneratorIke: Kupfer, Lithium) ──
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

    // ── WorldGeneratorIke ──
    /** {@code BlockOreOutgas(rock, true, 5, true)} 5.0/15.0: gibt Asbestgas ab (wie HBM {@code asbestos_ore}). */
    public static final RegistrySupplier<Block> ORE_ASBESTOS = PlanetOres.ore("ore_asbestos", BODY,
            p -> new com.hbm_m.block.gas.OutgasBlock(p, () -> com.hbm_m.block.ModBlocks.GAS_ASBESTOS.get(), true, true, false, false, true), 5.0F, 15.0F);
    public static final RegistrySupplier<Block> ORE_LITHIUM = PlanetOres.ore("ore_lithium", BODY, 5.0F, 10.0F);
    /** {@code ore_coltan = new BlockOre(rock)} 15.0/10.0. */
    public static final RegistrySupplier<Block> ORE_COLTAN = PlanetOres.ore("ore_coltan", BODY, 15.0F, 10.0F);
    /** {@code ore_mineral = new BlockOre(rock)} 5.0/10.0 (neu in NTM Space, droppt sich selbst). */
    public static final RegistrySupplier<Block> ORE_MINERAL = PlanetOres.ore("ore_mineral", BODY, 5.0F, 10.0F);

    // ── ChunkProviderIke: Sole-Blasen (BlockOreFluid BRINE, leer -> ore_brine_empty) ──
    public static final RegistrySupplier<Block> ORE_BRINE = PlanetOres.ore("ore_brine", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_BRINE_EMPTY = PlanetOres.ore("ore_brine_empty", BODY, 5.0F, 10.0F);

    // ── Alkylbromid (ModBlocks.bromine_fluid / bromine_block, ModItems.bucket_bromine) ──
    // bromine_fluid = new GenericFluid("bromine_fluid").setDensity(3000).setViscosity(3000).setTemperature(273);
    // bromine_block = new GenericFluidBlock(bromine_fluid, Material.water, "bromine_still", "bromine_flowing")
    //                     .setDamage(ModDamageSource.acid, 5F).setResistance(500F)
    // BlockFluidClassic: quantaPerBlock 8 (Wasser-Ausbreitung, dropOff 1), tickRate = Viskositaet / 200 = 15.

    private static final RegistrySupplier<?>[] BROMINE_REFS = new RegistrySupplier<?>[2];

    @SuppressWarnings("unchecked")
    private static final SimpleArchitecturyFluidAttributes BROMINE_ATTRIBUTES = SimpleArchitecturyFluidAttributes
            .ofSupplier(() -> (Supplier<? extends Fluid>) BROMINE_REFS[1], () -> (Supplier<? extends Fluid>) BROMINE_REFS[0])
            .sourceTexture(SpaceMod.id("block/bromine_still"))
            .flowingTexture(SpaceMod.id("block/bromine_flowing"))
            .density(3000)
            .viscosity(3000)
            .luminosity(0)
            .temperature(273)
            .dropOff(1)
            .slopeFindDistance(4)
            .tickDelay(15)
            .explosionResistance(500F)
            .convertToSource(false)
            .blockSupplier(() -> IkeBlocks.BROMINE_BLOCK)
            .bucketItemSupplier(() -> IkeBlocks.BUCKET_BROMINE)
            .fillSound(SoundEvents.BUCKET_FILL)
            .emptySound(SoundEvents.BUCKET_EMPTY);

    public static final RegistrySupplier<Fluid> BROMINE_FLUID = SpaceFluids.FLUIDS.register("bromine_fluid",
            () -> new com.hbm_m.inventory.fluid.HbmFlowingFluid.Source(BROMINE_ATTRIBUTES));
    public static final RegistrySupplier<Fluid> BROMINE_FLUID_FLOWING = SpaceFluids.FLUIDS.register("bromine_fluid_flowing",
            () -> new com.hbm_m.inventory.fluid.HbmFlowingFluid.Flowing(BROMINE_ATTRIBUTES));

    static {
        BROMINE_REFS[0] = BROMINE_FLUID;
        BROMINE_REFS[1] = BROMINE_FLUID_FLOWING;
    }

    /** Welt-Fluessigkeit ohne eigenes Item (Original: Block ohne Kreativ-Tab, Beute keine). */
    public static final RegistrySupplier<LiquidBlock> BROMINE_BLOCK = SpaceBlocks.register("bromine_block",
            () -> new com.hbm_m.block.fluid.GenericFluidBlock(() -> (FlowingFluid) BROMINE_FLUID.get(),
                    com.hbm_m.platform.BlockProps.copy(Blocks.WATER).strength(100F, 500F).noLootTable())
                    .setDamage((level, e) -> com.hbm_m.damagesource.ModDamageSources.acid(level), 5F));

    /** {@code bucket_bromine = new ItemModBucket(bromine_block)}, blockTab, Rest-Eimer. */
    public static final RegistrySupplier<Item> BUCKET_BROMINE = SpaceItems.basic("bucket_bromine",
            () -> new ArchitecturyBucketItem(() -> (FlowingFluid) BROMINE_FLUID.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)),
            SpaceCreativeTabs.Tab.BLOCKS);

    /** {@code divine_shard = new Item().setCreativeTab(null)}: Inhalt des Sockels im Ike-Artefakt. */
    public static final RegistrySupplier<Item> DIVINE_SHARD = SpaceItems.basic("divine_shard",
            () -> new Item(new Item.Properties()), null);

    /** Erzwingt das Laden der Klasse (Registrierung vor {@code SpaceBlocks.init()}). */
    public static void init() {
    }
}
