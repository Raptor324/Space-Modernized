package com.space_m.blockentity;

import java.util.function.Supplier;

import com.space_m.SpaceMod;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block-Entities des Addons (Original: 37 neue TileEntities von NTM Space, Registrierung in
 * {@code TileMappings}). ID = Original-Mapping-Name ohne Praefix {@code tileentity_}, kleingeschrieben.
 *
 * <p>Muster: BEs erben von {@code com.hbm_m.blockentity.BaseHbmBlockEntity} und ueberschreiben nur
 * {@code writeNbtData/readNbtData} (siehe VERSIONPORT.md, Abschnitt 4).
 */
public final class SpaceBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(SpaceMod.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    // ── Eintraege (je Runde/Agent ein eigener, kommentierter Abschnitt) ─────────────────────────

    // R1-C Himmel: Planetarium (Original tileentity_orrery)
    public static final RegistrySupplier<BlockEntityType<com.space_m.orrery.OrreryBlockEntity>> ORRERY =
            register("orrery", com.space_m.orrery.OrreryBlockEntity::new, com.space_m.block.SpaceBlocks.ORRERY);

    // R2-C Atmo-Maschinen (Original tileentity_atmospheric_tower/_atmospheric_vent/_atmo_emitter/_atmosphere_editor)
    public static final RegistrySupplier<BlockEntityType<com.space_m.machine.atmo.AtmoTowerBlockEntity>> ATMOSPHERIC_TOWER =
            register("atmospheric_tower", com.space_m.machine.atmo.AtmoTowerBlockEntity::new, com.space_m.block.SpaceBlocks.MACHINE_ATMO_TOWER);
    public static final RegistrySupplier<BlockEntityType<com.space_m.machine.atmo.AtmosphericCompressorBlockEntity>> ATMOSPHERIC_VENT =
            register("atmospheric_vent", com.space_m.machine.atmo.AtmosphericCompressorBlockEntity::new, com.space_m.block.SpaceBlocks.MACHINE_ATMO_VENT);
    public static final RegistrySupplier<BlockEntityType<com.space_m.machine.atmo.AtmosphericEmitterBlockEntity>> ATMO_EMITTER =
            register("atmo_emitter", com.space_m.machine.atmo.AtmosphericEmitterBlockEntity::new, com.space_m.block.SpaceBlocks.MACHINE_ATMO_EMITTER);
    public static final RegistrySupplier<BlockEntityType<com.space_m.machine.atmo.AtmosphereEditorBlockEntity>> ATMOSPHERE_EDITOR =
            register("atmosphere_editor", com.space_m.machine.atmo.AtmosphereEditorBlockEntity::new, com.space_m.block.SpaceBlocks.ATMOSPHERE_EDITOR);

    // R2-B Lebenserhaltung (Original tileentity_air_vent/_air_scrubber/_algae_film)
    public static final RegistrySupplier<BlockEntityType<com.space_m.lifesupport.TileEntityAirPump>> AIR_VENT =
            register("air_vent", com.space_m.lifesupport.TileEntityAirPump::new, com.space_m.block.SpaceBlocks.AIR_VENT);
    public static final RegistrySupplier<BlockEntityType<com.space_m.lifesupport.TileEntityAirScrubber>> AIR_SCRUBBER =
            register("air_scrubber", com.space_m.lifesupport.TileEntityAirScrubber::new, com.space_m.block.SpaceBlocks.AIR_SCRUBBER);
    public static final RegistrySupplier<BlockEntityType<com.space_m.lifesupport.TileEntityAlgaeFilm>> ALGAE_FILM =
            register("algae_film", com.space_m.lifesupport.TileEntityAlgaeFilm::new, com.space_m.block.SpaceBlocks.ALGAE_FILM);

    // R5-C Transporter/Drop-Pods (Original tileentity_transporter / tileentity_combat_pod)
    public static final RegistrySupplier<BlockEntityType<com.space_m.rocket.transport.TransporterRocketBlockEntity>> TRANSPORTER_ROCKET =
            register("transporter_rocket", com.space_m.rocket.transport.TransporterRocketBlockEntity::new, com.space_m.block.SpaceBlocks.TRANSPORTER_ROCKET);
    public static final RegistrySupplier<BlockEntityType<com.space_m.rocket.transport.CombatDropPodBlockEntity>> COMBAT_DROP_POD =
            register("combat_drop_pod", com.space_m.rocket.transport.CombatDropPodBlockEntity::new, com.space_m.block.SpaceBlocks.COMBAT_DROP);

    // R5-A Raketenmontage (Original tileentity_rocket_assembly)
    public static final RegistrySupplier<BlockEntityType<com.space_m.rocket.MachineRocketAssemblyBlockEntity>> ROCKET_ASSEMBLY =
            register("rocket_assembly", com.space_m.rocket.MachineRocketAssemblyBlockEntity::new, com.space_m.rocket.SpaceRocketBlocks.MACHINE_ROCKET_ASSEMBLY);

    // R5-B Raketen-Startrampe (Original tileentity_launchpad_rocket) + Turmzellen (Kernposition)
    public static final RegistrySupplier<BlockEntityType<com.space_m.rocket.pad.TileEntityLaunchPadRocket>> LAUNCH_PAD_ROCKET =
            register("launchpad_rocket", com.space_m.rocket.pad.TileEntityLaunchPadRocket::new, com.space_m.block.SpaceBlocks.LAUNCH_PAD_ROCKET);
    public static final RegistrySupplier<BlockEntityType<com.space_m.rocket.pad.LaunchPadRocketTowerBlock.TowerBlockEntity>> LAUNCH_PAD_ROCKET_TOWER =
            register("launch_pad_rocket_tower", com.space_m.rocket.pad.LaunchPadRocketTowerBlock.TowerBlockEntity::new, com.space_m.block.SpaceBlocks.LAUNCH_PAD_ROCKET_TOWER);

    private SpaceBlockEntities() {}

    /** Kurzform fuer {@code BlockEntityType.Builder.of(factory, blocks).build(null)} (beide Versionen identisch). */
    @SafeVarargs
    public static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> register(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block>... blocks) {
        return BLOCK_ENTITIES.register(name, () -> {
            Block[] valid = new Block[blocks.length];
            for (int i = 0; i < blocks.length; i++) valid[i] = blocks[i].get();
            return BlockEntityType.Builder.of(factory, valid).build(null);
        });
    }

    public static void init() {
        com.space_m.orbit.propulsion.PropulsionBlockEntities.init(); // R6-B (Klasse laden)
        com.space_m.machine.dyson.DysonBlockEntities.init(); // R7-A (Klasse laden)
        com.space_m.machine.misc.MiscBlockEntities.init(); // R7-A (Klasse laden)
        BLOCK_ENTITIES.register();
    }
}
