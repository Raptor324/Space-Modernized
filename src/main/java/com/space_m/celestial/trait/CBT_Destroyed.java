package com.space_m.celestial.trait;

import com.space_m.celestial.CelestialBody;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Zerstoerter Himmelskoerper (Sonne -> Schwarzes Loch, Planet nach Krieg), 1:1 NTM Space {@code CBT_Destroyed}.
 * Original-Klasse: CBT_Destroyed
 */
public class CBT_Destroyed extends CelestialBodyTrait {

    public float destProgress;

    public CBT_Destroyed() {}

    public CBT_Destroyed(float destProgress) {
        this.destProgress = destProgress;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("destProgress", destProgress);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        destProgress = nbt.getFloat("destProgress");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        //buf.writeFloat(interp);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        //interp = buf.readFloat();
    }

    @Override
    public void update(boolean isRemote, CelestialBody body) {
        if (isRemote) {
            destProgress = Math.min(201.0f, destProgress + 0.0025f * (201.0f - destProgress) * 0.15f);
            if (destProgress >= 200) {
                destProgress = 0;
            }
        }
    }
}
