package com.space_m.rocket.pad.client;

import java.util.List;

import com.hbm_m.inventory.gui.GuiInfoScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.rocket.client.RocketPronter;
import com.space_m.rocket.pad.ContainerLaunchPadRocket;
import com.space_m.rocket.pad.TileEntityLaunchPadRocket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 NTM Space {@code GUILaunchPadRocket} (188 x 236): Stromleiste, Startlampe, Stromlampe, drehende Vorschau der
 * eingelegten Rakete (Schere 50 x 106), Fehlerliste in halber Schrift, Knopf "COMMIT TO LAUNCH" (nur wenn die Rakete
 * gueltig ist) -> {@code NBTControlPacket {launch: true}}.
 */
public class GUILaunchPadRocket extends GuiInfoScreen<ContainerLaunchPadRocket> {

    private static final ResourceLocation texture = SpaceMod.id("textures/gui/machine/gui_launchpad_rocket.png");

    public GUILaunchPadRocket(ContainerLaunchPadRocket menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 188;
        this.imageHeight = 236;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        TileEntityLaunchPadRocket machine = menu.machine;
        if (machine != null) {
            drawElectricityInfo(g, mouseX, mouseY, 167, 36, 16, 52, machine.getPower(), TileEntityLaunchPadRocket.maxPower);

            drawCustomInfoStat(g, mouseX, mouseY, 56, 20, 18, 17, mouseX, mouseY, Component.literal("COMMIT TO LAUNCH"));
        }

        renderTooltip(g, mouseX, mouseY);
    }

    /** Original zeichnet keine Titel/Inventar-Beschriftung. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        TileEntityLaunchPadRocket machine = menu.machine;
        if (machine == null) return;

        int p = (int) (machine.getPower() * 52 / TileEntityLaunchPadRocket.maxPower);
        g.blit(texture, leftPos + 167, topPos + 36 + 52 - p, imageWidth, 8 + 52 - p, 16, p);

        if (machine.rocket != null) {
            int ox = machine.canLaunch() ? 12 : 0;
            g.blit(texture, leftPos + 59, topPos + 43, imageWidth + ox, 0, 12, 8);

            ox = machine.getPower() > TileEntityLaunchPadRocket.maxPower * 0.75 ? 0 : 6;
            g.blit(texture, leftPos + 172, topPos + 23, imageWidth + 16 + ox, 8, 6, 8);

            var ps = g.pose();
            ps.pushPose();
            {
                g.enableScissor(leftPos + 97, topPos + 18, leftPos + 97 + 50, topPos + 18 + 106);

                ps.translate(leftPos + 122, topPos + 119, 100);
                ps.mulPose(Axis.YN.rotationDegrees(System.currentTimeMillis() / 10 % 360));

                double size = 86;
                double height = machine.rocket.getHeight();
                float targetScale = (float) (size / Math.max(height, 6));

                ps.scale(-targetScale, -targetScale, -targetScale);

                com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
                RocketPronter.prontRocket(machine.rocket, ps, g.bufferSource(), LightTexture.FULL_BRIGHT);
                g.flush();
                com.mojang.blaze3d.platform.Lighting.setupForFlatItems();

                g.disableScissor();
            }
            ps.popPose();

            ps.pushPose();
            {
                ps.scale(0.5F, 0.5F, 0.5F);

                List<Component> issues = machine.findIssues();
                for (int i = 0; i < issues.size(); i++) {
                    g.drawString(font, issues.get(i), (leftPos + 6) * 2, (topPos + 66) * 2 + i * 8, 0xFFFFFF, false);
                }
            }
            ps.popPose();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        TileEntityLaunchPadRocket machine = menu.machine;

        // COMMIT TO LAUNCH
        if (machine != null && machine.rocket != null && machine.rocket.validate()
                && leftPos + 56 <= x && leftPos + 56 + 18 > x && topPos + 20 < y && topPos + 20 + 17 >= y) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean("launch", true);
            com.hbm_m.network.NBTControlPacket.sendToServer(machine.getBlockPos(), data);
        }

        return super.mouseClicked(x, y, button);
    }
}
