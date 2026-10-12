package com.space_m.rocket.client;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.rocket.RocketPart;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.RocketStruct.RocketStage;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;

/**
 * Zeichnet eine mehrstufige Rakete, 1:1 NTM Space {@code MissilePronter.prontRocket}: je Stufe Triebwerk, Landebeine und
 * Rumpf (gestapelt {@code getStack()}-mal, als Buendel {@code getCluster()}-mal im Kreis um den Radius versetzt), darueber die
 * Kapsel. Ab der zweiten Stufe umhuellt die Verkleidung des Rumpfs ({@code getShroud()}, Textur {@code universal}) das
 * Triebwerk - beschnitten an der Triebwerkshoehe (Original {@code GL_CLIP_PLANE0 {0,-1,0,h}}, hier per Dreiecks-Clipping).
 * Abwurf-/Verkleidungs-Animation ueber {@code decoupleTimer}/{@code shroudTimer}.
 * Original-Klasse: MissilePronter (prontRocket; prontMissile bleibt HBM)
 */
public final class RocketPronter {

    private RocketPronter() {}

    private static final Map<String, RocketPartRenderer> PART_RENDERERS = new HashMap<>();

    /** Original {@code MissilePart.withRenderer(...)} (Schluessel = {@link RocketPart#rendererId}). */
    public static void registerPartRenderer(String rendererId, RocketPartRenderer renderer) {
        PART_RENDERERS.put(rendererId, renderer);
    }

    private static void part(String model, String texture, PoseStack ps, MultiBufferSource buf, int light) {
        RocketModels.model(model).renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(RocketModels.texture(texture))), light);
    }

    public static void prontRocket(RocketStruct rocket, PoseStack ps, MultiBufferSource buf, int light) {
        prontRocket(rocket, null, ps, buf, light, true, 0, 0, 0);
    }

    public static void prontRocket(RocketStruct rocket, PoseStack ps, MultiBufferSource buf, int light, boolean isDeployed) {
        prontRocket(rocket, null, ps, buf, light, isDeployed, 0, 0, 0);
    }

    // Attaches a set of stages together
    public static void prontRocket(RocketStruct rocket, @Nullable Entity entity, PoseStack ps, MultiBufferSource buf, int light,
                                   boolean isDeployed, int decoupleTimer, int shroudTimer, float interp) {
        ps.pushPose();

        boolean hasShroud = false;

        for (RocketStage stage : rocket.stages) {
            int stack = stage.getStack();
            int cluster = stage.getCluster();

            if (isDeployed && stage.thruster != null && stage.fins != null && stage.fins.height > stage.thruster.height) {
                ps.translate(0, stage.fins.height - stage.thruster.height, 0);
            }

            for (int c = 0; c < cluster; c++) {
                ps.pushPose();
                {

                    if (decoupleTimer > 0) {
                        float decoupleLerp = decoupleTimer + interp;
                        ps.translate(0, -decoupleLerp, 0);
                        ps.mulPose(Axis.XP.rotationDegrees(decoupleLerp));
                    }

                    if (c > 0) {
                        float spin = (float) c / (float) (cluster - 1);
                        ps.mulPose(Axis.YP.rotationDegrees(360.0F * spin));

                        if (stage.fuselage != null) {
                            ps.translate(stage.fuselage.bottom.radius, 0, 0);
                        } else if (stage.thruster != null) {
                            ps.translate(stage.thruster.top.radius, 0, 0);
                        }
                    }

                    if (stage.thruster != null) {
                        if (hasShroud && stage.fuselage != null) {
                            boolean animate = shroudTimer > 0;
                            if (animate) {
                                float shroudLerp = shroudTimer + interp;
                                ps.pushPose();
                                ps.translate(0, -shroudLerp, 0);
                                ps.mulPose(Axis.XP.rotationDegrees(shroudLerp * 0.5F));
                            }

                            renderClipped(RocketModels.model(stage.fuselage.getShroud()),
                                    buf.getBuffer(RenderType.entityCutoutNoCull(RocketModels.texture(universalTexture()))),
                                    ps, light, (float) stage.thruster.height);

                            if (animate) {
                                ps.popPose();
                            }
                        }

                        if (!hasShroud || shroudTimer > 0) {
                            part(stage.thruster.getModel(isDeployed), stage.thruster.texture, ps, buf, light);
                        }

                        ps.translate(0, stage.thruster.height, 0);
                    }

                    if (stage.fuselage != null) {
                        if (stage.fins != null) {
                            part(stage.fins.getModel(isDeployed), stage.fins.texture, ps, buf, light);
                        }

                        for (int s = 0; s < stack; s++) {
                            part(stage.fuselage.getModel(isDeployed), stage.fuselage.texture, ps, buf, light);
                            ps.translate(0, stage.fuselage.height, 0);
                        }
                    }

                }
                ps.popPose();
            }

            if (stage.thruster != null) ps.translate(0, stage.thruster.height, 0);
            if (stage.fuselage != null) ps.translate(0, stage.fuselage.height * stack, 0);

            // Only the bottom-most stage can be deployed OR staged
            isDeployed = false;
            decoupleTimer = 0;
            if (hasShroud) shroudTimer = 0; // Only the bottom-most shroud (second stage from bottom) should animate
            hasShroud = true;
        }

        if (rocket.capsule != null) {
            RocketPartRenderer custom = rocket.capsule.rendererId != null ? PART_RENDERERS.get(rocket.capsule.rendererId) : null;
            if (entity != null && custom != null) {
                custom.render(ps, buf, light, entity, interp);
            } else {
                part(rocket.capsule.model, rocket.capsule.texture, ps, buf, light);
            }
        }

        ps.popPose();
    }

    /** Original {@code ResourceManager.universal}. */
    static String universalTexture() {
        return "hbm_m:textures/models/thegadget3_.png";
    }

    /** {@code glClipPlane(GL_CLIP_PLANE0, {0, -1, 0, h})} + {@code renderAll()}: alles mit {@code y <= h} bleibt. */
    private static void renderClipped(SimpleObjModel model, VertexConsumer vc, PoseStack ps, int light, float h) {
        Matrix4f identity = new Matrix4f();
        for (String name : model.getPartNames()) {
            model.renderPartClipped(name, ps, identity, vc, light, 0F, -1F, 0F, h);
        }
    }
}
