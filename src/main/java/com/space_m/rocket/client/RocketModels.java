package com.space_m.rocket.client;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.client.render.SimpleObjModel;

import net.minecraft.resources.ResourceLocation;

/** Modell-/Textur-Cache der Raketenteile ("ns:pfad" aus {@link com.space_m.rocket.RocketPart}). */
public final class RocketModels {

    private RocketModels() {}

    private static final Map<String, SimpleObjModel> MODELS = new HashMap<>();
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    public static SimpleObjModel model(String loc) {
        return MODELS.computeIfAbsent(loc, l -> new SimpleObjModel(ResourceLocation.tryParse(l)));
    }

    public static ResourceLocation texture(String loc) {
        return TEXTURES.computeIfAbsent(loc, ResourceLocation::tryParse);
    }
}
