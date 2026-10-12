package com.space_m.machine.chem.client;

import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.machine.chem.ContainerVacuumCircuit;
import com.space_m.machine.chem.TileEntityMachineVacuumCircuit;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUIVacuumCircuit}: 176x204, Textur {@code gui/processing/gui_vac_cir_station.png}, Strombalken
 * 143|18 (16x52, Quelle 176|52-p), Fortschritt 63|28 (bis 33 breit, Quelle 192|0), Lampe 146|4 (Quelle 176|52,
 * wenn Strom &gt;= Verbrauch), Aufwertungs-Infofeld 78|76 (kleiner blauer Stern), Titel 18 links der Mitte (dunkel).
 * Original-Klasse: GUIVacuumCircuit
 */
public class GUIVacuumCircuit extends GuiInfoScreen<ContainerVacuumCircuit> {

    private static final ResourceLocation TEXTURE = SpaceMod.id("textures/gui/processing/gui_vac_cir_station.png");

    private final TileEntityMachineVacuumCircuit sucker;

    public GUIVacuumCircuit(ContainerVacuumCircuit menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.sucker = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float interp) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, interp);
        super.render(g, mouseX, mouseY, interp);

        drawElectricityInfo(g, mouseX, mouseY, 143, 18, 16, 52, sucker.getPower(), sucker.getMaxEnergyStored());
        drawCustomInfoStat(g, mouseX, mouseY, 78, 76, 8, 8, mouseX, mouseY, getUpgradeInfo(sucker));

        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2 - 18, 6, 4210752, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int p = (int) (sucker.getEnergyStored() * 52 / Math.max(sucker.getMaxEnergyStored(), 1));
        if (p > 0) g.blit(TEXTURE, leftPos + 143, topPos + 70 - p, 176, 52 - p, 16, p);

        int i = sucker.progress * 33 / Math.max(sucker.processTime, 1);
        if (i > 0) g.blit(TEXTURE, leftPos + 63, topPos + 28, 192, 0, i, 14);

        if (sucker.getEnergyStored() >= sucker.consumption) {
            g.blit(TEXTURE, leftPos + 146, topPos + 4, 176, 52, 9, 12);
        }

        drawInfoPanel(g, 78, 76, PanelType.SMALL_BLUE_STAR);
    }
}
