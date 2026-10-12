package com.space_m.rocket.transport;

import java.util.ArrayList;
import java.util.List;

import com.space_m.network.SpacePacket;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1:1 NTM Space {@code TransporterLinkerPacket} (S2C): Liste der im Linker gespeicherten Transporter als NBT
 * ({@code "a"}: Liste von {@link TransporterInfo}); der Client ersetzt {@code ItemTransporterLinker.currentTransporters}.
 * Original-Klasse: TransporterLinkerPacket
 */
public final class TransporterLinkerPacket implements SpacePacket {

    private final CompoundTag nbt;

    private TransporterLinkerPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static TransporterLinkerPacket of(List<TransporterInfo> transporters) {
        CompoundTag nbt = new CompoundTag();
        ListTag transporterList = new ListTag();
        for (TransporterInfo info : transporters) {
            CompoundTag tag = new CompoundTag();
            info.writeToNBT(tag);
            transporterList.add(tag);
        }
        nbt.put("a", transporterList);
        return new TransporterLinkerPacket(nbt);
    }

    public static void send(ServerPlayer player, List<TransporterInfo> transporters) {
        SpacePacketHandler.sendToPlayer(player, SpacePacketHandler.TRANSPORTER_LINKER, of(transporters));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }

    public static TransporterLinkerPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new TransporterLinkerPacket(tag != null ? tag : new CompoundTag());
    }

    /** Liest die Liste (auch fuer Tests). */
    public List<TransporterInfo> read() {
        List<TransporterInfo> list = new ArrayList<>();
        ListTag transporterList = nbt.getList("a", Tag.TAG_COMPOUND);
        for (int i = 0; i < transporterList.size(); i++) {
            list.add(TransporterInfo.readFromNBT(transporterList.getCompound(i)));
        }
        return list;
    }

    public static void handle(TransporterLinkerPacket packet, PacketContext ctx) {
        ctx.queue(() -> {
            try {
                ItemTransporterLinker.currentTransporters = packet.read();
            } catch (Exception x) {
                // Original: Fehler still verwerfen
            }
        });
    }
}
