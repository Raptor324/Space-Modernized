package com.space_m.deco;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.item.SpaceCrops;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * R9-B2: Deko-/Restbloecke des Space-Zweigs ({@code ModBlocks}/{@code ModItems}). Widerstand = 1.7.10-Wert * 0.6
 * (wie {@code PlanetOres.rock}; ohne {@code setResistance} gilt 1.7.10 Haerte * 5).
 *
 * <ul>
 *   <li>{@code astroturf} ({@link BlockAstroturf}, Material.grass 0.6, Gras-Klang, blockTab).</li>
 *   <li>{@code primed_log}, {@code eu_log}, {@code bf_log}, {@code lattice_log} ({@link BlockLogNT}, 0.5/2.5, Holz, blockTab).</li>
 *   <li>{@code flesh_block}, {@code charred_flesh_block}, {@code carbonized_flesh_block} ({@link MeltedFlesh}, blockTab).
 *       {@code flesh_fluid} ist im Original auskommentiert.</li>
 *   <li>{@code geysir_volcanic} ({@link BlockGeysirVolcanic}); {@code geysir_chlorine}/{@code geysir_nether} hat HBM-Modernized.</li>
 *   <li>{@code block_nickel}, {@code block_osmiridium} ({@code BlockBeaconable}, Material.iron 5/10, Metall, blockTab):
 *       Tag {@code minecraft:beacon_base_blocks}, OreDict {@code blockNickelPure}/{@code blockOsmiridium}.</li>
 *   <li>{@code fence_gate} ({@link BlockFenceGate}) + Gegenstand {@code fence_gate} ({@link ItemFenceGate}).</li>
 *   <li>{@code crop_strawberry} ({@code BlockCrop} = {@link SpaceCrops.BlockSpaceCrop}, Atmung Erdluft/O2 &gt;= 0.1,
 *       Hydroponik ja); Saat ist HBMs {@code hbm_m:strawberry} (Original {@code ItemSeedFood(3, 0.4F, crop_strawberry,
 *       farmland)}) - Pflanzen per Rechtsklick auf Ackerboden ueber das Interaktions-Event, Essen bleibt moeglich.</li>
 * </ul>
 *
 * <p>{@code statue_elb}, {@code statue_elb_g}, {@code statue_elb_w} sind im Original nur deklarierte Felder ohne
 * Zuweisung/Registrierung (nur {@code statue_elb_f} existiert, in HBM-Modernized portiert) - nichts zu portieren.
 *
 * <p>Zaehl-Hinweis (space_gap): {@code BlockStepEffect} ist im Space-Zweig nur eine Umsortierung der
 * HBM-Bloecke ({@code frozen_dirt}, {@code waste_planks}, {@code block_meteor_*}, {@code ore_gneiss_*} ...), die
 * HBM-Modernized bereits hat; kein Space-Block nutzt die Klasse.
 */
// Original-Klasse: BlockStepEffect
public final class SpaceDeco {

    private SpaceDeco() {}

