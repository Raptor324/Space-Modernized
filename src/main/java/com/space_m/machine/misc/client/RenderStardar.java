package com.space_m.machine.misc.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.misc.TileEntityMachineStardar;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderStardar}: Kern-Mitte, 3 tiefer (Boden). Sockel {@code base} um 180 Grad plus Ausrichtung
 * (2: 0, 4: 90, 3: 180, 5: 270); danach unabhaengig von der Ausrichtung Drehteil {@code rotation} um {@code dishYaw}
 * und Schuessel {@code pitch} zusaetzlich um {@code dishPitch} (Drehpunkt 10,6 ueber dem Boden), beide interpoliert.
 * Item ({@code getRenderer()}): Inventar T(0, -5, 0) S(1,5), gemeinsam S(0,55), ganzes Modell ohne Rueckseiten-Culling.
 *
 * <p>Abweichung (technisch): {@code glShadeModel(GL_SMOOTH/GL_FLAT)} gibt es im Kernprofil nicht; die Normalen kommen
 * je Ecke aus dem OBJ.
 * Original-Klasse: RenderStardar
 */
public class RenderStardar implements HbmBerBounds<TileEntityMachineStardar> {

    /** Original {@code ResourceManager.stardar} ({@code models/machines/stardar.obj}). */
    public static final SimpleObjModel STARDAR = new SimpleObjModel(SpaceMod.id("models/machines/stardar.obj"));
    /** Original {@code ResourceManager.stardar_tex} ({@code textures/models/machines/stardar.png}). */
    public static final ResourceLocation STARDAR_TEX = SpaceMod.id("textures/block/machine/stardar.png");

    public RenderStardar(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineStardar stardar, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, -3D, 0.5D);

            ps.pushPose();
            {
                ps.mulPose(Axis.YP.rotationDegrees(180));

                switch (ObjBerHelper.meta(stardar)) {
                    case 2 -> ps.mulPose(Axis.YP.rotationDegrees(0));
                    case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90));
                    case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180));
                    case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270));
                    default -> { }
                }

                STARDAR.renderPart("base", ps, buf.getBuffer(RenderType.entityCutout(STARDAR_TEX)), light);
            }
            ps.popPose();

            float dishYaw = stardar.prevDishYaw + (stardar.dishYaw - stardar.prevDishYaw) * interp;
            float dishPitch = stardar.prevDishPitch + (stardar.dishPitch - stardar.prevDishPitch) * interp;
            float dishOffset = 10.6F;

            ps.mulPose(Axis.YP.rotationDegrees(dishYaw));
            STARDAR.renderPart("rotation", ps, buf.getBuffer(RenderType.entityCutout(STARDAR_TEX)), light);

            ps.translate(0, dishOffset, 0);
            ps.mulPose(Axis.XP.rotationDegrees(dishPitch));
            ps.translate(0, -dishOffset, 0);
            STARDAR.renderPart("pitch", ps, buf.getBuffer(RenderType.entityCutout(STARDAR_TEX)), light);
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineStardar be) { return true; }
    @Override public int getViewDistance() { return 256; } // getMaxRenderDistanceSquared 65536

    /** Original {@code getRenderer()} ({@code ItemRenderBase}). */
    public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
        @Override
        public void renderInventory(PoseStack ps) {
            ps.translate(0, -5, 0);
            ps.scale(1.5F, 1.5F, 1.5F);
        }

        @Override
        public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
            ps.scale(0.55F, 0.55F, 0.55F);
            // GL_CULL_FACE aus
            STARDAR.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(STARDAR_TEX)), light);
        }
    };
}
