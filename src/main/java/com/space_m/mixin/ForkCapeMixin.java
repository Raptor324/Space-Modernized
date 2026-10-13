package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.space_m.fork.ForkCapes;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * E11 Umhaenge der Fork-Entwickler (Space {@code ModEventHandlerClient}: {@code player.func_152121_a(Type.CAPE, cloak)}
 * je Bild fuer {@code RenderAccessoryUtility.getCloakFromPlayer}). 1.20.1: Umhang-Textur + "geladen";
 * 1.21.1: {@code PlayerSkin.capeTexture} ersetzen.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class ForkCapeMixin {

    //? if < 1.21.1 {
    @Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
    private void space_m$forkCape(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation cape = ForkCapes.getCloak(((AbstractClientPlayer) (Object) this).getUUID());
        if (cape != null) cir.setReturnValue(cape);
    }

    @Inject(method = "isCapeLoaded", at = @At("HEAD"), cancellable = true)
    private void space_m$forkCapeLoaded(CallbackInfoReturnable<Boolean> cir) {
        if (ForkCapes.getCloak(((AbstractClientPlayer) (Object) this).getUUID()) != null) cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void space_m$forkCape(CallbackInfoReturnable<net.minecraft.client.resources.PlayerSkin> cir) {
        ResourceLocation cape = ForkCapes.getCloak(((AbstractClientPlayer) (Object) this).getUUID());
        if (cape == null) return;
        net.minecraft.client.resources.PlayerSkin s = cir.getReturnValue();
        cir.setReturnValue(new net.minecraft.client.resources.PlayerSkin(s.texture(), s.textureUrl(), cape, s.elytraTexture(), s.model(), s.secure()));
    }
    *///?}
}
