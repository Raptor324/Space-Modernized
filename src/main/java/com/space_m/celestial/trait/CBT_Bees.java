package com.space_m.celestial.trait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Amount of bees in a dimension (1 == x100 bees), 1:1 NTM Space {@code CBT_Bees}.
 * Original-Klasse: CBT_Bees
 */
public class CBT_Bees extends CelestialBodyTrait {

    public float bees;

    public CBT_Bees() {}

    public CBT_Bees(float bees) {
        this.bees = bees;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("bees", bees);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        bees = nbt.getFloat("bees");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeFloat(bees);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        bees = buf.readFloat();
    }
}
