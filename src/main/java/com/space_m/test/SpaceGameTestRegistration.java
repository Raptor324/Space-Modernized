package com.space_m.test;

import com.space_m.SpaceMod;

/**
 * Meldet die GameTests des Addons auf beiden Loadern an (Muster {@code com.hbm_m.test.GameTestRegistration}).
 * Lauf: {@code :1.20.1-forge:runGameTestServer} / {@code :1.21.1-neoforge:runGameTestServer}
 * (Namespace-Filter {@code space_m} in den Build-Skripten). Neue Testklassen in beiden Zweigen eintragen.
 */
//? if forge {
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = SpaceMod.MOD_ID)
*///?}
public final class SpaceGameTestRegistration {

    private SpaceGameTestRegistration() {}

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(SpaceFoundationGameTest.class);
        event.register(CelestialGameTest.class); // R1-A
        event.register(SpaceTravelGameTest.class); // R1-D
        event.register(DimensionGameTest.class); // R1-B
        event.register(EveMohoDimensionGameTest.class); // R3-3
        event.register(MinmusIkeGameTest.class); // R3-1
        event.register(DunaDresGameTest.class); // R3-2
        event.register(LaytheTektoThatmoGameTest.class); // R3-4
        event.register(LifeSupportGameTest.class); // R2-B
        event.register(AtmosphereGameTest.class); // R2-A
        event.register(com.space_m.machine.atmo.AtmoMachinesGameTest.class); // R2-C
        event.register(com.space_m.fluid.SpaceFluidsGameTest.class); // R4-A
    }
}
