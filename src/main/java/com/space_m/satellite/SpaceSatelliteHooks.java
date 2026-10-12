package com.space_m.satellite;

import com.hbm_m.api.space.SatelliteHooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * HBM-Satelliten-Haken (Space-API W-R6C-2..4): koerpergebundene Satellitendaten in Verknuepfer/Dock/Horizons/
 * {@code /ntmsatellites}, Aussetzen ueber {@link SpaceSatellites#orbit} (auch Soyuz/Lambda/Befehl), Bahndaten beim
 * Verknuepfer kopieren, {@code ItemSatellite}-Tooltip statt {@code satchip.*}.
 * Original-Klassen: SatelliteSavedData.getData(world, x, z), XSatelliteRegistry.orbit, TileEntityMachineSatLinker, ItemSatellite
 */
public final class SpaceSatelliteHooks implements SatelliteHooks {

    @Override
    public ServerLevel satelliteLevel(ServerLevel level, int x, int z) {
        return SpaceSatellites.getLevel(level, x, z);
    }

    @Override
    public boolean orbit(ServerLevel level, ItemStack stack, int freq, double x, double y, double z) {
        SpaceSatellites.orbit(level, stack, freq, x, y, z);
        return true;
    }

    @Override
    public void onLinkerCopy(ItemStack source, ItemStack target) {
        // Original: if(isSatelliteItem(slot0) && isSatelliteItem(slot1)) SatelliteBase.copyItemData(slot0, slot1)
        if (SpaceSatelliteRegistry.isSatelliteItem(source.getItem()) && SpaceSatelliteRegistry.isSatelliteItem(target.getItem())) {
            SatelliteItemData.copyItemData(source, target);
        }
    }

    @Override
    public boolean showHbmChipDescription(ItemStack stack) {
        // HBM-Nutzlasten sind im Original ItemSatellite: Beschreibung item.sat.desc.* (SatelliteClient) statt satchip.*
        return !SatelliteEvents.isHbmPayloadSatellite(stack.getItem());
    }
}
