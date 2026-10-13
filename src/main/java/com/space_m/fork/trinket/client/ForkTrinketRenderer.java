package com.space_m.fork.trinket.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.fork.trinket.ForkTrinketBlock;
import com.space_m.fork.trinket.ForkTrinketBlockEntity;
import com.space_m.fork.trinket.ForkTrinkets;
import com.space_m.fork.trinket.ForkTrinkets.ForkBobbleType;
import com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Space {@code RenderBobble} (Typen GWEN/JUICE/JAMESH_2/MRKIMKIMORA) und {@code RenderPlushie} (TETO/MIKU/NERU/FATO),
 * Matrizenfolgen 1:1; GL-Zustaende wie HBM {@code TrinketRenderer} auf RenderTypes abgebildet. Modell
 * {@code bobble.obj} aus dem Space-Zweig (enthaelt den Teil {@code MrKimkimora}).
 */
public class ForkTrinketRenderer implements com.hbm_m.client.render.HbmBerBounds<ForkTrinketBlockEntity> {

    private static ResourceLocation rl(String path) { return SpaceMod.id(path); }

    public static final SimpleObjModel BOBBLE = new SimpleObjModel(rl("models/trinkets/bobble.obj"));
    public static final ResourceLocation SOCKET = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/models/trinkets/socket.png");
    public static final ResourceLocation BOBBLE_GWEN = rl("textures/models/trinkets/gwen.png");
    public static final ResourceLocation BOBBLE_JUICE = rl("textures/models/trinkets/juicy_lad.png");
    public static final ResourceLocation BOBBLE_JAMESH_2 = rl("textures/models/trinkets/jamesh_2.png");
    public static final ResourceLocation BOBBLE_MRKIMKIMORA = rl("textures/models/trinkets/mrkimkimora.png");
    public static final ResourceLocation UNIVERSAL = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/models/thegadget3_.png");

    // VOCALOIDS
    public static final SimpleObjModel TETO = new SimpleObjModel(rl("models/trinkets/teto.obj"));
    public static final ResourceLocation TETO_TEX = rl("textures/models/trinkets/teto.png");
    public static final ResourceLocation TETO_BLUSH = rl("textures/models/trinkets/teto_squished.png");
    public static final SimpleObjModel MIKU = new SimpleObjModel(rl("models/trinkets/miku.obj"));
    public static final ResourceLocation MIKU_TEX = rl("textures/models/trinkets/miku.png");
    public static final ResourceLocation MIKU_BLUSH = rl("textures/models/trinkets/miku_squished.png");
    public static final SimpleObjModel NERU = new SimpleObjModel(rl("models/trinkets/neru.obj"));
    public static final ResourceLocation NERU_TEX = rl("textures/models/trinkets/neru.png");
    public static final ResourceLocation NERU_BLUSH = rl("textures/models/trinkets/neru_squished.png");

    public ForkTrinketRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(ForkTrinketBlockEntity te, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState state = te.getBlockState();
        if (!(state.getBlock() instanceof ForkTrinketBlock block)) return;
        float rot = 22.5F * state.getValue(ForkTrinketBlock.ROTATION) + 90F;

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        if (block.kind == ForkTrinketBlock.Kind.BOBBLE) {
            ps.scale(0.25F, 0.25F, 0.25F);
            ps.mulPose(Axis.YN.rotationDegrees(rot));
            renderBobble(ps, buf, ForkTrinkets.safe(ForkBobbleType.class, te.type), light);
        } else {
            ps.mulPose(Axis.YN.rotationDegrees(rot));
            ForkPlushieType type = ForkTrinkets.safe(ForkPlushieType.class, te.type);
            if (te.miseryFactor > 0.01F) {
                ps.scale(1F, 1F - Mth.lerp(interp, te.lastMiseryFactor, te.miseryFactor), 1F);
            } else if (te.squishTimer > 0) {
                double squish = te.squishTimer - interp;
                ps.scale(1F, (float) (1 + (-(Math.sin(squish)) * squish) * 0.025), 1F);
            }
            switch (type) {
                case TETO, MIKU, NERU -> ps.scale(0.5F, 0.5F, 0.5F);
                case FATO -> ps.scale(1.0F, 0.5F, 1.0F);
                default -> { }
            }
            renderPlushie(ps, buf, type, te.squishTimer, light);
        }
        ps.popPose();
    }

