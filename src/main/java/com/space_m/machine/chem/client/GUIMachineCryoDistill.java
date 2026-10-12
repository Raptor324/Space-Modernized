package com.space_m.machine.chem.client;

import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.machine.chem.ContainerMachineCryoDistill;
import com.space_m.machine.chem.TileEntityMachineCryoDistill;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUIMachineCryoDistill}: 176x238, Textur {@code gui/processing/gui_cryodistill.png}, Strombalken
 * (26|18, 16x52, Fuellhoehe auf 54 skaliert, Quelle 176|52-j), Tanks 44/80/98/116/134 | 18 (16x52), Titel weiss mittig.
 * Original-Klasse: GUIMachineCryoDistill
 */
public class GUIMachineCryoDistill extends GuiInfoScreen<ContainerMachineCryoDistill> {

    private static final ResourceLocation TEXTURE = SpaceMod.id("textures/gui/processing/gui_cryodistill.png");
    private static final int[] TANK_X = { 44, 80, 98, 116, 134 };

    private final TileEntityMachineCryoDistill cryo;

    public GUIMachineCryoDistill(ContainerMachineCryoDistill menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.cryo = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        for (int i = 0; i < 5; i++) {
            if (isPointInRect(TANK_X[i], 70 - 52, 16, 52, mouseX, mouseY)) {
                cryo.tanks[i].renderTankInfo(g, font, mouseX, mouseY, leftPos + TANK_X[i], topPos + 70 - 52, 16, 52);
            }
        }
        drawElectricityInfo(g, mouseX, mouseY, 26, 70 - 52, 16, 52, cryo.getEnergyStored(), TileEntityMachineCryoDistill.maxPower);

        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 5, 0xffffff, false);
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int j = (int) (cryo.getEnergyStored() * 54 / TileEntityMachineCryoDistill.maxPower);
        if (j > 0) g.blit(TEXTURE, leftPos + 26, topPos + 70 - j, 176, 52 - j, 16, j);

        for (int i = 0; i < 5; i++) {
            cryo.tanks[i].renderTank(g, leftPos + TANK_X[i], topPos + 70 - 52, 16, 52);
        }
    }
}
