package com.space_m.machine.chem.client;

import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.machine.chem.ContainerMachineMilkReformer;
import com.space_m.machine.chem.TileEntityMachineMilkReformer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUIMilkReformer}: 176x238, Textur {@code gui/processing/gui_milk_reformer.png}, waagrechter
 * Strombalken 98|13 (bis 63 breit, Quelle 176|6), Milchtank 50|32 (5x30), Ausgabetanks 101/128/155 | 53 (3x10);
 * Tooltip-Felder wie das Original (Milch 45|38 16x29, Ausgaenge 90/117/143 | 55 21x10, Strom 98|8 70x20). Kein Titel.
 * Original-Klasse: GUIMilkReformer
 */
public class GUIMilkReformer extends GuiInfoScreen<ContainerMachineMilkReformer> {

    private static final ResourceLocation TEXTURE = SpaceMod.id("textures/gui/processing/gui_milk_reformer.png");

    private final TileEntityMachineMilkReformer refinery;

    public GUIMilkReformer(ContainerMachineMilkReformer menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.refinery = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    private void tankInfo(GuiGraphics g, int i, int mouseX, int mouseY, int x, int y, int w, int h) {
        if (isPointInRect(x, y, w, h, mouseX, mouseY)) {
            refinery.tanks[i].renderTankInfo(g, font, mouseX, mouseY, leftPos + x, topPos + y, w, h);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        tankInfo(g, 0, mouseX, mouseY, 45, 90 - 52, 16, 29);
        tankInfo(g, 1, mouseX, mouseY, 90, 70 - 15, 21, 10);
        tankInfo(g, 2, mouseX, mouseY, 117, 70 - 15, 21, 10);
        tankInfo(g, 3, mouseX, mouseY, 143, 70 - 15, 21, 10);
        drawElectricityInfo(g, mouseX, mouseY, 98, 60 - 52, 70, 20, refinery.getEnergyStored(), TileEntityMachineMilkReformer.maxPower);

        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float f, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int j = (int) (refinery.getEnergyStored() * 63 / TileEntityMachineMilkReformer.maxPower);
        if (j > 0) g.blit(TEXTURE, leftPos + 98, topPos + 13, 176, 6, j, 6);

        // Original renderTank(x, yUnten, z, w, h)
        refinery.tanks[0].renderTank(g, leftPos + 50, topPos + 62 - 30, 5, 30);
        refinery.tanks[1].renderTank(g, leftPos + 101, topPos + 63 - 10, 3, 10);
        refinery.tanks[2].renderTank(g, leftPos + 128, topPos + 63 - 10, 3, 10);
        refinery.tanks[3].renderTank(g, leftPos + 155, topPos + 63 - 10, 3, 10);
    }
}
