package com.space_m.satellite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;
import com.space_m.network.SpacePacket;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.networking.NetworkManager.PacketContext;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C: Satelliten je Himmelskoerper, 1:1 Abschnitt SATELLITES von NTM Space {@code PermaSyncHandler}
 * (je Koerper: Frequenz, Satelliten-ID, {@code serialize}). Das Original schickt ihn jedem Spieler jeden Tick; hier wird
 * jeden Tick gebaut, aber nur bei Aenderung (und sonst alle 100 Ticks) gesendet - gleicher Client-Zustand, weniger Last.
 */
public final class SatelliteSyncPacket implements SpacePacket {

    private final byte[] data;

    private SatelliteSyncPacket(byte[] data) {
        this.data = data;
    }

    private static final Map<UUID, byte[]> LAST_SENT = new HashMap<>();

    /** Aus dem Server-Tick (Ende). */
    public static void tick(MinecraftServer server) {
        long time = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            byte[] bytes = build(player);
            byte[] last = LAST_SENT.get(player.getUUID());
            if (last != null && Arrays.equals(last, bytes) && time % 100 != 0) continue;
            LAST_SENT.put(player.getUUID(), bytes);
            SpacePacketHandler.sendToPlayer(player, SpacePacketHandler.SATELLITES, new SatelliteSyncPacket(bytes));
        }
        LAST_SENT.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    static byte[] build(ServerPlayer player) {
        SpaceSatelliteRegistry.ensureRegistered();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ServerLevel world = player.serverLevel();
        int x = (int) player.getX();
        int z = (int) player.getZ();
        Map<String, ServerLevel> levels = SpaceSatellites.syncedLevels(world, x, z);

        buf.writeUtf(SpaceSatellites.bodyName(SpaceSatellites.getLevel(world, x, z)));
        buf.writeInt(levels.size());
        for (Map.Entry<String, ServerLevel> e : levels.entrySet()) {
            buf.writeUtf(e.getKey());
            SatelliteManager manager = SatelliteManager.get(e.getValue());
            Map<Integer, SatelliteOrbit> orbits = SpaceSatellites.orbits(e.getValue());
            buf.writeInt(manager.all().size());
            for (Map.Entry<Integer, Satellite> sat : manager.all().entrySet()) {
                buf.writeInt(sat.getKey());
                buf.writeInt(sat.getValue().getID());
                orbits.get(sat.getKey()).serialize(buf);
                if (sat.getValue() instanceof ISyncedSatellite synced) synced.serialize(buf);
            }
        }
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        buf.release();
        return out;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByteArray(data);
    }

    public static SatelliteSyncPacket decode(FriendlyByteBuf buf) {
        return new SatelliteSyncPacket(buf.readByteArray());
    }

    public static void handle(SatelliteSyncPacket pkt, PacketContext ctx) {
        ctx.queue(() -> apply(pkt.data));
    }

    /** Original {@code PermaSyncHandler.readPacket} (SATELLITES). */
    static void apply(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        String current = buf.readUtf();
        int bodies = buf.readInt();
        Map<String, Map<Integer, ClientSatellite>> byBody = new HashMap<>();
        for (int b = 0; b < bodies; b++) {
            String body = buf.readUtf();
            int count = buf.readInt();
            Map<Integer, ClientSatellite> sats = new HashMap<>();
            for (int i = 0; i < count; i++) {
                int freq = buf.readInt();
                int id = buf.readInt();
                Satellite satellite = SpaceSatelliteRegistry.createFromId(id);
                SatelliteOrbit orbit = new SatelliteOrbit();
                orbit.deserialize(buf);
                if (satellite instanceof ISyncedSatellite synced) synced.deserialize(buf);
                if (satellite != null) sats.put(freq, new ClientSatellite(freq, satellite, orbit));
            }
            byBody.put(body, sats);
        }
        SatelliteClientData.setClientSatsByBody(byBody);
        Map<Integer, ClientSatellite> currentSats = byBody.get(current);
        SatelliteClientData.setClientSats(currentSats != null ? currentSats : new HashMap<>());
    }

    /** Spieler hat den Server verlassen (Tests/Neustart). */
    public static void reset() {
        LAST_SENT.clear();
    }
}
