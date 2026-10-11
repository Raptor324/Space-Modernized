package com.space_m.dim.duna;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;

import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.SpaceWorldgen;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code com.hbm.dim.duna.WorldGeneratorDuna}.
 *
 * <p><b>Erze</b> ({@code generateDuna}): Datapack-Features in allen Duna-Biomen
 * ({@code placed_feature/duna/ore_iron_duna|ore_zinc_duna|basalt_*}), Werte 1:1: Eisen {@code ironSpawn}x8 y32-95,
 * Zink {@code zincSpawn}x9 y4-30, Basalt-Erze nur im Basalt der Tiefland-Hoehlen (Schwefel 12x6, Fluorit 8x8,
 * Asbest 8x9, Edelstein 2x4, Molysit 6x10, je y0-15).
 *
 * <p><b>Laterne</b>: {@link DunaLanternFeature} ({@code space_m:duna_lantern}, 1/1234 je Chunk).
 *
 * <p><b>Funkstation</b> ({@code SpawnCondition "duna_comms"}): Einzelteil {@code duna-comms.nbt}, Hoehenversatz -1,
 * Gewicht 6, nur in flachen Biomen ({@code heightVariation < 0.1}: Ebene, Tiefland, Eisschild), Leergewicht 18; wie
 * die Mun-Basis ueber die NBTStructure-Schnittstelle von HBM unter der Original-ID 413016 angemeldet - erscheint erst
 * mit Wunsch W-B1. Die Marsbasis ({@code martian-base.nbt}) baut {@link MartianWorld} bei Weltart "The Martian".
 *
 * <p>{@code BlockOre.addValidBody(ore_oil, DUNA)} folgt mit dem BlockOre-Tooltip (R8).
 */
public final class WorldGeneratorDuna {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.dunaDimension}. */
    public static final int DUNA_DIMENSION_ID = 413016;

    public static final RegistrySupplier<DunaLanternFeature> LANTERN = SpaceWorldgen.FEATURES.register("duna_lantern", DunaLanternFeature::new);

    private WorldGeneratorDuna() {}

    public static void init() {
        LifecycleEvent.SETUP.register(WorldGeneratorDuna::registerStructures);
    }

    static NBTStructure load(String file) {
        String path = "/assets/" + SpaceMod.MOD_ID + "/structures/" + file;
        try (InputStream in = WorldGeneratorDuna.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path);
            return new NBTStructure(file, in);
        } catch (IOException e) {
            throw new IllegalStateException("[space_m] Duna structure could not be loaded: " + path, e);
        }
    }

    /** {@code biome.heightVariation} eines Koerper-Bioms ueber den Biom-Schluessel ({@code LegacyBiome.name}). */
    static float heightVariation(String biomeKey) {
        ResourceLocation rl = ResourceLocation.tryParse(biomeKey);
        BiomeGenBaseCelestial b = rl == null ? null : BiomeGenBaseCelestial.byKey(ResourceKey.create(Registries.BIOME, rl));
        return b == null ? Float.MAX_VALUE : b.heightVariation;
    }

    private static void registerStructures() {
        try {
            JigsawPiece comms = new JigsawPiece("duna_comms", load("duna-comms.nbt"), -1);
            NBTStructure.registerStructure(DUNA_DIMENSION_ID, new SpawnCondition("duna_comms") {{
                structure = comms;
                canSpawn = biome -> heightVariation(biome.name) < 0.1F;
                spawnWeight = 6;
            }});
            NBTStructure.registerNullWeight(DUNA_DIMENSION_ID, 18);
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Duna structure registration failed", e);
        }
    }
}
