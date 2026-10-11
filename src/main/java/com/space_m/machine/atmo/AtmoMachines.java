package com.space_m.machine.atmo;

import java.util.List;
import java.util.function.Supplier;

import com.space_m.block.SpaceBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Einstieg der Atmosphaeren-Maschinen (R2-C): Atmospheric Liquefaction Tower, Atmospheric Compressor
 * ({@code machine_atmo_vent}), Atmospheric Emitter, Atmosphere Editor, Atmosphaeren-Chemie ({@link AtmosphereRecipes})
 * und Fluid-Trait {@link FT_Terraformer}. Registrierung der Bloecke/BEs in {@code SpaceBlocks}/{@code SpaceBlockEntities}.
 */
public final class AtmoMachines {
    private AtmoMachines() {}

    /**
     * Original {@code Material.iron}, {@code setHardness(10.0F).setResistance(20.0F)} (-> 20*3/5 = 12), Trittgeraeusch
     * Stein (Block-Standard 1.7.10), Werkzeug noetig (Eisen-Material); Mehrblock: nicht verdeckend, nicht erstickend.
     */
    public static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10.0F, 12.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops().noOcclusion().isSuffocating((s, w, p) -> false);
    }

    /** Original {@code atmosphere_editor}: {@code Material.iron}, {@code setHardness(1.0F)} (Widerstand 5 -> 3). */
    public static BlockBehaviour.Properties editorProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F, 3.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    /** Spitzhacken-Bloecke (Datagen {@code mineable/pickaxe}). */
    public static List<Supplier<? extends Block>> pickaxeBlocks() {
        return List.of(SpaceBlocks.MACHINE_ATMO_TOWER, SpaceBlocks.MACHINE_ATMO_VENT, SpaceBlocks.MACHINE_ATMO_EMITTER, SpaceBlocks.ATMOSPHERE_EDITOR);
    }

    /** Gemeinsame Initialisierung (aus {@code SpaceRegistry.init}). */
    public static void init() {
        FT_Terraformer.register();
        AtmosphereRecipes.registerDefaults();
    }
}
