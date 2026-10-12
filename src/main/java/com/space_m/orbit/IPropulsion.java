package com.space_m.orbit;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code api.hbm.tile.IPropulsion}: Antrieb einer Raumstation (Xenon-Triebwerk, HTR3/HTRF4/HTR-Neo, LPW2,
 * Kreativ-Antrieb - R6-B). Muss an einer {@link BlockEntity} haengen; die Station unter ihr nutzt alle angemeldeten
 * Antriebe fuer {@link OrbitalStation#travelTo}.
 * Original-Klasse: IPropulsion
 */
// it go
public interface IPropulsion {

    /**
     * Adds a propulsion system to a station
     * Must be associated with a TileEntity
     */

    // Register the propulsion source to a station, so the station can utilise it for travel
    default void registerPropulsion() {
        OrbitalStation.addPropulsion(this);
    }

    // And unregister when the block is broken
    default void unregisterPropulsion() {
        OrbitalStation.removePropulsion(this);
    }

    /** Original {@code getTileEntity()}. */
    BlockEntity getBlockEntity();

    // Is the engine sufficiently fueled to perform a burn changing the velocity of a given mass by deltaV
    boolean canPerformBurn(int shipMass, double deltaV);

    // If the above method fails, add any information about why it couldn't
    void addErrors(List<Component> errors);

    // How much thrust does this engine produce? Higher = faster travel
    float getThrust();

    // Turns the engine on if not already running
    // returns a number of ticks to wait before the engine can provide thrust
    int startBurn();

    // Turns the engine off if it wasn't already running
    // returns a number of ticks to wait before another attempt to travel can be made
    int endBurn();
}
