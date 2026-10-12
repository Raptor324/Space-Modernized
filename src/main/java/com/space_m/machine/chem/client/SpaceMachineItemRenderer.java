package com.space_m.machine.chem.client;

import java.util.IdentityHashMap;
import java.util.Map;

import com.hbm_m.client.render.item.OrigInventoryTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Item-Darstellung der R7-B-Maschinen, 1:1 die Original-{@code ItemRenderBase}-Kette: im Inventar
 * {@code OrigInventoryTransform} + {@code renderInventory()}, sonst Hand T(0.5, 0.25, 0) bzw. am Boden S(1.5), dann
 * S(0.25) und ausser Dritte Person Rot Y 90; danach {@code renderCommon()}. Eintraege meldet jede Client-Klasse mit
 * {@link #register(Block, Entry)} an (Muster {@code ItemRenderPropulsion} von R6-B).
 */
public class SpaceMachineItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** {@code renderInventory()} + {@code renderCommon()} eines Original-{@code ItemRenderBase}. */
    public interface Entry {
        void inventory(PoseStack ps);

        void common(PoseStack ps, MultiBufferSource buf, int light);
    }

    private static final Map<Block, Entry> ENTRIES = new IdentityHashMap<>();
    private static SpaceMachineItemRenderer instance;

    public static void register(Block block, Entry entry) {
        ENTRIES.put(block, entry);
    }

    public static SpaceMachineItemRenderer instance() {
        if (instance == null) instance = new SpaceMachineItemRenderer();
        return instance;
    }

    private SpaceMachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem bi)) return;
        Entry entry = ENTRIES.get(bi.getBlock());
        if (entry == null) return;

        ps.pushPose();
        boolean gui = ctx == ItemDisplayContext.GUI;
        int l = gui ? 0xF000F0 : light;
        if (gui) {
            OrigInventoryTransform.apply(ps);
            entry.inventory(ps);
        } else {
            ps.translate(0.5, 0.5, 0.5);
            boolean entity = ctx == ItemDisplayContext.GROUND || ctx == ItemDisplayContext.FIXED;
            if (!entity) ps.translate(0.5, 0.25, 0);
            else ps.scale(1.5F, 1.5F, 1.5F);
            ps.scale(0.25F, 0.25F, 0.25F);
            boolean equipped = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            if (!equipped) ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        entry.common(ps, buf, l);
        ps.popPose();
    }
}
