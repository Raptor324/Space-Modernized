package com.space_m.machine.misc;

import com.space_m.blockentity.SpaceBlockEntities;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * R7-A Blockentities (Original {@code TileMappings}: {@code tileentity_solarpanel}, {@code tileentity_stardar},
 * {@code tileentity_radiator}, {@code tileentity_furnace_space}, {@code tileentity_war}; IDs ohne "tileentity_").
 */
public final class MiscBlockEntities {

    private MiscBlockEntities() {}

    public static final RegistrySupplier<BlockEntityType<TileEntityMachineSolarPanel>> SOLAR_PANEL =
            SpaceBlockEntities.register("solarpanel", TileEntityMachineSolarPanel::new, MiscBlocks.MACHINE_SOLAR);
    public static final RegistrySupplier<BlockEntityType<TileEntityMachineStardar>> STARDAR =
            SpaceBlockEntities.register("stardar", TileEntityMachineStardar::new, MiscBlocks.MACHINE_STARDAR);
    public static final RegistrySupplier<BlockEntityType<TileEntityRadiator>> RADIATOR =
            SpaceBlockEntities.register("radiator", TileEntityRadiator::new, MiscBlocks.MACHINE_RADIATOR);
    public static final RegistrySupplier<BlockEntityType<TileEntityFurnaceSpace>> FURNACE_SPACE =
            SpaceBlockEntities.register("furnace_space", TileEntityFurnaceSpace::new, MiscBlocks.FURNACE, MiscBlocks.LIT_FURNACE);
    public static final RegistrySupplier<BlockEntityType<TileEntityMachineWarController>> WAR =
            SpaceBlockEntities.register("war", TileEntityMachineWarController::new, MiscBlocks.WAR_CONTROLLER);

    /** Aus {@code SpaceBlockEntities.init()} vor {@code BLOCK_ENTITIES.register()} (Klasse laden). */
    public static void init() { }
}
