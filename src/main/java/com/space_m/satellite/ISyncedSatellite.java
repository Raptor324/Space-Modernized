package com.space_m.satellite;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Zusatzdaten eines Addon-Satelliten fuer den Client-Sync (Original {@code SatelliteBase.serialize/deserialize},
 * ueberschrieben von {@code SatelliteWar}/{@code SatelliteRailgun}). Die HBM-Satelliten haben keine Zusatzdaten.
 */
public interface ISyncedSatellite {

    void serialize(FriendlyByteBuf buf);

    void deserialize(FriendlyByteBuf buf);
}
