package com.space_m.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.space_m.SpaceMod;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Bloecke des Addons (Original: die 147 neuen Felder in {@code com.hbm.blocks.ModBlocks} von NTM Space).
 *
 * <p>Registry-ID = Original-{@code setBlockName("...")} (wie in HBM-Modernized), damit {@code space_gap.py}
 * und Lang-Werkzeug die Zuordnung finden. Eintraege nur mit kleinen Einfuegungen ergaenzen (mehrere Agenten).
 *
 * <p>Helfer:
 * <ul>
 *   <li>{@link #register(String, Supplier)} - nur Block, kein Item, keine automatischen Daten.</li>
 *   <li>{@link #registerWithItem(String, Supplier, SpaceCreativeTabs.Tab)} - Block + BlockItem, Tab des Originals.</li>
 *   <li>{@link #cube(String, Supplier, SpaceCreativeTabs.Tab)} - wie oben und Datagen erzeugt cube_all-Modell,
 *       Blockstate, Item-Modell und dropSelf-Loot (Textur {@code space_m:block/<id>} muss existieren).</li>
 * </ul>
 */
public final class SpaceBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.BLOCK);

    /** Datagen-Hinweise: Bloecke mit Standard-Modell (cube_all) und Standard-Loot (dropSelf). */
    private static final List<RegistrySupplier<Block>> SIMPLE_CUBES = new ArrayList<>();
    /** Datagen-Hinweise: Bloecke, die sich selbst droppen (ohne Modellvorgabe). */
    private static final List<RegistrySupplier<Block>> DROP_SELF = new ArrayList<>();

    // ── Eintraege (je Runde/Agent ein eigener, kommentierter Abschnitt) ─────────────────────────

    // R1-C Himmel: Planetarium (Original orrery: Material.iron, Haerte 10, setResistance(20) -> 20*3/5 = 12, machineTab)
    public static final RegistrySupplier<com.space_m.orrery.OrreryBlock> ORRERY = registerWithItem("orrery",
            () -> new com.space_m.orrery.OrreryBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(10.0F, 12.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()),
            SpaceCreativeTabs.Tab.MACHINE);

    // R2-C Atmo-Maschinen (Original ModBlocks: Material.iron, setHardness(10), setResistance(20) -> 20*3/5 = 12, machineTab;
    // atmosphere_editor setHardness(1) -> Widerstand 1*5*3/5 = 3, Textur atmosphere_editor)
    public static final RegistrySupplier<com.space_m.machine.atmo.AtmoTowerBlock> MACHINE_ATMO_TOWER = dropsSelf(registerWithItem("machine_atmo_tower",
            () -> new com.space_m.machine.atmo.AtmoTowerBlock(com.space_m.machine.atmo.AtmoMachines.machineProps()),
            SpaceCreativeTabs.Tab.MACHINE, b -> new com.hbm_m.multiblock.MultiblockBlockItem(b, new Item.Properties())));
    public static final RegistrySupplier<com.space_m.machine.atmo.AtmosphericCompressorBlock> MACHINE_ATMO_VENT = dropsSelf(registerWithItem("machine_atmo_vent",
            () -> new com.space_m.machine.atmo.AtmosphericCompressorBlock(com.space_m.machine.atmo.AtmoMachines.machineProps()),
            SpaceCreativeTabs.Tab.MACHINE, b -> new com.hbm_m.multiblock.MultiblockBlockItem(b, new Item.Properties())));
    public static final RegistrySupplier<com.space_m.machine.atmo.AtmosphericEmitterBlock> MACHINE_ATMO_EMITTER = dropsSelf(registerWithItem("machine_atmo_emitter",
            () -> new com.space_m.machine.atmo.AtmosphericEmitterBlock(com.space_m.machine.atmo.AtmoMachines.machineProps()),
            SpaceCreativeTabs.Tab.MACHINE, b -> new com.hbm_m.multiblock.MultiblockBlockItem(b, new Item.Properties())));
    public static final RegistrySupplier<com.space_m.machine.atmo.AtmosphereEditorBlock> ATMOSPHERE_EDITOR = cube("atmosphere_editor",
            () -> new com.space_m.machine.atmo.AtmosphereEditorBlock(com.space_m.machine.atmo.AtmoMachines.editorProps()),
            SpaceCreativeTabs.Tab.MACHINE);

    // R2-B Lebenserhaltung (Original ModBlocks: Material.iron, setHardness(5), setResistance(10) -> 10*3/5 = 6, machineTab)
    public static final RegistrySupplier<com.space_m.lifesupport.BlockAirPump> AIR_VENT = dropsSelf(registerWithItem("air_vent",
            () -> new com.space_m.lifesupport.BlockAirPump(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(5.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)),
            SpaceCreativeTabs.Tab.MACHINE));
    public static final RegistrySupplier<com.space_m.lifesupport.BlockAirScrubber> AIR_SCRUBBER = dropsSelf(registerWithItem("air_scrubber",
            () -> new com.space_m.lifesupport.BlockAirScrubber(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(5.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()),
            SpaceCreativeTabs.Tab.MACHINE));
    public static final RegistrySupplier<com.space_m.lifesupport.BlockAlgaeFilm> ALGAE_FILM = dropsSelf(registerWithItem("algae_film",
            () -> new com.space_m.lifesupport.BlockAlgaeFilm(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(5.0F, 6.0F)
                    .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()),
            SpaceCreativeTabs.Tab.MACHINE));

    private SpaceBlocks() {}

    public static <T extends Block> RegistrySupplier<T> register(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    public static <T extends Block> RegistrySupplier<T> registerWithItem(String name, Supplier<T> block, SpaceCreativeTabs.Tab tab) {
        return registerWithItem(name, block, tab, b -> new BlockItem(b, new Item.Properties()));
    }

    public static <T extends Block> RegistrySupplier<T> registerWithItem(String name, Supplier<T> block, SpaceCreativeTabs.Tab tab,
                                                                       Function<T, ? extends Item> item) {
        RegistrySupplier<T> reg = BLOCKS.register(name, block);
        RegistrySupplier<Item> it = SpaceItems.ITEMS.register(name, () -> item.apply(reg.get()));
        if (tab != null) SpaceCreativeTabs.add(tab, it);
        return reg;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Block> RegistrySupplier<T> cube(String name, Supplier<T> block, SpaceCreativeTabs.Tab tab) {
        RegistrySupplier<T> reg = registerWithItem(name, block, tab);
        SIMPLE_CUBES.add((RegistrySupplier<Block>) (RegistrySupplier<?>) reg);
        DROP_SELF.add((RegistrySupplier<Block>) (RegistrySupplier<?>) reg);
        return reg;
    }

    /** Markiert einen Block fuer dropSelf-Loot im Datagen. */
    @SuppressWarnings("unchecked")
    public static <T extends Block> RegistrySupplier<T> dropsSelf(RegistrySupplier<T> reg) {
        DROP_SELF.add((RegistrySupplier<Block>) (RegistrySupplier<?>) reg);
        return reg;
    }

    public static List<RegistrySupplier<Block>> simpleCubes() { return Collections.unmodifiableList(SIMPLE_CUBES); }

    public static List<RegistrySupplier<Block>> dropSelf() { return Collections.unmodifiableList(DROP_SELF); }

    public static void init() {
        BLOCKS.register();
    }
}
