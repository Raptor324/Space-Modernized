package com.space_m.dim.legacy;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Helfer fuer die 1:1 portierten 1.7.10-Generatoren, die auf einem {@code Block[65536]}-Puffer arbeiten.
 *
 * <p>Der Puffer ist hier ein {@code BlockState[65536]} mit dem Original-Index {@code (x * 16 + z) * 256 + y}.
 * Wie im Original gibt es zwei Arten "leer": {@code null} (nie gesetzt bzw. von Hoehlen ausgehoelt) und
 * {@link net.minecraft.world.level.block.Blocks#AIR} (vom Rauschfueller explizit gesetzt) - einige Generatoren
 * ({@code MapGenPlateau}) unterscheiden das, darum bleibt die Unterscheidung erhalten. Vergleiche
 * {@code blocks[i] == Blocks.x} des Originals vergleichen den Block (Metadaten ignoriert) -> {@link #is}.
 */
public final class Legacy {

    private Legacy() {}

    /** Puffergroesse eines 1.7.10-Chunks (16 x 16 x 256). */
    public static final int CHUNK_SIZE = 65536;

    /** {@code (x * 16 + z) * 256 + y}. */
    public static int index(int x, int y, int z) {
        return (x * 16 + z) * 256 + y;
    }

    /** {@code block == other} (Blockvergleich wie im Original; {@code null} nur gleich {@code null}). */
    public static boolean is(@Nullable BlockState state, @Nullable BlockState other) {
        if (state == null || other == null) return state == other;
        return state.getBlock() == other.getBlock();
    }

    public static boolean is(@Nullable BlockState state, Block block) {
        return state != null && state.getBlock() == block;
    }

    /** 1.7.10 {@code Block.isOpaqueCube()}: voller, undurchsichtiger Block. */
    public static boolean isOpaqueCube(@Nullable BlockState state) {
        return state != null && !state.isAir() && state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** 1.7.10 {@code getMaterial().isLiquid()}. */
    public static boolean isLiquid(@Nullable BlockState state) {
        return state != null && !state.getFluidState().isEmpty();
    }

    /** 1.7.10 {@code getMaterial() == Material.air} (fuer gesetzte Bloecke). */
    public static boolean isAirMaterial(@Nullable BlockState state) {
        return state != null && state.isAir();
    }

    /** {@code MathHelper.floor_double}. */
    public static int floor(double d) {
        int i = (int) d;
        return d < (double) i ? i - 1 : i;
    }

    /** {@code MathHelper.floor_double_long}. */
    public static long floorLong(double d) {
        long i = (long) d;
        return d < (double) i ? i - 1L : i;
    }

    /** {@code MathHelper.ceiling_double_int}. */
    public static int ceil(double d) {
        int i = (int) d;
        return d > (double) i ? i + 1 : i;
    }

    /** {@code MathHelper.clamp_double}. */
    public static double clamp(double d, double min, double max) {
        return d < min ? min : (d > max ? max : d);
    }

    /** {@code MathHelper.denormalizeClamp}. */
    public static double denormalizeClamp(double a, double b, double t) {
        return t < 0.0D ? a : (t > 1.0D ? b : a + (b - a) * t);
    }

    /** {@code MathHelper.sqrt_float}. */
    public static float sqrt(float f) {
        return (float) Math.sqrt((double) f);
    }
}
