//? if forge || neoforge {
package com.space_m.orbit.client;

import com.hbm_m.client.render.item.OrigInventoryTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Item-Darstellung der Stationsports, 1:1 NTM Space {@code RenderOrbitalStation.getRenderer()} ({@code ItemRenderBase}):
 * Inventar T(0, 2, 0) S(2); ganzes Modell ohne Flaechen-Culling, Textur {@code docking_port} (Startport R6-B:
 * {@code docking_port_launcher}, eigener Renderer). Ausserhalb des Inventars die {@code ItemRenderBase}-Kette
 * (Hand: T(0.5, 0.25, 0), am Boden S(1.5); S(0.25); ausser Dritte Person Rot Y 90).
 */
public class ItemRenderOrbitalStation extends BlockEntityWithoutLevelRenderer {

    private static ItemRenderOrbitalStation instance;

    public static ItemRenderOrbitalStation instance() {
        if (instance == null) instance = new ItemRenderOrbitalStation();
        return instance;
    }

    private ItemRenderOrbitalStation() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        boolean gui = ctx == ItemDisplayContext.GUI;
        if (gui) {
            OrigInventoryTransform.apply(ps);
            // renderInventory
            ps.translate(0, 2, 0);
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
        // renderCommonWithStack (GL_CULL_FACE aus)
        boolean launcher = stack.getItem() instanceof BlockItem bi
                && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bi.getBlock()).getPath().equals("orbital_station_launcher");
        RenderOrbitalStation.DOCKING_PORT.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(
                launcher ? RenderOrbitalStation.DOCKING_PORT_LAUNCHER_TEX : RenderOrbitalStation.DOCKING_PORT_TEX)), gui ? 0xF000F0 : light);
        ps.popPose();
    }
}
//?}
