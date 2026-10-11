package com.space_m.dim.duna.biome;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.duna.DunaBlocks;

/**
 * 1:1 {@code com.hbm.dim.duna.biome.BiomeGenBaseDuna}: kein Regen, Temperatur -1, Niederschlag 0, Oberflaeche
 * {@code duna_sands} ueber {@code duna_rock}. Biom-IDs = Standardwerte {@code SpaceConfig.duna*Biome}
 * (122/121/120/119/118).
 *
 * <p>{@code BiomeDecoratorCelestial(duna_rock)} mit {@code generateLakes = false} erzeugt nichts (keine Lava-Quellen,
 * keine Seen) - daher kein Dekorator-Feature im Biom-JSON. {@code BiomeDictionary COLD/DRY/DEAD} -> Biom-Tags
 * {@code forge:is_cold/is_dry/is_dead} bzw. {@code c:...} ({@code data/space_m/tags/worldgen/biome}).
 */
public abstract class BiomeGenBaseDuna extends BiomeGenBaseCelestial {

    public static final BiomeGenBaseCelestial dunaPlains = new BiomeGenDunaPlains(122);
    public static final BiomeGenBaseCelestial dunaLowlands = new BiomeGenDunaLowlands(121);
    public static final BiomeGenBaseCelestial dunaPolar = new BiomeGenDunaPolar(120);
    public static final BiomeGenBaseCelestial dunaHills = new BiomeGenDunaHills(119);
    public static final BiomeGenBaseCelestial dunaPolarHills = new BiomeGenDunaPolarHills(118);

    public BiomeGenBaseDuna(int id, String path) {
        super(id, path);

        this.setDisableRain();
        this.setTemperatureRainfall(-1.0F, 0.0F);

        this.setTopBlock(() -> DunaBlocks.DUNA_SANDS.get().defaultBlockState());
        this.setFillerBlock(() -> DunaBlocks.DUNA_ROCK.get().defaultBlockState());
    }

    /** Erzwingt das Laden (Anmeldung der Biom-IDs fuer die GenLayer). */
    public static void init() {
    }
}
