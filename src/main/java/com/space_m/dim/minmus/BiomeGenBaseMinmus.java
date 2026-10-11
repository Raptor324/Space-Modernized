package com.space_m.dim.minmus;

import com.space_m.dim.BiomeGenBaseCelestial;

/**
 * 1:1 {@code com.hbm.dim.minmus.biome.BiomeGenBaseMinmus}: Schnee ({@code setEnableSnow}), Temperatur -1,
 * Niederschlag 0, Oberflaeche und Fuellung {@code minmus_regolith}.
 *
 * <p>{@code theBiomeDecorator = new BiomeDecoratorCelestial(minmus_regolith)} mit {@code generateLakes = false} und
 * {@code lakeChancePerChunk = 0} erzeugt nichts (keine Lava-Quellen, keine Seen) - daher kein
 * {@code space_m:celestial_decorator} in den Biom-JSONs ({@code data/space_m/worldgen/biome/minmus_*.json}).
 */
public abstract class BiomeGenBaseMinmus extends BiomeGenBaseCelestial {

    /** {@code SpaceConfig.minmusBiome} (124). */
    public static final int MINMUS_BIOME_ID = 124;
    /** {@code SpaceConfig.minmusBasinBiome} (123). */
    public static final int MINMUS_BASIN_BIOME_ID = 123;

    public static final BiomeGenBaseMinmus minmusPlains = new BiomeGenMinmusHills(MINMUS_BIOME_ID);
    public static final BiomeGenBaseMinmus minmusCanyon = new BiomeGenMinmusBasin(MINMUS_BASIN_BIOME_ID);

    public BiomeGenBaseMinmus(int id, String path) {
        super(id, path);
        // setEnableSnow(): Biom-JSON has_precipitation true (Schnee bei Temperatur -1)
        this.setTemperatureRainfall(-1.0F, 0.0F);

        this.setTopBlock(() -> MinmusBlocks.MINMUS_REGOLITH.get().defaultBlockState()); //remind me to send roadhog his daily modmail
        this.setFillerBlock(() -> MinmusBlocks.MINMUS_REGOLITH.get().defaultBlockState());
    }
}
