package com.space_m.atmosphere;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.trait.CBT_Atmosphere;

/**
 * 1:1 NTM Space {@code IPlantableBreathing}: Pflanze mit eigener Atmosphaeren-Bedingung (Original-Nutzer
 * {@code BlockCrop}, {@code BlockNTSapling}). Abweichung: {@code IPlantable} gibt es auf NeoForge 1.21.1 nicht
 * mehr - "Pflanze" erkennt {@link ChunkAtmosphereHandler#isPlant} (Vanilla-Pflanzenklassen + Tag
 * {@code space_m:breathing_plants} + dieses Interface).
 */
public interface IPlantableBreathing {

	public boolean canBreathe(@Nullable CBT_Atmosphere atmosphere);

}
