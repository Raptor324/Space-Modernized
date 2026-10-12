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
        event.register(GravityRadiationGameTest.class); // Schwerkraft + Sonnenstrahlung
        event.register(MaterialsGameTest.class); // R4-B
        event.register(com.space_m.rocket.RocketStructGameTest.class); // R5-A
        event.register(com.space_m.rocket.transport.TransportGameTest.class); // R5-C
        event.register(com.space_m.rocket.entity.RocketFlightGameTest.class); // R5-B
        event.register(com.space_m.satellite.SatelliteGameTest.class); // R6-C
        event.register(com.space_m.orbit.propulsion.PropulsionGameTest.class); // R6-B
        event.register(com.space_m.machine.chem.ChemMachinesGameTest.class); // R7-B Chemie
        event.register(com.space_m.machine.mining.MiningMachinesGameTest.class); // R7-B Bergbau
        event.register(com.space_m.machine.mining.DishControlGameTest.class); // R7-B StarDar/Dish
        event.register(com.space_m.entity.mob.MobGameTest.class); // R8-A
        event.register(com.space_m.war.WarResourcesGameTest.class); // R8-B
        event.register(com.space_m.machine.misc.R7aMachinesGameTest.class); // R7-A Dyson/Sonstiges
        event.register(SpaceTabGameTest.class); // Sammel-Tab NTM Space
        event.register(R9aForkGameTest.class); // R9-A X?-Abgleich
        event.register(com.space_m.bomb.BombGameTest.class); // R9-B1 Bomben/Einzelbloecke
        event.register(R9b4GameTest.class); // R9-B4 LPW-2, Angeln, Siege-Craft
        event.register(com.space_m.deco.SpaceDecoGameTest.class); // R9-B2 Deko/Erdbeere/Staemme
    }
}
