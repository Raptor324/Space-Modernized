package com.space_m.dim;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
//? if >= 1.21.1 {
/*import com.mojang.serialization.MapCodec;
*///?}
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Biomverteilung der Himmelskoerper ({@code "type": "space_m:celestial"}) - Huelle um den 1:1 portierten
 * {@link WorldChunkManagerCelestial} (GenLayer-Ketten des Originals bzw. {@code WorldChunkManagerHell}).
 *
 * <p>Gespeicherte Chunk-Biome sind in 1.20 je 4x4x4 Bloecke ("Quart"); jede Quart bekommt das Biom der
 * Detailschicht in ihrer Mitte. Die Generierung selbst (Rauschhoehen, Oberflaeche, Hoehlen) fragt die
 * GenLayer weiter blockgenau ab wie im Original.
 *
 * <p>JSON: {@code {"type": "space_m:celestial", "body": "mun", "biomes": ["space_m:mun"]}} - {@code biomes} listet
 * alle Biome, die die Schichten liefern koennen (Datapack-Pflicht fuer Features/Strukturen/locate).
 */
public class CelestialBiomeSource extends BiomeSource {

    private static final Logger LOGGER = LogUtils.getLogger();

    //? if < 1.21.1 {
    public static final Codec<CelestialBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    //?} else {
    /*public static final MapCodec<CelestialBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    *///?}
            Codec.STRING.fieldOf("body").forGetter(s -> s.body),
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(s -> s.biomes)
    ).apply(instance, instance.stable(CelestialBiomeSource::new)));

    private final String body;
    private final HolderSet<Biome> biomes;
    private volatile Map<ResourceKey<Biome>, Holder<Biome>> byKey;
    private volatile boolean warned;

    public CelestialBiomeSource(String body, HolderSet<Biome> biomes) {
        this.body = body;
        this.biomes = biomes;
    }

    public String body() {
        return body;
    }

    //? if < 1.21.1 {
    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }
    //?} else {
    /*@Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }
    *///?}

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    private Map<ResourceKey<Biome>, Holder<Biome>> byKey() {
        Map<ResourceKey<Biome>, Holder<Biome>> m = byKey;
        if (m == null) {
            Map<ResourceKey<Biome>, Holder<Biome>> built = new HashMap<>();
            for (Holder<Biome> h : biomes) {
                h.unwrapKey().ifPresent(k -> built.put(k, h));
            }
            byKey = m = built;
        }
        return m;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        BiomeGenBaseCelestial biome = CelestialGenerators.state(body).chunkManager.getBiomeGenForCoords((quartX << 2) + 2, (quartZ << 2) + 2);
        Holder<Biome> holder = byKey().get(biome.key);
        if (holder == null) {
            if (!warned) {
                warned = true;
                LOGGER.warn("[space_m] Biome {} of body {} is not listed in its biome_source, using the first entry", biome.key.location(), body);
            }
            holder = biomes.get(0);
        }
        return holder;
    }
}
