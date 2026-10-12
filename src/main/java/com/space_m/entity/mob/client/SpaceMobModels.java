package com.space_m.entity.mob.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.entity.mob.EntityDepthSquid;
import com.space_m.entity.mob.EntityFish;
import com.space_m.entity.mob.EntityScuttlecrab;

/**
 * 1:1 {@code ModelScutter}, {@code ModelScuttlecrab}, {@code ModelScrapFish}, {@code ModelSifterEel},
 * {@code ModelDepthSquid} (NTM Space): OBJ-Teile mit Schwimm-/Laufzyklen aus {@code limbSwing}.
 */
// Original-Klasse: ModelScutter
// Original-Klasse: ModelScuttlecrab
// Original-Klasse: ModelScrapFish
// Original-Klasse: ModelSifterEel
// Original-Klasse: ModelDepthSquid
public final class SpaceMobModels {

    private SpaceMobModels() {}

    static final SimpleObjModel SCUTTERFISH = ObjMobModel.obj("scutterfish");
    static final SimpleObjModel SCUTTLECRAB = ObjMobModel.obj("scuttlecrab");
    static final SimpleObjModel SCRAPFISH = ObjMobModel.obj("scrapfish");
    static final SimpleObjModel DEPTHSQUID = ObjMobModel.obj("depthsquid");
    static final SimpleObjModel SIFTER_EEL = ObjMobModel.obj("siftereel");

    static void rx(PoseStack ps, double deg) { ps.mulPose(Axis.XP.rotationDegrees((float) deg)); }
    static void ry(PoseStack ps, double deg) { ps.mulPose(Axis.YP.rotationDegrees((float) deg)); }
    static void rz(PoseStack ps, double deg) { ps.mulPose(Axis.ZP.rotationDegrees((float) deg)); }

