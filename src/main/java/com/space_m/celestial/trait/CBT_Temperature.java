package com.space_m.celestial.trait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Oberflaechentemperatur in Grad Celsius, 1:1 NTM Space {@code CBT_Temperature}.
 * Original-Klasse: CBT_Temperature
 */
public class CBT_Temperature extends CelestialBodyTrait {

    public float degrees; // In SI units (Celsius)

    public CBT_Temperature() {
        this.degrees = 20;
    }

    public CBT_Temperature(float degrees) {
        this.degrees = degrees;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("c", degrees);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        degrees = nbt.getFloat("c");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeFloat(degrees);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        degrees = buf.readFloat();
    }
}
