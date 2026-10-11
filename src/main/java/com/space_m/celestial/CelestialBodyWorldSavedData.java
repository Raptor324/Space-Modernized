package com.space_m.celestial;

import com.space_m.platform.SavedDataHooks;
import com.space_m.platform.SpaceSavedData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Zustand je Dimension eines Himmelskoerpers: lokale Uhrzeit (eigene Tageslaenge) und die Zusatzdaten des
 * Welt-Providers, 1:1 NTM Space {@code CelestialBodyWorldSavedData}.
 * Original-Klasse: CelestialBodyWorldSavedData
 *
 * <p>Gespeichert je Dimension ({@link SavedDataHooks#get}, Original {@code perWorldStorage}). Das Original reicht
 * das Tag an {@code WorldProviderCelestial.readFromNBT/writeToNBT} weiter; hier liegt es als {@link #providerData()}
 * (Wetter/sonstige Provider-Felder, Nutzer: Tageslaenge-/Wetter-Teil).
 */
public class CelestialBodyWorldSavedData extends SpaceSavedData {

    public static final String DATA_NAME = "space_m_celestial_body";

    private long localTime;
    private CompoundTag providerData = new CompoundTag();

    public CelestialBodyWorldSavedData() {}

    public static CelestialBodyWorldSavedData get(ServerLevel level) {
        return SavedDataHooks.get(level, DATA_NAME, CelestialBodyWorldSavedData::new, CelestialBodyWorldSavedData::load);
    }

    public static CelestialBodyWorldSavedData load(CompoundTag nbt) {
        CelestialBodyWorldSavedData data = new CelestialBodyWorldSavedData();
        data.localTime = nbt.getLong("time");
        data.providerData = nbt.copy();
        data.providerData.remove("time");
        return data;
    }

    @Override
    protected CompoundTag write(CompoundTag nbt) {
        nbt.merge(providerData);
        nbt.putLong("time", localTime);
        return nbt;
    }

    public long getLocalTime() {
        return localTime;
    }

    public void setLocalTime(long time) {
        localTime = time;
        setDirty();
    }

    /** Zusatzdaten des Providers (veraenderbar; danach {@link #setDirty()}). */
    public CompoundTag providerData() {
        return providerData;
    }
}
