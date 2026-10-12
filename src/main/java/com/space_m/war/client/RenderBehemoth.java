package com.space_m.war.client;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.war.EntityWarBehemoth;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1:1 NTM Space {@code RenderBehemoth} + {@code ModelBehemoth} ({@code war_machine.obj}): Rumpf/Luken/Geschuetz/Helm/
 * Auge neigen sich zum Ziel (Original {@code setLivingAnimations}: Neigung += Differenz * 0,1 je Bild), die Beine
 * laufen ueber eine Zwei-Gelenk-IK (Oberschenkel 6, Schienbein 8) im Takt des Laufzyklus.
 * Grundtransformation wie {@code RenderLiving} (180 - Koerperdrehung, Spiegelung -1/-1/1, -24/16 - 1/128) +
 * Modell (X 180, -1,5). Treffer-Rot ueber das Overlay wie bei Lebewesen.
 *
 * <p>Abweichung: Das Original verstellt im Modell zusaetzlich {@code newRotationYaw} (Client-Interpolationsziel
 * der Koerperdrehung); dieses Feld ist in 1.20 privat - die Drehung kommt daher nur vom Server (KI dreht den
 * Koerper mit dem Kopf).
 */
public class RenderBehemoth extends EntityRenderer<EntityWarBehemoth> {

    public static final SimpleObjModel BEHEMOTH = new SimpleObjModel(SpaceMod.id("models/mobs/war_machine.obj"));

    public static final ResourceLocation BODY = SpaceMod.id("textures/entity/wm_body.png");
    public static final ResourceLocation EYE = SpaceMod.id("textures/entity/wm_eye.png");
    public static final ResourceLocation HELMET = SpaceMod.id("textures/entity/wm_helmet.png");
    public static final ResourceLocation LEG = SpaceMod.id("textures/entity/wm_leg.png");
    public static final ResourceLocation KNEE = SpaceMod.id("textures/entity/wm_knee.png");
    public static final ResourceLocation HIP = SpaceMod.id("textures/entity/wm_hip.png");

    public RenderBehemoth(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F; // Original shadowOpaque = 0
    }

    @Override
    public void render(@NotNull EntityWarBehemoth behemoth, float yaw, float interp, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
        setLivingAnimations(behemoth);

        int overlay = LivingEntityRenderer.getOverlayCoords(behemoth, 0F);
        float bodyYaw = Mth.rotLerp(interp, behemoth.yBodyRotO, behemoth.yBodyRot);
        float limbSwing = behemoth.walkAnimation.position(interp);

        ps.pushPose();
        // RenderLiving: rotateCorpse + glScalef(-1, -1, 1) + glTranslatef(0, -24 * 0.0625 - 0.0078125, 0)
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.5078125F, 0.0F);

        // ModelBehemoth.render
        ps.mulPose(Axis.XP.rotationDegrees(180));
        ps.translate(0, -1.5F, 0);

        renderModel(behemoth, limbSwing, ps, buffers, light, overlay);

