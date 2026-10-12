package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.orbit.propulsion.TileEntityMachineHTRNeo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1:1 NTM Space {@code RenderHTRNeo}: Kern-Mitte, 2 tiefer, Ausrichtung wie HTR3; {@code Base} + {@code Engine}, Rotoren
 * um die Z-Achse in 2,5 Hoehe ({@code prevRotor..rotor}). Abgasstrahl in der Plasmafarbe ({@code plasmaR/G/B}, Alpha
 * {@code thrustAmount}) ab z = 12 um {@code (1,2 - r^2 * 0,1) * thrustAmount} gestreckt; darueber additiv das
 * Plasma-Glitzern (Farbe x2, Alpha x0,75) mit Texturmatrix {@code rotate(-90, Y)} + {@code translate(0, t/250 ms % 1)}
 * - das setzt u auf 0 und laesst v laufen ("concentric rings"), hier ueber {@code UvShiftConsumer(0, spin, 0, 1)}.
 * Original-Klasse: RenderHTRNeo
 */
public class RenderHTRNeo implements HbmBerBounds<TileEntityMachineHTRNeo> {

    public RenderHTRNeo(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TileEntityMachineHTRNeo rocket, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, -2.0D, 0.5D);

        PropulsionModels.rotateThruster(ps, ObjBerHelper.meta(rocket));

        float rot = rocket.prevRotor + (rocket.rotor - rocket.prevRotor) * interp;

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(PropulsionModels.HTRF4_NEO_TEX));
        PropulsionModels.HTRF4_NEO.renderPart("Base", ps, vc, light);
        PropulsionModels.HTRF4_NEO.renderPart("Engine", ps, vc, light);

        ps.pushPose();
        {
            ps.translate(0, 2.5F, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(rot));
            ps.translate(0, -2.5F, 0);
            PropulsionModels.HTRF4_NEO.renderPart("Rotor", ps, vc, light);
            PropulsionModels.HTRF4_NEO.renderPart("Rotor2", ps, vc, light);
        }
        ps.popPose();

        float trailStretch = rocket.getLevel() != null ? rocket.getLevel().random.nextFloat() : 0F;
        trailStretch = 1.2F - (trailStretch * trailStretch * 0.1F);
        trailStretch *= rocket.thrustAmount;

        if (trailStretch > 0) {
            ps.translate(0, 0, 12);
            ps.scale(1, 1, trailStretch);
            ps.translate(0, 0, -12);

            PropulsionModels.HTRF4_NEO.renderPartEntity("Exhaust", ps,
                    buf.getBuffer(RenderType.entityTranslucentEmissive(PropulsionModels.HTRF4_EXHAUST_TEX)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, rocket.plasmaR, rocket.plasmaG, rocket.plasmaB, rocket.thrustAmount);

            long time = System.currentTimeMillis();
            double sparkleSpin = time / 250D % 1D;

            // GL_SRC_ALPHA, GL_ONE: additiv, Farbe bereits mit Alpha gewichtet
            float a = rocket.thrustAmount * 0.75F;
            VertexConsumer sparkle = new UvShiftConsumer(buf.getBuffer(RenderType.eyes(PropulsionModels.PLASMA_SPARKLE_TEX)), 0F, (float) sparkleSpin, 0F, 1F);
            PropulsionModels.HTRF4_NEO.renderPartEntity("Exhaust", ps, sparkle, 0xF000F0, OverlayTexture.NO_OVERLAY,
                    Math.min(rocket.plasmaR * 2 * a, 1F), Math.min(rocket.plasmaG * 2 * a, 1F), Math.min(rocket.plasmaB * 2 * a, 1F), 1F);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileEntityMachineHTRNeo be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
