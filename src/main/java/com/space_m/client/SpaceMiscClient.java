package com.space_m.client;

import java.util.Random;

import com.hbm_m.particle.nt.ParticleEngineNT;
import com.space_m.orbit.propulsion.client.Lpw2Overlay;
import com.space_m.particle.client.ParticleDuoDecNT;
import com.space_m.rocket.client.RenderDropPod;
import com.space_m.rocket.client.RocketPronter;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

/**
 * R9-B4: Client-Reste von NTM Space ohne eigenen Bereich:
 * <ul>
 *   <li>LPW-2-Blick-Overlay im Orbit ({@link Lpw2Overlay}, Original {@code MachineLPW2.printHook}).</li>
 *   <li>Teil-Renderer {@code "drop_pod"} von {@code rp_pod_20} ({@link RenderDropPod}).</li>
 *   <li>Original {@code ModEventHandler.onPlayerTick} (Client, START, nicht unsichtbar/schleichend): Spieler
 *       {@code ShadyUtil.DUODEC_} hinterlaesst Notenpartikel ({@code ParticleUtil.spawnTuneFlame} -> "duoewe",
 *       {@code spawnJesusFlame} -> "duodec", je 1/12 pro Tick, Punkt in bis zu 3 Bloecken um Hueftehoehe).</li>
 * </ul>
 */
public final class SpaceMiscClient {

    /** Original {@code ShadyUtil.DUODEC_}. */
    public static final String DUODEC_ = "85d54b8c-1862-4c86-b351-5ef1b998aa32";

    private static final Random rand = new Random();

    private SpaceMiscClient() {}

    /** Aus {@code SpaceClient.init()}. */
    public static void init() {
        Lpw2Overlay.init();
        RocketPronter.registerPartRenderer("drop_pod", new RenderDropPod());
        TickEvent.PLAYER_PRE.register(SpaceMiscClient::playerTick);
    }

    private static void playerTick(Player player) {
        if (!player.level().isClientSide || player.isInvisible() || player.isShiftKeyDown()) return;
        if (!player.getUUID().toString().equals(DUODEC_)) return;

        // Vec3.createVectorHelper(3 * rand, 0, 0).rotateAroundZ(rand * PI).rotateAroundY(rand * 2PI)
        double x = 3 * rand.nextDouble(), y = 0, z = 0;
        double a = rand.nextDouble() * Math.PI;
        double cos = Math.cos(a), sin = Math.sin(a);
        double x1 = x * cos + y * sin;
        double y1 = y * cos - x * sin;
        a = rand.nextDouble() * Math.PI * 2;
        cos = Math.cos(a);
        sin = Math.sin(a);
        double x2 = x1 * cos + z * sin;
        double z2 = z * cos - x1 * sin;

        double px = player.getX() + x2, py = player.getY() + 1 + y1, pz = player.getZ() + z2;
        spawn(px, py, pz, false); // spawnTuneFlame -> "duoewe" -> ParticleDuoDec
        spawn(px, py, pz, true);  // spawnJesusFlame -> "duodec" -> ParticleDuoDec2
    }

    /** {@code if(rand.nextInt(12) == 0)} + {@code ClientProxy.effectNT}: Partikeleinstellung Alle bzw. Verringert (halbe Chance). */
    private static void spawn(double x, double y, double z, boolean duodec) {
        if (rand.nextInt(12) != 0) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        ParticleStatus setting = mc.options.particles().get();
        if (setting == ParticleStatus.ALL || (setting == ParticleStatus.DECREASED && level.random.nextBoolean())) {
            ParticleEngineNT.INSTANCE.add(new ParticleDuoDecNT(level, x, y, z, duodec));
        }
    }
}
