package com.space_m.entity.mob.client;

import org.joml.Matrix4f;

import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.space_m.SpaceMod;
import com.space_m.entity.mob.EntityMoonCow;

import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderMoonCow} + {@code ModelMoonCow}: Vanilla-Kuhmodell mit Textur {@code moon_cow} (Schatten 0.7) und
 * Raumhelm - ein Wuerfel mit {@code glass_boron}-Textur, an den Kopf gebunden, 0.25 nach vorn, auf 0.7 verkleinert
 * ({@code RenderUtil.renderBlock}, sechs Seiten).
 */
public class RenderMoonCow extends MobRenderer<EntityMoonCow, RenderMoonCow.MoonCowModel> {

    public static final ResourceLocation TEXTURE = SpaceMod.id("textures/entity/moon_cow.png");
    public static final ResourceLocation GLASS = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/block/glass_boron.png");

    public RenderMoonCow(EntityRendererProvider.Context ctx) {
        super(ctx, new MoonCowModel(ctx.bakeLayer(ModelLayers.COW)), 0.7F);
        this.addLayer(new HelmetLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMoonCow entity) {
        return TEXTURE;
    }

// Original-Klasse: ModelMoonCow
    public static class MoonCowModel extends CowModel<EntityMoonCow> {
        public MoonCowModel(ModelPart root) {
            super(root);
        }

        public ModelPart headPart() {
            return this.head;
        }
    }

    static class HelmetLayer extends RenderLayer<EntityMoonCow, MoonCowModel> {

        HelmetLayer(RenderLayerParent<EntityMoonCow, MoonCowModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffers, int light, EntityMoonCow cow, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ps.pushPose();
            if (cow.isBaby()) {
                // ModelQuadruped Kind: Kopf um (0, 8, 4)/16 verschoben
                ps.translate(0.0F, 8.0F / 16F, 4.0F / 16F);
            }
            this.getParentModel().headPart().translateAndRotate(ps);
            ps.translate(0, 0, -0.25F);
            ps.scale(0.7F, 0.7F, 0.7F);

            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(GLASS));
            int overlay = LivingEntityRenderer.getOverlayCoords(cow, 0.0F);
            Matrix4f m = ps.last().pose();
            float[][] quads = {
                    {-0.5F, 0.5F, -0.5F, 1, 1}, {0.5F, 0.5F, -0.5F, 0, 1}, {0.5F, -0.5F, -0.5F, 0, 0}, {-0.5F, -0.5F, -0.5F, 1, 0},
                    {-0.5F, 0.5F, 0.5F, 1, 1}, {-0.5F, 0.5F, -0.5F, 0, 1}, {-0.5F, -0.5F, -0.5F, 0, 0}, {-0.5F, -0.5F, 0.5F, 1, 0},
                    {0.5F, 0.5F, 0.5F, 1, 1}, {-0.5F, 0.5F, 0.5F, 0, 1}, {-0.5F, -0.5F, 0.5F, 0, 0}, {0.5F, -0.5F, 0.5F, 1, 0},
                    {0.5F, 0.5F, -0.5F, 1, 1}, {0.5F, 0.5F, 0.5F, 0, 1}, {0.5F, -0.5F, 0.5F, 0, 0}, {0.5F, -0.5F, -0.5F, 1, 0},
                    {-0.5F, -0.5F, -0.5F, 1, 1}, {0.5F, -0.5F, -0.5F, 0, 1}, {0.5F, -0.5F, 0.5F, 0, 0}, {-0.5F, -0.5F, 0.5F, 1, 0},
                    {0.5F, 0.5F, -0.5F, 1, 1}, {-0.5F, 0.5F, -0.5F, 0, 1}, {-0.5F, 0.5F, 0.5F, 0, 0}, {0.5F, 0.5F, 0.5F, 1, 0}};
            for (float[] v : quads) {
                RenderHooks.vertexFull(vc, m, v[0], v[1], v[2], 255, 255, 255, 255, v[3], v[4], overlay, light, 0, 1, 0);
            }
            ps.popPose();
        }
    }
}
