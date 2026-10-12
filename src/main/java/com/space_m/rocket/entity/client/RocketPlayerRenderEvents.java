package com.space_m.rocket.entity.client;

import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.rocket.entity.EntityRideableRocket;

/**
 * Original {@code ModEventHandlerRenderer.onRenderRidingPlayerPre/Post}: ein Spieler in der Rakete wird mit ihrer
 * Neigung mitgedreht ({@code glRotated(-pitch, 0, 0, 1)}). Hier um die Fuesse des Spielers, weil der PoseStack des
 * Ereignisses (anders als 1.7.10) schon auf die Spielerposition verschoben ist.
 */
//? if forge {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
//?} elif neoforge {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = SpaceMod.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RocketPlayerRenderEvents {

    private RocketPlayerRenderEvents() {}

    private static boolean wasRiding;

    //? if forge {
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onRenderRidingPlayerPre(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
    //?} elif neoforge {
    /*@net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onRenderRidingPlayerPre(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event) {
    *///?}
        wasRiding = event.getEntity().getVehicle() instanceof EntityRideableRocket;
        if (!wasRiding) return;

        event.getPoseStack().pushPose();
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(-event.getEntity().getVehicle().getXRot()));
    }

    //? if forge {
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onRenderRidingPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {
    //?} elif neoforge {
    /*@net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onRenderRidingPlayerPost(net.neoforged.neoforge.client.event.RenderPlayerEvent.Post event) {
    *///?}
        if (!wasRiding) return;
        wasRiding = false;

        event.getPoseStack().popPose();
    }
}
