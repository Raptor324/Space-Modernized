package com.space_m.satellite;

import com.space_m.celestial.AstronomyUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Die Space-Erweiterung von {@code SatelliteBase} (NTM Space): Bahn- und Darstellungsdaten eines Satelliten im Orbit
 * (Inklination, Hoehe, Phase, Blinken, Besitzer, Farbe) plus die Bahnmathematik.
 *
 * <p>HBM-Modernized ({@code com.hbm_m.satellite.Satellite}) kennt diese Felder nicht und wird vom Addon nicht geaendert;
 * sie liegen deshalb je Frequenz neben dem HBM-Satelliten ({@link SatelliteOrbitSavedData}, Wunsch W-R6C-1 fuer ein
 * Zusatzfeld in HBM). NBT-/Puffer-Schluessel 1:1 Original ({@code satInclination}, {@code satAltitude} ...).
 * Original-Klasse: SatelliteBase (Space-Felder)
 */
public final class SatelliteOrbit {

    public static final float DEFAULT_INCLINATION = 0F;
    public static final float MIN_INCLINATION = -180.0F;
    public static final float MAX_INCLINATION = 180.0F;
    public static final float DEFAULT_ALTITUDE_KM = AstronomyUtil.DEFAULT_ALTITUDE_KM;
    public static final float MIN_ALTITUDE_KM = 80.0F;
    public static final float MAX_ALTITUDE_KM = 125.0F;
    public static final float MIN_BLINK_PERIOD = 0.3F;
    public static final float MAX_BLINK_PERIOD = 1.0F;
    public static final boolean DEFAULT_IS_BLINKING = false;
    public static final float DEFAULT_BLINK_PERIOD = MIN_BLINK_PERIOD;
    public static final String DEFAULT_OWNER = "None";
    public static final float DEFAULT_PHASE_OFFSET = 0.0F;

    public float inclination = DEFAULT_INCLINATION;
    public float altitude = DEFAULT_ALTITUDE_KM;
    public float phaseOffset = DEFAULT_PHASE_OFFSET;

    public boolean isBlinking = DEFAULT_IS_BLINKING;
    public float blinkPeriod = DEFAULT_BLINK_PERIOD;

    public String owner = DEFAULT_OWNER;

    public float colorR;
    public float colorG;
    public float colorB;

    public SatelliteOrbit() {}

    /** Original {@code XSatelliteRegistry.createFromId}: Farbe = registrierte Farbe der Satellitenklasse. */
    public static SatelliteOrbit withColor(float[] color) {
        SatelliteOrbit o = new SatelliteOrbit();
        o.colorR = color[0];
        o.colorG = color[1];
        o.colorB = color[2];
        return o;
    }

    public SatelliteOrbit copy() {
        SatelliteOrbit o = new SatelliteOrbit();
        o.inclination = inclination;
        o.altitude = altitude;
        o.phaseOffset = phaseOffset;
        o.isBlinking = isBlinking;
        o.blinkPeriod = blinkPeriod;
        o.owner = owner;
        o.colorR = colorR;
        o.colorG = colorG;
        o.colorB = colorB;
        return o;
    }

    // ── NBT (Original writeToNBT/readFromNBT, Space-Teil) ──

    public void writeToNBT(CompoundTag nbt) {
        nbt.putFloat("satInclination", inclination);
        nbt.putFloat("satAltitude", altitude);
        nbt.putFloat("satPhaseOffset", normalizePhaseOffset(phaseOffset));
        nbt.putBoolean("satIsBlinking", isBlinking);
        nbt.putFloat("satBlink", blinkPeriod);
        nbt.putString("satOwner", owner);
        nbt.putFloat("satColorR", colorR);
        nbt.putFloat("satColorG", colorG);
        nbt.putFloat("satColorB", colorB);
    }

