package com.space_m.rocket.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ausblendbarer Slot, 1:1 NTM Space {@code SlotLayer}: gehoert zu einer "Ebene" (z.B. Raketenstufe) und ist nur sichtbar und
 * belegbar, wenn diese Ebene gewaehlt ist. Abweichung: statt die Y-Position auf 9999 zu setzen ({@code Slot.y} ist ab 1.17
 * final) liefert {@link #isActive()} false - Vanilla zeichnet und bedient inaktive Slots nicht.
 * Original-Klasse: SlotLayer
 */
public class SlotLayer extends Slot {

    private final int layer;
    private boolean isVisible = true;

    public SlotLayer(Container inventory, int id, int x, int y, int layer) {
        super(inventory, id, x, y);
        this.layer = layer;

        if (layer != 0) hide();
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    public void setLayer(int layer) {
        if (this.layer == layer) {
            show();
        } else {
            hide();
        }
    }

    private void hide() {
        isVisible = false;
    }

    private void show() {
        isVisible = true;
    }

    @Override
    public boolean isActive() {
        return isVisible;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isVisible;
    }
}
