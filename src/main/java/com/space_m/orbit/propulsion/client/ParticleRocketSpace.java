package com.space_m.orbit.propulsion.client;

import java.util.Random;

import com.hbm_m.particle.nt.ParticleRocketFlameNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Raketenflammen von NTM Space mit Atmosphaerendruck ({@code ParticleRocketFlame.setAtmosphericPressure}) und den
 * Farbvarianten {@code ParticleRocketMUD/SCH/UP/Fusion/BF} ({@code effectNT} "missileContrail", "missileContrailMUD",
 * "missileContrailSCH", "missileContrailUP", "missileContrailf", "missileContrailbf"). HBMs {@code ParticleRocketFlameNT}
 * kennt nur die Grundflamme ohne Druck (Master-Stand); die Varianten leben deshalb im Addon.
 * Original-Klassen: ParticleRocketMUD, ParticleRocketSCH, ParticleRocketUP, ParticleRocketFusion, ParticleRocketBF
 * Original-Klasse: ParticleRocketMUD
 * Original-Klasse: ParticleRocketSCH
 * Original-Klasse: ParticleRocketUP
 * Original-Klasse: ParticleRocketFusion
 * Original-Klasse: ParticleRocketBF
 */
public class ParticleRocketSpace extends ParticleRocketFlameNT {

    public enum Variant {
        /** {@code ParticleRocketFlame}: Orange -> dunkel, voll hell. */
        FLAME(0xF000F0),
        /** {@code ParticleRocketMUD}: R/G {@code 1 + add}, B {@code add}, Helligkeit 20. */
        MUD(20),
        /** {@code ParticleRocketSCH}: R {@code light + add}, G/B {@code 1 + add}, Helligkeit 240. */
        SCH(0xF000F0),
        /** {@code ParticleRocketUP}: G {@code 1 + add}, Helligkeit 80. */
        UP(80),
        /** {@code ParticleRocketFusion}: R/G {@code 1 + add}, B {@code light + add}, Helligkeit 240. */
        FUSION(0xF000F0),
        /** {@code ParticleRocketBF}: G {@code 1 + add}, Helligkeit 240. */
        BF(0xF000F0);

        final int light;

        Variant(int light) {
            this.light = light;
        }

        /** Original {@code effectNT}-Typname. */
        public static Variant of(String type) {
            return switch (type) {
                case "missileContrailMUD" -> MUD;
                case "missileContrailSCH" -> SCH;
                case "missileContrailUP" -> UP;
                case "missileContrailf" -> FUSION;
                case "missileContrailbf" -> BF;
                default -> FLAME;
            };
        }
    }

    private final Variant variant;

    public ParticleRocketSpace(ClientLevel level, double x, double y, double z, Variant variant) {
        super(level, x, y, z);
        this.variant = variant;
    }

    /** Original {@code setAtmosphericPressure}: unter 0,08 atm Streuung, unter 0,05 / 0,2 kuerzeres Leben. */
    public ParticleRocketSpace setAtmosphericPressure(double pressure) {
        if (pressure < 0.08) {
            double factor = (0.08 - pressure) * 20;
            this.xd += (random.nextDouble() - 0.5) * factor;
            this.yd += (random.nextDouble() - 0.5) * factor;
            this.zd += (random.nextDouble() - 0.5) * factor;
        }

        if (pressure < 0.05) {
            this.lifetime /= 4;
        } else if (pressure < 0.2) {
            this.lifetime /= 2;
        }

        return this;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        if (variant == Variant.FLAME && age == 0) return;

        VertexConsumer c = buffer();
        Random urandom = new Random(this.seed);
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);

        for (int i = 0; i < 10; i++) {
            float add = urandom.nextFloat() * 0.3F;
            float light = 1 - Math.min(((float) age / (float) (lifetime * 0.25F)), 1);

            float r, g, b;
            switch (variant) {
                case MUD -> { r = 1 + add; g = 1 + add; b = add; }
                case SCH -> { r = 1 * light + add; g = 1 + add; b = 1 + add; }
                case UP, BF -> { r = add; g = 1 + add; b = add; }
                case FUSION -> { r = 1 + add; g = 1 + add; b = 1 * light + add; }
                default -> { r = 1 * light + add; g = 0.6F * light + add; b = add; }
            }

            float a = (float) Math.pow(1 - Math.min(((float) age / (float) lifetime), 1), 0.5);

            float spread = (float) Math.pow(((float) age / (float) lifetime) * 4F, 1.5) + 1F;
            spread *= this.particleScale;
            float scale = (urandom.nextFloat() * 0.5F + 0.1F + ((float) age / (float) lifetime) * 2F) * particleScale;
            float pX = (float) (p.x + (urandom.nextGaussian() - 1D) * 0.2F * spread);
            float pY = (float) (p.y + (urandom.nextGaussian() - 1D) * 0.5F * spread);
            float pZ = (float) (p.z + (urandom.nextGaussian() - 1D) * 0.2F * spread);
            quad(c, pX, pY, pZ, l, u, scale, Math.min(r, 1F), Math.min(g, 1F), Math.min(b, 1F), a * 0.75F, variant.light, 0, 0, 1, 1);
        }
    }
}
