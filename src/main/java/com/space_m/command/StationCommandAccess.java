package com.space_m.command;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Anbindung von {@link CommandStations} an Stationen und Stations-Laufwerke. Im Original direkte Aufrufe von
 * {@code OrbitalStation}, {@code ItemVOTVdrive} und {@code SolarSystemWorldSavedData.getStations()} - diese
 * entstehen erst mit Orbit/Station (R6) bzw. den Laufwerk-Items; bis dahin gilt {@link #NONE}
 * ("kein gueltiges Laufwerk", "keine Stationen"). R6 setzt die echte Implementierung mit {@link CommandStations#setAccess}.
 */
public interface StationCommandAccess {

    /**
     * Original {@code ItemVOTVdrive.Destination}: Koerper + Ziel-Chunk (Orbit: Stationsraster). {@code body} ist im
     * Orbit null (Original {@code SolarSystem.Body.ORBIT}), {@code dimension} = Original {@code body.getDimensionId()}.
     */
    record Destination(@Nullable CelestialBody body, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, int x, int z) {}

    /** Original {@code OrbitalStation} (Felder fuer list/fetch). */
    record StationInfo(int dX, int dZ, String name, boolean hasStation) {}

    /** {@code stack instanceof ItemVOTVdrive ? ItemVOTVdrive.getDestination(stack) : null}. */
    @Nullable Destination getDestination(ItemStack stack);

    /** {@code OrbitalStation.addStation(x, z, orbiting)}. */
    void addStation(int x, int z, CelestialBody orbiting);

    /** {@code OrbitalStation.spawn(orbitWorld, x, z)} (Blockkoordinaten der Stationsmitte). */
    void spawnStation(ServerLevel orbit, int x, int z);

    /** {@code SolarSystemWorldSavedData.getStations().values()}. */
    Collection<StationInfo> getStations();

    /** {@code new ItemStack(ModItems.full_drive, 1, Body.ORBIT.ordinal())} mit x/z/Processed/stationName. */
    ItemStack createDrive(StationInfo station);

    /** {@code OrbitalStation.STATION_SIZE}. */
    int stationSize();

    StationCommandAccess NONE = new StationCommandAccess() {
        @Override public Destination getDestination(ItemStack stack) { return null; }
        @Override public void addStation(int x, int z, CelestialBody orbiting) {}
        @Override public void spawnStation(ServerLevel orbit, int x, int z) {}
        @Override public Collection<StationInfo> getStations() { return List.of(); }
        @Override public ItemStack createDrive(StationInfo station) { return ItemStack.EMPTY; }
        @Override public int stationSize() { return 1024; }
    };
}
