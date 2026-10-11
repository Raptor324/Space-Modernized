package com.space_m.platform;

import java.nio.file.Path;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

/**
 * Allgemeine Loader-/Umgebungsabfragen des Addons (Muster {@code com.hbm_m.platform.PlatformHooks}).
 * Alle {@code com.space_m.platform}-Fassaden kapseln Versions-/Loader-Unterschiede, damit Fachcode ohne
 * Stonecutter-Weichen auskommt:
 * <ul>
 *   <li>{@link GravityHooks} - Schwerkraft-Attribut (Forge {@code ForgeMod.ENTITY_GRAVITY} / 1.21.1 {@code Attributes.GRAVITY})</li>
 *   <li>{@link TeleportHooks} - Dimensionswechsel (Forge {@code ITeleporter} / 1.21.1 {@code DimensionTransition})</li>
 *   <li>{@link SavedDataHooks} / {@link SpaceSavedData} - SavedData-Factory und {@code save}-Signatur</li>
 *   <li>{@link CapabilityHooks} - Item-Capabilities (Forge {@code ForgeCapabilities} / NeoForge ueber HBMs Bruecke)</li>
 *   <li>Netzwerk: {@link com.space_m.network.SpacePacketHandler} (Architectury, Buffertyp je Version)</li>
 * </ul>
 */
public final class SpacePlatform {
    private SpacePlatform() {}

    /** Globaler {@code <game>/config}-Ordner. */
    public static Path configDir() {
        return Platform.getConfigFolder();
    }

    public static boolean isClient() {
        return Platform.getEnvironment() == Env.CLIENT;
    }

    public static boolean isModLoaded(String modId) {
        return Platform.isModLoaded(modId);
    }

    public static boolean isForge() {
        //? if forge {
        return true;
        //?} else {
        /*return false;
        *///?}
    }
}
