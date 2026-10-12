package com.space_m.rocket.client;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * Eigene Darstellung eines Raketenteils (Original {@code RenderRocketPart}: "Handles custom rendering, animations, all
 * that fun stuff - Early version, only handles pods at the moment"). R5-C meldet den Drop-Pod an
 * ({@link RocketPronter#registerPartRenderer}).
 * Original-Klasse: RenderRocketPart
 */
public interface RocketPartRenderer {

    void render(PoseStack ps, MultiBufferSource buf, int light, @Nullable Entity rocket, float interp);
}
