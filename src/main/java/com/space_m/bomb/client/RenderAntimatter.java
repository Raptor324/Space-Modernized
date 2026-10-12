package com.space_m.bomb.client;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.SpaceMod;
import com.space_m.bomb.TileEntityAntimatter;
import com.space_m.machine.misc.client.ItemRenderMachines;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code RenderAntimatter}: T(0.5, 0, 0.5), Drehung nach Metadaten (im Original immer 0, siehe
 * {@code NukeAntimatter}), Modell {@code models/bombs/blomb.obj} mit {@code blomb_tex}. Mit Antimaterie-Kern zusaetzlich
 * eine fast schwarze Kugel ({@code sphere_uv}, Farbe 0x010101, Skala 0.1, schwebend um 2.4 +- sin(t*0.1)*0.0125) und nach
 * T(0, 1, 0) drei Strahlen zur Kugel (Spirale 0x101020, zwei Zufallswellen 0x202060).
 * Item ({@code ItemRenderLibrary}): Inventar T(0, 7, 0) S(3.25) Rot(180 um X); Modell.
 * Original-Klasse: RenderAntimatter
 */
public class RenderAntimatter implements HbmBerBounds<TileEntityAntimatter> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/bombs/blomb.obj"));
    public static final ResourceLocation TEXTURE = SpaceMod.id("textures/models/bombs/blomb_tex.png");
    public static final SimpleObjModel SPHERE = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath("hbm_m", "models/sphere_uv.obj"));

    public RenderAntimatter(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityAntimatter amat, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        long time = amat.getLevel() != null ? amat.getLevel().getGameTime() : 0;

        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);
        // Original switch(getBlockMetadata()) 2/4/3/5 -> Metadaten sind immer 0: keine Drehung

        double scale = 0.1;
        boolean core = amat.hasAmatCore();
        if (core) {
            int c = 0x010101;
            ps.pushPose();
            ps.translate(0, 2.4D + Math.sin(((time + f) * 0.1D) % (Math.PI * 2D)) * 0.125 * scale, 0);
            ps.scale((float) scale, (float) scale, (float) scale);
            SPHERE.renderAllColor(ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL),
                    ((c >> 16) & 0xff) / 255F, ((c >> 8) & 0xff) / 255F, (c & 0xff) / 255F, 1F);
            ps.popPose();
        }

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light);

        if (core) {
            ps.translate(0, 1, 0);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 2.5, 0), EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0x101020, 0x101020, 0, 1, 0F, 6, (float) scale * 0.1F);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 2.5, 0), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 2) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 2.5, 0), EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, (int) (time / 4) % 1000, 6, (float) scale, 2, 0.0625F * (float) scale);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityAntimatter be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536

    /** Original {@code ItemRenderLibrary} (nuke_antimatter). */
    public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
        @Override
        public void renderInventory(PoseStack ps) {
            ps.translate(0, 7, 0);
            ps.scale(3.25F, 3.25F, 3.25F);
            // GL11.glRotatef(180, 90, 0, 0): Achse (90,0,0) normiert = X
            ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
        }

        @Override
        public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
            MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light);
        }
    };
}