    // ── Klang (Original sounds.json "block.fenceGate" -> block/fence_gate) ──
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.SOUND_EVENT);
    public static final RegistrySupplier<SoundEvent> BLOCK_FENCEGATE = SOUNDS.register("block.fencegate",
            () -> SoundEvent.createVariableRangeEvent(SpaceMod.id("block.fencegate")));

    // ── Staemme (Original-Registrierreihenfolge primed, eu, bf, lattice) ──
    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                .strength(0.5F, 1.5F).sound(SoundType.WOOD).ignitedByLava();
    }

    public static final RegistrySupplier<BlockLogNT> PRIMED_LOG = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("primed_log",
            () -> new BlockLogNT(wood()), SpaceCreativeTabs.Tab.BLOCKS));
    public static final RegistrySupplier<BlockLogNT> EU_LOG = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("eu_log",
            () -> new BlockLogNT(wood()), SpaceCreativeTabs.Tab.BLOCKS));
    public static final RegistrySupplier<BlockLogNT> BF_LOG = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("bf_log",
            () -> new BlockLogNT(wood()), SpaceCreativeTabs.Tab.BLOCKS));
    public static final RegistrySupplier<BlockLogNT> LATTICE_LOG = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("lattice_log",
            () -> new BlockLogNT(wood()), SpaceCreativeTabs.Tab.BLOCKS));

    // ── Kunstrasen (Material.grass, Haerte 0.6 -> Widerstand 3 * 0.6) ──
    public static final RegistrySupplier<BlockAstroturf> ASTROTURF = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("astroturf",
            () -> new BlockAstroturf(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.6F, 1.8F).sound(SoundType.GRASS)),
            SpaceCreativeTabs.Tab.BLOCKS));

    // ── Vulkan-Geysir (Material.rock, Licht 1.0, Haerte 2 -> Widerstand 10 * 0.6, kein Tab, keine Beute) ──
    public static final RegistrySupplier<BlockGeysirVolcanic> GEYSIR_VOLCANIC = SpaceBlocks.registerWithItem("geysir_volcanic",
            () -> new BlockGeysirVolcanic(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(2.0F, 6.0F).lightLevel(s -> 15).sound(SoundType.STONE).noLootTable()),
            null);

    // ── Metallbloecke (BlockBeaconable, Material.iron 5/10) ──
    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL);
    }

    public static final RegistrySupplier<Block> BLOCK_NICKEL = SpaceBlocks.cube("block_nickel", () -> new Block(metal()), SpaceCreativeTabs.Tab.BLOCKS);
    public static final RegistrySupplier<Block> BLOCK_OSMIRIDIUM = SpaceBlocks.cube("block_osmiridium", () -> new Block(metal()), SpaceCreativeTabs.Tab.BLOCKS);

    // ── Maschendraht-Tor (Material.iron, Haerte 5, setResistance(1) -> 0.6; Stein-Klang = Block-Standard) ──
    public static final RegistrySupplier<BlockFenceGate> FENCE_GATE = SpaceBlocks.register("fence_gate",
            () -> new BlockFenceGate(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(5.0F, 0.6F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY)));
    /** {@code ModItems.fence_gate = ItemModDoor} (blockTab, Stapel 1). */
    public static final RegistrySupplier<Item> FENCE_GATE_ITEM = SpaceItems.ITEMS.register("fence_gate",
            () -> new ItemFenceGate(FENCE_GATE.get(), new Item.Properties().stacksTo(1)));

    // ── Fleischschichten (Material.snow, Haerte 0.1 -> Widerstand 0.5 * 0.6, Kies-Klang) ──
    private static BlockBehaviour.Properties flesh() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).replaceable().forceSolidOff().noOcclusion().randomTicks()
                .strength(0.1F, 0.3F).sound(SoundType.GRAVEL).requiresCorrectToolForDrops().pushReaction(PushReaction.DESTROY);
    }

    public static final RegistrySupplier<MeltedFlesh> FLESH_BLOCK = SpaceBlocks.registerWithItem("flesh_block",
            () -> new MeltedFlesh(flesh()), SpaceCreativeTabs.Tab.BLOCKS);
    public static final RegistrySupplier<MeltedFlesh> CHARRED_FLESH_BLOCK = SpaceBlocks.registerWithItem("charred_flesh_block",
            () -> new MeltedFlesh(flesh()), SpaceCreativeTabs.Tab.BLOCKS);
    public static final RegistrySupplier<MeltedFlesh> CARBONIZED_FLESH_BLOCK = SpaceBlocks.registerWithItem("carbonized_flesh_block",
            () -> new MeltedFlesh(flesh()), SpaceCreativeTabs.Tab.BLOCKS);

    // ── Erdbeeren (BlockCrop wie SpaceCrops: kein ItemBlock-Tab, Haerte 0, Gras-Klang) ──
    private static final Predicate<CBT_Atmosphere> EARTH_LIKE =
            a -> a.hasFluid(CelestialFluids.EARTHAIR, 0.1) || a.hasFluid(CelestialFluids.OXYGEN, 0.1);

    public static final RegistrySupplier<SpaceCrops.BlockSpaceCrop> CROP_STRAWBERRY = SpaceBlocks.register("crop_strawberry",
            () -> new SpaceCrops.BlockSpaceCrop(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks()
                    .instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY),
                    () -> Blocks.FARMLAND, EARTH_LIKE, () -> com.hbm_m.item.ModItems.STRAWBERRY.get()));

    /** Aus {@code SpaceRegistry.init()} vor {@code SpaceBlocks.init()}: Klasse laden, Klang-Register, Tab, Pflanz-Event. */
    public static void init() {
        SOUNDS.register();
        SpaceCreativeTabs.add(SpaceCreativeTabs.Tab.BLOCKS, FENCE_GATE_ITEM);
        InteractionEvent.RIGHT_CLICK_BLOCK.register(SpaceDeco::plantStrawberry);
    }

    /**
     * Original {@code ItemSeedFood.onItemUse} fuer {@code hbm_m:strawberry}: nur Oberseite, Spieler darf beide Positionen
     * bearbeiten, Boden traegt die Pflanze (Ackerboden bzw. Hydroponik-Beet), darueber Luft -> {@code crop_strawberry},
     * ein Stueck weniger (Kreativ nicht). Sonst bleibt der Rechtsklick beim Essen.
     */
    private static EventResult plantStrawberry(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
                                               BlockPos pos, Direction face) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getItem() != com.hbm_m.item.ModItems.STRAWBERRY.get()) return EventResult.pass();
        if (face != Direction.UP) return EventResult.pass();
        Level level = player.level();
        BlockPos up = pos.above();
        if (!player.mayUseItemAt(pos, face, stack) || !player.mayUseItemAt(up, face, stack)) return EventResult.pass();
        if (!canPlantStrawberry(level, pos)) return EventResult.pass();

        if (!level.isClientSide) {
            Block crop = CROP_STRAWBERRY.get();
            level.setBlock(up, crop.defaultBlockState(), 3);
            SoundType st = SoundType.GRASS;
            level.playSound(null, up, st.getPlaceSound(), SoundSource.BLOCKS, (st.getVolume() + 1.0F) / 2.0F, st.getPitch() * 0.8F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        player.swing(hand);
        return EventResult.interruptTrue();
    }

    /** Boden traegt Erdbeeren ({@code canSustainPlant}: Ackerboden oder Hydroponik) und darueber ist Luft. */
    public static boolean canPlantStrawberry(Level level, BlockPos soil) {
        boolean ok = level.getBlockState(soil).is(Blocks.FARMLAND) || com.space_m.machine.chem.HydroponicSoil.sustains(level, soil);
        return ok && level.isEmptyBlock(soil.above());
    }

    // ── Datagen ──

    private static TagKey<Block> forgeBlock(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", path));
    }

    private static TagKey<Item> forgeItem(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }

    private static TagKey<Item> oredict(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("hbm_m", "oredict/" + path));
    }

    /** Block-Tags (Aufruf aus {@code SpaceTagProviders.Blocks}). */
    public static void addBlockTags(BiConsumer<TagKey<Block>, Block> out) {
        out.accept(BlockTags.MINEABLE_WITH_SHOVEL, ASTROTURF.get());                  // Material.grass
        for (RegistrySupplier<BlockLogNT> l : java.util.List.of(PRIMED_LOG, EU_LOG, BF_LOG, LATTICE_LOG)) {
            out.accept(BlockTags.MINEABLE_WITH_AXE, l.get());                          // Material.wood
            out.accept(BlockTags.LOGS, l.get());                                       // BlockLog: canSustainLeaves/isWood
        }
        out.accept(BlockTags.MINEABLE_WITH_PICKAXE, GEYSIR_VOLCANIC.get());            // Material.rock
        out.accept(BlockTags.MINEABLE_WITH_PICKAXE, BLOCK_NICKEL.get());               // Material.iron
        out.accept(BlockTags.MINEABLE_WITH_PICKAXE, BLOCK_OSMIRIDIUM.get());
        out.accept(BlockTags.MINEABLE_WITH_PICKAXE, FENCE_GATE.get());
        out.accept(BlockTags.BEACON_BASE_BLOCKS, BLOCK_NICKEL.get());                  // BlockBeaconable
        out.accept(BlockTags.BEACON_BASE_BLOCKS, BLOCK_OSMIRIDIUM.get());
        out.accept(forgeBlock("storage_blocks/nickel_pure"), BLOCK_NICKEL.get());
        out.accept(forgeBlock("storage_blocks/nickel"), BLOCK_NICKEL.get());
        out.accept(forgeBlock("storage_blocks/osmiridium"), BLOCK_OSMIRIDIUM.get());
        out.accept(BlockTags.CROPS, CROP_STRAWBERRY.get());
        out.accept(BlockTags.MAINTAINS_FARMLAND, CROP_STRAWBERRY.get());
    }

    /** Item-Tags (Aufruf aus {@code SpaceTagProviders.Items}): OreDict {@code NI.block}/{@code OSMIRIDIUM.block}. */
    public static void addItemTags(BiConsumer<TagKey<Item>, Item> out) {
        out.accept(oredict("block/nickel_pure"), BLOCK_NICKEL.get().asItem());
        out.accept(forgeItem("storage_blocks/nickel_pure"), BLOCK_NICKEL.get().asItem());
        out.accept(forgeItem("storage_blocks/nickel"), BLOCK_NICKEL.get().asItem());
        out.accept(oredict("block/osmiridium"), BLOCK_OSMIRIDIUM.get().asItem());
        out.accept(forgeItem("storage_blocks/osmiridium"), BLOCK_OSMIRIDIUM.get().asItem());
    }
}
