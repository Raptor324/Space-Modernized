package com.space_m.machine.mining.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.MinerRocketRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.SpaceMod;
import com.space_m.machine.mining.TileEntityMachineGasDock;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 NTM Space {@code RenderDecoBlock}, Zweig {@code gas_dock}: T(0,5, 1,5, 0,5), zweimal R(180 Z) heben sich auf,
 * T(0, -1,5, 0) -> Blockmitte am Boden; {@code sat_dock}-Modell mit Textur {@code gas_dock}; solange
 * {@code launchTicks < 100} die Sammelrakete ({@code minerRocket}, Textur {@code gasRocket}, ohne Rueckseitenausblendung)
 * bei Hoehe {@code 0,75 + clamp(launchTicks -+ interp, 0, 200)}.
 * Item ({@code ItemRenderLibrary}): S(3) im Inventar, R(90 um -Y), {@code sat_dock} mit Textur {@code sat_dock}.
 * Original-Klasse: RenderDecoBlock
 */
public class RenderGasDock implements HbmBerBounds<TileEntityMachineGasDock> {

    /** Original {@code ResourceManager.satDock} ({@code models/sat_dock.obj}); HBM fuehrt dieselbe Datei. */
    public static final SimpleObjModel SAT_DOCK = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath("hbm_m", "models/block/machines/sat_dock.obj"));
    /** Original {@code gasdock_tex}. */
    public static final ResourceLocation GAS_DOCK_TEX = SpaceMod.id("textures/models/gas_dock.png");
    /** Original {@code satdock_tex} (HBM, identisch). */
    public static final ResourceLocation SAT_DOCK_TEX = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/block/machine/sat_dock.png");
    /** Original {@code gasRocket_tex} ({@code textures/models/gasRocket.png}; Pfad klein geschrieben). */
    public static final ResourceLocation GAS_ROCKET_TEX = SpaceMod.id("textures/models/gas_rocket.png");

    public RenderGasDock(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineGasDock transporter, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();

        // glTranslatef(x + 0.5, y + 1.5, z + 0.5); glRotatef(180, Z); glRotatef(180, Z); glTranslatef(0, -1.5, 0)
        ps.translate(0.5D, 0.0D, 0.5D);

        SAT_DOCK.renderAll(ps, buf.getBuffer(RenderType.entityCutout(GAS_DOCK_TEX)), light);

        if (transporter.launchTicks < 100) {
            ps.pushPose();
            {
                ps.translate(0.0F, 0.75F + Mth.clamp(transporter.launchTicks + (transporter.hasRocket ? -f : f), 0, 200), 0.0F);

                // GL_CULL_FACE aus
                MinerRocketRenderer.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(GAS_ROCKET_TEX)), light);
            }
            ps.popPose();
        }

        ps.popPose();
    }

    /** Original {@code getMaxRenderDistanceSquared = 65536} (256 Bloecke). */
    @Override public int getViewDistance() { return 256; }
}
