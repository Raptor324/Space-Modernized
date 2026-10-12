package com.space_m.machine.misc.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.SpaceMod;
import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.trait.CBT_Impact;
import com.space_m.celestial.trait.CBT_Lights;
import com.space_m.client.sky.AtmosphereRenderUtil;
import com.space_m.client.sky.SkyGL;
import com.space_m.client.sky.SkyHooks;
import com.space_m.client.sky.SkyShader;
import com.space_m.client.sky.SkyShaders;
import com.space_m.client.sky.SkyTess;
import com.space_m.dim.SpaceDimensions;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.machine.misc.ContainerStardar;
import com.space_m.machine.misc.TileEntityMachineStardar;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.SpaceRocketItems;
import com.space_m.satellite.ClientSatellite;
import com.space_m.satellite.SatelliteClientData;
import com.space_m.satellite.SatelliteOrbit;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code GUIMachineStardar} (176x256, {@code gui_stardar.png}).
 *
 * <p><b>Systemkarte</b> (158x108 bei 9,9 auf {@code starmap3.png}, isometrisch {@code ISO_X 0,70 / ISO_Y 0,35}):
 * Sonnensystem um {@code kerbol} mit Keplerbahnen (Planetenbahnen ganz, Mondbahnen als Vorder-/Rueckhaelfte und nur bei
 * fokussiertem Planeten bzw. Mond), Koerper mit Ringen, Atmosphaeren-Leuchtrand, Drehung bzw. U-Versatz, Shader
 * (Atmosphaere, Sichel samt Mondfinsternis, emissive Atmosphaere, Nachtlichter, Blitze, Nuke-Schock), Einschlag-Lava,
 * Beschriftungen (Monde nur im Fokus-Planetensystem, Skalierung 0,6), kuenstliche Satelliten des aktuellen Koerpers
 * (Bahnhaelften, Symbol {@code sat_scanner}, Tooltip Besitzer/Geschwindigkeit/Hoehe/Inklination/Phase).
 * Ziehen verschiebt die Karte, Mausrad zoomt (0,5-180, Faktor 1,1, im Fokus begrenzt auf die Mondbahnen bzw. die
 * Detailstufe), Klick auf einen Koerper fokussiert ihn (Kamerafahrt 500 ms, folgt dem Koerper), Rechtsklick zurueck zum
 * Elternkoerper bzw. zur Gesamtansicht (420 ms), Klick auf einen Satelliten folgt ihm.
 * Zweiter Klick auf den fokussierten, landbaren Koerper mit leerem Laufwerk -> Landemodus.
 *
 * <p><b>Landemodus</b>: das Laufwerk wird auf den Koerper geschrieben ({@code pid}, auf dem eigenen Koerper mit den
 * StarDar-Koordinaten), danach zeigt die Karte die 256x256-Hoehenkarte des Servers (2 Pixel je Block, Gruen = Hoehe
 * mod 16) zum Verschieben; die 5x5-Umgebung unter der Maus muss eben sein, Klick setzt die Landekoordinaten
 * ({@code px}/{@code pz}). Ein beschriebenes, landbares Laufwerk im Slot oeffnet den Landemodus selbst, Entnahme
 * schliesst ihn; Rechtsklick verlaesst ihn.
 *
 * <p>Knoepfe: (129,123) aktuellen Koerper fokussieren, (129,143) neue Orbitalstation auf das Laufwerk,
 * (149,143) aktuellen Koerper (mit StarDar-Koordinaten) auf das Laufwerk.
 *
 * <p>Abweichungen (technisch): GL11/Tessellator ueber {@link SkyGL}/{@link SkyTess} (Shader {@link SkyShaders}),
 * Scissor ueber {@link GuiGraphics#enableScissor}, {@code Mouse.isButtonDown(0)} ueber GLFW, LWJGL2-Mausereignisse
 * ({@code handleMouseInput}) auf die einzelnen Ereignisse der Screen-API verteilt (Reihenfolge wie im Original: erst
 * {@code mouseMovedOrUp}/{@code mouseClickMove}, dann die Landemodus-Begrenzung), Dimensionsnummern als Dimensions-ID.
 * Die Bodentextur wird beim Schliessen freigegeben (Original: nie).
 * Original-Klasse: GUIMachineStardar
 */
public class GUIMachineStardar extends AbstractContainerScreen<ContainerStardar> {

    public static final ResourceLocation texture = SpaceMod.id("textures/gui/machine/gui_stardar.png");
    private static final ResourceLocation starmapTexture = SpaceMod.id("textures/gui/starmap3.png");
    private static final ResourceLocation ringTexture = SpaceMod.id("textures/misc/space/rings.png");
    private static final ResourceLocation impactTexture = SpaceMod.id("textures/misc/space/impact.png");
    private static final ResourceLocation defaultMask = SpaceMod.id("textures/misc/space/default_mask.png");
    private static final ResourceLocation satelliteTextureDefault = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/item/sat_scanner.png");
    private static final ResourceLocation[] citylights = new ResourceLocation[] {
        SpaceMod.id("textures/misc/space/citylights_0.png"),
        SpaceMod.id("textures/misc/space/citylights_1.png"),
        SpaceMod.id("textures/misc/space/citylights_2.png"),
        SpaceMod.id("textures/misc/space/citylights_3.png"),
    };

    private static final int MAP_X = 9;
    private static final int MAP_Y = 9;
    private static final int MAP_W = 158;
    private static final int MAP_H = 108;

    private static final float MAP_TEX_W = 1024F;
    private static final float MAP_TEX_H = 1024F;
    private static final float STARMAP_BG_TEX_W = 1024F;
    private static final float STARMAP_BG_TEX_H = 1024F;
    private static final float SYSTEM_LAYOUT_REF_TEX = 256F;

    private static final float MIN_ZOOM = 0.5F;
    private static final float MAX_ZOOM = 180.0F;
    private static final float ZOOM_STEP = 1.1F;

    private static final float ISO_X = 0.70F;
    private static final float ISO_Y = 0.35F;
    private static final float STARDAR_BODY_SCALE = 0.45F;
    private static final float STARDAR_MOON_VISUAL_SCALE = 0.82F;
    private static final float STARDAR_SYSTEM_RADIUS_FRACTION = 0.50F;
    private static final float SATELLITE_ICON_BODY_SCALE = 0.75F;
    private static final float SATELLITE_ORBIT_RADIUS_SCALE = 1.5F;
    private static final long SATELLITE_CYCLE_MS = 600L * 50L;

    @Nullable private final TileEntityMachineStardar star;
    private final DynamicTexture groundTexture;
    private final ResourceLocation groundMap;
    private final Map<ResourceLocation, Boolean> textureAlphaCache = new HashMap<>();

    private final List<BodyRenderInfo> renderedBodies = new ArrayList<>();
    private final List<BodyLabelRenderInfo> renderedBodyLabels = new ArrayList<>();
    private final List<SatelliteRenderInfo> renderedSatellites = new ArrayList<>();
    private final BodyPosition trackedBodyPosition = new BodyPosition();
    private final BodyPosition trackedSatellitePosition = new BodyPosition();

    private final CelestialBody currentBody;
    private CelestialBody focusedBody;
    private CelestialBody focusAnimationBody;
    private BodyRenderInfo hoveredBody;
    private SatelliteRenderInfo hoveredSatellite;
    private SatelliteRenderInfo focusedSatellite;
    private Integer focusedSatelliteFrequency;
    private CelestialBody landingBody;

    private boolean landingMode;
    private boolean hadFullDrive;
    private int lastFullDriveMeta = -1;

    private boolean draggingMap;
    private int dragLastX;
    private int dragLastY;

    private int mX;
    private int mY;
    private int lX;
    private int lY;
    private int sX;
    private int sY;
    private boolean draggingSurface;

    private float surfaceX;
    private float surfaceY;
    private float velocityX;
    private float velocityY;

    private float mapU = 0F;
    private float mapV = 0F;
    private float mapZoom = 1.0F;

    private boolean focusAnimationActive = false;
    private boolean focusAnimationToBody = false;
    private float focusAnimStartCenterU = 0F;
    private float focusAnimStartCenterV = 0F;
    private float focusAnimStartZoom = 1.0F;
    private float focusAnimTargetCenterU = 0F;
    private float focusAnimTargetCenterV = 0F;
    private float focusAnimTargetZoom = 1.0F;
    private boolean focusAnimTrackingInitialized = false;
    private float focusAnimTrackingOffsetU = 0F;
    private float focusAnimTrackingOffsetV = 0F;
    private long focusAnimStartTimeMs = 0L;
    private int focusAnimDurationMs = 500;

    private int lastMouseX;
    private int lastMouseY;

    /** Aktuelle Zeichenflaeche (nur waehrend {@link #render}). */
    private GuiGraphics g;

    public GUIMachineStardar(ContainerStardar menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.star = menu.stardar;

        this.imageWidth = 176;
        this.imageHeight = 256;

        this.currentBody = CelestialBody.getBody(star != null && star.getLevel() != null ? star.getLevel() : playerInventory.player.level());
        this.focusedBody = null;

        groundTexture = new DynamicTexture(256, 256, true);
        groundMap = net.minecraft.client.Minecraft.getInstance().getTextureManager().register("stardar_ground_map", groundTexture);

        setViewFromCenter(MAP_TEX_W * 0.5F, MAP_TEX_H * 0.5F, mapZoom);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateLandingModeFromDrive();
    }

    @Override
    public void removed() {
        super.removed();
        minecraft.getTextureManager().release(groundMap);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.g = graphics;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        GuiCompat.renderBackground(this, graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);

        this.drawCustomInfoStat(mouseX, mouseY, leftPos + 129, topPos + 124, 18, 18, mouseX, mouseY, tr("gui.stardar.tooltip.focus_current_body"));
        this.drawCustomInfoStat(mouseX, mouseY, leftPos + 129, topPos + 143, 18, 18, mouseX, mouseY, tr("gui.stardar.tooltip.program_new_station"));
        this.drawCustomInfoStat(mouseX, mouseY, leftPos + 149, topPos + 143, 18, 18, mouseX, mouseY, tr("gui.stardar.tooltip.program_current_body"));
        drawSatelliteTooltip(mouseX, mouseY);
    }

    /** Original {@code GuiInfoContainer.drawCustomInfoStat}. */
    private void drawCustomInfoStat(int mouseX, int mouseY, int x, int y, int width, int height, int tPosX, int tPosY, String text) {
        if (x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY) {
            g.renderComponentTooltip(font, List.of(Component.literal(text)), tPosX, tPosY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        this.g = graphics;
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        pushScissor(MAP_X, MAP_Y, MAP_W, MAP_H);
        SkyGL.begin(graphics.pose().last().pose());
        if (landingMode) {
            drawSurfaceMap();
        } else {
            drawSystemMap(partialTicks);
        }
        SkyGL.reset();
        popScissor();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        this.g = graphics;
        if (landingMode) {
            drawLandingForeground();
        } else {
            drawMapForeground();
        }
    }

    /** Original {@code GuiInfoContainer.pushScissor} (Koordinaten relativ zur GUI). */
    private void pushScissor(int x, int y, int width, int height) {
        g.enableScissor(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height);
    }

    private void popScissor() {
        g.disableScissor();
    }

    private void drawText(String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private ItemStack slotStack() {
        return menu.getSlot(0).getItem();
    }

    @Nullable
    private int[] heightmap() {
        return star != null ? star.heightmap : null;
    }

    private void drawMapForeground() {
        if (focusedSatelliteFrequency != null) {
            drawText(tr("gui.stardar.label.focus_satellite"), 10, 128, 0x00FF00);
            drawText(tr("gui.stardar.label.unfocus_hint"), 10, 148, 0x00FF00);
            return;
        }

        CelestialBody activeFocus = getRenderFocusBody();
        CelestialBody displayBody = hoveredBody != null ? hoveredBody.body : activeFocus;

        if (displayBody == null) {
            drawText(tr("gui.stardar.label.drag_map"), 10, 128, 0x00FF00);
            drawText(tr("gui.stardar.label.click_body_focus"), 10, 148, 0x00FF00);
            return;
        }

        drawText(tr("gui.stardar.label.focus_body", getBodyDisplayName(displayBody)), 10, 128, 0x00FF00);
        if (activeFocus != null && displayBody == activeFocus) {
            if (!displayBody.canLand) {
                drawText(tr("gui.stardar.label.cant_land"), 10, 148, 0xFF4040);
            } else {
                ItemStack slotStack = slotStack();

                if (slotStack.isEmpty()) {
                    drawText(tr("gui.stardar.label.insert_drive"), 10, 148, 0x00FF00);
                } else if (slotStack.getItem() == SpaceMaterialItems.HARD_DRIVE.get()) {
                    drawText(tr("gui.stardar.label.click_again_land"), 10, 148, 0x00FF00);
                } else {
                    drawText(tr("gui.stardar.label.insert_drive"), 10, 148, 0x00FF00);
                }
            }
        } else {
            drawText(tr("gui.stardar.label.click_body_focus"), 10, 148, 0x00FF00);
        }
    }

    private void drawLandingForeground() {
        if (heightmap() == null) {
            drawText(tr("gui.stardar.label.loading_surface_map"), 10, 148, 0x00FF00);
            return;
        }

        int sx = lastMouseX - ((lastMouseX + (int) surfaceX) % 2);
        int sy = lastMouseY - ((lastMouseY + (int) surfaceY) % 2);

        if (lastMouseX < leftPos + MAP_X || lastMouseX >= leftPos + MAP_X + MAP_W || lastMouseY < topPos + MAP_Y || lastMouseY >= topPos + MAP_Y + MAP_H) {
            drawText(tr("gui.stardar.label.select_landing_zone"), 10, 128, 0x00FF00);
            return;
        }

        int hx = (lastMouseX - (int) surfaceX - leftPos - MAP_X + 256) / 2;
        int hz = (lastMouseY - (int) surfaceY - topPos - MAP_Y + 256) / 2;
        String info = landingInfo(hx, hz);
        int altitude = altitude(hx, hz);
        boolean canLand = info == null;

        pushScissor(MAP_X, MAP_Y, MAP_W, MAP_H);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(texture, sx - leftPos - 6, sy - topPos - 6, imageWidth + (canLand ? 14 : 0), 28, 14, 14);
        popScissor();

        drawText(canLand ? tr("gui.stardar.label.valid_location") : info, 10, 128, canLand ? 0x00FF00 : 0xFF0000);
        if (altitude > 0) drawText(tr("gui.stardar.label.target_altitude", altitude), 10, 148, 0x00FF00);
    }

    private void drawSystemMap(float partialTicks) {
        updateDragPanning(lastMouseX, lastMouseY);

        float bgSrcW = MAP_W;
        float bgSrcH = MAP_H;
        float bgU = clampF(mapU + (MAP_W / mapZoom) * 0.5F - bgSrcW * 0.5F, 0F, MAP_TEX_W - bgSrcW);
        float bgV = clampF(mapV + (MAP_H / mapZoom) * 0.5F - bgSrcH * 0.5F, 0F, MAP_TEX_H - bgSrcH);
        float bgScaleU = STARMAP_BG_TEX_W / MAP_TEX_W;
        float bgScaleV = STARMAP_BG_TEX_H / MAP_TEX_H;
        float bgUTex = bgU * bgScaleU;
        float bgVTex = bgV * bgScaleV;
        float bgSrcWTex = bgSrcW * bgScaleU;
        float bgSrcHTex = bgSrcH * bgScaleV;

        SkyGL.bind(starmapTexture);
        drawPartialTex(
            leftPos + MAP_X,
            topPos + MAP_Y,
            MAP_W,
            MAP_H,
            bgUTex / STARMAP_BG_TEX_W,
            bgVTex / STARMAP_BG_TEX_H,
            (bgUTex + bgSrcWTex) / STARMAP_BG_TEX_W,
            (bgVTex + bgSrcHTex) / STARMAP_BG_TEX_H
        );

        renderedBodies.clear();
        renderedBodyLabels.clear();
        renderedSatellites.clear();
        hoveredSatellite = null;
        focusedSatellite = null;

        CelestialBody root = SolarSystem.kerbol;
        if (root == null) return;

        double dayTicks = minecraft != null && minecraft.level != null ? (double) minecraft.level.getGameTime() + (double) partialTicks : 0D;
        double worldTicks = dayTicks * (double) AstronomyUtil.TIME_MULTIPLIER;
        float centerU = MAP_TEX_W * 0.5F;
        float centerV = MAP_TEX_H * 0.5F;
        float systemOrbitScale = getOrbitScalePxPerKm(root);

        if (focusedBody != null && focusedBody.getStar() != root) {
            cancelFocusState();
        }
        if (focusAnimationBody != null && focusAnimationBody.getStar() != root) {
            cancelFocusState();
        }

        // glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT | GL_LINE_BIT)
        SkyGL.enableBlend();
        SkyGL.blendFunc(SkyGL.GL_SRC_ALPHA, SkyGL.GL_ONE_MINUS_SRC_ALPHA);

        updateFocusView(root, centerU, centerV, systemOrbitScale, worldTicks);
        drawBodyTree(root, centerU, centerV, systemOrbitScale, worldTicks, dayTicks, Float.NaN, Float.NaN);
        drawQueuedBodyLabels();

        // glPopAttrib
        SkyGL.reset();
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1F);

        hoveredBody = findBodyUnderCursor(lastMouseX, lastMouseY);
        hoveredSatellite = findSatelliteUnderCursor(lastMouseX, lastMouseY);
    }

    private void drawSurfaceMap() {
        if (!isLeftMouseDown()) {
            velocityX *= 0.85F;
            velocityY *= 0.85F;
            surfaceX += velocityX;
            surfaceY += velocityY;
            surfaceX = clampF(surfaceX, -256 + MAP_W, 256);
            surfaceY = clampF(surfaceY, -256 + MAP_H, 256);
        }

        SkyGL.bind(starmapTexture);
        drawPartialTex(
            leftPos + MAP_X,
            topPos + MAP_Y,
            MAP_W,
            MAP_H,
            0F,
            0F,
            MAP_W / STARMAP_BG_TEX_W,
            MAP_H / STARMAP_BG_TEX_H
        );

        int[] heightmap = heightmap();
        if (heightmap == null) {
            return;
        }

        if (star.updateHeightmap) {
            NativeImage pixels = groundTexture.getPixels();
            if (pixels != null) {
                for (int i = 0; i < heightmap.length && i < 256 * 256; i++) {
                    int h = Math.floorMod(heightmap[i], 16) * 16;
                    pixels.setPixelRGBA(i % 256, i / 256, 255 << 24 | h << 8); // ARGB und ABGR: Gruen liegt gleich
                }
                groundTexture.upload();
            }
            star.updateHeightmap = false;
        }

        SkyGL.bind(groundMap);
        drawPartialTex(
            leftPos + MAP_X + (int) surfaceX - 256,
            topPos + MAP_Y + (int) surfaceY - 256,
            512,
            512,
            0F,
            0F,
            1F,
            1F
        );
    }

    private boolean isLeftMouseDown() {
        return GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private void drawBodyTree(CelestialBody body, float bodyMapU, float bodyMapV, float systemOrbitScalePxPerKm, double worldTicks, double dayTicks, float parentMapU, float parentMapV) {
        if (body == null) return;

        List<ChildRenderState> childStates = new ArrayList<>();
        float parentScreenY = mapToScreenY(bodyMapU, bodyMapV);

        if (body.satellites != null) {
            for (CelestialBody child : body.satellites) {
                if (child == null) continue;

                float childScale = getChildOrbitScalePxPerKm(body, systemOrbitScalePxPerKm);
                double meanAnomaly = calculateMeanAnomaly(child, worldTicks);
                float[] offset = calculateOrbitOffsetPx(body, child, meanAnomaly, childScale);

                boolean moonChild = body.parent != null;
                float childMapU = bodyMapU + offset[0];
                float childMapV = bodyMapV + offset[1];
                boolean drawInFrontOfParent = !moonChild || mapToScreenY(childMapU, childMapV) > parentScreenY;

                childStates.add(new ChildRenderState(child, childScale, offset[0], offset[1], moonChild, drawInFrontOfParent));
            }
        }

        for (ChildRenderState childState : childStates) {
            if (!shouldDrawChildOrbit(body, childState.childBody)) {
                continue;
            }

            if (childState.moonChild) {
                drawOrbitHalf(bodyMapU, bodyMapV, body, childState.childBody, childState.orbitScalePxPerKm, false);
            } else {
                drawOrbit(bodyMapU, bodyMapV, body, childState.childBody, childState.orbitScalePxPerKm);
            }
        }

        for (ChildRenderState childState : childStates) {
            if (!childState.moonChild || childState.drawInFrontOfParent) {
                continue;
            }

            drawBodyTree(
                childState.childBody,
                bodyMapU + childState.orbitOffsetU,
                bodyMapV + childState.orbitOffsetV,
                systemOrbitScalePxPerKm,
                worldTicks,
                dayTicks,
                bodyMapU,
                bodyMapV
            );
        }

        drawArtificialSatellites(body, bodyMapU, bodyMapV, false);
        drawBody(body, bodyMapU, bodyMapV, dayTicks, parentMapU, parentMapV);
        drawArtificialSatellites(body, bodyMapU, bodyMapV, true);

        for (ChildRenderState childState : childStates) {
            if (!childState.moonChild || !shouldDrawChildOrbit(body, childState.childBody)) {
                continue;
            }

            drawOrbitHalf(bodyMapU, bodyMapV, body, childState.childBody, childState.orbitScalePxPerKm, true);
        }

        for (ChildRenderState childState : childStates) {
            if (childState.moonChild && !childState.drawInFrontOfParent) {
                continue;
            }

            drawBodyTree(
                childState.childBody,
                bodyMapU + childState.orbitOffsetU,
                bodyMapV + childState.orbitOffsetV,
                systemOrbitScalePxPerKm,
                worldTicks,
                dayTicks,
                bodyMapU,
                bodyMapV
            );
        }
    }

    private boolean shouldDrawChildOrbit(CelestialBody parent, CelestialBody child) {
        if (parent == null || child == null) return false;

        if (parent.parent == null) {
            return true;
        }

        if (!isMoon(child)) {
            return true;
        }

        // Moon orbit lines are visible only when the parent body (or one of its moons) is focused.
        CelestialBody activeFocus = getOrbitRuleFocusBody();
        return activeFocus != null && (activeFocus == parent || activeFocus.parent == parent);
    }

    private void drawOrbit(float parentMapU, float parentMapV, CelestialBody parent, CelestialBody body, float orbitScalePxPerKm) {
        if (body == null || body.semiMajorAxisKm <= 0F) return;

        SkyGL.disableTexture();
        RenderSystem.lineWidth(2F);

        float[] color = body.color != null && body.color.length >= 3 ? body.color : null;
        float r = color != null ? color[0] : 0.8F;
        float gr = color != null ? color[1] : 0.8F;
        float b = color != null ? color[2] : 0.8F;

        SkyTess.startDrawing(SkyTess.LINE_STRIP);
        SkyTess.setColorRGBA_F(r, gr, b, 0.4F);

        for (int i = 0; i <= 96; i++) {
            double meanAnomaly = 2D * Math.PI * ((double) i / 96D);
            float[] offset = calculateOrbitOffsetPx(parent, body, meanAnomaly, orbitScalePxPerKm);
            float orbitMapU = parentMapU + offset[0];
            float orbitMapV = parentMapV + offset[1];
            SkyTess.addVertex(mapToScreenX(orbitMapU, orbitMapV), mapToScreenY(orbitMapU, orbitMapV), 0);
        }
        SkyTess.draw();

        SkyGL.enableTexture();
        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private void drawOrbitHalf(float parentMapU, float parentMapV, CelestialBody parent, CelestialBody body, float orbitScalePxPerKm, boolean frontHalf) {
        if (body == null || body.semiMajorAxisKm <= 0F) {
            return;
        }

        SkyGL.disableTexture();
        RenderSystem.lineWidth(2F);

        float[] color = body.color != null && body.color.length >= 3 ? body.color : null;
        float r = color != null ? color[0] : 0.8F;
        float gr = color != null ? color[1] : 0.8F;
        float b = color != null ? color[2] : 0.8F;

        float crossY = mapToScreenY(parentMapU, parentMapV);
        boolean hasPrevPoint = false;
        float prevScreenX = 0F;
        float prevScreenY = 0F;
        boolean prevInFrontHalf = false;
        boolean drawing = false;

        for (int i = 0; i <= 96; i++) {
            double meanAnomaly = 2D * Math.PI * ((double) i / 96D);
            float[] offset = calculateOrbitOffsetPx(parent, body, meanAnomaly, orbitScalePxPerKm);
            float orbitMapU = parentMapU + offset[0];
            float orbitMapV = parentMapV + offset[1];
            float currScreenX = mapToScreenX(orbitMapU, orbitMapV);
            float currScreenY = mapToScreenY(orbitMapU, orbitMapV);
            boolean currInFrontHalf = currScreenY >= crossY;

            if (!hasPrevPoint) {
                prevScreenX = currScreenX;
                prevScreenY = currScreenY;
                prevInFrontHalf = currInFrontHalf;
                hasPrevPoint = true;
                continue;
            }

            boolean prevInSelectedHalf = prevInFrontHalf == frontHalf;
            boolean currInSelectedHalf = currInFrontHalf == frontHalf;

            if (prevInSelectedHalf && currInSelectedHalf) {
                if (!drawing) {
                    SkyTess.startDrawing(SkyTess.LINE_STRIP);
                    SkyTess.setColorRGBA_F(r, gr, b, 0.4F);
                    SkyTess.addVertex(prevScreenX, prevScreenY, 0);
                    drawing = true;
                }
                SkyTess.addVertex(currScreenX, currScreenY, 0);
            } else if (prevInSelectedHalf != currInSelectedHalf) {
                float dy = currScreenY - prevScreenY;
                float tCross = dy == 0F ? 0.5F : (crossY - prevScreenY) / dy;
                tCross = clampF(tCross, 0F, 1F);
                float crossX = prevScreenX + (currScreenX - prevScreenX) * tCross;

                if (prevInSelectedHalf) {
                    if (!drawing) {
                        SkyTess.startDrawing(SkyTess.LINE_STRIP);
                        SkyTess.setColorRGBA_F(r, gr, b, 0.4F);
                        SkyTess.addVertex(prevScreenX, prevScreenY, 0);
                        drawing = true;
                    }
                    SkyTess.addVertex(crossX, crossY, 0);
                    SkyTess.draw();
                    drawing = false;
                } else {
                    SkyTess.startDrawing(SkyTess.LINE_STRIP);
                    SkyTess.setColorRGBA_F(r, gr, b, 0.4F);
                    SkyTess.addVertex(crossX, crossY, 0);
                    SkyTess.addVertex(currScreenX, currScreenY, 0);
                    drawing = true;
                }
            }

            prevScreenX = currScreenX;
            prevScreenY = currScreenY;
            prevInFrontHalf = currInFrontHalf;
        }

        if (drawing) {
            SkyTess.draw();
        }

        SkyGL.enableTexture();
        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private void drawBody(CelestialBody body, float bodyMapU, float bodyMapV, double dayTicks, float parentMapU, float parentMapV) {
        if (body == null) return;

        float drawSize = getBodySizePxAtZoom(body);
        float bodyScreenX = mapToScreenX(bodyMapU, bodyMapV);
        float bodyScreenY = mapToScreenY(bodyMapU, bodyMapV);
        float half = drawSize * 0.5F;
        float ringHalfWidth = 0F;
        float ringHalfHeight = 0F;

        float minX = bodyScreenX - half;
        float maxX = bodyScreenX + half;
        float minY = bodyScreenY - half;
        float maxY = bodyScreenY + half;

        if (body.hasRings) {
            ringHalfWidth = drawSize * 0.5F * Math.max(1F, body.ringSize);
            float ringTiltSin = Math.abs(Mth.sin((float) Math.toRadians(body.ringTilt)));
            ringTiltSin = Math.max(0.08F, ringTiltSin);
            ringHalfHeight = Math.max(0.5F, ringHalfWidth * ringTiltSin);

            minX = Math.min(minX, bodyScreenX - ringHalfWidth);
            maxX = Math.max(maxX, bodyScreenX + ringHalfWidth);
            minY = Math.min(minY, bodyScreenY - ringHalfHeight);
            maxY = Math.max(maxY, bodyScreenY + ringHalfHeight);
        }

        if (maxX < leftPos + MAP_X || minX > leftPos + MAP_X + MAP_W ||
            maxY < topPos + MAP_Y || minY > topPos + MAP_Y + MAP_H) {
            return;
        }

        if (body.hasRings) {
            drawBodyRingHalf(body, bodyScreenX, bodyScreenY, ringHalfWidth, ringHalfHeight, false);
        }

        if (body.texture != null) {
            SkyGL.bind(body.texture);
            if (body.parent == null) {
                drawTexturedQuad(bodyScreenX, bodyScreenY, drawSize, 0F);
            } else {
                AtmosphereRenderUtil.renderAtmosphereGlow2D(body, bodyScreenX, bodyScreenY, drawSize, 1.0F);
                SkyGL.bind(body.texture);

                float phase = getBodyRotationPhase(body, dayTicks);
                float bodyRotationAngle = phase * 360F;
                boolean rotateBody = hasTransparentPixels(body.texture);
                float textureUOffset = 0F;
                if (rotateBody) {
                    drawTexturedQuadRotating(bodyScreenX, bodyScreenY, drawSize, bodyRotationAngle);
                } else {
                    drawTexturedQuad(bodyScreenX, bodyScreenY, drawSize, phase);
                    textureUOffset = phase;
                }
                drawBodyOverlays(body, bodyScreenX, bodyScreenY, drawSize, bodyMapU, bodyMapV, parentMapU, parentMapV, rotateBody, bodyRotationAngle, dayTicks, textureUOffset);
            }
        } else {
            int color = colorArrayToRgb(body.color);
            g.fill((int) (bodyScreenX - half), (int) (bodyScreenY - half), (int) (bodyScreenX + half), (int) (bodyScreenY + half), color);
        }

        if (body.hasRings) {
            drawBodyRingHalf(body, bodyScreenX, bodyScreenY, ringHalfWidth, ringHalfHeight, true);
        }

        float labelAnchorExtentX = half;
        float labelAnchorExtentY = half;
        if (body.parent == null) {
            float starLabelHalfExtent = (8F * mapZoom) * 0.5F;
            labelAnchorExtentX = Math.min(labelAnchorExtentX, starLabelHalfExtent);
            labelAnchorExtentY = Math.min(labelAnchorExtentY, starLabelHalfExtent);
        }
        renderedBodyLabels.add(new BodyLabelRenderInfo(body, bodyScreenX + labelAnchorExtentX, bodyScreenY - labelAnchorExtentY));

        renderedBodies.add(new BodyRenderInfo(body, bodyMapU, bodyMapV, drawSize));
    }

    private void drawArtificialSatellites(CelestialBody body, float bodyMapU, float bodyMapV, boolean frontHalf) {
        if (!shouldDrawArtificialSatellites(body)) {
            return;
        }

        Map<Integer, ClientSatellite> satellites = SatelliteClientData.getClientSats();
        if (satellites == null || satellites.isEmpty()) {
            return;
        }

        float baseOrbitRadiusMapPx = getBodySizePxAt1x(body) * SATELLITE_ORBIT_RADIUS_SCALE;
        if (baseOrbitRadiusMapPx <= 0F) {
            return;
        }

        if (shouldDrawArtificialSatelliteOrbitLines(body)) {
            for (ClientSatellite satellite : satellites.values()) {
                if (satellite == null) {
                    continue;
                }
                drawArtificialSatelliteOrbitHalf(bodyMapU, bodyMapV, baseOrbitRadiusMapPx, satellite.orbit, frontHalf);
            }
        }

        float iconSize = clampF(getBodySizePxAtZoom(body) * SATELLITE_ICON_BODY_SCALE * 0.25F, 0.2F, 4.0F);
        float iconHalf = iconSize * 0.5F;
        float mapLeft = leftPos + MAP_X;
        float mapTop = topPos + MAP_Y;
        float mapRight = mapLeft + MAP_W;
        float mapBottom = mapTop + MAP_H;
        double angle = getArtificialSatelliteAngle();

        SkyGL.color(1F, 1F, 1F, 1F);
        for (Map.Entry<Integer, ClientSatellite> entry : satellites.entrySet()) {
            Integer frequency = entry.getKey();
            ClientSatellite satellite = entry.getValue();
            if (frequency == null || satellite == null) {
                continue;
            }

            SatelliteOrbit orbit = satellite.orbit;
            float satelliteAngle = SatelliteOrbit.applyPhaseOffsetToOrbitAngle(orbit.phaseOffset, orbit.altitude, angle, (float) (2D * Math.PI));
            SatelliteOrbitPoint orbitPoint = getArtificialSatelliteOrbitPoint(orbit, satelliteAngle, baseOrbitRadiusMapPx);
            float screenX = mapToScreenX(bodyMapU + orbitPoint.offsetU, bodyMapV + orbitPoint.offsetV);
            float screenY = mapToScreenY(bodyMapU + orbitPoint.offsetU, bodyMapV + orbitPoint.offsetV);
            boolean inFrontHalf = orbitPoint.depth >= 0F;
            if (inFrontHalf != frontHalf) {
                continue;
            }
            if (screenX + iconHalf < mapLeft || screenX - iconHalf > mapRight || screenY + iconHalf < mapTop || screenY - iconHalf > mapBottom) {
                continue;
            }

            SkyGL.bind(satelliteTextureDefault);
            drawPartialTex(screenX - iconHalf, screenY - iconHalf, iconSize, iconSize, 0F, 0F, 1F, 1F);

            SatelliteRenderInfo renderInfo = new SatelliteRenderInfo(frequency.intValue(), orbit, bodyMapU + orbitPoint.offsetU, bodyMapV + orbitPoint.offsetV, iconSize);
            renderedSatellites.add(renderInfo);
            if (focusedSatelliteFrequency != null && focusedSatelliteFrequency.intValue() == renderInfo.frequency) {
                focusedSatellite = renderInfo;
            }
        }
    }

    private boolean shouldDrawArtificialSatellites(CelestialBody body) {
        return body != null && currentBody != null && body == currentBody;
    }

    private void drawArtificialSatelliteOrbitHalf(float bodyMapU, float bodyMapV, float baseRadiusMapPx, SatelliteOrbit satellite, boolean frontHalf) {
        float r = clampF(satellite.colorR, 0F, 1F);
        float gr = clampF(satellite.colorG, 0F, 1F);
        float b = clampF(satellite.colorB, 0F, 1F);

        SkyGL.disableTexture();
        RenderSystem.lineWidth(1F);

        boolean hasPrev = false;
        float prevX = 0F;
        float prevY = 0F;
        float prevDepth = 0F;
        boolean prevInFrontHalf = false;
        boolean drawing = false;

        for (int i = 0; i <= 64; i++) {
            float angle = (float) (2D * Math.PI * ((double) i / 64D));
            SatelliteOrbitPoint orbitPoint = getArtificialSatelliteOrbitPoint(satellite, angle, baseRadiusMapPx);
            float currX = mapToScreenX(bodyMapU + orbitPoint.offsetU, bodyMapV + orbitPoint.offsetV);
            float currY = mapToScreenY(bodyMapU + orbitPoint.offsetU, bodyMapV + orbitPoint.offsetV);
            float currDepth = orbitPoint.depth;
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
                    SkyTess.setColorRGBA_F(r, gr, b, 0.25F);
                    SkyTess.addVertex(prevX, prevY, 0);
                    drawing = true;
                }
                SkyTess.addVertex(currX, currY, 0);
            } else if (prevSelected != currSelected) {
                float depthDelta = currDepth - prevDepth;
                float t = depthDelta == 0F ? 0.5F : -prevDepth / depthDelta;
                t = clampF(t, 0F, 1F);
                float crossX = prevX + (currX - prevX) * t;
                float crossPointY = prevY + (currY - prevY) * t;

                if (prevSelected) {
                    if (!drawing) {
                        SkyTess.startDrawing(SkyTess.LINE_STRIP);
                        SkyTess.setColorRGBA_F(r, gr, b, 0.25F);
                        SkyTess.addVertex(prevX, prevY, 0);
                        drawing = true;
                    }
                    SkyTess.addVertex(crossX, crossPointY, 0);
                    SkyTess.draw();
                    drawing = false;
                } else {
                    SkyTess.startDrawing(SkyTess.LINE_STRIP);
                    SkyTess.setColorRGBA_F(r, gr, b, 0.25F);
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

        if (drawing) {
            SkyTess.draw();
        }

        SkyGL.enableTexture();
        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private SatelliteOrbitPoint getArtificialSatelliteOrbitPoint(@Nullable SatelliteOrbit satellite, float angle, float baseRadiusMapPx) {
        float altitude = satellite != null ? satellite.altitude : SatelliteOrbit.DEFAULT_ALTITUDE_KM;
        double inclination = Math.toRadians(satellite != null ? satellite.inclination : SatelliteOrbit.DEFAULT_INCLINATION);
        double radiusMapPx = baseRadiusMapPx * (altitude / SatelliteOrbit.DEFAULT_ALTITUDE_KM);

        double x = radiusMapPx * Mth.cos(angle);
        double y = radiusMapPx * Mth.sin(angle);
        double z = Math.sin(inclination) * y;
        y = Math.cos(inclination) * y;

        y -= z * 0.35D;
        y *= 0.8D;

        return new SatelliteOrbitPoint((float) x, (float) y, (float) z);
    }

    private double getArtificialSatelliteAngle() {
        long cycle = SATELLITE_CYCLE_MS;
        double progress = (double) System.currentTimeMillis() / (double) cycle;
        return -progress * 2D * Math.PI;
    }

    private void drawQueuedBodyLabels() {
        if (renderedBodyLabels.isEmpty() || this.font == null) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        List<BodyLabelRenderInfo> sortedLabels = new ArrayList<>(renderedBodyLabels);
        sortedLabels.sort(new Comparator<BodyLabelRenderInfo>() {
            @Override
            public int compare(BodyLabelRenderInfo a, BodyLabelRenderInfo b) {
                return Integer.compare(getBodyHierarchyDepth(b.body), getBodyHierarchyDepth(a.body));
            }
        });
        for (BodyLabelRenderInfo labelInfo : sortedLabels) {
            if (labelInfo == null || labelInfo.body == null) {
                continue;
            }
            drawBodyLabel(labelInfo.body, labelInfo.topRightAnchorScreenX, labelInfo.topRightAnchorScreenY);
        }

        RenderSystem.depthMask(true);
    }

    private void drawBodyLabel(CelestialBody body, float anchorScreenX, float anchorScreenY) {
        if (body == null || this.font == null) {
            return;
        }
        if (!shouldShowBodyLabel(body)) {
            return;
        }

        String label = getBodyDisplayName(body);
        if (label.isEmpty()) {
            return;
        }

        float labelX = anchorScreenX + 1.5F;
        float labelY = anchorScreenY - 1.5F - this.font.lineHeight * 0.6F;
        int labelColor = colorArrayToRgb(body.color);
        int labelDrawX = Math.round(labelX / 0.6F);
        int labelDrawY = Math.round(labelY / 0.6F);

        g.pose().pushPose();
        g.pose().scale(0.6F, 0.6F, 1F);
        g.drawString(this.font, label, labelDrawX, labelDrawY, labelColor, true);
        g.pose().popPose();
        SkyGL.color(1F, 1F, 1F, 1F);
    }

    private boolean shouldShowBodyLabel(CelestialBody body) {
        if (body == null) {
            return false;
        }

        if (body.parent == null) {
            return false;
        }

        if (!isMoon(body)) {
            return true;
        }

        CelestialBody focusedTopLevelPlanet = getTopLevelPlanetForBody(getRenderFocusBody());
        if (focusedTopLevelPlanet == null) {
            return false;
        }
        return getTopLevelPlanetForBody(body) == focusedTopLevelPlanet;
    }

    private void drawBodyRingHalf(CelestialBody body, float bodyScreenX, float bodyScreenY, float ringHalfWidth, float drawH, boolean frontHalf) {
        if (body == null || ringHalfWidth <= 0F || drawH <= 0F) {
            return;
        }

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
        SkyTess.addVertexWithUV(x - half, y + half, 0, minU, 1);
        SkyTess.addVertexWithUV(x + half, y + half, 0, maxU, 1);
        SkyTess.addVertexWithUV(x + half, y - half, 0, maxU, 0);
        SkyTess.addVertexWithUV(x - half, y - half, 0, minU, 0);
        SkyTess.draw();
    }

    private void drawTexturedQuadRotating(float x, float y, float size, float angle) {
        SkyGL.push();
        SkyGL.translate(x, y, 0F);
        SkyGL.rotate(angle, 0F, 0F, 1F);
        drawTexturedQuad(0F, 0F, size, 0F);
        SkyGL.pop();
    }

    /** {@code if(rotateBody) drawTexturedQuadRotating(...) else drawTexturedQuad(..., uOffset)}. */
    private void drawBodyQuad(float x, float y, float size, boolean rotateBody, float angle, float uOffset) {
        if (rotateBody) {
            drawTexturedQuadRotating(x, y, size, angle);
        } else {
            drawTexturedQuad(x, y, size, uOffset);
        }
    }

    private void drawBodyOverlays(CelestialBody body, float bodyScreenX, float bodyScreenY, float drawSize, float bodyMapU, float bodyMapV, float parentMapU, float parentMapV, boolean rotateBody, float bodyRotationAngle, double dayTicks, float textureUOffset) {
        if (body == null || body.parent == null || body.texture == null) {
            return;
        }

        float phase = calculateHorizontalCrescentPhase(body, bodyMapU, bodyMapV, parentMapU, parentMapV);
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

    // yeah no, radius checks and new trait just don't work. drop them transparency checks, planets won't have it anyway, how bad it can be!
    private boolean hasTransparentPixels(ResourceLocation tex) {
        if (tex == null || minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }

        Boolean cached = textureAlphaCache.get(tex);
        if (cached != null) {
            return cached;
        }

        boolean hasAlpha = false;
        Optional<Resource> resource = minecraft.getResourceManager().getResource(tex);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                // Bilder ohne Alphakanal (Original getColorModel().hasAlpha() == false): getPixelRGBA wirft -> false
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

    private float calculateHorizontalCrescentPhase(CelestialBody body, float bodyMapU, float bodyMapV, float parentMapU, float parentMapV) {
        float sunMapU = MAP_TEX_W * 0.5F;
        float sunMapV = MAP_TEX_H * 0.5F;
        float bodyProjX = projectMapToIsoX(bodyMapU, bodyMapV);
        float bodyProjY = projectMapToIsoY(bodyMapU, bodyMapV);
        float sunProjX = projectMapToIsoX(sunMapU, sunMapV);
        float sunProjY = projectMapToIsoY(sunMapU, sunMapV);

        float dx = bodyProjX - sunProjX;
        float dy = bodyProjY - sunProjY;
        float lengthSq = dx * dx + dy * dy;
        if (lengthSq <= 0.0001F) {
            return 0F;
        }

        float length = Mth.sqrt(lengthSq);
        float verticalFactor = dy / length;
        float phaseMagnitude = clampF((verticalFactor + 1F) * 0.5F, 0F, 1F);

        phaseMagnitude = applyMoonEclipseDarkening(body, bodyMapU, bodyMapV, parentMapU, parentMapV, sunMapU, sunMapV, phaseMagnitude);

        float phaseSign = dx <= 0F ? 1F : -1F;
        return clampF(phaseMagnitude * phaseSign, -1F, 1F);
    }

    private float applyMoonEclipseDarkening(CelestialBody body, float moonMapU, float moonMapV,
                                            float parentMapU, float parentMapV, float sunMapU, float sunMapV, float phaseMagnitude) {
        if (!isMoon(body) || body.parent == null) {
            return phaseMagnitude;
        }
        if (Float.isNaN(parentMapU) || Float.isNaN(parentMapV)) {
            return phaseMagnitude;
        }

        float moonProjX = projectMapToIsoX(moonMapU, moonMapV);
        float moonProjY = projectMapToIsoY(moonMapU, moonMapV);
        float parentProjX = projectMapToIsoX(parentMapU, parentMapV);
        float parentProjY = projectMapToIsoY(parentMapU, parentMapV);
        float sunProjX = projectMapToIsoX(sunMapU, sunMapV);
        float sunProjY = projectMapToIsoY(sunMapU, sunMapV);

        float sunToParentX = parentProjX - sunProjX;
        float sunToParentY = parentProjY - sunProjY;
        float parentToMoonX = moonProjX - parentProjX;
        float parentToMoonY = moonProjY - parentProjY;

        float sunParentLenSq = sunToParentX * sunToParentX + sunToParentY * sunToParentY;
        if (sunParentLenSq <= 0.0001F) {
            return phaseMagnitude;
        }

        float sunParentLen = Mth.sqrt(sunParentLenSq);
        float alongShadowAxis = (sunToParentX * parentToMoonX + sunToParentY * parentToMoonY) / sunParentLen;
        float lineDistance = Math.abs(sunToParentX * parentToMoonY - sunToParentY * parentToMoonX) / sunParentLen;
        float eclipseRadius = Math.max(1.5F, getBodySizePxAt1x(body.parent) * 0.45F);
        float penumbraRadius = eclipseRadius * 2F;
        float behindFade = Math.max(0.15F, eclipseRadius * 0.12F);

        if (lineDistance >= penumbraRadius || alongShadowAxis <= -behindFade) {
            return phaseMagnitude;
        }

        float behindFactor = alongShadowAxis >= 0F ? 1F : smoothstep(alongShadowAxis, -behindFade, 0F);
        if (lineDistance <= eclipseRadius) {
            return clampF(phaseMagnitude + (1F - phaseMagnitude) * behindFactor, 0F, 1F);
        }

        float penumbraFactor = 1F - smoothstep(lineDistance, eclipseRadius, penumbraRadius);
        float eclipseFactor = behindFactor * penumbraFactor;
        return clampF(phaseMagnitude + (1F - phaseMagnitude) * eclipseFactor, 0F, 1F);
    }

    /** Original {@code Library.smoothstep(t, edge0, edge1)}. */
    private static float smoothstep(float t, float edge0, float edge1) {
        t = clampF((t - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Original {@code MathHelper.clamp_float} (bei NaN-Grenzen bleibt der Wert, anders als {@code Mth.clamp}). */
    private static float clampF(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private float projectMapToIsoX(float mapU, float mapV) {
        return (mapU - mapV) * ISO_X;
    }

    private float projectMapToIsoY(float mapU, float mapV) {
        return (mapU + mapV) * ISO_Y;
    }

    private float mapToScreenX(float mapCoordU, float mapCoordV) {
        float centerU = mapU + (MAP_W / mapZoom) * 0.5F;
        float centerV = mapV + (MAP_H / mapZoom) * 0.5F;
        float relU = mapCoordU - centerU;
        float relV = mapCoordV - centerV;
        return leftPos + MAP_X + MAP_W * 0.5F + (relU - relV) * mapZoom * ISO_X;
    }

    private float mapToScreenY(float mapCoordU, float mapCoordV) {
        float centerU = mapU + (MAP_W / mapZoom) * 0.5F;
        float centerV = mapV + (MAP_H / mapZoom) * 0.5F;
        float relU = mapCoordU - centerU;
        float relV = mapCoordV - centerV;
        return topPos + MAP_Y + MAP_H * 0.5F + (relU + relV) * mapZoom * ISO_Y;
    }

    private BodyRenderInfo findBodyUnderCursor(int mouseX, int mouseY) {
        BodyRenderInfo nearest = null;
        float nearestDistSq = Float.MAX_VALUE;

        for (int i = renderedBodies.size() - 1; i >= 0; i--) {
            BodyRenderInfo bodyInfo = renderedBodies.get(i);
            float bodyX = mapToScreenX(bodyInfo.mapU, bodyInfo.mapV);
            float bodyY = mapToScreenY(bodyInfo.mapU, bodyInfo.mapV);
            float clickableRadius = Math.max(5F, bodyInfo.drawSize * 0.5F);

            float dx = mouseX - bodyX;
            float dy = mouseY - bodyY;
            float distanceSq = dx * dx + dy * dy;

            if (distanceSq <= clickableRadius * clickableRadius && distanceSq < nearestDistSq) {
                nearestDistSq = distanceSq;
                nearest = bodyInfo;
            }
        }

        return nearest;
    }

    private SatelliteRenderInfo findSatelliteUnderCursor(int mouseX, int mouseY) {
        SatelliteRenderInfo nearest = null;
        float nearestDistSq = Float.MAX_VALUE;

        for (int i = renderedSatellites.size() - 1; i >= 0; i--) {
            SatelliteRenderInfo satelliteInfo = renderedSatellites.get(i);
            float satelliteX = mapToScreenX(satelliteInfo.mapU, satelliteInfo.mapV);
            float satelliteY = mapToScreenY(satelliteInfo.mapU, satelliteInfo.mapV);
            float clickableRadius = Math.max(4F, satelliteInfo.drawSize * 0.75F);
            float dx = mouseX - satelliteX;
            float dy = mouseY - satelliteY;
            float distanceSq = dx * dx + dy * dy;

            if (distanceSq <= clickableRadius * clickableRadius && distanceSq < nearestDistSq) {
                nearestDistSq = distanceSq;
                nearest = satelliteInfo;
            }
        }

        return nearest;
    }

    // ── Maus (Original handleMouseInput / mouseClicked / mouseClickMove / mouseMovedOrUp) ──

    //? if < 1.21.1 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        super.mouseScrolled(mouseX, mouseY, delta);
        handleWheel((int) mouseX, (int) mouseY, delta);
        return true;
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        handleWheel((int) mouseX, (int) mouseY, deltaY);
        return true;
    }
    *///?}

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        if (landingMode) handleLandingMouseInput(-1, false);
    }

    /** Original {@code handleMouseInput}, Mausrad-Zweig. */
    private void handleWheel(int mouseX, int mouseY, double scroll) {
        if (landingMode) {
            handleLandingMouseInput(-1, false);
            return;
        }

        if (this.minecraft == null) return;

        int wheel = scroll > 0 ? 1 : scroll < 0 ? -1 : 0;
        if (wheel == 0) return;

        if (mouseX < leftPos + MAP_X || mouseX >= leftPos + MAP_X + MAP_W || mouseY < topPos + MAP_Y || mouseY >= topPos + MAP_Y + MAP_H || focusAnimationActive) return;
        if (isMoon(focusedBody)) return;

        float oldZoom = mapZoom;
        float minZoom = MIN_ZOOM;
        float maxZoom = MAX_ZOOM;

        if (focusedBody != null) {
            float[] bounds = new float[2];
            if (calculateFocusedChildZoomBounds(focusedBody, bounds)) {
                minZoom = bounds[0];
                maxZoom = Math.max(bounds[1], getBodyDetailZoomCap(focusedBody));
            }
        }

        float newZoom = oldZoom;
        if (wheel > 0) newZoom = Math.min(maxZoom, oldZoom * ZOOM_STEP);
        if (wheel < 0) newZoom = Math.max(minZoom, oldZoom / ZOOM_STEP);

        if (newZoom != oldZoom) {
            if (isFocusContextActive()) {
                mapZoom = newZoom;
            } else {
                zoomAtMouse(mouseX, mouseY, oldZoom, newZoom);
            }
        }
    }

    /**
     * Original {@code handleLandingMouseInput} (nach jedem Mausereignis im Landemodus). Wie im Original laeuft es nach
     * {@code mouseMovedOrUp}, das {@code draggingSurface} beim Loslassen schon zuruecksetzt.
     */
    private void handleLandingMouseInput(int button, boolean buttonState) {
        if (surfaceX > 256) {
            velocityX = 0;
            surfaceX = 256;
        }
        if (surfaceX < -256 + MAP_W) {
            velocityX = 0;
            surfaceX = -256 + MAP_W;
        }
        if (surfaceY > 256) {
            velocityY = 0;
            surfaceY = 256;
        }
        if (surfaceY < -256 + MAP_H) {
            velocityY = 0;
            surfaceY = -256 + MAP_H;
        }

        if (draggingSurface && button == 0 && !buttonState) {
            velocityX = (mX - lX) * 0.8F;
            velocityY = (mY - lY) * 0.8F;
            draggingSurface = false;
        }
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        super.mouseDragged(x, y, button, dragX, dragY);
        mouseClickMove((int) x, (int) y, button);
        if (landingMode) handleLandingMouseInput(button, true);
        return true;
    }

    /** Original {@code mouseClickMove}. */
    private void mouseClickMove(int mouseX, int mouseY, int button) {
        if (!landingMode || !draggingSurface) return;

        int deltaX = mouseX - mX;
        int deltaY = mouseY - mY;
        surfaceX += deltaX;
        surfaceY += deltaY;

        lX = mX;
        lY = mY;
        mX = mouseX;
        mY = mouseY;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        super.mouseClicked(x, y, button);
        onMouseClicked((int) x, (int) y, button);
        if (landingMode) handleLandingMouseInput(button, true);
        return true;
    }

    private void playButton() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean checkClick(int x, int y, int left, int top, int sizeX, int sizeY) {
        return leftPos + left <= x && leftPos + left + sizeX > x && topPos + top < y && topPos + top + sizeY >= y;
    }

    private void sendControl(CompoundTag data) {
        if (star == null) return;
        NBTControlPacket.sendToServer(star.getBlockPos(), data);
    }

    /** Original {@code mouseClicked}. */
    private void onMouseClicked(int mouseX, int mouseY, int button) {
        if (checkClick(mouseX, mouseY, 129, 143, 18, 18)) {
            playButton();
            CompoundTag data = new CompoundTag();
            data.putString("pid", SpaceDimensions.ORBIT.location().toString());
            sendControl(data);
            return;
        }

        if (checkClick(mouseX, mouseY, 129, 123, 18, 18)) {
            playButton();
            CelestialBody body = currentBody;
            if (body != null && body.getStar() != null) {
                clearSatelliteFocus();
                if (landingMode) {
                    exitLandingMode();
                }
                configureFocusAnimation(body, body, true, 420, 0F, 0F, getFocusZoomForBody(body));
            }
            return;
        }

        if (checkClick(mouseX, mouseY, 149, 143, 18, 18)) {
            playButton();
            if (star != null && star.getLevel() != null) {
                CompoundTag data = new CompoundTag();
                data.putString("pid", star.getLevel().dimension().location().toString());
                data.putInt("ix", star.getBlockPos().getX());
                data.putInt("iz", star.getBlockPos().getZ());
                sendControl(data);
            }
            return;
        }

        if (mouseX < leftPos + MAP_X || mouseX >= leftPos + MAP_X + MAP_W || mouseY < topPos + MAP_Y || mouseY >= topPos + MAP_Y + MAP_H) {
            draggingMap = false;
            draggingSurface = false;
            return;
        }

        if (landingMode) {
            if (button == 1) {
                exitLandingMode();
                return;
            }
            if (button == 0) {
                draggingSurface = true;
                sX = mX = lX = mouseX;
                sY = mY = lY = mouseY;
            }
            return;
        }

        if (button == 1) {
            if (focusedSatelliteFrequency != null) {
                clearSatelliteFocus();
                draggingMap = false;
                return;
            }
            beginUnfocusAnimation();
            draggingMap = false;
            return;
        }

        if (button != 0) {
            return;
        }

        SatelliteRenderInfo clickedSatellite = findSatelliteUnderCursor(mouseX, mouseY);
        if (clickedSatellite != null) {
            focusSatellite(clickedSatellite);
            draggingMap = false;
            return;
        }

        BodyRenderInfo clickedBody = findBodyUnderCursor(mouseX, mouseY);
        if (clickedBody != null) {
            CelestialBody activeFocusBody = getRenderFocusBody();
            if (activeFocusBody != null && clickedBody.body == activeFocusBody) {
                handleFocusedBodySecondClick(clickedBody.body);
                draggingMap = false;
                return;
            }

            if (clickedBody.body.parent != null) {
                startFocusAnimation(clickedBody);
                draggingMap = false;
                return;
            }
        }

        if (isFocusContextActive()) {
            draggingMap = false;
            return;
        }

        draggingMap = true;
        dragLastX = mouseX;
        dragLastY = mouseY;
    }

    private void handleFocusedBodySecondClick(CelestialBody body) {
        if (body == null) return;
        if (!body.canLand) {
            return;
        }

        ItemStack slotStack = slotStack();
        if (!slotStack.isEmpty() && slotStack.getItem() == SpaceMaterialItems.HARD_DRIVE.get()) {
            enterLandingMode(body);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        super.mouseReleased(x, y, button);
        mouseMovedOrUp((int) x, (int) y, button);
        if (landingMode) handleLandingMouseInput(button, false);
        return true;
    }

    /** Original {@code mouseMovedOrUp} (Loslassen einer Taste). */
    private void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (which == 0) {
            draggingMap = false;
            draggingSurface = false;
        }

        if (!landingMode || which == -1 || which == 1) return;

        if (Math.abs(sX - mouseX) > 2 || Math.abs(sY - mouseY) > 2) return;
        if (mouseX < leftPos + MAP_X || mouseX >= leftPos + MAP_X + MAP_W || mouseY < topPos + MAP_Y || mouseY >= topPos + MAP_Y + MAP_H) return;
        if (heightmap() == null) return;

        int hx = (mouseX - (int) surfaceX - leftPos - MAP_X + 256) / 2;
        int hz = (mouseY - (int) surfaceY - topPos - MAP_Y + 256) / 2;
        if (landingInfo(hx, hz) != null) return;

        playButton();
        CompoundTag data = new CompoundTag();
        data.putInt("px", hx);
        data.putInt("pz", hz);
        sendControl(data);
    }

    private void updateDragPanning(int mouseX, int mouseY) {
        if (!draggingMap) {
            return;
        }

        if (isFocusContextActive() || !isLeftMouseDown()) {
            draggingMap = false;
            return;
        }

        int dx = mouseX - dragLastX;
        int dy = mouseY - dragLastY;
        if (dx != 0 || dy != 0) {
            panByScreenDelta(dx, dy);
            clampMapUV();
        }

        dragLastX = mouseX;
        dragLastY = mouseY;
    }

    private void panByScreenDelta(int dx, int dy) {
        float zoom = Math.max(0.0001F, mapZoom);
        float normalizedZoom = (clampF(mapZoom, MIN_ZOOM, 5.0F) - MIN_ZOOM) / (5.0F - MIN_ZOOM);
        float speed = (0.35F + (1.20F - 0.35F) * normalizedZoom) * 0.75F;

        float a = -dx / (zoom * ISO_X);
        float b = -dy / (zoom * ISO_Y);
        float deltaU = 0.5F * (a + b);
        float deltaV = 0.5F * (b - a);

        mapU += deltaU * speed;
        mapV += deltaV * speed;
    }

    private void startFocusAnimation(BodyRenderInfo bodyInfo) {
        if (bodyInfo == null || bodyInfo.body == null || bodyInfo.body.parent == null) {
            return;
        }
        if (bodyInfo.body == focusedBody) {
            return;
        }
        if (focusAnimationActive && focusAnimationToBody && bodyInfo.body == focusAnimationBody) {
            return;
        }

        clearSatelliteFocus();
        playButton();
        configureFocusAnimation(bodyInfo.body, bodyInfo.body, true, 500, bodyInfo.mapU, bodyInfo.mapV, getFocusZoomForBody(bodyInfo.body));
    }

    private void beginUnfocusAnimation() {
        if (!hasBodyFocusContext()) {
            return;
        }

        CelestialBody activeFocus = focusedBody != null ? focusedBody : focusAnimationBody;
        CelestialBody parentFocus = activeFocus != null ? activeFocus.parent : null;
        if (parentFocus != null && parentFocus.parent != null) {
            configureFocusAnimation(parentFocus, parentFocus, true, 420, 0F, 0F, getFocusZoomForBody(parentFocus));
            return;
        }

        configureFocusAnimation(null, null, false, 420, MAP_TEX_W * 0.5F, MAP_TEX_H * 0.5F, 1.0F);
    }

    private void configureFocusAnimation(CelestialBody newFocusedBody, CelestialBody animationBody, boolean animationToBody,
                                         int durationMs, float targetCenterU, float targetCenterV, float targetZoom) {
        focusedBody = newFocusedBody;
        focusAnimationBody = animationBody;
        focusAnimationToBody = animationToBody;
        focusAnimationActive = true;
        focusAnimDurationMs = durationMs;
        focusAnimStartTimeMs = System.currentTimeMillis();
        focusAnimStartCenterU = mapU + (MAP_W / mapZoom) * 0.5F;
        focusAnimStartCenterV = mapV + (MAP_H / mapZoom) * 0.5F;
        focusAnimStartZoom = mapZoom;
        focusAnimTargetCenterU = targetCenterU;
        focusAnimTargetCenterV = targetCenterV;
        focusAnimTargetZoom = targetZoom;
        focusAnimTrackingInitialized = false;
        focusAnimTrackingOffsetU = 0F;
        focusAnimTrackingOffsetV = 0F;
    }

    private void updateFocusView(CelestialBody starBody, float starCenterU, float starCenterV, float systemOrbitScalePxPerKm, double worldTicks) {
        if (updateFocusedSatelliteView(starBody, starCenterU, starCenterV, systemOrbitScalePxPerKm, worldTicks)) {
            return;
        }

        if (!hasBodyFocusContext()) {
            return;
        }

        CelestialBody trackingBody = focusedBody != null ? focusedBody : focusAnimationBody;
        boolean hasTrackingBodyPosition = false;
        if (trackingBody != null) {
            hasTrackingBodyPosition = findBodyMapPosition(starBody, starCenterU, starCenterV, systemOrbitScalePxPerKm, worldTicks, trackingBody, trackedBodyPosition);
        }

        if (focusAnimationActive) {
            long now = System.currentTimeMillis();
            double duration = Math.max(1D, focusAnimDurationMs);
            float t = clampF((float) ((now - focusAnimStartTimeMs) / duration), 0F, 1F);
            float eased = t * t * (3F - 2F * t);

            float centerU;
            float centerV;
            if (focusAnimationToBody && hasTrackingBodyPosition) {
                if (!focusAnimTrackingInitialized) {
                    focusAnimTrackingOffsetU = focusAnimStartCenterU - trackedBodyPosition.mapU;
                    focusAnimTrackingOffsetV = focusAnimStartCenterV - trackedBodyPosition.mapV;
                    focusAnimTrackingInitialized = true;
                }
                float remaining = 1F - eased;
                centerU = trackedBodyPosition.mapU + focusAnimTrackingOffsetU * remaining;
                centerV = trackedBodyPosition.mapV + focusAnimTrackingOffsetV * remaining;
            } else {
                centerU = focusAnimStartCenterU + (focusAnimTargetCenterU - focusAnimStartCenterU) * eased;
                centerV = focusAnimStartCenterV + (focusAnimTargetCenterV - focusAnimStartCenterV) * eased;
            }

            float zoom = focusAnimStartZoom + (focusAnimTargetZoom - focusAnimStartZoom) * eased;
            setViewFromCenter(centerU, centerV, zoom);

            if (t >= 1F) {
                focusAnimationActive = false;
                focusAnimationBody = null;
                focusAnimTrackingInitialized = false;
            }
            return;
        }

        if (focusedBody != null && hasTrackingBodyPosition) {
            setViewFromCenter(trackedBodyPosition.mapU, trackedBodyPosition.mapV, mapZoom);
            return;
        }

        if (focusedBody != null) {
            cancelFocusState();
        }
    }

    private boolean updateFocusedSatelliteView(CelestialBody starBody, float starCenterU, float starCenterV, float systemOrbitScalePxPerKm, double worldTicks) {
        if (focusedSatelliteFrequency == null) {
            return false;
        }

        if (!findSatelliteMapPosition(starBody, starCenterU, starCenterV, systemOrbitScalePxPerKm, worldTicks, focusedSatelliteFrequency.intValue(), trackedSatellitePosition)) {
            clearSatelliteFocus();
            return false;
        }

        setViewFromCenter(trackedSatellitePosition.mapU, trackedSatellitePosition.mapV, mapZoom);
        return true;
    }

    private boolean findBodyMapPosition(CelestialBody body, float bodyMapU, float bodyMapV, float systemOrbitScalePxPerKm,
                                        double worldTicks, CelestialBody targetBody, BodyPosition outPosition) {
        if (body == targetBody) {
            outPosition.mapU = bodyMapU;
            outPosition.mapV = bodyMapV;
            return true;
        }

        if (body.satellites == null) {
            return false;
        }

        float childOrbitScale = getChildOrbitScalePxPerKm(body, systemOrbitScalePxPerKm);
        for (CelestialBody childBody : body.satellites) {
            if (childBody == null) continue;
            double meanAnomaly = calculateMeanAnomaly(childBody, worldTicks);
            float[] orbitOffset = calculateOrbitOffsetPx(body, childBody, meanAnomaly, childOrbitScale);
            if (findBodyMapPosition(childBody, bodyMapU + orbitOffset[0], bodyMapV + orbitOffset[1], systemOrbitScalePxPerKm, worldTicks, targetBody, outPosition)) {
                return true;
            }
        }

        return false;
    }

    private boolean findSatelliteMapPosition(CelestialBody starBody, float starCenterU, float starCenterV, float systemOrbitScalePxPerKm, double worldTicks, int frequency, BodyPosition outPosition) {
        if (currentBody == null || outPosition == null) {
            return false;
        }

        Map<Integer, ClientSatellite> satellites = SatelliteClientData.getClientSats();
        if (satellites == null || satellites.isEmpty()) {
            return false;
        }

        ClientSatellite satellite = satellites.get(frequency);
        if (satellite == null) {
            return false;
        }

        if (!findBodyMapPosition(starBody, starCenterU, starCenterV, systemOrbitScalePxPerKm, worldTicks, currentBody, trackedBodyPosition)) {
            return false;
        }

        float baseOrbitRadiusMapPx = getBodySizePxAt1x(currentBody) * SATELLITE_ORBIT_RADIUS_SCALE;
        if (baseOrbitRadiusMapPx <= 0F) {
            return false;
        }

        SatelliteOrbit orbit = satellite.orbit;
        float satelliteAngle = SatelliteOrbit.applyPhaseOffsetToOrbitAngle(orbit.phaseOffset, orbit.altitude, getArtificialSatelliteAngle(), (float) (2D * Math.PI));
        SatelliteOrbitPoint orbitPoint = getArtificialSatelliteOrbitPoint(orbit, satelliteAngle, baseOrbitRadiusMapPx);
        outPosition.mapU = trackedBodyPosition.mapU + orbitPoint.offsetU;
        outPosition.mapV = trackedBodyPosition.mapV + orbitPoint.offsetV;
        return true;
    }

    private void zoomAtMouse(int mouseX, int mouseY, float oldZoom, float newZoom) {
        float minZoom = MIN_ZOOM;
        if (oldZoom <= 0F || newZoom <= 0F) {
            mapZoom = clampF(newZoom, minZoom, MAX_ZOOM);
            clampMapUV();
            return;
        }

        float mapCenterScreenX = leftPos + MAP_X + MAP_W * 0.5F;
        float mapCenterScreenY = topPos + MAP_Y + MAP_H * 0.5F;

        float oldCenterU = mapU + (MAP_W / oldZoom) * 0.5F;
        float oldCenterV = mapV + (MAP_H / oldZoom) * 0.5F;

        float oldA = (mouseX - mapCenterScreenX) / (oldZoom * ISO_X);
        float oldB = (mouseY - mapCenterScreenY) / (oldZoom * ISO_Y);
        float targetMapU = oldCenterU + 0.5F * (oldA + oldB);
        float targetMapV = oldCenterV + 0.5F * (oldB - oldA);

        float newA = (mouseX - mapCenterScreenX) / (newZoom * ISO_X);
        float newB = (mouseY - mapCenterScreenY) / (newZoom * ISO_Y);
        float newCenterU = targetMapU - 0.5F * (newA + newB);
        float newCenterV = targetMapV - 0.5F * (newB - newA);

        mapZoom = clampF(newZoom, minZoom, MAX_ZOOM);
        float viewW = MAP_W / mapZoom;
        float viewH = MAP_H / mapZoom;
        mapU = newCenterU - viewW * 0.5F;
        mapV = newCenterV - viewH * 0.5F;
        clampMapUV();
    }

    private CelestialBody getRenderFocusBody() {
        if (focusedBody != null) {
            return focusedBody;
        }
        if (focusAnimationActive && focusAnimationToBody) {
            return focusAnimationBody;
        }
        return null;
    }

    private CelestialBody getLocalOrbitParent() {
        CelestialBody activeFocus = getRenderFocusBody();
        if (activeFocus == null) {
            return null;
        }
        if (isTopLevelPlanet(activeFocus)) {
            return activeFocus;
        }
        if (activeFocus.parent != null) {
            return activeFocus.parent;
        }
        return null;
    }

    private CelestialBody getTopLevelPlanetForBody(CelestialBody body) {
        if (body == null) {
            return null;
        }

        CelestialBody current = body;
        int steps = 0;
        while (current.parent != null && current.parent.parent != null && steps <= 64) {
            current = current.parent;
            steps++;
        }
        return isTopLevelPlanet(current) ? current : null;
    }

    private boolean isTopLevelPlanet(CelestialBody body) {
        return body != null && body.parent != null && body.parent.parent == null;
    }

    private boolean isMoon(CelestialBody body) {
        return body != null && body.parent != null && body.parent.parent != null;
    }

    private void cancelFocusState() {
        focusedBody = null;
        focusAnimationBody = null;
        focusAnimationActive = false;
        focusAnimationToBody = false;
        focusAnimTrackingInitialized = false;
    }

    private void clearSatelliteFocus() {
        focusedSatelliteFrequency = null;
        focusedSatellite = null;
    }

    private void focusSatellite(SatelliteRenderInfo satelliteInfo) {
        if (satelliteInfo == null || satelliteInfo.satellite == null) {
            return;
        }
        if (focusedSatelliteFrequency != null && focusedSatelliteFrequency.intValue() == satelliteInfo.frequency) {
            return;
        }

        cancelFocusState();
        focusedSatelliteFrequency = satelliteInfo.frequency;
        focusedSatellite = satelliteInfo;
        setViewFromCenter(satelliteInfo.mapU, satelliteInfo.mapV, mapZoom);
        playButton();
    }

    private void drawSatelliteTooltip(int mouseX, int mouseY) {
        if (landingMode) {
            return;
        }

        boolean focusedTooltip = focusedSatelliteFrequency != null && focusedSatellite != null;
        if (!focusedTooltip) {
            if (!isMouseInsideMap(mouseX, mouseY)) {
                return;
            }
            if (!shouldDrawArtificialSatelliteOrbitLines(currentBody)) {
                return;
            }
        }

        SatelliteRenderInfo satelliteInfo = focusedTooltip ? focusedSatellite : hoveredSatellite;
        if (satelliteInfo == null || satelliteInfo.satellite == null) {
            return;
        }

        SatelliteOrbit satellite = satelliteInfo.satellite;
        String owner = satellite.owner != null && !satellite.owner.isEmpty() ? satellite.owner : SatelliteOrbit.DEFAULT_OWNER;
        List<Component> tooltip = new ArrayList<>(5);
        tooltip.add(Component.literal(ChatFormatting.YELLOW + tr("item.sat.desc.owner") + ": " + ChatFormatting.GOLD + owner));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + tr("item.sat.desc.speed") + ": " + ChatFormatting.GOLD + formatOrbitSpeed(satellite.altitude) + tr("gui.sat.settings.unit.km_per_second")));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + tr("item.sat.desc.altitude") + ": " + ChatFormatting.GOLD + formatValue(satellite.altitude) + tr("gui.sat.settings.unit.km")));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + tr("item.sat.desc.inclination") + ": " + ChatFormatting.GOLD + formatValue(satellite.inclination) + "°"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + tr("item.sat.desc.phase") + ": " + ChatFormatting.GOLD + formatPhaseOffset(satellite.phaseOffset) + "°"));

        int tooltipX = mouseX;
        int tooltipY = mouseY;
        if (focusedTooltip) {
            float screenX = mapToScreenX(satelliteInfo.mapU, satelliteInfo.mapV);
            float screenY = mapToScreenY(satelliteInfo.mapU, satelliteInfo.mapV);
            float half = satelliteInfo.drawSize * 0.5F;
            tooltipX = Math.round(screenX + half);
            tooltipY = Math.round(screenY - half);
        }

        g.renderComponentTooltip(font, tooltip, tooltipX, tooltipY);
    }

    private boolean isMouseInsideMap(int mouseX, int mouseY) {
        return mouseX >= leftPos + MAP_X && mouseX < leftPos + MAP_X + MAP_W && mouseY >= topPos + MAP_Y && mouseY < topPos + MAP_Y + MAP_H;
    }

    private boolean shouldDrawArtificialSatelliteOrbitLines(CelestialBody body) {
        CelestialBody activeFocus = getOrbitRuleFocusBody();
        return body != null && activeFocus != null && (activeFocus == body || activeFocus.parent == body);
    }

    private CelestialBody getOrbitRuleFocusBody() {
        CelestialBody activeFocus = getRenderFocusBody();
        if (activeFocus != null) {
            return activeFocus;
        }
        if (focusedSatelliteFrequency != null) {
            return currentBody;
        }
        return null;
    }

    private boolean hasBodyFocusContext() {
        return focusedBody != null || focusAnimationActive;
    }

    private boolean isFocusContextActive() {
        return hasBodyFocusContext() || focusedSatelliteFrequency != null;
    }

    private void setViewFromCenter(float centerU, float centerV, float zoom) {
        mapZoom = clampF(zoom, MIN_ZOOM, MAX_ZOOM);
        float viewW = MAP_W / mapZoom;
        float viewH = MAP_H / mapZoom;
        mapU = centerU - viewW * 0.5F;
        mapV = centerV - viewH * 0.5F;
        clampMapUV();
    }

    private float getFocusZoomForBody(CelestialBody body) {
        // If the focused body has satellites, start zoom so all of them are visible.
        if (body != null) {
            float[] focusZoomBounds = new float[2];
            if (calculateFocusedChildZoomBounds(body, focusZoomBounds)) {
                return focusZoomBounds[0];
            }
        }

        float bodySizeAt1x = isMoon(body) ? getMoonSizePxAt1x(body) : getBodySizePxAt1x(body);
        if (bodySizeAt1x <= 0F) {
            return clampF(mapZoom, MIN_ZOOM, MAX_ZOOM);
        }

        float targetBodySizePx = Math.min(MAP_W, MAP_H) * 0.88F;
        float targetZoom = targetBodySizePx / bodySizeAt1x;
        targetZoom /= 3.0F;
        targetZoom = Math.max(targetZoom, 4.0F);
        return clampF(targetZoom, MIN_ZOOM, MAX_ZOOM);
    }

    private boolean calculateFocusedChildZoomBounds(CelestialBody focusBody, float[] outBounds) {
        if (outBounds == null || outBounds.length < 2 || focusBody == null || focusBody.satellites == null || focusBody.satellites.isEmpty()) {
            return false;
        }

        CelestialBody starBody = focusBody.getStar();
        if (starBody == null) {
            return false;
        }

        float systemOrbitScale = getOrbitScalePxPerKm(starBody);
        if (systemOrbitScale <= 0F) {
            return false;
        }

        float childOrbitScale = getChildOrbitScalePxPerKm(focusBody, systemOrbitScale);
        if (childOrbitScale <= 0F) {
            return false;
        }

        float nearestProjectedDist = Float.MAX_VALUE;
        float furthestProjectedDist = 0F;
        for (CelestialBody childBody : focusBody.satellites) {
            if (childBody == null || childBody.semiMajorAxisKm <= 0F) {
                continue;
            }
            for (int i = 0; i <= 96; i++) {
                double meanAnomaly = 2D * Math.PI * ((double) i / 96D);
                float[] orbitOffset = calculateOrbitOffsetPx(focusBody, childBody, meanAnomaly, childOrbitScale);
                float projected = getProjectedOffsetDistanceAt1x(orbitOffset[0], orbitOffset[1]);
                if (projected <= 0F || Float.isNaN(projected) || Float.isInfinite(projected)) {
                    continue;
                }
                nearestProjectedDist = Math.min(nearestProjectedDist, projected);
                furthestProjectedDist = Math.max(furthestProjectedDist, projected);
            }
        }

        if (nearestProjectedDist == Float.MAX_VALUE || furthestProjectedDist <= 0F) {
            return false;
        }

        float availableRadiusPx = Math.max(1F, Math.min(MAP_W, MAP_H) * 0.5F - 10F);
        float minZoom = availableRadiusPx / furthestProjectedDist;
        float maxZoom = availableRadiusPx / nearestProjectedDist;

        minZoom = clampF(minZoom, MIN_ZOOM, MAX_ZOOM);
        maxZoom = clampF(maxZoom, MIN_ZOOM, MAX_ZOOM);
        if (maxZoom < minZoom) {
            float swap = minZoom;
            minZoom = maxZoom;
            maxZoom = swap;
        }

        outBounds[0] = minZoom;
        outBounds[1] = maxZoom;
        return true;
    }

    private float getProjectedOffsetDistanceAt1x(float offsetU, float offsetV) {
        float screenOffsetX = (offsetU - offsetV) * ISO_X;
        float screenOffsetY = (offsetU + offsetV) * ISO_Y;
        return Mth.sqrt(screenOffsetX * screenOffsetX + screenOffsetY * screenOffsetY);
    }

    private float getBodyDetailZoomCap(CelestialBody body) {
        if (body == null) {
            return MIN_ZOOM;
        }

        float bodySizeAt1x = isMoon(body) ? getMoonSizePxAt1x(body) : getBodySizePxAt1x(body);
        if (bodySizeAt1x <= 0F || Float.isNaN(bodySizeAt1x) || Float.isInfinite(bodySizeAt1x)) {
            return MIN_ZOOM;
        }

        float targetBodySizePx = Math.min(MAP_W, MAP_H) * 0.88F;
        float detailZoom = targetBodySizePx / bodySizeAt1x;
        return clampF(detailZoom, MIN_ZOOM, MAX_ZOOM);
    }

    private void clampMapUV() {
        float viewW = MAP_W / mapZoom;
        float viewH = MAP_H / mapZoom;
        float maxU = Math.max(0F, MAP_TEX_W - viewW);
        float maxV = Math.max(0F, MAP_TEX_H - viewH);
        mapU = clampF(mapU, 0F, maxU);
        mapV = clampF(mapV, 0F, maxV);
    }

    private float getOrbitScalePxPerKm(CelestialBody starBody) {
        float maxDistanceKm = getSystemMaxDistanceKm(starBody);
        if (maxDistanceKm <= 0F) {
            return 0F;
        }

        float starRadiusPxAt1x = getBodySizePxAt1x(starBody) * 0.5F;
        float availableRadiusPx = Math.max(0F, (SYSTEM_LAYOUT_REF_TEX * STARDAR_SYSTEM_RADIUS_FRACTION) - starRadiusPxAt1x);
        if (availableRadiusPx <= 0F) {
            return 0F;
        }

        return availableRadiusPx / maxDistanceKm;
    }

    private float getSystemMaxDistanceKm(CelestialBody starBody) {
        if (starBody == null || starBody.satellites == null) {
            return 0F;
        }

        float maxDistance = 0F;
        for (CelestialBody child : starBody.satellites) {
            if (child == null) {
                continue;
            }

            float apoapsisKm = child.semiMajorAxisKm * (1F + child.eccentricity);
            maxDistance = Math.max(maxDistance, Math.max(0F, apoapsisKm));
        }

        return maxDistance;
    }

    private float getChildOrbitScalePxPerKm(CelestialBody parent, float systemOrbitScalePxPerKm) {
        if (parent != null && parent.parent != null) {
            float moonOrbitScale = getMoonOrbitScalePxPerKm(parent, systemOrbitScalePxPerKm);
            if (moonOrbitScale > 0F) {
                return moonOrbitScale;
            }
        }
        return systemOrbitScalePxPerKm;
    }

    private float getMoonOrbitScalePxPerKm(CelestialBody parent, float systemOrbitScalePxPerKm) {
        if (parent == null || parent.satellites == null || parent.satellites.isEmpty()) {
            return 0F;
        }

        float maxMoonDistanceKm = 0F;
        for (CelestialBody moon : parent.satellites) {
            if (moon == null) continue;
            float apoapsisKm = moon.semiMajorAxisKm * (1F + moon.eccentricity);
            maxMoonDistanceKm = Math.max(maxMoonDistanceKm, Math.max(0F, apoapsisKm));
        }

        if (maxMoonDistanceKm <= 0F) {
            return 0F;
        }

        float parentRadiusPxAt1x = getBodySizePxAt1x(parent) * 0.5F;
        float moonOrbitRadiusPxAt1x = Math.max(10.0F, parentRadiusPxAt1x * 14F);

        float nearestSiblingGapKm = getNearestSiblingOrbitGapKm(parent);
        if (systemOrbitScalePxPerKm > 0F && nearestSiblingGapKm > 0F) {
            float siblingGapPxAt1x = nearestSiblingGapKm * systemOrbitScalePxPerKm;
            float maxAllowedMoonOrbitRadiusPxAt1x = siblingGapPxAt1x * 0.45F;
            if (maxAllowedMoonOrbitRadiusPxAt1x > 0F) {
                moonOrbitRadiusPxAt1x = Math.min(moonOrbitRadiusPxAt1x, maxAllowedMoonOrbitRadiusPxAt1x);
            }
        }

        return moonOrbitRadiusPxAt1x / maxMoonDistanceKm;
    }

    private float getNearestSiblingOrbitGapKm(CelestialBody body) {
        if (body == null || body.parent == null || body.semiMajorAxisKm <= 0F || body.parent.satellites == null) {
            return 0F;
        }

        float nearestGapKm = Float.MAX_VALUE;
        for (CelestialBody sibling : body.parent.satellites) {
            if (sibling == null || sibling == body || sibling.semiMajorAxisKm <= 0F) {
                continue;
            }
            float gapKm = Math.abs(sibling.semiMajorAxisKm - body.semiMajorAxisKm);
            if (gapKm > 0F) {
                nearestGapKm = Math.min(nearestGapKm, gapKm);
            }
        }

        return nearestGapKm == Float.MAX_VALUE ? 0F : nearestGapKm;
    }

    private float[] calculateOrbitOffsetPx(CelestialBody parent, CelestialBody body, double meanAnomaly, float orbitScalePxPerKm) {
        double eccentricAnomaly = calculateEccentricAnomaly(meanAnomaly, body.eccentricity);
        double semiMinorAxisFactor = body.semiMinorAxisFactor > 0 ? body.semiMinorAxisFactor : Math.sqrt(1D - (body.eccentricity * body.eccentricity));

        double x = body.semiMajorAxisKm * (Math.cos(eccentricAnomaly) - body.eccentricity);
        double y = body.semiMajorAxisKm * semiMinorAxisFactor * Math.sin(eccentricAnomaly);
        double z;

        double px = x;
        x = Math.cos(body.argumentPeriapsis) * px - Math.sin(body.argumentPeriapsis) * y;
        y = Math.sin(body.argumentPeriapsis) * px + Math.cos(body.argumentPeriapsis) * y;

        z = Math.sin(body.inclination) * y;
        y = Math.cos(body.inclination) * y;

        px = x;
        x = Math.cos(body.ascendingNode) * px - Math.sin(body.ascendingNode) * y;
        y = Math.sin(body.ascendingNode) * px + Math.cos(body.ascendingNode) * y;

        y -= z * 0.35D;
        y *= 0.8F;

        float mapX = (float) (x * orbitScalePxPerKm);
        float mapY = (float) (y * orbitScalePxPerKm);

        if (parent != null) {
            float parentVisualRadiusPx = getBodySizePxAt1x(parent) * 0.5F;
            float distanceFromParentPx = Mth.sqrt(mapX * mapX + mapY * mapY);
            if (distanceFromParentPx > 0F) {
                float radialScale = (distanceFromParentPx + parentVisualRadiusPx) / distanceFromParentPx;
                mapX *= radialScale;
                mapY *= radialScale;
            } else {
                mapX = parentVisualRadiusPx;
                mapY = 0F;
            }
        }

        return new float[] {mapX, mapY};
    }

    private double calculateMeanAnomaly(CelestialBody body, double worldTicks) {
        if (body == null) {
            return 0D;
        }

        double orbitPeriodTicks = getOrbitalPeriodTicks(body);
        if (orbitPeriodTicks <= 0D || Double.isNaN(orbitPeriodTicks) || Double.isInfinite(orbitPeriodTicks)) {
            return 0D;
        }

        return 2D * Math.PI * (worldTicks / orbitPeriodTicks);
    }

    private double getOrbitalPeriodTicks(CelestialBody body) {
        if (body == null || body.parent == null || body.semiMajorAxisKm <= 0F) {
            return 0D;
        }

        double orbitPeriodDays = body.getOrbitalPeriod();
        if (orbitPeriodDays <= 0D || Double.isNaN(orbitPeriodDays) || Double.isInfinite(orbitPeriodDays)) {
            return 0D;
        }

        return orbitPeriodDays * (double) AstronomyUtil.TICKS_IN_DAY;
    }

    private double calculateEccentricAnomaly(double meanAnomaly, float eccentricity) {
        double eccentricAnomaly = meanAnomaly;
        for (int i = 0; i < 4; i++) {
            eccentricAnomaly = meanAnomaly + eccentricity * Math.sin(eccentricAnomaly);
        }
        return eccentricAnomaly;
    }

    private float getBodyRotationPhase(CelestialBody body, double dayTicks) {
        if (body == null) {
            return 0F;
        }

        double period = body.getRotationalPeriod();
        if (period <= 0D || Double.isNaN(period) || Double.isInfinite(period)) {
            return 0F;
        }

        return (float) ((dayTicks % period) / period);
    }

    private float getBodySizePxAtZoom(CelestialBody body) {
        float bodySize = getBodySizePxAt1x(body) * mapZoom;
        CelestialBody renderFocusBody = getRenderFocusBody();
        if (renderFocusBody != null) {
            CelestialBody localOrbitParent = getLocalOrbitParent();
            boolean isLocalMoon = localOrbitParent != null && body.parent == localOrbitParent;
            if (isLocalMoon) {
                float moonSizeAt1x = getMoonSizePxAt1x(body);
                bodySize = moonSizeAt1x * mapZoom;
            }
        }
        return bodySize;
    }

    private float getBodySizePxAt1x(CelestialBody body) {
        if (body == null) {
            return 1F;
        }
        if (body.parent == null) {
            return 36F * STARDAR_BODY_SCALE;
        }
        if (isMoon(body)) {
            return getMoonSizePxAt1x(body);
        }
        float bodySizeAt1x = body.radiusKm * (36F / 261_600F) * 2.6F;
        bodySizeAt1x = clampF(bodySizeAt1x, 0.8F, 2.0F);
        return bodySizeAt1x * STARDAR_BODY_SCALE;
    }

    private float getMoonSizePxAt1x(CelestialBody moon) {
        if (moon == null) {
            return 0.2F * STARDAR_BODY_SCALE * STARDAR_MOON_VISUAL_SCALE;
        }
        float moonRadiusKm = Math.max(0F, moon.radiusKm);
        float radiusRangeKm = 500F - 65F;
        float t = (moonRadiusKm - 65F) / radiusRangeKm;
        t = clampF(t, 0F, 1F);
        return (0.2F + (0.5F - 0.2F) * t) * STARDAR_BODY_SCALE * STARDAR_MOON_VISUAL_SCALE;
    }

    private void enterLandingMode(CelestialBody body) {
        landingMode = true;
        landingBody = body;
        surfaceX = 0;
        surfaceY = 0;
        velocityX = 0;
        velocityY = 0;

        playButton();

        CompoundTag data = new CompoundTag();
        if (body.dimension != null) data.putString("pid", body.dimension.location().toString());

        if (star != null && star.getLevel() != null && body == currentBody && !CelestialBody.inOrbit(star.getLevel())) {
            data.putInt("ix", star.getBlockPos().getX());
            data.putInt("iz", star.getBlockPos().getZ());
        }

        sendControl(data);
    }

    private void exitLandingMode() {
        landingMode = false;
        landingBody = null;
        draggingSurface = false;
    }

    private void updateLandingModeFromDrive() {
        ItemStack slotStack = slotStack();

        if (slotStack.isEmpty()) {
            hadFullDrive = false;
            lastFullDriveMeta = -1;
            if (landingMode) {
                exitLandingMode();
            }
            return;
        }

        if (slotStack.getItem() != SpaceRocketItems.FULL_DRIVE.get()) {
            hadFullDrive = false;
            lastFullDriveMeta = -1;
            if (landingMode && slotStack.getItem() != SpaceMaterialItems.HARD_DRIVE.get()) {
                exitLandingMode();
            }
            return;
        }

        int meta = ItemVOTVdrive.getBody(slotStack).ordinal();
        if (hadFullDrive && meta == lastFullDriveMeta) return;

        hadFullDrive = true;
        lastFullDriveMeta = meta;

        CelestialBody body = ItemVOTVdrive.getBody(slotStack).getBody();
        if (landingMode && landingBody == body) return;

        if (body != null && body.canLand) {
            enterLandingMode(body);
        } else if (landingMode) {
            exitLandingMode();
        }
    }

    private String landingInfo(int x, int z) {
        int[] heightmap = heightmap();
        if (heightmap == null) return tr("gui.stardar.landing.no_heightmap");
        if (x < 3 || x > 252 || z < 3 || z > 252) return tr("gui.stardar.landing.outside_bounds");

        for (int ox = x - 2; ox <= x + 2; ox++) {
            for (int oz = z - 2; oz <= z + 2; oz++) {
                if (heightmap[256 * oz + ox] != heightmap[256 * z + x]) {
                    return tr("gui.stardar.landing.area_not_flat");
                }
            }
        }

        return null;
    }

    private int altitude(int x, int z) {
        int[] heightmap = heightmap();
        if (heightmap == null) return -1;
        if (x < 0 || x > 255 || z < 0 || z > 255) return -1;
        return heightmap[256 * z + x];
    }

    private String getBodyDisplayName(CelestialBody body) {
        if (body == null || body.name == null) return tr("gui.stardar.body.unknown");
        String key = "body." + body.name;
        String translated = I18n.get(key);
        if (translated != null && !translated.equals(key)) return translated;
        if (body.name.length() <= 1) return body.name.toUpperCase();
        return body.name.substring(0, 1).toUpperCase() + body.name.substring(1);
    }

    private static String formatValue(float value) {
        return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
    }

    private static String formatOrbitSpeed(float altitude) {
        return formatValue(Math.round(SatelliteOrbit.getOrbitSpeedKmPerSecond(altitude) * 10.0F) / 10.0F);
    }

    private static String formatPhaseOffset(float phaseOffset) {
        float rounded = Math.round(SatelliteOrbit.normalizePhaseOffset(phaseOffset) * 10.0F) / 10.0F;
        return formatValue(rounded);
    }

    private int colorArrayToRgb(float[] color) {
        if (color == null || color.length < 3) {
            return 0xFFFFFFFF;
        }
        int r = Mth.clamp((int) (color[0] * 255F), 0, 255);
        int gr = Mth.clamp((int) (color[1] * 255F), 0, 255);
        int b = Mth.clamp((int) (color[2] * 255F), 0, 255);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private int getBodyHierarchyDepth(CelestialBody body) {
        if (body == null) {
            return 0;
        }

        int depth = 0;
        CelestialBody current = body;
        while (current.parent != null && depth < 64) {
            depth++;
            current = current.parent;
        }
        return depth;
    }

    private static class BodyPosition {
        float mapU;
        float mapV;
    }

    private static class ChildRenderState {
        final CelestialBody childBody;
        final float orbitScalePxPerKm;
        final float orbitOffsetU;
        final float orbitOffsetV;
        final boolean moonChild;
        final boolean drawInFrontOfParent;

        ChildRenderState(CelestialBody childBody, float orbitScalePxPerKm, float orbitOffsetU, float orbitOffsetV, boolean moonChild, boolean drawInFrontOfParent) {
            this.childBody = childBody;
            this.orbitScalePxPerKm = orbitScalePxPerKm;
            this.orbitOffsetU = orbitOffsetU;
            this.orbitOffsetV = orbitOffsetV;
            this.moonChild = moonChild;
            this.drawInFrontOfParent = drawInFrontOfParent;
        }
    }

    private static class SatelliteOrbitPoint {
        final float offsetU;
        final float offsetV;
        final float depth;

        SatelliteOrbitPoint(float offsetU, float offsetV, float depth) {
            this.offsetU = offsetU;
            this.offsetV = offsetV;
            this.depth = depth;
        }
    }

    private static class BodyRenderInfo {
        final CelestialBody body;
        final float mapU;
        final float mapV;
        final float drawSize;

        BodyRenderInfo(CelestialBody body, float mapU, float mapV, float drawSize) {
            this.body = body;
            this.mapU = mapU;
            this.mapV = mapV;
            this.drawSize = drawSize;
        }
    }

    /** Original {@code SatelliteRenderInfo} (Satellit = Bahndaten {@link SatelliteOrbit}, Original {@code SatelliteBase}). */
    private static class SatelliteRenderInfo {
        final int frequency;
        final SatelliteOrbit satellite;
        final float mapU;
        final float mapV;
        final float drawSize;

        SatelliteRenderInfo(int frequency, SatelliteOrbit satellite, float mapU, float mapV, float drawSize) {
            this.frequency = frequency;
            this.satellite = satellite;
            this.mapU = mapU;
            this.mapV = mapV;
            this.drawSize = drawSize;
        }
    }

    private static class BodyLabelRenderInfo {
        final CelestialBody body;
        final float topRightAnchorScreenX;
        final float topRightAnchorScreenY;

        BodyLabelRenderInfo(CelestialBody body, float topRightAnchorScreenX, float topRightAnchorScreenY) {
            this.body = body;
            this.topRightAnchorScreenX = topRightAnchorScreenX;
            this.topRightAnchorScreenY = topRightAnchorScreenY;
        }
    }
}
