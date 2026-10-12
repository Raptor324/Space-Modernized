package com.space_m.machine.misc.client;

import com.hbm_m.client.model.ConnectedDecoBlockBakedModel;
import com.hbm_m.platform.PlatformHooks;
import com.space_m.SpaceMod;
import com.space_m.machine.misc.MiscBlocks;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

//? if forge {
import net.minecraftforge.client.event.ModelEvent;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?}

/**
 * Verbundtextur von {@code deco_stainless} (Original {@code BlockDecoCT}, {@code IBlockCT}: Fragmente aus
 * {@code deco_stainless} + {@code deco_stainless_ct}, verbunden nur mit sich selbst): das gebackene Blockmodell wird wie
 * bei HBMs CT-Bloecken in {@link ConnectedDecoBlockBakedModel} gehuellt (Mod-Bus {@code ModelEvent.ModifyBakingResult}).
 */
public final class DecoStainlessCt {

    private DecoStainlessCt() {}

    /** Aus {@code SpaceClient.init} (Mod-Konstruktor, Mod-Bus). */
    public static void init() {
        //? if forge {
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(DecoStainlessCt::onModelBake);
        //?} elif neoforge {
        /*net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus().addListener(DecoStainlessCt::onModelBake);
        *///?}
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void onModelBake(ModelEvent.ModifyBakingResult event) {
        java.util.Map models = event.getModels();
        Object loc = PlatformHooks.createModelLocation(MiscBlocks.DECO_STAINLESS.getId(), "");
        BakedModel baked = (BakedModel) models.get(loc);
        if (baked == null || baked instanceof ConnectedDecoBlockBakedModel) return;

        ResourceLocation full = SpaceMod.id("block/deco_stainless");
        ResourceLocation ct = SpaceMod.id("block/deco_stainless_ct");
        net.minecraft.world.level.block.Block self = MiscBlocks.DECO_STAINLESS.get();
        models.put(loc, new ConnectedDecoBlockBakedModel(baked, full, ct, null, (a, b) -> b == self));
    }
}
