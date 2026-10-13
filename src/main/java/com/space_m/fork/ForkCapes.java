package com.space_m.fork;

import java.util.UUID;

import javax.annotation.Nullable;

import com.space_m.SpaceMod;

import net.minecraft.resources.ResourceLocation;

/**
 * E11: Umhaenge der Fork-Entwickler (Space {@code RenderAccessoryUtility.getCloakFromPlayer}, Ergaenzung des Forks):
 * {@code ShadyUtil.DUODEC_} -> {@code CapeGwen}, {@code ShadyUtil.MellowRPG8} -> {@code CapeMellow}. Wie im Original
 * ersetzt der Umhang einen Vanilla-Umhang ({@code func_152121_a(Type.CAPE, cloak)}); angewendet ueber
 * {@code com.space_m.mixin.ForkCapeMixin}. Die uebrigen Umhaenge der Liste sind Master-Stand (nicht Teil des Forks).
 */
public final class ForkCapes {

    private ForkCapes() {}

    public static final UUID DUODEC_ = UUID.fromString("85d54b8c-1862-4c86-b351-5ef1b998aa32");
    public static final UUID MELLOW_RPG8 = UUID.fromString("99b9efa5-8c38-4b22-86c2-baf97789d2fb");

    public static final ResourceLocation GWEN = SpaceMod.id("textures/models/capes/capegwen.png");
    public static final ResourceLocation MELLOW = SpaceMod.id("textures/models/capes/capemellow.png");

    @Nullable
    public static ResourceLocation getCloak(UUID uuid) {
        if (DUODEC_.equals(uuid)) return GWEN;
        if (MELLOW_RPG8.equals(uuid)) return MELLOW;
        return null;
    }
}
