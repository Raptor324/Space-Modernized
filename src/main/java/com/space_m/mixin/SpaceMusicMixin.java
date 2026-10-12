package com.space_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.space_m.dim.SpaceDimensions;
import com.space_m.sound.SpaceSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;

/**
 * R9-B, Original {@code ModEventHandlerClient.onPlayMusic}: ausserhalb der Erde ({@code WorldProviderCelestial} oder
 * {@code WorldProviderOrbit}, Dimension != 0) ersetzt {@code hbm:music.game.space} die Spiel- und Kreativmusik
 * ({@code music.game} / {@code music.game.creative}). Pausen wie die Vanilla-Spielmusik (12000-24000 Ticks); ein
 * laufendes Lied wird nicht ueberspielt (Original: {@code isSoundPlaying(currentSong) -> DENY}).
 */
@Mixin(Minecraft.class)
public abstract class SpaceMusicMixin {

    @Unique
    private static Music space_m$music;

    @Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
    private void space_m$spaceMusic(CallbackInfoReturnable<Music> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.level == null || mc.player == null) return;
        if (!SpaceDimensions.isSpaceDimension(mc.level.dimension())) return;
        if (space_m$music == null) {
            space_m$music = new Music(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SpaceSounds.MUSIC_GAME_SPACE.get()), 12000, 24000, false);
        }
        cir.setReturnValue(space_m$music);
    }
}
