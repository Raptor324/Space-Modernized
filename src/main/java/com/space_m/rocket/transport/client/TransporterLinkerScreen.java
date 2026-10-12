package com.space_m.rocket.transport.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.item.SpaceItems;
import com.space_m.rocket.transport.TransporterInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1 NTM Space {@code GUITransporterLinker} (218 x 201, {@code gui_transport_linker.png}): oben die angeklickte Rampe
 * (Planetensymbol, Name, Koordinaten), Suchfeld (59|46), darunter bis zu 5 verknuepfbare Rampen (Abstand 26 px,
 * Mausrad/Scrollbalken). Klick auf den Knopf einer Rampe verknuepft (beide Richtungen, Server), Klick auf die schon
 * verknuepfte loest, Klick auf den oberen Knopf loest ebenfalls. Rampen, die mit einer dritten verknuepft sind, werden
 * ausgeblendet. Schliesst, sobald der Linker nicht mehr in der Hand ist.
 *
 * <p>Abweichung (technisch noetig): Verknuepfungspaket traegt die Ziel-Dimension als Kennung ({@code "dimension"}).
 * Original-Klasse: GUITransporterLinker
 */
public class TransporterLinkerScreen extends Screen {

    protected static final ResourceLocation texture = SpaceMod.id("textures/gui/tool/gui_transport_linker.png");

    protected int xSize = 218;
    protected int ySize = 201;
    protected int guiLeft;
    protected int guiTop;

    private EditBox search;

    private int index = 0;

    protected final TransporterInfo linkFromTransporter;
    @Nullable protected TransporterInfo linkToTransporter;
    protected final List<TransporterInfo> transporters;

    private List<TransporterInfo> visibleTransporters = new ArrayList<>();

    private final Player player;

    public TransporterLinkerScreen(Player player, List<TransporterInfo> transporters, TransporterInfo linkFromTransporter) {
        super(Component.empty());
        this.player = player;
        this.linkFromTransporter = linkFromTransporter;
        this.linkToTransporter = linkFromTransporter.linkedTo;
        this.transporters = transporters != null ? transporters : new ArrayList<>();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        drawGuiContainerBackgroundLayer(g, mouseX, mouseY);
        drawGuiContainerForegroundLayer(g, mouseX, mouseY);
    }

    @Override
    public void tick() {
        if (player.getMainHandItem().isEmpty() || player.getMainHandItem().getItem() != SpaceItems.TRANSPORTER_LINKER.get())
            onClose();
    }

    @Override
    protected void init() {
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;

        search = new EditBox(font, guiLeft + 59, guiTop + 46, 86, 12, Component.empty());
        search.setTextColor(-1);
        search.setTextColorUneditable(-1);
        search.setBordered(false);
        search.setFocused(true);
        search.setResponder(s -> updateSearch());
        addWidget(search);
        setFocused(search);

        updateSearch();
    }

    protected void drawGuiContainerBackgroundLayer(GuiGraphics g, int mouseX, int mouseY) {
        int hideScroll = visibleTransporters.size() <= 5 ? 20 : 0;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, guiLeft, guiTop, 0, 0, xSize - hideScroll, ySize);

        // Draw search box
        if (search.isFocused())
            g.blit(texture, guiLeft + 55, guiTop + 41, 0, ySize, 107, 18);

        search.render(g, mouseX, mouseY, 0);

        // Draw current transporter
        int x = guiLeft + 37;
        int textLeftX = guiLeft + 62;
        int textRightX = guiLeft + 188;
        planet(g, linkFromTransporter, x, guiTop + 14);

        String coordinates = "x: " + linkFromTransporter.x + ", z: " + linkFromTransporter.z;
        int width = font.width(coordinates);
        g.drawString(font, linkFromTransporter.name, textLeftX, guiTop + 13, 0x00ff00, true);
        g.drawString(font, coordinates, textRightX - width, guiTop + 23, 0x00ff00, true);

