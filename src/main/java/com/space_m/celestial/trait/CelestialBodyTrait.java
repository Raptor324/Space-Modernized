package com.space_m.celestial.trait;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.HashBiMap;
import com.space_m.celestial.CelestialBody;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Basis aller Himmelskoerper-Eigenschaften, 1:1 NTM Space {@code com.hbm.dim.trait.CelestialBodyTrait}.
 * Original-Klasse: CelestialBodyTrait
 *
 * <p>Namen und Reihenfolge der Registrierung sind identisch zum Original (NBT-Schluessel im SavedData, Index im
 * Sync-Paket). {@code ByteBuf} -> {@link FriendlyByteBuf}, {@code NBTTagCompound} -> {@link CompoundTag}.
 */
public abstract class CelestialBodyTrait {

    // Similarly to fluid traits, we have classes, and instance members.
    // For the simple traits, we'll just init both here rather than two places.

    /** Original-Klasse: CBT_BATTLEFIELD */
    public static class CBT_BATTLEFIELD extends CelestialBodyTrait { }
    public static final CBT_BATTLEFIELD BATTLE = new CBT_BATTLEFIELD();

    /** Original-Klasse: CBT_COMPROMISED */
    public static class CBT_COMPROMISED extends CelestialBodyTrait { }
    public static final CBT_COMPROMISED COMP = new CBT_COMPROMISED();

    // Constructor and loading
    public static final List<Class<? extends CelestialBodyTrait>> traitList = new ArrayList<>();
    public static final HashBiMap<String, Class<? extends CelestialBodyTrait>> traitMap = HashBiMap.create();
    private static final List<Supplier<? extends CelestialBodyTrait>> factories = new ArrayList<>();

    static {
        registerTrait("atmosphere", CBT_Atmosphere.class, CBT_Atmosphere::new);
        registerTrait("temperature", CBT_Temperature.class, CBT_Temperature::new);
        registerTrait("bees", CBT_Bees.class, CBT_Bees::new);
        registerTrait("war", CBT_War.class, CBT_War::new);
        registerTrait("destroyed", CBT_Destroyed.class, CBT_Destroyed::new);
        registerTrait("water", CBT_Water.class, CBT_Water::new);
        registerTrait("weather", CBT_Weather.class, CBT_Weather::new);
        registerTrait("battle", CBT_BATTLEFIELD.class, CBT_BATTLEFIELD::new);
        registerTrait("infected", CBT_COMPROMISED.class, CBT_COMPROMISED::new);
        registerTrait("dyson", CBT_Dyson.class, CBT_Dyson::new);
        registerTrait("impact", CBT_Impact.class, CBT_Impact::new);
        registerTrait("lights", CBT_Lights.class, CBT_Lights::new);
        registerTrait("invasion", CBT_Invasion.class, CBT_Invasion::new);
    }

    private static <T extends CelestialBodyTrait> void registerTrait(String name, Class<T> clazz, Supplier<T> factory) {
        traitList.add(clazz);
        traitMap.put(name, clazz);
        factories.add(factory);
    }

    /** Neue Instanz der Eigenschaft (Original {@code clazz.newInstance()}). */
    public static CelestialBodyTrait create(Class<? extends CelestialBodyTrait> clazz) {
        return factories.get(traitList.indexOf(clazz)).get();
    }

    /** Neue Instanz nach Registrierungsindex (Sync-Paket). */
    public static CelestialBodyTrait create(int index) {
        return factories.get(index).get();
    }

    // Serialization
    public void readFromNBT(CompoundTag nbt) { }
    public void writeToNBT(CompoundTag nbt) { }

    public void readFromBytes(FriendlyByteBuf buf) { }
    public void writeToBytes(FriendlyByteBuf buf) { }

    public void update(boolean isRemote, CelestialBody body) { }
}
