package com.space_m.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Basis fuer alle SavedData des Addons (Himmelskoerper-Traits, Stationen, Atmosphaeren-Blobs, Satelliten).
 * Unterklassen implementieren nur {@link #write(CompoundTag)}; die {@code save}-Signatur
 * (1.20.1 {@code save(tag)}, 1.21.1 {@code save(tag, HolderLookup.Provider)}) liegt hier.
 * Laden/Anlegen ueber {@link SavedDataHooks}.
 */
public abstract class SpaceSavedData extends SavedData {

    /** Schreibt den Zustand in {@code tag} und gibt ihn zurueck. */
    protected abstract CompoundTag write(CompoundTag tag);

    //? if < 1.21.1 {
    @Override
    public CompoundTag save(CompoundTag tag) {
        return write(tag);
    }
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return write(tag);
    }
    *///?}
}
