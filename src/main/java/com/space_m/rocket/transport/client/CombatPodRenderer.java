package com.space_m.rocket.transport.client;

import java.util.Random;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.rocket.transport.CombatDropPodBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderCombatPod}: gelandete Kapsel, zufaellig (Seed = Blockposition) gedreht (Gier 0-360, Nick
 * 0-8, Roll 0-6 Grad), Luken {@code hatch1}/{@code hatch2} klappen mit {@code hatchopen}/{@code hatchopen2} auf.
 * Farbe 0 weiss, 1 rot, 2 gelb, sonst weiss.
 * Original-Klasse: RenderCombatPod
 */
public class CombatPodRenderer implements HbmBerBounds<CombatDropPodBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/combat_drop_pod.obj"));
    public static final ResourceLocation SKIN_BLUE = SpaceMod.id("textures/models/bombs/combat_drop_pod.blue.png");
    public static final ResourceLocation SKIN_YELLOW = SpaceMod.id("textures/models/bombs/combat_drop_pod.yellow.png");
    public static final ResourceLocation SKIN_WHITE = SpaceMod.id("textures/models/bombs/combat_drop_pod.white.png");
    public static final ResourceLocation SKIN_GREEN = SpaceMod.id("textures/models/bombs/combat_drop_pod.green.png");
    public static final ResourceLocation SKIN_RED = SpaceMod.id("textures/models/bombs/combat_drop_pod.red.png");

    public static final Random rand = new Random();

    public CombatPodRenderer(BlockEntityRendererProvider.Context ctx) { }

    public static ResourceLocation skin(int color) {
        return switch (color) {
            case 1 -> SKIN_RED;
            case 2 -> SKIN_YELLOW;
            default -> SKIN_WHITE;
        };
    }

    /** {@code fauxpointtwelve.BlockPos.getIdentity}. */
    private static int identity(BlockPos p) {
        return (p.getY() + p.getZ() * 27644437) * 27644437 + p.getX();
    }

    @Override
    public void render(CombatDropPodBlockEntity pod, float i, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 1.5D, 0.5D);

        ps.translate(0.0F, -0.25F, 0.0F);
        rand.setSeed(identity(pod.getBlockPos()));
        double yaw = rand.nextDouble() * 360;
        double pitch = rand.nextDouble() * 8;
        double roll = rand.nextDouble() * 6;

        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
        ps.mulPose(Axis.XP.rotationDegrees((float) pitch));
        ps.mulPose(Axis.ZP.rotationDegrees((float) roll));

        double open = pod.prevHatchopen + (pod.hatchopen - pod.prevHatchopen) * i;
        double open2 = pod.prevHatchopen2 + (pod.hatchopen2 - pod.prevHatchopen2) * i;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(skin(pod.color)));

        ps.pushPose();
        ps.translate(0, open * 0.013, open * 0.01);
        ps.mulPose(Axis.XP.rotationDegrees((float) open));
        MODEL.renderPart("hatch1", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -open2 * 0.013, -open * 0.01);
        ps.mulPose(Axis.XP.rotationDegrees((float) open2));
        MODEL.renderPart("hatch2", ps, vc, light);
        ps.popPose();

        MODEL.renderPart("bomb", ps, vc, light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(CombatDropPodBlockEntity be) { return true; }
}
