package com.space_m.rocket.client;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.celestial.CelestialBody;
import com.space_m.rocket.entity.EntityRideableRocket;
import com.space_m.rocket.entity.EntityRideableRocket.RocketState;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * 1:1 NTM Space {@code RenderDropPod} (Teil-Renderer von {@code rp_pod_20}, Original
 * {@code MissilePart.withRenderer(new RenderDropPod())}): im Orbit das ganze Modell; sonst Kapsel, Tuer (oeffnet sich
 * nach der Landung mit 2 Grad/Tick bis 90, schliesst sich beim Warten), vier Luftbremsen (beim Landen bis 65 Grad
 * ausgefahren; faellt die Rakete langsamer als 0,4, klappen sie in 1 s wieder ein) und Landebeine (beim Start
 * eingezogen = 0, sonst 0,5 tiefer). Angemeldet als Teil-Renderer {@code "drop_pod"} bei {@link RocketPronter}.
 * Original-Klasse: RenderDropPod
 */
public class RenderDropPod implements RocketPartRenderer {

    private static final String MODEL = "space_m:models/missile_parts/rp_drop_pod.obj";
    private static final String TEXTURE = "space_m:textures/models/missile_parts/warheads/rp_drop_pod.png";

    private boolean brakes;
    private long brakeStart;

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, @Nullable Entity entity, float interp) {
        SimpleObjModel model = RocketModels.model(MODEL);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(RocketModels.texture(TEXTURE)));

        if (!(entity instanceof EntityRideableRocket rocket) || CelestialBody.inOrbit(entity.level())) {
            model.renderAll(ps, vc, light);
            return;
        }

        RocketState state = rocket.getState();
        int timer = rocket.getStateTimer();
        float lerpTimer = (float) timer + interp;

        model.renderPart("DropPod", ps, vc, light);

        // Render door
        ps.pushPose();
        {
            float doorRotation = 0;
            if (state == RocketState.LANDED) {
                doorRotation = Mth.clamp(lerpTimer * 2, 0, 90);
            } else if (state == RocketState.AWAITING) {
                doorRotation = Mth.clamp(90 - lerpTimer * 2, 0, 90);
            }

            ps.translate(0.69291F, 2.8333F, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(doorRotation));
            ps.translate(-0.69291F, -2.8333F, 0);
            model.renderPart("Door", ps, vc, light);
        }
        ps.popPose();

        // Render airbrakes
        ps.pushPose();
        {
            float brakeRotation = 0;
            if (state == RocketState.LANDING) {
                if (rocket.getDeltaMovement().y > -0.4F) {
                    if (brakes) {
                        brakes = false;
                        brakeStart = System.currentTimeMillis();
                    }
                } else {
                    brakes = true;
                }

                if (!brakes) {
                    float t = Mth.clamp((float) (System.currentTimeMillis() - brakeStart) / 1000, 0, 1);
                    brakeRotation = (1 - t) * 65;
                } else {
                    brakeRotation = 65;
                }
            }

            for (int i = 0; i < 4; i++) {
                ps.pushPose();
                {
                    ps.mulPose(Axis.YP.rotationDegrees(i * 90 - 45));
                    ps.translate(0.46194, 3.5, 0);
                    ps.mulPose(Axis.ZP.rotationDegrees(brakeRotation));
                    ps.translate(-0.46194, -3.5, 0);
                    ps.mulPose(Axis.YP.rotationDegrees(45));
                    model.renderPart("Airbrake0", ps, vc, light);
                }
                ps.popPose();
            }
        }
        ps.popPose();

        // Render legs
        ps.pushPose();
        {
            double legExtension = 1;
            if (state == RocketState.LAUNCHING) legExtension = 0;

            ps.translate(0, -legExtension * 0.5, 0);
            model.renderPart("Legs", ps, vc, light);
        }
        ps.popPose();
    }
}
