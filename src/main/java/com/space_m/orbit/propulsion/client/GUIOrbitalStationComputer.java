package com.space_m.orbit.propulsion.client;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.NBTControlPacket;
import com.space_m.SpaceMod;
import com.space_m.orbit.OrbitalStation;
import com.space_m.orbit.propulsion.ContainerOrbitalStationComputer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUIOrbitalStationComputer} (198x135, {@code gui_ship_computer.png}): Namensfeld (52,26 136x12,
 * weiss, ohne Rahmen, sofort fokussiert; jede Aenderung schickt {@code name}), Schwerkraftschalter (36,84 29x5,
 * Anzeige an = Textur bei (xSize, 0); Klick schickt {@code gravity}). Keine Beschriftungen.
 * Original-Klasse: GUIOrbitalStationComputer
 */
public class GUIOrbitalStationComputer extends AbstractContainerScreen<ContainerOrbitalStationComputer> {

    private static final ResourceLocation texture = SpaceMod.id("textures/gui/machine/gui_ship_computer.png");

    private EditBox stationName;

    public GUIOrbitalStationComputer(ContainerOrbitalStationComputer menu, Inventory inv, Component title) {
        super(menu, inv, title);

        imageWidth = 198;
        imageHeight = 135;
    }

    @Override
    protected void init() {
        super.init();

        stationName = new EditBox(this.font, leftPos + 52, topPos + 26, 136, 12, Component.empty());
        stationName.setTextColor(0xffffff);
        stationName.setTextColorUneditable(0xdddddd);
        stationName.setBordered(false);
        stationName.setMaxLength(32);
        stationName.setValue(OrbitalStation.clientStation().name);
        stationName.setResponder(this::setName);
        addWidget(stationName);
        setInitialFocus(stationName);
        stationName.setFocused(true);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, delta);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (OrbitalStation.clientStation().gravityMultiplier > 0) {
            g.blit(texture, leftPos + 36, topPos + 84, imageWidth, 0, 29, 5);
        }

        stationName.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    private boolean checkClick(double x, double y, int left, int top, int sizeX, int sizeY) {
        return leftPos + left <= x && leftPos + left + sizeX > x && topPos + top < y && topPos + top + sizeY >= y;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean ret = super.mouseClicked(x, y, button);

        // Toggle gravity
        if (checkClick(x, y, 36, 84, 29, 5)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            setGravity(OrbitalStation.clientStation().gravityMultiplier == 0);
            return true;
        }

        return ret;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (stationName.isFocused() && stationName.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (stationName.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            stationName.keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void setName(String name) {
        if (menu.computer == null) return;
        CompoundTag data = new CompoundTag();
        data.putString("name", name);
        NBTControlPacket.sendToServer(menu.computer.getBlockPos(), data);
    }

    private void setGravity(boolean enabled) {
        if (menu.computer == null) return;
        CompoundTag data = new CompoundTag();
        data.putBoolean("gravity", enabled);
        NBTControlPacket.sendToServer(menu.computer.getBlockPos(), data);
    }
}
