package com.space_m.dim.legacy;

/**
 * Ersatz fuer {@code net.minecraft.world.gen.layer.IntCache} (1.7.10), damit portierte Schichten unveraendert
 * {@code IntCache.getIntCache(n)} schreiben koennen. Das Original verwendet Felder wieder (statisch, nicht
 * threadsicher); hier wird jedes Mal ein neues Feld angelegt - die Werte sind identisch.
 */
public final class IntCache {

    private IntCache() {}

    public static int[] getIntCache(int size) {
        return new int[size];
    }

    public static void resetIntCache() {
    }
}
