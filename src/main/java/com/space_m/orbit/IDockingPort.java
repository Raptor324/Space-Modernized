package com.space_m.orbit;

import com.space_m.rocket.entity.EntityRideableRocket;

/**
 * Andockziel einer stehenden Rakete. Im Original fragt {@code EntityRideableRocket.onUpdate} den Kern unter der
 * Raketenspitze direkt ab ({@code te instanceof TileEntityOrbitalStation} bzw. {@code TileEntityOrbitalStationLauncher}
 * -> {@code dockRocket(this)}); hier implementieren beide Kerne diese Schnittstelle
 * ({@link TileEntityOrbitalStation} R6-A, Startport R6-B).
 */
public interface IDockingPort {
    void dockRocket(EntityRideableRocket rocket);
}
