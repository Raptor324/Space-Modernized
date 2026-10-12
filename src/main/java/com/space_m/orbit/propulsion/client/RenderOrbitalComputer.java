package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.orbit.propulsion.TileEntityOrbitalStationComputer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1:1 NTM Space {@code RenderOrbitalComputer}: Blockmitte, Ausrichtung (2: 0, 4: 90, 3: 180, 5: 270), Gehaeuse
 * ({@code renderAllExcept("Drive")}) und mit Laufwerk zusaetzlich {@code Drive}.
 * Original-Klasse: RenderOrbitalComputer
 */
public class RenderOrbitalComputer implements HbmBerBounds<TileEntityOrbitalStationComputer> {

    public RenderOrbitalComputer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityOrbitalStationComputer computer, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, 0D, 0.5D);

            rotate(ps, ObjBerHelper.meta(computer));

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(PropulsionModels.ORBITAL_COMPUTER_TEX));
            renderAllExceptDrive(ps, vc, light);

            if (computer.hasDrive) {
                PropulsionModels.ORBITAL_COMPUTER.renderPart("drive", ps, vc, light);
            }
        }
        ps.popPose();
    }

    public static void rotate(PoseStack ps, int meta) {
        switch (meta) {
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
    }

    /** {@code renderAllExcept("Drive")} (HFR-Teilnamen ohne Gross-/Kleinschreibung). */
    public static void renderAllExceptDrive(PoseStack ps, VertexConsumer vc, int light) {
        for (String part : PropulsionModels.ORBITAL_COMPUTER.getPartNames()) {
            if (part.equalsIgnoreCase("drive")) continue;
            PropulsionModels.ORBITAL_COMPUTER.renderPart(part, ps, vc, light);
        }
    }

    @Override public boolean shouldRenderOffScreen(TileEntityOrbitalStationComputer be) { return false; }
}
