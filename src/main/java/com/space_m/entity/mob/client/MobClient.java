package com.space_m.entity.mob.client;

import com.hbm_m.client.render.entity.mob.RenderCreeperUniversal;
import com.hbm_m.client.render.mob.SiegeCraftRenderer;
import com.space_m.SpaceMod;
import com.space_m.entity.mob.EntityDepthSquid;
import com.space_m.entity.mob.EntityScrapFish;
import com.space_m.entity.mob.EntityScuttlecrab;
import com.space_m.entity.mob.EntitySifterEel;
import com.space_m.entity.mob.SpaceMobs;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * R8-A: Renderer der Space-Mobs (Original {@code ClientProxy.registerEntityRenderers}), beide Loader ueber Architectury.
 */
// Original-Klasse: RenderScutter (Scutterfish: RenderEntityMulti mit festem Textur-Lambda scutterfish.png)
public final class MobClient {

    private MobClient() {}

    public static final ResourceLocation SCUTTERFISH_TEX = SpaceMod.id("textures/entity/scutterfish.png");
    public static final ResourceLocation CREEPER_FLESH_TEX = SpaceMod.id("textures/entity/creeper_flesh.png");

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void init() {
        EntityRendererRegistry.register(SpaceMobs.MOON_COW, RenderMoonCow::new);
        EntityRendererRegistry.register(SpaceMobs.SCUTTERFISH,
                ctx -> new RenderEntityMulti<>(ctx, new SpaceMobModels.Scutter<>(), 0.3F, e -> SCUTTERFISH_TEX));
        EntityRendererRegistry.register(SpaceMobs.TANKBOT, RenderTankbot::new);
        EntityRendererRegistry.register(SpaceMobs.SCUTTLECRAB,
                ctx -> new RenderEntityMulti<>(ctx, new SpaceMobModels.Scuttlecrab(), 0.5F, RenderEntityMulti.byEnum(EntityScuttlecrab.Scuttlecrab.class)));
        EntityRendererRegistry.register(SpaceMobs.DEPTH_SQUID,
                ctx -> new RenderEntityMulti<>(ctx, new SpaceMobModels.DepthSquid(), 0.5F, RenderEntityMulti.byEnum(EntityDepthSquid.DepthSquid.class)));
        EntityRendererRegistry.register(SpaceMobs.SCRAP_FISH,
                ctx -> new RenderEntityMulti<>(ctx, new SpaceMobModels.ScrapFish<>(), 0.5F, RenderEntityMulti.byEnum(EntityScrapFish.ScrapFish.class)));
        EntityRendererRegistry.register(SpaceMobs.SIFTER_EEL,
                ctx -> new RenderEntityMulti<>(ctx, new SpaceMobModels.SifterEel<>(), 0.5F, RenderEntityMulti.byEnum(EntitySifterEel.SifterEel.class)));
        EntityRendererRegistry.register(SpaceMobs.BF_ANGEL, RenderBFA::new);
        // Original RenderCreeperUniversal(creeper_flesh.png, Vanilla creeper_armor.png)
        EntityRendererRegistry.register(SpaceMobs.CREEPER_FLESH, (EntityRendererProvider) ctx ->
                new RenderCreeperUniversal(ctx, CREEPER_FLESH_TEX, RenderCreeperUniversal.VANILLA_CREEPER_ARMOR_TEXTURE));
        // Original RenderSiegeCraft (HBM-Port)
        EntityRendererRegistry.register(SpaceMobs.MICRO_UFO, (EntityRendererProvider) SiegeCraftRenderer::new);
    }
}
