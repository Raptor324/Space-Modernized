package com.space_m.client.sky;

import com.space_m.config.SpaceConfig;

/** Config-Werte, die der Himmel liest (Original {@code GeneralConfig}). */
public final class SkyConfig {
    private SkyConfig() {}

    /** Original {@code GeneralConfig.enableHardcoreDarkness} ("1.44_hardcoreDarkness"). */
    public static boolean enableHardcoreDarkness() {
        return SpaceConfig.get().enableHardcoreDarkness;
    }
}
