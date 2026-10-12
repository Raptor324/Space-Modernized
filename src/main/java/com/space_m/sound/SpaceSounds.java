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

    // R5-C Drop-Pods: Original "block.hatchOpen" (misc/bomb_open) / "block.hatchImpact" (misc/bombimpact), TileEntityCombatDropPod/EntityCombatDropPod
    public static final RegistrySupplier<SoundEvent> BLOCK_HATCHOPEN = register("block.hatchOpen");
    public static final RegistrySupplier<SoundEvent> BLOCK_HATCHIMPACT = register("block.hatchImpact");

    // R5-B Rakete (Original EntityRideableRocket: Zuendung, Flugschleifen leicht/schwer, Stufentrennung, Umkippen)
    public static final RegistrySupplier<SoundEvent> ENTITY_ROCKET_IGNITION = register("entity.rocketIgnition");
    public static final RegistrySupplier<SoundEvent> ENTITY_ROCKET_FLY_LIGHT = register("entity.rocketFlyLight");
    public static final RegistrySupplier<SoundEvent> ENTITY_ROCKET_FLY_HEAVY = register("entity.rocketFlyHeavy");
    public static final RegistrySupplier<SoundEvent> ENTITY_ROCKET_STAGE = register("entity.rocketStage");
    public static final RegistrySupplier<SoundEvent> ENTITY_PIPEFAIL = register("entity.pipefail");

    // R6-B Stationsantrieb (Original TileEntityMachineHTR3/HTRF4/HTRNeo/LPW2: Start, Stopp, Schleife)
    public static final RegistrySupplier<SoundEvent> MISC_LPWSTART = register("misc.lpwstart");
    public static final RegistrySupplier<SoundEvent> MISC_LPWSTOP = register("misc.lpwstop");
    public static final RegistrySupplier<SoundEvent> MISC_LPWLOOP = register("misc.lpwloop");
    public static final RegistrySupplier<SoundEvent> MISC_HTRSTART = register("misc.htrstart");
    public static final RegistrySupplier<SoundEvent> MISC_HTRSTOP = register("misc.htrstop");
    public static final RegistrySupplier<SoundEvent> MISC_HTRFSTART = register("misc.htrfstart");
    public static final RegistrySupplier<SoundEvent> MISC_HTRLOOP = register("misc.htrloop");

    // R6-C Kriegssatellit (Original SatelliteWar/SatelliteRailgun.playsound: "hbm:misc.fireflash" -> misc/flashfire)
    public static final RegistrySupplier<SoundEvent> MISC_FIREFLASH = register("misc.fireflash");

    // R6-A Station: Brummen an Bord (Original ModEventHandlerClient, "hbm:misc.stationhum", Kategorie ambient)
    public static final RegistrySupplier<SoundEvent> MISC_STATIONHUM = register("misc.stationhum");

    // R8-B Krieg/Invasion: Einschlag (misc/cenimp, ModEventHandlerClient/WorldProviderThatmo), Carpet-Jet-Schleife
    // (EntityBomber Typ >= 9), Behemoth-Schritt (EntityAIStepTowardsTarget), Invasionswarnung (CBT_Invasion), Siege-UFO-Schuss
    public static final RegistrySupplier<SoundEvent> MISC_IMPACT = register("misc.impact");
    public static final RegistrySupplier<SoundEvent> ENTITY_JETLOOPBOMB = register("entity.jetloopbomb", 250F); // Original getLoopedSound(..., 2F, 250F, ...)
    public static final RegistrySupplier<SoundEvent> ENTITY_BEEP = register("entity.beep");
    public static final RegistrySupplier<SoundEvent> ALARM_PING = register("alarm.ping");
    public static final RegistrySupplier<SoundEvent> ENTITY_BFASHOOT = register("entity.bfaShoot");

    // R8-A Mobs: Radioaktiver Engel laedt den Strahl auf (Original EntityBFAngel "hbm:entity.bfatalk", entity/bfa_talk)
    public static final RegistrySupplier<SoundEvent> ENTITY_BFATALK = register("entity.bfatalk");

    // R7-A Dyson-System (Original TileEntityDysonLauncher "hbm:misc.spincharge"/"hbm:misc.spinshot", TileEntityDysonReceiver "hbm:block.dysonBeam")
    public static final RegistrySupplier<SoundEvent> MISC_SPINCHARGE = register("misc.spincharge");
    public static final RegistrySupplier<SoundEvent> MISC_SPINSHOT = register("misc.spinshot");
    public static final RegistrySupplier<SoundEvent> BLOCK_DYSONBEAM = register("block.dysonBeam");

    // R9-B: Weltraummusik (Original ModEventHandlerClient.onPlayMusic, "hbm:music.game.space"), Schallplatten
    // (ItemModRecord gs/gp/el), Animan-Pille (ItemPill "hbm:player.oil"), Eve-Gewitter (WorldProviderEve "hbm:misc.rumble"),
    // Satelliten-Einstellungen Batterie (GUIScreenSatSettings "item.unpackInsert/Remove" -> tool/extract2/1)
    public static final RegistrySupplier<SoundEvent> MUSIC_GAME_SPACE = register("music.game.space");
    public static final RegistrySupplier<SoundEvent> MUSIC_RECORD_ETHEREAL = register("music.recordEthereal");
    public static final RegistrySupplier<SoundEvent> MUSIC_RECORD_GODSPEED = register("music.recordGodSpeed");
    public static final RegistrySupplier<SoundEvent> MUSIC_RECORD_GOOP = register("music.recordGoop");
    public static final RegistrySupplier<SoundEvent> PLAYER_OIL = register("player.oil");
    public static final RegistrySupplier<SoundEvent> MISC_RUMBLE = register("misc.rumble");
    public static final RegistrySupplier<SoundEvent> ITEM_UNPACK_INSERT = register("item.unpackInsert");
    public static final RegistrySupplier<SoundEvent> ITEM_UNPACK_REMOVE = register("item.unpackRemove");

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
