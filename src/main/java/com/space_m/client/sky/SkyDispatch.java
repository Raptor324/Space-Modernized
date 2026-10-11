package com.space_m.client.sky;

import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3f;

import com.hbm_m.block.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.space_m.celestial.trait.CBT_Invasion;
import com.space_m.dim.SpaceDimensions;
import com.space_m.main.SpaceRegistry;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gemeinsame Client-Logik des Himmels: welche Welten der Space-Himmel zeichnet (Oberwelt = Kerbin und alle
 * Space-Dimensionen; in 1.7.10 ersetzt {@code WorldProviderEarth} den Oberwelt-Provider), Modelview-Umschaltung,
 * Lichtkarte, Client-Tick (Meteore, Eve/Thatmo, Leitstern) und Fehlerprotokoll.
 */
public final class SkyDispatch {
    private SkyDispatch() {}

    /** true = diese Welt hat den Space-Himmel (Kerbin oder Space-Dimension). */
    public static boolean active(Level level) {
        if (!(level instanceof ClientLevel cl)) return false;
        if (!(cl.effects() instanceof CelestialEffects)) return false;
        return level.dimension() == Level.OVERWORLD || SpaceDimensions.isSpaceDimension(level.dimension());
    }

    /** Provider der Welt oder null. */
    public static CelestialSkyProvider provider(Level level) {
        if (!active(level)) return null;
        return ((CelestialEffects) ((ClientLevel) level).effects()).provider;
    }

    // ── Modelview (Himmel/Wolken: Vertices werden mit der Kameramatrix auf der CPU transformiert) ──

    public static void pushIdentityModelView() {
        //? if < 1.21.1 {
        com.mojang.blaze3d.vertex.PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();
        //?} else {
        /*org.joml.Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        *///?}
        RenderSystem.applyModelViewMatrix();
    }

    public static void popModelView() {
        //? if < 1.21.1 {
        RenderSystem.getModelViewStack().popPose();
        //?} else {
        /*RenderSystem.getModelViewStack().popMatrix();
        *///?}
        RenderSystem.applyModelViewMatrix();
    }

    /** Vanilla {@code renderSky}: kein Himmel in Pulverschnee/Lava oder mit Blindheit/Dunkelheit. */
    public static boolean skyBlocked(Camera camera) {
        FogType fogtype = camera.getFluidInCamera();
        if (fogtype == FogType.POWDER_SNOW || fogtype == FogType.LAVA) return true;
        Entity entity = camera.getEntity();
        return entity instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
    }

    // ── Lichtkarte ──

    /**
     * Vanilla-Zelle ({@code LightTexture.updateLightTexture}) mit der Sonnenhelligkeit des Providers
     * ({@code getSunBrightness} statt {@code getSkyDarken}) neu rechnen, dann {@code updateLightmap} des Koerpers.
     */
    static void adjustLightmap(CelestialEffects effects, ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                               int pixelX, int pixelY, Vector3f colors) {
        Minecraft mc = Minecraft.getInstance();
        CelestialSkyProvider provider = effects.provider;
        boolean darkened = mc.gameRenderer.getDarkenWorldAmount(partialTicks) > 0.0F;
        float sky = LightTexture.getBrightness(level.dimensionType(), pixelY);
        // erste Zelle der Aktualisierung (Vanilla-Schleife: Himmel aussen, Block innen)
        if (pixelX == 0 && pixelY == 0) provider.lightmapSunBrightness = provider.getSunBrightness(level, 1.0F);

        if (!darkened && level.getSkyFlashTime() <= 0) {
            float f = Math.max(provider.lightmapSunBrightness, 0.0F);
            float f1 = f * 0.95F + 0.05F;
            Vector3f skyTint = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
            float f8 = sky * f1;
            float f9 = LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
            float f10 = f9 * ((f9 * 0.6F + 0.4F) * 0.6F + 0.4F);
            float f11 = f9 * (f9 * f9 * 0.6F + 0.4F);
            colors.set(f9, f10, f11);
            colors.add(new Vector3f(skyTint).mul(f8));
            colors.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
        }

        float[] rgb = {colors.x(), colors.y(), colors.z()};
        if (provider.updateLightmap(level, sky, rgb)) {
            colors.set(rgb[0], rgb[1], rgb[2]);
        }
    }

    // ── Client-Tick ──

    private static boolean renderLodeStar;
    private static long lastStarCheck;

    /** Original {@code ModEventHandlerClient.renderLodeStar}. */
    public static boolean renderLodeStar() {
        return renderLodeStar;
    }

    /** Original {@code updateWeather} (Client) des Providers + {@code onClientTickLast} (Leitstern). */
    public static void clientTick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) return;

        CelestialSkyProvider provider = provider(level);
        if (provider != null) {
            try {
                provider.clientTick(level);
            } catch (RuntimeException e) {
                logOnce("tick", e);
            }
        }

        long millis = System.currentTimeMillis();
        if (lastStarCheck + 200 < millis) {
            renderLodeStar = false;
            lastStarCheck = millis;

            LocalPlayer player = mc.player;
            if (player != null) {
                Vec3 pos = player.position();
                Vec3 heading = new Vec3(0, 0, -1D).xRot((float) Math.toRadians(-15)).scale(25);
                Vec3 nextPos = pos.add(heading);
                BlockHitResult mop = level.clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (mop != null && mop.getType() == HitResult.Type.BLOCK && level.getBlockState(mop.getBlockPos()).is(ModBlocks.GLASS_POLARIZED.get())) {
                    renderLodeStar = true;
                }
            }
        }
    }

    /** Meteor-Hook fuer {@code CBT_Invasion} (vorhandenen Hook behalten, nur {@code addMeteor} ergaenzen). */
    public static void installInvasionHook() {
        CBT_Invasion.ClientHooks previous = CBT_Invasion.clientHooks;
        CBT_Invasion.clientHooks = new CBT_Invasion.ClientHooks() {
            @Override
            public void warn(CBT_Invasion invasion) {
                previous.warn(invasion);
            }

            @Override
            public void addMeteor() {
                CelestialSkyProvider.Meteor.addMeteor();
            }
        };
    }

    // ── Fehler ──

    private static final Set<String> LOGGED = new HashSet<>();

    static void logOnce(String what, Throwable t) {
        if (LOGGED.add(what)) SpaceRegistry.LOGGER.error("[space_m] Sky {} rendering failed", what, t);
    }
}
