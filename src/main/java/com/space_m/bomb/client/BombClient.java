package com.space_m.bomb.client;

import com.hbm_m.particle.helper.ParticleCreators;
import com.hbm_m.particle.nt.NukeExplosionRings;
import com.hbm_m.particle.nt.NukeTorex;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.space_m.bomb.EntityBalefireAntimatter;
import com.space_m.bomb.SpaceBombs;
import com.space_m.machine.misc.client.ItemRenderMachines;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.util.Mth;

/**
 * Client-Anmeldung R9-B1 (Original {@code ClientProxy}: {@code RenderAntimatter}, {@code GUINukeAntimatter},
 * Item-Renderer {@code nuke_antimatter}; Effekt {@code EntityNukeTorex.startFacAnti}).
 */
public final class BombClient {

    private BombClient() {}

    /** Aus {@code SpaceClient.init()} (Mod-Konstruktor, nur Client). */
    public static void init() {
        EntityRendererRegistry.register(SpaceBombs.BALEFIRE_ANTIMATTER, NoopRenderer::new);

        // Original startFacAnti: Pilz Typ 2 (Regenbogen), Skala clamp(scale * 0.01, 0.5, 5) - ohne Wurzel wie statFac
        ParticleCreators.particleCreators().putIfAbsent(EntityBalefireAntimatter.TOREX_ANTI, (level, player, rand, x, y, z, tag) -> {
            float scale = Mth.clamp(tag.getFloat("scale") * 0.01F, 0.5F, 5F);
            NukeTorex torex = new NukeTorex(level, x, y, z).setScale(scale, true);
            torex.setType(2);
            ParticleEngineNT.INSTANCE.add(torex);
            ParticleEngineNT.INSTANCE.add(new NukeExplosionRings(level, x, y, z).setScale(scale));
        });

        ClientLifecycleEvent.CLIENT_SETUP.register(mc -> setup());
    }

    private static void setup() {
        BlockEntityRendererRegistry.register(SpaceBombs.ANTIMATTER_BE.get(), RenderAntimatter::new);
        MenuRegistry.registerScreenFactory(SpaceBombs.ANTIMATTER_MENU.get(), GUINukeAntimatter::new);
        ItemRenderMachines.register(SpaceBombs.NUKE_ANTIMATTER, RenderAntimatter.ITEM);
    }
}
