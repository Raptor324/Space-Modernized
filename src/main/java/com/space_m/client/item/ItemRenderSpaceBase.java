package com.space_m.client.item;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.item.OrigInventoryTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;
import com.space_m.entity.mob.client.RenderBFA;
import com.space_m.item.ItemAngelCore;
import com.space_m.item.ItemSwarmMember;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 3D-Item-Darstellung zweier Space-Gegenstaende ueber die Kette des Original-{@code ItemRenderBase} (Inventar:
 * {@link OrigInventoryTransform} + {@code renderInventory()}; sonst T(0.5, 0.25, 0) bzw. am Boden/Rahmen S(1.5), dann
 * S(0.25) und ausser in dritter Person R(90 Y); danach {@code renderCommonWithStack}):
 * <ul>
 *   <li>{@code swarm_member}: {@code ItemRenderLibrary}-Eintrag - Inventar T(0,3,0) S(1.8) R(-45 Z) R(30 X) und
 *       Drehung {@code currentTimeMillis % 7200 * -0.05} um Y; Modell {@code dyson_swarm_satellite.obj}.</li>
 *   <li>{@code core_angel}: {@code ItemRenderAngelCore} - Inventar T(0,-3,0) S(4); immer S(4) und der Rumpf
 *       ({@code body}) des Balefire-Engels mit {@code bfa_body}.</li>
 * </ul>
 * Original-Klasse: ItemRenderAngelCore
 * Original-Klasse: ItemRenderLibrary (swarm_member)
 */
public class ItemRenderSpaceBase extends BlockEntityWithoutLevelRenderer {

    private static final SimpleObjModel SWARM_MEMBER = new SimpleObjModel(SpaceMod.id("models/dyson_swarm_satellite.obj"));
    private static final ResourceLocation SWARM_MEMBER_TEX = SpaceMod.id("textures/models/dyson_swarm_satellite.png");
    private static final SimpleObjModel BFANGEL = new SimpleObjModel(SpaceMod.id("models/mobs/balefire_angel.obj"));

    private static ItemRenderSpaceBase instance;

    public static ItemRenderSpaceBase instance() {
        if (instance == null) instance = new ItemRenderSpaceBase();
        return instance;
    }

    private ItemRenderSpaceBase() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        boolean swarm = stack.getItem() instanceof ItemSwarmMember;
        boolean angel = stack.getItem() instanceof ItemAngelCore;
        if (!swarm && !angel) return;

        ps.pushPose();
        boolean gui = ctx == ItemDisplayContext.GUI;
        int l = gui ? 0xF000F0 : light;
        if (gui) {
            OrigInventoryTransform.apply(ps);
            if (swarm) {
                ps.translate(0, 3, 0);
                ps.scale(1.8F, 1.8F, 1.8F);
                ps.mulPose(Axis.ZP.rotationDegrees(-45));
                ps.mulPose(Axis.XP.rotationDegrees(30));
                ps.mulPose(Axis.YP.rotationDegrees(System.currentTimeMillis() % 7200 * -0.05F));
            } else {
                ps.translate(0, -3, 0);
                ps.scale(4, 4, 4);
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

        if (swarm) {
            SWARM_MEMBER.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(SWARM_MEMBER_TEX)), l);
        } else {
            // GL_SMOOTH: SimpleObjModel nutzt ohnehin die Vertexnormalen
            ps.scale(4, 4, 4);
            BFANGEL.renderPart("body", ps, buf.getBuffer(RenderType.entityCutoutNoCull(RenderBFA.TEXTURE)), l);
        }
        ps.popPose();
    }
}
