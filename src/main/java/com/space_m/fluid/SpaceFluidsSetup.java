package com.space_m.fluid;

/**
 * Wendet Temperatur/Traits/Gefahrenraute/Behaelter der Space-Fluide an ({@link SpaceFluids#setup()}).
 *
 * <p>Reihenfolge (W-R4A-2): HBMs {@code ModFluidTraitsBootstrap} laeuft im Common-Setup und schaetzt dort die
 * Gefahrenraute aller {@code ModFluids}-Eintraege (auch der Space-Fluide) bzw. setzt HBM-Traits ({@code COOLANT}-Heizkette,
 * Kerosin-Brennwert). Frueher lief das Addon deshalb erst in {@code FMLLoadCompleteEvent}; jetzt meldet
 * {@link SpaceFluids#init()} {@link #run} bei {@code HbmFluidApi.afterHbmFluidDefaults} an - HBM ruft es direkt nach seiner
 * Vorbelegung auf (gleicher Thread, beide Loader), also nie davor und ohne vom Lade-Zeitpunkt anderer Mods abzuhaengen.
 */
public final class SpaceFluidsSetup {

    private SpaceFluidsSetup() {}

    static void run() {
        SpaceFluids.setup();
    }
}
