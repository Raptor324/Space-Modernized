package com.space_m.bomb;

import com.hbm_m.block.generic.BlockFallout;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NTM Space {@code salted_fallout} ({@code BlockFallout}, Textur {@code ash}): Verhalten der HBM-Fallout-Schicht
 * (Form 1/8, Drop {@code fallout}, Platzierungsregel, Kontamination beim Betreten).
 *
 * <p>Original {@code updateTick}: {@code if(this == ModBlocks.salted_fallout) incrementRad(world, x, y, z, 50)}. Der
 * Konstruktor setzt {@code setTickRandomly(true)} aber nur {@code if(this == ModBlocks.salted_fallout)} - waehrend der
 * Konstruktor laeuft, ist das Feld noch {@code null}, der Block tickt im Original also nie. 1:1 uebernommen:
 * {@link #RANDOM_TICKS} = false (Umschalter, falls das beabsichtigte Verhalten gewuenscht ist).
 * Original-Klasse: BlockFallout
 */
public class BlockSaltedFallout extends BlockFallout {

    /** Original: nie zufaellig getickt (siehe Klassenkommentar). */
    public static final boolean RANDOM_TICKS = false;

    public BlockSaltedFallout(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return RANDOM_TICKS;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationManager.getProxy().incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), 50);
    }
}
