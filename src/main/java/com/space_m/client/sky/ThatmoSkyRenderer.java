package com.space_m.client.sky;

import java.util.ArrayList;
import java.util.Random;

import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CelestialBodyTrait.CBT_BATTLEFIELD;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code com.hbm.dim.thatmo.SkyProviderThatmo}: Schlachtfeld-Himmel von Thatmo (Schildeinschlaege,
 * Railgun-Satellit, Strahlen, Meteorschauer). Der Animationszustand lag im Original statisch in
 * {@code WorldProviderThatmo} (Client-Teil von {@code updateWeather}) und steht hier in {@link State}.
 * Original-Klasse: SkyProviderThatmo
 */
public class ThatmoSkyRenderer extends CelestialSkyRenderer {
    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/particle/shockwave.png");
    private static final ResourceLocation ThatmoShield = SpaceMod.id("textures/particle/cens.png");
    private static final ResourceLocation flash = SpaceMod.id("textures/misc/space/flare.png");
    private static final ResourceLocation flare = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/particle/flare.png");


    //someone beat me to a pulp for ever conceiving this awful noxious class my god take me out of my misery pls....

    @Override
    public void renderSpecialEffects(float partialTicks, ClientLevel world, Minecraft mc) {
        float alpha = (State.flashd <= 0) ? 0.0F : 1.0F - Math.min(1.0F, State.flashd / 100);

        SkyGL.push();
        float var14 = State.flashd * 2 + partialTicks;

        SkyGL.enableBlend();
        SkyGL.rotate(180.0, 0.0, 5.0, 0.0);
        SkyGL.rotate(90.0, -12.0, 7.3F, -4.0);

        SkyGL.bind(texture);

        SkyGL.color(1, 1, 1, alpha);
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(-var14, 100.0D, -var14, 0.0D, 0.0D);
        SkyTess.addVertexWithUV(var14, 100.0D, -var14, 1.0D, 0.0D);
        SkyTess.addVertexWithUV(var14, 100.0D, var14, 1.0D, 1.0D);
        SkyTess.addVertexWithUV(-var14, 100.0D, var14, 0.0D, 1.0D);
        SkyTess.draw();

        SkyGL.pop();

        SkyGL.push();
        SkyGL.rotate(180.0, 0.0, 5.0, 0.0);
        SkyGL.rotate(90.0, -12.0, 7.3F, -4.0);
        var14 = var14 * 0.5F;
        SkyGL.bind(flash);

        SkyGL.color(1, 1, 1, alpha);
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(-var14, 100.0D, -var14, 0.0D, 0.0D);
        SkyTess.addVertexWithUV(var14, 100.0D, -var14, 1.0D, 0.0D);
        SkyTess.addVertexWithUV(var14, 100.0D, var14, 1.0D, 1.0D);
        SkyTess.addVertexWithUV(-var14, 100.0D, var14, 0.0D, 1.0D);
        SkyTess.draw();
        SkyGL.pop();
        Random random = new Random(42);
        float alt = State.altitude + partialTicks;
        float rnd = State.randPos;
        float beamscale = State.scale;
        float shieldscale = State.shield;
        float waveinterp = State.nmass;
        float shieldinterp = State.shielde;
        float shielalpha = State.csyw + partialTicks;

        SkyGL.enableTexture();

        float alphad = 1.0F - Math.min(1.0F, waveinterp / 100);
        float alpd = 1.0F - Math.min(1.0F, shielalpha / 100);

        SkyGL.push();

        SkyGL.translate(21.5, 33, -28);
        SkyGL.scale(0 + shieldscale, 0 + shieldscale, 0 + shieldscale);
        SkyGL.rotate(90.0, -10.0, -1.0, 50.0);
        SkyGL.rotate(20.0, -0.0, -1.0, 1.0);

        SkyGL.color(1, 1, 1, alphad);

        SkyGL.bind(texture);
        SkyObjModel.PLANE.renderAll();

        SkyGL.pop();
        SkyGL.push();


        SkyGL.translate(21.5, 33, -28);

        SkyGL.scale(0 + shieldinterp, 0 + shieldinterp, 0 + shieldinterp);
        SkyGL.rotate(90.0, -10.0, -1.0, 50.0);
        SkyGL.rotate(20.0, -0.0, -1.0, 1.0);
        SkyGL.color(1, 1, 1, alpd);

        SkyGL.bind(ThatmoShield);
        SkyObjModel.PLANE.renderAll();

        SkyGL.pop();

        LocalPlayer player = mc.player;
        Vec3 eye = camera.getPosition();

        for (State.Meteor meteor : State.meteors) {
            renderMeteor(meteor, eye, mc, 41, false, partialTicks);
        }
        for (State.Meteor fragment : State.fragments) {
            renderMeteor(fragment, eye, mc, 82, false, partialTicks);
        }
        for (State.Meteor smoke : State.smoke) {
            renderMeteor(smoke, eye, mc, 82, true, partialTicks);
        }

        SkyGL.push();

        //god awful and shouldnt even be here. ill figure out something later im so sorry
        //another thing, this is a thatmo exclusive, when the war update rolls out there will be a better way to render a beam from a sattelite
        //help me i beg
        //:(
        SkyGL.translate(16.5, 28.5, 100);
        SkyGL.scale(10, 10, 10);
        SkyGL.rotate(-63.5, 0.0, 0.0, 1.0);
        SkyBeamPronter.prontBeam(new Vec3(0, State.flashd * 0.5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, 0, 1, 0F, 6, (float) 0.2 * 0.2F, alpha);
        SkyBeamPronter.prontBeam(new Vec3(0, State.flashd * 0.5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, 0, 1, 0F, 6, (float) 0.2 * 0.6F, alpha);
        SkyBeamPronter.prontBeam(new Vec3(0, State.flashd * 0.5, 0), SkyBeamPronter.EnumWaveType.RANDOM, SkyBeamPronter.EnumBeamType.SOLID, 0x202060, 0x202060, (int) (world.getGameTime() / 5) % 1000, 25, 0.2F, 6, (float) 0.2 * 0.1F, alpha);

        SkyGL.rotate(27, 00, 80, 0);

        SkyGL.color(1, 1, 1, alpha);

        SkyGL.pop();

        //genuinley horrific peice of shit i actually vomit looking at this


        SkyGL.enableTexture();
        SkyGL.depthMask(true);
        SkyGL.push();
        SkyGL.translate(145, 75.5, -15);
        SkyGL.rotate(90.0, -10.0, -1.0, 50.0);
        SkyGL.rotate(20.0, -0.0, -1.0, 1.0);

        SkyGL.scale(6, 6, 6);

        SkyGL.color(1, 1, 1, 1);
        SkyBeamPronter.prontBeam(new Vec3(0, beamscale, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0xFF9000, 0xFF9000, 0, 2, 0F, 2, (float) alphad * 0.1F, 0.5F);



        SkyGL.pop();
        SkyGL.push();
        SkyGL.rotate(25.0 * rnd, -12.0, 5.0 + rnd, 2.0 + rnd);
        SkyGL.rotate(15.0 * rnd, -11.0, 20.0 + rnd, 1.0 + rnd);

        SkyGL.translate(50 - rnd, alt - rnd * rnd, -10 * rnd);
        for (int i = 0; i < 17; i++) {
            SkyGL.translate(-rnd, 0 - random.nextInt(20), rnd);
            SkyBeamPronter.prontBeam(new Vec3(0, 5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0xFF9000, 0xFF9000, 0, 1, 0F, 6, (float) 0.2 * 0.2F, 0.5F);
        }

        SkyGL.pop();

        SkyGL.push();
        SkyGL.rotate(45.0 * rnd, -12.0, 5.0, 0.0);

        SkyGL.translate(-40 + rnd, alt - 50 * rnd, -90 * rnd);

        for (int i = 0; i < 17; i++) {
            SkyGL.translate(-rnd, 0 - random.nextInt(20), -1 - rnd);
            SkyBeamPronter.prontBeam(new Vec3(0, 5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0xFF9000, 0xFF9000, 0, 1, 0F, 6, (float) 0.2 * 0.2F, 0.5F);
        }
        SkyGL.pop();

        SkyGL.push();
        SkyGL.rotate(-90.0 * rnd, -12.0, 5.0, -20 + -45);

        SkyGL.translate(-10 * rnd, rnd + alt - 110, -80 + rnd);

        for (int i = 0; i < 17; i++) {
            SkyGL.translate(-rnd, 0 - random.nextInt(20), -1 - rnd);
            SkyBeamPronter.prontBeam(new Vec3(0, 5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0xFF9000, 0xFF9000, 0, 1, 0F, 6, (float) 0.2 * 0.2F, 0.5F);
        }
        SkyGL.pop();

        SkyGL.push();
        SkyGL.rotate(-90.0 * 0.5961033, -11.0, 5.0, -20 + -45);
        SkyGL.translate(80 * rnd, rnd + alt - 110, 55 + rnd);

        for (int i = 0; i < 17; i++) {
            SkyGL.translate(-rnd, 0 - random.nextInt(20), -1 - rnd);
            SkyBeamPronter.prontBeam(new Vec3(0, 5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0xFF9000, 0xFF9000, 0, 1, 0F, 6, (float) 0.2 * 0.2F, 0.5F);
        }
        SkyGL.pop();


        SkyGL.push();
        SkyGL.rotate(-25.0 - rnd, -16.0, 5.0, 0.0);
        SkyGL.translate(-60 - rnd, alt - 150 * rnd, 20 * rnd);

        for (int i = 0; i < 17; i++) {
            SkyGL.translate(-rnd, 0 - random.nextInt(15), rnd);
            SkyBeamPronter.prontBeam(new Vec3(0, 5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0xFF9000, 0xFF9000, 0, 1, 0F, 6, (float) 0.2 * 0.2F, 0.5F);
        }
        SkyGL.pop();

        SkyGL.push();
        SkyGL.rotate(-65.0 - rnd, -12.0, 5.0, 0.0);

        SkyGL.translate(50 + rnd, alt - 150 * rnd, -80 * rnd);

        for (int i = 0; i < 17; i++) {
            SkyGL.translate(rnd, 0 - random.nextInt(15), -rnd);
            SkyBeamPronter.prontBeam(new Vec3(0, 5, 0), SkyBeamPronter.EnumWaveType.SPIRAL, SkyBeamPronter.EnumBeamType.SOLID, 0xFF9000, 0xFF9000, 0, 1, 0F, 6, (float) 0.2 * 0.2F, 0.5F);
        }
        SkyGL.pop();
        SkyGL.enableCull();


        // the reason we don't write big messy functions is because it makes it
        // impossible to understand the current state of the GL state machine
        SkyGL.depthMask(false);
    }

    /** Die drei identischen Meteor-/Fragment-/Rauchschleifen des Originals. */
    private void renderMeteor(State.Meteor meteor, Vec3 eye, Minecraft mc, double divisor, boolean smoke, float partialTicks) {
        SkyGL.push();
        SkyGL.enableTexture();
        Vec3 vec = new Vec3(meteor.posX - eye.x, meteor.posY - eye.y, meteor.posZ - eye.z);
        Vec3 vec2 = vec;
        double l = Math.min(mc.options.getEffectiveRenderDistance() * 16, vec.length());
        vec = vec.normalize();
        Vec3 vecd = new Vec3(vec.x * l, vec.y * l, vec.z * l);
        SkyGL.translate(vecd.x, vecd.y, vecd.z);
        double descent = 2017d - meteor.posY;
        double quadratic = (-1 * Math.pow(descent, 2) + (1517 * descent)) / divisor;
        float scalar = (float) (quadratic / vec2.length());
        if (smoke) {
            SkyGL.color(1, 0, 0, 1);
            SkyGL.scale(scalar, scalar, scalar);
            renderSmoke(particleBase, meteor.age);
        } else {
            SkyGL.scale(scalar, scalar, scalar);
            renderGlow(flare, 1, 1, 1, partialTicks);
        }
        SkyGL.disableTexture();
        SkyGL.pop();
    }

    public void renderSmoke(ResourceLocation loc1, long age) {
        SkyGL.push();
        SkyGL.enableBlend();
        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;
        float dark = 1f - Math.min(((float) (age) / (float) (100f * 0.35F)), 1f);
        billboard();
        SkyGL.color(0.6 * dark + 0.0, 0.6 * dark + 0.0, 1 * dark + 0.0, 1);
        SkyGL.bind(loc1);
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(0.0F - f5, 0.0F - f6, 0.0D, 1, 0);
        SkyTess.addVertexWithUV(f4 - f5, 0.0F - f6, 0.0D, 0, 0);
        SkyTess.addVertexWithUV(f4 - f5, f4 - f6, 0.0D, 0, 1);
        SkyTess.addVertexWithUV(0.0F - f5, f4 - f6, 0.0D, 1, 1);
        SkyTess.draw();
        SkyGL.disableBlend();
        SkyGL.pop();
    }

    public void renderGlow(ResourceLocation loc1, double x, double y, double z, float partialTicks) {
        SkyGL.push();
        SkyGL.enableBlend();
        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;
        billboard();
        double near = 0.51d * (Math.min(40000f, Math.max(0d, y - 35000d)) / 40000d);
        ClientLevel level = Minecraft.getInstance().level;
        float rain = level != null ? level.getRainLevel(partialTicks) : 0F;
        double entry = near * (1d - rain) + (1d - (Math.min(200d, Math.max(0d, x - 2017d)) / 200f));
        SkyGL.color(entry, entry, entry, entry);
        SkyGL.bind(loc1);
        SkyTess.startDrawingQuads();
        SkyTess.addVertexWithUV(0.0F - f5, 0.0F - f6, 0.0D, 1, 0);
        SkyTess.addVertexWithUV(f4 - f5, 0.0F - f6, 0.0D, 0, 0);
        SkyTess.addVertexWithUV(f4 - f5, f4 - f6, 0.0D, 0, 1);
        SkyTess.addVertexWithUV(0.0F - f5, f4 - f6, 0.0D, 1, 1);
        SkyTess.draw();
        SkyGL.disableBlend();
        SkyGL.pop();
    }

    @Override
    public void render3DModel(float partialTicks, ClientLevel world, Minecraft mc) {
        CelestialBody body = CelestialBody.getBody(world);
        CBT_BATTLEFIELD wared = body.getTrait(CBT_BATTLEFIELD.class);
        if (wared != null) {
            SkyGL.push();
            SkyGL.enableDepthTest();
            SkyGL.disableBlend();
            SkyGL.translate(-35, 4.5, 100);
            SkyGL.scale(10, 10, 10);
            SkyGL.rotate(180.0, 0.0, 5.0, 0.0);
            SkyGL.rotate(90.0, -12.0, 5.0, 0.0);

            SkyGL.color(0, 0, 0, 1);

            SkyGL.bind(SkyObjModel.SAT_RAIL_TEX);
            SkyObjModel.SAT_RAIL.renderAll();
            SkyGL.clearDepth();
            SkyGL.enableBlend();
            SkyGL.pop();

        }
    }

    /**
     * Statischer Schlachtfeld-Zustand und Client-Tick aus {@code WorldProviderThatmo} (Felder {@code chargetime},
     * {@code flashd}, ... sowie {@code updateWeather} auf dem Client). Der Server-Teil (Bomber) gehoert R8.
     * Himmel-Teil von WorldProviderThatmo.
     */
    public static final class State {
        private State() {}

        public static int chargetime;
        public static float flashd;
        public static float altitude;
        public static float randPos;
        public static float scale;
        public static float shield;
        public static float cooldown;
        public static float nmass;
        public static float shielde;
        public static float csyw;
        public static ArrayList<Meteor> meteors = new ArrayList<>();
        public static ArrayList<Meteor> fragments = new ArrayList<>();
        public static ArrayList<Meteor> smoke = new ArrayList<>();

        /** Additiver Blitz fuer Himmel, Nebel und Sonne ({@code xalpha + alpha}). */
        public static float flashAlpha() {
            float xalpha = (csyw <= 0) ? 0.0F : 0.5F - Math.min(0.5F, csyw / 100);
            float alpha = (flashd <= 0) ? 0.0F : 1.0F - Math.min(1.0F, flashd / 100);
            return alpha + xalpha;
        }

        /** Client-Teil von {@code WorldProviderThatmo.updateWeather}. */
        public static void clientTick(ClientLevel worldObj) {
            CBT_BATTLEFIELD war = CelestialBody.getTrait(worldObj, CBT_BATTLEFIELD.class);

            // i am violating the genercizing philosphy mellow told me about, this shit sucks
            // if other planets have the battlefield trait, what could be done....?

            if (war == null) return;

            Random rand = new Random();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            if (chargetime <= 0 || chargetime <= 1000) {
                chargetime += 1;
                flashd = 0;
            } else if (chargetime >= 100) {
                flashd += 0.3f;
                flashd = Math.min(100.0f, flashd + 0.3f * (100.0f - flashd) * 0.15f);

                if (flashd <= 5) {
                    SkySounds.playFireflash(player);
                }

                if (flashd >= 100) {
                    chargetime = 0;
                }
            }

            if (chargetime == 285) {
                SkySounds.playImpact(player);
            }

            if (chargetime >= 300 && chargetime <= 430) {
                if (scale <= 0 || scale <= 20) {
                    scale += 1.5;
                }

                if (scale >= 20) {
                    scale = 20;
                    shield += 0.4f;
                    shield = Math.min(25.0f, shield + 0.5f * (25.0f - shield) * 0.15f);
                    nmass = Math.min(180.0f, nmass + 0.2f * (180.0f - nmass) * 0.15f);
                    shielde = Math.min(15.0f, shielde + 0.6f * (15.0f - shielde) * 0.10f);
                    csyw += 0.2f;
                    csyw = Math.min(100.0f, csyw + 0.2f * (100.0f - csyw) * 0.15f);
                }

                if (shield > 0) {
                    if (cooldown <= 0 || cooldown <= 60) {
                        cooldown += 0.5;
                    }

                    if (cooldown >= 60) {
                        reset();
                    }
                }
            } else {
                reset();
            }

            if (altitude <= 400) {
                altitude += 5;
            }

            if (altitude >= 400) {
                altitude = 0;
                randPos = worldObj.random.nextFloat();
            }

            for (Meteor meteor : new ArrayList<>(meteors)) {
                meteor.update();
            }

            for (Meteor fragment : new ArrayList<>(fragments)) {
                fragment.update();
            }

            for (Meteor smoke : new ArrayList<>(State.smoke)) {
                smoke.update();
            }

            if (rand.nextInt(1) == 0) {
                Meteor meteor = new Meteor((player.getX() + rand.nextInt(16000)) - 8000, 2017, (player.getZ() + rand.nextInt(16000)) - 8000);
                meteors.add(meteor);
            }

            meteors.removeIf(x -> x.isDead);
            fragments.removeIf(xx -> xx.isDead);
            smoke.removeIf(xxx -> xxx.isDead);
        }

        private static void reset() {
            cooldown = shield = scale = nmass = shielde = csyw = 0;
        }

        /** Original {@code WorldProviderThatmo.Meteor}. */
        public static class Meteor {

            public double posX;
            public double posY;
            public double posZ;
            public double prevPosX;
            public double prevPosY;
            public double prevPosZ;
            public double motionX;
            public double motionY;
            public double motionZ;
            public boolean isDead = false;
            public long age;
            public CelestialSkyProvider.MeteorType type;

            public Meteor(double posX, double posY, double posZ) {
                this(posX, posY, posZ, CelestialSkyProvider.MeteorType.STANDARD, -31.2, -20.8, 20);
            }

            public Meteor(double posX, double posY, double posZ, CelestialSkyProvider.MeteorType type, double motionX, double motionY, double motionZ) {
                this.posX = posX;
                this.posY = posY;
                this.posZ = posZ;
                this.type = type;
                this.motionX = motionX;
                this.motionY = motionY;
                this.motionZ = motionZ;
            }

            private void update() {
                Random rand = new Random();
                if (this.type != CelestialSkyProvider.MeteorType.SMOKE && this.type != CelestialSkyProvider.MeteorType.FRAGMENT) {
                    Meteor meteor = new Meteor((this.posX + rand.nextInt(16)) - 8, (this.posY + rand.nextInt(16)), (this.posZ + rand.nextInt(16)) - 8, CelestialSkyProvider.MeteorType.SMOKE, 0, 0, 0);
                    smoke.add(meteor);
                }

                if (this.posY <= 500 && this.type != CelestialSkyProvider.MeteorType.SMOKE) {
                    this.isDead = true;
                }

                if (this.type == CelestialSkyProvider.MeteorType.SMOKE) {
                    this.age++;
                    if (this.age >= 60)
                        this.isDead = true;
                }

                this.prevPosX = this.posX;
                this.prevPosY = this.posY;
                this.prevPosZ = this.posZ;
                this.posX += this.motionX;
                this.posY += this.motionY;
                this.posZ += this.motionZ;
            }
        }
    }

}
