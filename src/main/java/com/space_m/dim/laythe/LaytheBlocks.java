package com.space_m.dim.laythe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.dim.PlanetOres;
import com.space_m.dim.laythe.block.BlockCoral;
import com.space_m.dim.laythe.block.BlockFallingTint;
import com.space_m.dim.laythe.block.BlockKelp;
import com.space_m.dim.laythe.block.BlockTallPlantWater;
import com.space_m.dim.laythe.block.BlockWaterPlant;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Bloecke/Items von Laythe (Original {@code ModBlocks}/{@code ModItems}, Werte 1:1; Widerstand 1.7.10 -> 1.20 wie
 * {@link PlanetOres#rock}: {@code setResistance(r)} -> {@code r * 0.6}, nur {@code setHardness(h)} -> {@code h}).
 * Planetengestein ist {@code stone} ({@code WorldProviderCelestial.getStone}), Erz-Varianten {@code *_laythe} mit
 * Vanilla-Stein-Textur ({@code withBlockTextures("textures/blocks/stone.png", ...)}).
 */
public final class LaytheBlocks {

    private LaytheBlocks() {}

    public static final String BODY = "laythe";

    private static final List<Supplier<? extends Block>> SHOVEL = new ArrayList<>();
    private static final List<Supplier<? extends Block>> AXE = new ArrayList<>();

    /** {@code laythe_silt = BlockFallingTint(Material.sand)}, Haerte 0.5, Sand-Klang. */
    public static final RegistrySupplier<Block> LAYTHE_SILT = shovel(SpaceBlocks.registerWithItem("laythe_silt",
            () -> new BlockFallingTint(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.SAND)),
            SpaceCreativeTabs.Tab.BLOCKS));

    /** {@code laythe_root = BlockDecoCT(Material.wood).noFortune()}, Haerte 2.0 (Verbundtextur -> Grundtextur). */
    public static final RegistrySupplier<Block> LAYTHE_ROOT = axe(SpaceBlocks.registerWithItem("laythe_root",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD)),
            SpaceCreativeTabs.Tab.BLOCKS));

    private static BlockBehaviour.Properties waterPlant() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WATER).noCollission().instabreak().sound(SoundType.GRASS)
                .noOcclusion().pushReaction(PushReaction.DESTROY).replaceable();
    }

    /** {@code laythe_short = BlockWaterPlant} ("laythe_seagrass"). */
    public static final RegistrySupplier<Block> LAYTHE_SHORT = SpaceBlocks.registerWithItem("laythe_seagrass",
            () -> new BlockWaterPlant(waterPlant()), SpaceCreativeTabs.Tab.BLOCKS);
    /** {@code laythe_glow = BlockWaterPlant}, {@code setLightLevel(1.0F)} = 15 ("laythe_glowgrass"). */
    public static final RegistrySupplier<Block> LAYTHE_GLOW = SpaceBlocks.registerWithItem("laythe_glowgrass",
            () -> new BlockWaterPlant(waterPlant().lightLevel(s -> 15)), SpaceCreativeTabs.Tab.BLOCKS);
    /** {@code plant_tall_laythe = BlockTallPlantWater}. */
    public static final RegistrySupplier<Block> PLANT_TALL_LAYTHE = SpaceBlocks.registerWithItem("plant_tall_laythe",
            () -> new BlockTallPlantWater(waterPlant()), SpaceCreativeTabs.Tab.BLOCKS);
    /** {@code laythe_kelp = BlockKelp}. */
    public static final RegistrySupplier<Block> LAYTHE_KELP = SpaceBlocks.registerWithItem("laythe_kelp",
            () -> new BlockKelp(waterPlant()), SpaceCreativeTabs.Tab.BLOCKS);

    /** {@code BlockCoral.EnumCoral} in Original-Reihenfolge. */
    public static final String[] CORALS = {"tube", "brain", "bubble", "fire", "horn"};

    /** {@code laythe_coral = BlockCoral}, Haerte 0, Gras-Klang - je Sorte ein Block. */
    public static final Map<String, RegistrySupplier<Block>> LAYTHE_CORAL = new LinkedHashMap<>();
    /** {@code laythe_coral_block = BlockEnumMulti(Material.coral)}, Haerte 0.5, Gras-Klang - je Sorte ein Block. */
    public static final Map<String, RegistrySupplier<Block>> LAYTHE_CORAL_BLOCK = new LinkedHashMap<>();

    static {
        for (String c : CORALS) {
            LAYTHE_CORAL.put(c, SpaceBlocks.registerWithItem("laythe_coral_" + c,
                    () -> new BlockCoral(waterPlant()), SpaceCreativeTabs.Tab.BLOCKS));
            LAYTHE_CORAL_BLOCK.put(c, SpaceBlocks.registerWithItem("laythe_coral_block_" + c,
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5F).sound(SoundType.GRASS)),
                    SpaceCreativeTabs.Tab.BLOCKS));
        }
    }

    // ── Erze (WorldGeneratorCelestial.generateNTMOres mit Metadatum LAYTHE; stone -> keine generateVanillaOres) ──
    public static final CelestialOreSet ORES = CelestialOreSet.create(BODY, false, true);
    /** WorldGeneratorLaythe: {@code ore_asbestos = BlockOreOutgas(rock, true, 5, true)}, 5.0/15.0. */
    public static final RegistrySupplier<Block> ORE_ASBESTOS = PlanetOres.ore("ore_asbestos", BODY,
            p -> new com.hbm_m.block.gas.OutgasBlock(p, () -> com.hbm_m.block.ModBlocks.GAS_ASBESTOS.get(), true, true, false, false, true), 5.0F, 15.0F);
    /** ChunkProviderLaythe: Oelblasen {@code ore_oil} (BlockOreFluid OIL, Laythe = entschwefeltes Oel), 5.0/10.0. */
    public static final RegistrySupplier<Block> ORE_OIL = PlanetOres.ore("ore_oil", BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_OIL_EMPTY = PlanetOres.ore("ore_oil_empty", BODY, 5.0F, 10.0F);

    // ── Items ──
    /** {@code saltleaf = ItemLemon(2, 0.5F, false)} (partsTab). */
    public static final RegistrySupplier<Item> SALTLEAF = SpaceItems.basic("saltleaf",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2)
                    //? if < 1.21.1 {
                    .saturationMod(0.5F)
                    //?} else {
                    /*.saturationModifier(0.5F)
                    *///?}
                    .build())), SpaceCreativeTabs.Tab.PARTS);

    private static <T extends Block> RegistrySupplier<T> shovel(RegistrySupplier<T> b) {
        SHOVEL.add(b);
        return b;
    }

    private static <T extends Block> RegistrySupplier<T> axe(RegistrySupplier<T> b) {
        AXE.add(b);
        return b;
    }

    /** Werkzeug-Tags (Datagen {@code SpaceTagProviders}). */
    public static void addTags(BiConsumer<TagKey<Block>, Block> tag) {
        for (Supplier<? extends Block> b : SHOVEL) tag.accept(BlockTags.MINEABLE_WITH_SHOVEL, b.get());
        for (Supplier<? extends Block> b : AXE) tag.accept(BlockTags.MINEABLE_WITH_AXE, b.get());
    }

    public static void init() {
    }
}
