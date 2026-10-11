package com.space_m.machine.atmo;

/**
 * NeoForge 1.21.1: Block-Capabilities muessen je {@code BlockEntityType} angemeldet werden. HBM meldet nur seine eigenen
 * Typen an ({@code ModCapabilities.register}); diese Klasse traegt die R2-C-Typen mit derselben Weiche
 * ({@code HbmCaps.query} -> {@code getHbmCapability} der BEs) ein. Forge 1.20.1: Capabilities haengen direkt am BE.
 */
//? if neoforge {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = com.space_m.SpaceMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
*///?}
public final class AtmoCapabilities {
    private AtmoCapabilities() {}

    //? if neoforge {
    /*@net.neoforged.bus.api.SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void register(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        var types = java.util.List.of(
                com.space_m.blockentity.SpaceBlockEntities.ATMOSPHERIC_TOWER,
                com.space_m.blockentity.SpaceBlockEntities.ATMOSPHERIC_VENT,
                com.space_m.blockentity.SpaceBlockEntities.ATMO_EMITTER);
        for (var supplier : types) {
            var type = (net.minecraft.world.level.block.entity.BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity>) (Object) supplier.get();
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, type,
                    (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.FLUID_HANDLER, side));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, type,
                    (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.ENERGY, side));
            event.registerBlockEntity(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER, type,
                    (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_PROVIDER, side));
            event.registerBlockEntity(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER, type,
                    (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_RECEIVER, side));
            event.registerBlockEntity(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR, type,
                    (be, side) -> com.hbm_m.platform.HbmCaps.query(be, com.hbm_m.platform.HbmCap.HBM_ENERGY_CONNECTOR, side));
        }
    }
    *///?}
}
