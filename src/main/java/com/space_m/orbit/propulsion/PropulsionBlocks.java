package com.space_m.orbit.propulsion;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;
import com.space_m.main.SpaceCreativeTabs;
import com.space_m.orbit.OrbitalStationBlockItem;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R6-B: Bloecke des Stationsantriebs (Original {@code ModBlocks}, alle {@code Material.iron}, machineTab, Textur
 * {@code block_steel}):
 * <ul>
 *   <li>Triebwerke {@code machine_htr3}, {@code machine_htrf4}, {@code machine_htrf4neo}, {@code machine_xenon_thruster}:
 *       {@code setHardness(5.0F).setResistance(100.0F)} (-> 100*3/5 = 60).</li>
 *   <li>{@code orbital_station_launcher}, {@code orbital_station_computer}, {@code propulsion_creative}:
 *       {@code setHardness(1.0F)} (Widerstand 5*3/5 = 3).</li>
 * </ul>
 * Das LPW-2 ({@code machine_lpw2}) ist ein HBM-Block (Master-Stand: Deko) - Antrieb dort ist ein Wunsch an die API.
 */
public final class PropulsionBlocks {

    private PropulsionBlocks() {}

    private static BlockBehaviour.Properties thruster() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 60.0F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false);
    }

    private static BlockBehaviour.Properties soft() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.METAL);
    }

    public static final RegistrySupplier<MachineHTR3> MACHINE_HTR3 = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_htr3",
            () -> new MachineHTR3(thruster()), SpaceCreativeTabs.Tab.MACHINE, b -> new PropulsionBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineHTRF4> MACHINE_HTRF4 = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_htrf4",
            () -> new MachineHTRF4(thruster()), SpaceCreativeTabs.Tab.MACHINE, b -> new PropulsionBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineHTRFNeo> MACHINE_HTRF4NEO = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_htrf4neo",
            () -> new MachineHTRFNeo(thruster()), SpaceCreativeTabs.Tab.MACHINE, b -> new PropulsionBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<MachineXenonThruster> MACHINE_XENON_THRUSTER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("machine_xenon_thruster",
            () -> new MachineXenonThruster(thruster()), SpaceCreativeTabs.Tab.MACHINE, b -> new PropulsionBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<BlockOrbitalStationLauncher> ORBITAL_STATION_LAUNCHER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("orbital_station_launcher",
            () -> new BlockOrbitalStationLauncher(soft().noOcclusion()), SpaceCreativeTabs.Tab.MACHINE, b -> new OrbitalStationBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<BlockOrbitalStationComputer> ORBITAL_STATION_COMPUTER = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("orbital_station_computer",
            () -> new BlockOrbitalStationComputer(soft().noOcclusion()), SpaceCreativeTabs.Tab.MACHINE, b -> new PropulsionBlockItem(b, new Item.Properties())));

    public static final RegistrySupplier<BlockStationPropulsionCreative> PROPULSION_CREATIVE = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem("propulsion_creative",
            () -> new BlockStationPropulsionCreative(soft()), SpaceCreativeTabs.Tab.MACHINE, b -> new BlockItem(b, new Item.Properties())));

    /** Spitzhacken-Bloecke (Datagen {@code mineable/pickaxe}, Original Material.iron). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(MACHINE_HTR3, MACHINE_HTRF4, MACHINE_HTRF4NEO, MACHINE_XENON_THRUSTER, ORBITAL_STATION_LAUNCHER,
                ORBITAL_STATION_COMPUTER, PROPULSION_CREATIVE);
    }

    /** Aus {@code SpaceBlocks.init()} vor {@code BLOCKS.register()} (Klasse laden). */
    public static void init() { }
}
