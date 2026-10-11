package com.space_m.atmosphere;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.trait.CBT_Atmosphere;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Vertrag des Original-{@code ItemModOxy} (PLSS/Pinwheel im Helm-Plattenslot), den die Lebenserhaltungs-Runde
 * (R2-B) implementiert. {@link AtmosphereEffects#getOxygenTank} findet ihn im Helm ({@code plate_only}-Slot der
 * HBM-Ruestungsmods), wenn alle vier Teile im Tag {@code space_m:sealed_armor} sind.
 */
public interface IOxygenTank {

	/**
	 * Original {@code ItemModOxy.attemptBreathing}: true, wenn die Entity atmen kann (Umgebung oder Tank); verbraucht
	 * dabei ggf. Tankinhalt. {@code forceConsume} = unter Wasser (Tank immer benutzen).
	 */
	boolean attemptBreathing(LivingEntity entity, ItemStack stack, @Nullable CBT_Atmosphere atmosphere, boolean forceConsume);

	/** Original {@code ItemModOxy.getFuel} (HUD-Balken). */
	int getFuel(ItemStack stack);

	/** Original {@code ItemModOxy.getMaxFuel} (HUD-Balken). */
	int getMaxFuel();
}
