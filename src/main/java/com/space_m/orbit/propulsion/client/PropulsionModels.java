package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;

import net.minecraft.resources.ResourceLocation;

/**
 * Modelle und Texturen des Stationsantriebs (Original {@code ResourceManager}: {@code htr3}, {@code htrf4},
 * {@code htrf4_neo} ({@code models/fusion/htrf4.obj}), {@code xenon_thruster}, {@code orbital_computer}
 * ({@code spaceship_computer.obj}); Texturen {@code lpw2_tex}, {@code htrf4_neo_tex}, {@code htrf4_exhaust_tex},
 * {@code fusion_plasma_sparkle_tex}, {@code xenon_thruster_tex}, {@code xenon_exhaust_tex}, {@code orbital_computer_tex}).
 */
public final class PropulsionModels {

    private PropulsionModels() {}

    public static final SimpleObjModel HTR3 = new SimpleObjModel(SpaceMod.id("models/machines/htr3.obj"));
    public static final SimpleObjModel HTRF4 = new SimpleObjModel(SpaceMod.id("models/machines/htrf4.obj"));
    public static final SimpleObjModel HTRF4_NEO = new SimpleObjModel(SpaceMod.id("models/machines/htrf4_neo.obj"));
    public static final SimpleObjModel XENON_THRUSTER = new SimpleObjModel(SpaceMod.id("models/machines/xenon_thruster.obj"));
    public static final SimpleObjModel ORBITAL_COMPUTER = new SimpleObjModel(SpaceMod.id("models/machines/spaceship_computer.obj"));

    public static final ResourceLocation LPW2_TEX = SpaceMod.id("textures/block/machine/lpw2.png");
    public static final ResourceLocation HTRF4_NEO_TEX = SpaceMod.id("textures/block/machine/htrf4_neo.png");
    public static final ResourceLocation HTRF4_EXHAUST_TEX = SpaceMod.id("textures/block/machine/htrf4_trail.png");
    public static final ResourceLocation PLASMA_SPARKLE_TEX = SpaceMod.id("textures/block/machine/plasma_sparkle.png");
    public static final ResourceLocation XENON_THRUSTER_TEX = SpaceMod.id("textures/block/machine/xenon_thruster.png");
    public static final ResourceLocation XENON_EXHAUST_TEX = SpaceMod.id("textures/block/machine/xenon_trail.png");
    public static final ResourceLocation ORBITAL_COMPUTER_TEX = SpaceMod.id("textures/block/machine/spaceship_computer.png");

    /** Triebwerke: {@code case 3: 270; case 5: 0; case 2: 90; case 4: 180}. */
    public static void rotateThruster(PoseStack ps, int meta) {
        switch (meta) {
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case 4 -> ps.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }
    }
}
