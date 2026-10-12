package com.space_m.satellite.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Impact;
import com.space_m.celestial.trait.CBT_Lights;
import com.space_m.client.sky.AtmosphereRenderUtil;
import com.space_m.client.sky.SkyGL;
import com.space_m.client.sky.SkyHooks;
import com.space_m.client.sky.SkyShader;
import com.space_m.client.sky.SkyShaders;
import com.space_m.client.sky.SkyTess;
import com.space_m.network.SpacePacketHandler;
import com.space_m.satellite.ClientSatellite;
import com.space_m.satellite.SatSettingsPacket;
import com.space_m.satellite.SatelliteClientData;
import com.space_m.satellite.SatelliteEvents;
import com.space_m.satellite.SatelliteItemData;
import com.space_m.satellite.SatelliteItemLogic;
import com.space_m.satellite.SatelliteOrbit;
import com.space_m.satellite.SpaceSatelliteRegistry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Einstellungen des gehaltenen Satelliten (Schleichen + Rechtsklick), 1:1 NTM Space {@code GUIScreenSatSettings}:
 * Bahnvorschau um den Zielkoerper (Sternenkarte, Koerper mit Atmosphaere/Sichel/Lichtern/Blitzen/Einschlag/Nuke-Shader,
 * eigene Satelliten desselben Besitzers), Geschwindigkeit/Hoehe/Inklination bzw. Phase/Besitzer per Mausrad, Farbe per
 * Schieber, Blinken an/aus + Blinkdauer, "Batterie" blendet die Anzeige aus. Beim Schliessen werden die Werte auf den
 * Stapel geschrieben und an den Server geschickt ({@link SatSettingsPacket}).
 * Original-Klasse: GUIScreenSatSettings
 */
public class GUIScreenSatSettings extends Screen {

    private static final ResourceLocation texture = SpaceMod.id("textures/gui/satellites/gui_sat_settings.png");
    private static final ResourceLocation starmapTexture = SpaceMod.id("textures/gui/starmap3.png");
    private static final ResourceLocation ringTexture = SpaceMod.id("textures/misc/space/rings.png");
    private static final ResourceLocation impactTexture = SpaceMod.id("textures/misc/space/impact.png");
    private static final ResourceLocation defaultMask = SpaceMod.id("textures/misc/space/default_mask.png");
    private static final ResourceLocation satelliteTextureDefault = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/item/sat_scanner.png");
    private static final ResourceLocation[] citylights = new ResourceLocation[] {
        SpaceMod.id("textures/misc/space/citylights_0.png"),
        SpaceMod.id("textures/misc/space/citylights_1.png"),
        SpaceMod.id("textures/misc/space/citylights_2.png"),
        SpaceMod.id("textures/misc/space/citylights_3.png")
    };

    private static final int TEX_W = 152;
    private static final int TEX_H = 221;

    private final Player player;
    private final Map<ResourceLocation, Boolean> textureAlphaCache = new HashMap<>();
    private int guiLeft;
    private int guiTop;
    private int draggedSlider = -1;
    private boolean hasPendingChanges;
    private String editOwner;
    private float editAltitude;
    private float editInclination;
    private float editPhaseOffset;
    private boolean editBlinking;
    private float editBlinkPeriod;
    private int editColorR;
    private int editColorG;
    private int editColorB;
    private boolean showSatelliteDetails = true;
    private int detailsMode = 0;

    private GuiGraphics g;

    public GUIScreenSatSettings(Player player) {
        super(Component.empty());
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        playUi(sound("hbm_m", "block.bobble"));
        guiLeft = (width - 134) / 2;
        guiTop = (height - 221) / 2;
        loadEditableValues();
    }

