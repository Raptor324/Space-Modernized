package com.space_m.celestial.trait;

import com.space_m.celestial.CelestialFluids;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Defines a water table (or any sort of fluid cycle, for exotic planets), 1:1 NTM Space {@code CBT_Water}.
 * Original-Klasse: CBT_Water
 *
 * <p>{@code FluidType} -> Fluid-Registry-ID (NBT als Text, s. {@link CBT_Atmosphere}).
 */
public class CBT_Water extends CelestialBodyTrait {

    // Doesn't necessarily have to be water
    public ResourceLocation fluid;

    public CBT_Water() {
        fluid = CelestialFluids.WATER;
    }

    public CBT_Water(ResourceLocation fluid) {
        this.fluid = fluid;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putString("fluid", fluid.toString());
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        fluid = CelestialFluids.parse(nbt.getString("fluid"));
        if (fluid.equals(CelestialFluids.NONE)) fluid = CelestialFluids.WATER;
    }

    // These methods are for client syncing, the precision loss is intentional
    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(fluid);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        fluid = buf.readResourceLocation();
    }
}
