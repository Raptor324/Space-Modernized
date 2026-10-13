package com.space_m.fork.trinket.client;

import com.hbm_m.sound.HbmSoundsNT;
import com.space_m.fork.trinket.ForkTrinkets;
import com.space_m.fork.trinket.ForkTrinkets.ForkBobbleType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

/**
 * Space {@code GUIScreenBobble} fuer die Fork-Wackelkoepfe (wie HBM {@code TrinketScreens.Tablet}): gruene Tafel
 * 300x150 mit Titel, Name, "Has contributed" und Gravur, beim Oeffnen {@code block.bobble}.
 */
public class ForkBobbleScreen extends Screen {

    private final ForkBobbleType type;

    public static void open(int type) {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(HbmSoundsNT.get("block.bobble"), 1.0F));
        mc.setScreen(new ForkBobbleScreen(ForkTrinkets.safe(ForkBobbleType.class, type)));
    }

    private ForkBobbleScreen(ForkBobbleType type) {
        super(Component.empty());
        this.type = type;
    }

    private void centered(GuiGraphics g, String text, double left, double sizeX, int y, int color) {
        g.drawString(font, text, (int) (left + sizeX / 2 - font.width(text) / 2), y, color, true);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);

        double sizeX = 300, sizeY = 150;
        double left = (this.width - sizeX) / 2;
        double top = (this.height - sizeY) / 2;
        g.fill((int) left, (int) top, (int) (left + sizeX), (int) (top + sizeY), 0xCC003300);

        int next = (int) top + 10;
        centered(g, "Nuclear Tech Commemorative Bobblehead", left, sizeX, next, 0x00ff00);
        next += 10;
        centered(g, type.title, left, sizeX, next, 0x009900);
        next += 20;

        if (type.contribution != null) {
            centered(g, "Has contributed", left, sizeX, next, 0x00ff00);
            next += 10;
            for (String text : type.contribution.split("\\$")) {
                centered(g, text, left, sizeX, next, 0x009900);
                next += 10;
            }
            next += 10;
        }

        if (type.inscription != null) {
            centered(g, "On the bottom is the following inscription:", left, sizeX, next, 0x00ff00);
            next += 10;
            for (String text : type.inscription.split("\\$")) {
                centered(g, text, left, sizeX, next, 0x009900);
                next += 10;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
