package com.space_m.machine.mining.client;

import org.joml.Matrix4f;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.mining.MiningMachines;
import com.space_m.machine.mining.TileEntityMachineMagma;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 NTM Space {@code RenderMagma}: Kernmitte, Ausrichtung (3: 0, 5: 90, 2: 180, 4: 270), {@code getHeightOffset()} (3)
 * tiefer; {@code magma_drill} ohne {@code DrillHead}/{@code Blades}, dann Bohrkopf mit {@code +drillRotation} und Klingen
 * mit {@code -drillRotation} um Y. Ab Lavaspiegel 0,01 eine Lava-Oberflaeche (Blockatlas, {@code lava}-Symbol) ueber 3x3
 * Felder in Hoehe {@code lavaHeight}. Item: {@code renderInventory} T(0,-2,0) S(3), {@code renderCommon} R(90 Y) S(0,5).
 * Original-Klasse: RenderMagma
 */
public class RenderMagma implements HbmBerBounds<TileEntityMachineMagma> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/machines/magma_drill.obj"));
    public static final ResourceLocation TEX = SpaceMod.id("textures/models/machines/magma_drill.png");

    public RenderMagma(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineMagma drill, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, 0.0D, 0.5D);

            switch (ObjBerHelper.meta(drill)) {
                case 3 -> ps.mulPose(Axis.YP.rotationDegrees(0));
                case 5 -> ps.mulPose(Axis.YP.rotationDegrees(90));
                case 2 -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case 4 -> ps.mulPose(Axis.YP.rotationDegrees(270));
                default -> { }
            }

            ps.translate(0, -MiningMachines.MACHINE_MAGMA.get().getHeightOffset(), 0);

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
            renderAllExcept(ps, vc, light, "DrillHead", "Blades");

            float drillRotation = drill.prevDrillRotation + (drill.drillRotation - drill.prevDrillRotation) * interp;
            float lavaHeight = drill.prevLavaHeight + (drill.lavaHeight - drill.prevLavaHeight) * interp;

            ps.pushPose();
            {
                ps.mulPose(Axis.YP.rotationDegrees(drillRotation));
                renderPart(ps, vc, light, "DrillHead");
            }
            ps.popPose();

            ps.pushPose();
            {
                ps.mulPose(Axis.YP.rotationDegrees(-drillRotation));
                renderPart(ps, vc, light, "Blades");
            }
            ps.popPose();

            if (lavaHeight > 0.01F) {
                TextureAtlasSprite lava = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                        .getParticleIcon(Blocks.LAVA.defaultBlockState());
                VertexConsumer lvc = buf.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
                Matrix4f m = ps.last().pose();
                org.joml.Vector3f n = new org.joml.Vector3f(0F, 1F, 0F).mul(ps.last().normal());

                // RenderBlocks.renderFaceYPos mit Grenzen (0,0,0)-(1,lavaHeight,1) bei (ox-0,5, 0, oz-0,5)
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oz = -1; oz <= 1; oz++) {
                        float minX = ox - 0.5F, maxX = ox + 0.5F, minZ = oz - 0.5F, maxZ = oz + 0.5F;
                        vertex(lvc, m, n, maxX, lavaHeight, maxZ, lava.getU1(), lava.getV1(), light);
                        vertex(lvc, m, n, maxX, lavaHeight, minZ, lava.getU1(), lava.getV0(), light);
                        vertex(lvc, m, n, minX, lavaHeight, minZ, lava.getU0(), lava.getV0(), light);
                        vertex(lvc, m, n, minX, lavaHeight, maxZ, lava.getU0(), lava.getV1(), light);
                    }
                }
            }
        }
        ps.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, org.joml.Vector3f n, float x, float y, float z, float u, float v, int light) {
        RenderHooks.vertexFull(vc, m, x, y, z, 255, 255, 255, 255, u, v, OverlayTexture.NO_OVERLAY, light, n.x, n.y, n.z);
    }

    /** {@code HFRWavefrontObject.renderPart}: Gruppenname ohne Gross-/Kleinschreibung (OBJ: {@code Drillhead}). */
    static void renderPart(PoseStack ps, VertexConsumer vc, int light, String name) {
        for (String part : MODEL.getPartNames()) {
            if (part.equalsIgnoreCase(name)) MODEL.renderPart(part, ps, vc, light);
        }
    }

    /** {@code HFRWavefrontObject.renderAllExcept}. */
    static void renderAllExcept(PoseStack ps, VertexConsumer vc, int light, String... excluded) {
        for (String part : MODEL.getPartNames()) {
            boolean skip = false;
            for (String ex : excluded) if (ex.equalsIgnoreCase(part)) skip = true;
            if (!skip) MODEL.renderPart(part, ps, vc, light);
        }
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineMagma be) { return true; }

    /** Original {@code getMaxRenderDistanceSquared = 65536} (256 Bloecke). */
    @Override public int getViewDistance() { return 256; }
}
