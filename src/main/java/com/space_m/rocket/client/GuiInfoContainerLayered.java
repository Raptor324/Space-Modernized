package com.space_m.rocket.client;

import com.space_m.network.SpacePacketHandler;
import com.space_m.rocket.menu.GuiLayerPacket;
import com.space_m.rocket.menu.SlotLayer;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * GUI mit umschaltbaren Slot-Ebenen, 1:1 NTM Space {@code GuiInfoContainerLayered}: {@link #setLayer} schaltet alle
 * {@link SlotLayer} um und meldet die Ebene dem Server ({@link GuiLayerPacket}).
 * Original-Klasse: GuiInfoContainerLayered
 */
public abstract class GuiInfoContainerLayered<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected int currentLayer = 0;

    public GuiInfoContainerLayered(T container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    public void setLayer(int layer) {
        currentLayer = layer;
        for (Slot o : menu.slots) {
            if (!(o instanceof SlotLayer slot)) continue;

            slot.setLayer(layer);
        }

        SpacePacketHandler.sendToServer(GuiLayerPacket.ID, new GuiLayerPacket(layer));
    }

    public int getLayer() {
        return currentLayer;
    }

    /** Original {@code GuiInfoContainer.checkClick}. */
    protected boolean checkClick(double x, double y, int left, int top, int sizeX, int sizeY) {
        return leftPos + left <= x && leftPos + left + sizeX > x && topPos + top < y && topPos + top + sizeY >= y;
    }
}
