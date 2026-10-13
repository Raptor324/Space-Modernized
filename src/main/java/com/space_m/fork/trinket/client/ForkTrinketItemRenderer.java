package com.space_m.fork.trinket.client;

import com.hbm_m.client.render.item.OrigInventoryTransform;
import com.hbm_m.item.TrinketBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.fork.trinket.ForkTrinketBlock;
import com.space_m.fork.trinket.ForkTrinkets;
import com.space_m.fork.trinket.ForkTrinkets.ForkBobbleType;
import com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Gegenstandsdarstellung der Fork-Figuren wie HBM {@code TrinketItemRenderer}: im Inventar 1:1 die Original-Kette
 * ({@code ItemRenderLibrary} bobblehead bzw. {@code RenderPlushie.getRenderer}), sonst auf Blockgroesse skaliert.
 */
public class ForkTrinketItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static ForkTrinketItemRenderer instance;

    public static ForkTrinketItemRenderer instance() {
        if (instance == null) instance = new ForkTrinketItemRenderer();
        return instance;
    }

    private ForkTrinketItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ForkTrinketBlock block)) return;
        int type = TrinketBlockItem.getType(stack);

        if (ctx == ItemDisplayContext.GUI) {
            renderInventory(block.kind, type, ps, buf, light);
            return;
        }

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        ps.mulPose(Axis.YN.rotationDegrees(270));
        if (block.kind == ForkTrinketBlock.Kind.BOBBLE) {
            ps.scale(0.35F, 0.35F, 0.35F);
            ForkTrinketRenderer.renderBobble(ps, buf, ForkTrinkets.safe(ForkBobbleType.class, type), light);
        } else {
            ForkPlushieType p = ForkTrinkets.safe(ForkPlushieType.class, type);
            switch (p) {
                case TETO, MIKU, NERU -> ps.scale(0.6F, 0.6F, 0.6F);
                case FATO -> ps.scale(1.2F, 0.6F, 1.2F);
                default -> { }
            }
            ForkTrinketRenderer.renderPlushie(ps, buf, p, 0, light);
        }
        ps.popPose();
    }

    private static void renderInventory(ForkTrinketBlock.Kind kind, int type, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        OrigInventoryTransform.apply(ps);
        if (kind == ForkTrinketBlock.Kind.BOBBLE) {
            ps.translate(0, -3.5, 0);
            ps.scale(10F, 10F, 10F);
            ps.scale(0.5F, 0.5F, 0.5F);
            ForkTrinketRenderer.renderBobble(ps, buf, ForkTrinkets.safe(ForkBobbleType.class, type), light);
        } else {
            ps.translate(0, -6, 0);
            ps.scale(6F, 6F, 6F);
            ps.translate(0, 0.25, 0);
            ForkPlushieType p = ForkTrinkets.safe(ForkPlushieType.class, type);
            switch (p) {
                case TETO, MIKU, NERU -> { ps.translate(0, 0.25, 0); ps.scale(1.5F, 1.5F, 1.5F); }
                case FATO -> { ps.translate(0, 0.25, 0); ps.scale(3.0F, 1.5F, 3.0F); }
                default -> { }
            }
            ForkTrinketRenderer.renderPlushie(ps, buf, p, 0, light);
        }
        ps.popPose();
    }
}
