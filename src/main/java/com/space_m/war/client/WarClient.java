package com.space_m.war.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.projectile.BombletThetaRenderer;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Invasion;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.client.sky.CelestialSkyProvider;
import com.space_m.client.sky.SkyHooks;
import com.space_m.network.SpacePacketHandler;
import com.space_m.sound.SpaceSounds;
import com.space_m.war.CelestialNukeShockHandler;
import com.space_m.war.EntityCarpetBomblet;
import com.space_m.war.EntityCarpetJet;
import com.space_m.war.WarEntities;
import com.space_m.war.WarEvents;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Client-Teil von Krieg/Invasion (R8-B), aus {@code SpaceClient.init()}:
 * <ul>
 *   <li>Renderer: Siege-UFO, War-Behemoth, Carpet-Jet (+ HBM-Bomblet-Renderer fuer dessen Bomblets).</li>
 *   <li>Nuklear-Erschuetterungen: Empfang + {@link SkyHooks#clientShocks} (Dekorator, alles andere an {@code prev}).</li>
 *   <li>Invasion: Meteore {@code WorldProviderCelestial.Meteor.addMeteor()} (Dekorator, Warnung bleibt bei R1).</li>
 *   <li>Krieg: Projektil-Sync (flashtime/traveltime) und Einschlagton {@code misc.impact} bei traveltime 18
 *       (Original {@code ModEventHandlerClient.onClientTick}).</li>
 *   <li>Carpet-Jet: Schleifenton {@code entity.jetloopbomb} (Original {@code getLoopedSound(..., 2F, 250F, 1F, 20)}).</li>
 * </ul>
 */
public final class WarClient {

    private WarClient() {}

    private static boolean initialized;

    public static void init() {
        if (initialized) return;
        initialized = true;

        SpacePacketHandler.registerS2C(SpacePacketHandler.NUKE_SHOCKS, CelestialNukeShockHandler.Packet::decode,
                (pkt, ctx) -> ctx.queue(pkt::apply));
        SpacePacketHandler.registerS2C(SpacePacketHandler.WAR_PROJECTILES, WarEvents.WarProjectilePacket::decode,
                (pkt, ctx) -> ctx.queue(() -> applyProjectiles(pkt)));

        SkyHooks.set(new ShockSkyHooks(SkyHooks.get()));

        CBT_Invasion.ClientHooks prevInvasion = CBT_Invasion.clientHooks;
        CBT_Invasion.clientHooks = new CBT_Invasion.ClientHooks() {
            @Override
            public void warn(CBT_Invasion invasion) {
                prevInvasion.warn(invasion);
            }

            @Override
            public void addMeteor() {
                CelestialSkyProvider.Meteor.addMeteor();
            }
        };

        ClientTickEvent.CLIENT_POST.register(WarClient::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> CelestialNukeShockHandler.clearClient());

        WarEntities.clientTick = WarClient::tickCarpetJet;

        EntityRendererRegistry.register(WarEntities.SIEGE_UFO, RenderSiegeUFO::new);
        EntityRendererRegistry.register(WarEntities.WAR_BEHEMOTH, RenderBehemoth::new);
        EntityRendererRegistry.register(WarEntities.CARPET_JET, RenderCarpetJet::new);
        EntityRendererRegistry.register(WarEntities.CARPET_BOMBLET, ctx -> castRenderer(new BombletThetaRenderer(ctx)));
    }

    @SuppressWarnings("unchecked")
    private static EntityRenderer<EntityCarpetBomblet> castRenderer(EntityRenderer<?> renderer) {
        return (EntityRenderer<EntityCarpetBomblet>) renderer;
    }

    // ── Krieg ──

    private static void applyProjectiles(WarEvents.WarProjectilePacket pkt) {
        CelestialBody body = CelestialBody.getBodyOrNull(pkt.body);
        if (body == null) return;
        CBT_War war = body.getTrait(CBT_War.class);
        if (war == null) return;
        List<CBT_War.Projectile> projectiles = war.getProjectiles();
        int n = Math.min(projectiles.size(), pkt.flash.length);
        for (int i = 0; i < n; i++) {
            projectiles.get(i).setFlashtime(pkt.flash[i]);
            projectiles.get(i).setTravel(pkt.travel[i]);
        }
    }

    private static void onClientTick(Minecraft mc) {
        if (mc.isPaused() || mc.level == null || mc.player == null) return;

        CBT_War war = CelestialBody.getTrait(mc.level, CBT_War.class);

        if (war != null) {
            for (int i = 0; i < war.getProjectiles().size(); i++) {
                CBT_War.Projectile projectile = war.getProjectiles().get(i);
                if (projectile != null && projectile.getTravel() >= 18 && projectile.getTravel() <= 18) {
                    mc.player.playSound(SpaceSounds.MISC_IMPACT.get(), 10F, 1F);
                }
            }
        }
    }

    // ── Nuklear-Erschuetterungen am Himmel ──

    static final class ShockSkyHooks implements SkyHooks {
        private final SkyHooks prev;

        ShockSkyHooks(SkyHooks prev) {
            this.prev = prev;
        }

        @Override
        public List<ShockStatus> clientShocks(CelestialBody body) {
            List<CelestialNukeShockHandler.ShockStatus> list = CelestialNukeShockHandler.getClientShocks(body);
            if (list.isEmpty()) return prev.clientShocks(body);
            List<ShockStatus> out = new ArrayList<>(list.size());
            for (CelestialNukeShockHandler.ShockStatus s : list) {
                out.add(new ShockStatus(s.time(), s.centerX(), s.centerY(), s.strength()));
            }
            return out;
        }

        @Override @Nullable public CelestialBody stationOrbiting() { return prev.stationOrbiting(); }
        @Override @Nullable public CelestialBody stationTarget() { return prev.stationTarget(); }
        @Override public StationState stationState() { return prev.stationState(); }
        @Override public double stationTransferProgress(float partialTicks) { return prev.stationTransferProgress(partialTicks); }
        @Override public double stationUnscaledProgress(float partialTicks) { return prev.stationUnscaledProgress(partialTicks); }
        @Override public List<StationPos> orbitingStations() { return prev.orbitingStations(); }
        @Override public void renderSatellites(float partialTicks, ClientLevel level, float solarAngle) { prev.renderSatellites(partialTicks, level, solarAngle); }
    }

    // ── Carpet-Jet-Schleife ──

    private static final Map<EntityCarpetJet, JetLoop> LOOPS = new WeakHashMap<>();

    private static void tickCarpetJet(EntityCarpetJet jet) {
        if (jet.getSyncedHealth() <= 0) return;
        JetLoop loop = LOOPS.get(jet);
        if (loop != null && !loop.isStopped()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.distanceToSqr(jet) > 250 * 250) return;

        loop = new JetLoop(jet);
        LOOPS.put(jet, loop);
        mc.getSoundManager().play(loop);
    }

    private static final class JetLoop extends AbstractTickableSoundInstance {
        private final EntityCarpetJet jet;

        JetLoop(EntityCarpetJet jet) {
            super(SpaceSounds.ENTITY_JETLOOPBOMB.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.jet = jet;
            this.looping = true;
            this.delay = 0;
            this.volume = 2.0F;
            this.pitch = 1.0F;
            this.x = jet.getX();
            this.y = jet.getY();
            this.z = jet.getZ();
        }

        @Override
        public void tick() {
            if (jet.isRemoved() || jet.getSyncedHealth() <= 0) {
                this.stop();
                return;
            }
            this.x = jet.getX();
            this.y = jet.getY();
            this.z = jet.getZ();
        }
    }
}
