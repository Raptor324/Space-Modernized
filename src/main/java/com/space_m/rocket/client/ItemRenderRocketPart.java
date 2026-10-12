//? if forge || neoforge {
package com.space_m.rocket.client;

import org.joml.Vector3f;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.space_m.rocket.RocketPart;
import com.space_m.rocket.RocketPart.PartType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Item-Darstellung der Space-Raketenteile, 1:1 NTM Space {@code ItemRenderMissilePart} (Space-Fassung: Rumpf im Inventar
 * {@code (h/4 - 0.5) * s} statt {@code h/4 * s}): Inventar = GUI-Hoehe (0 -> 4), Massstab 10/Hoehe, schraeg gestellt und
 * langsam um -Y drehend, unbeleuchtet; in der Hand T(0.5,0,0) S(0.4), am Boden S(0.4).
 *
 * <p>Das Item-Modell ({@code builtin/entity}) hat keine Display-Werte; die Hand-/Boden-Lagen der HBM-Raketenteile werden hier
 * angewendet (wie HBM {@code RBMKColumnItemRenderer.applyDisplay}).
 * Original-Klasse: ItemRenderMissilePart (Space-Teile)
 */
public class ItemRenderRocketPart extends BlockEntityWithoutLevelRenderer {

    private static ItemRenderRocketPart instance;

    public static ItemRenderRocketPart instance() {
        if (instance == null) instance = new ItemRenderRocketPart();
        return instance;
    }

    private ItemRenderRocketPart() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    // Display-Werte der HBM-Teilemodelle (mp_thruster_10_kerosene.json)
    static final ItemTransform FP_LEFT = new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(), new Vector3f(0.4F, 0.4F, 0.4F));
    static final ItemTransform FP_RIGHT = new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(), new Vector3f(0.4F, 0.4F, 0.4F));
    static final ItemTransform TP = new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5F / 16F, 0), new Vector3f(0.375F, 0.375F, 0.375F));
    static final ItemTransform GROUND = new ItemTransform(new Vector3f(), new Vector3f(0, 3F / 16F, 0), new Vector3f(0.25F, 0.25F, 0.25F));
    static final ItemTransform FIXED = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(0.5F, 0.5F, 0.5F));

    static void applyDisplay(ItemDisplayContext ctx, PoseStack ps) {
        ItemTransform t = switch (ctx) {
            case FIRST_PERSON_LEFT_HAND -> FP_LEFT;
            case FIRST_PERSON_RIGHT_HAND -> FP_RIGHT;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> TP;
            case GROUND -> GROUND;
            case FIXED -> FIXED;
            default -> ItemTransform.NO_TRANSFORM;
        };
        boolean left = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        ps.translate(0.5F, 0.5F, 0.5F);
        t.apply(left, ps);
        ps.translate(-0.5F, -0.5F, -0.5F);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        RocketPart part = RocketPart.getPart(stack);
        if (part == null || !part.renderInventoryModel) return;
        SimpleObjModel model = RocketModels.model(part.model);
        ResourceLocation tex = RocketModels.texture(part.texture);

        ps.pushPose();
        if (ctx == ItemDisplayContext.GUI) {
            // GUI-Pose von renderByItem (T(x+8,y+8) S(16,-16,16) T(-0.5)) -> linke obere Slotecke, y nach unten
            ps.translate(0.5F, 0.5F, 0.5F);
            ps.scale(1F / 16F, -1F / 16F, 1F / 16F);
            ps.translate(-8F, -8F, 0F);
            double height = part.guiheight;
            if (height == 0D) height = 4D;
            double size = 10;
            double scale = size / height;
            ps.translate(height / 2 * scale, 0, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(135));
            ps.mulPose(Axis.XP.rotationDegrees(145));
            if (part.type == PartType.WARHEAD) ps.translate(0, height / 8 * scale, 0);
            if (part.type == PartType.FUSELAGE) ps.translate(0, (height / 4 - 0.5) * scale, 0);
            ps.translate(3.5, 14, 0);
            ps.scale((float) -scale, (float) -scale, (float) -scale);
            ps.mulPose(Axis.YN.rotationDegrees(System.currentTimeMillis() / 25 % 360));
            // Original: GL_LIGHTING aus -> unbeleuchtet (Textur pur)
            model.renderAll(ps, buf.getBuffer(RenderType.text(tex)), 0xF000F0);
        } else {
            applyDisplay(ctx, ps);
            ps.translate(0.5, 0, 0.5);
            if (ctx != ItemDisplayContext.GROUND && ctx != ItemDisplayContext.FIXED) ps.translate(0.5, 0, 0);
            ps.scale(0.4F, 0.4F, 0.4F);
            model.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex)), light);
        }
        ps.popPose();
    }
}
//?}
