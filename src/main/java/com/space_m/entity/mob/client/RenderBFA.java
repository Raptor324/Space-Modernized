package com.space_m.entity.mob.client;

import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.space_m.SpaceMod;
import com.space_m.entity.mob.EntityBFAngel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.space_m.entity.mob.client.SpaceMobModels.rx;
import static com.space_m.entity.mob.client.SpaceMobModels.ry;
import static com.space_m.entity.mob.client.SpaceMobModels.rz;

/**
 * 1:1 {@code RenderBFA}: {@code balefire_angel.obj} (Textur {@code bfa_body}) doppelt gross, nach Gier/Neigung des
 * Engels gedreht, sechs schlagende Fluegel, 64 rot-gruene Lichtstrahlen um den Kern ({@code renderFlash}, additiv) und
 * bei aktivem Todesstrahl drei Strahlen bis zum Boden. Die Bossleiste kommt vom Server ({@code ServerBossEvent}).
 */
public class RenderBFA extends EntityRenderer<EntityBFAngel> {

    public static final ResourceLocation TEXTURE = SpaceMod.id("textures/entity/bfa_body.png");
    static final SimpleObjModel BFANGEL = ObjMobModel.obj("balefire_angel");

    public RenderBFA(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(EntityBFAngel entity, Frustum frustum, double x, double y, double z) {
        return true; // ignoreFrustumCheck
    }

    @Override
    public void render(EntityBFAngel ufo, float entityYaw, float f1, PoseStack ps, MultiBufferSource buffers, int light) {
        float yaw = ufo.yRotO + (ufo.getYRot() - ufo.yRotO) * f1;
        float pitch = ufo.xRotO + (ufo.getXRot() - ufo.xRotO) * f1;

        ps.pushPose();
        ps.translate(0, 1, 0);
        ry(ps, -yaw);
        rx(ps, pitch);

        double time = ufo.tickCount + f1;
        double cy0 = Math.sin(time * 0.3 % (Math.PI * 2));
        double cy1 = Math.sin(time * 0.25 % (Math.PI * 2) - Math.PI * 0.2);
        double cy2 = Math.sin(time * 0.3 % (Math.PI * 2) - Math.PI * 0.4);
        double scale = 2D;

        renderFlash(ps, buffers, 0.4D, (float) (time + f1) * 0.06F);

        ps.pushPose();
        ps.scale(2, 2, 2);
        ps.translate(0.5, 0.2, 0.5);
        ps.translate(0, 0.5F, 0);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int o = OverlayTexture.NO_OVERLAY;

        // Core
        BFANGEL.renderPartEntity("eye", ps, vc, light, o, 1, 1, 1, 1);
        BFANGEL.renderPartEntity("body", ps, vc, light, o, 1, 1, 1, 1);

        // === Wings === (Original ohne Standardbeleuchtung)
        wing(ps, vc, light, "wingL1", cy0 * 2, cy0 * 20, false);
        wing(ps, vc, light, "wingR1", cy0 * -2, cy0 * -20, false);
        wing(ps, vc, light, "wingL2", cy1 * 5, cy1 * 20, false);
        wing(ps, vc, light, "wingR2", cy1 * -5, cy1 * -20, false);
        wing(ps, vc, light, "wingL3", cy1 * 5, cy2 * 20, true);
        wing(ps, vc, light, "wingR3", cy1 * -5, cy2 * -20, true);

        ps.popPose();

        if (ufo.getBeam()) {
            int ix = Mth.floor(ufo.getX());
            int iz = Mth.floor(ufo.getZ());
            int iy = 0;

            for (int i = (int) Math.ceil(ufo.getY()); i >= 0; i--) {
                if (!ufo.level().getBlockState(new BlockPos(ix, i, iz)).isAir()) {
                    iy = i;
                    break;
                }
            }

            double length = ufo.getY() - iy;

            if (length > 0) {
                Vec3 v = new Vec3(0, -length, 0);
                BeamPronter.prontBeam(ps, buffers, v, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0x101020, 0x101020, 0, (int) (length + 1), 0F, 6, (float) scale * 0.75F);
                BeamPronter.prontBeam(ps, buffers, v, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, ufo.tickCount / 2, (int) (length / 2 + 1), (float) scale * 1.5F, 2, 0.0625F);
                BeamPronter.prontBeam(ps, buffers, v, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, ufo.tickCount / 4, (int) (length / 2 + 1), (float) scale * 1.5F, 2, 0.0625F);
            }
        }

        ps.popPose();
        super.render(ufo, entityYaw, f1, ps, buffers, light);
    }

    /** Fluegel: 1./2. Paar Z dann Y, 3. Paar X dann Y. */
    private static void wing(PoseStack ps, VertexConsumer vc, int light, String name, double a, double b, boolean xAxis) {
        ps.pushPose();
        if (xAxis) rx(ps, a); else rz(ps, a);
        ry(ps, b);
        BFANGEL.renderPartEntity(name, ps, vc, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        ps.popPose();
    }

    /** {@code renderFlash}: 64 Strahlen-Faecher (GL_TRIANGLE_FAN, additiv, ohne Textur), Mitte 1/0.6/0.6, Rand transparent. */
    private static void renderFlash(PoseStack ps, MultiBufferSource buffers, double intensity, float seed) {
        Random random = new Random(425L);
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());

        ps.pushPose();
        ps.translate(1, 2.2, 1);
        float scale = 2;

        int ca = (int) (Mth.clamp((float) (intensity * intensity) * 2F, 0F, 1F) * 255);

        for (int i = 0; i < 64; i++) {
            rx(ps, random.nextFloat() * 360.0F + seed);
            ry(ps, random.nextFloat() * 360.0F + seed);
            rz(ps, random.nextFloat() * 360.0F + seed);
            rx(ps, random.nextFloat() * 360.0F + seed);
            ry(ps, random.nextFloat() * 360.0F + seed);

            float vert1 = (random.nextFloat() * 20.0F + 5.0F + 1 * 10.0F) * (float) (intensity * intensity * scale);
            float vert2 = (random.nextFloat() * 2.0F + 1.0F + 1 * 2.0F) * (float) (intensity * intensity * scale);

            Matrix4f m = ps.last().pose();
            float[][] rim = {
                    {-0.866F * vert2, vert1, -0.5F * vert2},
                    {0.866F * vert2, vert1, -0.5F * vert2},
                    {0.0F, vert1, 1.0F * vert2},
                    {-0.866F * vert2, vert1, -0.5F * vert2}};
            // Faecher (Mitte, Rand i, Rand i+1) als entartete Vierecke
            for (int k = 0; k < 3; k++) {
                RenderHooks.vertexColor(vc, m, 0, 0, 0, 255, 153, 153, ca);
                RenderHooks.vertexColor(vc, m, rim[k][0], rim[k][1], rim[k][2], 25, 153, 25, 0);
                RenderHooks.vertexColor(vc, m, rim[k + 1][0], rim[k + 1][1], rim[k + 1][2], 25, 153, 25, 0);
                RenderHooks.vertexColor(vc, m, rim[k + 1][0], rim[k + 1][1], rim[k + 1][2], 25, 153, 25, 0);
            }
        }

        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBFAngel entity) {
        return TEXTURE;
    }
}