    @Override
    public void tick() {
        if (getHeldSatellite() == null) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.g = graphics;
        com.hbm_m.client.GuiCompat.renderBackground(this, graphics, mouseX, mouseY, partialTicks);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(texture, guiLeft, guiTop, 0, 0, 134, 221, TEX_W, TEX_H);

        ItemStack held = getHeldSatellite();
        if (held == null) return;

        if (showSatelliteDetails) {
            drawBatterySlice();
            drawOrbitPreview(held, partialTicks);
            if (detailsMode == 0) {
                drawLeftAligned(10, 130, 140, tr("item.sat.desc.speed") + ": " + SatelliteItemLogic.formatOrbitSpeed(editAltitude) + tr("gui.sat.settings.unit.km_per_second"), 0x00FF00);
                drawLeftAligned(10, 145, 155, tr("item.sat.desc.altitude") + ": " + SatelliteItemLogic.formatValue(editAltitude) + tr("gui.sat.settings.unit.km"), 0x00FF00);
                drawLeftAligned(10, 160, 170, tr("item.sat.desc.inclination") + ": " + SatelliteItemLogic.formatValue(editInclination) + "°", 0x00FF00);
            } else {
                drawLeftAligned(10, 130, 140, tr("gui.sat.settings.label.orbital_phase") + ": " + SatelliteItemLogic.formatPhaseOffset(editPhaseOffset) + "°", 0x00FF00);
                drawLeftAligned(10, 145, 155, tr("item.sat.desc.owner") + ": " + (editOwner != null ? editOwner : SatelliteOrbit.DEFAULT_OWNER), 0x00FF00);
                drawLeftAligned(10, 160, 170, "", 0x00FF00);
            }
            graphics.fill(guiLeft + 81, guiTop + 177, guiLeft + 110, guiTop + 199, 0xFF000000 | (editColorR << 16) | (editColorG << 8) | editColorB);
            drawRightAligned(108, 204, 213, SatelliteItemLogic.formatValue(editBlinkPeriod) + tr("gui.sat.settings.unit.seconds"), 0xFFFFFF, 2F / 3F);
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        drawSlider(editColorR, 180, 17);
        drawSlider(editColorG, 187, 24);
        drawSlider(editColorB, 194, 31);
        if (showSatelliteDetails && editBlinking) {
            graphics.blit(texture, guiLeft + 114, guiTop + 202, 136, 3, 12, 12, TEX_W, TEX_H);
        }

        if (showSatelliteDetails && isOwnerButtonAt(mouseX, mouseY)) {
            tooltip("gui.sat.settings.tooltip.owner_assign", mouseX, mouseY);
        } else if (showSatelliteDetails && isModeButtonAt(mouseX, mouseY)) {
            tooltip("gui.sat.settings.tooltip.switch_settings_list", mouseX, mouseY);
        } else if (showSatelliteDetails) {
            int scrollField = getScrollFieldAt(mouseX, mouseY);
            if (scrollField == 0) {
                if (detailsMode == 0) {
                    tooltip("gui.sat.settings.tooltip.scroll_speed", mouseX, mouseY);
                } else {
                    tooltip("gui.sat.settings.tooltip.scroll_phase", mouseX, mouseY);
                }
            } else if (scrollField == 1) {
                tooltip("gui.sat.settings.tooltip.scroll_altitude", mouseX, mouseY);
            } else if (scrollField == 2) {
                tooltip("gui.sat.settings.tooltip.scroll_inclination", mouseX, mouseY);
            } else if (scrollField == 3) {
                tooltip("gui.sat.settings.tooltip.scroll_blink", mouseX, mouseY);
            }
        }
    }

    private void tooltip(String key, int mouseX, int mouseY) {
        g.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == InputConstants.KEY_ESCAPE || minecraft.options.keyInventory.matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        super.mouseClicked(mx, my, button);
        int mouseX = (int) mx;
        int mouseY = (int) my;

        if (button != 0) return false;
        if (getHeldSatellite() == null) return false;

        if (isBatteryToggleAt(mouseX, mouseY)) {
            boolean removeBattery = showSatelliteDetails;
            playUi(sound("space_m", removeBattery ? "item.unpackremove" : "item.unpackinsert")); // Original item.unpackRemove / unpackInsert
            showSatelliteDetails = !showSatelliteDetails;
            return true;
        }

        if (isBlinkButtonAt(mouseX, mouseY)) {
            playButton();
            editBlinking = !editBlinking;
            markDirty();
            return true;
        }

        if (isOwnerButtonAt(mouseX, mouseY)) {
            playButton();
            String owner = player.getName().getString();
            if (owner.equals(editOwner)) return true;
            editOwner = owner;
            markDirty();
            return true;
        }

        if (isModeButtonAt(mouseX, mouseY)) {
            playButton();
            detailsMode = detailsMode == 0 ? 1 : 0;
            return true;
        }

        draggedSlider = getSliderAt(mouseX, mouseY);
        if (draggedSlider >= 0) {
            updateSlider(draggedSlider, mouseX);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && draggedSlider >= 0) {
            updateSlider(draggedSlider, (int) mx);
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggedSlider >= 0) {
            draggedSlider = -1;
        }
        return super.mouseReleased(mx, my, button);
    }

    //? if < 1.21.1 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scroll((int) mouseX, (int) mouseY, delta);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return scroll((int) mouseX, (int) mouseY, deltaY);
    }
    *///?}

    private boolean scroll(int mouseX, int mouseY, double scroll) {
        if (scroll == 0) return false;
        int scrollField = getScrollFieldAt(mouseX, mouseY);
        if (scrollField < 0) return false;

        adjustScrollField(scrollField, scroll > 0 ? 1 : -1);
        return true;
    }

    private void drawLeftAligned(int x, int y1, int y2, String text, int color) {
        drawScaledString(text, guiLeft + x, getCenteredY(y1, y2, 2F / 3F), color, 2F / 3F);
    }

    private void drawRightAligned(int x, int y1, int y2, String text, int color, float scale) {
        drawScaledString(text, guiLeft + x - Math.round(font.width(text) * scale), getCenteredY(y1, y2, scale), color, scale);
    }

    private int getCenteredY(int y1, int y2, float scale) {
        return guiTop + y1 + ((y2 - y1 + 1) - Math.round(font.lineHeight * scale)) / 2;
    }