    // =========================== Wackelkopf ===========================

    private static ResourceLocation skin(ForkBobbleType type) {
        return switch (type) {
            case GWEN -> BOBBLE_GWEN;
            case JUICE -> BOBBLE_JUICE;
            case JAMESH_2 -> BOBBLE_JAMESH_2;
            case MRKIMKIMORA -> BOBBLE_MRKIMKIMORA;
            default -> UNIVERSAL;
        };
    }

    public static void renderBobble(PoseStack ps, MultiBufferSource buf, ForkBobbleType type, int light) {
        long time = System.currentTimeMillis();
        BOBBLE.renderPart("Socket", ps, buf.getBuffer(RenderType.entityCutout(SOCKET)), light);

        ResourceLocation skin = skin(type);
        if (type == ForkBobbleType.MRKIMKIMORA) {
            BOBBLE.renderPart("MrKimkimora", ps, buf.getBuffer(RenderType.entityCutout(skin)), light);
        } else {
            renderGuy(ps, buf, type, skin, light, time);
        }
        // renderPost: keine Zusaetze fuer die Fork-Typen
        renderSocket(ps, buf, type, light);
    }

    /** {@code renderGuy} ohne Figurenhaltung (die Fork-Typen haben in {@code setupFigurineRotation} keinen Eintrag). */
    public static void renderGuy(PoseStack ps, MultiBufferSource buf, ForkBobbleType type, ResourceLocation tex, int light, long time) {
        ps.pushPose();
        var vc = buf.getBuffer(RenderType.entityTranslucent(tex));
        String suffix = type.skinLayers ? "" : "17";

        BOBBLE.renderPart("LL" + suffix, ps, vc, light);
        BOBBLE.renderPart("RL" + suffix, ps, vc, light);
        BOBBLE.renderPart("LA" + suffix, ps, vc, light);
        BOBBLE.renderPart("RA" + suffix, ps, vc, light);
        BOBBLE.renderPart("Body" + suffix, ps, vc, light);

        double speed = 0.005, amplitude = 1;
        ps.pushPose();
        ps.translate(0, 1.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(time * speed) * amplitude)));
        ps.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(time * speed + (Math.PI * 0.5)) * amplitude)));
        ps.translate(0, -1.75, 0);
        BOBBLE.renderPart("Head" + suffix, ps, vc, light);
        ps.popPose();
        ps.popPose();
    }

    private static void renderSocket(PoseStack ps, MultiBufferSource buf, ForkBobbleType type, int light) {
        Font font = Minecraft.getInstance().font;
        float f3 = 0.01F;
        ps.pushPose();
        ps.translate(0.63, 0.175F, 0.0);
        ps.scale(f3, -f3, f3);
        ps.translate(0, 0, font.width(type.label) * 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.translate(0, 1, 0);
        font.drawInBatch(type.label, 0, 0, 0xffffff, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        ps.popPose();
    }

    // =========================== Plueschtier ===========================

    public static void renderPlushie(PoseStack ps, MultiBufferSource buf, ForkPlushieType type, int squishTimer, int light) {
        switch (type) {
            case TETO, FATO -> TETO.renderAll(ps, buf.getBuffer(RenderType.entityCutout(squishTimer > 6 ? TETO_BLUSH : TETO_TEX)), light);
            case MIKU -> MIKU.renderAll(ps, buf.getBuffer(RenderType.entityCutout(squishTimer > 6 ? MIKU_BLUSH : MIKU_TEX)), light);
            case NERU -> NERU.renderAll(ps, buf.getBuffer(RenderType.entityCutout(squishTimer > 6 ? NERU_BLUSH : NERU_TEX)), light);
            default -> { }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ForkTrinketBlockEntity te) {
        return false;
    }
}
