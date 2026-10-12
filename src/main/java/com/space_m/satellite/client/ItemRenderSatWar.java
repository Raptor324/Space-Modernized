//? if forge || neoforge {
package com.space_m.satellite.client;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.SpaceMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Item-Darstellung von {@code sat_war}: 1:1 der {@code ItemRenderLibrary}-Eintrag von NTM Space (ItemRenderBase):
 * Inventar T(8,10) R(-30 X) R(45 Y) S(-1) dann T(3,-2,0) S(3) R(-45 Y) R(-45 Z); sonst T(0.5,0.25,0) (Boden S(1.5)),
 * S(0.25), R(90 Y) ausser dritte Person; immer T(0,0,-0.875) und das Railkanonen-Modell ({@code railgun_main}).
 * Original-Klasse: ItemRenderLibrary (sat_war), ItemRenderBase
 */
public class ItemRenderSatWar extends BlockEntityWithoutLevelRenderer {

    private static final SimpleObjModel MODEL = new SimpleObjModel(SpaceMod.id("models/obj/railgun_main.obj"));
    private static final ResourceLocation TEXTURE = SpaceMod.id("textures/models/railgun_main.png");

    private static ItemRenderSatWar instance;

    public static ItemRenderSatWar instance() {
        if (instance == null) instance = new ItemRenderSatWar();
        return instance;
    }

    private ItemRenderSatWar() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {
            // GUI-Pose von renderByItem (T(x+8,y+8) S(16,-16,16) T(-0.5)) -> linke obere Slotecke, y nach unten
            ps.translate(0.5F, 0.5F, 0.5F);
            ps.scale(1F / 16F, -1F / 16F, 1F / 16F);
            ps.translate(-8F, -8F, 0F);
            ps.translate(8, 10, 0);
            ps.mulPose(Axis.XP.rotationDegrees(-30));
            ps.mulPose(Axis.YP.rotationDegrees(45));
            ps.scale(-1, -1, -1);
            // renderInventory
            ps.translate(3, -2, 0);
            ps.scale(3, 3, 3);
            ps.mulPose(Axis.YP.rotationDegrees(-45));
            ps.mulPose(Axis.ZP.rotationDegrees(-45));
        } else {
            ps.translate(0.5F, 0.5F, 0.5F);
            if (ctx != ItemDisplayContext.GROUND) ps.translate(0.5, 0.25, 0);
            else ps.scale(1.5F, 1.5F, 1.5F);
            ps.scale(0.25F, 0.25F, 0.25F);
            if (ctx != ItemDisplayContext.THIRD_PERSON_LEFT_HAND && ctx != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        // renderCommonWithStack
        ps.translate(0, 0, -0.875);
        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), ctx == ItemDisplayContext.GUI ? 0xF000F0 : light);
        ps.popPose();
    }
}
//?}
