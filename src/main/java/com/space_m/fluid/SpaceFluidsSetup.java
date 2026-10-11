package com.space_m.fluid;

import com.space_m.SpaceMod;

//? if forge {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
//?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
*///?}

/**
 * Wendet Temperatur/Traits/Gefahrenraute/Behaelter der Space-Fluide an ({@link SpaceFluids#setup()}), sobald alle
 * Mods ihr Common-Setup beendet haben. Grund: HBMs {@code ModFluidTraitsBootstrap} laeuft in {@code LifecycleEvent.SETUP}
 * und schaetzt dort die Gefahrenraute aller {@code ModFluids}-Eintraege (also auch der Space-Fluide) neu bzw. setzt
 * HBM-Traits ({@code COOLANT}-Heizkette, Kerosin-Brennwert); die Mod-Konstruktoren laufen parallel, daher ist die
 * Reihenfolge der SETUP-Listener nicht festgelegt. {@code FMLLoadCompleteEvent} liegt auf beiden Loadern sicher danach;
 * {@code enqueueWork} fuehrt auf dem Hauptthread aus.
 */
//? if forge {
@Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} elif neoforge {
/*@EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?}
public final class SpaceFluidsSetup {

    private SpaceFluidsSetup() {}

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(SpaceFluids::setup);
    }
}