    private void drawScaledString(String text, int x, int y, int color, float scale) {
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1F);
        g.drawString(font, text, Math.round(x / scale), Math.round(y / scale), color, false);
        g.pose().popPose();
    }

    private void drawSlider(int value, int y, int v) {
        g.blit(texture, guiLeft + 12 + Math.round(value * 62 / 255F), guiTop + y - 2, 136, v, 2, 6, TEX_W, TEX_H);
    }

    private int getSliderAt(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;

        if (x < 12 || x >= 12 + 64) return -1;
        if (y >= 180 - 2 && y < 180 - 2 + 6) return 0;
        if (y >= 187 - 2 && y < 187 - 2 + 6) return 1;
        if (y >= 194 - 2 && y < 194 - 2 + 6) return 2;
        return -1;
    }

    private void drawBatterySlice() {
        float u1 = 136F / 152F;
        float v1 = 38F / 221F;
        float u2 = 149F / 152F;
        float v2 = 67F / 221F;
        float x = guiLeft + 15;
        float y = guiTop + 200;
        beginGL();
        SkyGL.bind(texture);
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(x, y + 13F, 0, u2, v2);
        SkyTess.addVertexWithUV(x + 29F, y + 13F, 0, u2, v1);
        SkyTess.addVertexWithUV(x + 29F, y, 0, u1, v1);
        SkyTess.addVertexWithUV(x, y, 0, u1, v2);
        SkyTess.draw();
        SkyGL.reset();
    }

    private void updateSlider(int slider, int mouseX) {
        ItemStack held = getHeldSatellite();
        if (held == null) return;

        int value = Math.round(Mth.clamp(mouseX - guiLeft - 12, 0, 62) * 255F / 62);

        if (slider == 0) {
            if (editColorR == value) return;
            editColorR = value;
            markDirty();
            return;
        }
        if (slider == 1) {
            if (editColorG == value) return;
            editColorG = value;
            markDirty();
            return;
        }
        if (slider == 2) {
            if (editColorB == value) return;
            editColorB = value;
            markDirty();
        }
    }

    private void adjustScrollField(int field, int delta) {
        if (getHeldSatellite() == null) return;

        if (detailsMode != 0) {
            if (field == 0) adjustPhaseOffset(delta);
            return;
        }

        switch (field) {
            case 0 -> adjustSpeed(delta);
            case 1 -> adjustAltitude(delta);
            case 2 -> adjustInclination(delta);
            case 3 -> adjustBlinkPeriod(delta);
            default -> { }
        }
    }

    private void adjustAltitude(int delta) {
        float oldValue = editAltitude;
        float step = hasShiftDown() ? 5.0F : 1.0F;
        float newValue = Mth.clamp(oldValue + (delta > 0 ? step : -step), SatelliteOrbit.MIN_ALTITUDE_KM, SatelliteOrbit.MAX_ALTITUDE_KM);

        if (newValue == oldValue) return;

        editAltitude = newValue;
        markDirty();
    }

    private void adjustInclination(int delta) {
        float oldValue = editInclination;
        float step = hasShiftDown() ? 15.0F : 1.0F;
        float newValue = Mth.clamp(oldValue + (delta > 0 ? step : -step), SatelliteOrbit.MIN_INCLINATION, SatelliteOrbit.MAX_INCLINATION);

        if (newValue == oldValue) return;

        editInclination = newValue;
        markDirty();
    }

    private void adjustSpeed(int delta) {
        // speed is just the inverse of the altitude
        adjustAltitude(-delta);
    }

    private void adjustPhaseOffset(int delta) {
        float oldValue = editPhaseOffset;
        float step = hasShiftDown() ? 15.0F : 1.0F;
        float newValue = SatelliteOrbit.normalizePhaseOffset(oldValue + (delta > 0 ? step : -step));
        if (Math.abs(newValue - oldValue) < 0.0005F) return;

        editPhaseOffset = newValue;
        markDirty();
    }

    private void adjustBlinkPeriod(int delta) {
        float oldValue = editBlinkPeriod;
        float newValue = Math.round((oldValue + (delta > 0 ? 0.1F : -0.1F)) * 10F) / 10F;

        newValue = SatelliteOrbit.clampBlinkPeriod(newValue);
        if (newValue == oldValue) return;

        editBlinkPeriod = newValue;
        markDirty();
    }

    private int getScrollFieldAt(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;

        if (x >= 9 && x < 111) {
            if (y >= 130 && y <= 140) return 0;
            if (detailsMode == 0) {
                if (y >= 145 && y <= 155) return 1;
                if (y >= 160 && y <= 170) return 2;
            }
        }
        if (x >= 81 && x < 111 && y >= 204 && y <= 213) return 3;
        return -1;
    }

    private boolean isBlinkButtonAt(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        return x >= 114 && x < 127 && y >= 202 && y < 215;
    }

    private boolean isBatteryToggleAt(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        return x >= 15 && x < 44 && y >= 200 && y < 213;
    }

    private boolean isOwnerButtonAt(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        return x >= 113 && x < 126 && y >= 128 && y < 141;
    }

    private boolean isModeButtonAt(int mouseX, int mouseY) {
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        return x >= 113 && x < 126 && y >= 143 && y < 156;
    }

    private ItemStack getHeldSatellite() {
        ItemStack held = player.getMainHandItem();
        return !held.isEmpty() && SatelliteEvents.isPayloadSatellite(held.getItem()) ? held : null;
    }

    private void loadEditableValues() {
        ItemStack held = getHeldSatellite();
        if (held == null) return;

        SatelliteOrbit o = SatelliteItemData.read(held);
        hasPendingChanges = false;
        editOwner = o.owner;
        editAltitude = o.altitude;
        editInclination = o.inclination;
        editPhaseOffset = o.phaseOffset;
        editBlinking = o.isBlinking;
        editBlinkPeriod = o.blinkPeriod;
        editColorR = toColorChannel(o.colorR);
        editColorG = toColorChannel(o.colorG);
        editColorB = toColorChannel(o.colorB);
    }

    private void markDirty() {
        hasPendingChanges = true;
    }

    private void applyEditableValues(ItemStack held) {
        SatelliteItemData.setOwner(held, editOwner);
        SatelliteItemData.setAltitude(held, editAltitude);
        SatelliteItemData.setInclination(held, editInclination);
        SatelliteItemData.setPhaseOffset(held, editPhaseOffset);
        SatelliteItemData.setBlinking(held, editBlinking);
        SatelliteItemData.setBlinkPeriod(held, editBlinkPeriod);
        SatelliteItemData.setColor(held, editColorR / 255F, editColorG / 255F, editColorB / 255F);
    }

    private CompoundTag buildControlData() {
        CompoundTag data = new CompoundTag();
        data.putString("satOwner", editOwner);
        data.putFloat("satAltitude", editAltitude);
        data.putFloat("satInclination", editInclination);
        data.putFloat("satPhaseOffset", editPhaseOffset);
        data.putBoolean("satIsBlinking", editBlinking);
        data.putFloat("satBlink", editBlinkPeriod);
        data.putInt("satColorR", editColorR);
        data.putInt("satColorG", editColorG);
        data.putInt("satColorB", editColorB);
        return data;
    }

    @Override
    public void removed() {
        if (hasPendingChanges) {
            ItemStack held = getHeldSatellite();
            if (held != null) {
                applyEditableValues(held);
                SpacePacketHandler.sendToServer(SatSettingsPacket.ID, new SatSettingsPacket(buildControlData()));
            }
            hasPendingChanges = false;
        }
        super.removed();
    }

    // ── Bahnvorschau ──

    /** GL-Ersatz auf den GUI-Posenstapel setzen. */
    private void beginGL() {
        SkyGL.begin(g.pose().last().pose());
    }

    private void drawOrbitPreview(ItemStack held, float partialTicks) {
        CelestialBody body = getPreviewBody(held);
        float bodySizeAt1x = getBodySizePxAt1x(body);
        float baseOrbitRadiusMapPx = bodySizeAt1x * 1.5F;
        Map<Integer, ClientSatellite> satellites = SatelliteClientData.getClientSats(body);
        String owner = editOwner;
        float maxAltitude = editAltitude;

        for (ClientSatellite satellite : satellites.values()) {
            if (owner.equals(satellite.orbit.owner)) {
                maxAltitude = Math.max(maxAltitude, satellite.orbit.altitude);
            }
        }

        float renderZoom = getPreviewZoom(bodySizeAt1x, baseOrbitRadiusMapPx, maxAltitude) * 2.0F;
        float centerX = guiLeft + 9 + 116 * 0.5F;
        float centerY = guiTop + 8 + 116 * 0.5F;
        float bodySize = Mth.clamp(bodySizeAt1x * renderZoom, 8F, 96F);
        float iconSize = Mth.clamp(bodySize * 0.75F * 0.25F, 0.4F, 9.0F);
        double angle = getArtificialSatelliteAngle();

        float heldAltitude = editAltitude;
        float heldInclination = editInclination;
        float heldPhaseOffset = editPhaseOffset;
        float heldR = editColorR / 255F;
        float heldG = editColorG / 255F;
        float heldB = editColorB / 255F;
        ResourceLocation heldTexture = satelliteTextureDefault;

        double dayTicks = minecraft.level.getGameTime() + partialTicks;

        g.enableScissor(guiLeft + 9, guiTop + 8, guiLeft + 9 + 116, guiTop + 8 + 116);
        beginGL();
        drawStarmapBackground();

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);

        drawOwnedSatellites(satellites, owner, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, angle, iconSize, false);
        float heldBlinkAlpha = SatelliteOrbit.getBlinkAlpha(editBlinking, editBlinkPeriod);
        drawSatelliteOrbitHalf(centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldAltitude, heldInclination, heldR, heldG, heldB, false, 0.45F * heldBlinkAlpha);
        drawSatelliteIcon(heldTexture, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldPhaseOffset, heldAltitude, heldInclination, angle, false, iconSize * 1.2F);
        drawBodyPreview(body, centerX, centerY, bodySize, dayTicks);

        drawOwnedSatellites(satellites, owner, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, angle, iconSize, true);
        drawSatelliteOrbitHalf(centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldAltitude, heldInclination, heldR, heldG, heldB, true, 0.45F * heldBlinkAlpha);
        drawSatelliteIcon(heldTexture, centerX, centerY, baseOrbitRadiusMapPx, renderZoom, heldPhaseOffset, heldAltitude, heldInclination, angle, true, iconSize * 1.2F);

        SkyGL.reset();
        g.disableScissor();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private void drawStarmapBackground() {
        float bgSrcW = 116;
        float bgSrcH = 116;
        float bgU = Mth.clamp(1024F * 0.5F - bgSrcW * 0.5F, 0F, 1024F - bgSrcW);
        float bgV = Mth.clamp(1024F * 0.5F - bgSrcH * 0.5F, 0F, 1024F - bgSrcH);

        SkyGL.color(1F, 1F, 1F, 1F);
        SkyGL.bind(starmapTexture);
        drawPartialTex(guiLeft + 9, guiTop + 8, 116, 116, bgU / 1024F, bgV / 1024F, (bgU + bgSrcW) / 1024F, (bgV + bgSrcH) / 1024F);
    }

    private void drawOwnedSatellites(Map<Integer, ClientSatellite> satellites, String owner, float centerX, float centerY, float baseOrbitRadiusMapPx, float zoom, double angle, float iconSize, boolean frontHalf) {
        for (ClientSatellite satellite : satellites.values()) {
            SatelliteOrbit o = satellite.orbit;
            if (!owner.equals(o.owner)) continue;
            float blinkAlpha = SatelliteOrbit.getBlinkAlpha(o.isBlinking, o.blinkPeriod);

            drawSatelliteOrbitHalf(centerX, centerY, baseOrbitRadiusMapPx, zoom, o.altitude, o.inclination, o.colorR, o.colorG, o.colorB, frontHalf, 0.25F * blinkAlpha);
        }

        for (ClientSatellite satellite : satellites.values()) {
            SatelliteOrbit o = satellite.orbit;
            if (!owner.equals(o.owner)) continue;

            drawSatelliteIcon(satelliteTextureDefault, centerX, centerY, baseOrbitRadiusMapPx, zoom, o.phaseOffset, o.altitude, o.inclination, angle, frontHalf, iconSize);
        }
    }

    private void drawSatelliteOrbitHalf(float centerX, float centerY, float baseRadiusMapPx, float zoom, float altitude, float inclination, float r, float g, float b, boolean frontHalf, float alpha) {
        SkyGL.disableTexture();
        RenderSystem.lineWidth(1F);

        float lineR = Mth.clamp(r, 0F, 1F);
        float lineG = Mth.clamp(g, 0F, 1F);
        float lineB = Mth.clamp(b, 0F, 1F);

        boolean hasPrev = false;
        float prevX = 0F;
        float prevY = 0F;
        float prevDepth = 0F;
        boolean prevInFrontHalf = false;
        boolean drawing = false;

        for (int i = 0; i <= 64; i++) {
            float orbitAngle = (float) (2D * Math.PI * ((double) i / 64D));
            float[] orbitPoint = getArtificialSatelliteOrbitPoint(altitude, inclination, orbitAngle, baseRadiusMapPx);
            float currX = centerX + orbitPoint[0] * zoom;
            float currY = centerY + orbitPoint[1] * zoom;
            float currDepth = orbitPoint[2];
            boolean currInFrontHalf = currDepth >= 0F;

            if (!hasPrev) {
                prevX = currX;
                prevY = currY;
                prevDepth = currDepth;
                prevInFrontHalf = currInFrontHalf;
                hasPrev = true;
                continue;
            }

            boolean prevSelected = prevInFrontHalf == frontHalf;
            boolean currSelected = currInFrontHalf == frontHalf;

            if (prevSelected && currSelected) {
                if (!drawing) {
                    SkyTess.startDrawing(SkyTess.LINE_STRIP);
                    SkyTess.setColorRGBA_F(lineR, lineG, lineB, alpha);
                    SkyTess.addVertex(prevX, prevY, 0);
                    drawing = true;
                }
                SkyTess.addVertex(currX, currY, 0);
            } else if (prevSelected != currSelected) {
                float depthDelta = currDepth - prevDepth;
                float t = depthDelta == 0F ? 0.5F : -prevDepth / depthDelta;
                t = Mth.clamp(t, 0F, 1F);
                float crossX = prevX + (currX - prevX) * t;
                float crossPointY = prevY + (currY - prevY) * t;

                if (prevSelected) {
                    if (!drawing) {
                        SkyTess.startDrawing(SkyTess.LINE_STRIP);
                        SkyTess.setColorRGBA_F(lineR, lineG, lineB, alpha);
                        SkyTess.addVertex(prevX, prevY, 0);
                        drawing = true;
                    }
                    SkyTess.addVertex(crossX, crossPointY, 0);
                    SkyTess.draw();
                    drawing = false;
                } else {
                    SkyTess.startDrawing(SkyTess.LINE_STRIP);
                    SkyTess.setColorRGBA_F(lineR, lineG, lineB, alpha);
                    SkyTess.addVertex(crossX, crossPointY, 0);
                    SkyTess.addVertex(currX, currY, 0);
                    drawing = true;
                }
            }

            prevX = currX;
            prevY = currY;
            prevDepth = currDepth;
            prevInFrontHalf = currInFrontHalf;
        }

        if (drawing) SkyTess.draw();
        SkyGL.enableTexture();
        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private void drawSatelliteIcon(ResourceLocation tex, float centerX, float centerY, float baseRadiusMapPx, float zoom, float phaseOffset, float altitude, float inclination, double angle, boolean frontHalf, float size) {
        float satelliteAngle = SatelliteOrbit.applyPhaseOffsetToOrbitAngle(phaseOffset, altitude, angle, (float) (2D * Math.PI));
        float[] orbitPoint = getArtificialSatelliteOrbitPoint(altitude, inclination, satelliteAngle, baseRadiusMapPx);
        float screenX = centerX + orbitPoint[0] * zoom;
        float screenY = centerY + orbitPoint[1] * zoom;
        if ((orbitPoint[2] >= 0F) != frontHalf) return;

        float half = size * 0.5F;
        float minX = guiLeft + 9;
        float minY = guiTop + 8;
        float maxX = minX + 116;
        float maxY = minY + 116;

        if (screenX + half < minX || screenX - half > maxX || screenY + half < minY || screenY - half > maxY) return;

        SkyGL.color(1F, 1F, 1F, 1F);
        SkyGL.bind(tex != null ? tex : satelliteTextureDefault);
        drawPartialTex(screenX - half, screenY - half, size, size, 0F, 0F, 1F, 1F);
        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private void drawBodyPreview(CelestialBody body, float centerX, float centerY, float size, double dayTicks) {
        float half = size * 0.5F;
        float ringHalfWidth = 0F;
        float ringHalfHeight = 0F;
        float minX = centerX - half;
        float maxX = centerX + half;
        float minY = centerY - half;
        float maxY = centerY + half;

        if (body.hasRings) {
            ringHalfWidth = size * 0.5F * Math.max(1F, body.ringSize);
            float ringTiltSin = Math.abs(Mth.sin((float) Math.toRadians(body.ringTilt)));
            ringTiltSin = Math.max(0.08F, ringTiltSin);
            ringHalfHeight = Math.max(0.5F, ringHalfWidth * ringTiltSin);

            minX = Math.min(minX, centerX - ringHalfWidth);
            maxX = Math.max(maxX, centerX + ringHalfWidth);
            minY = Math.min(minY, centerY - ringHalfHeight);
            maxY = Math.max(maxY, centerY + ringHalfHeight);
        }

        if (maxX < guiLeft + 9 || minX > guiLeft + 9 + 116 || maxY < guiTop + 8 || minY > guiTop + 8 + 116) {
            return;
        }

        if (body.hasRings) {
            drawBodyRingHalf(body, centerX, centerY, ringHalfWidth, ringHalfHeight, false);
        }

        if (body.texture != null) {
            SkyGL.bind(body.texture);
            if (body.parent == null) {
                drawTexturedQuad(centerX, centerY, size, 0F);
            } else {
                AtmosphereRenderUtil.renderAtmosphereGlow2D(body, centerX, centerY, size, 1.0F);
                SkyGL.enableTexture();
                SkyGL.enableBlend();
                SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);
                SkyGL.bind(body.texture);

                float phase = getBodyRotationPhase(body, dayTicks);
                float bodyRotationAngle = phase * 360F;
                boolean rotateBody = hasTransparentPixels(body.texture);
                float textureUOffset = 0F;

                if (rotateBody) {
                    drawTexturedQuadRotating(centerX, centerY, size, bodyRotationAngle);
                } else {
                    drawTexturedQuad(centerX, centerY, size, phase);
                    textureUOffset = phase;
                }

                drawBodyOverlays(body, centerX, centerY, size, rotateBody, bodyRotationAngle, dayTicks, textureUOffset);
            }
        } else {
            int color = 0xFF666666;
            if (body.color != null && body.color.length >= 3) {
                color = 0xFF000000 | (toColorChannel(body.color[0]) << 16) | (toColorChannel(body.color[1]) << 8) | toColorChannel(body.color[2]);
            }
            g.fill((int) (centerX - half), (int) (centerY - half), (int) (centerX + half), (int) (centerY + half), color);
        }

        if (body.hasRings) {
            drawBodyRingHalf(body, centerX, centerY, ringHalfWidth, ringHalfHeight, true);
        }
    }

    private void drawBodyRingHalf(CelestialBody body, float bodyScreenX, float bodyScreenY, float ringHalfWidth, float drawH, boolean frontHalf) {
        if (body == null || ringHalfWidth <= 0F || drawH <= 0F) return;

        float[] ringColor = body.ringColor != null && body.ringColor.length >= 3 ? body.ringColor : null;
        float r = ringColor != null ? ringColor[0] : 0.5F;
        float gr = ringColor != null ? ringColor[1] : 0.5F;
        float b = ringColor != null ? ringColor[2] : 0.5F;
        float a = ringColor != null && ringColor.length >= 4 ? ringColor[3] : 1F;
        float drawX = bodyScreenX - ringHalfWidth;
        float drawY = frontHalf ? bodyScreenY : bodyScreenY - drawH;
        float drawW = ringHalfWidth * 2F;
        float v1 = frontHalf ? 0.5F : 0F;
        float v2 = frontHalf ? 1F : 0.5F;

        SkyGL.color(r, gr, b, a);
        SkyGL.bind(ringTexture);
        drawPartialTex(drawX, drawY, drawW, drawH, 0F, v1, 1F, v2);
        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private void drawBodyOverlays(CelestialBody body, float bodyScreenX, float bodyScreenY, float drawSize, boolean rotateBody, float bodyRotationAngle, double dayTicks, float textureUOffset) {
        float phase = calculateBodyCrescentPhase(body, dayTicks);
        CBT_Impact impact = body.getTrait(CBT_Impact.class);
        CBT_Lights light = body.getTrait(CBT_Lights.class);
        List<SkyHooks.ShockStatus> nukeShocks = SkyHooks.get().clientShocks(body);
        double impactTime = impact != null ? dayTicks - impact.time : 0.0D;
        float impactAnimationTime = impact != null ? (float) impactTime : -1.0F;
        int lightIntensity = light != null && impactTime < 40.0D ? Mth.clamp(light.getIntensity(), 0, citylights.length - 1) : 0;
        int activeBlackouts = Math.max(0, Math.min((int) (impactTime / 8.0D), 5));
        float atmosphereAlpha = AtmosphereRenderUtil.getAtmosphereSurfaceAlpha(body);
        float atmosphereDensity = AtmosphereRenderUtil.getAtmosphereDensity(body);
        Vec3 atmosphereColor = AtmosphereRenderUtil.getBodyAtmosphereColor(body);
        Vec3 cloudColor = AtmosphereRenderUtil.getBodyCloudColor(body);
        float cloudTintStrength = AtmosphereRenderUtil.getBodyCloudTintStrength(body);
        float weatherPartialTicks = (float) (dayTicks - Math.floor(dayTicks));
        float cloudStormDarkness = AtmosphereRenderUtil.getBodyCloudStormDarkness(body, weatherPartialTicks);
        float cloudLightningStrength = AtmosphereRenderUtil.getBodyCloudLightningStrength(body, weatherPartialTicks);
        int atmosphereStyle = AtmosphereRenderUtil.getAtmosphereStyle(body);
        float atmosphereTime = (float) (dayTicks / 20.0D);
        float atmospherePatternOffset = textureUOffset;
        if (!rotateBody && body != null) {
            double period = body.getRotationalPeriod();
            if (period > 0D && !Double.isNaN(period) && !Double.isInfinite(period)) {
                atmospherePatternOffset = (float) (dayTicks / period);
            }
        }

        if (atmosphereAlpha > 0.001F) {
            SkyGL.enableBlend();
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);
            SkyGL.color(1F, 1F, 1F, 1F);

            SkyShader atmosphereShader = SkyShaders.ATMOSPHERE;
            if (atmosphereShader.use()) {
                atmosphereShader.setUniform1f("offset", textureUOffset);
                atmosphereShader.setUniform1f("patternOffset", atmospherePatternOffset);
                atmosphereShader.setUniform1i("bodyTex", 0);
                atmosphereShader.setUniform1i("useBodyAlphaMask", 1);
                atmosphereShader.setUniform1f("atmosphereColorR", (float) atmosphereColor.x);
                atmosphereShader.setUniform1f("atmosphereColorG", (float) atmosphereColor.y);
                atmosphereShader.setUniform1f("atmosphereColorB", (float) atmosphereColor.z);
                atmosphereShader.setUniform1f("cloudColorR", (float) cloudColor.x);
                atmosphereShader.setUniform1f("cloudColorG", (float) cloudColor.y);
                atmosphereShader.setUniform1f("cloudColorB", (float) cloudColor.z);
                atmosphereShader.setUniform1f("cloudTintStrength", cloudTintStrength);
                atmosphereShader.setUniform1f("cloudStormDarkness", cloudStormDarkness);
                atmosphereShader.setUniform1f("atmosphereAlpha", atmosphereAlpha);
                atmosphereShader.setUniform1f("atmosphereTime", atmosphereTime);
                atmosphereShader.setUniform1i("atmosphereStyle", atmosphereStyle);
                atmosphereShader.setUniform1f("impactTime", impactAnimationTime);
                AtmosphereRenderUtil.applyNukeShockUniforms(atmosphereShader, nukeShocks, dayTicks);

                SkyGL.bind(body.texture);
                drawBodyQuad(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, 0F);

                atmosphereShader.stop();
            }
        }

        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);
        SkyGL.color(1F, 1F, 1F, 1F);

        SkyShader crescentShader = SkyShaders.CRESCENT;
        if (crescentShader.use()) {
            crescentShader.setUniform1f("phase", phase);
            crescentShader.setUniform1f("offset", textureUOffset);
            crescentShader.setUniform1i("bodyTex", 0);
            crescentShader.setUniform1i("useBodyAlphaMask", 1);

            SkyGL.bind(body.texture);
            drawBodyQuad(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, 0F);

            crescentShader.stop();
        }

        if (lightIntensity > 0
                && atmosphereDensity > 0.001F
                && (atmosphereStyle == AtmosphereRenderUtil.ATMOSPHERE_STYLE_CLOUDS || atmosphereStyle == AtmosphereRenderUtil.ATMOSPHERE_STYLE_HAZE)) {
            SkyGL.enableBlend();
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);
            SkyGL.color(1F, 1F, 1F, 1F);

            SkyShader atmosphereEmissiveShader = SkyShaders.ATMOSPHERE_EMISSIVE;
            if (atmosphereEmissiveShader.use()) {
                atmosphereEmissiveShader.setUniform1f("phase", phase);
                atmosphereEmissiveShader.setUniform1f("offset", textureUOffset);
                atmosphereEmissiveShader.setUniform1f("atmosphereDensity", atmosphereDensity);
                atmosphereEmissiveShader.setUniform1f("patternOffset", atmospherePatternOffset);
                atmosphereEmissiveShader.setUniform1f("atmosphereTime", atmosphereTime);
                atmosphereEmissiveShader.setUniform1i("atmosphereStyle", atmosphereStyle);
                atmosphereEmissiveShader.setUniform1f("impactTime", impactAnimationTime);
                AtmosphereRenderUtil.applyNukeShockUniforms(atmosphereEmissiveShader, nukeShocks, dayTicks);
                atmosphereEmissiveShader.setUniform1i("bodyTex", 0);
                atmosphereEmissiveShader.setUniform1i("lights", 1);
                atmosphereEmissiveShader.setUniform1i("cityMask", 2);
                atmosphereEmissiveShader.setUniform1i("blackouts", activeBlackouts);
                atmosphereEmissiveShader.setUniform1i("useBodyAlphaMask", 1);

                SkyGL.bind(body.texture);
                SkyGL.bindUnit(1, citylights[lightIntensity]);
                SkyGL.bindUnit(2, body.cityMask != null ? body.cityMask : defaultMask);

                drawBodyQuad(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, 0F);

                atmosphereEmissiveShader.stop();
            }
        }

        if (lightIntensity > 0) {
            SkyGL.enableBlend();
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);
            SkyGL.color(1F, 1F, 1F, 1F);

            SkyShader nightLightsShader = SkyShaders.NIGHT_LIGHTS;
            if (nightLightsShader.use()) {
                nightLightsShader.setUniform1f("phase", phase);
                nightLightsShader.setUniform1f("offset", textureUOffset);
                nightLightsShader.setUniform1f("atmosphereDensity", atmosphereDensity);
                nightLightsShader.setUniform1f("patternOffset", atmospherePatternOffset);
                nightLightsShader.setUniform1f("atmosphereTime", atmosphereTime);
                nightLightsShader.setUniform1i("atmosphereStyle", atmosphereStyle);
                nightLightsShader.setUniform1f("impactTime", impactAnimationTime);
                AtmosphereRenderUtil.applyNukeShockUniforms(nightLightsShader, nukeShocks, dayTicks);
                nightLightsShader.setUniform1i("bodyTex", 0);
                nightLightsShader.setUniform1i("lights", 1);
                nightLightsShader.setUniform1i("cityMask", 2);
                nightLightsShader.setUniform1i("blackouts", activeBlackouts);
                nightLightsShader.setUniform1i("useBodyAlphaMask", 1);

                SkyGL.bind(body.texture);
                SkyGL.bindUnit(1, citylights[lightIntensity]);
                SkyGL.bindUnit(2, body.cityMask != null ? body.cityMask : defaultMask);

                drawBodyQuad(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, 0F);

                nightLightsShader.stop();
            }
        }

        if (atmosphereAlpha > 0.001F && cloudLightningStrength > 0.001F
                && (atmosphereStyle == AtmosphereRenderUtil.ATMOSPHERE_STYLE_CLOUDS || atmosphereStyle == AtmosphereRenderUtil.ATMOSPHERE_STYLE_HAZE)) {
            SkyGL.enableBlend();
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);
            SkyGL.color(1F, 1F, 1F, 1F);

            SkyShader lightningShader = SkyShaders.LIGHTNING;
            if (lightningShader.use()) {
                lightningShader.setUniform1f("phase", phase);
                lightningShader.setUniform1f("offset", textureUOffset);
                lightningShader.setUniform1f("patternOffset", atmospherePatternOffset);
                lightningShader.setUniform1i("bodyTex", 0);
                lightningShader.setUniform1i("cityMask", 1);
                lightningShader.setUniform1i("useBodyAlphaMask", 1);
                lightningShader.setUniform1f("cloudTintStrength", cloudTintStrength);
                lightningShader.setUniform1f("cloudLightningStrength", cloudLightningStrength);
                lightningShader.setUniform1f("atmosphereAlpha", atmosphereAlpha);
                lightningShader.setUniform1f("atmosphereTime", atmosphereTime);
                lightningShader.setUniform1f("eveFlashStrength", AtmosphereRenderUtil.getBodyEveFlashStrength(body, atmosphereTime));
                lightningShader.setUniform1i("atmosphereStyle", atmosphereStyle);
                lightningShader.setUniform1i("lightningMode", AtmosphereRenderUtil.getBodyLightningMode(body));
                lightningShader.setUniform1f("impactTime", impactAnimationTime);
                AtmosphereRenderUtil.applyNukeShockUniforms(lightningShader, nukeShocks, dayTicks);

                SkyGL.bind(body.texture);
                SkyGL.bindUnit(1, body.cityMask != null ? body.cityMask : defaultMask);

                drawBodyQuad(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, 0F);

                lightningShader.stop();
            }
        }

        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);

        if (impact != null) {
            float lavaAlpha = (float) Math.min(impactTime * 0.1D, 1.0D);
            if (lavaAlpha > 0F) {
                SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);
                SkyGL.color(1F, 1F, 1F, lavaAlpha);
                SkyGL.bind(impactTexture);
                drawBodyQuad(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, textureUOffset);
                SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);
            }
        }

        if (!nukeShocks.isEmpty()) {
            renderNukeImpactOverlay(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, phase, nukeShocks, dayTicks);
            SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);
        }

        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private void renderNukeImpactOverlay(float bodyScreenX, float bodyScreenY, float drawSize, boolean rotateBody, float bodyRotationAngle, float phase, List<SkyHooks.ShockStatus> nukeShocks, double currentShockTime) {
        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE);
        SkyGL.color(1F, 1F, 1F, 1F);

        SkyShader nukeShader = SkyShaders.NUKE;
        if (!nukeShader.use()) return;
        nukeShader.setUniform1f("phase", phase);
        AtmosphereRenderUtil.applyNukeShockUniforms(nukeShader, nukeShocks, currentShockTime);

        drawBodyQuad(bodyScreenX, bodyScreenY, drawSize, rotateBody, bodyRotationAngle, 0F);

        nukeShader.stop();
    }

    /** {@code if(rotateBody) drawTexturedQuadRotating(...) else drawTexturedQuad(..., uOffset)}. */
    private void drawBodyQuad(float x, float y, float size, boolean rotateBody, float angle, float uOffset) {
        if (rotateBody) {
            drawTexturedQuadRotating(x, y, size, angle);
        } else {
            drawTexturedQuad(x, y, size, uOffset);
        }
    }

    private boolean hasTransparentPixels(ResourceLocation tex) {
        Boolean cached = textureAlphaCache.get(tex);
        if (cached != null) return cached;

        boolean hasAlpha = false;
        Optional<Resource> resource = minecraft.getResourceManager().getResource(tex);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                for (int y = 0; y < image.getHeight() && !hasAlpha; y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        if (((image.getPixelRGBA(x, y) >>> 24) & 255) < 255) {
                            hasAlpha = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        textureAlphaCache.put(tex, hasAlpha);
        return hasAlpha;
    }

    private float calculateBodyCrescentPhase(CelestialBody body, double dayTicks) {
        if (body == null || body.parent == null) return 0F;

        double orbitalPeriodTicks = body.getOrbitalPeriod() * (double) AstronomyUtil.TICKS_IN_DAY;
        if (orbitalPeriodTicks <= 0D) return 0F;

        double worldTicks = dayTicks * AstronomyUtil.TIME_MULTIPLIER;
        float orbitalAngle = (float) (2D * Math.PI * (worldTicks / orbitalPeriodTicks));
        float dx = Mth.cos(orbitalAngle);
        float dy = Mth.sin(orbitalAngle);

        float phaseMagnitude = Mth.clamp((dy + 1F) * 0.5F, 0F, 1F);
        float phaseSign = dx <= 0F ? 1F : -1F;
        return Mth.clamp(phaseMagnitude * phaseSign, -1F, 1F);
    }

    private void drawPartialTex(float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(x, y + h, 0, u1, v2);
        SkyTess.addVertexWithUV(x + w, y + h, 0, u2, v2);
        SkyTess.addVertexWithUV(x + w, y, 0, u2, v1);
        SkyTess.addVertexWithUV(x, y, 0, u1, v1);
        SkyTess.draw();
    }

    private void drawTexturedQuad(float x, float y, float size, float uOffset) {
        float half = size * 0.5F;
        float minU = uOffset;
        float maxU = 1F + uOffset;
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(x - half, y + half, 0, minU, 1F);
        SkyTess.addVertexWithUV(x + half, y + half, 0, maxU, 1F);
        SkyTess.addVertexWithUV(x + half, y - half, 0, maxU, 0F);
        SkyTess.addVertexWithUV(x - half, y - half, 0, minU, 0F);
        SkyTess.draw();
    }

    private void drawTexturedQuadRotating(float x, float y, float size, float angle) {
        SkyGL.push();
        SkyGL.translate(x, y, 0F);
        SkyGL.rotate(angle, 0F, 0F, 1F);
        drawTexturedQuad(0F, 0F, size, 0F);
        SkyGL.pop();
    }

    private float getPreviewZoom(float bodySizeAt1x, float baseOrbitRadiusMapPx, float maxAltitude) {
        float altitude = Math.max(SatelliteOrbit.DEFAULT_ALTITUDE_KM, maxAltitude);
        float altitudeFactor = altitude / SatelliteOrbit.DEFAULT_ALTITUDE_KM;
        float maxOrbitRadius = 116F * 0.46F;
        float zoomForOrbit = maxOrbitRadius / Math.max(0.0001F, baseOrbitRadiusMapPx * altitudeFactor);
        float zoomForBody = 16F / Math.max(0.0001F, bodySizeAt1x);
        return Mth.clamp(Math.min(zoomForOrbit, zoomForBody), 16F, 240F);
    }

    /** Original {@code getArtificialSatelliteOrbitPoint}: {offsetU, offsetV, depth}. */
    private float[] getArtificialSatelliteOrbitPoint(float altitude, float inclination, float angle, float baseRadiusMapPx) {
        double satInclination = Math.toRadians(inclination);
        double radiusMapPx = baseRadiusMapPx * (altitude / SatelliteOrbit.DEFAULT_ALTITUDE_KM);

        double x = radiusMapPx * Mth.cos(angle);
        double orbitY = radiusMapPx * Mth.sin(angle);
        double y = Math.sin(satInclination) * orbitY;
        double z = Math.cos(satInclination) * orbitY;

        return new float[] {(float) x, (float) y, (float) z};
    }

    private double getArtificialSatelliteAngle() {
        long cycle = 30000L;
        double progress = (double) System.currentTimeMillis() / (double) cycle;
        return -progress * 2D * Math.PI;
    }

    private CelestialBody getPreviewBody(ItemStack held) {
        CelestialBody current = SatelliteSkyHooks.currentBody(player.level());
        return SpaceSatelliteRegistry.getTargetBody(held, current.getEnum()).getBody();
    }

    private float getBodyRotationPhase(CelestialBody body, double dayTicks) {
        double period = body.getRotationalPeriod();
        return (float) ((dayTicks % period) / period);
    }

    private float getBodySizePxAt1x(CelestialBody body) {
        if (body.parent == null) return 36F * 0.45F;
        if (isMoon(body)) return getMoonSizePxAt1x(body);

        float size = body.radiusKm * (36F / 261_600F) * 2.6F;
        return Mth.clamp(size, 0.8F, 2.0F) * 0.45F;
    }

    private float getMoonSizePxAt1x(CelestialBody moon) {
        float t = (Math.max(0F, moon.radiusKm) - 65F) / (500F - 65F);
        t = Mth.clamp(t, 0F, 1F);
        return (0.2F + (0.5F - 0.2F) * t) * 0.45F * 0.82F;
    }

    private boolean isMoon(CelestialBody body) {
        return body.parent != null && body.parent.parent != null;
    }

    private static int toColorChannel(float value) {
        int color = Math.round(value * 255.0F);
        if (color < 0) return 0;
        if (color > 255) return 255;
        return color;
    }

    // ── Geraeusche ──

    private static SoundEvent sound(String ns, String path) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    private void playUi(SoundEvent event) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F));
    }

    private void playButton() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
