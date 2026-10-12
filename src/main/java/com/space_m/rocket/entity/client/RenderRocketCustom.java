package com.space_m.rocket.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.celestial.CelestialBody;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.client.RocketPronter;
import com.space_m.rocket.entity.EntityRideableRocket;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 NTM Space {@code RenderRocketCustom}: die fliegende Rakete aus ihren {@link RocketStruct}-Teilen (OBJ), gedreht um
 * Gier ({@code yaw - 90}) und Neigung, Landebeine ausgeklappt ausser im Orbit, Stufentrennung/Verkleidung ueber
 * {@code decoupleTimer}/{@code shroudTimer}. Gezeichnet wird mit {@link RocketPronter} (R5-A).
 */
public class RenderRocketCustom extends EntityRenderer<EntityRideableRocket> {

    public RenderRocketCustom(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityRideableRocket entity, float yaw, float interp, PoseStack ps, MultiBufferSource buf, int light) {
        RocketStruct rocket = entity.getRocket();

        ps.pushPose();
        {
            float rYaw = Mth.lerp(interp, entity.yRotO, entity.getYRot()) - 90.0F;
            float rPitch = Mth.lerp(interp, entity.xRotO, entity.getXRot());

            // GL11.glRotatef(yaw - 90, 0, 1, 0); glRotatef(pitch, 0, 0, 1); glRotatef(yaw - 90, 0, -1, 0)
            ps.mulPose(Axis.YP.rotationDegrees(rYaw));
            ps.mulPose(Axis.ZP.rotationDegrees(rPitch));
            ps.mulPose(Axis.YP.rotationDegrees(-rYaw));

            RocketPronter.prontRocket(rocket, entity, ps, buf, light, !CelestialBody.inOrbit(entity.level()),
                    entity.decoupleTimer, entity.shroudTimer, interp);
        }
        ps.popPose();

        super.render(entity, yaw, interp, ps, buf, light);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(EntityRideableRocket entity) {
        return TextureAtlas.LOCATION_BLOCKS; // Original ResourceManager.universal - die Teile binden ihre Texturen selbst
    }
}
