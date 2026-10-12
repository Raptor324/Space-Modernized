package com.space_m.satellite.client;

import java.util.List;

import com.space_m.client.sky.SkyHooks;
import com.space_m.network.SpacePacketHandler;
import com.space_m.satellite.SatelliteClientData;
import com.space_m.satellite.SatelliteEvents;
import com.space_m.satellite.SatelliteItemLogic;
import com.space_m.satellite.SatelliteSyncPacket;
import com.space_m.satellite.SatelliteWar;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * R6-C Client: Satelliten-Sync, Satelliten am Himmel ({@link SatelliteSkyHooks}), Einstellungs-GUI, Space-Tooltip der
 * HBM-Satelliten-Nutzlasten, Kriegssatelliten-Geraeusch.
 */
public final class SatelliteClient {

    private SatelliteClient() {}

    /** Aus {@code SpaceClient.init()}. */
    public static void init() {
        SpacePacketHandler.registerS2C(SpacePacketHandler.SATELLITES, SatelliteSyncPacket::decode, SatelliteSyncPacket::handle);

        SkyHooks.set(new SatelliteSkyHooks(SkyHooks.get()));

        SatelliteItemLogic.openSettings = () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.setScreen(new GUIScreenSatSettings(mc.player));
        };
        SatelliteItemLogic.shiftDown = Screen::hasShiftDown;
        SatelliteItemLogic.clientLevel = () -> Minecraft.getInstance().level;

        // Original playsound(): thePlayer.playSound("hbm:misc.fireflash", 10F, 1F)
        SatelliteWar.clientFireSound = () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.playSound(com.space_m.sound.SpaceSounds.MISC_FIREFLASH.get(), 10F, 1F);
        };

        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register((stack, lines, flag) -> addTooltip(stack, lines));
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> addTooltip(stack, lines));
        *///?}

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> SatelliteClientData.clear());
    }

    /**
     * ItemSatellite-Tooltip fuer die HBM-Nutzlasten: HBM zeigt nur noch die Frequenzzeile (W-R6C-4,
     * {@code SpaceSatelliteHooks.showHbmChipDescription}), Bahndaten + {@code item.sat.desc.*} kommen von hier.
     */
    private static void addTooltip(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty() || !SatelliteEvents.isHbmPayloadSatellite(stack.getItem())) return;
        SatelliteItemLogic.appendTooltip(stack, Minecraft.getInstance().level, lines, true);
    }
}
