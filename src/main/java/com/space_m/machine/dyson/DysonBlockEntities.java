package com.space_m.machine.dyson;

import com.space_m.blockentity.SpaceBlockEntities;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * R7-A Dyson-System (Original {@code TileMappings}: {@code tileentity_dyson_launcher}, {@code tileentity_dyson_receiver},
 * {@code tileentity_dyson_converter_tu}, {@code tileentity_dyson_converter_he},
 * {@code tileentity_dyson_converter_anatmogenesis}; IDs ohne "tileentity_").
 */
public final class DysonBlockEntities {

    private DysonBlockEntities() {}

    public static final RegistrySupplier<BlockEntityType<TileEntityDysonLauncher>> LAUNCHER =
            SpaceBlockEntities.register("dyson_launcher", TileEntityDysonLauncher::new, DysonBlocks.DYSON_LAUNCHER);
    public static final RegistrySupplier<BlockEntityType<TileEntityDysonReceiver>> RECEIVER =
            SpaceBlockEntities.register("dyson_receiver", TileEntityDysonReceiver::new, DysonBlocks.DYSON_RECEIVER);
    public static final RegistrySupplier<BlockEntityType<TileEntityDysonConverterTU>> CONVERTER_TU =
            SpaceBlockEntities.register("dyson_converter_tu", TileEntityDysonConverterTU::new, DysonBlocks.DYSON_CONVERTER_TU);
    public static final RegistrySupplier<BlockEntityType<TileEntityDysonConverterHE>> CONVERTER_HE =
            SpaceBlockEntities.register("dyson_converter_he", TileEntityDysonConverterHE::new, DysonBlocks.DYSON_CONVERTER_HE);
    public static final RegistrySupplier<BlockEntityType<TileEntityDysonConverterAnatmogenesis>> CONVERTER_ANATMOGENESIS =
            SpaceBlockEntities.register("dyson_converter_anatmogenesis", TileEntityDysonConverterAnatmogenesis::new, DysonBlocks.DYSON_CONVERTER_ANATMOGENESIS);

    /** Aus {@code SpaceBlockEntities.init()} vor {@code BLOCK_ENTITIES.register()} (Klasse laden). */
    public static void init() { }
}
