package com.space_m.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Paket des Addons (S2C oder C2S). Gegenstueck zu HBMs {@code S2CPacket}/{@code C2SPacket}:
 * {@code write} serialisiert, ein statisches {@code decode(FriendlyByteBuf)} plus {@code handle(pkt, ctx)}
 * werden in {@link SpacePacketHandler} bzw. {@code SpaceClient} angemeldet. Auf 1.21.1 ist der Buffer ein
 * {@code RegistryFriendlyByteBuf} (Unterklasse) - Pakete lesen/schreiben nur ueber {@link FriendlyByteBuf}.
 */
public interface SpacePacket {
    void write(FriendlyByteBuf buf);
}
