package com.space_m.war.client;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.mob.siege.SiegeTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.war.EntitySiegeUFO;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderSiegeUFO}: kreiselndes Mini-UFO ({@code mini_ufo.obj}, 5 Grad/Tick), Textur je Stufe
 * ({@code ufo_siege_<stufe>.png}), beim Sterben kippend (Achse 1/0/1), bei Treffer-Unverwundbarkeit wackelnd.
 */
public class RenderSiegeUFO extends EntityRenderer<EntitySiegeUFO> {

    public static final SimpleObjModel MINI_UFO = new SimpleObjModel(SpaceMod.id("models/mobs/mini_ufo.obj"));

    public RenderSiegeUFO(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull EntitySiegeUFO ufo, float yaw, float f1, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.translate(0, 0.25, 0);

        double rot = (ufo.tickCount + f1) * 5 % 360D;
        ps.mulPose(Axis.YP.rotationDegrees((float) rot));

        if (!ufo.isAlive()) {
            float tilt = ufo.deathTime + f1;
            Vector3f axis = new Vector3f(1, 0, 1).normalize();
            ps.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(tilt * 5), axis.x, axis.y, axis.z));
        } else if (ufo.invulnerableTime > 0) {
            ps.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(System.currentTimeMillis() * 0.01D) * (ufo.invulnerableTime - f1))));
        }

        MINI_UFO.renderAllEntity(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(ufo))), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);

        ps.popPose();
        super.render(ufo, yaw, f1, ps, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntitySiegeUFO entity) {
        SiegeTier tier = entity.getTier();
        return SpaceMod.id("textures/entity/ufo_siege_" + tier.name + ".png");
    }
}
