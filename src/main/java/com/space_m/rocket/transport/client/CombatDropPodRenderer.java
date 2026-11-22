package com.space_m.rocket.transport.client;

import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.rocket.client.RocketModels;
import com.space_m.rocket.transport.EntityCombatDropPod;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderCombatDropPod}: fallende Kapsel als ganzes {@code combat_drop_pod}-Modell mit Farbhaut
 * (0 weiss, 1 rot, 2 gelb, sonst weiss). Modernisiert auf GPU VBO-Rendering via SingleMeshVboRenderer.
 * Original-Klasse: RenderCombatDropPod
 */
public class CombatDropPodRenderer extends EntityRenderer<EntityCombatDropPod> {

    public static final String MODEL_LOC = "space_m:models/combat_drop_pod.obj";
    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/combat_drop_pod.obj"));
    public static final ResourceLocation SKIN_BLUE = SpaceMod.id("textures/models/bombs/combat_drop_pod.blue.png");
    public static final ResourceLocation SKIN_YELLOW = SpaceMod.id("textures/models/bombs/combat_drop_pod.yellow.png");
    public static final ResourceLocation SKIN_WHITE = SpaceMod.id("textures/models/bombs/combat_drop_pod.white.png");
    public static final ResourceLocation SKIN_GREEN = SpaceMod.id("textures/models/bombs/combat_drop_pod.green.png");
    public static final ResourceLocation SKIN_RED = SpaceMod.id("textures/models/bombs/combat_drop_pod.red.png");

    public static ResourceLocation skin(int color) {
        return switch (color) {
            case 1 -> SKIN_RED;
            case 2 -> SKIN_YELLOW;
            default -> SKIN_WHITE;
        };
    }

    public CombatDropPodRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityCombatDropPod pod, float yaw, float interp, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        int height = 7;
        ps.translate(0.0F, height, 0.0F);
        ps.translate(0.0F, -height, 0.0F);

        SingleMeshVboRenderer.setEntityMissileDepthBias(true);
        try {
            ResourceLocation texture = skin(pod.getColor());
            SingleMeshVboRenderer vbo = RocketModels.getVbo(MODEL_LOC, null, texture);
            if (vbo != null) {
                vbo.render(ps, light, pod.blockPosition(), null, buf);
            } else {
                MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(texture)), light);
            }
        } finally {
            SingleMeshVboRenderer.setEntityMissileDepthBias(false);
        }

        ps.popPose();
        super.render(pod, yaw, interp, ps, buf, light);
    }

    public static void renderPartVbo(String part, PoseStack ps, MultiBufferSource buf, int light, ResourceLocation texture, BlockPos pos) {
        SingleMeshVboRenderer vbo = RocketModels.getVbo(MODEL_LOC, part, texture);
        if (vbo != null) {
            vbo.render(ps, light, pos, null, buf);
        } else {
            MODEL.renderPart(part, ps, buf.getBuffer(RenderType.entityCutout(texture)), light);
        }
    }

    public static void renderDropPodParts(PoseStack ps, MultiBufferSource buf, int light, int color, double open, double open2, BlockPos pos) {
        ResourceLocation texture = skin(color);

        ps.pushPose();
        ps.translate(0, open * 0.013, open * 0.01);
        ps.mulPose(Axis.XP.rotationDegrees((float) open));
        renderPartVbo("hatch1", ps, buf, light, texture, pos);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -open2 * 0.013, -open * 0.01);
        ps.mulPose(Axis.XP.rotationDegrees((float) open2));
        renderPartVbo("hatch2", ps, buf, light, texture, pos);
        ps.popPose();

        renderPartVbo("bomb", ps, buf, light, texture, pos);
    }

    @Override
    public boolean shouldRender(EntityCombatDropPod entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true; // ignoreFrustumCheck
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCombatDropPod entity) {
        return SKIN_YELLOW;
    }
}
