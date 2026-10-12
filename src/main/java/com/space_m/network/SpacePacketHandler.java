package com.space_m.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.space_m.SpaceMod;
import com.space_m.platform.SpacePlatform;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Netzwerk des Addons ueber Architectury {@link NetworkManager} - Muster {@code com.hbm_m.network.ModPacketHandler}.
 *
 * <p>Anmeldung:
 * <ul>
 *   <li>C2S: in {@link #register()} ({@link #registerC2S}).</li>
 *   <li>S2C: ID in {@link #S2C_IDS} eintragen ({@link #s2c(String)}); Empfaenger meldet {@code SpaceClient.init()}
 *       ueber {@link #registerS2C} an (Client-Klassen bleiben vom Server fern). Auf 1.21.1-Servern wird nur der
 *       Payload-Typ angemeldet ({@link #register()}), sonst verwirft NeoForge die Sendung.</li>
 * </ul>
 * Alles laeuft im Mod-Konstruktor (NeoForge: vor {@code RegisterPayloadHandlersEvent}).
 *
 * <p>Original-Pakete von NTM Space: {@code GuiLayerPacket}, {@code TransporterLinkerPacket}, {@code EntityBufPacket}.
 */
public final class SpacePacketHandler {
    private static boolean registered;

    /** Alle S2C-IDs (fuer die reine Typanmeldung auf dem dedizierten 1.21.1-Server). */
    private static final List<ResourceLocation> S2C_IDS = new ArrayList<>();

    // ── IDs (je Runde/Agent ein eigener, kommentierter Abschnitt) ──────────────────────────────

    // R1-A Himmelskoerper: Trait-Sync (Original PermaSyncHandler, Abschnitt CBT)
    public static final ResourceLocation CELESTIAL_TRAITS = s2c("celestial_traits");

    // R2-A Atmosphaere: Luftvorrat/Schwerkraft-Flag des Spielers (Original ExtPropPacket, Teil HbmLivingProps.oxygen)
    public static final ResourceLocation ATMOSPHERE_OXY = s2c("atmosphere_oxy");

    // R3-2 Duna: Staubsturmstaerke (Original WorldProviderDuna.serialize ueber PermaSyncHandler)
    public static final ResourceLocation DUNA_DUST_STORM = s2c("duna_dust_storm");

    // R5-C Transporter-Linker: gespeicherte Rampen an den Client (Original TransporterLinkerPacket)
    public static final ResourceLocation TRANSPORTER_LINKER = s2c("transporter_linker");

    // R6-C Satelliten: Satelliten je Himmelskoerper (Original PermaSyncHandler, Abschnitt SATELLITES)
    public static final ResourceLocation SATELLITES = s2c("satellites");

    // R6-A Orbit/Station: Station unter dem Spieler (Original TileEntityOrbitalStation.networkPackNT / EntityRideableRocket-Buf)
    public static final ResourceLocation ORBITAL_STATION = s2c("orbital_station");

    // R8-B Krieg: Nuklear-Erschuetterungen je Koerper (Original PermaSyncHandler / CelestialNukeShockHandler.writeSync)
    // und Kriegsprojektil-Fortschritt (Original PermaSyncHandler, Abschnitt CBT_War: flashtime/traveltime je Tick)
    public static final ResourceLocation NUKE_SHOCKS = s2c("nuke_shocks");
    public static final ResourceLocation WAR_PROJECTILES = s2c("war_projectiles");

    private SpacePacketHandler() {}

    public static ResourceLocation s2c(String path) {
        ResourceLocation id = SpaceMod.id(path);
        S2C_IDS.add(id);
        return id;
    }

    public static ResourceLocation c2s(String path) {
        return SpaceMod.id(path);
    }

    public static void register() {
        if (registered) return;
        registered = true;

        // ── C2S ──
        com.space_m.rocket.menu.GuiLayerPacket.register(); // R5-A Mehrebenen-GUI (Original GuiLayerPacket)
        com.space_m.satellite.SatSettingsPacket.register(); // R6-C Satelliten-Einstellungen (Original NBTItemControlPacket)

        //? if >= 1.21.1 {
        /*if (!SpacePlatform.isClient()) {
            for (ResourceLocation id : S2C_IDS) NetworkManager.registerS2CPayloadType(id);
        }
        *///?}
    }

    /** S2C-Empfaenger (nur aus {@code SpaceClient.init()} aufrufen). */
    public static <T> void registerS2C(ResourceLocation id, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, id, (buf, ctx) -> handler.accept(decoder.apply(buf), ctx));
    }

    public static <T> void registerC2S(ResourceLocation id, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, id, (buf, ctx) -> handler.accept(decoder.apply(buf), ctx));
    }

    // ── Senden ──

    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, SpacePacket packet) {
        //? if < 1.21.1 {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        //?} else {
        /*net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), player.server.registryAccess());
        *///?}
        packet.write(buf);
        try {
            NetworkManager.sendToPlayer(player, id, buf);
        } catch (UnsupportedOperationException e) {
            // Verbindung ohne Payload-Aushandlung (z.B. Replay-Server) - wie HBM still verwerfen.
        }
    }

    public static void sendToAll(MinecraftServer server, ResourceLocation id, SpacePacket packet) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendToPlayer(p, id, packet);
    }

    public static void sendToDimension(ServerLevel level, ResourceLocation id, SpacePacket packet) {
        for (ServerPlayer p : level.players()) sendToPlayer(p, id, packet);
    }

    public static void sendToPlayersNear(ServerLevel level, Vec3 pos, double range, ResourceLocation id, SpacePacket packet) {
        double r2 = range * range;
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(pos) <= r2) sendToPlayer(p, id, packet);
        }
    }

    /** C2S - nur clientseitig aufrufen. */
    public static void sendToServer(ResourceLocation id, SpacePacket packet) {
        //? if < 1.21.1 {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        //?} else {
        /*net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), com.space_m.client.SpaceClient.registryAccess());
        *///?}
        packet.write(buf);
        NetworkManager.sendToServer(id, buf);
    }
}
