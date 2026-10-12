package com.space_m.machine.mining.client;

import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.hbm_m.network.NBTControlPacket;
import com.space_m.SpaceMod;
import com.space_m.machine.mining.ContainerDriveProcessor;
import com.space_m.machine.mining.TileEntityMachineDriveProcessor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUIMachineDriveProcessor} (176x207, {@code gui_drive_processor.png}): Strombalken (134,18 16x52),
 * Klonknopf (49,17 18x18 -> {@code clone}), Verarbeitungsknopf (38,61 18x18 -> {@code process = !isProcessing},
 * leuchtet waehrend der Verarbeitung), Fortschrittsbalken (59,63 bis 44 px), Statuszeile (41,86, weiss, mit den
 * Farbcodes der Maschine). Keine Titel/Inventar-Beschriftung (Original-Vordergrund leer).
 * Original-Klasse: GUIMachineDriveProcessor
 */
public class GUIMachineDriveProcessor extends GuiInfoScreen<ContainerDriveProcessor> {

    private static final ResourceLocation texture = SpaceMod.id("textures/gui/processing/gui_drive_processor.png");

    public GUIMachineDriveProcessor(ContainerDriveProcessor menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);

        this.imageWidth = 176;
        this.imageHeight = 207;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, gui, mouseX, mouseY, f);
        super.render(gui, mouseX, mouseY, f);

        TileEntityMachineDriveProcessor machine = menu.machine;
        if (machine != null) {
            this.drawElectricityInfo(gui, mouseX, mouseY, 134, 18, 16, 52, machine.getEnergyStored(), machine.maxPower);
        }

        this.drawCustomInfoStat(gui, mouseX, mouseY, 49, 17, 18, 18, mouseX, mouseY, Component.literal("Clone drive"));
        this.drawCustomInfoStat(gui, mouseX, mouseY, 38, 61, 18, 18, mouseX, mouseY, Component.literal("Start drive processing"));

        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float interp, int mX, int mY) {
        int guiLeft = leftPos, guiTop = topPos, xSize = imageWidth;
        gui.blit(texture, guiLeft, guiTop, 0, 0, imageWidth, imageHeight);

        TileEntityMachineDriveProcessor machine = menu.machine;
        if (machine == null) return;

        if (machine.isProcessing) {
            gui.blit(texture, guiLeft + 38, guiTop + 61, 192, 28, 18, 18);
        }

        int p = (int) (machine.getEnergyStored() * 52 / machine.maxPower);
        gui.blit(texture, guiLeft + 134, guiTop + 18 + 52 - p, xSize, 52 - p, 16, p);

        if (machine.isProcessing) {
            p = (int) (machine.progress * 44 / machine.maxProgress);
            gui.blit(texture, guiLeft + 59, guiTop + 63, 192, 14, p, 14);
        }

        gui.drawString(font, Component.literal(machine.status), guiLeft + 41, guiTop + 86, 0xFFFFFF, false);
    }

    /** Original {@code drawGuiContainerForegroundLayer}: leer. */
    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean handled = super.mouseClicked(x, y, i);

        TileEntityMachineDriveProcessor machine = menu.machine;
        if (machine == null) return handled;

        // Process drive
        if (isPointInRect(38, 61, 18, 18, (int) x, (int) y)) {
            playClickSound();

            CompoundTag data = new CompoundTag();
            data.putBoolean("process", !machine.isProcessing);

            NBTControlPacket.sendToServer(machine.getBlockPos(), data);
            return true;
        }

        // Clone drive
        if (isPointInRect(49, 17, 18, 18, (int) x, (int) y)) {
            playClickSound();

            CompoundTag data = new CompoundTag();
            data.putBoolean("clone", true);

            NBTControlPacket.sendToServer(machine.getBlockPos(), data);
            return true;
        }

        return handled;
    }
}
