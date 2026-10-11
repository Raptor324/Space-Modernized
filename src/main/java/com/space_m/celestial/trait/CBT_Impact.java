package com.space_m.celestial.trait;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Einschlagzeitpunkt (Asteroid), 1:1 NTM Space {@code CBT_Impact}.
 * Original-Klasse: CBT_Impact
 */
public class CBT_Impact extends CelestialBodyTrait {

    public long time;

    public CBT_Impact() {}

    public CBT_Impact(long time) {
        this.time = time;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putLong("time", time);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        time = nbt.getLong("time");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeLong(time);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        time = buf.readLong();
    }
}
