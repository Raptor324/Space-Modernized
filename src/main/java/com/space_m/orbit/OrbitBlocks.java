package com.space_m.orbit;

import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R6-A: Stationsbloecke (Original {@code ModBlocks}):
 * <ul>
 *   <li>{@code orbital_station} "Orbital Station Core": Material.iron, {@code setBlockUnbreakable()},
 *       {@code setResistance(Float.POSITIVE_INFINITY)}, kein Creative-Tab (NEI versteckt), Textur block_steel.</li>
 *   <li>{@code orbital_station_port} "Orbital Station Port": Material.iron, {@code setHardness(1.0F)} (Resistenz 5 -> 1),
 *       machineTab.</li>
 * </ul>
 * Beide Bloecke teilen sich die BE {@code orbital_station} (Original {@code TileEntityOrbitalStation}).
 */
public final class OrbitBlocks {

    private OrbitBlocks() {}

    private static BlockBehaviour.Properties iron() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).noOcclusion();
    }

    public static final RegistrySupplier<BlockOrbitalStation> ORBITAL_STATION = SpaceBlocks.registerWithItem("orbital_station",
            () -> new BlockOrbitalStation(iron().strength(-1.0F, 3_600_000.0F).noLootTable()), null,
            b -> new OrbitalStationBlockItem(b, new Item.Properties()));

    public static final RegistrySupplier<BlockOrbitalStation> ORBITAL_STATION_PORT = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem(
            "orbital_station_port", () -> new BlockOrbitalStation(iron().strength(1.0F, 1.0F)), SpaceCreativeTabs.Tab.MACHINE,
            b -> new OrbitalStationBlockItem(b, new Item.Properties())));

    @SuppressWarnings("unchecked")
    public static final RegistrySupplier<BlockEntityType<TileEntityOrbitalStation>> ORBITAL_STATION_BE = SpaceBlockEntities.register(
            "orbital_station", TileEntityOrbitalStation::new, ORBITAL_STATION, ORBITAL_STATION_PORT);

    /** Aus {@code SpaceBlocks.init()} vor {@code BLOCKS.register()} (Klasse laden). */
    public static void init() { }
}
