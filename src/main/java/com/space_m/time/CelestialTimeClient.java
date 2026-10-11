package com.space_m.time;

import net.minecraft.client.Minecraft;

/**
 * Client-Teil von {@link CelestialTime}: Gesamtzeit der Client-Welt fuer die Mondphase auf dem Render-Thread
 * (Original: {@code worldObj.getTotalWorldTime()} im Client-Provider). Nur auf dem Client geladen.
 */
public final class CelestialTimeClient {
    private CelestialTimeClient() {}

    static void init() {
        CelestialTime.clientLevel = () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.isSameThread() ? mc.level : null;
        };
    }
}
