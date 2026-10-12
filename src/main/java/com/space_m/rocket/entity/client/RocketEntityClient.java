package com.space_m.rocket.entity.client;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.particle.helper.ParticleEffectClient;
import com.space_m.celestial.CelestialBody;
import com.space_m.dim.SpaceDimensions;
import com.space_m.entity.SpaceEntities;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.entity.EntityRideableRocket;
import com.space_m.rocket.entity.EntityRideableRocket.RocketState;
import com.space_m.sound.SpaceSounds;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Client-Teil der fliegenden Rakete (R5-B): Renderer, Geraeusche (Original {@code AudioWrapper} in
 * {@code EntityRideableRocket.onUpdate}), Partikel-Weiterleitung und das Blick-Overlay {@code printHook}
 * ({@code ILookOverlay} der Rakete bzw. ihres Kapsel-Dummys, Original {@code ModEventHandlerClient} Fadenkreuz-Hook).
 * Kamera-Abstand: {@code com.space_m.mixin.RocketCameraMixin}; Spieler-Neigung: {@link RocketPlayerRenderEvents}.
 */
public final class RocketEntityClient implements EntityRideableRocket.RocketClientHook {

    private RocketEntityClient() {}

    /** Aus {@code SpaceClient.init()}. */
    public static void init() {
        EntityRideableRocket.clientHook = new RocketEntityClient();

        EntityRendererRegistry.register(SpaceEntities.ENTITY_RIDEABLE_ROCKET, RenderRocketCustom::new);
        EntityRendererRegistry.register(SpaceEntities.ENTITY_RIDEABLE_ROCKET_DUMMY, NoopRenderer::new); // Original RenderEmpty

        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> renderLookOverlay(graphics));
    }

    /** Aus {@code SpaceClient.setup()}: Startrampe (GUI, BER). */
    public static void setup() {
        dev.architectury.registry.menu.MenuRegistry.registerScreenFactory(com.space_m.inventory.SpaceMenus.LAUNCH_PAD_ROCKET.get(),
                com.space_m.rocket.pad.client.GUILaunchPadRocket::new);
        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(com.space_m.blockentity.SpaceBlockEntities.LAUNCH_PAD_ROCKET.get(),
                com.space_m.rocket.pad.client.RenderLaunchPadRocket::new);
    }

    // ── Geraeusche ─────────────────────────────────────────────────────────────────────────────

    @Override
    public void onStateChanged(EntityRideableRocket rocket, RocketState state) {
        if (state == RocketState.LAUNCHING) {
            RocketSound ignition = new RocketSound(SpaceSounds.ENTITY_ROCKET_IGNITION.get(), rocket.getX(), rocket.getY(), rocket.getZ(), 1.0F, 250.0F, 5, false);
            Minecraft.getInstance().getSoundManager().play(ignition);
        }
    }

    @Override
    public void tickSounds(EntityRideableRocket rocket, RocketState state) {
        // We can't start audio loops at the same time as playing a sound, for some reason
        if (state == RocketState.LAUNCHING
                || (state == RocketState.LANDING && rocket.getDeltaMovement().y > -0.4)
                || (state == RocketState.TRANSFER && rocket.getTransferProgress() <= 0.15)) {
            RocketSound audio = rocket.audio instanceof RocketSound s ? s : null;
            if (audio == null || !audio.isPlaying()) {
                boolean light = rocket.getRocket().stages.size() <= 1;
                audio = new RocketSound(light ? SpaceSounds.ENTITY_ROCKET_FLY_LIGHT.get() : SpaceSounds.ENTITY_ROCKET_FLY_HEAVY.get(),
                        rocket.getX(), rocket.getY(), rocket.getZ(), 1.0F, 250.0F, 5, true);
                rocket.audio = audio;
                Minecraft.getInstance().getSoundManager().play(audio);
            }

            audio.updatePosition(rocket.getX(), rocket.getY(), rocket.getZ());
            audio.keepAlive();
        } else {
            stop(rocket);
        }
    }

    @Override
    public void playStage(EntityRideableRocket rocket, float pitch) {
        // Original getLoopedSound(..., 0.5F, 250F, pitch, 40) - die Tonhoehe uebernimmt AudioWrapperClient nicht
        RocketSound decouple = new RocketSound(SpaceSounds.ENTITY_ROCKET_STAGE.get(), rocket.getX(), rocket.getY(), rocket.getZ(), 0.5F, 250.0F, 40, false);
        Minecraft.getInstance().getSoundManager().play(decouple);
    }

    @Override
    public void onRemoved(EntityRideableRocket rocket) {
        if (rocket.level().isClientSide) stop(rocket);
    }

    private static void stop(EntityRideableRocket rocket) {
        if (rocket.audio instanceof RocketSound s) {
            Minecraft.getInstance().getSoundManager().stop(s);
            s.stopSound();
        }
        rocket.audio = null;
    }

    // ── Partikel ───────────────────────────────────────────────────────────────────────────────

    /** Original {@code ExplosionLarge.spawnShock} auf der Clientseite ({@code proxy.effectNT}). */
    @Override
    public void spawnShock(Level level, double x, double y, double z, int count, double strength) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "smoke");
        data.putString("mode", "shock");
        data.putInt("count", count);
        data.putDouble("strength", strength);
        data.putDouble("posX", x);
        data.putDouble("posY", y + 0.5);
        data.putDouble("posZ", z);
        ParticleEffectClient.effectNT(data);
    }

    @Override
    public void effectNT(CompoundTag data) {
        ParticleEffectClient.effectNT(data);
    }

    // ── Blick-Overlay (Original ILookOverlay.printHook) ────────────────────────────────────────

    private static void renderLookOverlay(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult ehr) || hit.getType() != HitResult.Type.ENTITY) return;

        Entity entity = ehr.getEntity();
        EntityRideableRocket rocket = null;
        if (entity instanceof EntityRideableRocket r) rocket = r;
        else if (entity instanceof EntityRideableRocket.EntityRideableRocketDummy dummy) rocket = dummy.parent;
        if (rocket == null) return;

        printHook(g, rocket, mc.level);
    }

    public static void printHook(GuiGraphics g, EntityRideableRocket rocket, Level world) {
        RocketState state = rocket.getState();
        if (state == RocketState.LAUNCHING
         || state == RocketState.LANDING
         || state == RocketState.TIPPING
         || state == RocketState.DOCKING
         || state == RocketState.UNDOCKING)
            return;

        List<Component> text = new ArrayList<>();

        if (state == RocketState.TRANSFER) {
            double progress = rocket.getTransferProgress();
            CelestialBody target = rocket.getTransferTarget();

            text.add(Component.translatable("station.travelling").withStyle(ChatFormatting.AQUA).append(Component.literal(": ").withStyle(ChatFormatting.AQUA))
                    .append(Component.translatable("body." + (target != null ? target.name : "")).withStyle(ChatFormatting.RESET)));
            text.add(Component.translatable("station.progress").withStyle(ChatFormatting.AQUA).append(Component.literal(": ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(Math.round(progress * 100) + "%").withStyle(ChatFormatting.RESET)));

            printGeneric(g, "Rocket", 0xffff00, 0x404000, text);

            return;
        }

        RocketStruct rocketStruct = rocket.getRocket();
        if (rocketStruct.stages.size() == 0 && world.dimension() != SpaceDimensions.ORBIT && !rocket.isReusable()) return;

        Player player = Minecraft.getInstance().player;

        CelestialBody.Target from = CelestialBody.getTarget(world, (int) rocket.getX(), (int) rocket.getZ());
        ItemVOTVdrive.Target to = rocket.getTarget();

        boolean canLaunch = to.body() != null && state == RocketState.AWAITING;

        // Check if the stage can make the journey
        if (state == RocketState.NEEDSFUEL) {
            text.add(Component.literal("Rocket has no fuel!").withStyle(ChatFormatting.RED));
        } else if (canLaunch && !rocketStruct.hasSufficientFuel(from.body, to.body(), from.inOrbit, to.inOrbit())) {
            text.add(Component.literal("Rocket can't reach destination!").withStyle(ChatFormatting.RED));
            canLaunch = false;
        }

        Entity rider = rocket.getFirstPassenger();
        if (rider == null) {
            text.add(Component.literal("Interact to enter"));
        } else if (rider != player) {
            text.add(Component.literal("OCCUPIED"));
        } else {
            if (to.inOrbit()) {
                text.add(Component.literal("Destination: ORBITAL STATION"));
            } else if (to.body() != null) {
                text.add(Component.literal("Destination: ").append(Component.translatable("body." + to.body().name)));
            } else {
                text.add(Component.literal("Destination: NO DRIVE INSTALLED"));
            }

            if (canLaunch) {
                text.add(Component.literal("JUMP TO LAUNCH"));
            } else if (state == RocketState.LANDED) {
                text.add(Component.literal("Insert next drive to continue"));
            }

            ItemStack stack = player.getMainHandItem();
            if ((state == RocketState.LANDED || state == RocketState.AWAITING) && !stack.isEmpty() && stack.getItem() instanceof ItemVOTVdrive) {
                if (ItemVOTVdrive.getProcessed(stack)) {
                    text.add(Component.literal("Interact to swap drive"));
                }
            }
        }

        printGeneric(g, "Rocket", 0xffff00, 0x404000, text);
    }

    /** Original {@code ILookOverlay.printGeneric} (ohne die Erste-Person-Einschraenkung des HBM-Ports: in 1.7.10 lief der Fadenkreuz-Hook auch in der dritten Person). */
    public static void printGeneric(GuiGraphics g, String title, int titleCol, int bgCol, List<Component> text) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;

        g.drawString(font, title, pX + 1, pZ - 9, bgCol, false);
        g.drawString(font, title, pX, pZ - 10, titleCol, false);

        for (Component line : text) {
            g.drawString(font, line, pX, pZ, 0xFFFFFF, true);
            pZ += 10;
        }
    }
}