        // Draw linkable transporters
        for (int i = index; i < Math.min(index + 5, visibleTransporters.size()); i++) {
            TransporterInfo transporter = visibleTransporters.get(i);
            int y = guiTop + 70 + (i - index) * 26;

            planet(g, transporter, x, y);

            coordinates = "x: " + transporter.x + ", z: " + transporter.z;
            width = font.width(coordinates);
            g.drawString(font, transporter.name, textLeftX, y - 1, 0x00ff00, true);
            g.drawString(font, coordinates, textRightX - width, y + 9, 0x00ff00, true);
        }
    }

    private static void planet(GuiGraphics g, TransporterInfo info, int x, int y) {
        if (info.planet == null) return;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(info.planet, x, y, 0, 0, 16, 16, 16, 16);
    }

    protected void drawGuiContainerForegroundLayer(GuiGraphics g, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = guiLeft + 10;

        if (isInAABB(mouseX, mouseY, x, guiTop + 13, 18, 18)) {
            g.blit(texture, x, guiTop + 13, xSize + 18, 0, 18, 18);
        }

        for (int i = index; i < Math.min(index + 5, visibleTransporters.size()); i++) {
            TransporterInfo transporter = visibleTransporters.get(i);

            int y = guiTop + 69 + (i - index) * 26;
            if (linkToTransporter != null && linkToTransporter.equals(transporter)) {
                g.blit(texture, x, y, xSize, 18, 18, 18);

                if (isInAABB(mouseX, mouseY, x, y, 18, 18)) {
                    g.blit(texture, x, y, xSize + 18, 0, 18, 18);
                }
            } else {
                if (isInAABB(mouseX, mouseY, x, y, 18, 18)) {
                    g.blit(texture, x, y, xSize, 0, 18, 18);
                }
            }
        }

        // Draw scrollbar
        if (visibleTransporters.size() > 5) {
            x = guiLeft + 202;
            int y = guiTop + 68 + Mth.floor((float) index / (float) (visibleTransporters.size() - 5) * 114.0F);
            g.blit(texture, x, y, xSize, 54, 14, 15);
        }
    }

    //? if < 1.21.1 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll(delta);
        return true;
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll(deltaY);
        return true;
    }
    *///?}

    private void scroll(double scroll) {
        if (scroll > 0 && index > 0) index--;
        if (scroll < 0 && index < visibleTransporters.size() - 5) index++;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        search.mouseClicked(mouseX, mouseY, button);
        search.setFocused(isInAABB((int) mouseX, (int) mouseY, guiLeft + 59, guiTop + 46, 86, 12));

        int x = guiLeft + 10;
        int mx = (int) mouseX, my = (int) mouseY;

        if (isInAABB(mx, my, x, guiTop + 13, 18, 18)) {
            if (selectTransporter(null)) {
                onClose();
                return true;
            }
        }

        for (int i = index; i < Math.min(index + 5, visibleTransporters.size()); i++) {
            int y = guiTop + 69 + (i - index) * 26;
            if (isInAABB(mx, my, x, y, 18, 18)) {
                if (selectTransporter(visibleTransporters.get(i))) {
                    onClose();
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isInAABB((int) mouseX, (int) mouseY, guiLeft + 198, guiTop + 64, 22, 137) && visibleTransporters.size() > 5) {
            index = Mth.floor((float) (mouseY - (guiTop + 68)) / 114.0F * (float) (visibleTransporters.size() - 5));
            index = Mth.clamp(index, 0, visibleTransporters.size() - 5);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // Links commutatively
    private boolean selectTransporter(@Nullable TransporterInfo transporter) {
        if (linkFromTransporter.equals(transporter)) return false;

        if (linkToTransporter != null && linkToTransporter.equals(transporter)) {
            linkTransporters(linkFromTransporter, null);
        } else {
            linkTransporters(linkFromTransporter, transporter);
        }

        return true;
    }

    private void linkTransporters(@Nullable TransporterInfo from, @Nullable TransporterInfo to) {
        if (from == null && to == null) return;

        if (to == null) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("unlink", true);
            NBTControlPacket.sendToServer(new BlockPos(from.x, from.y, from.z), data);
            return;
        } else if (from == null) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("unlink", true);
            NBTControlPacket.sendToServer(new BlockPos(to.x, to.y, to.z), data);
            return;
        }

        CompoundTag data = new CompoundTag();
        data.putString("dimension", to.dimension.toString());
        data.putIntArray("linkedTo", new int[] { to.x, to.y, to.z });
        NBTControlPacket.sendToServer(new BlockPos(from.x, from.y, from.z), data);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (search.isFocused() && search.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (search.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            search.keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void updateSearch() {
        visibleTransporters = new ArrayList<>();
        index = 0;

        String subs = search.getValue().toLowerCase(Locale.US);

        for (TransporterInfo transporter : transporters) {
            if (transporter.equals(linkFromTransporter)) continue;
            if (transporter.linkedTo != null && !transporter.linkedTo.equals(linkFromTransporter)) continue;
            if (transporter.name.toLowerCase(Locale.US).contains(subs)) {
                visibleTransporters.add(transporter);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isInAABB(int mouseX, int mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }
}
