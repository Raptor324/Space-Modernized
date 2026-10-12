package com.space_m.machine.dyson;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.machine.misc.MachineBlockItem;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R7-A Dyson-System (Original {@code ModBlocks}: {@code dyson_launcher}, {@code dyson_receiver}, {@code dyson_converter_tu},
 * {@code dyson_converter_he}, {@code dyson_converter_anatmogenesis}; alle {@code Material.iron},
 * {@code setHardness(10.0F).setResistance(20.0F)} (-> 20*3/5 = 12), kein Trittgeraeusch gesetzt (Stein), machineTab,
 * Partikeltextur {@code block_steel_machine}).
 */
public final class DysonBlocks {

    private DysonBlocks() {}

    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10.0F, 12.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false);
    }

    public static final RegistrySupplier<MachineDysonLauncher> DYSON_LAUNCHER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("dyson_launcher",
            () -> new MachineDysonLauncher(props()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineDysonReceiver> DYSON_RECEIVER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("dyson_receiver",
            () -> new MachineDysonReceiver(props()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineDysonConverterTU> DYSON_CONVERTER_TU = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("dyson_converter_tu",
            () -> new MachineDysonConverterTU(props()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineDysonConverterHE> DYSON_CONVERTER_HE = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("dyson_converter_he",
            () -> new MachineDysonConverterHE(props()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineDysonConverterAnatmogenesis> DYSON_CONVERTER_ANATMOGENESIS = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("dyson_converter_anatmogenesis",
            () -> new MachineDysonConverterAnatmogenesis(props()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    /** Spitzhacken-Bloecke (Datagen {@code mineable/pickaxe}). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(DYSON_LAUNCHER, DYSON_RECEIVER, DYSON_CONVERTER_TU, DYSON_CONVERTER_HE, DYSON_CONVERTER_ANATMOGENESIS);
    }

    /** Aus {@code SpaceBlocks.init()} vor {@code BLOCKS.register()} (Klasse laden). */
    public static void init() { }
}
