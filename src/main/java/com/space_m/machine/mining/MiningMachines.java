package com.space_m.machine.mining;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.inventory.SpaceMenus;
import com.space_m.machine.chem.SpaceMachineBlockItem;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R7-B Bergbau (Magma-Bohrer, Gas-Dock, Drive-Processor): Bloecke, Blockentities, Menues. Aus {@code SpaceBlocks.init()}
 * geladen ({@link #init()}), gemeinsame Initialisierung ueber {@link #commonInit()} aus {@code SpaceRegistry}.
 *
 * <p>Original {@code ModBlocks} (alle {@code Material.iron}, Haerte 5):
 * <ul>
 *   <li>{@code machine_magma}: Widerstand 100 (-> 60), machineTab, Textur {@code block_steel}.</li>
 *   <li>{@code gas_dock}: Widerstand 10 (-> 6), missileTab, Textur {@code sat_dock}.</li>
 *   <li>{@code machine_drive_processor}: Widerstand 10 (-> 6), machineTab, Textur {@code block_steel_machine}.</li>
 * </ul>
 * {@code TileMappings}: {@code tileentity_magma}, {@code tileentity_gas_dock}, {@code tileentity_drive_processor}.
 */
public final class MiningMachines {
    private MiningMachines() {}

    private static BlockBehaviour.Properties iron(float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, resistance).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false);
    }

    // ── Bloecke ──────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<MachineMagma> MACHINE_MAGMA = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_magma",
            () -> new MachineMagma(iron(60.0F)), SpaceCreativeTabs.Tab.MACHINE, b -> new SpaceMachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineGasDock> GAS_DOCK = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("gas_dock",
            () -> new MachineGasDock(iron(6.0F)), SpaceCreativeTabs.Tab.MISSILE, b -> new SpaceMachineBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineDriveProcessor> MACHINE_DRIVE_PROCESSOR = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_drive_processor",
            () -> new MachineDriveProcessor(iron(6.0F)), SpaceCreativeTabs.Tab.MACHINE, b -> new SpaceMachineBlockItem(b, new Item.Properties())));

    // ── Blockentities ────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<BlockEntityType<TileEntityMachineMagma>> MAGMA_BE =
            SpaceBlockEntities.register("magma", TileEntityMachineMagma::new, MACHINE_MAGMA);

    public static final RegistrySupplier<BlockEntityType<TileEntityMachineGasDock>> GAS_DOCK_BE =
            SpaceBlockEntities.register("gas_dock", TileEntityMachineGasDock::new, GAS_DOCK);

    public static final RegistrySupplier<BlockEntityType<TileEntityMachineDriveProcessor>> DRIVE_PROCESSOR_BE =
            SpaceBlockEntities.register("drive_processor", TileEntityMachineDriveProcessor::new, MACHINE_DRIVE_PROCESSOR);

    // ── Menues ───────────────────────────────────────────────────────────────────────────────

    public static final RegistrySupplier<MenuType<ContainerDriveProcessor>> DRIVE_PROCESSOR_MENU =
            SpaceMenus.extended("machine_drive_processor", ContainerDriveProcessor::new);

    /** Spitzhacken-Bloecke (Datagen {@code mineable/pickaxe}, Original Material.iron). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(MACHINE_MAGMA, GAS_DOCK, MACHINE_DRIVE_PROCESSOR);
    }

    /** Aus {@code SpaceBlocks.init()} vor {@code BLOCKS.register()} (Klasse laden). */
    public static void init() { }

    /** Aus {@code SpaceRegistry.init()} (Events o.ae.). */
    public static void commonInit() { }
}
