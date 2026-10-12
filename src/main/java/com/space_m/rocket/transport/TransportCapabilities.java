package com.space_m.rocket.transport;

/**
 * NeoForge 1.21.1: Block-Capabilities der Transporter-Rampe (Item-Handler fuer Trichter/Rohre mit den Original-Regeln,
 * Fluid-Handler) je {@code BlockEntityType} anmelden - Muster {@code AtmoCapabilities}. Forge 1.20.1: haengen am BE.
 */
//? if neoforge {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = com.space_m.SpaceMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
*///?}
public final class TransportCapabilities {
    private TransportCapabilities() {}

    //? if neoforge {
    /*@net.neoforged.bus.api.SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void register(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        var type = (net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity>) (Object)
                com.space_m.blockentity.SpaceBlockEntities.TRANSPORTER_ROCKET.get();
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.ITEM_HANDLER, side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, type,
                (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.FLUID_HANDLER, side));
    }
    *///?}
}
