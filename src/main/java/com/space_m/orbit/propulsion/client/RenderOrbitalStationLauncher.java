package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.orbit.client.RenderOrbitalStation;
import com.space_m.orbit.propulsion.TileEntityOrbitalStationLauncher;
import com.space_m.rocket.client.RocketPronter;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Startport-Zweig von NTM Space {@code RenderOrbitalStation} ({@code te instanceof TileEntityOrbitalStationLauncher}):
 * Port + vier Arme (Textur {@code docking_port_launcher}, Drehung {@code prevRot..rot}) ueber den gemeinsamen
 * {@code RenderOrbitalStation.renderPort} (R6-A); darunter die im Bau befindliche Rakete, wenn sie keine Zusatzfehler hat
 * ({@code y - Hoehe - 0,5}, {@code prontRocket(..., false)} = eingefahren).
 * Original-Klasse: RenderOrbitalStation
 */
public class RenderOrbitalStationLauncher implements HbmBerBounds<TileEntityOrbitalStationLauncher> {

    public RenderOrbitalStationLauncher(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityOrbitalStationLauncher launcher, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        float armRotation = launcher.prevRot + (launcher.rot - launcher.prevRot) * interp;

        ps.pushPose();
        {
            ps.translate(0.5D, 1.0D, 0.5D);
            RenderOrbitalStation.rotate(ps, ObjBerHelper.meta(launcher));
            RenderOrbitalStation.renderPort(ps, buf, light, armRotation, RenderOrbitalStation.DOCKING_PORT_LAUNCHER_TEX);

            if (launcher.rocket != null && launcher.rocket.extraIssues.size() == 0) {
                ps.pushPose();
                {
                    ps.translate(0, -launcher.rocket.getHeight() - 0.5, 0);

                    RocketPronter.prontRocket(launcher.rocket, ps, buf, light, false);
                }
                ps.popPose();
            }
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityOrbitalStationLauncher be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
