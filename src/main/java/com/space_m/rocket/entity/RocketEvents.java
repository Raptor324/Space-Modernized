package com.space_m.rocket.entity;

import com.space_m.SpaceMod;

/**
 * Gemeinsame Ereignisse der fliegenden Rakete (Original {@code ModEventHandler} / {@code EntityEffectHandler}):
 * <ul>
 *   <li>Atmen in der Kapsel ({@code EntityEffectHandler.handleOxy/handleCorrosion}: "breathe easy in your ship") ueber
 *       {@code AtmosphereEffects.breathableVehicle} (R2-A).</li>
 *   <li>Kein Aussteigen im Flug ({@code ModEventHandler.worldTick}): Schleichen erhoeht {@code forceExitTimer}, erst
 *       nach 60 Ticks oder im Stand darf man raus. Der Ausstieg wird ueber {@code EntityMountEvent} (Forge/NeoForge,
 *       abbrechbar) verhindert - Vanilla wirft den Spieler sonst schon beim ersten Schleich-Tick ab.</li>
 * </ul>
 * Aufruf {@link #init()} aus {@code SpaceRegistry.init()}.
 */
//? if forge {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
//?} elif neoforge {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = SpaceMod.MOD_ID)
*///?}
public final class RocketEvents {

    private RocketEvents() {}

    public static void init() {
        // breathe easy in your ship
        com.space_m.atmosphere.AtmosphereEffects.breathableVehicle = vehicle -> vehicle instanceof EntityRideableRocket;
    }

    //? if forge {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onMount(net.minecraftforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof EntityRideableRocket rocket
                && rocket.blockDismount(event.getEntityMounting())) {
            event.setCanceled(true);
        }
    }
    //?} elif neoforge {
    /*@net.neoforged.bus.api.SubscribeEvent
    public static void onMount(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof EntityRideableRocket rocket
                && rocket.blockDismount(event.getEntityMounting())) {
            event.setCanceled(true);
        }
    }
    *///?}
}
