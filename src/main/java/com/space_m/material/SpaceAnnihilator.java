package com.space_m.material;

import java.math.BigInteger;
import java.util.ArrayList;

import com.hbm_m.annihilator.AnnihilatorRecipes;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.item.industrial.BlueprintPools;
import com.hbm_m.item.industrial.ItemBlueprints;

import dev.architectury.event.events.common.LifecycleEvent;

/**
 * R9-A: Original {@code AnnihilatorRecipes.registerDefaults}, Block "// space fork" (nur im 528-Modus):
 * Aluminiumbarren 512 -> Bauplan {@code 528:aluminium}, Edelstahlbarren 1024 -> {@code 528:stainless},
 * Gas-Chip ({@code circuit} GASCHIP) 512 -> {@code 528:gaschip}.
 *
 * <p>HBM fuehrt die Tabelle als offene Map {@link AnnihilatorRecipes#recipes} und fuellt sie selbst erst beim ersten
 * Annihilator-Zugriff ({@code registerDefaults()} ist faul und haengt an der Server-Config {@code enable528}). Das
 * Addon traegt seine drei Schluessel deshalb beim Serverstart nach (eigene Schluessel, HBMs Eintraege bleiben
 * unberuehrt). Schluessel wie im Port: {@code dict:<Item-Tag>} (1.20.1 {@code forge:}, 1.21.1 {@code c:}) bzw.
 * {@code comp:<Item-ID>} fuer das frueher per Metadaten unterschiedene Schaltkreis-Item.
 */
public final class SpaceAnnihilator {

    //? if >=1.21.1 {
    /*private static final String TAGS = "c:";
    *///?} else {
    private static final String TAGS = "forge:";
    //?}

    private static boolean added;

    private SpaceAnnihilator() {}

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(server -> addSpaceRecipes());
    }

    /** Fuegt die drei Space-Eintraege einmalig hinzu, wenn der 528-Modus aktiv ist. */
    public static synchronized void addSpaceRecipes() {
        if (added || !GeneralConfig.enable528) return;
        added = true;
        put("dict:" + TAGS + "ingots/aluminum", "512", "aluminium");
        put("dict:" + TAGS + "ingots/stainless_steel", "1024", "stainless");
        put("comp:space_m:gaschip", "512", "gaschip");
    }

    /** true, sobald die Space-Eintraege in HBMs Tabelle stehen (GameTest). */
    public static boolean added() { return added; }

    private static void put(String key, String amount, String pool) {
        AnnihilatorRecipes.recipes.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new AnnihilatorRecipes.Milestone(new BigInteger(amount), () -> ItemBlueprints.make(BlueprintPools.POOL_PREFIX_528 + pool)));
    }
}
