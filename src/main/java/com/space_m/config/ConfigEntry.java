package com.space_m.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadaten eines Config-Feldes von {@link SpaceConfig} (Gegenstueck zu HBMs {@code ConfigSchema}/{@code ConfigField}).
 * Schluessel = Original-Schluessel aus der 1.7.10-{@code hbm.cfg} bzw. {@code hbmClient.json}, damit Werte
 * 1:1 uebertragbar bleiben.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {
    /** Original-Kategorie ({@code CommonConfig.CATEGORY_*}). */
    String category();

    /** Original-Schluessel, z.B. {@code 1.90_maxProbeDistance}. */
    String key();

    /** Original-Kommentar. */
    String comment();

    /** Original-Klasse ({@code SpaceConfig}, {@code WorldConfig} ...), nur Doku/Abgleich. */
    String origin();

    boolean client() default false;

    double min() default -Double.MAX_VALUE;

    double max() default Double.MAX_VALUE;
}
