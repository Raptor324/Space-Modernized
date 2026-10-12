package com.space_m.machine.misc.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.hbm_m.interfaces.IEnergyConnector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.machine.misc.TileEntityMachineSolarPanel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code RenderSolarPanel}: Panel {@code solar_panel} (Rot Y 90, dann 2: 0, 4: 90, 3: 180, 5: 270) und
 * je Stromverbinder am Kern ein Anschlussstueck ({@code connector_front/side/top/bottom}, Ost/West um 90 Grad gedreht),
 * wie {@code Library.canConnect}. Item: Inventar S(3.75); S(1), ganzes Modell ohne Culling.
 * Original-Klasse: RenderSolarPanel
 */
public class RenderSolarPanel implements HbmBerBounds<TileEntityMachineSolarPanel> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/machines/solar_panel.obj"));
    public static final ResourceLocation TEXTURE = SpaceMod.id("textures/block/machine/solar_panel.png");

    public RenderSolarPanel(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineSolarPanel te, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEXTURE));

        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(90));

        switch (ObjBerHelper.meta(te)) {
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case 5 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderPart("solar_panel", ps, vc, light);
        ps.popPose();

        Level level = te.getLevel();
        BlockPos p = te.getBlockPos();

        if (level != null) {
            if (canConnect(level, p.south(), Direction.NORTH))
                MODEL.renderPart("connector_front", ps, vc, light);

            if (canConnect(level, p.north(), Direction.SOUTH))
                MODEL.renderPart("connector_side", ps, vc, light);

            if (canConnect(level, p.east(), Direction.WEST)) {
                ps.mulPose(Axis.YP.rotationDegrees(90));
                MODEL.renderPart("connector_front", ps, vc, light);
                ps.mulPose(Axis.YP.rotationDegrees(-90));
            }

            if (canConnect(level, p.west(), Direction.EAST)) {
                ps.mulPose(Axis.YP.rotationDegrees(90));
                MODEL.renderPart("connector_side", ps, vc, light);
                ps.mulPose(Axis.YP.rotationDegrees(-90));
            }

            if (canConnect(level, p.above(), Direction.DOWN))
                MODEL.renderPart("connector_top", ps, vc, light);

            if (canConnect(level, p.below(), Direction.UP))
                MODEL.renderPart("connector_bottom", ps, vc, light);
        }

        ps.popPose();
    }

    /** {@code Library.canConnect}: Energie-Verbinder, der von dieser Seite aus verbinden will. */
    private static boolean canConnect(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IEnergyConnector c && c.canConnectEnergy(side);
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineSolarPanel be) { return true; }

    /** Original {@code getRenderer()}. */
    public static final ItemRenderMachines.ItemRender ITEM = new ItemRenderMachines.ItemRender() {
        @Override
        public void renderInventory(PoseStack ps) {
            ps.scale(3.75F, 3.75F, 3.75F);
        }

        @Override
        public void renderCommon(PoseStack ps, MultiBufferSource buf, int light) {
            MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light);
        }
    };
}
