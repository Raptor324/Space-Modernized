package com.space_m.dim.laythe;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;

import com.hbm_m.world.gen.LegacyBiome;
import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.laythe.biome.BiomeGenBaseLaythe;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code com.hbm.dim.laythe.WorldGeneratorLaythe}.
 *
 * <p><b>Erze</b> ({@code generateLaythe}, im Laythe-Gestein {@code stone}) als Datapack-Features der vier Laythe-Biome:
 * Asbest {@code asbestosSpawn}x4 y16-31, Beryllium {@code berylliumSpawn}x4 y5-34, Seltene Erden {@code rareSpawn}x5
 * y5-24 ({@code placed_feature/laythe/laythe_*}).
 *
 * <p><b>Strukturen</b> (Dimension 413022): Atom-U-Boot ({@code nuke-sub}, nur Sagan Sea, hoechstens y 54),
 * Vertibird und Vertibird-Wrack (Hoehe -3 / -10, nur {@code rootHeight >= 0}), je Gewicht 6. Vertibirds sind die
 * HBM-Strukturen ({@code StructureManager.vertibird/crashed_vertibird}), das U-Boot liegt umgesetzt unter
 * {@code assets/space_m/structures/nuke-sub.nbt}. Erscheinen erst, wenn HBM die Dimension im Strukturgenerator
 * auswertet (Wunsch W-B1).
 *
 * <p>{@code BlockOre.addValidBody} (Tooltip "Can be found on": emerald, lapis, asbestos, oil) folgt mit R8.
 */
public final class WorldGeneratorLaythe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.laytheDimension}. */
    public static final int LAYTHE_DIMENSION_ID = 413022;

    private WorldGeneratorLaythe() {}

    public static void init() {
        LifecycleEvent.SETUP.register(WorldGeneratorLaythe::registerStructures);
    }

    /** Laythe-/Space-Biom eines {@link LegacyBiome} (Name = Registry-Schluessel fuer Fremdbiome). */
    public static BiomeGenBaseCelestial celestial(LegacyBiome biome) {
        ResourceLocation rl = ResourceLocation.tryParse(biome.name);
        return rl == null ? null : BiomeGenBaseCelestial.byKey(ResourceKey.create(Registries.BIOME, rl));
    }

    public static NBTStructure load(String file) {
        String path = "/assets/" + SpaceMod.MOD_ID + "/structures/" + file;
        try (InputStream in = WorldGeneratorLaythe.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path);
            return new NBTStructure(file, in);
        } catch (IOException e) {
            throw new IllegalStateException("[space_m] structure could not be loaded: " + path, e);
        }
    }

    private static void registerStructures() {
        try {
            NBTStructure nukeSub = load("nuke-sub.nbt");
            NBTStructure.registerStructure(LAYTHE_DIMENSION_ID, new SpawnCondition("laythe_nuke_sub") {{
                structure = new JigsawPiece("laythe_nuke_sub", nukeSub);
                canSpawn = biome -> celestial(biome) == BiomeGenBaseLaythe.laytheOcean;
                maxHeight = 54;
                spawnWeight = 6;
            }});
            NBTStructure.registerStructure(LAYTHE_DIMENSION_ID, new SpawnCondition("laythe_vertibird") {{
                structure = new JigsawPiece("laythe_vertibird", com.hbm_m.main.StructureManager.vertibird, -3);
                canSpawn = biome -> {
                    BiomeGenBaseCelestial b = celestial(biome);
                    return b != null ? b.rootHeight >= 0 : biome.rootHeight >= 0;
                };
                spawnWeight = 6;
            }});
            NBTStructure.registerStructure(LAYTHE_DIMENSION_ID, new SpawnCondition("laythe_crashed_vertibird") {{
                structure = new JigsawPiece("laythe_crashed_vertibird", com.hbm_m.main.StructureManager.crashed_vertibird, -10);
                canSpawn = biome -> {
                    BiomeGenBaseCelestial b = celestial(biome);
                    return b != null ? b.rootHeight >= 0 : biome.rootHeight >= 0;
                };
                spawnWeight = 6;
            }});
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Laythe structure registration failed", e);
        }
    }
}
