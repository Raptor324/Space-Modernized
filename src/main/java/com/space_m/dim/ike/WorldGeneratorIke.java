package com.space_m.dim.ike;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;

import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;
import com.space_m.dim.SpaceWorldgen;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;

/**
 * 1:1 {@code com.hbm.dim.Ike.WorldGeneratorIke}.
 *
 * <p><b>Erze</b> ({@code generateIke}): Datapack-Features im Biom {@code space_m:ike}, Werte 1:1: Asbest
 * {@code asbestosSpawn}x8 y3-24, Kupfer {@code copperSpawn}x9 y4-30, Eisen {@code ironSpawn}x8 y1-33, Lithium
 * {@code lithiumSpawn}x6 y4-11, Coltan 2x4 y15-54, Mineral {@code mineralSpawn}x10 y12-43 - im Ike-Gestein.
 *
 * <p><b>Sockel-Hack</b>: {@link IkePedestalFeature} ({@code space_m:ike_pedestal}).
 *
 * <p><b>Ike-Artefakt</b> ({@code SpawnCondition "ike_artifact"}, Teil {@code ike_artifact.nbt} mit Hoehenversatz -5,
 * Gewicht 4, Null-Gewicht 12): angemeldet unter der Original-Dimensions-ID 413017; die HBM-Strukturerzeugung fragt
 * derzeit nur Dimension 0 ab (Wunsch W-B1).
 *
 * <p><b>Pyramide</b> ({@code pyramidStructure}, {@code AncientTomb}): braucht in 1.20 eine Struktur ueber mehrere
 * Chunks; HBMs {@code LegacyDungeonStructure} prueft fest die Wuesten-Biombedingung -> Wunsch W-R3a-1.
 */
public final class WorldGeneratorIke {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.ikeDimension}. */
    public static final int IKE_DIMENSION_ID = 413017;

    public static RegistrySupplier<IkePedestalFeature> IKE_PEDESTAL;

    private WorldGeneratorIke() {}

    public static void init() {
        IKE_PEDESTAL = SpaceWorldgen.FEATURES.register("ike_pedestal", IkePedestalFeature::new);
        LifecycleEvent.SETUP.register(WorldGeneratorIke::registerStructures);
    }

    private static NBTStructure load(String file) {
        String path = "/assets/" + SpaceMod.MOD_ID + "/structures/" + file;
        try (InputStream in = WorldGeneratorIke.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path);
            return new NBTStructure(file, in);
        } catch (IOException e) {
            throw new IllegalStateException("[space_m] Ike structure could not be loaded: " + path, e);
        }
    }

    private static void registerStructures() {
        try {
            JigsawPiece artifact = new JigsawPiece("ike_artifact", load("ike_artifact.nbt"), -5);
            NBTStructure.registerStructure(IKE_DIMENSION_ID, new SpawnCondition("ike_artifact") {{
                structure = artifact;
                spawnWeight = 4;
            }});

            NBTStructure.registerNullWeight(IKE_DIMENSION_ID, 12);
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Ike artifact structure registration failed", e);
        }
    }
}
