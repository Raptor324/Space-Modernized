package com.space_m.machine.chem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.space_m.machine.chem.ChemMachines;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Client-Anmeldung R7-B Chemie (Original {@code ClientProxy}: {@code RenderAlkylation}, {@code RenderCryoDistill},
 * {@code RenderVacuumCircuit}, {@code RenderMilkReformer}, {@code RenderHydroponic}; GUIs {@code GUIMachineCryoDistill},
 * {@code GUIVacuumCircuit}, {@code GUIMilkReformer}, {@code GUIHydroponic}) und Item-Darstellung der
 * {@code IItemRendererProvider.getRenderer()}-Werte.
 */
public final class ChemClient {
    private ChemClient() {}

    public static void setup() {
        BlockEntityRendererRegistry.register(ChemMachines.ALKYLATION_BE.get(), RenderAlkylation::new);
        BlockEntityRendererRegistry.register(ChemMachines.CRYO_DISTILL_BE.get(), RenderCryoDistill::new);
        BlockEntityRendererRegistry.register(ChemMachines.VACUUM_CIRCUIT_BE.get(), RenderVacuumCircuit::new);
        BlockEntityRendererRegistry.register(ChemMachines.MILK_REFORMER_BE.get(), RenderMilkReformer::new);
        BlockEntityRendererRegistry.register(ChemMachines.HYDROBAY_BE.get(), RenderHydroponic::new);

        MenuRegistry.registerScreenFactory(ChemMachines.CRYO_DISTILL_MENU.get(), GUIMachineCryoDistill::new);
        MenuRegistry.registerScreenFactory(ChemMachines.VACUUM_CIRCUIT_MENU.get(), GUIVacuumCircuit::new);
        MenuRegistry.registerScreenFactory(ChemMachines.MILK_REFORMER_MENU.get(), GUIMilkReformer::new);
        MenuRegistry.registerScreenFactory(ChemMachines.HYDROBAY_MENU.get(), GUIHydroponic::new);

        // RenderAlkylation.getRenderer(): Inventar T(0,-2,0) S(5); S(0.5)
        SpaceMachineItemRenderer.register(ChemMachines.MACHINE_ALKYLATION.get(), new SpaceMachineItemRenderer.Entry() {
            @Override public void inventory(PoseStack ps) { ps.translate(0, -2, 0); ps.scale(5F, 5F, 5F); }
            @Override public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ps.scale(0.5F, 0.5F, 0.5F);
                ChemModels.ALKYL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChemModels.ALKYL_TEX)), light);
            }
        });
        // RenderCryoDistill.getRenderer(): Inventar T(0,-3,0) S(3.5); S(0.5)
        SpaceMachineItemRenderer.register(ChemMachines.MACHINE_CRYO_DISTILL.get(), new SpaceMachineItemRenderer.Entry() {
            @Override public void inventory(PoseStack ps) { ps.translate(0, -3, 0); ps.scale(3.5F, 3.5F, 3.5F); }
            @Override public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ps.scale(0.5F, 0.5F, 0.5F);
                ChemModels.CRYO_DISTILL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChemModels.CRYODISTILL_TEX)), light);
            }
        });
        // RenderVacuumCircuit.getRenderer(): Inventar T(0,-1,0) S(3.5); ganzes Modell
        SpaceMachineItemRenderer.register(ChemMachines.MACHINE_VACUUM_CIRCUIT.get(), new SpaceMachineItemRenderer.Entry() {
            @Override public void inventory(PoseStack ps) { ps.translate(0, -1, 0); ps.scale(3.5F, 3.5F, 3.5F); }
            @Override public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ChemModels.VAC_CIR_STATION.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChemModels.VAC_CIR_STATION_TEX)), light);
            }
        });
        // RenderMilkReformer.getRenderer(): Inventar T(0,-3,0) S(3.5); S(0.5)
        SpaceMachineItemRenderer.register(ChemMachines.MACHINE_MILK_REFORMER.get(), new SpaceMachineItemRenderer.Entry() {
            @Override public void inventory(PoseStack ps) { ps.translate(0, -3, 0); ps.scale(3.5F, 3.5F, 3.5F); }
            @Override public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ps.scale(0.5F, 0.5F, 0.5F);
                ChemModels.MILK_REFORMER.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(ChemModels.MILK_REFORMER_TEX)), light);
            }
        });
        // RenderHydroponic.getRenderer(): Inventar T(0,-1.75,0) S(3); ganzes Modell
        SpaceMachineItemRenderer.register(ChemMachines.HYDROBAY.get(), new SpaceMachineItemRenderer.Entry() {
            @Override public void inventory(PoseStack ps) { ps.translate(0, -1.75, 0); ps.scale(3F, 3F, 3F); }
            @Override public void common(PoseStack ps, MultiBufferSource buf, int light) {
                ChemModels.HYDROPONIC.renderAll(ps, buf.getBuffer(RenderType.entityCutout(ChemModels.HYDROPONIC_TEX)), light);
            }
        });
    }
}
