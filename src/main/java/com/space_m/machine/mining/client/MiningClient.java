package com.space_m.machine.mining.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.machine.chem.client.SpaceMachineItemRenderer;
import com.space_m.machine.mining.MiningMachines;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Client-Anmeldung R7-B Bergbau (Original {@code ClientProxy}: {@code RenderMagma}, {@code RenderDecoBlock} fuer das
 * Gas-Dock, {@code RenderDriveProcessor}; GUI {@code GUIMachineDriveProcessor}; Item-Darstellung aus
 * {@code RenderMagma.getRenderer()}, {@code ItemRenderLibrary} (gas_dock) und {@code RenderDriveProcessor.getRenderer()}).
 * Aus {@code SpaceClient.setup}.
 */
public final class MiningClient {
    private MiningClient() {}

    public static void setup() {
        BlockEntityRendererRegistry.register(MiningMachines.MAGMA_BE.get(), RenderMagma::new);
        BlockEntityRendererRegistry.register(MiningMachines.GAS_DOCK_BE.get(), RenderGasDock::new);
        BlockEntityRendererRegistry.register(MiningMachines.DRIVE_PROCESSOR_BE.get(), RenderDriveProcessor::new);

        MenuRegistry.registerScreenFactory(MiningMachines.DRIVE_PROCESSOR_MENU.get(), GUIMachineDriveProcessor::new);

        // RenderMagma.getRenderer()
        SpaceMachineItemRenderer.register(MiningMachines.MACHINE_MAGMA.get(), new SpaceMachineItemRenderer.Entry() {
            @Override
            public void inventory(PoseStack ps) {
                ps.translate(0, -2, 0);
                ps.scale(3, 3, 3);
            }

            @Override
            public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ps.mulPose(Axis.YP.rotationDegrees(90));
                ps.scale(0.5F, 0.5F, 0.5F);
                RenderMagma.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(RenderMagma.TEX)), light);
            }
        });

        // ItemRenderLibrary: gas_dock (Textur sat_dock wie im Original)
        SpaceMachineItemRenderer.register(MiningMachines.GAS_DOCK.get(), new SpaceMachineItemRenderer.Entry() {
            @Override
            public void inventory(PoseStack ps) {
                ps.scale(3, 3, 3);
            }

            @Override
            public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ps.mulPose(Axis.YN.rotationDegrees(90));
                RenderGasDock.SAT_DOCK.renderAll(ps, buf.getBuffer(RenderType.entityCutout(RenderGasDock.SAT_DOCK_TEX)), light);
            }
        });

        // RenderDriveProcessor.getRenderer()
        SpaceMachineItemRenderer.register(MiningMachines.MACHINE_DRIVE_PROCESSOR.get(), new SpaceMachineItemRenderer.Entry() {
            @Override
            public void inventory(PoseStack ps) {
                ps.scale(7F, 7F, 7F);
            }

            @Override
            public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ps.translate(0.5, 0, 0);
                var vc = buf.getBuffer(RenderType.entityCutout(RenderDriveProcessor.TEX));
                RenderDriveProcessor.MODEL.renderPart("Base", ps, vc, light);
                RenderDriveProcessor.MODEL.renderPart("Drive", ps, vc, light);
            }
        });
    }
}
