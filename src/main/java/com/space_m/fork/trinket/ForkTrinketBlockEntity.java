package com.space_m.fork.trinket;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Space {@code TileEntityBobble} / {@code TileEntityPlushie} der Fork-Typen: Typ (Byte "type"), beim Plueschtier der
 * Quetsch-Zaehler und der Pressen-Faktor {@code miseryFactor} (Original nur von der Presse gesetzt; der Pfad ist im
 * Original tot, das Feld y+1 ist immer der Pressen-Dummy - Wert bleibt 0, Renderzweig bleibt 1:1 erhalten).
 */
public class ForkTrinketBlockEntity extends BaseHbmBlockEntity {

    public int type;
    public int squishTimer;
    public float miseryFactor;
    public float lastMiseryFactor;

    public ForkTrinketBlockEntity(BlockPos pos, BlockState state) {
        super(ForkTrinkets.TRINKET_BE.get(), pos, state);
    }

    /** Original {@code updateEntity}. */
    public static void tick(Level level, BlockPos pos, BlockState state, ForkTrinketBlockEntity te) {
        if (te.squishTimer > 0) te.squishTimer--;
        te.lastMiseryFactor = te.miseryFactor;
    }

    public void setType(int type) {
        this.type = type;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        this.type = Math.abs(nbt.getByte("type"));
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putByte("type", (byte) type);
    }
}
