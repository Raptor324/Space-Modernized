package com.space_m.bomb.client;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.hbm_m.network.NBTControlPacket;
import com.space_m.SpaceMod;
import com.space_m.bomb.ContainerNukeAntimatter;
import com.space_m.bomb.TileEntityAntimatter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUINukeAntimatter} (176x222, {@code gui/weapon/BlombSchematic.png}): Startfeld (80,85) solange nicht
 * scharf (scharf: Overlay 176/26), Timer-Eingabe in Sekunden (80,108; 1-999, rot), Restzeit in 0.6-facher Groesse bei
 * (95, 66) - nur mit Antimaterie-Kern.
 * Original-Klasse: GUINukeAntimatter
 */
public class GUINukeAntimatter extends GuiInfoScreen<ContainerNukeAntimatter> {

    private static final ResourceLocation TEXTURE = SpaceMod.id("textures/gui/weapon/blombschematic.png");

    private final TileEntityAntimatter bomb;
    private EditBox timer;

    public GUINukeAntimatter(ContainerNukeAntimatter menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.bomb = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.timer = new EditBox(this.font, this.leftPos + 80, this.topPos + 108, 29, 12, Component.empty());
        this.timer.setTextColor(0xff0000);
        this.timer.setTextColorUneditable(0x800000);
        this.timer.setBordered(false);
        this.timer.setMaxLength(3);
        this.timer.setValue(String.valueOf(bomb != null ? bomb.timer / 20 : 0));
        this.timer.setResponder(text -> {
            if (bomb == null) return;
            try {
                int j = Mth.clamp((int) Double.parseDouble(text), 1, 999);
                send(j, 1);
            } catch (NumberFormatException ignored) { }
        });
        addRenderableWidget(this.timer);
    }

    private void send(int value, int meta) {
        CompoundTag data = new CompoundTag();
        data.putInt("value", value);
        data.putInt("meta", meta);
        NBTControlPacket.sendToServer(bomb.getBlockPos(), data);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean result = super.mouseClicked(x, y, button);

        if (bomb != null && !bomb.started) {
            if (leftPos + 80 <= x && leftPos + 80 + 18 > x && topPos + 85 < y && topPos + 85 + 18 >= y) {
                send(0, 0);
            }
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.timer != null && this.timer.isFocused() && keyCode != 256) {
            this.timer.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        if (bomb != null && bomb.started)
            g.blit(TEXTURE, leftPos + 80, topPos + 85, 176, 26, 18, 18);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

        if (bomb != null && bomb.hasAmatCore()) {
            String timer = bomb.getMinutes() + ":" + bomb.getSeconds();
            float scale = 0.60F;
            g.pose().pushPose();
            g.pose().scale(scale, scale, scale);
            g.drawString(this.font, timer, (int) ((95 - this.font.width(timer) / 2) * (1 / scale)), (int) (66 * (1 / scale)), 0xff0000, false);
            g.pose().popPose();
        }
    }
}
