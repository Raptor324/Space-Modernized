package com.space_m.satellite;

import com.hbm_m.platform.PlatformHooks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Die Satellitendaten eines Satelliten-Gegenstands (Original {@code SatelliteBase.copyItemData/ensureItemData/getItemData}
 * und die Getter/Setter je Schluessel), NBT-Schluessel 1:1 ({@code satInclination} ...).
 *
 * <p>Abweichung (technisch): das Original legt fehlende Werte beim <i>Lesen</i> an ({@code getItemData} schreibt ins NBT).
 * Auf 1.21.1 sind Gegenstandsdaten unveraenderlich (Data-Components), Lesen darf den Stapel nicht aendern; daher liefern
 * die Getter dieselben Standardwerte ohne zu schreiben, und nur {@link #ensureItemData} schreibt. Fehlen bei einem
 * vorhandenen NBT Hoehe/Besitzer/Farbe (z.B. HBM-Satellitenchip, dem der Verknuepfer nur {@code freq} gab), gelten
 * ebenfalls die Standardwerte (Original: 0 bzw. leer, weil dort jeder Satellit beim Herstellen {@code ensureItemData} bekam).
 * Original-Klasse: SatelliteBase (Gegenstands-Statics)
 */
public final class SatelliteItemData {

    private SatelliteItemData() {}

    /** Original {@code copyItemData}. */
    public static void copyItemData(ItemStack from, ItemStack to) {
        if (to == null || to.isEmpty()) return;
        setInclination(to, getInclination(from));
        setAltitude(to, getAltitude(from));
        setPhaseOffset(to, getPhaseOffset(from));
        setOwner(to, getOwner(from));
        setColor(to, getColorR(from), getColorG(from), getColorB(from));
        setBlinking(to, isBlinking(from));
        setBlinkPeriod(to, getBlinkPeriod(from));
    }

    /** Original {@code ensureItemData}: fehlende Werte ins NBT schreiben. */
    public static void ensureItemData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        SatelliteOrbit o = read(stack);
        PlatformHooks.editItemTag(stack, o::writeToNBT);
    }

    /** Alle Werte des Stapels (fehlende = Standard/registrierte Farbe). */
    public static SatelliteOrbit read(ItemStack stack) {
        SatelliteOrbit o = new SatelliteOrbit();
        CompoundTag nbt = stack == null ? null : PlatformHooks.getItemTag(stack);
        float[] color = SpaceSatelliteRegistry.getRegisteredColorOrDefault(stack == null ? null : stack.getItem());
        if (nbt == null) nbt = new CompoundTag();
        o.readFromNBT(nbt, color);
        return o;
    }

    public static float getInclination(ItemStack stack) { return read(stack).inclination; }

    public static float getAltitude(ItemStack stack) { return read(stack).altitude; }

    public static float getPhaseOffset(ItemStack stack) { return read(stack).phaseOffset; }

    public static String getOwner(ItemStack stack) { return read(stack).owner; }

    public static float getColorR(ItemStack stack) { return read(stack).colorR; }

    public static float getColorG(ItemStack stack) { return read(stack).colorG; }

    public static float getColorB(ItemStack stack) { return read(stack).colorB; }

    public static float getBlinkPeriod(ItemStack stack) { return read(stack).blinkPeriod; }

    public static boolean isBlinking(ItemStack stack) { return read(stack).isBlinking; }

    public static void setInclination(ItemStack stack, float inclination) {
        PlatformHooks.editItemTag(stack, t -> t.putFloat("satInclination", inclination));
    }

    public static void setAltitude(ItemStack stack, float altitude) {
        PlatformHooks.editItemTag(stack, t -> t.putFloat("satAltitude", altitude));
    }

    public static void setPhaseOffset(ItemStack stack, float phaseOffset) {
        PlatformHooks.editItemTag(stack, t -> t.putFloat("satPhaseOffset", SatelliteOrbit.normalizePhaseOffset(phaseOffset)));
    }

    public static void setOwner(ItemStack stack, String owner) {
        PlatformHooks.editItemTag(stack, t -> t.putString("satOwner", owner));
    }

    public static void setColor(ItemStack stack, float r, float g, float b) {
        PlatformHooks.editItemTag(stack, t -> {
            t.putFloat("satColorR", r);
            t.putFloat("satColorG", g);
            t.putFloat("satColorB", b);
        });
    }

    public static void setBlinking(ItemStack stack, boolean isBlinking) {
        PlatformHooks.editItemTag(stack, t -> t.putBoolean("satIsBlinking", isBlinking));
    }

    public static void setBlinkPeriod(ItemStack stack, float blinkPeriod) {
        PlatformHooks.editItemTag(stack, t -> t.putFloat("satBlink", SatelliteOrbit.clampBlinkPeriod(blinkPeriod)));
    }
}
