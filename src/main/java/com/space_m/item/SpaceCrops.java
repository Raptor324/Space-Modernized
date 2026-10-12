package com.space_m.item;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.space_m.SpaceMod;
import com.space_m.atmosphere.IPlantableBreathing;
import com.space_m.block.SpaceBlocks;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.dim.tekto.TektoBlocks;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * R4-B: Feldfruechte von NTM Space ({@code com.hbm.blocks.BlockCrop}: {@code crop_mint}, {@code crop_coffee},
 * {@code crop_tea}, {@code crop_paraffin}) und {@code rubber_farmland} ({@code BlockRubberFarm}). {@code crop_strawberry}
 * (Saat = HBMs {@code hbm_m:strawberry}) liegt in {@code com.space_m.deco.SpaceDeco} (R9-B2).
 *
 * <p>Original-Verhalten: jeder Zufallstick +1 Stufe (ohne Licht-/Feuchteabfrage), Knochenmehl +2..5, Boden nur der
 * eine Original-Block (Ackerboden bzw. Gummi-Ackerboden), Atmung ueber {@link IPlantableBreathing}
 * (Erdluft/Sauerstoff bzw. Tekto-Luft/Chlor je &gt;= 0.1 atm). Hitbox 1 x 0.25 x 1, Kreuzmodell mit
 * Texturen {@code <name>_1.._5}. Beute als Loot-Tabellen (Stufe 7: 4 Stueck, sonst Stufe/2; Tee/Paraffin Zusatzsaat).
 */
public final class SpaceCrops {

    private SpaceCrops() {}

    private static final ResourceLocation CHLORINE = ResourceLocation.fromNamespaceAndPath("hbm_m", "chlorine");

    private static final Predicate<CBT_Atmosphere> EARTH_LIKE =
            a -> a.hasFluid(CelestialFluids.EARTHAIR, 0.1) || a.hasFluid(CelestialFluids.OXYGEN, 0.1);
    private static final Predicate<CBT_Atmosphere> TEKTO_LIKE =
            a -> a.hasFluid(CelestialFluids.TEKTOAIR, 0.1) || a.hasFluid(CHLORINE, 0.1);

