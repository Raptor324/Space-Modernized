package com.space_m.orbit.propulsion.client;

import java.util.List;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.orbit.propulsion.ContainerOrbitalStationLauncher;
import com.space_m.orbit.propulsion.TileEntityOrbitalStationLauncher;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.ItemVOTVdrive.Target;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.client.GuiInfoContainerLayered;
import com.space_m.rocket.client.RocketPronter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code GUIOrbitalStationLauncher} (192x224, {@code gui_orbital_launcher.png}): Stufenwahl (Pfeile
 * 17,34 / 17,98, Stufenanzeige 47,39), sich drehende Vorschau der gewaehlten Stufe (Ausschnitt 65,5 90x106), Hinweise der
 * Stufe (halbgross, mit Schatten) und Start-/Zielkoerper der Stufenlaufwerke, Hinweisliste des Ports (Tanks,
 * Laufwerk, Reichweite) von unten, Startknopf (41,12 18x17 -> {@code launch}). Mit angedockter Rakete nur
 * "Ready for launch!".
 * Original-Klasse: GUIOrbitalStationLauncher
 */
public class GUIOrbitalStationLauncher extends GuiInfoContainerLayered<ContainerOrbitalStationLauncher> {

    private static final ResourceLocation texture = SpaceMod.id("textures/gui/machine/gui_orbital_launcher.png");

    private double currentOffset = 0;
    private double currentScale = 1;
    private long lastTime = 0;

    public GUIOrbitalStationLauncher(ContainerOrbitalStationLauncher menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);

        this.imageWidth = 192;
        this.imageHeight = 224;
    }

    private RocketStruct rocket() {
        TileEntityOrbitalStationLauncher m = menu.machine;
        return m != null && m.rocket != null ? m.rocket : new RocketStruct();
    }

    @Override
    protected void init() {
        super.init();
        lastTime = System.nanoTime();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, gui, mouseX, mouseY, delta);
        super.render(gui, mouseX, mouseY, delta);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTicks, int mouseX, int mouseY) {
        TileEntityOrbitalStationLauncher machine = menu.machine;
        RocketStruct rocket = rocket();
        int guiLeft = leftPos, guiTop = topPos, xSize = imageWidth;

        gui.blit(texture, guiLeft, guiTop, 0, 0, imageWidth, imageHeight);

        int stage = Math.max(rocket.stages.size() - 1 - getLayer(), -1);

        gui.blit(texture, guiLeft + 47, guiTop + 39, xSize + 18 + (stage + 1) * 6, 0, 6, 8);

        stage = Math.max(stage, 0);

        double dt = (double) (System.nanoTime() - lastTime) / 1000000000;
        lastTime = System.nanoTime();

        // Original drawGuiContainerForegroundLayer (Pfeil-Hervorhebung)
        if (checkClick(mouseX, mouseY, 17, 34, 18, 8)) {
            gui.blit(texture, guiLeft + 17, guiTop + 34, xSize, 36, 18, 8);
        }

        if (checkClick(mouseX, mouseY, 17, 98, 18, 8)) {
            gui.blit(texture, guiLeft + 17, guiTop + 98, xSize, 44, 18, 8);
        }

        if (machine != null && machine.hasDocked) {
            gui.drawString(font, "Ready for launch!", guiLeft + 74, guiTop + 11, 0x00FF00, false);
            return;
        }

        PoseStack ps = gui.pose();
        ps.pushPose();
        {

            gui.enableScissor(guiLeft + 65, guiTop + 5, guiLeft + 65 + 90, guiTop + 5 + 106);

            ps.translate(guiLeft + 116, guiTop + 103, 100);
            ps.mulPose(Axis.YN.rotationDegrees(System.currentTimeMillis() / 10 % 360));

            double size = 86;
            double height = rocket.getHeight(stage);
            double targetScale = size / Math.max(height, 6);
            currentScale = currentScale + (targetScale - currentScale) * dt * 4;

            double targetOffset = rocket.getOffset(stage);
            currentOffset = currentOffset + (targetOffset - currentOffset) * dt * 4;

            ps.scale((float) -currentScale, (float) -currentScale, (float) -currentScale);
            ps.translate(0, -currentOffset, 0);

            Lighting.setupFor3DItems();
            RenderSystem.disableCull();
            RocketPronter.prontRocket(rocket, ps, gui.bufferSource(), LightTexture.FULL_BRIGHT);
            gui.flush();
            RenderSystem.enableCull();
            Lighting.setupForFlatItems();

            gui.disableScissor();

        }
        ps.popPose();

        ps.pushPose();
        {

            ps.translate(0, 0, 150);
            ps.scale(0.5F, 0.5F, 0.5F);

            ItemStack fromStack = machine != null ? machine.fromDrive(currentLayer) : ItemStack.EMPTY;
            ItemStack toStack = machine != null ? machine.toDrive(currentLayer) : ItemStack.EMPTY;

            Target from = ItemVOTVdrive.getTarget(fromStack, Minecraft.getInstance().level);
            Target to = ItemVOTVdrive.getTarget(toStack, Minecraft.getInstance().level);

            List<String> issues = rocket.findIssues(stage, from.body(), to.body(), from.inOrbit(), to.inOrbit());
            for (int i = 0; i < issues.size(); i++) {
                gui.drawString(font, issues.get(i), (guiLeft + 65) * 2, (guiTop + 5) * 2 + i * 8, 0xFFFFFF, true);
            }

            if (from.body() != null) gui.drawString(font, Component.translatable("body." + from.body().name), (guiLeft + 162) * 2, (guiTop + 75) * 2, 0x00FF00, false);
            if (to.body() != null) gui.drawString(font, Component.translatable("body." + to.body().name), (guiLeft + 162) * 2, (guiTop + 108) * 2, 0x00FF00, false);

        }
        ps.popPose();

        ps.pushPose();
        {

            ps.translate(0, 0, 150);
            ps.scale(0.5F, 0.5F, 0.5F);

            if (machine != null) {
                List<Component> issues = machine.findIssues();
                for (int i = 0; i < issues.size(); i++) {
                    gui.drawString(font, issues.get(i), (guiLeft + 65) * 2, (guiTop + 111) * 2 + i * 8 - issues.size() * 8, 0xFFFFFF, false);
                }
            }

        }
        ps.popPose();
    }

    /** Original zeichnet keine Titel/Inventar-Beschriftung. */
    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) { }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean ret = super.mouseClicked(x, y, i);
        RocketStruct rocket = rocket();

        // Stage up
        if (checkClick(x, y, 17, 34, 18, 8)) {
            click();

            if (getLayer() > 0) {
                setLayer(getLayer() - 1);
            }
            return true;
        }

        // Stage down
        if (checkClick(x, y, 17, 98, 18, 8)) {
            click();

            if (getLayer() < Math.min(rocket.stages.size(), RocketStruct.MAX_STAGES - 1)) {
                setLayer(getLayer() + 1);
            }
            return true;
        }

        // COMMIT TO LAUNCH
        if (menu.machine != null && rocket.validate() && checkClick(x, y, 41, 12, 18, 17)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("launch", true);
            NBTControlPacket.sendToServer(menu.machine.getBlockPos(), data);
            return true;
        }

        return ret;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
