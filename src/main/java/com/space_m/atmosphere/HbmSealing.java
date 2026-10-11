package com.space_m.atmosphere;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.space_m.lib.SpaceTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Abdichtung fuer Bloecke ausserhalb des Addons (Original: {@code IBlockSealable} direkt an den Klassen).
 * <ul>
 *   <li>Tag {@code space_m:sealable}: immer dicht (Original {@code isSealed = true}, z.B. Stationskern, HTR-Neo).</li>
 *   <li>Tag {@code space_m:unsealable}: nie dicht, obwohl volle Box (Original {@code isSealed = false}: Ziegelofen,
 *       Hochofen, Diesel, Ventilator, Luftpumpe, Raumofen).</li>
 *   <li>Raptors HBM-Tueren ({@code DoorBlock} + Phantom-Teile {@code UniversalMachinePartBlock}): Original
 *       {@code BlockDoorGeneric.isSealed = !getBlocksMovement} = Tuer geschlossen. Nur <b>lesend</b> ueber den
 *       Tuerzustand ({@code DoorBlockEntity.getState() == 0}, sonst Blockzustand {@code open}); dieselbe Bedingung,
 *       mit der die Tuer ihre Kollision schaltet. Statt des Original-Aufrufs {@code updateSealedState} in
 *       {@code TileEntityDoorGeneric} beobachtet der Handler die Tuerbloecke (keine Aenderung an Raptors Code).</li>
 *   <li>Sonst HBM-Multiblocks ({@code BlockDummyable} im Original): nie dicht.</li>
 * </ul>
 */
public final class HbmSealing implements AtmosphereBlob.SealProvider {

	private HbmSealing() {}

	public static void register() {
		AtmosphereBlob.registerSealProvider(new HbmSealing());
	}

	/** Original {@code block instanceof BlockDummyable}: HBM-Multiblock-Kern oder -Teil. */
	public static boolean isMultiblock(BlockState state) {
		return state.getBlock() instanceof IMultiblockController || state.getBlock() instanceof UniversalMachinePartBlock;
	}

	@Override
	@Nullable
	public Boolean isSealed(Level world, BlockPos pos, BlockState state, AtmosphereBlob.ChunkLookup lookup) {
		if(state.is(SpaceTags.UNSEALABLE)) return false;
		if(state.is(SpaceTags.SEALABLE)) return true;

		if(state.getBlock() instanceof DoorBlock) {
			return isDoorClosed(state, lookup.blockEntity(pos));
		}

		if(state.getBlock() instanceof UniversalMachinePartBlock) {
			BlockEntity be = lookup.blockEntity(pos);
			if(be instanceof IMultiblockPart part) {
				BlockPos controllerPos = part.getControllerPos();
				if(controllerPos != null) {
					BlockState controller = lookup.state(controllerPos);
					if(controller != null && controller.getBlock() instanceof DoorBlock) {
						return isDoorClosed(controller, lookup.blockEntity(controllerPos));
					}
				}
			}
		}

		return null;
	}

	@Override
	public boolean isDynamic(BlockState state) {
		return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof UniversalMachinePartBlock;
	}

	private static boolean isDoorClosed(BlockState controller, @Nullable BlockEntity be) {
		if(be instanceof DoorBlockEntity door) return door.getState() == 0;
		return !controller.getValue(DoorBlock.OPEN);
	}
}
