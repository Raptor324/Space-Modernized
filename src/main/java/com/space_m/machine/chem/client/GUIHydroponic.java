package com.space_m.machine.chem.client;

import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.machine.chem.ContainerHydroponic;
import com.space_m.machine.chem.TileEntityHydroponic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code GUIHydroponic}: 176x186, Textur {@code gui/machine/gui_hydrobay.png}, CO2/O2-Tanks 13/31 | 18
 * (16x52), Strombalken 147|18 (16x34, Quelle 176|34-p), Duengerbalken 102|37 (6x32, Quelle 192|32-f), Lampe 151|4
 * (Quelle 176|34, ab 200 HE). Ueber dem leeren Duengerplatz wechselt jede Sekunde die Anzeige der gueltigen Duenger
 * ({@code drawStackText}: Gegenstaende + Name). Titel bei x 97 zentriert.
 * Original-Klasse: GUIHydroponic
 */
public class GUIHydroponic extends GuiInfoScreen<ContainerHydroponic> {

    private static final ResourceLocation TEXTURE = SpaceMod.id("textures/gui/machine/gui_hydrobay.png");

    private final TileEntityHydroponic hydro;

    public GUIHydroponic(ContainerHydroponic menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.hydro = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        if (isPointInRect(13, 70 - 52, 16, 52, mouseX, mouseY)) hydro.tanks[0].renderTankInfo(g, font, mouseX, mouseY, leftPos + 13, topPos + 18, 16, 52);
        if (isPointInRect(31, 70 - 52, 16, 52, mouseX, mouseY)) hydro.tanks[1].renderTankInfo(g, font, mouseX, mouseY, leftPos + 31, topPos + 18, 16, 52);
        drawElectricityInfo(g, mouseX, mouseY, 147, 52 - 34, 16, 34, hydro.getEnergyStored(), TileEntityHydroponic.maxPower);

        Slot fert = menu.getSlot(1);
        if (menu.getCarried().isEmpty() && !fert.hasItem() && isPointInRect(fert.x, fert.y, 16, 16, mouseX, mouseY)) {
            ItemStack[] validItems = hydro.getValidFertilizers();
            int cycle = (int) ((System.currentTimeMillis() % (1000L * validItems.length)) / 1000);
            ItemStack selected = validItems[cycle];

            // drawStackText: Zeile mit allen gueltigen Gegenstaenden, darunter der Name des aktuellen
            int x = mouseX + 12, y = mouseY - 12;
            int w = Math.max(validItems.length * 18, font.width(selected.getHoverName()) + 4);
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.fill(x - 3, y - 3, x + w + 3, y + 30, 0xF0100010);
            for (int i = 0; i < validItems.length; i++) g.renderItem(validItems[i], x + i * 18, y);
            g.drawString(font, selected.getHoverName(), x, y + 19, 0xFFFFFF);
            g.pose().popPose();
        } else {
            renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 97 - font.width(title) / 2, 5, 4210752, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float f, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int power = (int) (hydro.getEnergyStored() * 34 / TileEntityHydroponic.maxPower);
        if (power > 0) g.blit(TEXTURE, leftPos + 147, topPos + 52 - power, imageWidth, 34 - power, 16, power);

        int fertilizer = hydro.fertilizer * 32 / TileEntityHydroponic.maxFertilizer;
        if (fertilizer > 0) g.blit(TEXTURE, leftPos + 102, topPos + 69 - fertilizer, imageWidth + 16, 32 - fertilizer, 6, fertilizer);

        if (hydro.getEnergyStored() >= 200) {
            g.blit(TEXTURE, leftPos + 151, topPos + 4, imageWidth, 34, 9, 12);
        }

        hydro.tanks[0].renderTank(g, leftPos + 13, topPos + 70 - 52, 16, 52);
        hydro.tanks[1].renderTank(g, leftPos + 31, topPos + 70 - 52, 16, 52);
    }
}
