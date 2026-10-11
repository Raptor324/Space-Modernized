package com.space_m.platform;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * SavedData laden/anlegen (Muster {@code AnnihilatorPoolManager.get} in HBM-Modernized):
 * 1.20.1 {@code computeIfAbsent(load, ctor, name)}, 1.21.1 {@code computeIfAbsent(SavedData.Factory, name)}.
 *
 * <p>Je Dimension ({@link #get}) fuer Atmosphaeren-Blobs/Stationen; global ({@link #global}) - in der Overworld
 * gespeichert - fuer Himmelskoerper-/Sonnensystemzustand (Original: {@code CelestialBodyWorldSavedData} u.ae.).
 */
public final class SavedDataHooks {
    private SavedDataHooks() {}

    public static <T extends SavedData> T get(ServerLevel level, String name, Supplier<T> create, Function<CompoundTag, T> load) {
        //? if < 1.21.1 {
        return level.getDataStorage().computeIfAbsent(load, create, name);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(create, (tag, registries) -> load.apply(tag), null), name);
        *///?}
    }

    public static <T extends SavedData> T global(MinecraftServer server, String name, Supplier<T> create, Function<CompoundTag, T> load) {
        return get(server.overworld(), name, create, load);
    }
}
