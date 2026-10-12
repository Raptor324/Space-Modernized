package com.space_m.orbit.propulsion;

import com.space_m.blockentity.SpaceBlockEntities;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * R6-B: Blockentities des Stationsantriebs (Original {@code TileMappings}: {@code tileentity_machine_htr3},
 * {@code tileentity_machine_htrf4}, {@code tileentity_machine_htrneo}, {@code tileentity_xenon_thruster},
 * {@code tileentity_orbital_station_launcher}, {@code tileentity_orbital_station_computer},
 * {@code tileentity_propulsion_creative}; IDs ohne "tileentity_").
 */
public final class PropulsionBlockEntities {

    private PropulsionBlockEntities() {}

    public static final RegistrySupplier<BlockEntityType<TileEntityMachineHTR3>> HTR3 =
            SpaceBlockEntities.register("machine_htr3", TileEntityMachineHTR3::new, PropulsionBlocks.MACHINE_HTR3);
    public static final RegistrySupplier<BlockEntityType<TileEntityMachineHTRF4>> HTRF4 =
            SpaceBlockEntities.register("machine_htrf4", TileEntityMachineHTRF4::new, PropulsionBlocks.MACHINE_HTRF4);
    public static final RegistrySupplier<BlockEntityType<TileEntityMachineHTRNeo>> HTRF4_NEO =
            SpaceBlockEntities.register("machine_htrneo", TileEntityMachineHTRNeo::new, PropulsionBlocks.MACHINE_HTRF4NEO);
    public static final RegistrySupplier<BlockEntityType<TileEntityXenonThruster>> XENON_THRUSTER =
            SpaceBlockEntities.register("xenon_thruster", TileEntityXenonThruster::new, PropulsionBlocks.MACHINE_XENON_THRUSTER);
    public static final RegistrySupplier<BlockEntityType<TileEntityOrbitalStationLauncher>> ORBITAL_STATION_LAUNCHER =
            SpaceBlockEntities.register("orbital_station_launcher", TileEntityOrbitalStationLauncher::new, PropulsionBlocks.ORBITAL_STATION_LAUNCHER);
    public static final RegistrySupplier<BlockEntityType<TileEntityOrbitalStationComputer>> ORBITAL_STATION_COMPUTER =
            SpaceBlockEntities.register("orbital_station_computer", TileEntityOrbitalStationComputer::new, PropulsionBlocks.ORBITAL_STATION_COMPUTER);
    public static final RegistrySupplier<BlockEntityType<TileEntityStationPropulsionCreative>> PROPULSION_CREATIVE =
            SpaceBlockEntities.register("propulsion_creative", TileEntityStationPropulsionCreative::new, PropulsionBlocks.PROPULSION_CREATIVE);

    /** Aus {@code SpaceBlockEntities.init()} vor {@code BLOCK_ENTITIES.register()} (Klasse laden). */
    public static void init() { }
}
