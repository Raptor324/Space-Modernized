package com.space_m.celestial.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stadtlichter auf der Nachtseite, 1:1 NTM Space {@code CBT_Lights}.
 * Original-Klasse: CBT_Lights
 *
 * <p>{@code Block.getLightValue()} -> {@code BlockState.getLightEmission(level, pos)}.
 */
public class CBT_Lights extends CelestialBodyTrait {

    public int lights;
    public boolean isCivilized;

    public CBT_Lights() {}

    public CBT_Lights(int light) {
        this.lights = light;
    }

    public int getIntensity() {
        if (lights > 10000) return 3;
        if (lights > 2000) return 2;
        if (lights > 400) return 1;
        return 0;
    }

    public void addLight(BlockState block, BlockGetter level, BlockPos pos) {
        lights += block.getLightEmission(level, pos);
    }

    public void removeLight(BlockState block, BlockGetter level, BlockPos pos) {
        lights -= block.getLightEmission(level, pos);
        if (lights < 0) lights = 0;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("lights", lights);
        nbt.putBoolean("isCiv", isCivilized);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        lights = nbt.getInt("lights");
        isCivilized = nbt.getBoolean("isCiv");
    }

    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeInt(lights);
        buf.writeBoolean(isCivilized);
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        lights = buf.readInt();
        isCivilized = buf.readBoolean();
    }
}
