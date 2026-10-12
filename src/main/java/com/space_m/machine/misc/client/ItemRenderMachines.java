package com.space_m.machine.misc.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
 * Item-Darstellung der R7-A-Maschinen: die Kette des Original-{@code ItemRenderBase} (Inventar: {@link OrigInventoryTransform}
 * + {@code renderInventory()}; sonst Hand T(0.5, 0.25, 0) bzw. am Boden/Rahmen S(1.5), dann S(0.25), ausser Dritte Person
 * Rot Y 90) und danach {@code renderCommon()} des jeweiligen Renderers. Renderer melden sich mit {@link #register} an.
 */
public class ItemRenderMachines extends BlockEntityWithoutLevelRenderer {

    /** Original {@code ItemRenderBase}: {@code renderInventory()} und {@code renderCommon()}. */
    public interface ItemRender {
        void renderInventory(PoseStack ps);

        void renderCommon(PoseStack ps, MultiBufferSource buf, int light);
    }

    private static final Map<Supplier<? extends Block>, ItemRender> REGISTERED = new HashMap<>();
    private static Map<Block, ItemRender> resolved;
    private static ItemRenderMachines instance;

    public static ItemRenderMachines instance() {
        if (instance == null) instance = new ItemRenderMachines();
        return instance;
    }

    /** Aus dem Client-Setup (Bloecke werden erst beim Rendern aufgeloest). */
    public static void register(Supplier<? extends Block> block, ItemRender render) {
        REGISTERED.put(block, render);
        resolved = null;
    }

    private ItemRenderMachines() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ItemRender find(Block block) {
        if (resolved == null) {
            Map<Block, ItemRender> map = new HashMap<>();
            for (Map.Entry<Supplier<? extends Block>, ItemRender> e : REGISTERED.entrySet()) map.put(e.getKey().get(), e.getValue());
            resolved = map;
        }
        return resolved.get(block);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem bi)) return;
        ItemRender render = find(bi.getBlock());
        if (render == null) return;

        ps.pushPose();
        boolean gui = ctx == ItemDisplayContext.GUI;
        int l = gui ? 0xF000F0 : light;
        if (gui) {
            OrigInventoryTransform.apply(ps);
            render.renderInventory(ps);
        } else {
            ps.translate(0.5, 0.5, 0.5);
            boolean entity = ctx == ItemDisplayContext.GROUND || ctx == ItemDisplayContext.FIXED;
            if (!entity) ps.translate(0.5, 0.25, 0);
            else ps.scale(1.5F, 1.5F, 1.5F);
            ps.scale(0.25F, 0.25F, 0.25F);
            boolean equipped = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            if (!equipped) ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        render.renderCommon(ps, buf, l);
        ps.popPose();
    }
}
