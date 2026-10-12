package com.space_m.rocket.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.space_m.rocket.MachineRocketAssemblyBlockEntity;
import com.space_m.rocket.RocketStruct.RocketStage;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * BER der VAB, 1:1 NTM Space {@code RenderRocketAssembly}: Sockel ("Base") zwei Bloecke unter dem Kern, darauf - nur ohne
 * Bau-Hinweise ({@code extraIssues}) - die eingefahrene Rakete ab Hoehe 2.95 (minus Triebwerkshoehe der ersten Stufe) und je
 * Stufe Stuetzen ("Support", je Meter) bis zur Arbeitsbuehne ("Level"). Die Drehung nach Meta 2-5 greift im Original nie
 * (Kern-Meta >= 12), daher keine Drehung.
 * Original-Klasse: RenderRocketAssembly
 */
public class RenderRocketAssembly implements BlockEntityRenderer<MachineRocketAssemblyBlockEntity> {

    public static final String MODEL = "space_m:models/machines/rocket_assembly.obj";
    public static final String TEXTURE = "space_m:textures/models/machines/rocket_assembly.png";

    public RenderRocketAssembly(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineRocketAssemblyBlockEntity te, float partialTicks, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();

        ps.translate(0.5, -2.0, 0.5);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(RocketModels.texture(TEXTURE)));
        RocketModels.model(MODEL).renderPart("Base", ps, vc, light);

        if (te.rocket != null && te.rocket.extraIssues.size() == 0) {
            ps.pushPose();
            {

                ps.translate(0F, 2.95F, 0F);

                if (te.rocket.stages.size() > 0) {
                    RocketStage stage = te.rocket.stages.get(0);
                    if (stage.thruster != null) {
                        ps.translate(0, -stage.thruster.height, 0);
                    }
                }

                RocketPronter.prontRocket(te.rocket, ps, buf, light, false);

            }
            ps.popPose();

            vc = buf.getBuffer(RenderType.entityCutout(RocketModels.texture(TEXTURE)));

            for (int i = 0; i < te.rocket.stages.size(); i++) {
                RocketStage stage = te.rocket.stages.get(i);
                RocketStage nextStage = i < te.rocket.stages.size() - 1 ? te.rocket.stages.get(i + 1) : null;

                double targetHeight = 0;
                if (stage.fuselage != null) targetHeight += stage.fuselage.height * stage.getStack();
                if (nextStage != null && nextStage.thruster != null) targetHeight += nextStage.thruster.height;

                while (targetHeight > 1) {
                    ps.translate(0, 1, 0);
                    RocketModels.model(MODEL).renderPart("Support", ps, vc, light);
                    targetHeight--;
                }
                ps.translate(0, targetHeight, 0);

                RocketModels.model(MODEL).renderPart("Level", ps, vc, light);
            }
        }

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineRocketAssemblyBlockEntity te) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256; // Original getMaxRenderDistanceSquared 65536
    }

    //? if >= 1.21.1 {
    /*@Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(MachineRocketAssemblyBlockEntity te) {
        return com.hbm_m.platform.RenderBounds.INFINITE;
    }
    *///?}

}
