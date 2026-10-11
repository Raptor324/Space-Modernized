package com.space_m.client.sky;

import com.space_m.SpaceMod;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;

/**
 * Clientseitige Himmels-Sounds des Originals ({@code player.playSound("hbm:misc.rumble" | "misc.fireflash" |
 * "misc.impact", 10F, 1F)}). Abgespielt ueber die Sound-ID {@code space_m:<name>}; Eintrag in {@code sounds.json}
 * + OGG liefert R4 (Sounds). Bis dahin meldet Vanilla nur "unknown sound".
 */
public final class SkySounds {
    private SkySounds() {}

    private static final SoundEvent RUMBLE = SoundEvent.createVariableRangeEvent(SpaceMod.id("misc.rumble"));
    private static final SoundEvent FIREFLASH = SoundEvent.createVariableRangeEvent(SpaceMod.id("misc.fireflash"));
    private static final SoundEvent IMPACT = SoundEvent.createVariableRangeEvent(SpaceMod.id("misc.impact"));

    public static void playRumble(LocalPlayer player) { player.playSound(RUMBLE, 10F, 1F); }

    public static void playFireflash(LocalPlayer player) { player.playSound(FIREFLASH, 10F, 1F); }

    public static void playImpact(LocalPlayer player) { player.playSound(IMPACT, 10F, 1F); }
}
