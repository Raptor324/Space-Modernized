package com.space_m.machine.misc;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R7-A Sonstige Maschinen (Original {@code ModBlocks}):
 * <ul>
 *   <li>{@code machine_solar}, {@code machine_stardar}, {@code machine_radiator}: {@code Material.iron},
 *       {@code setHardness(5.0F).setResistance(10.0F)} (-> 10*3/5 = 6), kein Trittgeraeusch gesetzt (Stein), machineTab,
 *       Partikeltextur {@code block_steel_machine}.</li>
 *   <li>{@code furnace}/{@code lit_furnace} ({@code BlockFurnaceSpace}): Haerte 3.5, {@code soundTypePiston} (Stein), ohne
 *       Tab (NEI versteckt), {@code lit_furnace} Licht 0.875 (-> 13); Drop = Vanilla-Ofen.</li>
 *   <li>{@code war_controller} ({@code BlockMachineBase}): {@code soundTypeMetal}, Haerte 10, Widerstand 10 (-> 6), ohne Tab,
 *       Textur {@code block_steel}.</li>
 *   <li>{@code deco_stainless} ({@code BlockDecoCT}): {@code Material.iron}, Haerte 5, Widerstand 10 (-> 6), blockTab,
 *       Verbundtextur {@code deco_stainless}/{@code deco_stainless_ct}.</li>
 * </ul>
 */
public final class MiscBlocks {

    private MiscBlocks() {}

    /** Original {@code Material.iron}, {@code setHardness(5.0F).setResistance(10.0F)}, Mehrblock (nicht verdeckend). */
    public static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false);
    }

    private static BlockBehaviour.Properties furnace(int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops().lightLevel(s -> light);
    }

    public static final RegistrySupplier<MachineSolar> MACHINE_SOLAR = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_solar",
            () -> new MachineSolar(machine()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineStardar> MACHINE_STARDAR = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_stardar",
            () -> new MachineStardar(machine()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineRadiator> MACHINE_RADIATOR = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_radiator",
            () -> new MachineRadiator(machine()), SpaceCreativeTabs.Tab.MACHINE, b -> new MachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<BlockFurnaceSpace> FURNACE = SpaceBlocks.registerWithItem("furnace",
            () -> new BlockFurnaceSpace(false, furnace(0)), null);

    public static final RegistrySupplier<BlockFurnaceSpace> LIT_FURNACE = SpaceBlocks.registerWithItem("lit_furnace",
            () -> new BlockFurnaceSpace(true, furnace(13)), null);

    public static final RegistrySupplier<WarController> WAR_CONTROLLER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("war_controller",
            () -> new WarController(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10.0F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()), null, b -> new BlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<Block> DECO_STAINLESS = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("deco_stainless",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()), SpaceCreativeTabs.Tab.BLOCKS));

    /** Spitzhacken-Bloecke (Datagen {@code mineable/pickaxe}). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(MACHINE_SOLAR, MACHINE_STARDAR, MACHINE_RADIATOR, FURNACE, LIT_FURNACE, WAR_CONTROLLER, DECO_STAINLESS);
    }

    /** Aus {@code SpaceBlocks.init()} vor {@code BLOCKS.register()} (Klasse laden). */
    public static void init() { }
}
