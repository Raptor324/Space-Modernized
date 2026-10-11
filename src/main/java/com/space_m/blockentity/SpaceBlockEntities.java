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
        BLOCK_ENTITIES.register();
    }
}
