package com.space_m.sound;

import com.space_m.SpaceMod;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound-Events des Addons (Original: 51 neue Eintraege in {@code assets/hbm/sounds.json} von NTM Space,
 * z.B. {@code alarm.ducc}, {@code block.dysonBeam}, {@code music.game.space}).
 *
 * <p>ID = Original-Eventname unveraendert (Punkte bleiben; Gross-/Kleinschreibung -> klein, Minecraft verlangt
 * Kleinbuchstaben: {@code block.dysonBeam} -> {@code block.dysonbeam}). Die {@code sounds.json} des Addons fuehrt
 * dieselben Schluessel; OGGs unter {@code assets/space_m/sounds/}.
 */
public final class SpaceSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.SOUND_EVENT);

    // ── Eintraege (R4: 51 Sounds) ─────────────────────────────────────────────────────────────

    // R2-B Lebenserhaltung: PLSS-Atemgeraeusch (Original "player.plss_breathing", ItemModOxy)
    public static final RegistrySupplier<SoundEvent> PLAYER_PLSS_BREATHING = register("player.plss_breathing");

    private SpaceSounds() {}

    /** Registriert {@code space_m:<originalName in Kleinbuchstaben>}. */
    public static RegistrySupplier<SoundEvent> register(String originalName) {
        String id = originalName.toLowerCase(java.util.Locale.ROOT);
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(SpaceMod.id(id)));
    }

    /** Sound mit fester Hoerweite (Original: {@code "attenuation_distance"}). */
    public static RegistrySupplier<SoundEvent> register(String originalName, float range) {
        String id = originalName.toLowerCase(java.util.Locale.ROOT);
        return SOUNDS.register(id, () -> SoundEvent.createFixedRangeEvent(SpaceMod.id(id), range));
    }

    public static void init() {
        SOUNDS.register();
    }
}
