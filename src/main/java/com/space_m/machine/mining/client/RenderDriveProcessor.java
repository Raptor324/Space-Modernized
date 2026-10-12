package com.space_m.machine.mining.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.mining.TileEntityMachineDriveProcessor;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderDriveProcessor}: Blockmitte, R(90 Y) + Ausrichtung (2: 0, 4: 90, 3: 180, 5: 270),
 * {@code drive_processor} Teil {@code Base}, mit eingelegtem Laufwerk ({@code hasDrive}) zusaetzlich {@code Drive}.
 * Item: {@code renderInventory} S(7), {@code renderCommon} T(0,5, 0, 0), {@code Base} + {@code Drive}.
 * Original-Klasse: RenderDriveProcessor
 */
public class RenderDriveProcessor implements HbmBerBounds<TileEntityMachineDriveProcessor> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/machines/drive_processor.obj"));
    public static final ResourceLocation TEX = SpaceMod.id("textures/models/machines/drive_processor.png");

    public RenderDriveProcessor(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineDriveProcessor processor, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        {
            ps.translate(0.5D, 0.0D, 0.5D);

            ps.mulPose(Axis.YP.rotationDegrees(90));

            switch (ObjBerHelper.meta(processor)) {
                case 2 -> ps.mulPose(Axis.YP.rotationDegrees(0F));
                case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90F));
                case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180F));
                case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270F));
                default -> { }
            }

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
            MODEL.renderPart("Base", ps, vc, light);

            if (processor.hasDrive)
                MODEL.renderPart("Drive", ps, vc, light);
        }
        ps.popPose();
    }
}
