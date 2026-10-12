package com.space_m.machine.dyson.client;

import com.hbm_m.particle.nt.ParticleQuadNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1:1 NTM Space {@code ParticleGlow} ({@code effectNT "flare"}, {@code ParticleUtil.spawnFlare}): weisses Leuchten mit
 * Textur {@code particle/yelflare}, Alpha 0.8, volle Helligkeit, kollisionsfrei, Lebensdauer 50-99 Ticks; die Groesse
 * waechst bis zur Lebensmitte auf {@code scale} und faellt wieder ({@code peak * sin(t*pi/2)}). Bewegung wie
 * {@code EntityFX.onUpdate} (Reibung 0.98, keine Schwerkraft); Quadgroesse wie {@code EntityFX}: {@code 0.1 * scale}.
 * Original-Klasse: ParticleGlow
 */
public class ParticleGlowNT extends ParticleQuadNT {

    private final float peakScale;

    public ParticleGlowNT(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, float scale) {
        super(level, x, y, z, com.space_m.SpaceMod.id("textures/particle/yelflare.png")); // R9-B4: Original-Icon particle/yelflare (Blockatlas), nicht HBMs flare
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.particleScale = peakScale = scale;
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.age = 1;
        this.lifetime = 50 + level.random.nextInt(50);
        this.alpha = 0.8F;
        this.noClip = true;
        this.fullBright = true;
        this.gravity = 0F;
    }

    @Override
    public void tick() {
        super.tick();

        // Increases in size to maximum then returns to minimum
        float t = (float) age / (float) lifetime;
        t = Math.abs(0.5F - (t - 0.5F)) * 2;

        particleScale = peakScale * (float) Math.sin(t * Math.PI / 2);
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        VertexConsumer c = buffer();
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);
        float s = 0.1F * particleScale;
        quad(c, (float) p.x, (float) p.y, (float) p.z, l, u, s, rCol, gCol, bCol, alpha, light(), 0F, 0F, 1F, 1F);
    }
}
