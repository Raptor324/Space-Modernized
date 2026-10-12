package com.space_m.orbit.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.orbit.TileEntityOrbitalStation;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderOrbitalStation}: Andockring ({@code docking_port.obj}, Teil {@code Port}) und vier
 * Klammerarme ({@code ArmZP}, je um 90 Grad gedreht), die beim Andocken um die Achse (0, -1.75, -2) auf 90 Grad
 * aufklappen ({@code rot}, 2.25 Grad je Tick). Der Startport (R6-B) nutzt {@link #renderPort} mit
 * {@link #DOCKING_PORT_LAUNCHER_TEX} und zeichnet zusaetzlich seine Rakete.
 * Original-Klasse: RenderOrbitalStation
 */
public class RenderOrbitalStation implements BlockEntityRenderer<TileEntityOrbitalStation> {

    public static final SimpleObjModel DOCKING_PORT = new SimpleObjModel(SpaceMod.id("models/machines/docking_port.obj"));
    public static final ResourceLocation DOCKING_PORT_TEX = SpaceMod.id("textures/models/machines/docking_port.png");
    public static final ResourceLocation DOCKING_PORT_LAUNCHER_TEX = SpaceMod.id("textures/models/machines/docking_port_launcher.png");

    public RenderOrbitalStation(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityOrbitalStation station, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        float armRotation = station.prevRot + (station.rot - station.prevRot) * interp;

        ps.pushPose();
        {
            ps.translate(0.5D, 1.0D, 0.5D);
            rotate(ps, ObjBerHelper.meta(station));
            renderPort(ps, buf, light, armRotation, DOCKING_PORT_TEX);
        }
        ps.popPose();
    }

    /** Original {@code switch(getBlockMetadata() - BlockDummyable.offset)}. */
    public static void rotate(PoseStack ps, int meta) {
        switch (meta) {
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(0));
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
    }

    /**
     * Ring + vier Arme (Pose bereits auf Blockmitte, y + 1, gedreht). Gemeinsam mit dem Startport (R6-B).
     */
    public static void renderPort(PoseStack ps, MultiBufferSource buf, int light, float armRotation, ResourceLocation texture) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(texture));

        DOCKING_PORT.renderPart("Port", ps, vc, light);

        ps.pushPose();
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            {
                // one hop this time
                ps.translate(0, -1.75F, -2);

                // criss cross
                ps.mulPose(Axis.XP.rotationDegrees(-armRotation));

                // one hop this time
                ps.translate(0, 1.75F, 2);

                // let's go to work
                DOCKING_PORT.renderPart("ArmZP", ps, vc, light);
            }
            ps.popPose();

            // cha cha real smooth
            ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityOrbitalStation be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536
}
