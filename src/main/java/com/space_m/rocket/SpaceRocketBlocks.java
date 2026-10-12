package com.space_m.rocket;

import com.space_m.block.SpaceBlocks;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * R5-A: Bloecke der Raketenmontage (Original {@code ModBlocks.machine_rocket_assembly}: Material.iron, setHardness(5),
 * setResistance(10) -> 10*3/5 = 6, missileTab).
 */
public final class SpaceRocketBlocks {

    private SpaceRocketBlocks() {}

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().noOcclusion();
    }

    public static final RegistrySupplier<MachineRocketAssemblyBlock> MACHINE_ROCKET_ASSEMBLY = SpaceBlocks.dropsSelf(SpaceBlocks.registerWithItem(
            "machine_rocket_assembly", () -> new MachineRocketAssemblyBlock(props()), SpaceCreativeTabs.Tab.MISSILE,
            b -> new RocketAssemblyBlockItem(b, new Item.Properties())));

    /** Technisch noetig (s. {@link RocketAssemblyPartBlock}): Buehnen und Saeulen der VAB, ohne Item. */
    public static final RegistrySupplier<RocketAssemblyPartBlock> ROCKET_ASSEMBLY_PART = SpaceBlocks.register(
            "machine_rocket_assembly_part", () -> new RocketAssemblyPartBlock(props().noLootTable()));

    /** Aus {@code SpaceBlocks.init()} vor {@code BLOCKS.register()}. */
    public static void init() { }
}
