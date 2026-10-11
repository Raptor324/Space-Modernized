package com.space_m.dim.legacy;

import com.space_m.dim.BiomeGenBaseCelestial;

/**
 * Vanilla-1.7.10-Biome, die von den Original-GenLayer-Ketten der Koerper als int-ID geliefert werden, obwohl der
 * Koerper sie nicht als eigenes Biom hat. Einziger Fall: {@code BiomeGenBase.river} (ID 7) aus
 * {@link GenLayerRiverMix} (Duna, Moho; Eve/Tekto mischen ueber eigene RiverMix-Schichten). Die Mischung liegt im
 * Original nur in der groben Schicht ({@code biomeLayer} = Rauschhoehen), nie in der Detailschicht - der Fluss
 * formt also nur die Rinnen im Gelaende ({@code rootHeight -0.5}), Oberflaeche und gespeicherte Biome bleiben die
 * des Koerpers. Deshalb gibt es hierfuer kein Datapack-Biom.
 *
 * <p>Gemeinsam fuer alle Koerper (eine ID darf nur einmal angemeldet werden): vor der ersten Generierung
 * {@link #init()} aufrufen.
 */
public final class LegacyBiomes {

    private LegacyBiomes() {}

    /** 1.7.10 {@code BiomeGenBase.height_ShallowWaters}. */
    public static final BiomeGenBaseCelestial.Height height_ShallowWaters = new BiomeGenBaseCelestial.Height(-0.5F, 0.0F);

    /** 1.7.10 {@code BiomeGenBase.river = new BiomeGenRiver(7).setColor(255).setBiomeName("River").setHeight(height_ShallowWaters)}. */
    public static final BiomeGenBaseCelestial RIVER = new Vanilla(GenLayer.RIVER, "legacy_river", "River", height_ShallowWaters);

    /** Erzwingt das Laden (Anmeldung der IDs in {@link BiomeGenBaseCelestial#getBiome}). */
    public static void init() {
    }

    private static final class Vanilla extends BiomeGenBaseCelestial {
        Vanilla(int id, String path, String name, Height height) {
            super(id, path);
            setBiomeName(name);
            setHeight(height);
        }
    }
}
