package com.space_m.dim.duna.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 1:1 {@code com.hbm.particle.ParticleDust} (Staubsturm der Duna): Grundtextur {@code particle_base}
 * ({@code hbm_m:particle_base}), Farbe (0.4, 0.2, 0.1), Alpha 0.5, Groesse 4 (das Original zeichnet den Quader mit
 * {@code particleScale} direkt als halbe Kantenlaenge), Lebensdauer 50-99 Ticks, fliegt durch Bloecke
 * ({@code noClip}), Bewegung exakt wie uebergeben (Original setzt {@code motionX/Y/Z} nach dem Zufallsanteil des
 * {@code EntityFX}-Konstruktors zurueck). Reibung 0.98 und keine Schwerkraft wie {@code EntityFX.onUpdate}.
 */
// Original-Klasse: ParticleDust
public class DunaDustParticle extends TextureSheetParticle {

    /** {@code ParticleUtil.spawnDustFlame}: {@code scale = 4F}. */
    public static final float SCALE = 4F;

    protected DunaDustParticle(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, float scale) {
        super(level, x, y, z, mX, mY, mZ);
        this.rCol = 0.4F;
        this.gCol = 0.2F;
        this.bCol = 0.1F;
        this.quadSize = scale;
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.age = 1;
        this.lifetime = 50 + level.random.nextInt(50);
        this.alpha = 0.5F;
        this.hasPhysics = false;
        this.gravity = 0;
        this.friction = 0.98F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double mX, double mY, double mZ) {
            DunaDustParticle p = new DunaDustParticle(level, x, y, z, mX, mY, mZ, SCALE);
            p.pickSprite(sprites);
            return p;
        }
    }
}
