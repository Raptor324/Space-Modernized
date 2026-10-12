package com.space_m.machine.misc.client;

import com.space_m.machine.misc.MiscBlockEntities;
import com.space_m.machine.misc.MiscBlocks;
import com.space_m.machine.misc.MiscMenus;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;

/**
 * Client-Anmeldung R7-A Sonstiges (Original {@code ClientProxy}: {@code RenderSolarPanel}, {@code RenderStardar},
 * {@code RenderRadiator}; GUIs {@code GUIMachineStardar}, {@code GUIWarController}; Item-Renderer der Maschinen).
 */
public final class MiscClient {

    private MiscClient() {}

    /** Aus {@code SpaceClient.setup} (Client-Setup). */
    public static void setup() {
        BlockEntityRendererRegistry.register(MiscBlockEntities.SOLAR_PANEL.get(), RenderSolarPanel::new);
        BlockEntityRendererRegistry.register(MiscBlockEntities.STARDAR.get(), RenderStardar::new);
        BlockEntityRendererRegistry.register(MiscBlockEntities.RADIATOR.get(), RenderRadiator::new);

        MenuRegistry.registerScreenFactory(MiscMenus.STARDAR.get(), GUIMachineStardar::new);
        MenuRegistry.registerScreenFactory(MiscMenus.WAR_CONTROLLER.get(), GUIWarController::new);

        ItemRenderMachines.register(MiscBlocks.MACHINE_SOLAR, RenderSolarPanel.ITEM);
        ItemRenderMachines.register(MiscBlocks.MACHINE_STARDAR, RenderStardar.ITEM);
        ItemRenderMachines.register(MiscBlocks.MACHINE_RADIATOR, RenderRadiator.ITEM);
    }
}
