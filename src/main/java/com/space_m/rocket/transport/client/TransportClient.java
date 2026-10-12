package com.space_m.rocket.transport.client;

import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.entity.SpaceEntities;
import com.space_m.inventory.SpaceMenus;
import com.space_m.network.SpacePacketHandler;
import com.space_m.rocket.transport.ItemTransporterLinker;
import com.space_m.rocket.transport.TransporterLinkerPacket;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.Minecraft;

/**
 * Client-Anmeldung R5-C (Original {@code ClientProxy}: TESR Transporter/Kampfkapsel, Entity-Renderer Kapsel,
 * {@code TransporterLinkerPacket}-Empfang, GUIs).
 */
public final class TransportClient {
    private TransportClient() {}

    /** Aus {@code SpaceClient.init()} (Mod-Konstruktor). */
    public static void init() {
        SpacePacketHandler.registerS2C(SpacePacketHandler.TRANSPORTER_LINKER, TransporterLinkerPacket::decode, TransporterLinkerPacket::handle);
        EntityRendererRegistry.register(SpaceEntities.COMBAT_POD, CombatDropPodRenderer::new);
        ItemTransporterLinker.openGui = info -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.setScreen(new TransporterLinkerScreen(mc.player, ItemTransporterLinker.currentTransporters, info));
        };
    }

    /** Aus {@code SpaceClient.setup()} (CLIENT_SETUP). */
    public static void setup() {
        MenuRegistry.registerScreenFactory(SpaceMenus.TRANSPORTER_ROCKET.get(), TransporterRocketScreen::new);
        BlockEntityRendererRegistry.register(SpaceBlockEntities.TRANSPORTER_ROCKET.get(), TransporterRocketRenderer::new);
        BlockEntityRendererRegistry.register(SpaceBlockEntities.COMBAT_DROP_POD.get(), CombatPodRenderer::new);
    }
}