    /** {@code rubber_farmland = BlockRubberFarm(Material.sand)}, Kies-Klang, 1.0/1.0 (blockTab). */
    public static final RegistrySupplier<BlockRubberFarm> RUBBER_FARMLAND = SpaceBlocks.registerWithItem("rubber_farmland",
            () -> new BlockRubberFarm(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.0F, 1.0F)
                    .sound(SoundType.GRAVEL).randomTicks()),
            SpaceCreativeTabs.Tab.BLOCKS);

    public static final RegistrySupplier<BlockSpaceCrop> CROP_MINT = crop("crop_mint", () -> net.minecraft.world.level.block.Blocks.FARMLAND, EARTH_LIKE,
            () -> SpaceMaterialItems.MINT_LEAVES.get());
    public static final RegistrySupplier<BlockSpaceCrop> CROP_COFFEE = crop("crop_coffee", () -> net.minecraft.world.level.block.Blocks.FARMLAND, EARTH_LIKE,
            () -> SpaceMaterialItems.BEAN_RAW.get());
    public static final RegistrySupplier<BlockSpaceCrop> CROP_TEA = crop("crop_tea", () -> net.minecraft.world.level.block.Blocks.FARMLAND, EARTH_LIKE,
            () -> SpaceMaterialItems.TEASEEDS.get());
    public static final RegistrySupplier<BlockSpaceCrop> CROP_PARAFFIN = crop("crop_paraffin", () -> RUBBER_FARMLAND.get(), TEKTO_LIKE,
            () -> SpaceMaterialItems.PARAFFIN_SEEDS.get());

    private static RegistrySupplier<BlockSpaceCrop> crop(String name, Supplier<Block> soil, Predicate<CBT_Atmosphere> breathe, Supplier<ItemLike> seed) {
        // Original: kein ItemBlock-Tab, Haerte 0, Gras-Klang, disableStats
        return SpaceBlocks.register(name, () -> new BlockSpaceCrop(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission()
                .randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY), soil, breathe, seed));
    }

    public static void init() {}

    // Original-Klasse: BlockCrop
    /** 1:1 {@code BlockCrop}. */
    public static class BlockSpaceCrop extends CropBlock implements IPlantableBreathing {

        private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);

        private final Supplier<Block> soil;
        private final Predicate<CBT_Atmosphere> breathe;
        private final Supplier<ItemLike> seed;

        public BlockSpaceCrop(Properties props, Supplier<Block> soil, Predicate<CBT_Atmosphere> breathe, Supplier<ItemLike> seed) {
            super(props);
            this.soil = soil;
            this.breathe = breathe;
            this.seed = seed;
        }

        @Override
        public boolean canBreathe(@Nullable CBT_Atmosphere atmosphere) {
            return atmosphere != null && breathe.test(atmosphere);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(soil.get());
        }

        /** Original {@code canBlockStay}: nur der Boden zaehlt (keine Lichtabfrage wie bei Vanilla-Feldfruechten). */
        @Override
        public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
            BlockPos below = pos.below();
            // R7-B: Original canBlockStay fragt soil.canSustainPlant -> Hydroponik traegt mint/coffee/tea (canHydro)
            if (com.space_m.machine.chem.HydroponicSoil.sustains(level, below)) return true;
            return mayPlaceOn(level.getBlockState(below), level, below);
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return seed.get();
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        /** Original {@code updateTick}: jede Zufallsaktualisierung eine Stufe weiter, bis 7. */
        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int age = getAge(state);
            if (age < getMaxAge()) level.setBlock(pos, getStateForAge(age + 1), 2);
        }

        //? if >= 1.21.1 {
        /*public static final com.mojang.serialization.MapCodec<BlockSpaceCrop> CODEC = simpleCodec(p -> new BlockSpaceCrop(p,
                () -> net.minecraft.world.level.block.Blocks.FARMLAND, a -> true, () -> net.minecraft.world.item.Items.WHEAT_SEEDS));
        @Override public com.mojang.serialization.MapCodec<? extends CropBlock> codec() { return CODEC; }
        *///?}
    }

    /**
     * 1:1 {@code BlockRubberFarm}: Gummi-Ackerboden. Feucht nur mit CCL ({@code ccl_block}) im Umkreis 4 (Hoehe 0..1) oder
     * Regen; trocknet nie zu Erde aus. Wird beim Darauffallen/festem Block darueber wieder {@code rubber_silt},
     * droppt {@code rubber_silt}. Feuchte im Original Metadatum 0/1 -> hier {@code MOISTURE} 0/7.
     */
    // Original-Klasse: BlockRubberFarm
    public static class BlockRubberFarm extends FarmBlock {

        public BlockRubberFarm(Properties props) {
            super(props);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean wet = nearCcl(level, pos) || level.isRainingAt(pos.above());
            level.setBlock(pos, state.setValue(MOISTURE, wet ? MAX_MOISTURE : 0), 2);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.canSurvive(level, pos)) toSilt(level, pos);
        }

        private static boolean nearCcl(LevelAccessor level, BlockPos pos) {
            Block ccl = BuiltInRegistries.BLOCK.get(SpaceMod.id("ccl_block"));
            if (ccl == null || ccl == net.minecraft.world.level.block.Blocks.AIR) return false;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
                if (level.getBlockState(p).is(ccl)) return true;
            }
            return false;
        }

        /** Original {@code onFallenUpon}: ab Fallhoehe zufaellig zurueck zu rubber_silt (Mob-Griefing beachten). */
        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            if (!level.isClientSide && level.random.nextFloat() < fallDistance - 0.5F) {
                if (entity instanceof Player || level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
                    toSilt(level, pos);
                }
            }
            entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
        }

        private static void toSilt(Level level, BlockPos pos) {
            level.setBlockAndUpdate(pos, TektoBlocks.RUBBER_SILT.get().defaultBlockState());
        }

        @Override
        public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
            return defaultBlockState();
        }

    }

    /** {@code ItemSeedFood(heal, sat, crop, soil)}: Saat, die man auch essen kann. */
    public static Item.Properties seedProps(int heal, float sat) {
        return new Item.Properties().food(com.hbm_m.platform.PlatformHooks.foodBuilder(heal, sat).build());
    }

    public static BlockItem seed(Block crop, int heal, float sat) {
        return new net.minecraft.world.item.ItemNameBlockItem(crop, seedProps(heal, sat));
    }
}
