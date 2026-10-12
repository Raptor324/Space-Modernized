package com.space_m.machine.misc.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.hbm_m.item.ISatChip;
import com.hbm_m.network.NBTControlPacket;
import com.space_m.SpaceMod;
import com.space_m.machine.misc.ContainerMachineWarController;
import com.space_m.satellite.ClientSatellite;
import com.space_m.satellite.SatelliteClientData;
import com.space_m.satellite.SatelliteWar;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code GUIWarController} (176x204, {@code gui_controlpanel.png}): Befehlskonsole. Das Eingabefeld liegt
 * unsichtbar links neben der GUI (immer fokussiert, ohne Rahmen, max. 100 Zeichen); Eingabe und Verlauf (die letzten 6
 * Zeilen, gruen, halbe Groesse) werden im Bildschirm gezeichnet ({@code "> " + text + "_"} blinkend). Befehle:
 * {@code setpos x z} (schreibt Laufwerkskoordinaten, Paket {@code xcoord}/{@code zcoord}), {@code health},
 * {@code fire} (nur Meldung, wie im Original auskommentiertes Paket), {@code getsat}; sonst "Unknown command.".
 * Stromanzeige (132,18 16x52) mit den Originalwerten 0/0.
 * Original-Klasse: GUIWarController
 */
public class GUIWarController extends GuiInfoScreen<ContainerMachineWarController> {

    private List<String> commandHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 6; // Number of lines to keep visible
    private EditBox textField;

    private static final ResourceLocation texture = SpaceMod.id("textures/gui/machine/gui_controlpanel.png");

    public GUIWarController(ContainerMachineWarController menu, Inventory inv, Component title) {
        super(menu, inv, title);

        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void init() {
        super.init();
        textField = new EditBox(this.font, leftPos - 74, topPos + 28, 100, 20, Component.empty());
        textField.setMaxLength(100);
        textField.setBordered(false);
        addWidget(textField);
        setInitialFocus(textField);
        textField.setFocused(true);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);
        renderTooltip(g, x, y);

        this.drawElectricityInfo(g, x, y, 132, 18, 16, 52, 0L, 0L);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int i, int j) {
        int yOffset = 15;
        for (int x = Math.max(0, commandHistory.size() - MAX_HISTORY); x < commandHistory.size(); x++) {
            String text = commandHistory.get(x);

            float scale = 0.5F;
            g.pose().pushPose();
            g.pose().scale(scale, scale, scale);
            g.drawString(font, text, (int) (78 / scale), (int) (yOffset / scale), 0x00FF00, false);
            g.pose().popPose();

            yOffset += 12 * scale;
        }

        String typedText = textField.getValue();
        float textScale = 0.5F;

        g.pose().pushPose();
        g.pose().scale(textScale, textScale, textScale);
        boolean blink = minecraft != null && minecraft.level != null && minecraft.level.getGameTime() % 20 < 10;
        g.drawString(font, "> " + typedText + (textField.isFocused() && blink ? "_" : ""), (int) (78 / textScale), (int) (57 / textScale), 0x00FF00, false);
        g.pose().popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            processCommand(textField.getValue().trim());
            textField.setValue(""); // Clear after processing
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyCode, scanCode, modifiers);
        if (textField.keyPressed(keyCode, scanCode, modifiers) || textField.canConsumeInput()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (textField.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }

    private void processCommand(String command) {
        if (command.isEmpty()) return;
        if (menu.shill == null) return;

        ItemStack chip = menu.shill.getInventory().getStackInSlot(2);
        int satId = ISatChip.getFreqS(chip);
        ClientSatellite client = SatelliteClientData.getClientSats().get(satId);
        Object sat = client != null ? client.satellite : null;
        String[] parts = command.split(" ");

        String cmd = parts[0].toLowerCase();
        CompoundTag data = new CompoundTag();

        switch (cmd) {

            case "setpos":
                if (parts.length < 3) {
                    addCommandHistory("> " + command);
                    addCommandHistory("Error, invalid args", ChatFormatting.RED);
                    return;
                }

                String xValueStr = parts[1];
                String zValueStr = parts[2];
                if (menu.shill.getInventory().getStackInSlot(1).isEmpty()) {
                    addCommandHistory("> " + command);
                    addCommandHistory("No drive.", ChatFormatting.RED);
                    return;
                }
                if (!xValueStr.matches("-?\\d+") || !zValueStr.matches("-?\\d+")) {
                    addCommandHistory("> " + command);
                    addCommandHistory("Invalid number format.", ChatFormatting.RED);
                    return;
                }

                int xValue = Integer.parseInt(xValueStr);
                int zValue = Integer.parseInt(zValueStr);

                addCommandHistory("> " + command);
                addCommandHistory("Set to: X=" + xValue + ", Z=" + zValue);

                data.putInt("xcoord", xValue);
                data.putInt("zcoord", zValue);

                NBTControlPacket.sendToServer(menu.shill.getBlockPos(), data);
                break;

            case "health":
                addCommandHistory("> " + command);
                addCommandHistory("Requesting " + cmd + "...");

                if (sat == null) {
                    addCommandHistory("Satellite not in orbit!", ChatFormatting.RED);
                } else {
                    addCommandHistory("health: " + (sat instanceof SatelliteWar war ? war.health : 0));
                }

                break;

            case "fire":
                if (sat == null) {
                    addCommandHistory("Satellite not in orbit!", ChatFormatting.RED);
                } else {
                    if (sat instanceof SatelliteWar) {
                        addCommandHistory("Firing!");

                        // PacketDispatcher.wrapper.sendToServer(new SatActivatePacket(satId));
                    } else {
                        addCommandHistory("Wrong satellite" + ChatFormatting.RED);
                    }
                }
                break;

            case "getsat":
                addCommandHistory("> " + command);
                if (chip.isEmpty()) {
                    addCommandHistory("No satellite chip in slot 2.", ChatFormatting.RED);
                } else {
                    addCommandHistory("Requesting Satellite ID: " + satId);

                    if (sat == null) {
                        addCommandHistory("Satellite not in orbit!", ChatFormatting.RED);
                    } else {
                        addCommandHistory("Satellite: " + sat.getClass().getSimpleName());
                    }
                }
                break;

            default:
                addCommandHistory("> " + command);
                addCommandHistory("Unknown command.", ChatFormatting.RED);
                break;
        }
    }

    private void addCommandHistory(String text) {
        addToHistory(text, null);
    }

    private void addCommandHistory(String text, ChatFormatting color) {
        addToHistory(text, color);
    }

    private void addToHistory(String text, ChatFormatting color) {
        if (color != null) {
            commandHistory.add(color + text + ChatFormatting.RESET);
        } else {
            commandHistory.add(text);
        }

        if (commandHistory.size() > MAX_HISTORY * 2) {
            commandHistory = new ArrayList<>(commandHistory.subList(commandHistory.size() - MAX_HISTORY * 2, commandHistory.size()));
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
