package com.space_m.orbit.propulsion.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.item.OrigInventoryTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.orbit.propulsion.PropulsionBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Item-Darstellung der Triebwerke und des Stationscomputers, 1:1 die {@code getRenderer()} ({@code ItemRenderBase}) von
 * {@code RenderXenonThruster} (Inventar T(0,-4,0) S(4), {@code Thruster}), {@code RenderHTR3} (T(0,-3,0) S(2.5); S(0.5),
 * ohne {@code ExhaustVacuum}), {@code RenderHTRF4} (T(0,-1,0) S(1.5); S(0.5), alles), {@code RenderHTRNeo} (wie HTRF4,
 * ohne {@code Exhaust}) und {@code RenderOrbitalComputer} (T(0,-2.5,0) S(6), ohne {@code Drive}, ohne Culling).
 * Ausserhalb des Inventars die {@code ItemRenderBase}-Kette (Hand: T(0.5, 0.25, 0), am Boden S(1.5); S(0.25); ausser
 * Dritte Person Rot Y 90).
 */
public class ItemRenderPropulsion extends BlockEntityWithoutLevelRenderer {

    private static ItemRenderPropulsion instance;

    public static ItemRenderPropulsion instance() {
        if (instance == null) instance = new ItemRenderPropulsion();
        return instance;
    }

    private ItemRenderPropulsion() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem bi)) return;
        Block block = bi.getBlock();

        ps.pushPose();
        boolean gui = ctx == ItemDisplayContext.GUI;
        int l = gui ? 0xF000F0 : light;
        if (gui) {
            OrigInventoryTransform.apply(ps);
            // renderInventory
            if (block == PropulsionBlocks.MACHINE_XENON_THRUSTER.get()) {
                ps.translate(0, -4, 0);
                ps.scale(4F, 4F, 4F);
            } else if (block == PropulsionBlocks.MACHINE_HTR3.get()) {
                ps.translate(0, -3, 0);
                ps.scale(2.5F, 2.5F, 2.5F);
            } else if (block == PropulsionBlocks.ORBITAL_STATION_COMPUTER.get()) {
                ps.translate(0, -2.5, 0);
                ps.scale(6F, 6F, 6F);
            } else {
                ps.translate(0, -1, 0);
                ps.scale(1.5F, 1.5F, 1.5F);
            }
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
        if (block == PropulsionBlocks.MACHINE_XENON_THRUSTER.get()) {
            PropulsionModels.XENON_THRUSTER.renderPart("Thruster", ps, buf.getBuffer(RenderType.entityCutout(PropulsionModels.XENON_THRUSTER_TEX)), l);
        } else if (block == PropulsionBlocks.MACHINE_HTR3.get()) {
            ps.scale(0.5F, 0.5F, 0.5F);
            allExcept(PropulsionModels.HTR3, "ExhaustVacuum", ps, buf.getBuffer(RenderType.entityCutout(PropulsionModels.LPW2_TEX)), l);
        } else if (block == PropulsionBlocks.MACHINE_HTRF4.get()) {
            ps.scale(0.5F, 0.5F, 0.5F);
            PropulsionModels.HTRF4.renderAll(ps, buf.getBuffer(RenderType.entityCutout(PropulsionModels.LPW2_TEX)), l);
        } else if (block == PropulsionBlocks.MACHINE_HTRF4NEO.get()) {
            ps.scale(0.5F, 0.5F, 0.5F);
            allExcept(PropulsionModels.HTRF4_NEO, "Exhaust", ps, buf.getBuffer(RenderType.entityCutout(PropulsionModels.HTRF4_NEO_TEX)), l);
        } else if (block == PropulsionBlocks.ORBITAL_STATION_COMPUTER.get()) {
            RenderOrbitalComputer.renderAllExceptDrive(ps, buf.getBuffer(RenderType.entityCutoutNoCull(PropulsionModels.ORBITAL_COMPUTER_TEX)), l);
        }
        ps.popPose();
    }

    private static void allExcept(SimpleObjModel model, String except, PoseStack ps, VertexConsumer vc, int light) {
        for (String part : model.getPartNames()) {
            if (part.equalsIgnoreCase(except)) continue;
            model.renderPart(part, ps, vc, light);
        }
    }
}