    /** {@code ModelScutter}. */
    public static class Scutter<T extends EntityFish> extends ObjMobModel<T> {
        @Override
        protected void draw(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float b, float a) {
            SimpleObjModel m = SCUTTERFISH;
            double cy0 = Math.sin(limbSwing % (Math.PI * 2));
            double cy1 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.2);
            double cy2 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.4);
            double cy3 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.6);

            rz(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            part(m, "body", ps, vc, l, o, r, g, b, a);

            ps.pushPose();
            rx(ps, headPitch);
            ry(ps, netHeadYaw);
            part(m, "head", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            String[] left = {"leftfin1", "leftfin2", "leftfin3"};
            String[] right = {"rightfin1", "rightfin2", "rightfin3"};
            double[] cy = {cy0, cy1, cy2};
            for (int i = 0; i < 3; i++) {
                ps.pushPose(); rz(ps, cy[i] * 20); part(m, left[i], ps, vc, l, o, r, g, b, a); ps.popPose();
                ps.pushPose(); rz(ps, cy[i] * -20); part(m, right[i], ps, vc, l, o, r, g, b, a); ps.popPose();
            }

            ps.pushPose();
            rz(ps, cy1 * 10);
            rx(ps, cy2 * -5);
            part(m, "tailmeat", ps, vc, l, o, r, g, b, a);
            rx(ps, cy3 * -5);
            part(m, "tailthin", ps, vc, l, o, r, g, b, a);
            ps.popPose();
        }
    }

    /** {@code ModelScuttlecrab}. */
    public static class Scuttlecrab extends ObjMobModel<EntityScuttlecrab> {
        @Override
        protected void draw(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float b, float a) {
            SimpleObjModel m = SCUTTLECRAB;
            double ls = limbSwing * 6;
            double cy0 = Math.sin(ls % (Math.PI * 2));
            double cy1 = Math.sin(ls % (Math.PI * 2) - Math.PI * 0.4);
            double cy2 = Math.sin(ls % (Math.PI * 2) - Math.PI * 0.8);
            double cy3 = Math.sin(ls % (Math.PI * 2) - Math.PI * 1.2);
            double cy4 = Math.sin(ls % (Math.PI * 2) - Math.PI * 1.6);

            rz(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            part(m, "Body", ps, vc, l, o, r, g, b, a);

            ps.pushPose();
            ps.translate(-0.125F, 0.5625F, -0.3125F);
            rx(ps, headPitch);
            ry(ps, netHeadYaw);
            ps.translate(0.125F, -0.5625F, 0.3125F);
            part(m, "EyeL", ps, vc, l, o, r, g, b, a);
            ps.popPose();
            ps.pushPose();
            ps.translate(0.125F, 0.5625F, -0.3125F);
            rx(ps, headPitch);
            ry(ps, netHeadYaw);
            ps.translate(-0.125F, -0.5625F, 0.3125F);
            part(m, "EyeR", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            part(m, "ClawL", ps, vc, l, o, r, g, b, a);
            part(m, "ClawR", ps, vc, l, o, r, g, b, a);

            double[] left = {cy0, cy1, cy2, cy3};
            double[] right = {cy1, cy2, cy3, cy4};
            for (int i = 0; i < 4; i++) {
                ps.pushPose(); rz(ps, left[i] * 8); part(m, "LegL" + (i + 1), ps, vc, l, o, r, g, b, a); ps.popPose();
            }
            for (int i = 0; i < 4; i++) {
                ps.pushPose(); rz(ps, right[i] * -8); part(m, "LegR" + (i + 1), ps, vc, l, o, r, g, b, a); ps.popPose();
            }
        }
    }

    /** {@code ModelScrapFish}. */
    public static class ScrapFish<T extends EntityFish> extends ObjMobModel<T> {
        @Override
        protected void draw(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float b, float a) {
            SimpleObjModel m = SCRAPFISH;
            double t = limbSwing % (Math.PI * 2);
            double cy0 = Math.sin(t);
            double cy1 = Math.sin(t - Math.PI * 0.2);
            double cy2 = Math.sin(t - Math.PI * 0.4);
            double cy3 = Math.sin(t - Math.PI * 0.6);
            double cy4 = Math.sin(t - Math.PI * 0.8);
            double cy5 = Math.sin(t - Math.PI * 1.0);

            rz(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            part(m, "Body", ps, vc, l, o, r, g, b, a);

            ps.pushPose();
            ps.translate(0, 0, -0.3125F);
            rx(ps, headPitch);
            ry(ps, netHeadYaw);
            ps.translate(0, 0, 0.3125F);
            part(m, "Head", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            ps.pushPose(); rx(ps, cy0 * 20); part(m, "FinL", ps, vc, l, o, r, g, b, a); ps.popPose();
            ps.pushPose(); rx(ps, cy0 * -20); part(m, "FinR", ps, vc, l, o, r, g, b, a); ps.popPose();
            ps.pushPose(); rx(ps, cy1 * 20); part(m, "VentralFL", ps, vc, l, o, r, g, b, a); ps.popPose();
            ps.pushPose(); rx(ps, cy1 * -20); part(m, "VentralFR", ps, vc, l, o, r, g, b, a); ps.popPose();
            ps.pushPose(); rx(ps, cy2 * 20); part(m, "VentralBL", ps, vc, l, o, r, g, b, a); ps.popPose();
            ps.pushPose(); rx(ps, cy2 * -20); part(m, "VentralBR", ps, vc, l, o, r, g, b, a); ps.popPose();

            ps.pushPose();
            ry(ps, cy3 * 10);
            part(m, "Dorsal", ps, vc, l, o, r, g, b, a);
            ry(ps, cy4 * 6);
            part(m, "Tail", ps, vc, l, o, r, g, b, a);
            ry(ps, cy5 * 4);
            part(m, "TailFin", ps, vc, l, o, r, g, b, a);
            ps.popPose();
        }
    }

    /** {@code ModelSifterEel}. */
    public static class SifterEel<T extends EntityFish> extends ObjMobModel<T> {
        @Override
        protected void draw(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float b, float a) {
            SimpleObjModel m = SIFTER_EEL;
            double t = limbSwing % (Math.PI * 2);
            double cy0 = Math.sin(t);
            double cy1 = Math.sin(t - Math.PI * 0.2);
            double cy2 = Math.sin(t - Math.PI * 0.4);
            double cy3 = Math.sin(t - Math.PI * 0.6);
            double cy4 = Math.sin(t - Math.PI * 0.9);
            double cy5 = Math.sin(t - Math.PI * 1.2);
            double cy6 = Math.sin(t - Math.PI * 1.5);
            double cy7 = Math.sin(t - Math.PI * 1.9);

            rz(ps, 180.0F);
            ps.translate(0, -1.5F, 0);

            ps.pushPose();
            ps.translate(0, 0, -0.5F);
            rx(ps, headPitch);
            ry(ps, netHeadYaw);
            ps.translate(0, 0, 0.5F);
            part(m, "Head", ps, vc, l, o, r, g, b, a);
            part(m, "Jaw", ps, vc, l, o, r, g, b, a);
            ps.popPose();

            ps.pushPose(); rx(ps, cy0 * 20); part(m, "FinL", ps, vc, l, o, r, g, b, a); ps.popPose();
            ps.pushPose(); rx(ps, cy0 * -20); part(m, "FinR", ps, vc, l, o, r, g, b, a); ps.popPose();

            ps.pushPose();
            part(m, "Body1", ps, vc, l, o, r, g, b, a);
            ry(ps, cy1 * 10);
            ry(ps, cy2 * -2);
            part(m, "Dorsal1", ps, vc, l, o, r, g, b, a);
            part(m, "Body2", ps, vc, l, o, r, g, b, a);
            ry(ps, cy3 * -2);
            part(m, "Dorsal2", ps, vc, l, o, r, g, b, a);
            part(m, "Dorsal3", ps, vc, l, o, r, g, b, a);
            ry(ps, cy4 * -6);
            part(m, "Body3", ps, vc, l, o, r, g, b, a);
            part(m, "Ventral1", ps, vc, l, o, r, g, b, a);
            part(m, "Ventral2", ps, vc, l, o, r, g, b, a);
            part(m, "Ventral3", ps, vc, l, o, r, g, b, a);
            ry(ps, cy5 * -4);
            part(m, "Dorsal4", ps, vc, l, o, r, g, b, a);
            part(m, "Ventral4", ps, vc, l, o, r, g, b, a);
            part(m, "Body4", ps, vc, l, o, r, g, b, a);
            ry(ps, cy6 * -6);
            part(m, "Body5", ps, vc, l, o, r, g, b, a);
            part(m, "Body6", ps, vc, l, o, r, g, b, a);
            ry(ps, cy7 * -5);
            part(m, "Tail", ps, vc, l, o, r, g, b, a);
            ps.popPose();
        }
    }

    /** {@code ModelDepthSquid}: Koerperneigung und Tentakelwinkel des Vanilla-Tintenfischs (interpoliert). */
    public static class DepthSquid extends ObjMobModel<EntityDepthSquid> {
        @Override
        protected void draw(PoseStack ps, VertexConsumer vc, int l, int o, float r, float g, float b, float a) {
            SimpleObjModel m = DEPTHSQUID;
            float tentacleAngle = 0;
            float squidPitch = 0;
            if (entity != null) {
                tentacleAngle = entity.oldTentacleAngle + (entity.tentacleAngle - entity.oldTentacleAngle) * partialTick;
                squidPitch = entity.xBodyRotO + (entity.xBodyRot - entity.xBodyRotO) * partialTick;
            }

            double cy0 = Math.sin(limbSwing % (Math.PI * 2));
            double cy1 = Math.sin(limbSwing % (Math.PI * 2) - Math.PI * 0.4);

            ps.translate(0, 1.0F, 0);
            rx(ps, squidPitch);

            part(m, "Body", ps, vc, l, o, r, g, b, a);

            ps.pushPose(); rx(ps, cy1 * 20); ry(ps, cy0 * 10); part(m, "FinL", ps, vc, l, o, r, g, b, a); ps.popPose();
            ps.pushPose(); rx(ps, cy1 * 20); ry(ps, cy0 * -10); part(m, "FinR", ps, vc, l, o, r, g, b, a); ps.popPose();

            // dibblies
            for (int i = 0; i < 8; i++) {
                ps.pushPose();
                ps.translate(0, -0.9, 0);
                ry(ps, i * -45.0F);
                rx(ps, tentacleAngle * (180F / (float) Math.PI));
                ps.translate(0, 0.9, 0);
                part(m, "T0", ps, vc, l, o, r, g, b, a);
                ps.popPose();
            }
        }
    }
}