    /** @param registeredColor Original {@code XSatelliteRegistry.getRegisteredColor(getClass())} fuer fehlende Farbwerte */
    public void readFromNBT(CompoundTag nbt, float[] registeredColor) {
        inclination = nbt.getFloat("satInclination");
        altitude = nbt.contains("satAltitude") ? nbt.getFloat("satAltitude") : DEFAULT_ALTITUDE_KM;
        phaseOffset = nbt.contains("satPhaseOffset") ? normalizePhaseOffset(nbt.getFloat("satPhaseOffset")) : DEFAULT_PHASE_OFFSET;
        isBlinking = nbt.contains("satIsBlinking") ? nbt.getBoolean("satIsBlinking") : DEFAULT_IS_BLINKING;
        blinkPeriod = nbt.contains("satBlink") ? clampBlinkPeriod(nbt.getFloat("satBlink")) : DEFAULT_BLINK_PERIOD;
        owner = nbt.contains("satOwner") ? nbt.getString("satOwner") : DEFAULT_OWNER;
        colorR = nbt.contains("satColorR") ? nbt.getFloat("satColorR") : registeredColor[0];
        colorG = nbt.contains("satColorG") ? nbt.getFloat("satColorG") : registeredColor[1];
        colorB = nbt.contains("satColorB") ? nbt.getFloat("satColorB") : registeredColor[2];
    }

    // ── Puffer (Original serialize/deserialize, Reihenfolge 1:1) ──

    public void serialize(FriendlyByteBuf buf) {
        buf.writeFloat(inclination);
        buf.writeFloat(altitude);
        buf.writeFloat(normalizePhaseOffset(phaseOffset));
        buf.writeUtf(owner);
        buf.writeFloat(colorR);
        buf.writeFloat(colorG);
        buf.writeFloat(colorB);
        buf.writeBoolean(isBlinking);
        buf.writeFloat(blinkPeriod);
    }

    public void deserialize(FriendlyByteBuf buf) {
        inclination = buf.readFloat();
        altitude = buf.readFloat();
        phaseOffset = normalizePhaseOffset(buf.readFloat());
        owner = buf.readUtf();
        colorR = buf.readFloat();
        colorG = buf.readFloat();
        colorB = buf.readFloat();
        isBlinking = buf.readBoolean();
        blinkPeriod = clampBlinkPeriod(buf.readFloat());
    }

    // ── Bahnmathematik (Original-Statics) ──

    public static float clampBlinkPeriod(float blinkPeriod) {
        return Math.max(MIN_BLINK_PERIOD, Math.min(MAX_BLINK_PERIOD, blinkPeriod));
    }

    public static float applyPhaseOffsetToOrbitAngle(float phaseOffset, float altitude, double baseAngle, float fullRotation) {
        double orbitSpeed = getAltitudeOrbitSpeed(altitude);
        double phase = normalizePhaseOffset(phaseOffset) / 360.0D * fullRotation;
        double angle = baseAngle * orbitSpeed + phase;
        double wrapped = angle % fullRotation;
        if (wrapped < 0.0D) wrapped += fullRotation;
        return (float) wrapped;
    }

    public static float getOrbitSpeedKmPerSecond(float altitude) {
        double radiusKm = Math.max(1.0D, altitude);
        double turnsPerSecond = getAltitudeOrbitSpeed(altitude) / 30.0D;
        return (float) (2.0D * Math.PI * radiusKm * turnsPerSecond);
    }

    private static double getAltitudeOrbitSpeed(float altitude) {
        return Math.pow((double) DEFAULT_ALTITUDE_KM / Math.max(1.0D, altitude), 1.5D);
    }

    public static float normalizePhaseOffset(float phaseOffset) {
        float wrapped = phaseOffset % 360.0F;
        if (wrapped < 0.0F) wrapped += 360.0F;
        return wrapped;
    }

    /** Original {@code getBlinkAlpha} (Himmel und Einstellungs-GUI). */
    public static float getBlinkAlpha(boolean isBlinking, float blinkPeriod) {
        if (!isBlinking) return 1.0F;
        long cycleMillis = (long) (clampBlinkPeriod(blinkPeriod) * 1000.0F);
        if (cycleMillis <= 0L) return 1.0F;
        return 1.0F - (float) (System.currentTimeMillis() % cycleMillis) / cycleMillis;
    }
}
