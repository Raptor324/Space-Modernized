package com.space_m.rocket.menu;

import com.space_m.network.SpacePacket;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;

/**
 * C2S: gewaehlte Ebene einer Mehrebenen-GUI, 1:1 NTM Space {@code GuiLayerPacket} - setzt die Ebene aller
 * {@link SlotLayer} im offenen Menue des Spielers.
 * Original-Klasse: GuiLayerPacket
 */
public class GuiLayerPacket implements SpacePacket {

    public static final ResourceLocation ID = SpacePacketHandler.c2s("gui_layer");

    private final int layer;

    public GuiLayerPacket(int layer) {
        this.layer = layer;
    }

    public static GuiLayerPacket decode(FriendlyByteBuf buf) {
        return new GuiLayerPacket(buf.readByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(layer);
    }

    public static void handle(GuiLayerPacket message, PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
            if (player.containerMenu != null) {
                for (Slot slot : player.containerMenu.slots) {
                    if (slot instanceof SlotLayer layered) {
                        layered.setLayer(message.layer);
                    }
                }
            }
        });
    }

    /** Aus {@code SpacePacketHandler.register()} (C2S-Abschnitt). */
    public static void register() {
        SpacePacketHandler.registerC2S(ID, GuiLayerPacket::decode, GuiLayerPacket::handle);
    }
}