        ps.popPose();
        super.render(behemoth, yaw, interp, ps, buffers, light);
    }

    /** Original {@code ModelBehemoth.setLivingAnimations}: Rumpf zum Ziel neigen. */
    private static void setLivingAnimations(EntityWarBehemoth behemoth) {
        int targetId = behemoth.getTargetId();
        Entity target = targetId != 0 ? behemoth.level().getEntity(targetId) : null;
        if (target instanceof LivingEntity targetBase) {
            double dx = targetBase.getX() - behemoth.getX();
            double dy = (targetBase.getY() - targetBase.getEyeHeight()) - (behemoth.getY() + behemoth.getEyeHeight());
            double dz = targetBase.getZ() - behemoth.getZ();

            // Calculate Pitch (vertical rotation)
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz); // Distance in XZ plane
            double targetPitch = Math.atan2(dy, horizontalDistance) * (-180 / Math.PI);
            double deltaPitch = Mth.wrapDegrees(targetPitch - behemoth.renderPitch);
            behemoth.renderPitch += (float) (deltaPitch * 0.1); // Smooth pitch rotation
        }
    }

    private void renderModel(EntityWarBehemoth behemoth, float limbSwing, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();

        double s = 1.2;
        ps.scale((float) s, (float) s, (float) s);

        double walkCycle = limbSwing;

        ps.translate(0, -0.5F, 0F);
        ps.mulPose(Axis.YP.rotationDegrees(-180));

        ps.pushPose();
        ps.translate(0, 8.6F, 0.5F);
        ps.mulPose(Axis.XP.rotationDegrees(-behemoth.renderPitch)); // Apply pitch rotation
        ps.translate(0, -8.7F, -0.5F);
        part("body", BODY, ps, buffers, light, overlay);
        part("hatch1", BODY, ps, buffers, light, overlay);
        part("hatch2", BODY, ps, buffers, light, overlay);
        part("gun", BODY, ps, buffers, light, overlay);
        part("helmet", HELMET, ps, buffers, light, overlay);
        part("eye", EYE, ps, buffers, light, overlay);
        ps.popPose();

        double stepHeight = 5.5;
        double stepLength = 2.5;
        double stepTime = walkCycle * 0.25;

        double leftFootY = Math.sin(stepTime) * stepHeight - 2.0;
        double leftFootX = Math.sin(stepTime) * stepLength + 5.0;
        double leftFootZ = Math.cos(stepTime) * stepLength + 1.5;

        double rightFootY = Math.sin(stepTime + Math.PI) * stepHeight - 4.0;
        double rightFootX = Math.sin(stepTime + Math.PI) * stepLength + 5.0;
        double rightFootZ = Math.cos(stepTime + Math.PI) * stepLength + 1.5;

        leftFootY = Math.max(leftFootY, 7.0);
        rightFootY = Math.max(rightFootY, 7.0);

        ps.pushPose(); // RIGHT LEG MATRIX START
        renderLegIK("right", rightFootX, rightFootY, rightFootZ, ps, buffers, light, overlay);
        ps.popPose(); // RIGHT LEG MATRIX END

        ps.pushPose(); // LEFT LEG MATRIX START
        renderLegIK("left", leftFootX, leftFootY, leftFootZ, ps, buffers, light, overlay);
        ps.popPose(); // LEFT LEG MATRIX END

        ps.popPose();
    }

    private void renderLegIK(String leg, double targetFootX, double targetFootY, double targetFootZ, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        // Calculate leg IK based on the target foot position
        double thighLength = 6.0;
        double shinLength = 8.0;

        double deltaX = targetFootX;
        double deltaY = targetFootY - 8.5; // Hip height offset
        double deltaZ = targetFootZ - 1.5; // Foot Z offset

        double distanceToTarget = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        double cosKneeAngle = (thighLength * thighLength + shinLength * shinLength - distanceToTarget * distanceToTarget) / (2 * thighLength * shinLength);
        double kneeAngle = Math.acos(cosKneeAngle);
        double hipAngle = Math.atan2(deltaY, deltaX) - Math.atan2(shinLength * Math.sin(kneeAngle), thighLength + shinLength * Math.cos(kneeAngle));

        ps.translate(0, 8.5F, 0.5F);
        ps.mulPose(Axis.XP.rotationDegrees((float) (hipAngle * (180.0 / Math.PI))));
        ps.translate(0, -8.6F, -0.5F);
        part(leg.equals("right") ? "hip_right" : "hip_left", HIP, ps, buffers, light, overlay);
        ps.translate(0, 8.6F, 0.5F);

        ps.pushPose(); // KNEE MATRIX START
        ps.translate(0, -1.8F, -0F);
        ps.mulPose(Axis.XP.rotationDegrees((float) (kneeAngle * (180.0 / Math.PI)))); // Knee rotation

        ps.translate(0, -6.7F, -0.5F);
        part(leg.equals("right") ? "knee_right" : "knee_left", KNEE, ps, buffers, light, overlay);
        ps.translate(0, 6.7F, 0.5F);

        ps.translate(0, -5.6F, 0F);
        ps.mulPose(Axis.XP.rotationDegrees((float) (cosKneeAngle * (-90.0 / Math.PI))));
        ps.translate(0, -1F, -0.5F);
        part(leg.equals("right") ? "leg_right" : "leg_left", LEG, ps, buffers, light, overlay);

        ps.translate(0, 1F, 0.5F);
        ps.popPose(); // KNEE MATRIX END
    }

    private static void part(String name, ResourceLocation tex, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BEHEMOTH.renderPartEntity(name, ps, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay, 1F, 1F, 1F, 1F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityWarBehemoth entity) {
        return BODY;
    }
}
