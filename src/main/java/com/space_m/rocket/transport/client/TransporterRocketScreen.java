package com.space_m.rocket.transport.client;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.rocket.transport.TransporterRocketBlockEntity;
import com.space_m.rocket.transport.TransporterRocketMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUITransporterRocket} (230 x 236, {@code gui_transporter.png}): Namensfeld (8|12, gruen, ohne
 * Rahmen; jede Aenderung geht per {@code NBTControlPacket "name"} an den Server), Schwellen-Anzeige "x2^(n-1)" rechts
 * buendig bei 167|12, Schieberegler (Ziehen im Feld 98|120, 74 x 20, Schritt 6,78 px, 0..10, Paket "threshold"),
 * Tanks 4 Eingabe (8|70) + 4 Ausgabe (98|70) je 16 x 34, 2 Treibstoff (188|18) je 16 x 70.
 * Original-Klasse: GUITransporterRocket
 */
public class TransporterRocketScreen extends AbstractContainerScreen<TransporterRocketMenu> {

    protected static final ResourceLocation texture = SpaceMod.id("textures/gui/machine/gui_transporter.png");

    private EditBox transporterName;

    private final TransporterRocketBlockEntity transporter;

    public TransporterRocketScreen(TransporterRocketMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);
        this.transporter = menu.transporter;

        imageWidth = 230;
        imageHeight = 236;
    }

    @Override
    protected void init() {
        super.init();

        transporterName = new EditBox(this.font, leftPos + 8, topPos + 12, 122, 12, Component.empty());
        transporterName.setTextColor(0x00ff00);
        transporterName.setTextColorUneditable(0x00ff00);
        transporterName.setBordered(false);
        transporterName.setValue(transporter.getTransporterName());
        transporterName.setResponder(text -> setName());
        addWidget(transporterName);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < 4; i++) {
            int x = leftPos + 8 + i * 18;
            int y = topPos + 70;
            transporter.tanks[i].renderTank(g, x, y, 16, 34);
        }

        for (int i = 0; i < 4; i++) {
            int x = leftPos + 98 + i * 18;
            int y = topPos + 70;
            transporter.tanks[i + 4].renderTank(g, x, y, 16, 34);
        }

        for (int i = 0; i < 2; i++) {
            int x = leftPos + 188 + i * 18;
            int y = topPos + 18;
            transporter.tanks[i + 8].renderTank(g, x, y, 16, 70);
        }

        for (int i = 0; i < 4; i++) {
            int x = leftPos + 8 + i * 18;
            int y = topPos + 70;
            transporter.tanks[i].renderTankInfo(g, font, mouseX, mouseY, x, y, 16, 34);
        }

        for (int i = 0; i < 4; i++) {
            int x = leftPos + 98 + i * 18;
            int y = topPos + 70;
            transporter.tanks[i + 4].renderTankInfo(g, font, mouseX, mouseY, x, y, 16, 34);
        }

        for (int i = 0; i < 2; i++) {
            int x = leftPos + 188 + i * 18;
            int y = topPos + 18;
            transporter.tanks[i + 8].renderTankInfo(g, font, mouseX, mouseY, x, y, 16, 70);
        }

        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float f, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        g.blit(texture, leftPos + 100 + (int) (transporter.threshold * 6.78), topPos + 122, imageWidth, 0, 4, 15);

        int threshold = transporter.getThreshold();
        int width = font.width("x" + threshold);

        g.drawString(font, "x" + threshold, leftPos + 167 - width, topPos + 12, -1, true);

        transporterName.render(g, mouseX, mouseY, f);
    }

    /** Original zeichnet keine Titel. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = transporterName.mouseClicked(mouseX, mouseY, button);
        transporterName.setFocused(handled);
        return super.mouseClicked(mouseX, mouseY, button) || handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean r = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        if (isInAABB((int) mouseX, (int) mouseY, leftPos + 98, topPos + 120, 74, 20)) {
            int slidPos = (int) ((mouseX - (leftPos + 98)) / 6.78);
            slidPos = Mth.clamp(slidPos, 0, 10); // 2^0 - 2^9 | 0 - 512

            CompoundTag data = new CompoundTag();
            data.putDouble("threshold", slidPos);
            NBTControlPacket.sendToServer(transporter.getBlockPos(), data);
        }
        return r;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (transporterName.isFocused() && transporterName.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (transporterName.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            transporterName.keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void setName() {
        if (!transporterName.getValue().equals(transporter.getTransporterName()))
            transporter.setTransporterName(transporterName.getValue());
    }

    private boolean isInAABB(int mouseX, int mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }
}
