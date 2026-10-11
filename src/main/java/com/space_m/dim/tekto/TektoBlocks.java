package com.space_m.dim.tekto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.dim.PlanetOres;
import com.space_m.dim.laythe.CelestialOreSet;
import com.space_m.dim.tekto.block.BlockGeysierDCM;
import com.space_m.dim.tekto.block.BlockNTSapling;
import com.space_m.dim.tekto.block.BlockRubberPlant;
import com.space_m.dim.tekto.block.BlockVinylVine;
import com.space_m.dim.tekto.block.RubberGrass;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.core.fluid.SimpleArchitecturyFluidAttributes;
import dev.architectury.core.item.ArchitecturyBucketItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Bloecke/Items/Fluessigkeit von Tekto (Original {@code ModBlocks}/{@code ModItems}, Werte 1:1, Widerstand wie
 * {@link PlanetOres#rock}). Planetengestein ist {@code hbm_m:basalt} ({@code WorldProviderTekto.getStone}), Erz-
 * Varianten {@code *_tekto} mit Basalt-Textur, Meer aus Tetrachlorkohlenstoff ({@code ccl_block}).
 */
public final class TektoBlocks {

    private TektoBlocks() {}

    public static final String BODY = "tekto";

    /** Eigene Registries (die gemeinsamen Fluid-Register sind beim Laden der Koerper schon angemeldet). */
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.FLUID);

    private static final Map<TagKey<Block>, List<Supplier<? extends Block>>> TAGS = new LinkedHashMap<>();

    private static <T extends Block> RegistrySupplier<T> tag(RegistrySupplier<T> b, TagKey<Block>... tags) {
        for (TagKey<Block> t : tags) TAGS.computeIfAbsent(t, k -> new ArrayList<>()).add(b);
        return b;
    }

    // ── Boden ──
    /** {@code rubber_grass = RubberGrass(Material.grass, false)}, 0.5/2.5, Gras-Klang. */
    public static final RegistrySupplier<Block> RUBBER_GRASS = tag(SpaceBlocks.registerWithItem("rubber_grass",
            () -> new RubberGrass(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.5F, 1.5F).sound(SoundType.GRASS)),
            SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_SHOVEL);
    /** {@code rubber_silt = BlockGeneric(Material.sand)}, 1.0/1.0, Kies-Klang. */
    public static final RegistrySupplier<Block> RUBBER_SILT = tag(SpaceBlocks.registerWithItem("rubber_silt",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.0F, 0.6F).sound(SoundType.GRAVEL)),
            SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_SHOVEL);
    /** {@code vinyl_sand = BlockFalling(Material.sand)}, 1.0/1.0, Sand-Klang (Textur {@code sand_vinyl}). */
    public static final RegistrySupplier<Block> VINYL_SAND = tag(SpaceBlocks.registerWithItem("vinyl_sand",
            () -> new com.space_m.dim.laythe.block.BlockFallingTint(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.0F, 0.6F).sound(SoundType.SAND)),
            SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_SHOVEL);

    /** {@code geysir_chloric = BlockGeysierDCM(Material.rock)}, Licht 15, Haerte 2.0, Stein-Klang. */
    public static final RegistrySupplier<Block> GEYSIR_CHLORIC = PlanetOres.pickaxe(SpaceBlocks.registerWithItem("geysir_chloric",
            () -> new BlockGeysierDCM(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .lightLevel(s -> 15).strength(2.0F).sound(SoundType.STONE).noOcclusion()),
            SpaceCreativeTabs.Tab.BLOCKS));
    /** {@code tileentity_dcm}. */
    public static final RegistrySupplier<BlockEntityType<BlockGeysierDCM.TileEntityDCM>> DCM =
            SpaceBlockEntities.register("dcm", BlockGeysierDCM.TileEntityDCM::new, GEYSIR_CHLORIC);

    // ── Baeume ──
    private static BlockBehaviour.Properties wood(float h, float r) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(h, r).sound(SoundType.WOOD).ignitedByLava();
    }

    private static BlockBehaviour.Properties leaves() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.5F, 1.5F).randomTicks().sound(SoundType.GRASS)
                .noOcclusion().isValidSpawn((s, l, p, t) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                .ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, p) -> false);
    }

    /** {@code pvc_log = BlockLogNT}, 0.5/2.5, Holz-Klang. */
    public static final RegistrySupplier<Block> PVC_LOG = tag(SpaceBlocks.registerWithItem("pvc_log",
            () -> new RotatedPillarBlock(wood(0.5F, 1.5F)), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS);
    /** {@code vinyl_log = BlockLogNT}, 0.5/2.5. */
    public static final RegistrySupplier<Block> VINYL_LOG = tag(SpaceBlocks.registerWithItem("vinyl_log",
            () -> new RotatedPillarBlock(wood(0.5F, 1.5F)), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_AXE, BlockTags.LOGS);
    /** {@code rubber_leaves = BlockRubberLeaves}, 0.5/2.5. */
    public static final RegistrySupplier<Block> RUBBER_LEAVES = tag(SpaceBlocks.registerWithItem("rubber_leaves",
            () -> new LeavesBlock(leaves()), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES);
    /** {@code pet_leaves = BlockRubberLeaves}, 0.5/2.5. */
    public static final RegistrySupplier<Block> PET_LEAVES = tag(SpaceBlocks.registerWithItem("pet_leaves",
            () -> new LeavesBlock(leaves()), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_HOE, BlockTags.LEAVES);
    /** {@code vinyl_vines = BlockVinylVine} ("vinyl_vine"), 1.0/1.0. */
    public static final RegistrySupplier<Block> VINYL_VINES = tag(SpaceBlocks.registerWithItem("vinyl_vine",
            () -> new BlockVinylVine(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().randomTicks()
                    .strength(1.0F, 0.6F).sound(SoundType.VINE).ignitedByLava().pushReaction(PushReaction.DESTROY)),
            SpaceCreativeTabs.Tab.BLOCKS), BlockTags.CLIMBABLE);
    /** {@code vinyl_planks = BlockGeneric(Material.wood)}, 1.0/1.0. */
    public static final RegistrySupplier<Block> VINYL_PLANKS = tag(SpaceBlocks.registerWithItem("vinyl_planks",
            () -> new Block(wood(1.0F, 0.6F)), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_AXE, BlockTags.PLANKS);
    /** {@code pvc_planks = BlockGeneric(Material.wood)}, 1.0/1.0. */
    public static final RegistrySupplier<Block> PVC_PLANKS = tag(SpaceBlocks.registerWithItem("pvc_planks",
            () -> new Block(wood(1.0F, 0.6F)), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.MINEABLE_WITH_AXE, BlockTags.PLANKS);

    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.NONE).pushReaction(PushReaction.DESTROY);
    }

    /** {@code sapling_pvc = BlockNTSapling} Sorte VINYL ("sapling_vinyl"). */
    public static final RegistrySupplier<Block> SAPLING_VINYL = tag(SpaceBlocks.registerWithItem("sapling_vinyl",
            () -> new BlockNTSapling(plant(), BlockNTSapling.EnumSapling.VINYL), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.SAPLINGS);
    /** {@code sapling_pvc = BlockNTSapling} Sorte PVC ("sapling_pvc"). */
    public static final RegistrySupplier<Block> SAPLING_PVC = tag(SpaceBlocks.registerWithItem("sapling_pvc",
            () -> new BlockNTSapling(plant(), BlockNTSapling.EnumSapling.PVC), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.SAPLINGS);

    // ── Pflanzen (BlockEnumMulti -> je Sorte ein Block) ──
    /** {@code BlockRubberPlant.EnumRubberPlantType}. */
    public static final String[] RUBBER_PLANTS = {"grass", "fern", "shrub"};
    /** {@code BlockRubberCacti.EnumBushType}. */
    public static final String[] RUBBER_CACTI = {"cact", "bush", "flower"};

    private static final Predicate<BlockState> RUBBER_SOIL = s -> s.is(RUBBER_GRASS.get()) || s.is(RUBBER_SILT.get());
    private static final Predicate<BlockState> CACTI_SOIL = s -> s.is(VINYL_SAND.get()) || s.is(RUBBER_GRASS.get()) || s.is(RUBBER_SILT.get());

    /** {@code rubber_plant} (Material.vine, Haerte 0, Gras-Klang), Registry {@code rubber_plant_<sorte>}. */
    public static final Map<String, RegistrySupplier<Block>> RUBBER_PLANT = new LinkedHashMap<>();
    /** {@code spike_cacti} ("rubber_tall", Material.plants, Haerte 0), Registry {@code rubber_tall_<sorte>}. */
    public static final Map<String, RegistrySupplier<Block>> SPIKE_CACTI = new LinkedHashMap<>();

    static {
        for (String t : RUBBER_PLANTS) RUBBER_PLANT.put(t, tag(SpaceBlocks.registerWithItem("rubber_plant_" + t,
                () -> new BlockRubberPlant(plant().replaceable(), RUBBER_SOIL), SpaceCreativeTabs.Tab.BLOCKS), BlockTags.REPLACEABLE_BY_TREES));
        for (String t : RUBBER_CACTI) SPIKE_CACTI.put(t, SpaceBlocks.registerWithItem("rubber_tall_" + t,
                () -> new BlockRubberPlant(plant(), CACTI_SOIL), SpaceCreativeTabs.Tab.BLOCKS));
    }

    // ── Tetrachlorkohlenstoff (ccl_fluid/ccl_block/bucket_ccl) ──
    /** {@code ccl_fluid = GenericFluid("ccl_fluid")}: Dichte 1840, Zaehigkeit 1000, Temperatur 273. */
    public static final SimpleArchitecturyFluidAttributes CCL_ATTRIBUTES = SimpleArchitecturyFluidAttributes
            .ofSupplier(() -> TektoBlocks.CCL_FLOWING, () -> TektoBlocks.CCL_FLUID)
            .sourceTexture(SpaceMod.id("block/ccl")).flowingTexture(SpaceMod.id("block/ccl_flowing"))
            .density(1840).viscosity(1000).temperature(273)
            .dropOff(1).slopeFindDistance(4).tickDelay(5).explosionResistance(500F).convertToSource(false)
            .blockSupplier(() -> TektoBlocks.CCL_BLOCK).bucketItemSupplier(() -> TektoBlocks.BUCKET_CCL)
            .fillSound(SoundEvents.BUCKET_FILL).emptySound(SoundEvents.BUCKET_EMPTY);
    public static final RegistrySupplier<Fluid> CCL_FLUID = FLUIDS.register("ccl_fluid",
            () -> new com.hbm_m.inventory.fluid.HbmFlowingFluid.Source(CCL_ATTRIBUTES));
    public static final RegistrySupplier<Fluid> CCL_FLOWING = FLUIDS.register("ccl_fluid_flowing",
            () -> new com.hbm_m.inventory.fluid.HbmFlowingFluid.Flowing(CCL_ATTRIBUTES));
    /** {@code ccl_block = GenericFluidBlock(ccl_fluid, Material.water).setDamage(lead, 1F).setResistance(500F)}. */
    public static final RegistrySupplier<LiquidBlock> CCL_BLOCK = SpaceBlocks.register("ccl_block",
            () -> new com.hbm_m.block.fluid.GenericFluidBlock(() -> (FlowingFluid) CCL_FLUID.get(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).replaceable().noCollission().strength(100F, 500F)
                            .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY))
                    .setDamage((level, e) -> com.hbm_m.damagesource.ModDamageSources.lead(level), 1F));
    /** {@code bucket_ccl = ItemModBucket(ccl_block)} (blockTab). */
    public static final RegistrySupplier<Item> BUCKET_CCL = SpaceItems.basic("bucket_ccl",
            () -> new ArchitecturyBucketItem(CCL_FLUID, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)), SpaceCreativeTabs.Tab.BLOCKS);

    // ── Erze (generateVanillaOres + generateNTMOres im Basalt, Metadatum TEKTO) ──
    public static final CelestialOreSet ORES = CelestialOreSet.create(BODY, true, true);
    /** ChunkProviderTekto: {@code ore_tekto} (BlockOreFluid OIL -> Tekto-Rohoel + Wasserstoffgas), 5.0/10.0. */
    public static final RegistrySupplier<Block> ORE_TEKTO = PlanetOres.ore("ore_tekto", BODY, 5.0F, 10.0F);
    /** {@code ore_tekto_empty = BlockOre} (Textur {@code ore_oil_empty}), 5.0/10.0. */
    public static final RegistrySupplier<Block> ORE_TEKTO_EMPTY = PlanetOres.ore("ore_tekto_empty", BODY, 5.0F, 10.0F);
    /** MapGenBedrockOil: {@code ore_bedrock_oil} (unzerstoerbar, Widerstand 1 000 000) mit Metadatum TEKTO. */
    public static final RegistrySupplier<Block> ORE_BEDROCK_OIL = PlanetOres.ore("ore_bedrock_oil", BODY,
            p -> new Block(p.strength(-1.0F, 600_000F).noLootTable().isValidSpawn((s, l, pos, t) -> false)), -1.0F, 1_000_000F);

    // ── Items ──
    /** {@code leaf_rubber} ("rubber_leaves", partsTab). */
    public static final RegistrySupplier<Item> LEAF_RUBBER = SpaceItems.basic("leaf_rubber", () -> new Item(new Item.Properties()), SpaceCreativeTabs.Tab.PARTS);
    /** {@code leaf_pet} ("pet_leaves", partsTab). */
    public static final RegistrySupplier<Item> LEAF_PET = SpaceItems.basic("leaf_pet", () -> new Item(new Item.Properties()), SpaceCreativeTabs.Tab.PARTS);

    /** Tags (Datagen {@code SpaceTagProviders}). */
    public static void addTags(BiConsumer<TagKey<Block>, Block> tag) {
        TAGS.forEach((k, list) -> list.forEach(b -> tag.accept(k, b.get())));
    }

    public static void init() {
        FLUIDS.register();
    }
}
