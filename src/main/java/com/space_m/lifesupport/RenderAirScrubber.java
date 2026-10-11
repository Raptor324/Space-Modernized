package com.space_m.lifesupport;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 NTM Space {@code RenderAirScrubber}: Gehaeuse ({@code Base}) und der um die Hochachse drehende Luefter
 * ({@code Fan}, {@code prevRot/rot}) aus {@code air_scrubber.obj}; Grunddrehung 180 Grad, dann Metadaten 2/4/3/5 ->
 * 0/90/180/270 Grad. Das Item zeigt das ganze Modell (statisches OBJ-Itemmodell, Original {@code renderAll}).
 * Original-Klasse: RenderAirScrubber
 */
public class RenderAirScrubber implements com.hbm_m.client.render.HbmBerBounds<TileEntityAirScrubber> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/block/air_scrubber.obj"));
    public static final ResourceLocation TEX = SpaceMod.id("textures/block/air_scrubber.png");

    public RenderAirScrubber(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityAirScrubber scrubber, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        ps.mulPose(Axis.YP.rotationDegrees(180));

        switch (scrubber.getBlockState().getValue(BlockAirScrubber.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(0));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        float rotation = scrubber.prevRot + (scrubber.rot - scrubber.prevRot) * interp;

        ps.mulPose(Axis.YP.rotationDegrees(rotation));
        MODEL.renderPart("Fan", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        ps.popPose();
    }
}
