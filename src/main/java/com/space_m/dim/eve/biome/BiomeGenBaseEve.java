package com.space_m.dim.eve.biome;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.eve.EveBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.eve.biome.BiomeGenBaseEve}: Temperatur 1.0, Niederschlag 0.5 (Regen an), Wasserfarbe
 * {@code 0x5b009a}, Oberflaeche {@code eve_silt} ueber {@code eve_rock}, Dekorator {@code BiomeDecoratorCelestial(eve_rock)}
 * (Lavaquellen, im Biom-JSON {@code space_m:eve/decorator}). {@code BiomeDictionary} (HOT, DRY, DEAD, SPOOKY) =
 * Biom-Tags {@code forge:is_hot|is_dry|is_dead|is_spooky}.
 *
 * <p>Biom-IDs = Standardwerte aus {@code SpaceConfig} ({@code eveBiome} 117, {@code eveMountainsBiome} 116,
 * {@code eveOceanBiome} 115, {@code eveSeismicBiome} 114, {@code eveRiverBiome} 113).
 */
public abstract class BiomeGenBaseEve extends BiomeGenBaseCelestial {

    public static final int EVE_BIOME = 117;
    public static final int EVE_MOUNTAINS_BIOME = 116;
    public static final int EVE_OCEAN_BIOME = 115;
    public static final int EVE_SEISMIC_BIOME = 114;
    public static final int EVE_RIVER_BIOME = 113;

    public static final BiomeGenBaseCelestial evePlains = new BiomeGenEvePlains(EVE_BIOME);
    public static final BiomeGenBaseCelestial eveOcean = new BiomeGenEveOcean(EVE_OCEAN_BIOME);
    public static final BiomeGenBaseCelestial eveMountains = new BiomeGenEveMountains(EVE_MOUNTAINS_BIOME);
    public static final BiomeGenBaseCelestial eveSeismicPlains = new BiomeGenEveSeismicPlains(EVE_SEISMIC_BIOME);
    public static final BiomeGenBaseCelestial eveRiver = new BiomeGenEveRiver(EVE_RIVER_BIOME);

    /** {@code waterColorMultiplier} (Biom-JSON {@code water_color}). */
    public static final int WATER_COLOR = 0x5b009a;

    public BiomeGenBaseEve(int id, String path) {
        super(id, path);
        this.setTemperatureRainfall(1.0F, 0.5F);

        this.setTopBlock(() -> EveBlocks.EVE_SILT.get().defaultBlockState());
        this.setFillerBlock(() -> EveBlocks.EVE_ROCK.get().defaultBlockState());
    }

    /** Laedt die Biom-Objekte (Registrierung der IDs vor der ersten GenLayer-Abfrage). */
    public static void init() {
    }

    // ── Hilfen fuer die 1:1-Oberflaechen ──

    protected static BlockState eveRock() {
        return EveBlocks.EVE_ROCK.get().defaultBlockState();
    }

    protected static BlockState basalt() {
        return com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
    }

    protected static BlockState basaltSmooth() {
        return com.hbm_m.block.ModBlocks.BASALT_SMOOTH.get().defaultBlockState();
    }

    private static volatile BlockState dunaRock;
    private static volatile BlockState dunaSands;

    /**
     * {@code ModBlocks.duna_rock}: Ozean und Fluss vergleichen im Original mit dem Duna-Gestein (Kopierfehler im
     * Original - auf der Eve liegt dort nie Duna-Gestein, die Spalten bekommen daher nur Grundgestein). 1:1 beibehalten;
     * der Block gehoert zur Duna (R3), ohne ihn (Luft) ist der Vergleich ebenso immer falsch.
     */
    protected static BlockState dunaRock() {
        BlockState s = dunaRock;
        if (s == null) dunaRock = s = BuiltInRegistries.BLOCK.get(com.space_m.SpaceMod.id("duna_rock")).defaultBlockState();
        return s;
    }

    /** {@code ModBlocks.duna_sands} (nur im nie erreichten Zweig von Ozean/Fluss). */
    protected static BlockState dunaSands() {
        BlockState s = dunaSands;
        if (s == null) dunaSands = s = BuiltInRegistries.BLOCK.get(com.space_m.SpaceMod.id("duna_sands")).defaultBlockState();
        return s;
    }

    /**
     * Ersatz fuer {@code Math.random()} der Original-Oberflaechen (Basalt-Sprenkel unter y 62, Gesteinsflecken der
     * Seismischen Ebene): {@code Math.random} ist nicht reproduzierbar - derselbe Chunk saehe bei jeder Generierung
     * (z.B. Hoehenabfrage einer Struktur und eigentliche Erzeugung in verschiedenen Threads) anders aus. Gleiche
     * Verteilung (gleichverteilt in [0, 1)), aber je Spalte aus Weltseed und Koordinaten geseedet; der Chunk-Zufall
     * {@code rand} bleibt unberuehrt.
     */
    protected static Random columnRandom(CelestialWorld world, int x, int z) {
        return new Random(world.getSeed() ^ ((long) x * 341873128712L + (long) z * 132897987541L) ^ 0x5DEECE66DL);
    }

    protected static ResourceLocation id(String path) {
        return com.space_m.SpaceMod.id(path);
    }
}
