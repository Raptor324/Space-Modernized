//? if forge || neoforge {
package com.space_m.rocket.client;

import com.hbm_m.client.render.item.OrigInventoryTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Item-Darstellung der VAB, 1:1 NTM Space {@code RenderRocketAssembly.getRenderer()} ({@code ItemRenderBase}):
 * Inventar T(0,-1,0) S(2), gemeinsam S(0.55) und Teil "Base" des OBJ. Inventar-Kette ueber HBM {@code OrigInventoryTransform},
 * sonst die {@code ItemRenderBase}-Kette (Hand: T(0.5,0.25,0), am Boden S(1.5); S(0.25); ausser Dritte Person Rot Y 90).
 */
public class ItemRenderRocketAssembly extends BlockEntityWithoutLevelRenderer {

    private static ItemRenderRocketAssembly instance;

    public static ItemRenderRocketAssembly instance() {
        if (instance == null) instance = new ItemRenderRocketAssembly();
        return instance;
    }

    private ItemRenderRocketAssembly() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        boolean gui = ctx == ItemDisplayContext.GUI;
        if (gui) {
            OrigInventoryTransform.apply(ps);
            // renderInventory
            ps.translate(0, -1, 0);
            ps.scale(2.0F, 2.0F, 2.0F);
        } else {
            ps.translate(0.5, 0.5, 0.5);
            boolean entity = ctx == ItemDisplayContext.GROUND || ctx == ItemDisplayContext.FIXED;
            if (!entity) ps.translate(0.5, 0.25, 0);
            else ps.scale(1.5F, 1.5F, 1.5F);
            ps.scale(0.25F, 0.25F, 0.25F);
            boolean equipped = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            if (!equipped) ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        // renderCommon
        ps.scale(0.55F, 0.55F, 0.55F);
        RocketModels.model(RenderRocketAssembly.MODEL).renderPart("Base", ps,
                buf.getBuffer(RenderType.entityCutoutNoCull(RocketModels.texture(RenderRocketAssembly.TEXTURE))), gui ? 0xF000F0 : light);
        ps.popPose();
    }
}
//?}
