package com.space_m.rocket.client;

import java.util.List;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.ItemVOTVdrive.Target;
import com.space_m.rocket.MachineRocketAssemblyBlockEntity;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.menu.RocketAssemblyMenu;

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
 * GUI der VAB, 1:1 NTM Space {@code GUIMachineRocketAssembly} (192x224): Stufenwahl (Pfeile 17,34 / 17,98) mit
 * Stufenanzeige, sich drehende Vorschau der gewaehlten Stufe (Ausschnitt 65,5 90x106, weich nachgefuehrte Skalierung und
 * Hoehe), Hinweisliste halbgross, Namen von Start-/Zielkoerper der Laufwerke, Knopf "Bauen" bzw. "Zerlegen" (41,62).
 * Original-Klasse: GUIMachineRocketAssembly
 */
public class GUIMachineRocketAssembly extends GuiInfoContainerLayered<RocketAssemblyMenu> {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("space_m", "textures/gui/machine/gui_rocket_assembly.png");

    private double currentOffset = 0;
    private double currentScale = 1;
    private long lastTime = 0;

    public GUIMachineRocketAssembly(RocketAssemblyMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);

        this.imageWidth = 192;
        this.imageHeight = 224;
    }

    private MachineRocketAssemblyBlockEntity machine() {
        return menu.machine;
    }

    private RocketStruct rocket() {
        MachineRocketAssemblyBlockEntity m = machine();
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
        RocketStruct rocket = rocket();
        int guiLeft = leftPos, guiTop = topPos, xSize = imageWidth, ySize = imageHeight;

        gui.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);

        int stage = Math.max(rocket.stages.size() - 1 - getLayer(), -1);

        gui.blit(texture, guiLeft + 47, guiTop + 39, xSize + 18 + (stage + 1) * 6, 0, 6, 8);

        stage = Math.max(stage, 0);

        double dt = (double) (System.nanoTime() - lastTime) / 1000000000;
        lastTime = System.nanoTime();

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

            MachineRocketAssemblyBlockEntity m = machine();
            ItemStack fromStack = m != null ? m.fromDrive(currentLayer) : ItemStack.EMPTY;
            ItemStack toStack = m != null ? m.toDrive(currentLayer) : ItemStack.EMPTY;

            Target from = ItemVOTVdrive.getTarget(fromStack, Minecraft.getInstance().level);
            Target to = ItemVOTVdrive.getTarget(toStack, Minecraft.getInstance().level);

            List<String> issues = rocket.findIssues(stage, from.body(), to.body(), from.inOrbit(), to.inOrbit());
            for (int i = 0; i < issues.size(); i++) {
                String issue = issues.get(i);
                gui.drawString(font, issue, (guiLeft + 65) * 2, (guiTop + 5) * 2 + i * 8, 0xFFFFFF, true);
            }

            if (from.body() != null) gui.drawString(font, Component.translatable("body." + from.body().name), (guiLeft + 162) * 2, (guiTop + 75) * 2, 0x00FF00, false);
            if (to.body() != null) gui.drawString(font, Component.translatable("body." + to.body().name), (guiLeft + 162) * 2, (guiTop + 108) * 2, 0x00FF00, false);

        }
        ps.popPose();

        // Original drawGuiContainerForegroundLayer (Koordinaten relativ zur GUI)
        if (checkClick(mouseX, mouseY, 17, 34, 18, 8)) {
            gui.blit(texture, guiLeft + 17, guiTop + 34, xSize, 36, 18, 8);
        }

        if (checkClick(mouseX, mouseY, 17, 98, 18, 8)) {
            gui.blit(texture, guiLeft + 17, guiTop + 98, xSize, 44, 18, 8);
        }

        if (rocket.validate()) {
            gui.blit(texture, guiLeft + 41, guiTop + 62, xSize + 18, 8, 18, 18);
        }

        if (m() != null && m().canDeconstruct()) {
            gui.blit(texture, guiLeft + 39, guiTop + 52, xSize + 36, 8, 20, 38);
        }
    }

    private MachineRocketAssemblyBlockEntity m() {
        return machine();
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

        // Construct rocket
        if (checkClick(x, y, 41, 62, 18, 18) && machine() != null) {
            if (rocket.validate()) {
                click();
                CompoundTag data = new CompoundTag();
                data.putBoolean("construct", true);
                NBTControlPacket.sendToServer(machine().getBlockPos(), data);
                return true;
            } else if (machine().canDeconstruct()) {
                click();
                CompoundTag data = new CompoundTag();
                data.putBoolean("deconstruct", true);
                NBTControlPacket.sendToServer(machine().getBlockPos(), data);
                return true;
            }
        }

        return ret;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
