package com.space_m.dim;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.space_m.SpaceMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Gemeinsame Dimensions-Kennungen des Addons (ersetzt die int-IDs {@code SpaceConfig.*Dimension} 413015-413025 des
 * Originals). Die Dimensionen selbst legt das Dimensions-Framework als Datapack an
 * ({@code data/space_m/dimension/<name>.json} + {@code dimension_type/<name>.json}); Kerbin ist
 * {@link Level#OVERWORLD} und hat keine eigene Dimension.
 *
 * <p>Der Pfad entspricht dem Namen des Himmelskoerpers ({@code CelestialBody.name}), die Orbit-Dimension heisst
 * {@code orbit}. Zuordnung Koerper - Dimension: {@code com.space_m.celestial.CelestialBody#getBody(Level)}.
 */
public final class SpaceDimensions {
    private SpaceDimensions() {}

    /** Original {@code moonDimension} (413015). */
    public static final ResourceKey<Level> MUN = level("mun");
    /** Original {@code dunaDimension} (413016). */
    public static final ResourceKey<Level> DUNA = level("duna");
    /** Original {@code ikeDimension} (413017). */
    public static final ResourceKey<Level> IKE = level("ike");
    /** Original {@code eveDimension} (413018). */
    public static final ResourceKey<Level> EVE = level("eve");
    /** Original {@code dresDimension} (413019). */
    public static final ResourceKey<Level> DRES = level("dres");
    /** Original {@code mohoDimension} (413020). */
    public static final ResourceKey<Level> MOHO = level("moho");
    /** Original {@code minmusDimension} (413021). */
    public static final ResourceKey<Level> MINMUS = level("minmus");
    /** Original {@code laytheDimension} (413022). */
    public static final ResourceKey<Level> LAYTHE = level("laythe");
    /** Original {@code orbitDimension} (413023): Raumstationen, Schwerkraft 0, {@code inOrbit = true}. */
    public static final ResourceKey<Level> ORBIT = level("orbit");
    /** Original {@code tektoDimension} (413024). */
    public static final ResourceKey<Level> TEKTO = level("tekto");
    /** Original {@code thatmoDimension} (413025). */
    public static final ResourceKey<Level> THATMO = level("thatmo");

    /** Alle 11 Addon-Dimensionen in Original-ID-Reihenfolge (413015-413025). */
    public static final List<ResourceKey<Level>> ALL = List.of(MUN, DUNA, IKE, EVE, DRES, MOHO, MINMUS, LAYTHE, ORBIT, TEKTO, THATMO);

    /** Die 10 Planeten-/Mond-Dimensionen (ohne Orbit). */
    public static final List<ResourceKey<Level>> BODIES = List.of(MUN, DUNA, IKE, EVE, DRES, MOHO, MINMUS, LAYTHE, TEKTO, THATMO);

    /** {@code space_m:<name>} als Level-Schluessel. */
    public static ResourceKey<Level> level(String name) {
        return ResourceKey.create(Registries.DIMENSION, SpaceMod.id(name));
    }

    /** {@code space_m:<name>} als DimensionType-Schluessel (gleicher Name wie die Dimension). */
    public static ResourceKey<DimensionType> type(String name) {
        return ResourceKey.create(Registries.DIMENSION_TYPE, SpaceMod.id(name));
    }

    public static boolean isSpaceDimension(@Nullable ResourceKey<Level> key) {
        return key != null && ALL.contains(key);
    }

    public static boolean isOrbit(@Nullable ResourceKey<Level> key) {
        return ORBIT.equals(key);
    }
}
