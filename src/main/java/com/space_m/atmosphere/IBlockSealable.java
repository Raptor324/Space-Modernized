package com.space_m.atmosphere;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code IBlockSealable}: Bloecke des Addons, deren Abdichtung vom Zustand abhaengt (Tueren,
 * Stationskern = immer dicht, Luftpumpe/Raumofen = nie dicht). Bloecke anderer Mods: Block-Tags
 * {@code space_m:sealable} (immer dicht) / {@code space_m:unsealable} (nie dicht) bzw.
 * {@link AtmosphereBlob#registerSealProvider} (HBM-Tueren, {@link HbmSealing}).
 *
 * <p>Abweichung: {@code BlockDummyable}-Zweig von {@code updateSealedState} -> {@link #sealedPositions}
 * (Multiblock-Bloecke geben alle ihre Positionen zurueck).
 */
public interface IBlockSealable {

	public boolean isSealed(Level world, BlockPos pos);

	/** true = Zustand kann wechseln, ohne dass ein Block gesetzt/abgebaut wird (Tuer) -> wird beobachtet. */
	public default boolean isDynamic(BlockState state) {
		return false;
	}

	/** Alle Positionen, deren Abdichtung sich mit diesem Block aendert (Original: Multiblock-Ausdehnung). */
	public default Iterable<BlockPos> sealedPositions(Level world, BlockPos pos) {
		return List.of(pos);
	}

	/** Original {@code updateSealedState}: nach einem Zustandswechsel (Tuer offen/zu) aufrufen. */
	public default void updateSealedState(Level world, BlockPos pos) {
		List<AtmosphereBlob> nearbyBlobs = ChunkAtmosphereManager.proxy.getBlobsWithinRadius(world, pos);

		for(BlockPos p : sealedPositions(world, pos)) {
			ChunkAtmosphereManager.proxy.onSealableChange(world, p, this, nearbyBlobs);
		}
	}

}
