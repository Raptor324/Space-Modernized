package com.space_m.rocket.transport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.rocket.transport.EntityCombatDropPod;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderCombatDropPod}: fallende Kapsel als ganzes {@code combat_drop_pod}-Modell mit Farbhaut
 * (0 weiss, 1 rot, 2 gelb, sonst weiss), Rueckseiten ausgeblendet.
 * Original-Klasse: RenderCombatDropPod
 */
public class CombatDropPodRenderer extends EntityRenderer<EntityCombatDropPod> {

    public CombatDropPodRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityCombatDropPod pod, float yaw, float interp, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        int height = 7;
        ps.translate(0.0F, height, 0.0F);
        ps.translate(0.0F, -height, 0.0F);

        CombatPodRenderer.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(CombatPodRenderer.skin(pod.getColor()))), light);

        ps.popPose();
        super.render(pod, yaw, interp, ps, buf, light);
    }

    @Override
    public boolean shouldRender(EntityCombatDropPod entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true; // ignoreFrustumCheck
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCombatDropPod entity) {
        return CombatPodRenderer.SKIN_YELLOW;
    }
}
