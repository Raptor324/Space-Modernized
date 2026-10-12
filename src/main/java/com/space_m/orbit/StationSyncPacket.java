package com.space_m.orbit;

import com.space_m.network.SpacePacket;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Station unter dem Spieler an den Client (-> {@link OrbitalStation#clientStation()}). Ersetzt die beiden
 * Original-Wege {@code TileEntityOrbitalStation.serialize} (Kern, {@code networkPackNT(STATION_SIZE / 2)}) und
 * {@code EntityRideableRocket} ({@code EntityBufPacket} an den Fahrer im Transfer): beide schreiben
 * {@code OrbitalStation.getStationFromPosition(x, z).serialize(buf)}. Gesendet jeden Tick an Spieler im Orbit.
 */
public final class StationSyncPacket implements SpacePacket {

    private final OrbitalStation station;

    public StationSyncPacket(OrbitalStation station) {
        this.station = station;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        station.serialize(buf);
    }

    public static StationSyncPacket decode(FriendlyByteBuf buf) {
        return new StationSyncPacket(OrbitalStation.deserialize(buf));
    }

    /** Client-Thread: {@code OrbitalStation.clientStation = OrbitalStation.deserialize(buf)}. */
    public void apply() {
        OrbitalStation.setClientStation(station);
    }
}
