package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

/**
 * R5-B Rakete, Original {@code ModEventHandlerRenderer.onRenderTickPre}: "Zoom out third person view when inside a
 * rocket" - {@code thirdPersonDistance = 12} statt 4, solange der Spieler in einer {@code EntityRideableRocket} sitzt.
 * (Vanilla hat keinen Abstandswert mehr; der Startabstand von {@code Camera.getMaxZoom} wird verdreifacht.)
 */
@Mixin(Camera.class)
public abstract class RocketCameraMixin {

    //? if < 1.21.1 {
    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private double space_m$rocketZoom(double distance) {
        return space_m$inRocket() ? distance * 3.0D : distance;
    }
    //?} else {
    /*@ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private float space_m$rocketZoom(float distance) {
        return space_m$inRocket() ? distance * 3.0F : distance;
    }
    *///?}

    private static boolean space_m$inRocket() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof com.space_m.rocket.entity.EntityRideableRocket;
    }
}
