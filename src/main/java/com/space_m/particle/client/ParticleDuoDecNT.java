package com.space_m.particle.client;

import com.hbm_m.particle.nt.ParticleQuadNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.space_m.SpaceMod;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1:1 NTM Space {@code ParticleDuoDec} ({@code effectNT "duoewe"}, Icon {@code particle/particlenote2}) und
 * {@code ParticleDuoDec2} ({@code "duodec"}, Icon {@code particle/particlenote1}): Notenpartikel, grau
 * ({@code 1 - rand*0.2}), Groesse 0,3 (das Original zeichnet mit {@code particleScale} selbst, nicht {@code 0.1*scale}),
 * Lebensdauer 50-99, Schwerkraft 0,2; in der Luft leichtes Seitwaerts-Zittern ({@code gaussian * 0.002}) und
 * Fallgeschwindigkeit hoechstens 0,025. Blockatlas-Schicht mit Helligkeit am Ort.
 * Original-Klasse: ParticleDuoDec
 * Original-Klasse: ParticleDuoDec2
 */
public class ParticleDuoDecNT extends ParticleQuadNT {

    /** @param duodec true = {@code ParticleDuoDec2} ("duodec", particlenote1), false = {@code ParticleDuoDec} ("duoewe", particlenote2). */
    public ParticleDuoDecNT(ClientLevel level, double x, double y, double z, boolean duodec) {
        super(level, x, y, z, SpaceMod.id(duodec ? "textures/particle/particlenote1.png" : "textures/particle/particlenote2.png"));
        this.rCol = this.gCol = this.bCol = 1F - level.random.nextFloat() * 0.2F;
        this.particleScale = 0.3F;
        this.lifetime = 50 + level.random.nextInt(50);
        this.gravity = 0.2F;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.onGround) {
            this.xd += random.nextGaussian() * 0.002D;
            this.zd += random.nextGaussian() * 0.002D;

            if (this.yd < -0.025D)
                this.yd = -0.025D;
        }
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);
        quad(c, (float) p.x, (float) p.y, (float) p.z, l, u, particleScale, rCol, gCol, bCol, alpha, light(), 0F, 0F, 1F, 1F);
    }
}
