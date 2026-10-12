package com.space_m.entity.mob.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.space_m.SpaceMod;
import com.space_m.entity.mob.EntityTankbot;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import static com.space_m.entity.mob.client.SpaceMobModels.rx;
import static com.space_m.entity.mob.client.SpaceMobModels.ry;
import static com.space_m.entity.mob.client.SpaceMobModels.rz;

/**
 * 1:1 {@code RenderTankbot} + {@code ModelTankbot}: {@code tankbot.obj} (Textur {@code robtex}) doppelt gross, vier
 * Beine im Laufzyklus, Kopf mit Rohren folgt geglaettet dem Ziel (synchronisierte Ziel-ID, 10 % je Bild). Schatten 1.0
 * ohne Deckkraft ({@code shadowOpaque = 0}).
 */
public class RenderTankbot extends MobRenderer<EntityTankbot, RenderTankbot.ModelTankbot> {

    public static final ResourceLocation TEXTURE = SpaceMod.id("textures/entity/robtex.png");
    static final SimpleObjModel TANKBOT = ObjMobModel.obj("tankbot");

    public RenderTankbot(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelTankbot(), 1.0F);
        this.shadowStrength = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTankbot entity) {
        return TEXTURE;
    }

    public static class ModelTankbot extends ObjMobModel<EntityTankbot> {

        double bite = 0;

        @Override
        public void prepareMobModel(EntityTankbot tankbot, float limbSwing, float limbSwingAmount, float interp) {
            super.prepareMobModel(tankbot, limbSwing, limbSwingAmount, interp);
            bite = tankbot.getAttackAnim(interp);

            Entity targetBase = tankbot.level().getEntity(tankbot.getTargetId());
            if (targetBase != null && tankbot.getTargetId() != 0) {
                double dx = targetBase.getX() - tankbot.getX();
                double dz = targetBase.getZ() - tankbot.getZ();

                double targetYaw = Math.atan2(dz, dx) * (180 / Math.PI) - 90;
                double deltaYaw = Mth.wrapDegrees(targetYaw - tankbot.headTargetYaw);
                tankbot.headTargetYaw += deltaYaw * 0.1; // Adjust the multiplier for smoothing effect
            }
        }

        @Override
        protected void draw(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float b, float a) {
            rx(ps, 180);
            ps.translate(0, -1.5F, 0);

            ps.pushPose();
            double s = 2;
            ps.scale((float) s, (float) s, (float) s);

            double walkCycle = limbSwing;
            double cy2 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI);
            double cy3 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.75);

            double bite = entity.headTargetYaw;
            SimpleObjModel m = TANKBOT;

            part(m, "body", ps, vc, l, o, r, g, b, a);

            ps.pushPose();

            ps.pushPose();
            ps.translate(0.25, 0.625, 0.0625);
            ry(ps, 10);
            ry(ps, -cy3 * 10);
            rz(ps, cy2 * -2);
            ps.translate(-0.25, -0.625, -0.0625);
            part(m, "front2foot", ps, vc, l, o, r, g, b, a);
            part(m, "front2leg", ps, vc, l, o, r, g, b, a);
            part(m, "front2knee", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            ps.pushPose();
            ps.translate(0.25, 0.625, 0.4375);
            ry(ps, cy3 - cy2 * 10);
            rz(ps, cy2 - cy3 * -2);
            ps.translate(-0.25, -0.625, -0.4375);
            part(m, "front1foot", ps, vc, l, o, r, g, b, a);
            part(m, "front1leg", ps, vc, l, o, r, g, b, a);
            part(m, "front1knee", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            ps.pushPose();
            ps.translate(0.25, 0.625, 0.9375);
            ry(ps, cy3 - cy2 * 10);
            rz(ps, cy2 - cy3 * -2);
            ps.translate(-0.25, -0.625, -0.9375);
            part(m, "back1foot", ps, vc, l, o, r, g, b, a);
            part(m, "back1leg", ps, vc, l, o, r, g, b, a);
            part(m, "back1knee", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            ps.pushPose();
            ps.translate(0.25, 0.625, 0.9375);
            ry(ps, cy2 - cy3 * 5);
            rx(ps, cy2 * 2);
            ps.translate(-0.25, -0.625, -0.9375);
            part(m, "back2foot", ps, vc, l, o, r, g, b, a);
            part(m, "back2leg", ps, vc, l, o, r, g, b, a);
            part(m, "back2knee", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            ps.popPose();

            ps.pushPose();
            // "this makes NO SENSE but its working okay for some reason..." (Original)
            double bier = entity.getYHeadRot();
            ry(ps, -bite + bier);
            ry(ps, -90);
            part(m, "head", ps, vc, l, o, r, g, b, a);
            part(m, "barrelbottom", ps, vc, l, o, r, g, b, a);
            part(m, "barreltop", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            ps.popPose();
        }
    }
}
