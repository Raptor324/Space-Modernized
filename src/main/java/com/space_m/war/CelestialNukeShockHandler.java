package com.space_m.war;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.NuclearDetonation;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.space_m.celestial.CelestialBody;
import com.space_m.network.SpacePacket;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code com.hbm.handler.CelestialNukeShockHandler}: jede Atomexplosion hinterlaesst fuer 220 Ticks
 * einen Lichtblitz/eine Schockwelle auf der Planetenscheibe des getroffenen Koerpers (Himmel, Stardar,
 * Satelliten-GUI; hoechstens 4 gleichzeitig).
 *
 * <p>Ausloeser wie im Original: Mini-Nukes ueber den HBM-Detonationslistener ({@link SpaceApi#onNuclearDetonation},
 * HBM ruft ihn in {@code ExplosionNukeSmall} = Original {@code if(params.miniNuke) trigger(...)}), MK5-Explosionen
 * ueber das Entity-Join-Ereignis ({@code EntityJoinWorldEvent}, {@code ticksExisted <= 1}, Radius = {@code length}).
 *
 * <p>Abweichungen (technisch): Schluessel ist der Koerpername statt der int-Dimension (Koerper ohne Dimension,
 * z.B. Gasriesen, kollidieren so nicht mit Kerbin = 0); der Sync geht als eigenes Paket bei jeder Aenderung statt
 * jeden Tick im {@code PermaSyncHandler} - der Client bekommt denselben Stand.
 */
public final class CelestialNukeShockHandler {

    private static final long MAX_AGE = 220L;
    public static final int MAX_ACTIVE_SHOCKS = 4;
    private static final float FLASH_GRID = 16.0F;

    /** Original {@code CelestialNukeShockHandler.ShockStatus}. */
    public record ShockStatus(long time, float centerX, float centerY, float strength) {}

    private static final Map<String, List<ShockStatus>> serverStatuses = new LinkedHashMap<>();
    private static final Map<String, List<ShockStatus>> clientStatuses = new LinkedHashMap<>();

    private static boolean registered;

    private CelestialNukeShockHandler() {}

    public static void init() {
        if (registered) return;
        registered = true;

        // Mini-Nukes (HBM ExplosionNukeSmall); MK5 meldet HBM bewusst nicht -> Entity-Join wie im Original
        SpaceApi.onNuclearDetonation(CelestialNukeShockHandler::onDetonation);
        EntityEvent.ADD.register(CelestialNukeShockHandler::onEntityJoinWorld);
        TickEvent.SERVER_PRE.register(CelestialNukeShockHandler::onServerTick);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(CelestialNukeShockHandler::onWorldUnload);
        LifecycleEvent.SERVER_STOPPED.register(server -> serverStatuses.clear());
        PlayerEvent.PLAYER_JOIN.register(player -> SpacePacketHandler.sendToPlayer(player, SpacePacketHandler.NUKE_SHOCKS, new Packet(snapshot(serverStatuses))));
    }

    private static void onDetonation(NuclearDetonation d) {
        if (d.kind() == NuclearDetonation.Kind.MINI_NUKE) {
            trigger(d.level(), d.x(), d.z(), d.blastRadius());
        }
    }

    private static EventResult onEntityJoinWorld(Entity entity, Level level) {
        if (level == null || level.isClientSide || !(entity instanceof EntityNukeExplosionMK5 explosion)) {
            return EventResult.pass();
        }

        if (explosion.tickCount > 1) {
            return EventResult.pass();
        }

        trigger(level, explosion.getX(), explosion.getZ(), explosion.length);
        return EventResult.pass();
    }

    private static void onServerTick(MinecraftServer server) {
        if (serverStatuses.isEmpty()) {
            return;
        }

        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }

        if (cleanup(serverStatuses, overworld.getGameTime())) {
            broadcast(server);
        }
    }

    private static void onWorldUnload(ServerLevel level) {
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body != null) serverStatuses.remove(body.name);
    }

    public static void trigger(Level world, double x, double z, float blastRadius) {
        if (world == null || world.isClientSide) {
            return;
        }

        CelestialBody.Target target = CelestialBody.getTarget(world, (int) Math.floor(x), (int) Math.floor(z));
        CelestialBody body = target != null ? target.body : null;
        if (body == null) {
            return;
        }

        long time = world.getGameTime();
        float rawX = 0.18F + hashToUnit(x * 0.03125D + z * 0.0125D + time * 0.017D) * 0.64F;
        float rawY = 0.22F + hashToUnit(x * -0.015D + z * 0.0275D + time * 0.011D + 19.0D) * 0.52F;
        float centerX = quantizeCenter(rawX);
        float centerY = quantizeCenter(rawY);
        float strength = Mth.clamp(blastRadius / 140.0F, 0.35F, 1.0F);

        List<ShockStatus> statuses = serverStatuses.computeIfAbsent(body.name, k -> new ArrayList<>());

        pruneStatuses(statuses, time);
        statuses.add(new ShockStatus(time, centerX, centerY, strength));
        while (statuses.size() > MAX_ACTIVE_SHOCKS) {
            statuses.remove(0);
        }

        if (world.getServer() != null) broadcast(world.getServer());
    }

    /** Server: aktive Erschuetterungen eines Koerpers (Tests, Stardar-Server-Seite). */
    public static List<ShockStatus> getServerShocks(@Nullable CelestialBody body) {
        if (body == null) return Collections.emptyList();
        List<ShockStatus> statuses = serverStatuses.get(body.name);
        return statuses != null ? Collections.unmodifiableList(statuses) : Collections.emptyList();
    }

    /** Original {@code getClientShocks(body)}. */
    public static List<ShockStatus> getClientShocks(@Nullable CelestialBody body) {
        if (body == null) {
            return Collections.emptyList();
        }

        List<ShockStatus> statuses = clientStatuses.get(body.name);
        return statuses != null ? statuses : Collections.emptyList();
    }

    /** Client: beim Verlassen der Welt (Original {@code onWorldUnload} mit {@code isRemote}). */
    public static void clearClient() {
        clientStatuses.clear();
    }

    private static void broadcast(MinecraftServer server) {
        SpacePacketHandler.sendToAll(server, SpacePacketHandler.NUKE_SHOCKS, new Packet(snapshot(serverStatuses)));
    }

    private static Map<String, List<ShockStatus>> snapshot(Map<String, List<ShockStatus>> map) {
        Map<String, List<ShockStatus>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<ShockStatus>> e : map.entrySet()) copy.put(e.getKey(), List.copyOf(e.getValue()));
        return copy;
    }

    /** @return true, wenn etwas entfernt wurde. */
    private static boolean cleanup(Map<String, List<ShockStatus>> statuses, long currentTime) {
        boolean changed = false;
        Iterator<Map.Entry<String, List<ShockStatus>>> iterator = statuses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<ShockStatus>> entry = iterator.next();
            changed |= pruneStatuses(entry.getValue(), currentTime);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    private static boolean pruneStatuses(List<ShockStatus> statuses, long currentTime) {
        return statuses.removeIf(status -> currentTime - status.time() > MAX_AGE);
    }

    private static float hashToUnit(double value) {
        return (float) (value - Math.floor(value));
    }

    private static float quantizeCenter(float value) {
        float clamped = Mth.clamp(value, 0.05F, 0.95F);
        return (Mth.floor(clamped * FLASH_GRID) + 0.5F) / FLASH_GRID;
    }

    /** Original {@code writeSync}/{@code readSync}. */
    public static final class Packet implements SpacePacket {
        private final Map<String, List<ShockStatus>> statuses;

        public Packet(Map<String, List<ShockStatus>> statuses) {
            this.statuses = statuses;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeShort(statuses.size());
            for (Map.Entry<String, List<ShockStatus>> entry : statuses.entrySet()) {
                List<ShockStatus> list = entry.getValue();
                buf.writeUtf(entry.getKey());
                buf.writeByte(list.size());
                for (ShockStatus status : list) {
                    buf.writeLong(status.time());
                    buf.writeFloat(status.centerX());
                    buf.writeFloat(status.centerY());
                    buf.writeFloat(status.strength());
                }
            }
        }

        public static Packet decode(FriendlyByteBuf buf) {
            Map<String, List<ShockStatus>> map = new LinkedHashMap<>();
            int count = buf.readShort();
            for (int i = 0; i < count; i++) {
                String body = buf.readUtf();
                int shockCount = buf.readUnsignedByte();
                List<ShockStatus> list = new ArrayList<>(shockCount);
                for (int j = 0; j < shockCount; j++) {
                    long time = buf.readLong();
                    float centerX = buf.readFloat();
                    float centerY = buf.readFloat();
                    float strength = buf.readFloat();
                    list.add(new ShockStatus(time, centerX, centerY, strength));
                }
                map.put(body, list);
            }
            return new Packet(map);
        }

        /** Client-Thread. */
        public void apply() {
            clientStatuses.clear();
            clientStatuses.putAll(statuses);
        }
    }
}
