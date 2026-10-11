package com.space_m.dim.thatmo;

import com.space_m.dim.PlanetOres;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;

/**
 * Erze auf Thatmo. Original: {@code CelestialBody.getMeta(world)} faellt auf Thatmo (Koerper ohne Dimension,
 * kein {@code Body}-Eintrag) auf Kerbin zurueck - {@code WorldGeneratorCelestial} setzt also die Kerbin-Varianten
 * (Stein-Textur) in das geloeschte Sellafit. Das sind die Erze von HBM-Modernized bzw. Vanilla; nur Nickel und
 * Zink haben dort keine Kerbin-Form (Original {@code addAllExcept(ore_nickel, KERBIN)}) und kommen als
 * {@code ore_nickel_kerbin}/{@code ore_zinc_kerbin} hierher.
 */
public final class ThatmoBlocks {

    private ThatmoBlocks() {}

    public static final String BODY = "thatmo";
    /** Erz-Metadatum auf Thatmo = Kerbin. */
    public static final String ORE_BODY = "kerbin";

    public static final RegistrySupplier<Block> ORE_NICKEL = PlanetOres.ore("ore_nickel", ORE_BODY, 5.0F, 10.0F);
    public static final RegistrySupplier<Block> ORE_ZINC = PlanetOres.ore("ore_zinc", ORE_BODY, 5.0F, 10.0F);

    public static void init() {
    }
}
