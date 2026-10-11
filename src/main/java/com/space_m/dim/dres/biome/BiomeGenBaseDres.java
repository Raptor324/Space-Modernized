package com.space_m.dim.dres.biome;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockSellafieldSlaked;
import com.space_m.dim.BiomeGenBaseCelestial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.dres.biome.BiomeGenBaseDres}: kein Regen, Temperatur -1, Niederschlag 0, Oberflaeche und
 * Fuellung {@code sellafield_slaked} (geloeschtes Sellafit). Biom-IDs = {@code SpaceConfig.dresBiome} 112 /
 * {@code dresBasinBiome} 111. {@code BiomeDecoratorCelestial(dres_rock)} mit {@code generateLakes = false} erzeugt
 * nichts (kein Dekorator-Feature).
 *
 * <p><b>Sellafit-Metadaten:</b> Das Original faerbt {@code sellafield_slaked} ueber das Metadatum dunkler
 * ({@code colorMultiplier = HSB(0, 0, 1 - meta/15)}). HBM-Modernized bildet das als vier Bloecke ab
 * ({@code sellafield_slaked}, {@code _slaked1..3}) mit {@code color_level} - dieselbe Zuordnung wie
 * {@code FalloutConfigJSON.sellafiteForLevel} (>= 8 -> slaked3, >= 5 -> slaked2, >= 2 -> slaked1), dazu die
 * positionsabhaengige Texturvariante des Originals ({@code getIcon(world, x, y, z)} =
 * {@link BlockSellafieldSlaked#variantFromPos}).
 */
public abstract class BiomeGenBaseDres extends BiomeGenBaseCelestial {

    public static final BiomeGenBaseCelestial dresPlains = new BiomeGenDresPlains(112);
    public static final BiomeGenBaseCelestial dresCanyon = new BiomeGenDresCanyon(111);

    public BiomeGenBaseDres(int id, String path) {
        super(id, path);
        this.setDisableRain();
        this.setTemperatureRainfall(-1.0F, 0.0F);

        this.setTopBlock(() -> ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
        this.setFillerBlock(() -> ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
    }

    /** {@code sellafield_slaked} mit Metadatum {@code meta} an Weltposition (x, y, z). */
    public static BlockState slaked(int meta, int x, int y, int z) {
        Block block;
        if (meta >= 8) block = ModBlocks.SELLAFIELD_SLAKED3.get();
        else if (meta >= 5) block = ModBlocks.SELLAFIELD_SLAKED2.get();
        else if (meta >= 2) block = ModBlocks.SELLAFIELD_SLAKED1.get();
        else block = ModBlocks.SELLAFIELD_SLAKED.get();
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockSellafieldSlaked.COLOR_LEVEL)) state = state.setValue(BlockSellafieldSlaked.COLOR_LEVEL, Math.min(meta, 10));
        if (state.hasProperty(BlockSellafieldSlaked.VARIANT)) state = state.setValue(BlockSellafieldSlaked.VARIANT, BlockSellafieldSlaked.variantFromPos(new BlockPos(x, y, z)));
        return state;
    }

    /** Erzwingt das Laden (Anmeldung der Biom-IDs fuer die GenLayer). */
    public static void init() {
    }
}
