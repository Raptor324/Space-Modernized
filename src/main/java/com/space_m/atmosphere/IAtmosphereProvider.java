package com.space_m.atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code IAtmosphereProvider}: Quelle eines abgedichteten Raums (Luftpumpe, Station ...).
 * Abweichung: {@code ThreeInts} -> {@link BlockPos}, {@code FluidType} -> Fluid-ID (wie {@code CBT_Atmosphere}).
 */
public interface IAtmosphereProvider {

	Level getWorld();

	// the maximum distance an object can maintain a blob
	int getMaxBlobRadius();

	// the position to use as root, if it can't be pathed to, the bubble is popped
	BlockPos getRootPosition();

	// Get atmospheric information
	ResourceLocation getFluidType();
	double getFluidPressure();

	void consume(int amount);
	void produce(int amount);

	void onBlobCreated(AtmosphereBlob blob);

}
