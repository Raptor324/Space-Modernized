package com.space_m.war.client;

import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.war.EntityCarpetJet;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * NTM Space {@code RenderBomber}, Fall 10 (Carpet-Jet): Grunddrehung wie alle Bomber (Gier - 90 um Y, 90 um Z,
 * Neigung um Z, Rollen {@code sin(t*0,05)*10} um X), dann {@code b2.obj} x3, +2 X, -90 Y mit {@code b_2.png} und zwei
 * Xenon-Abgasstrahlen ({@code xenon_thruster.obj} Teil {@code Exhaust}, {@code xenon_trail.png}, Farbe
 * 0,25/0,88/0,82, Vollhelligkeit, Laenge zufaellig {@code (1,2 - r*r*0,2)*2}).
 */
public class RenderCarpetJet extends EntityRenderer<EntityCarpetJet> {

    public static final SimpleObjModel B2X = new SimpleObjModel(SpaceMod.id("models/mobs/b2.obj"));
    public static final SimpleObjModel XENON_THRUSTER = new SimpleObjModel(SpaceMod.id("models/machines/xenon_thruster.obj"));
    public static final ResourceLocation B2X_TEX = SpaceMod.id("textures/entity/b_2.png");
    public static final ResourceLocation XENON_EXHAUST_TEX = SpaceMod.id("textures/models/xenon_trail.png");

    private final Random rand = new Random();

    public RenderCarpetJet(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(@NotNull EntityCarpetJet entity, float yaw, float interp, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(interp, entity.yRotO, entity.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(90F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(interp, entity.xRotO, entity.getXRot())));

        ps.mulPose(Axis.XP.rotationDegrees((float) Math.sin((entity.tickCount + interp) * 0.05) * 10));

        // case 10
        ps.scale(3, 3, 3);
        ps.translate(2, 0, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-90));
        B2X.renderAllEntity(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(B2X_TEX)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);

        float trailStretch = rand.nextFloat();
        trailStretch = 1.2F - (trailStretch * trailStretch * 0.2F);
        trailStretch *= 2;

        if (trailStretch > 0) {
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(0.56F * side, -0.56F, 1F);
                ps.mulPose(Axis.YP.rotationDegrees(180));
                ps.scale(0.5F, 0.5F, trailStretch);
                ps.translate(0, 0, 1F);
                XENON_THRUSTER.renderPartEntity("Exhaust", ps, buffers.getBuffer(RenderType.entityTranslucentEmissive(XENON_EXHAUST_TEX)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.25F, 0.88F, 0.82F, 1F);
                ps.popPose();
            }
        }

        ps.popPose();
        super.render(entity, yaw, interp, ps, buffers, light);
    }

    @Override
    public boolean shouldRender(@NotNull EntityCarpetJet entity, @NotNull net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityCarpetJet entity) {
        return B2X_TEX;
    }
}
