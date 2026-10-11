package com.space_m.client.sky;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/**
 * Die Shader-Felder aus {@code SkyProviderCelestial} (NTM Space) plus der Koerper-Shader ({@code CelestialBody.withShader},
 * z.B. {@code shaders/blackhole.frag} der Sonne). Alle Programme werden beim ersten Gebrauch gebaut.
 */
public final class SkyShaders {
    private SkyShaders() {}

    public static final SkyShader FLESH = new SkyShader("fle");
    public static final SkyShader CRESCENT = new SkyShader("crescent");
    public static final SkyShader ATMOSPHERE = new SkyShader("atmosphere");
    public static final SkyShader ATMOSPHERE_EMISSIVE = new SkyShader("atmosphere_emissive");
    public static final SkyShader LIGHTNING = new SkyShader("lightning");
    public static final SkyShader NUKE = new SkyShader("nuke");
    public static final SkyShader NIGHT_LIGHTS = new SkyShader("nightlights");
    public static final SkyShader SWARM = new SkyShader("swarm", "swarm");
    /** Ersatz fuer GL_POINTS mit Textur in der festen Pipeline. */
    public static final SkyShader POINT_TEX = new SkyShader("point_tex");
    /** Wetter mit Lichtkarte ({@code WeatherProviderCelestial}, GL_COMBINE-Ersatz). */
    public static final SkyShader WEATHER = new SkyShader("weather", "weather");

    private static final Map<String, SkyShader> BODY_SHADERS = new HashMap<>();

    /**
     * Shader eines Himmelskoerpers aus dessen Fragment-Pfad ({@code hbm:shaders/blackhole.frag} ->
     * {@code space_m:shaders/sky/blackhole.fsh}); null, wenn der Koerper keinen hat.
     */
    public static SkyShader forBody(ResourceLocation fragment) {
        if (fragment == null) return null;
        String path = fragment.getPath();
        int slash = path.lastIndexOf('/');
        String name = path.substring(slash + 1);
        int dot = name.indexOf('.');
        if (dot >= 0) name = name.substring(0, dot);
        return BODY_SHADERS.computeIfAbsent(name, SkyShader::new);
    }

    /** Ressourcen neu geladen: Programme beim naechsten Gebrauch neu bauen. */
    public static void reload() {
        for (SkyShader s : new SkyShader[] {FLESH, CRESCENT, ATMOSPHERE, ATMOSPHERE_EMISSIVE, LIGHTNING, NUKE, NIGHT_LIGHTS, SWARM, POINT_TEX, WEATHER}) {
            s.cleanup();
        }
        BODY_SHADERS.values().forEach(SkyShader::cleanup);
    }
}
